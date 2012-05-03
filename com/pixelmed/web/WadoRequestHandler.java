/* Copyright (c) 2001-2011, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.web;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.pixelmed.database.DatabaseInformationModel;
import com.pixelmed.dicom.InformationEntity;
import com.pixelmed.dicom.SOPClass;
import com.pixelmed.display.ConsumerFormatImageMaker;


import com.pixelmed.dicom.DicomInputStream;
import com.pixelmed.dicom.DicomOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream; 
import com.pixelmed.dicom.TransferSyntax;
import com.pixelmed.dicom.DicomStreamCopier;

/**
 * <p>The {@link com.pixelmed.web.WadoRequestHandler WadoRequestHandler} creates a response to an HHTP request for
 * a WADO request.</p>
 *
 * @author	dclunie
 */
class WadoRequestHandler extends RequestHandler {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/web/WadoRequestHandler.java,v 1.9 2011/04/04 14:47:15 dclunie Exp $";

	private static Map cacheOfConvertedFiles = new HashMap();	// OK to be static as long as we never delete files from cache that may be in use by other threads
	
	private void addToCacheOfConvertedFiles(Object key,String filename) {
		synchronized (cacheOfConvertedFiles) {
			cacheOfConvertedFiles.put(key,filename);
		}
	}
	
	private String getFromCacheOfConvertedFiles(Object key) {
		String filename = null;
		synchronized (cacheOfConvertedFiles) {
			if (key != null) {
				filename = (String)(cacheOfConvertedFiles.get(key));
			}
		}
		return filename;
	}

	private static final Object makeCacheKey(String objectUID,double windowCenter,double windowWidth,int columns,int rows,int quality) {
		return objectUID+"#"+Double.toString(windowCenter)+"#"+Double.toString(windowWidth)+"#"+Integer.toString(columns)+"#"+Integer.toString(rows)+"#"+Integer.toString(quality);
	}

	protected WadoRequestHandler(String stylesheetPath,int webServerDebugLevel) {
		super(stylesheetPath,webServerDebugLevel);
	}

	protected void generateResponseToGetRequest(DatabaseInformationModel databaseInformationModel,String rootURL,String requestURI,WebRequest request,String requestType,OutputStream out) throws IOException {
		try {
			// assert (requestType == null);
			WadoRequest wadoRequest = new WadoRequest(request);
			String objectUID=wadoRequest.getObjectUID();
			// should really check Study and Series Instance UIDs as well, but don't need them to find SOP Instance
			ArrayList records = databaseInformationModel.findAllAttributeValuesForAllRecordsForThisInformationEntityWithSpecifiedUID(
				InformationEntity.INSTANCE,objectUID);
			if (records != null && records.size() == 1) {
				Map map = (Map)(records.get(0));
				String filename = (String)(map.get(databaseInformationModel.getLocalFileNameColumnName(InformationEntity.INSTANCE)));
if (webServerDebugLevel > 1) System.err.println("WadoRequestHandler.generateResponseToGetRequest(): Found in database "+filename);
				File file = new File(filename);
				if (file.exists() && file.isFile()) {		
if (webServerDebugLevel > 1) System.err.println("WadoRequestHandler.generateResponseToGetRequest(): File exists");
					if (wadoRequest.isContentTypeDicom()) {
if (webServerDebugLevel > 1) System.err.println("WadoRequestHandler.generateResponseToGetRequest(): is DICOM request");
						// not paying any attention to requested Transfer Syntax; give them whatever we have
						sendHeaderAndBodyOfFile(out,file,objectUID+".dcm","application/dicom");
						
						// make copy with IVRLE and no metaheader
						//File convertedFile  = File.createTempFile("RequestTypeServer",".dcm");
						//convertedFile.deleteOnExit();
						//DicomInputStream  i = new DicomInputStream (new BufferedInputStream(new FileInputStream(file)));
						//DicomOutputStream o = new DicomOutputStream(new BufferedOutputStream(new FileOutputStream(convertedFile)),null/*meta*/,TransferSyntax.ImplicitVRLittleEndian/*dataset*/);
						//new DicomStreamCopier(i,o);
						//sendHeaderAndBodyOfFile(out,convertedFile,objectUID+".dcm","application/dicom");
					}
					else {
if (webServerDebugLevel > 1) System.err.println("WadoRequestHandler.generateResponseToGetRequest(): is non-DICOM request");
						String sopClassUID = (String)(map.get("SOPCLASSUID"));
if (webServerDebugLevel > 1) System.err.println("WadoRequestHandler.generateResponseToGetRequest(): SOP Class UID from database = "+sopClassUID);
						if (sopClassUID != null) {
							if (SOPClass.isImageStorage(sopClassUID)) {
								double windowWidth = wadoRequest.getWindowWidth();
								double windowCenter = wadoRequest.getWindowCenter();
								int columns = wadoRequest.getColumns();
								int rows = wadoRequest.getRows();
								int quality = wadoRequest.getImageQuality();
								String convertedFormat = "jpeg";
								File convertedFile = null;
								Object cacheKey = makeCacheKey(objectUID,windowCenter,windowWidth,columns,rows,quality);
								String convertedFileName = getFromCacheOfConvertedFiles(cacheKey);
								try {
									if (convertedFileName == null) {
if (webServerDebugLevel > 1) System.err.println("WadoRequestHandler.generateResponseToGetRequest(): not in cache");
										convertedFile = File.createTempFile("RequestTypeServer",".jpg");
										convertedFile.deleteOnExit();
										convertedFileName = convertedFile.getAbsolutePath();
										ConsumerFormatImageMaker.convertFileToEightBitImage(
											filename,convertedFileName,convertedFormat,windowCenter,windowWidth,columns,rows,quality,
											ConsumerFormatImageMaker.ALL_ANNOTATIONS,
											webServerDebugLevel);
										addToCacheOfConvertedFiles(cacheKey,convertedFileName);
									}
									else {
if (webServerDebugLevel > 1) System.err.println("WadoRequestHandler.generateResponseToGetRequest(): in cache");
										convertedFile = new File(convertedFileName);
									}
									String convertedExtension = ".jpg";
									String convertedContentType = "image/jpeg";
									sendHeaderAndBodyOfFile(out,convertedFile,objectUID+convertedExtension,convertedContentType);
								}
								catch (Exception e) {
									e.printStackTrace(System.err);
									throw new Exception("Cannot convert image to "+convertedFormat);
								}
								finally {
if (webServerDebugLevel > 1) System.err.println("WadoRequestHandler.generateResponseToGetRequest(): convertedFileName = "+convertedFileName);
									//convertedFile.delete();	// don't delete, since caching
								}
							}
							else {
								throw new Exception("Only images supported");
							}
						}
						else {
							//throw new Exception("Unsupported contentType");
							throw new Exception("Cannot determine SOP Class of instance");
						}
					}
				}
				else {
					throw new Exception("SOP Instance "+objectUID+" in database but file \""+filename+"\" referenced by database missing");
				}
			}
			else {
				throw new Exception("Could not find SOP Instance "+objectUID);
			}
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
if (webServerDebugLevel > 0) System.err.println("WadoRequestHandler.generateResponseToGetRequest(): Sending 404 Not Found");
			send404NotFound(out,e.getMessage());
		}
	}
}


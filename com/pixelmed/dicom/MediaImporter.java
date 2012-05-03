/* Copyright (c) 2001-2011, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import java.io.File;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;

import java.awt.Component;

import javax.swing.JFileChooser;
import javax.swing.JProgressBar;

import com.pixelmed.utils.FileUtilities;
import com.pixelmed.utils.MessageLogger;
import com.pixelmed.utils.PrintStreamMessageLogger;		// used in main method for testing
import com.pixelmed.display.DialogMessageLogger;		// used in main method for testing

/**
 * <p>This class is designed to support the importation of DICOM files from
 * interchange media (such as CDs and DVDs).</p>
 * 
 * <p>It supports locating a DICOMDIR file and iterating through the list of
 * referenced files.</p>
 * 
 * <p>The actual work (e.g. to import the file into a database or similar) is
 * performed by the implementation of the {@link MediaImporter#doSomethingWithDicomFileOnMedia(String) doSomethingWithDicomFileOnMedia}
 * method in a sub-class of this class.</p>
 *
 * @see com.pixelmed.database.DatabaseMediaImporter
 * 
 * @author	dclunie
 */
public class MediaImporter {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/MediaImporter.java,v 1.9 2012/02/01 23:02:09 dclunie Exp $";

	protected String mediaDirectoryPath;
	protected MessageLogger logger;
	protected JProgressBar progressBar;
	
	/**
	 * @param	s	message to log
	 */
	protected void logLn(String s) {
		if (logger != null) {
			logger.sendLn(s);
		}
//System.err.println(s);
	}
		
	/**
	 * <p>Construct an importer that will looked for files in the system default path.</p>
	 *
	 * @param	logger			where to send status updates as files are read (may be null for no logging)
	 */
	public MediaImporter(MessageLogger logger) {
		mediaDirectoryPath=null;
		this.logger=logger;
		this.progressBar=null;
	}

	/**
	 * <p>Construct an importer that will looked for files in the specified path.</p>
	 *
	 * @param	mediaDirectoryPath	where to begin looking for the DICOMDIR and DICOM files
	 * @param	logger			where to send status updates as files are read (may be null for no logging)
	 */
	public MediaImporter(String mediaDirectoryPath,MessageLogger logger) {
		this.mediaDirectoryPath=mediaDirectoryPath;
		this.logger=logger;
		this.progressBar=null;
	}

	/**
	 * <p>Construct an importer that will looked for files in the specified path.</p>
	 *
	 * @param	mediaDirectoryPath	where to begin looking for the DICOMDIR and DICOM files
	 * @param	logger			where to send status updates as files are read (may be null for no logging)
	 * @param	progressBar		where to update progress as files are read (may be null for no progress bar)
	 */
	public MediaImporter(String mediaDirectoryPath,MessageLogger logger,JProgressBar progressBar) {
		this.mediaDirectoryPath=mediaDirectoryPath;
		this.logger=logger;
		this.progressBar=progressBar;
	}

	/**
	 * <p>Pop up a file chooser dialog that allows the user to specify the location of
	 * the DICOMDIR file, or the parent folder (for example, the drive or volume) in which
	 * the DICOMDIR file is located, and then import the referenced files.</p>
	 *
	 * <p>Will be positioned relative to the parent component (for example, centered over the component)
	 * if specified, else placed in a look-and-feel-dependent position such as the center of the screen if null.</p>
	 *
	 * @param		parent			the parent component of the dialog; can be <code>null</code>
	 * @exception	IOException		thrown if the DICOMDIR file (but not any referenced files) cannot be opened or read
	 * @exception	DicomException		thrown if the DICOMDIR file cannot be parsed
	 */
	public void choosePathAndImportDicomFiles(Component parent) throws IOException, DicomException {
		JFileChooser chooser = new JFileChooser(mediaDirectoryPath);
		chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
			mediaDirectoryPath=chooser.getCurrentDirectory().getAbsolutePath();	// keep around for next time
			String pathName = chooser.getSelectedFile().getAbsolutePath();
			importDicomFiles(pathName);
		}
	}

	/**
	 * <p>Pop up a file chooser dialog that allows the user to specify the location of
	 * the DICOMDIR file, or the parent folder (for example, the drive or volume) in which
	 * the DICOMDIR file is located, and then import the referenced files.</p>
	 *
	 * <p>Will be placed in a look-and-feel-dependent position such as the center of the screen.</p>
	 *
	 * @exception	IOException		thrown if the DICOMDIR file (but not any referenced files) cannot be opened or read
	 * @exception	DicomException		thrown if the DICOMDIR file cannot be parsed
	 */
	public void choosePathAndImportDicomFiles() throws IOException, DicomException {
		choosePathAndImportDicomFiles(null);
	}
	
	/**
	 * <p>Check for valid information, and that the file is not compressed or not a suitable storage object for import.</p>
	 *
	 * @param	sopClassUID
	 * @param	transferSyntaxUID
	 */
	protected boolean isOKToImport(String sopClassUID,String transferSyntaxUID) {
		return sopClassUID != null
		    && (SOPClass.isImageStorage(sopClassUID) || (SOPClass.isNonImageStorage(sopClassUID) && ! SOPClass.isDirectory(sopClassUID)))
		    && transferSyntaxUID != null
		    && (transferSyntaxUID.equals(TransferSyntax.ImplicitVRLittleEndian)
		     || transferSyntaxUID.equals(TransferSyntax.ExplicitVRLittleEndian)
		     || transferSyntaxUID.equals(TransferSyntax.ExplicitVRBigEndian));
	}

	/**
	 * <p>Read a DICOMDIR file, and then import any DICOM files that it references.</p>
	 *
	 * <p>How errors are handled during the importation of the referenced files
	 * depends on the implementation of {@link MediaImporter#doSomethingWithDicomFileOnMedia(String) doSomethingWithDicomFileOnMedia}
	 * in the sub-class. Any such errors will not interrupt the execution of this method (i.e., will not prevent the
	 * importation of the remaining files).</p>
	 *
	 * @param	pathName		the path name to a DICOMDIR file or folder containing a DICOMDIR file
	 *
	 * @exception	IOException		thrown if the DICOMDIR file (but not any referenced files) cannot be opened or read
	 * @exception	DicomException		thrown if the DICOMDIR file cannot be parsed
	 */
	public void importDicomFiles(String pathName) throws IOException, DicomException {
		if (pathName != null) {
			File path = new File(pathName);
			File dicomdirFile = null;		// look for DICOMDIR here or in root folder of here, with various case permutations
			if (path != null && path.exists()) {
				if (path.isFile() && path.getName().toUpperCase(java.util.Locale.US).equals("DICOMDIR")) {
					dicomdirFile=path;
				}
				else if (path.isDirectory()) {
					File tryFile = new File(path,"DICOMDIR");
					if (tryFile != null && tryFile.exists()) {
						dicomdirFile=tryFile;
					}
					else {
						tryFile = new File(path,"Dicomdir");
						if (tryFile != null && tryFile.exists()) {
							dicomdirFile=tryFile;
						}
						else {
							tryFile = new File(path,"dicomdir");
							if (tryFile != null && tryFile.exists()) {
								dicomdirFile=tryFile;
							}
							// else give up
						}
					}
				}
			}
			if (dicomdirFile != null) {
				logLn("Found DICOMDIR at: "+dicomdirFile);
				DicomInputStream i = new DicomInputStream(new BufferedInputStream(new FileInputStream(dicomdirFile)));
				AttributeList list = new AttributeList();
				list.read(i);
				i.close();
				DicomDirectory dicomDirectory = new DicomDirectory(list);
				HashMap allDicomFiles = dicomDirectory.findAllContainedReferencedFileNamesAndTheirRecords(dicomdirFile.getParentFile().getPath());
//System.err.println("Referenced files: "+allDicomFiles);
				if (progressBar != null) {
					progressBar.setValue(0);
					progressBar.setMaximum(allDicomFiles.size());
					progressBar.repaint();
				}
				int count = 0;
				Iterator it = allDicomFiles.keySet().iterator();
				while (it.hasNext()) {
					String mediaFileName = (String)it.next();
					if (mediaFileName != null) {
						boolean goodToGo = false;
						DicomDirectoryRecord record = (DicomDirectoryRecord)(allDicomFiles.get(mediaFileName));
						if (record != null) {
							AttributeList rlist = ((DicomDirectoryRecord)record).getAttributeList();
							if (rlist != null) {
								String sopClassUID = Attribute.getSingleStringValueOrNull(rlist,TagFromName.ReferencedSOPClassUIDInFile);
								String transferSyntaxUID = Attribute.getSingleStringValueOrNull(rlist,TagFromName.ReferencedTransferSyntaxUIDInFile);
								if (sopClassUID == null || transferSyntaxUID == null) {
									// the directory record is invalid; these should be present
									// don't give up though ... try reading the meta-information header ...
									try {
										DicomInputStream di = new DicomInputStream(new BufferedInputStream(new FileInputStream(mediaFileName)));
										if (di.haveMetaHeader()) {
											AttributeList dlist = new AttributeList();
											dlist.readOnlyMetaInformationHeader(di);
											// Don't replace them unless they were null; they might be missing in the meta header !
											if (sopClassUID == null) {
												sopClassUID = Attribute.getSingleStringValueOrNull(dlist,TagFromName.MediaStorageSOPClassUID);
											}
											if (transferSyntaxUID == null) {
												transferSyntaxUID = Attribute.getSingleStringValueOrNull(dlist,TagFromName.TransferSyntaxUID);
											}
										}
										di.close();
									}
									catch (Exception e) {
										// ignore the error ... will fail on null sopClassUID or transferSyntaxUID
									}
								}
								if (isOKToImport(sopClassUID,transferSyntaxUID)) {
									goodToGo=true;
								}
								else {
									logLn("Is a DICOM file but bad meta-header, not a storage object, or is compressed: "
										+mediaFileName+" SOP Class="+sopClassUID+", Transfer Syntax="+transferSyntaxUID);
								}
							}
						}
						if (goodToGo) {
							logLn("Is a suitable DICOMDIR referenced file: "+mediaFileName);
							doSomethingWithDicomFileOnMedia(mediaFileName);
						}
						else {
							logLn("Not a suitable DICOMDIR referenced file: "+mediaFileName);
						}
					}
					++count;
					if (progressBar != null) {
						progressBar.setValue(count);
						progressBar.repaint();
					}
				}
			}
			else {
				ArrayList listOfAllFiles = FileUtilities.listFilesRecursively(path);
				if (progressBar != null) {
					progressBar.setValue(0);
					progressBar.setMaximum(listOfAllFiles.size());
					progressBar.repaint();
				}
				int count = 0;
				Iterator it = listOfAllFiles.iterator();
				while (it.hasNext()) {
					File mediaFile = (File)it.next();
					if (mediaFile != null) {
						// It might or might not be a DICOM file ... only way to tell is to try it
						try {
							DicomInputStream i = new DicomInputStream(new BufferedInputStream(new FileInputStream(mediaFile)));
							boolean goodToGo = false;
							if (i.haveMetaHeader()) {
								AttributeList list = new AttributeList();
								list.readOnlyMetaInformationHeader(i);
								String sopClassUID = Attribute.getSingleStringValueOrNull(list,TagFromName.MediaStorageSOPClassUID);
								String transferSyntaxUID = Attribute.getSingleStringValueOrNull(list,TagFromName.TransferSyntaxUID);
								if (isOKToImport(sopClassUID,transferSyntaxUID)) {
									goodToGo=true;
								}
								else {
									logLn("Is a DICOM file but bad meta-header, not a storage object, or is compressed: "
										+mediaFile+" SOP Class="+sopClassUID+", Transfer Syntax="+transferSyntaxUID);
								}
							}
							i.close();	// do this BEFORE calling the handler, just in case
							if (goodToGo) {
								logLn("Is a DICOM file: "+mediaFile);
								doSomethingWithDicomFileOnMedia(mediaFile.getPath());
							}
							else {
								logLn("Not a DICOM PS 3.10 file: "+mediaFile);
							}
						}
						catch (Exception e) {
								logLn("Not a DICOM file: "+mediaFile);
						}
					}
					++count;
					if (progressBar != null) {
						progressBar.setValue(count);
						progressBar.repaint();
					}
				}
			}
		}
		logLn("Media import complete");
	}
	
	/**
	 * <p>Do something with the referenced DICOM file that has been encountered.</p>
	 *
	 * <p>This method needs to be implemented in a sub-class to do anything useful.
	 * The default method does nothing.</p>
	 *
	 * <p>This method does not define any exceptions and hence must handle any
	 * errors locally.</p>
	 *
	 * @param	mediaFileName	the fully qualified path name to a DICOM file
	 */
	protected void doSomethingWithDicomFileOnMedia(String mediaFileName) {
		//logLn("MediaImporter.doSomethingWithDicomFile(): "+mediaFileName);
	}

	/**
	 * <p>Check that DICOM files are present and importable.</p>
	 *
	 * @param	arg	array of one string - the path to the media or folder containing
	 * the files to check are importable (in which case will write messages to
	 * stderr), or else will pop up a file chooser dialog (and write messages to
	 * a dialog box)
	 */
	public static void main(String arg[]) {
		try {
			if (arg.length == 0) {
				MessageLogger logger = new DialogMessageLogger("MediaImporter",512,384,true);
				MediaImporter importer = new MediaImporter("/",logger);
				importer.choosePathAndImportDicomFiles();
			}
			else if (arg.length == 1) {
				String           pathName=arg[0];
				MessageLogger logger = new PrintStreamMessageLogger(System.err);
				MediaImporter importer = new MediaImporter(logger);
				importer.importDicomFiles(pathName);
			}
			else {
				throw new Exception("Argument list must be zero or one value");
			}
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
			System.exit(0);
		}
	}
}



/* Copyright (c) 2001-2011, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.util.Iterator;
import java.util.Arrays;

import java.awt.GraphicsEnvironment;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;

//import java.awt.Transparency;
//import java.awt.color.ColorSpace;
//import java.awt.image.ByteLookupTable;
//import java.awt.image.ComponentColorModel;
//import java.awt.image.DataBuffer;
//import java.awt.image.LookupOp;
import java.awt.RenderingHints;
//import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.ImageWriteParam;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageOutputStream;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.DecimalStringAttribute;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.DicomInputStream;
import com.pixelmed.dicom.GeometryOfVolumeFromAttributeList;
import com.pixelmed.dicom.ModalityTransform;
import com.pixelmed.dicom.SOPClass;
import com.pixelmed.dicom.TagFromName;
import com.pixelmed.dicom.VOITransform;

import com.pixelmed.geometry.GeometryOfVolume;

/**
 * <p>A class of static methods to make consumer format images from DICOM objects.</p>
 *
 * @author	dclunie
 */
public class ConsumerFormatImageMaker {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/ConsumerFormatImageMaker.java,v 1.15 2011/06/14 19:10:20 dclunie Exp $";
	
	public static final String ALL_ANNOTATIONS = "all";
	public static final String ICON_ANNOTATIONS = "icon";
	
	/**
	 * <p>Create a single frame 8-bit per channel image (windowed if grayscale) from the first, or only, frame.</p>
	 *
	 * <p>Uses the window center and width in the attribute list.</p>
	 *
	 * @param	list		the DICOM attributes
	 * @param	debugLevel	
	 * @return			an 8 bit BufferedImage
	 * @exception			if attribute is not an image
	 */
	public static final BufferedImage makeEightBitImage(AttributeList list,int debugLevel) throws DicomException {
		String sopClassUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPClassUID);
		if (!SOPClass.isImageStorage(sopClassUID)) {
			throw new DicomException("SOP Class is not an image");
		}
		
		SourceImage sImg = new SourceImage(list);
				
		BufferedImage useSrcImage = sImg.getBufferedImage(); 	// possibly the first of multiple
		BufferedImage renderedImage = null;
				
		if (useSrcImage.getColorModel().getNumComponents() != 1) {
			try {
				renderedImage=BufferedImageUtilities.convertToMostFavorableImageType(useSrcImage);
			}
			catch (Exception e) {
				e.printStackTrace(System.err);
				renderedImage=useSrcImage;
			}
		}
		else {
			ModalityTransform modalityTransform = sImg.getModalityTransform();
			VOITransform           voiTransform = sImg.getVOITransform();
			boolean                      signed = sImg.isSigned();
			boolean                    inverted = sImg.isInverted();
			boolean                      usePad = sImg.isPadded();
			int                             pad = sImg.getPadValue();
			int			     imgMin = sImg.getMinimum();
			int			     imgMax = sImg.getMaximum();
			int                     largestGray = sImg.getPaletteColorLargestGray();
			int                firstvalueMapped = sImg.getPaletteColorFirstValueMapped();
			int                 numberOfEntries = sImg.getPaletteColorNumberOfEntries();
			int                    bitsPerEntry = sImg.getPaletteColorBitsPerEntry();
			short                    redTable[] = sImg.getPaletteColorRedTable();
			short                  greenTable[] = sImg.getPaletteColorGreenTable();
			short                   blueTable[] = sImg.getPaletteColorBlueTable();

			double useSlope=1;
			double useIntercept=0;
			if (modalityTransform != null) {
				    useSlope = modalityTransform.getRescaleSlope    (0);		// (first) frame index
				useIntercept = modalityTransform.getRescaleIntercept(0);
			}

			double windowWidth=0;
			double windowCenter=0;
			if (voiTransform != null && voiTransform.getNumberOfTransforms(0) > 0) {	// (first) frame index
				 windowWidth = voiTransform.getWidth(0,0);					// (first) frame index, (first) transform
				windowCenter = voiTransform.getCenter(0,0);
			}
			if (windowWidth == 0) {			// use supplied window only if there was one, and if its width was not zero (center may legitimately be zero)
if (debugLevel > 2) System.err.println("For statistically derived window: imgMin = "+imgMin);
if (debugLevel > 2) System.err.println("For statistically derived window: imgMax = "+imgMax);
				double ourMin = imgMin*useSlope+useIntercept;
				double ourMax = imgMax*useSlope+useIntercept;
if (debugLevel > 2) System.err.println("For statistically derived window: rescaled min = "+ourMin);
if (debugLevel > 2) System.err.println("For statistically derived window: rescaled min = "+ourMax);
				windowWidth=(ourMax-ourMin);
				windowCenter=(ourMax+ourMin)/2.0;
if (debugLevel > 1) System.err.println("Using statistically derived center "+windowCenter+" and width "+windowWidth);
			}
				
if (debugLevel > 1) System.err.println("Using rescale slope "+useSlope+" and intercept "+useIntercept+" and window center "+windowCenter+" and width "+windowWidth);

			int useVOIFunction = 0;
			
			renderedImage = (numberOfEntries == 0 || redTable == null)
				? (useVOIFunction == 1
					? WindowCenterAndWidth.applyWindowCenterAndWidthLogistic(useSrcImage,windowCenter,windowWidth,signed,inverted,useSlope,useIntercept,usePad,pad)
					: WindowCenterAndWidth.applyWindowCenterAndWidthLinear(useSrcImage,windowCenter,windowWidth,signed,inverted,useSlope,useIntercept,usePad,pad)
					)
				: WindowCenterAndWidth.applyWindowCenterAndWidthWithPaletteColor(useSrcImage,windowCenter,windowWidth,sImg.isSigned(),inverted,useSlope,useIntercept,usePad,pad,
					largestGray,bitsPerEntry,numberOfEntries,redTable,greenTable,blueTable);
		}
		return renderedImage;
	}
	
	/**
	 * <p>Read a DICOM image input file, and create a single frame 8-bit per channel image (windowed if grayscale) from the first, or only, frame.</p>
	 *
	 * <p>Uses the window center and width in the file.</p>
	 *
	 * @param	dicomFileName	the input file name
	 * @param	outputFileName	the output file name
	 * @param	outputFormat	the output file format name that a JIIO SPI will recognize (e.g. "jpeg")
	 * @param	debugLevel	
	 */
	public static void convertFileToEightBitImage(String dicomFileName,String outputFileName,String outputFormat,int debugLevel) throws DicomException, IOException {
		 convertFileToEightBitImage(dicomFileName,outputFileName,outputFormat,0,0,0,0,-1,ALL_ANNOTATIONS,debugLevel);
	}
	
	/**
	 * <p>Read a DICOM image input file, and create a single frame 8-bit per channel image (windowed if grayscale) from the first, or only, frame.</p>
	 *
	 * @param	dicomFileName	the input file name
	 * @param	outputFileName	the output file name
	 * @param	outputFormat	the output file format name that a JIIO SPI will recognize (e.g. "jpeg")
	 * @param	windowCenter	the window center to use
	 * @param	windowWidth	the window width to use, or 0 if to use the width and center in the DICOM file
	 * @param	imageWidth	the width (number of columns) to make, or <= 0 if default (the width in the DICOM file, or scaled by height with pixel aspect ratio preserved)
	 * @param	imageHeight	the height (number of rows) to make, or <= 0 if default (the height in the DICOM file, or scaled by width with pixel aspect ratio preserved)
	 * @param	imageQuality	the image quality from 1 to 100 (best), or -1 if absent
	 * @param	annotation		the type of annotation to apply (choice is "all"; anything else means no annotation), or null if absent
	 * @param	debugLevel	
	 */
	public static void convertFileToEightBitImage(String dicomFileName,String outputFileName,String outputFormat,
			double windowCenter,double windowWidth,int imageWidth,int imageHeight,int imageQuality,String annotation,
			int debugLevel) throws DicomException, IOException {
		AttributeList list = new AttributeList();
		DicomInputStream in = new DicomInputStream(new BufferedInputStream(new FileInputStream(dicomFileName)));
		list.read(in);
		in.close();
		if (windowWidth != 0) {
			Attribute aWindowWidth = new DecimalStringAttribute(TagFromName.WindowWidth);
			aWindowWidth.addValue(windowWidth);
			list.put(aWindowWidth);			// removal of existing is implicit in the put
			Attribute aWindowCenter = new DecimalStringAttribute(TagFromName.WindowCenter);
			aWindowCenter.addValue(windowCenter);
			list.put(aWindowCenter);
		}
		BufferedImage renderedImage = makeEightBitImage(list,debugLevel);
		//BufferedImage windowedImage = makeEightBitImage(list,debugLevel);
		
		//renderedImage = BufferedImageUtilities.convertToMostFavorableImageTypeWithPixelCopy(renderedImage);
		//renderedImage = BufferedImageUtilities.convertToMostFavorableImageTypeWithBandCombineOp(renderedImage);

                //ColorModel dstColorModel = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getColorModel();
		//WritableRaster dstRaster = dstColorModel.createCompatibleWritableRaster(windowedImage.getWidth(),windowedImage.getHeight());
                //BufferedImage renderedImage = new BufferedImage(dstColorModel, dstRaster, dstColorModel.isAlphaPremultiplied(), null);
                //BufferedImage renderedImage = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(
		//	windowedImage.getWidth(),windowedImage.getHeight());
		//BufferedImage renderedImage = new BufferedImage(windowedImage.getWidth(),windowedImage.getHeight(),BufferedImage.TYPE_INT_ARGB);
		
		try {
if (debugLevel > 0) System.err.println("ConsumerFormatImageMaker.convertFileToEightBitImage(): Requested width = "+imageWidth+" height = "+imageHeight);
			int srcWidth = renderedImage.getWidth();
			int srcHeight = renderedImage.getHeight();
if (debugLevel > 1) System.err.println("ConsumerFormatImageMaker.convertFileToEightBitImage(): Source width = "+srcWidth+" width = "+srcHeight);
			if (imageWidth <= 0 && imageHeight > 0 && imageHeight != srcHeight) {
				// specified desired height only and different from source - preserve pixel aspect ratio
				double scale = ((double)imageHeight)/srcHeight;
if (debugLevel > 1) System.err.println("ConsumerFormatImageMaker.convertFileToEightBitImage(): Resizing - specified desired height only and different from source, scale = "+scale);
				renderedImage = BufferedImageUtilities.resampleWithAffineTransformOp(renderedImage,scale,scale);
			}
			else if (imageHeight <= 0 && imageWidth > 0 && imageWidth != srcWidth) {
				// specified desired width only and different from source - preserve pixel aspect ratio
				double scale = ((double)imageWidth)/srcWidth;
if (debugLevel > 1) System.err.println("ConsumerFormatImageMaker.convertFileToEightBitImage(): Resizing - specified desired width only and different from source, scale = "+scale);
				renderedImage = BufferedImageUtilities.resampleWithAffineTransformOp(renderedImage,scale,scale);
			}
			else if (imageWidth > 0 && imageHeight > 0 && (imageWidth != srcWidth || imageHeight != srcHeight)) {
				// specified both height and width and different from source ... implies possible pixel aspect ratio change
if (debugLevel > 1) System.err.println("ConsumerFormatImageMaker.convertFileToEightBitImage(): Resizing - specified desired width and height and different from source ");
				renderedImage = BufferedImageUtilities.resampleWithAffineTransformOp(renderedImage,imageWidth,imageHeight);
			}
if (debugLevel > 1) System.err.println("ConsumerFormatImageMaker.convertFileToEightBitImage(): Resized width = "+renderedImage.getWidth()+" height = "+renderedImage.getHeight());
		}
		catch (Exception e) {	// such as java.awt.image.ImagingOpException, java.awt.HeadlessException
			e.printStackTrace(System.err);
			// and leave it alone unresized
		}
		
		renderedImage = BufferedImageUtilities.convertToMostFavorableImageType(renderedImage);		// Otherwise will not draw color on grayscale images

		if (annotation != null && (annotation.equals(ALL_ANNOTATIONS) || annotation.equals(ICON_ANNOTATIONS))) {
			Graphics2D g2d = renderedImage.createGraphics();
		
			//g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			//g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			//g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			//g2d.drawImage(windowedImage,0,0,windowedImage.getWidth(),windowedImage.getHeight(),null/*no observer*/);
		
			GeometryOfVolume imageGeometry = null;
			try {
				imageGeometry = new GeometryOfVolumeFromAttributeList(list);
			}
			catch (Exception e) {
				// don't print exception, because it is legitimate for images to be missing this information
				//e.printStackTrace(System.err);
			}
			DemographicAndTechniqueAnnotations demographicAndTechniqueAnnotations =  annotation.equals(ICON_ANNOTATIONS) ? new IconDemographicAndTechniqueAnnotations(list) : new DemographicAndTechniqueAnnotations(list,imageGeometry);
			if (demographicAndTechniqueAnnotations != null) {
				String demographicAndTechniqueFontName = "";
				int demographicAndTechniqueFontStyle = Font.PLAIN;
				int demographicAndTechniqueFontSize = 10;
				Font demographicAndTechniqueFont = new Font(demographicAndTechniqueFontName,demographicAndTechniqueFontStyle,demographicAndTechniqueFontSize);
				Color demographicAndTechniqueColor = Color.pink;
		
				g2d.setColor(demographicAndTechniqueColor);
				g2d.setFont(demographicAndTechniqueFont);

				Iterator i = demographicAndTechniqueAnnotations.iterator(0/*useSrcImageIndex*/);
				while (i.hasNext()) {
					TextAnnotationPositioned.drawPositionedString((TextAnnotationPositioned)i.next(),g2d,renderedImage.getWidth(),renderedImage.getHeight(),5,5);
				}
			}
		}
		
		// Before writing, make it RGB again because codecs might otherwise write extra component for alpha channel
		renderedImage = BufferedImageUtilities.convertToThreeChannelImageTypeIfFour(renderedImage);
		
		//if (!ImageIO.write(renderedImage,outputFormat,new File(outputFileName))) {
		//	throw new DicomException("Cannot find writer for format"+outputFormat);
		//}
if (debugLevel > 1) System.err.println("ConsumerFormatImageMaker.convertFileToEightBitImage(): Attempting to write format = "+outputFormat);
		Iterator writers = ImageIO.getImageWritersByFormatName(outputFormat);
		if (writers != null && writers.hasNext()) {
			ImageWriter writer = (ImageWriter)writers.next();
			if (writer != null) {
				ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(new File(outputFileName));
				writer.setOutput(imageOutputStream);
				ImageWriteParam writeParameters;
				try {
					writeParameters = writer.getDefaultWriteParam();
					if (writeParameters.canWriteCompressed()) {
if (debugLevel > 1) System.err.println("ConsumerFormatImageMaker.convertFileToEightBitImage(): Default compression mode = "+writeParameters.getCompressionMode());
						String[] compressionTypesAvailable = writeParameters.getCompressionTypes();
if (debugLevel > 1) System.err.println("ConsumerFormatImageMaker.convertFileToEightBitImage(): Compression types available = "+Arrays.toString(compressionTypesAvailable));
						//if (compressionTypesAvailable != null && compressionTypesAvailable.length > 0) {
//if (debugLevel > 1) System.err.println("ConsumerFormatImageMaker.convertFileToEightBitImage(): Setting compression type to = "+compressionTypesAvailable[0]);
						//	writeParameters.setCompressionType(compressionTypesAvailable[0]);
						//}
						if (imageQuality >= 0 && imageQuality <= 100) {		// -1 is flag that it was not specified
							float quality = ((float)imageQuality)/100f;
if (debugLevel > 1) System.err.println("ConsumerFormatImageMaker.convertFileToEightBitImage(): Setting quality = "+quality);
							writeParameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
							writeParameters.setCompressionQuality(quality);
						}
					}
					if (writeParameters.canWriteProgressive()) {
if (debugLevel > 1) System.err.println("ConsumerFormatImageMaker.convertFileToEightBitImage(): Setting progressive mode");
						writeParameters.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);
					}
					writer.getDefaultWriteParam();
				}
				catch (Exception e) {
					e.printStackTrace(System.err);
					writeParameters=null;		// Ignore inability to alter parameters
				}
				IIOMetadata metadata = null;
				writer.write(metadata,new IIOImage(renderedImage,null/*no thumbnails*/,metadata),writeParameters);
				imageOutputStream.flush();
				imageOutputStream.close();
				try {
if (debugLevel > 1) System.err.println("ConsumerFormatImageMaker.convertFileToEightBitImage(): Calling dispose() on writer");
					writer.dispose();
				}
				catch (Exception e) {
					e.printStackTrace(System.err);
				}
			}
			else {
				throw new DicomException("Cannot find writer for format"+outputFormat);
			}
		}
		else {
			throw new DicomException("Cannot find writer for format"+outputFormat);
		}
	}
	
	/**
	 * <p>Read a DICOM image input file, and create a single frame 8-bit per channel image (windowed if grayscale) from the first, or only, frame.</p>
	 *
	 * @param	arg	two required parameters, the input file name, output file name, optionally the format (defaults to jpeg), then optionally the window center and width,
	 *			then optionally the desired width and height, then optionally the image quality from 0 to 100,
	 *			then optionally whether or not to include annotation (choice is "all"; anything else means no annotation)
	 *			then optionally the debug level
	 */
	public static void main(String arg[]) {
		String dicomFileName = arg[0];
		String outputFileName = arg[1];
		String outputFormat = arg.length > 2 ? arg[2] : "jpeg";
		double windowCenter = arg.length > 3 ? Double.parseDouble(arg[3]) : 0;
		double windowWidth  = arg.length >  4 ? Double.parseDouble(arg[4]) : 0;
		int      imageWidth = arg.length >  5 ? Integer.parseInt(arg[5]) : 0;
		int     imageHeight = arg.length >  6 ? Integer.parseInt(arg[6]) : 0;
		int    imageQuality = arg.length >  7 ? Integer.parseInt(arg[7]) : -1;
		String   annotation = arg.length >  8 ? arg[8] : ALL_ANNOTATIONS;
		int     debugLevel  = arg.length >  9 ? Integer.parseInt(arg[9]) : -1;
		
		try {
			convertFileToEightBitImage(dicomFileName,outputFileName,outputFormat,windowCenter,windowWidth,imageWidth,imageHeight,imageQuality,annotation,debugLevel);
		}
		catch (Exception e) {
			//System.err.println(e);
			e.printStackTrace(System.err);
		}
	}
}

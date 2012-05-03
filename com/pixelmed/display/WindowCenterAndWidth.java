/* Copyright (c) 2001-2007, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

//import java.io.BufferedInputStream;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.IOException;

import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.ByteLookupTable;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.LookupOp;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;

//import javax.imageio.ImageIO;

//import com.pixelmed.dicom.Attribute;
//import com.pixelmed.dicom.AttributeList;
//import com.pixelmed.dicom.DecimalStringAttribute;
//import com.pixelmed.dicom.DicomException;
//import com.pixelmed.dicom.DicomInputStream;
//import com.pixelmed.dicom.ModalityTransform;
//import com.pixelmed.dicom.SOPClass;
//import com.pixelmed.dicom.TagFromName;
//import com.pixelmed.dicom.VOITransform;

/**
 * <p>A class of static methods to perform window operations on images.</p>
 *
 * @author	dclunie
 */
public class WindowCenterAndWidth {

	/**
	 * @param	lut
	 * @param	pad
	 * @param	padRangeLimit
	 * @param	mask
	 */
	protected static void applyPaddingValueRangeToLUT(byte[] lut,int pad,int padRangeLimit,int mask) {
		//lut[pad&mask]=(byte)0;
		int maskedPadStart = pad&mask;
		int maskedPadEnd = padRangeLimit&mask;
		int incr = maskedPadStart <= maskedPadEnd ? 1 : -1;
		for (int i=maskedPadStart; i != maskedPadEnd; i+=incr) {
//System.err.println("WindowCenterAndWidth.applyPaddingValueRangeToLUT(): LUT index set to zero for pad in range "+i);
			lut[i]=(byte)0;
		}
		lut[maskedPadEnd]=(byte)0;		// since not done in for loop due to bi-directional end condition
//System.err.println("WindowCenterAndWidth.applyPaddingValueRangeToLUT(): LUT index set to zero for pad in range "+maskedPadEnd);
	}
	
	/**
	 * @param	src
	 * @param	center
	 * @param	width
	 * @param	signed
	 * @param	inverted
	 * @param	useSlope
	 * @param	useIntercept
	 * @param	usePad
	 * @param	pad
	 */
	public static final BufferedImage applyWindowCenterAndWidthLogistic(BufferedImage src,double center,double width,
			boolean signed,boolean inverted,double useSlope,double useIntercept,boolean usePad,int pad) {
		return applyWindowCenterAndWidthLogistic(src,center,width,signed,inverted,useSlope,useIntercept,usePad,pad,pad);
	}
	
	/**
	 * @param	src
	 * @param	center
	 * @param	width
	 * @param	signed
	 * @param	inverted
	 * @param	useSlope
	 * @param	useIntercept
	 * @param	usePad
	 * @param	pad
	 * @param	padRangeLimit
	 */
	public static final BufferedImage applyWindowCenterAndWidthLogistic(BufferedImage src,double center,double width,
			boolean signed,boolean inverted,double useSlope,double useIntercept,boolean usePad,int pad,int padRangeLimit) {
//System.err.println("WindowCenterAndWidth.applyWindowCenterAndWidthLogistic(): center="+center+" width="+width);

		int       ymin = 0;
		int       ymax = 255;
				
		int startx;
		int endx;
		byte lut[] = null;
		int mask;
		
//System.err.println("WindowCenterAndWidth.applyWindowCenterAndWidthLogistic(): bottom="+bottom+" top="+top);
		int dataType = src.getSampleModel().getDataType();
//System.err.println("WindowCenterAndWidth.applyWindowCenterAndWidthLogistic(): Data type "+dataType);
		if (dataType == DataBuffer.TYPE_SHORT || dataType == DataBuffer.TYPE_USHORT) {
//System.err.println("WindowCenterAndWidth.applyWindowCenterAndWidthLogistic(): Data type is short or ushort and signed is "+signed);
			startx = signed ? -32768 : 0;
			endx   = signed ?  32768 : 65536;
			lut=new byte[65536];
			mask=0xffff;
		}
		else if (dataType == DataBuffer.TYPE_BYTE) {
//System.err.println("WindowCenterAndWidth.applyWindowCenterAndWidthLogistic(): Data type is byte and signed is "+signed);
			startx = signed ? -128 : 0;
			endx   = signed ?  128 : 256;
			lut=new byte[256];
			mask=0xff;
		}
		else {
			throw new IllegalArgumentException();
		}
		
		//double a = ymax;
		//double b = 1/width;
		//double c = center < 0 ? 0 : center;
		//for (int xi=startx; xi<endx; ++xi) {
		//	double x = xi*useSlope+useIntercept;
		//	double y = a / (1 + c * Math.exp(-b*x)) + 0.5;
		//	if (y < ymin)  y=ymin;
		//	else if (y > ymax) y=ymax;
		//	if (inverted) y=(byte)(ymax-y);
		//	lut[xi&mask]=(byte)y;
		//}
		
		int yrange = ymax - ymin;
		
		for (int xi=startx; xi<endx; ++xi) {
			double x = xi*useSlope+useIntercept;
			double y = yrange / (1 + Math.exp(-4*(x - center)/width)) + ymin + 0.5;
			if (y < ymin)  y=ymin;
			else if (y > ymax) y=ymax;
			if (inverted) y=(byte)(ymax-y);
//if (xi%16 == 0) System.err.println(xi+"\t"+(((int)y)&0xff));
			lut[xi&mask]=(byte)y;
		}
		if (usePad) {
			applyPaddingValueRangeToLUT(lut,pad,padRangeLimit,mask);
		}
		LookupOp lookup=new LookupOp(new ByteLookupTable(0,lut), null);
		ColorModel dstColorModel=new ComponentColorModel(
			ColorSpace.getInstance(ColorSpace.CS_GRAY),
			new int[] {8},
			false,		// has alpha
			false,		// alpha premultipled
			Transparency.OPAQUE,
			DataBuffer.TYPE_BYTE
		);
		BufferedImage dst = lookup.filter(src,lookup.createCompatibleDestImage(src,dstColorModel));	// Fails if src is DataBufferShort
		return dst;
	}

	/**
	 * @param	src
	 * @param	center
	 * @param	width
	 * @param	signed
	 * @param	inverted
	 * @param	useSlope
	 * @param	useIntercept
	 * @param	usePad
	 * @param	pad
	 */
	public static final BufferedImage applyWindowCenterAndWidthLinear(BufferedImage src,double center,double width,
			boolean signed,boolean inverted,double useSlope,double useIntercept,boolean usePad,int pad) {
		return applyWindowCenterAndWidthLinear(src,center,width,signed,inverted,useSlope,useIntercept,usePad,pad,pad);
	}
	
	/**
	 * @param	src
	 * @param	center
	 * @param	width
	 * @param	signed
	 * @param	inverted
	 * @param	useSlope
	 * @param	useIntercept
	 * @param	usePad
	 * @param	pad
	 * @param	padRangeLimit
	 */
	public static final BufferedImage applyWindowCenterAndWidthLinear(BufferedImage src,double center,double width,
			boolean signed,boolean inverted,double useSlope,double useIntercept,boolean usePad,int pad,int padRangeLimit) {
//System.err.println("WindowCenterAndWidth.applyWindowCenterAndWidthLinear(): center="+center+" width="+width);

		int       ymin = 0;
		int       ymax = 255;

		byte     bymin = (byte)ymin;
		byte     bymax = (byte)ymax;
		double  yrange = ymax - ymin;

		double    cmp5 = center - 0.5;
		double     wm1 = width - 1.0;
		double halfwm1 = wm1/2.0;
		double  bottom = cmp5 - halfwm1;
		double     top = cmp5 + halfwm1;
		
		int startx;
		int endx;
		byte lut[] = null;
		int mask;
		
//System.err.println("WindowCenterAndWidth.applyWindowCenterAndWidthLinear(): bottom="+bottom+" top="+top);
		int dataType = src.getSampleModel().getDataType();
//System.err.println("WindowCenterAndWidth.applyWindowCenterAndWidthLinear(): Data type "+dataType);
		if (dataType == DataBuffer.TYPE_SHORT || dataType == DataBuffer.TYPE_USHORT) {
//System.err.println("WindowCenterAndWidth.applyWindowCenterAndWidthLinear(): Data type is short or ushort and signed is "+signed);
			startx = signed ? -32768 : 0;
			endx   = signed ?  32768 : 65536;
			lut=new byte[65536];
			mask=0xffff;
		}
		else if (dataType == DataBuffer.TYPE_BYTE) {
//System.err.println("WindowCenterAndWidth.applyWindowCenterAndWidthLinear(): Data type is byte and signed is "+signed);
			startx = signed ? -128 : 0;
			endx   = signed ?  128 : 256;
			lut=new byte[256];
			mask=0xff;
		}
		else {
			throw new IllegalArgumentException();
		}
		
		for (int xi=startx; xi<endx; ++xi) {
			double x = xi*useSlope+useIntercept;
			byte y;
			if (x <= bottom)  y=bymin;
			else if (x > top) y=bymax;
			else {
				y = (byte)(((x-cmp5)/wm1 + 0.5)*yrange+ymin);
//System.err.println("xi&0xffff="+(xi&0xffff)+" y="+((int)y&0xff)+" x="+x);
			}
			
			if (inverted) y=(byte)(ymax-y);
//if (xi%16 == 0) System.err.println(xi+"\t"+(((int)y)&0xff));
			lut[xi&mask]=y;
		}
		if (usePad) {
			applyPaddingValueRangeToLUT(lut,pad,padRangeLimit,mask);
		}
		LookupOp lookup=new LookupOp(new ByteLookupTable(0,lut), null);
		ColorModel dstColorModel=new ComponentColorModel(
			ColorSpace.getInstance(ColorSpace.CS_GRAY),
			new int[] {8},
			false,		// has alpha
			false,		// alpha premultipled
			Transparency.OPAQUE,
			DataBuffer.TYPE_BYTE
		);
		BufferedImage dst = lookup.filter(src,lookup.createCompatibleDestImage(src,dstColorModel));	// Fails if src is DataBufferShort
//System.err.println("WindowCenterAndWidth.applyWindowCenterAndWidthLinear(): BufferedImage out of LookupOp"+dst);
		return dst;
	}

	/**
	 * @param	src
	 * @param	center
	 * @param	width
	 * @param	signed
	 * @param	inverted
	 * @param	useSlope
	 * @param	useIntercept
	 * @param	usePad
	 * @param	pad
	 * @param	largestGray
	 * @param	bitsPerEntry
	 * @param	numberOfEntries
	 * @param	redTable
	 * @param	greenTable
	 * @param	blueTable
	 */
	public static final BufferedImage applyWindowCenterAndWidthWithPaletteColor(BufferedImage src,double center,double width,
			boolean signed,boolean inverted,double useSlope,double useIntercept,
			boolean usePad,int pad,
			int largestGray,int bitsPerEntry,int numberOfEntries,
			short[] redTable,short[] greenTable,short[] blueTable) {
		return applyWindowCenterAndWidthWithPaletteColor(src,center,width,signed,inverted,useSlope,useIntercept,usePad,pad,pad,
			largestGray,bitsPerEntry,numberOfEntries,redTable,greenTable,blueTable);
	}

	/**
	 * @param	src
	 * @param	center
	 * @param	width
	 * @param	signed
	 * @param	inverted
	 * @param	useSlope
	 * @param	useIntercept
	 * @param	usePad
	 * @param	pad
	 * @param	padRangeLimit
	 * @param	largestGray
	 * @param	bitsPerEntry
	 * @param	numberOfEntries
	 * @param	redTable
	 * @param	greenTable
	 * @param	blueTable
	 */
	public static final BufferedImage applyWindowCenterAndWidthWithPaletteColor(BufferedImage src,double center,double width,
			boolean signed,boolean inverted,double useSlope,double useIntercept,
			boolean usePad,int pad,int padRangeLimit,
			int largestGray,int bitsPerEntry,int numberOfEntries,
			short[] redTable,short[] greenTable,short[] blueTable) {
//System.err.println("WindowCenterAndWidth.applyWindowCenterAndWidthWithPaletteColor center="+center+" width="+width);

		int       ymin = 0;
		int       ymax = 255;

		byte     bymin = (byte)ymin;
		byte     bymax = (byte)ymax;
		double  yrange = ymax - ymin;

		double    cmp5 = center - 0.5;
		double     wm1 = width - 1.0;
		double halfwm1 = wm1/2.0;
		double  bottom = cmp5 - halfwm1;
		double     top = cmp5 + halfwm1;
		
		//double gamma = 2.2;

		int startx = signed ? -32768 : 0;		// hmmm ....
		int endx   = signed ?  32768 : 65536;
		//int endx   = largestGray;
		byte rlut[]=new byte[65536];
		byte glut[]=new byte[65536];
		byte blut[]=new byte[65536];
		for (int xi=startx; xi<endx; ++xi) {
			double x = xi*useSlope+useIntercept;
			byte y;
			if (x <= bottom)  y=bymin;
			else if (x > top) y=bymax;
			else {
				y = (byte)(((x-cmp5)/wm1 + 0.5)*yrange+ymin);
//System.err.println("xi&0xffff="+(xi&0xffff)+" y="+((int)y&0xff)+" x="+x);
			}
			
			if (inverted) y=(byte)(ymax-y);

			rlut[xi&0xffff]=y;
			glut[xi&0xffff]=y;
			blut[xi&0xffff]=y;
		}
//System.err.println("WindowCenterAndWidth.applyWindowCenterAndWidthWithPaletteColor(): numberOfEntries = "+numberOfEntries);
//System.err.println("WindowCenterAndWidth.applyWindowCenterAndWidthWithPaletteColor(): redTable.length = "+redTable.length);
//System.err.println("WindowCenterAndWidth.applyWindowCenterAndWidthWithPaletteColor(): greenTable.length = "+greenTable.length);
//System.err.println("WindowCenterAndWidth.applyWindowCenterAndWidthWithPaletteColor(): blueTable.length = "+blueTable.length);
//System.err.println("WindowCenterAndWidth.applyWindowCenterAndWidthWithPaletteColor(): largestGray="+largestGray);

		int shiftRight = bitsPerEntry-8;
		int i;
		int xi;
		for (xi=largestGray+1,i=0; i < numberOfEntries; ++xi,++i) {
//System.err.println("WindowCenterAndWidth.applyWindowCenterAndWidthWithPaletteColor: xi="+xi+" i="+i);
//System.err.println("WindowCenterAndWidth.applyWindowCenterAndWidthWithPaletteColor: redTable[i]="+redTable[i]);
//System.err.println("WindowCenterAndWidth.applyWindowCenterAndWidthWithPaletteColor: redTable[i]>>shiftRight="+(redTable[i]>>shiftRight));
//System.err.println("WindowCenterAndWidth.applyWindowCenterAndWidthWithPaletteColor: redTable[i]>>shiftRight="+Integer.toHexString((redTable[i]>>shiftRight)&0xff));
			rlut[xi&0xffff]=(byte)(redTable[i]>>shiftRight);
			glut[xi&0xffff]=(byte)(greenTable[i]>>shiftRight);
			blut[xi&0xffff]=(byte)(blueTable[i]>>shiftRight);
		}
		
		if (usePad) {
			applyPaddingValueRangeToLUT(rlut,pad,padRangeLimit,0xffff);
			applyPaddingValueRangeToLUT(glut,pad,padRangeLimit,0xffff);
			applyPaddingValueRangeToLUT(blut,pad,padRangeLimit,0xffff);
		}

		int columns = src.getWidth();
		int rows = src.getHeight();
        
		SampleModel srcSampleModel = src.getSampleModel();
		WritableRaster srcRaster = src.getRaster();
		DataBuffer srcDataBuffer = srcRaster.getDataBuffer();
		int srcNumBands = srcRaster.getNumBands();

		//ColorModel dstColorModel = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getColorModel();
		ColorModel dstColorModel = BufferedImageUtilities.getMostFavorableColorModel();
		if (dstColorModel == null) {
			// This color model is what we use in SourceImage when reading RGB images
			dstColorModel = new ComponentColorModel(
				ColorSpace.getInstance(ColorSpace.CS_sRGB),
				new int[] {8,8,8},
				false,		// has alpha
				false,		// alpha premultipled
				Transparency.OPAQUE,
				DataBuffer.TYPE_BYTE
			);
		}
		
		WritableRaster dstRaster = dstColorModel.createCompatibleWritableRaster(columns,rows);
		DataBuffer dstDataBuffer = dstRaster.getDataBuffer();
		BufferedImage dst = new BufferedImage(dstColorModel, dstRaster, dstColorModel.isAlphaPremultiplied(), null);
		SampleModel dstSampleModel = dst.getSampleModel();
		int dstNumBands = dstRaster.getNumBands();
        
		int srcPixels[] = null; // to disambiguate SampleModel.getPixels() method signature
		srcPixels = srcSampleModel.getPixels(0,0,columns,rows,srcPixels,srcDataBuffer);
		int srcPixelsLength = srcPixels.length;

		int dstPixels[] = null; // to disambiguate SampleModel.getPixels() method signature
		dstPixels = dstSampleModel.getPixels(0,0,columns,rows,dstPixels,dstDataBuffer);
		int dstPixelsLength = dstPixels.length;
        
//System.err.println("WindowCenterAndWidth.applyWindowCenterAndWidthWithPaletteColor: dstNumBands = "+dstNumBands);
		if (srcNumBands == 1 && dstNumBands == 4 && srcPixelsLength*4 == dstPixelsLength) {
//System.err.println("WindowCenterAndWidth.applyWindowCenterAndWidthWithPaletteColor: converting gray to RGBA");
			int dstIndex=0;
			for (int srcIndex=0; srcIndex<srcPixelsLength; ++srcIndex) {
				dstPixels[dstIndex++]=rlut[srcPixels[srcIndex]];
				dstPixels[dstIndex++]=glut[srcPixels[srcIndex]];
				dstPixels[dstIndex++]=blut[srcPixels[srcIndex]];
				dstPixels[dstIndex++]=-1;
			}
			dstSampleModel.setPixels(0,0,columns,rows,dstPixels,dstDataBuffer);
		}
		else if (srcNumBands == 1 && dstNumBands == 3 && srcPixelsLength*3 == dstPixelsLength) {
//System.err.println("WindowCenterAndWidth.applyWindowCenterAndWidthWithPaletteColor: converting gray to RGB");
			int dstIndex=0;
			for (int srcIndex=0; srcIndex<srcPixelsLength; ++srcIndex) {
				dstPixels[dstIndex++]=rlut[srcPixels[srcIndex]];
				dstPixels[dstIndex++]=glut[srcPixels[srcIndex]];
				dstPixels[dstIndex++]=blut[srcPixels[srcIndex]];
			}
			dstSampleModel.setPixels(0,0,columns,rows,dstPixels,dstDataBuffer);
		}

		return dst;
	}
	
	/**
	 * @param	src
	 * @param	center
	 * @param	width
	 * @param	identityCenter
	 * @param	identityWidth
	 * @param	signed
	 * @param	inverted
	 * @param	useSlope
	 * @param	useIntercept
	 * @param	usePad
	 * @param	pad
	 * @param	padRangeLimit
	 * @param	numberOfEntries
	 * @param	bitsPerEntry
	 * @param	grayTable
	 * @param	entryMin
	 * @param	entryMax
	 * @param	topOfEntryRange
	 */
	public static final BufferedImage applyVOILUT(BufferedImage src,double center,double width,double identityCenter,double identityWidth,
			boolean signed,boolean inverted,double useSlope,double useIntercept,
			boolean usePad,int pad,int padRangeLimit,
			int numberOfEntries,int firstValueMapped,int bitsPerEntry,short[] grayTable,int entryMin,int entryMax,int topOfEntryRange) {

//System.err.println("WindowCenterAndWidth.applyVOILUT(): firstValueMapped="+firstValueMapped);
//System.err.println("WindowCenterAndWidth.applyVOILUT center="+center+" width="+width);

		int       ymin = 0;
		int       ymax = 255;
		double  yrange = ymax - ymin;

		int bottomOfEntryRange = 0;
		double entryRange = topOfEntryRange - bottomOfEntryRange;
//System.err.println("WindowCenterAndWidth.applyVOILUT(): bottomOfEntryRange="+bottomOfEntryRange+" topOfEntryRange="+topOfEntryRange+" entryRange="+entryRange);

		int firstLUTValue = grayTable[0] & 0xffff;
		int lastLUTValue  = grayTable[numberOfEntries-1] & 0xffff;
//System.err.println("WindowCenterAndWidth.applyVOILUT(): firstLUTValue="+firstLUTValue+" lastLUTValue="+lastLUTValue);

		byte     bymin = (byte)(((firstLUTValue-bottomOfEntryRange)/entryRange)*yrange+ymin);
		byte     bymax = (byte)(((lastLUTValue -bottomOfEntryRange)/entryRange)*yrange+ymin);
//System.err.println("WindowCenterAndWidth.applyVOILUT(): bymin="+(bymin&0xff)+" bymax="+(bymax&0xff));

		int startx;
		int endx;
		byte lut[] = null;
		int mask;
		
		int dataType = src.getSampleModel().getDataType();
//System.err.println("WindowCenterAndWidth.applyVOILUT(): Data type "+dataType);
		if (dataType == DataBuffer.TYPE_SHORT || dataType == DataBuffer.TYPE_USHORT) {
//System.err.println("WindowCenterAndWidth.applyVOILUT(): Data type is short or ushort and signed is "+signed);
			startx = signed ? -32768 : 0;
			endx   = signed ?  32768 : 65536;
			lut=new byte[65536];
			mask=0xffff;
		}
		else if (dataType == DataBuffer.TYPE_BYTE) {
//System.err.println("WindowCenterAndWidth.applyVOILUT(): Data type is byte and signed is "+signed);
			startx = signed ? -128 : 0;
			endx   = signed ?  128 : 256;
			lut=new byte[256];
			mask=0xff;
		}
		else {
			throw new IllegalArgumentException();
		}

		for (int xi=startx; xi<endx; ++xi) {
			double x = xi*useSlope+useIntercept;
			byte y;
			//int lutIndex = (int)(x-firstValueMapped);		// warning: there could be sign issues here related to the nominal "VR" of the second LUT Descriptor entry - see PS 3.3 C.11.2.1.1 :(
			//int lutIndex = (int)(((x-center)/width) * numberOfEntries + numberOfEntries/2);
			int lutIndex = (int)(((x-center)/width) * identityWidth + identityCenter - firstValueMapped);
//System.err.println("xi&0xffff="+(xi&0xffff)+" x="+x+" lutIndex="+lutIndex);
			if (lutIndex < 0) {
				y=bymin;
			}
			else if (lutIndex > (numberOfEntries-1)) {
				y=bymax;
			}
			else {
					y = (byte)(((grayTable[lutIndex] & 0xffff)/entryRange)*yrange+ymin);
//System.err.println("xi&0xffff="+(xi&0xffff)+" y="+((int)y&0xff)+" x="+x+" lutIndex="+lutIndex);
			}
			
			if (inverted) y=(byte)(ymax-y);
//if (xi%16 == 0) System.err.println(xi+"\t"+(((int)y)&0xff));
			lut[xi&mask]=y;
		}

		if (usePad) {
			applyPaddingValueRangeToLUT(lut,pad,padRangeLimit,mask);
		}
		LookupOp lookup=new LookupOp(new ByteLookupTable(0,lut), null);
		ColorModel dstColorModel=new ComponentColorModel(
			ColorSpace.getInstance(ColorSpace.CS_GRAY),
			new int[] {8},
			false,		// has alpha
			false,		// alpha premultipled
			Transparency.OPAQUE,
			DataBuffer.TYPE_BYTE
		);
		BufferedImage dst = lookup.filter(src,lookup.createCompatibleDestImage(src,dstColorModel));	// Fails if src is DataBufferShort
//System.err.println("WindowCenterAndWidth.applyVOILUT(): BufferedImage out of LookupOp"+dst);
		return dst;
	}
						
}

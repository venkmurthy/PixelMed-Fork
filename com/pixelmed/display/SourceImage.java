/* Copyright (c) 2001-2011, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import java.awt.*; 
import java.awt.event.*; 
import java.awt.image.*; 
import java.awt.color.*; 
import java.util.*; 
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.ShortBuffer;
import javax.swing.*; 
import javax.swing.event.*;

import com.pixelmed.dicom.*;

/**
 * <p>A class that encapsulates the pixel data and features and values from an image source
 * (such as a DICOM image), usually for the purpose of displaying it.</p>
 *
 * @see com.pixelmed.display.SingleImagePanel
 *
 * @author	dclunie
 */
public class SourceImage {

	/***/
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/SourceImage.java,v 1.62 2011/08/31 18:45:03 dclunie Exp $";

	/***/
	BufferedImage[] imgs;
	/***/
	int width;
	/***/
	int height;
	/***/
	int nframes;
	/***/
	int imgMin;
	/***/
	int imgMax;
	/***/
	double imgMean;
	/***/
	double imgStandardDeviation;
	/***/
	boolean signed;
	/***/
	boolean inverted;
	/***/
	int mask;
	/***/
	boolean isGrayscale;
	/***/
	boolean isPaletteColor;
	/***/
	boolean isYBR;
	/***/
	int pad;
	/***/
	int padRangeLimit;
	/***/
	boolean hasPad;
	/***/
	boolean useMaskedPadRange;
	/***/
	int useMaskedPadRangeStart;
	/***/
	int useMaskedPadRangeEnd;
	/***/
	boolean useNonMaskedSinglePadValue;
	/***/
	int nonMaskedSinglePadValue;
	/***/
	int backgroundValue;
	/***/
	String title;

	// stuff for (supplemental) palette color LUT
	/***/
	private int largestGray;
	/***/
	private int firstValueMapped;
	/***/
	private int numberOfEntries;
	/***/
	private int bitsPerEntry;
	/***/
	private short redTable[];
	/***/
	private short greenTable[];
	/***/
	private short blueTable[];
	
	/***/
	private SUVTransform suvTransform;
	
	/***/
	private RealWorldValueTransform realWorldValueTransform;

	/***/
	private ModalityTransform modalityTransform;

	/***/
	private VOITransform voiTransform;

	/***/
	private DisplayShutter displayShutter;

	/***/
	private Overlay overlay;

	/**
	 * @param	w
	 * @param	h
	 * @param	data
	 * @param	offset
	 */
	private static BufferedImage createUnsignedShortGrayscaleImage(int w,int h,short data[],int offset) {
//System.err.println("createUnsignedShortGrayscaleImage");
		ComponentColorModel cm=new ComponentColorModel(
			ColorSpace.getInstance(ColorSpace.CS_GRAY),
			new int[] {16},
			false,		// has alpha
			false,		// alpha premultipled
			Transparency.OPAQUE,
			DataBuffer.TYPE_USHORT
		);

		ComponentSampleModel sm = new ComponentSampleModel(
			DataBuffer.TYPE_USHORT,
			w,
			h,
			1,
			w,
			new int[] {0}
		);

		DataBuffer buf = new DataBufferUShort(data,w,offset);

		WritableRaster wr = Raster.createWritableRaster(sm,buf,new Point(0,0));

		return new BufferedImage(cm,wr,true,null);	// no properties hash table
	}

	/**
	 * @param	w
	 * @param	h
	 * @param	data
	 * @param	offset
	 */
	private static BufferedImage createSignedShortGrayscaleImage(int w,int h,short data[],int offset) {
//System.err.println("createSignedShortGrayscaleImage");
		// DataBufferUShort and DataBuffer.TYPE_USHORT are used here,
		// otherwise lookup table operations for windowing fail;
		// concept of signedness is conveyed separately;
		// causes issues with JAI operations expecting signed shorts
		ComponentColorModel cm=new ComponentColorModel(
			ColorSpace.getInstance(ColorSpace.CS_GRAY),
			new int[] {16},
			false,		// has alpha
			false,		// alpha premultipled
			Transparency.OPAQUE,
			/*DataBuffer.TYPE_SHORT*/DataBuffer.TYPE_USHORT
		);

		ComponentSampleModel sm = new ComponentSampleModel(
			/*DataBuffer.TYPE_SHORT*/DataBuffer.TYPE_USHORT,
			w,
			h,
			1,
			w,
			new int[] {0}
		);

		DataBuffer buf = new /*DataBufferShort*/DataBufferUShort(data,w,offset);

		WritableRaster wr = Raster.createWritableRaster(sm,buf,new Point(0,0));

		return new BufferedImage(cm,wr,true,null);	// no properties hash table
	}

	/**
	 * @param	w
	 * @param	h
	 * @param	data
	 * @param	offset
	 */
	private static BufferedImage createByteGrayscaleImage(int w,int h,byte data[],int offset) {
//System.err.println("createByteGrayscaleImage");
		ComponentColorModel cm=new ComponentColorModel(
			ColorSpace.getInstance(ColorSpace.CS_GRAY),
			new int[] {8},
			false,		// has alpha
			false,		// alpha premultipled
			Transparency.OPAQUE,
			DataBuffer.TYPE_BYTE
		);

		ComponentSampleModel sm = new ComponentSampleModel(
			DataBuffer.TYPE_BYTE,
			w,
			h,
			1,
			w,
			new int[] {0}
		);

		DataBuffer buf = new DataBufferByte(data,w,offset);
                
		WritableRaster wr = Raster.createWritableRaster(sm,buf,new Point(0,0));
                
                return new BufferedImage(cm,wr,true,null);	// no properties hash table
	}

	/**
	 * @param	w
	 * @param	h
	 * @param	data
	 * @param	offset
	 */
	private static BufferedImage createPixelInterleavedByteRGBImage(int w,int h,byte data[],int offset) {
//System.err.println("createPixelInterleavedByteRGBImage");
		ComponentColorModel cm=new ComponentColorModel(
			ColorSpace.getInstance(ColorSpace.CS_sRGB),
			new int[] {8,8,8},
			false,		// has alpha
			false,		// alpha premultipled
			Transparency.OPAQUE,
			DataBuffer.TYPE_BYTE
		);

		ComponentSampleModel sm = new ComponentSampleModel(
			DataBuffer.TYPE_BYTE,
			w,
			h,
			3,
			w*3,
			new int[] {0,1,2}
		);
		//PixelInterleavedSampleModel sm = new PixelInterleavedSampleModel(
		//	DataBuffer.TYPE_BYTE,
		//	w,
		//	h,
		//	3,
		//	w*3,
		//	new int[] {0,1,2}
		//);

		DataBuffer buf = new DataBufferByte(data,w,offset);

		WritableRaster wr = Raster.createWritableRaster(sm,buf,new Point(0,0));

		return new BufferedImage(cm,wr,true,null);	// no properties hash table
	}

	/**
	 * @param	w
	 * @param	h
	 * @param	data
	 * @param	offset
	 */
	private static BufferedImage createBandInterleavedByteRGBImage(int w,int h,byte data[],int offset) {
//System.err.println("createBandInterleavedByteRGBImage");
		ComponentColorModel cm=new ComponentColorModel(
			ColorSpace.getInstance(ColorSpace.CS_sRGB),
			new int[] {8,8,8},
			false,		// has alpha
			false,		// alpha premultipled
			Transparency.OPAQUE,
			DataBuffer.TYPE_BYTE
		);

		ComponentSampleModel sm = new ComponentSampleModel(
			DataBuffer.TYPE_BYTE,
			w,
			h,
			1,
			w,
			new int[] {0,w*h,w*h*2}
		);
		//BandedSampleModel sm = new BandedSampleModel(		// doesn't work
		//	DataBuffer.TYPE_BYTE,
		//	w,
		//	h,
		//	w,
		//	new int[] {0,1,2},				// what should this be ?
		//	new int[] {0,w*h,w*h*2}
		//);

		DataBuffer buf = new DataBufferByte(data,w,offset);

		WritableRaster wr = Raster.createWritableRaster(sm,buf,new Point(0,0));

		return new BufferedImage(cm,wr,true,null);	// no properties hash table
	}

	/**
	 * <p>Construct a single-frame image from an unsigned raw image (no header) on
	 * an input stream with the specified width, height and bit depth.</p>
	 *
	 * @param	i		the input stream
	 * @param	w		image width
	 * @param	h		image height
	 * @param	depth		bit depth
	 * @exception	IOException
	 */
	public SourceImage(InputStream i,int w,int h,int depth) throws IOException {
		constructSourceImage(i,w,h,depth,1);
	}

	/**
	 * <p>Construct a multi-frame image from an unsigned raw image (no header) on
	 * an input stream with the specified width, height and bit depth.</p>
	 *
	 * @param	i		the input stream
	 * @param	w		image width
	 * @param	h		image height
	 * @param	depth		bit depth
	 * @param	frames		number of frames
	 * @exception	IOException
	 */
	public SourceImage(InputStream i,int w,int h,int depth,int frames) throws IOException {
		constructSourceImage(i,w,h,depth,frames);
	}

	/**
	 * @param	i
	 * @param	w
	 * @param	h
	 * @param	depth
	 * @param	frames
	 * @exception	IOException
	 */
	private void constructSourceImage(InputStream i,int w,int h,int depth,int frames) throws IOException {

		width = w;
		height = h;
		nframes=frames;
		if (nframes < 1) return;

		imgs=new BufferedImage[nframes];

		imgMin=65536;
		imgMax=0;

		for (int frame=0; frame < nframes; ++frame) {
			if (depth > 8) {
				short data[] = new short[width*height];
				byte buffer[] = new byte[width*height*2];
				i.read(buffer,0,width*height*2);
				int count=0;
				for (int row=0; row<height; ++row) {
					for (int col=0; col<width; ++col) {
						//int byte1=i.read();
						//int byte2=i.read();
						//if (byte1 == -1 || byte2 == -1) throw new EOFException();
						int byte1=((int)buffer[count++])&0xff;
						int byte2=((int)buffer[count++])&0xff;
						short value=(short)((byte2<<8) + byte1);
						data[row*w+col]=(short)value;
						//if (value > imgMax) imgMax=value;
						if (value > imgMax && value <= largestGray) imgMax=value;
						if (value < imgMin) imgMin=value;
					}
				}
				imgs[frame]=createUnsignedShortGrayscaleImage(width,height,data,0);
			}
			else {
				byte data[] = new byte[width*height];
				i.read(data,0,width*height);
				int count=0;
				for (int row=0; row<height; ++row) {
					for (int col=0; col<width; ++col) {
						//int value=i.read();
						//if (value == -1) throw new EOFException();
						//data[row*w+col]=(byte)value;
						int value=((int)data[count++])&0xff;
						//if (value > imgMax) imgMax=value;
						if (value > imgMax && value <= largestGray) imgMax=value;
						if (value < imgMin) imgMin=value;
					}
				}
				imgs[frame]=createByteGrayscaleImage(width,height,data,0);
			}
		}
	}

	/**
	 * <p>Construct an image from a single or multi-frame DICOM image from
	 * a file.</p>
	 *
	 * @param		filename
	 * @exception	IOException
	 * @exception	DicomException
	 */
	public SourceImage(String filename) throws IOException, DicomException {
		AttributeList list = new AttributeList();
		list.read(filename);
//System.err.println(list);

		if (list.get(TagFromName.PixelData) != null) constructSourceImage(list);
	}

	/**
	 * <p>Construct an image from a single or multi-frame DICOM image from
	 * an input stream (such as from a file or the network).</p>
	 *
	 * @param	i		the input stream
	 * @exception	IOException
	 * @exception	DicomException
	 */
	public SourceImage(DicomInputStream i) throws IOException, DicomException {
		AttributeList list = new AttributeList();
		list.read(i);
//System.err.println(list);

		if (list.get(TagFromName.PixelData) != null) constructSourceImage(list);
	}

	/**
	 * <p>Construct an image from a single or multi-frame DICOM image from
	 * a list of DICOM attributes.</p>
	 *
	 * @param	list		the list of attributes that include the description and values of the pixel data
	 * @exception	DicomException
	 */
	public SourceImage(AttributeList list) throws DicomException {
		if (list.get(TagFromName.PixelData) != null) constructSourceImage(list);
	}
	
	private abstract class BufferedImageSource {
		protected int nframesamples;
		private int cachedIndex;
		BufferedImage cachedBufferedImage;

		BufferedImageSource(int nframesamples) {
			this.nframesamples=nframesamples;
			cachedIndex=-1;
			cachedBufferedImage = null;
		}
		
		protected void finalize() throws Throwable {
//System.err.println("BufferedImageSource.finalize()");
			cachedBufferedImage=null;
			super.finalize();
		}
		
		public BufferedImage getBufferedImage(int index) {
			if (index != cachedIndex) {
				cachedBufferedImage = getUncachedBufferedImage(index);
				if (cachedBufferedImage != null) {
					cachedIndex = index;
				}
				else {
					cachedIndex=-1;
				}
			}
			return cachedBufferedImage;
		}

		abstract public BufferedImage getUncachedBufferedImage(int index);

		public int getMinimumPixelValueOfMostRecentBufferedImage(int oldMin) { return oldMin; }
		public int getMaximumPixelValueOfMostRecentBufferedImage(int oldMax) { return oldMax; }
	}
	
	private abstract class ShortBufferedImageSource extends BufferedImageSource {
		protected short data[];
		protected ShortBuffer buffer;
		protected boolean minMaxSet;
		protected int imgMin;
		protected int imgMax;

		public int getMinimumPixelValueOfMostRecentBufferedImage(int oldMin) { return minMaxSet ? imgMin : oldMin; }
		public int getMaximumPixelValueOfMostRecentBufferedImage(int oldMax) { return minMaxSet ? imgMax : oldMax; }

		ShortBufferedImageSource(short data[],int width,int height,int samples) {
			super(width*height*samples);
			this.data=data;
			minMaxSet=false;
		}

		ShortBufferedImageSource(ShortBuffer buffer,int width,int height,int samples) {
			super(width*height*samples);
			this.buffer=buffer;
			minMaxSet=false;
		}
		
		protected void finalize() throws Throwable {
//System.err.println("ShortBufferedImageSource.finalize()");
			data=null;
			buffer=null;
			super.finalize();
		}
	}
	
	private abstract class ByteBufferedImageSource extends BufferedImageSource {
		protected byte data[];

		ByteBufferedImageSource(byte data[],int width,int height,int samples) {
			super(width*height*samples);
			this.data=data;
		}
	}
	
	private class SignedShortGrayscaleBufferedImageSource extends ShortBufferedImageSource {
		protected int mask;
		protected int signbit;
		protected int extend;
		protected int largestGray;

		SignedShortGrayscaleBufferedImageSource(short data[],int width,int height,int mask,int signbit,int extend,int largestGray) {
			super(data,width,height,1);
			this.mask=mask;
			this.signbit=signbit;
			this.extend=extend;
			this.largestGray=largestGray;
			imgMin = 0x00007fff;		// i.e. start with the largest  possible 16 bit +ve value, sign extended to the full Java int 32 bits
			imgMax = 0xffff8000;		// i.e. start with the smallest possible 16 bit -ve value, sign extended to the full Java int 32 bits
		}
		
		SignedShortGrayscaleBufferedImageSource(ShortBuffer buffer,int width,int height,int mask,int signbit,int extend,int largestGray) {
			super(buffer,width,height,1);
			this.mask=mask;
			this.signbit=signbit;
			this.extend=extend;
			this.largestGray=largestGray;
			imgMin = 0x00007fff;		// i.e. start with the largest  possible 16 bit +ve value, sign extended to the full Java int 32 bits
			imgMax = 0xffff8000;		// i.e. start with the smallest possible 16 bit -ve value, sign extended to the full Java int 32 bits
		}
		
		public BufferedImage getUncachedBufferedImage(int index) {
//System.err.println("SourceImage.SignedShortGrayscaleBufferedImageSource.getUncachedBufferedImage(): index="+index);
			short[] useData;
			int useOffset;
			if (data == null) {
//System.err.println("SourceImage.SignedShortGrayscaleBufferedImageSource.getUncachedBufferedImage(): nframesamples = "+nframesamples);
				useData = new short[nframesamples];
				int position = nframesamples*index;
//System.err.println("SourceImage.SignedShortGrayscaleBufferedImageSource.getUncachedBufferedImage(): position = "+position);
				buffer.position(position);
				buffer.get(useData);
				useOffset = 0;
			}
			else {
				useData = data;
				useOffset = nframesamples*index;
			}
			// now copy the data for just one frame, masking and sign extending it
			short[] newData = new short[nframesamples];
			for (int i=useOffset,j=0; j<nframesamples; ++i,++j) {
				boolean isPaddingValue = false;
				short unmaskedValue=useData[i];
				if (useNonMaskedSinglePadValue && unmaskedValue == ((short)nonMaskedSinglePadValue)) {
					isPaddingValue = true;
				}
				int value=((int)unmaskedValue)&mask;
				int nonextendedvalue = value;
				if ((value & signbit) != 0) value=value | extend;
				newData[j]=(short)value;
				if (useMaskedPadRange && (nonextendedvalue >= useMaskedPadRangeStart && nonextendedvalue <= useMaskedPadRangeEnd)) {
					isPaddingValue = true;
				}
				if (!isPaddingValue) {
					if (value > imgMax && value <= largestGray) {
//System.err.println("SourceImage.SignedShortGrayscaleBufferedImageSource.getUncachedBufferedImage(): imgMax was = "+imgMax+" now setting to "+value);
						imgMax=value;
					}
					if (value < imgMin) {
//System.err.println("SourceImage.SignedShortGrayscaleBufferedImageSource.getUncachedBufferedImage(): imgMin was = "+imgMin+" now setting to "+value);
						imgMin=value;
					}
					//imgSum+=value;
					//imgSumOfSquares+=value*value;
				}
			}
			minMaxSet=true;
			return createSignedShortGrayscaleImage(width,height,newData,0);
		}
	}
	
	private class UnsignedShortGrayscaleBufferedImageSource extends ShortBufferedImageSource {
		protected int mask;
		protected int largestGray;

		UnsignedShortGrayscaleBufferedImageSource(short data[],int width,int height,int mask,int largestGray) {
			super(data,width,height,1);
//System.err.println("SourceImage.UnsignedShortGrayscaleBufferedImageSource():");
			this.mask=mask;
			this.largestGray=largestGray;
			imgMin = 0x0000ffff;	// i.e. start with the largest  possible 16 bit +ve value, sign extended to the full Java int 32 bits
			imgMax = 0x00000000;	// i.e. start with the smallest possible 16 bit -ve value, sign extended to the full Java int 32 bits
		}
		
		UnsignedShortGrayscaleBufferedImageSource(ShortBuffer buffer,int width,int height,int mask,int largestGray) {
			super(buffer,width,height,1);
//System.err.println("SourceImage.UnsignedShortGrayscaleBufferedImageSource():");
			this.mask=mask;
			this.largestGray=largestGray;
			imgMin = 0x0000ffff;	// i.e. start with the largest  possible 16 bit +ve value, sign extended to the full Java int 32 bits
			imgMax = 0x00000000;	// i.e. start with the smallest possible 16 bit -ve value, sign extended to the full Java int 32 bits
		}
		
		public BufferedImage getUncachedBufferedImage(int index) {
//System.err.println("SourceImage.UnsignedShortGrayscaleBufferedImageSource.getUncachedBufferedImage(): index="+index);
			short[] useData;
			int useOffset;
			if (data == null) {
//System.err.println("SourceImage.UnsignedShortGrayscaleBufferedImageSource.getUncachedBufferedImage(): nframesamples = "+nframesamples);
				useData = new short[nframesamples];
				int position = nframesamples*index;
//System.err.println("SourceImage.UnsignedShortGrayscaleBufferedImageSource.getUncachedBufferedImage(): position = "+position);
				buffer.position(position);
				buffer.get(useData);
				useOffset = 0;
			}
			else {
				useData = data;
				useOffset = nframesamples*index;
			}
			// now copy the data for just one frame, masking it
			short[] newData = new short[nframesamples];
			for (int i=useOffset,j=0; j<nframesamples; ++i,++j) {
				boolean isPaddingValue = false;
				short unmaskedValue=useData[i];
				if (useNonMaskedSinglePadValue && unmaskedValue == ((short)nonMaskedSinglePadValue)) {
					isPaddingValue = true;
				}
				int value=((int)unmaskedValue)&mask;
//System.err.println("SourceImage.UnsignedShortGrayscaleBufferedImageSource.getUncachedBufferedImage(): value ["+i+"] = "+value);
				// unsigned so no need to check for sign extension
				newData[j]=(short)value;
				if (useMaskedPadRange && (value >= useMaskedPadRangeStart && value <= useMaskedPadRangeEnd)) {
					isPaddingValue = true;
				}
				if (!isPaddingValue) {
					if (value > imgMax && value <= largestGray) {
//System.err.println("SourceImage.UnsignedShortGrayscaleBufferedImageSource.getUncachedBufferedImage(): imgMax was = "+imgMax+" now setting to "+value);
						imgMax=value;
					}
					if (value < imgMin) {
//System.err.println("SourceImage.UnsignedShortGrayscaleBufferedImageSource.getUncachedBufferedImage(): imgMin was = "+imgMin+" now setting to "+value);
						imgMin=value;
					}
					//imgSum+=value;
					//imgSumOfSquares+=value*value;
				}
			}
			minMaxSet=true;
			return createUnsignedShortGrayscaleImage(width,height,newData,0);
		}
	}
	
	private class ByteGrayscaleBufferedImageSource extends ByteBufferedImageSource {
		ByteGrayscaleBufferedImageSource(byte data[],int width,int height) {
			super(data,width,height,1);
		}
		
		public BufferedImage getUncachedBufferedImage(int index) {
//System.err.println("SourceImage.ByteGrayscaleBufferedImageSource.getUncachedBufferedImage(): index="+index);
			return createByteGrayscaleImage(width,height,data,nframesamples*index);
		}
	}
	
	private class BandInterleavedByteRGBBufferedImageSource extends ByteBufferedImageSource {
		BandInterleavedByteRGBBufferedImageSource(byte data[],int width,int height) {
			super(data,width,height,3);
		}
		
		public BufferedImage getUncachedBufferedImage(int index) {
//System.err.println("SourceImage.BandInterleavedByteRGBBufferedImageSource.getUncachedBufferedImage(): index="+index);
			return createBandInterleavedByteRGBImage(width,height,data,nframesamples*index);
		}
	}
	
	private class PixelInterleavedByteRGBBufferedImageSource extends ByteBufferedImageSource {
		PixelInterleavedByteRGBBufferedImageSource(byte data[],int width,int height) {
			super(data,width,height,3);
		}
		
		public BufferedImage getUncachedBufferedImage(int index) {
//System.err.println("SourceImage.PixelInterleavedByteRGBBufferedImageSource.getUncachedBufferedImage(): index="+index);
			return createPixelInterleavedByteRGBImage(width,height,data,nframesamples*index);
		}
	}

	// See "http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4724038" Bug ID: 4724038 (fs) Add unmap method to MappedByteBuffer
	//
	protected static void clean(final Object object) throws Exception {
		java.security.AccessController.doPrivileged(new java.security.PrivilegedAction() {
				public Object run() {
//System.err.println("SourceImage.clean.run()");
					try {
						java.lang.reflect.Method getCleanerMethod = object.getClass().getMethod("cleaner",new Class[0]);
						getCleanerMethod.setAccessible(true);
						sun.misc.Cleaner cleaner = (sun.misc.Cleaner)getCleanerMethod.invoke(object,new Object[0]);
						cleaner.clean();
					}
					catch(Exception e) {
						e.printStackTrace(System.err);
					}
					return null;
				}
			});
	}

	protected BufferedImageSource bufferedImageSource = null;
	protected FileInputStream memoryMappedFileInputStream = null;
	protected FileChannel memoryMappedFileChannel = null;
	protected MappedByteBuffer memoryMappedByteBuffer = null;
	
	public void close() throws Throwable {
//System.err.println("SourceImage.close()");
		bufferedImageSource=null;
		if (memoryMappedByteBuffer != null) {
			clean(memoryMappedByteBuffer);
			memoryMappedByteBuffer = null;
		}
		if (memoryMappedFileChannel != null) {
			memoryMappedFileChannel.close();
			memoryMappedFileChannel = null;
		}
		if (memoryMappedFileInputStream != null) {
			memoryMappedFileInputStream.close();
			memoryMappedFileInputStream = null;
		}
	}
	
	protected void finalize() throws Throwable {
//System.err.println("SourceImage.finalize()");
		close();
		super.finalize();
	}
	
	/**
	 * @param	list
	 * @exception	DicomException
	 */
	void constructSourceImage(AttributeList list) throws DicomException {

//System.err.println("constructSourceImage - start");

		//double imgSum=0;
		//double imgSumOfSquares=0;

		title=AttributeList.buildInstanceTitleFromAttributeList(list);

		width		= Attribute.getSingleIntegerValueOrDefault(list,TagFromName.Columns,0);
//System.err.println("width="+width);
		height		= Attribute.getSingleIntegerValueOrDefault(list,TagFromName.Rows,0);
//System.err.println("height="+height);
		int depth 	= Attribute.getSingleIntegerValueOrDefault(list,TagFromName.BitsAllocated,0);
//System.err.println("depth="+depth);
		int samples	= Attribute.getSingleIntegerValueOrDefault(list,TagFromName.SamplesPerPixel,1);
//System.err.println("samples="+samples);
		boolean byplane	= (samples > 1 && Attribute.getSingleIntegerValueOrDefault(list,TagFromName.PlanarConfiguration,0) == 1);
//System.err.println("byplane="+byplane);
		nframes		= Attribute.getSingleIntegerValueOrDefault(list,TagFromName.NumberOfFrames,1);
//System.err.println("nframes="+nframes);

		mask = 0;
		int extend = 0;
		int signbit = 1;
		int stored = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.BitsStored,depth);
//System.err.println("stored="+stored);
		if (depth < stored) {
			throw new DicomException("Unsupported Bits Allocated "+depth+"\" less then Bits Stored "+stored);
		}
		{
			int s = stored;
			while (s-- > 0) {
				mask=mask<<1 | 1;
				signbit=signbit<<1;
			}
			signbit=signbit>>1;
			extend=~mask;
		}
//System.err.println("mask=0x"+Integer.toHexString(mask));
//System.err.println("extend=0x"+Integer.toHexString(extend));
//System.err.println("signbit=0x"+Integer.toHexString(signbit));

		signed	= Attribute.getSingleIntegerValueOrDefault(list,TagFromName.PixelRepresentation,0) == 1;

		imgMin=signed ? 0x00007fff : 0x0000ffff;	// i.e. start with the largest  possible 16 bit +ve value, sign extended to the full Java int 32 bits
		imgMax=signed ? 0xffff8000 : 0x00000000;	// i.e. start with the smallest possible 16 bit -ve value, sign extended to the full Java int 32 bits

//System.err.println("signed="+signed);

		pad=0;
		hasPad=false;
		useMaskedPadRange=false;
		useNonMaskedSinglePadValue=false;
		Attribute aPixelPaddingValue = list.get(TagFromName.PixelPaddingValue);
		if (aPixelPaddingValue != null) {
			hasPad=true;
			pad=aPixelPaddingValue.getSingleIntegerValueOrDefault(0);
			padRangeLimit=Attribute.getSingleIntegerValueOrDefault(list,TagFromName.PixelPaddingRangeLimit,pad);
//System.err.println("hasPad="+hasPad);
//System.err.println("pad=0x"+Integer.toHexString(pad)+" ("+pad+" dec)");
//System.err.println("padRangeLimit=0x"+Integer.toHexString(padRangeLimit)+" ("+padRangeLimit+" dec)");
			useMaskedPadRangeStart = pad&mask;
			useMaskedPadRangeEnd = padRangeLimit&mask;
			if (useMaskedPadRangeStart == (pad & 0x0000ffff) && useMaskedPadRangeEnd == (padRangeLimit & 0x0000ffff)) {
//System.err.println("Padding values are within mask range and hence valid");
				useMaskedPadRange = true;
				if (useMaskedPadRangeStart > useMaskedPadRangeEnd) {
					int tmp = useMaskedPadRangeEnd;
					useMaskedPadRangeEnd = useMaskedPadRangeStart;
					useMaskedPadRangeStart = tmp;
				}
//System.err.println("useMaskedPadRangeStart="+useMaskedPadRangeStart);
//System.err.println("useMaskedPadRangeEnd="+useMaskedPadRangeEnd);
			}
			else {
//System.err.println("Padding values are outside mask range and theoretically invalid - ignore any range and just use fixed value of PixelPaddingValue");
				useNonMaskedSinglePadValue = true;
				nonMaskedSinglePadValue = pad;
			}
		}

		String vPhotometricInterpretation = Attribute.getSingleStringValueOrDefault(list,TagFromName.PhotometricInterpretation,"MONOCHROME2");

		if (vPhotometricInterpretation.equals("MONOCHROME2")) {
			isGrayscale = true;
		}
		else if (vPhotometricInterpretation.equals("MONOCHROME1")) {
			isGrayscale = true;
			inverted = true;
		}
		else if (vPhotometricInterpretation.equals("PALETTE COLOR")) {
			isPaletteColor = true;
		}
		else if (vPhotometricInterpretation.equals("YBR_FULL")) {
			isYBR = true;
		}

//System.err.println("inverted="+inverted);
//System.err.println("isGrayscale="+isGrayscale);
//System.err.println("isPaletteColor="+isPaletteColor);
//System.err.println("isYBR="+isYBR);

		// Get palette color LUT stuff, if present ...
		
		Attribute aLargestMonochromePixelValue  = list.get(TagFromName.LargestMonochromePixelValue);
		Attribute aRedPaletteColorLookupTableDescriptor = list.get(TagFromName.RedPaletteColorLookupTableDescriptor);
		Attribute aGreenPaletteColorLookupTableDescriptor = list.get(TagFromName.GreenPaletteColorLookupTableDescriptor);
		Attribute aBluePaletteColorLookupTableDescriptor = list.get(TagFromName.BluePaletteColorLookupTableDescriptor);
	
		largestGray = signed ? 0x00007fff : 0x0000ffff;			// default to largest possible in case nothing found
		boolean usedLargestMonochromePixelValue=false;
		if (aLargestMonochromePixelValue != null && aLargestMonochromePixelValue.getVM() == 1) {
			usedLargestMonochromePixelValue=true;
			largestGray=aLargestMonochromePixelValue.getIntegerValues()[0];
		}
		boolean usedLargestImagePixelValue=false;
		if (usedLargestMonochromePixelValue == false) {			// encountered this in an old MR SOP Class Siemens MR image
			Attribute aLargestImagePixelValue  = list.get(TagFromName.LargestImagePixelValue);
			if (aLargestImagePixelValue != null && aLargestImagePixelValue.getVM() == 1) {
				usedLargestImagePixelValue=true;
				largestGray=aLargestImagePixelValue.getIntegerValues()[0];
			}
		}
                
 		boolean usedFirstValueMapped=false;
 		if (aRedPaletteColorLookupTableDescriptor != null && aGreenPaletteColorLookupTableDescriptor != null && aBluePaletteColorLookupTableDescriptor != null) {
			// the descriptors should all be the same; should check but let's be lazy and just use one ...
			if (aRedPaletteColorLookupTableDescriptor != null && aRedPaletteColorLookupTableDescriptor.getVM() == 3) {
				numberOfEntries = aRedPaletteColorLookupTableDescriptor.getIntegerValues()[0];
				if (numberOfEntries == 0) numberOfEntries=65536;
				firstValueMapped = aRedPaletteColorLookupTableDescriptor.getIntegerValues()[1];
				if ((usedLargestMonochromePixelValue == false && usedLargestImagePixelValue == false)
				 || list.get(TagFromName.PhotometricInterpretation).getStringValues()[0].equals("PALETTE COLOR")) { // [bugs.mrmf] (000102) Palette Color image displays as gray when Largest Pixel Value present
					usedFirstValueMapped=true;
					largestGray=firstValueMapped-1;			// if a pure color image then firstValueMapped will be 0, and largestGray will be -1
				}
				bitsPerEntry = aRedPaletteColorLookupTableDescriptor.getIntegerValues()[2];
				if (bitsPerEntry > 0) {
					Attribute aRedPaletteColorLookupTableData = list.get(TagFromName.RedPaletteColorLookupTableData);
//System.err.println("SourceImage.constructSourceImage(): aRedPaletteColorLookupTableData = "+aRedPaletteColorLookupTableData);
					Attribute aGreenPaletteColorLookupTableData = list.get(TagFromName.GreenPaletteColorLookupTableData);
					Attribute aBluePaletteColorLookupTableData = list.get(TagFromName.BluePaletteColorLookupTableData);
					if (aRedPaletteColorLookupTableData != null && aGreenPaletteColorLookupTableData != null && aBluePaletteColorLookupTableData != null) {
//System.err.println("SourceImage.constructSourceImage(): setting color palette tables");
						redTable = aRedPaletteColorLookupTableData.getShortValues();
//System.err.println("SourceImage.constructSourceImage(): redTable = "+redTable);
						greenTable = aGreenPaletteColorLookupTableData.getShortValues();
						blueTable = aBluePaletteColorLookupTableData.getShortValues();
						if (redTable == null || greenTable == null || blueTable == null
						 || redTable.length == 0 || greenTable.length == 0 || blueTable.length == 0) {
							// occasionally see an incorrect image with attribute present but nothing in it ... (000570)
System.err.println("SourceImage.constructSourceImage(): bad color palette (empty data), ignoring");
							redTable=null;
							greenTable=null;
							blueTable=null;
						}
					}
				}
				else {
System.err.println("SourceImage.constructSourceImage(): bad color palette (zero value for bitsPerEntry), ignoring");
					// else was just bad ... bitsPerEntry is a better flag than numberOfEntries and firstValueMapped, since the last two may legitimately be zero, not not the first ... (000570)
				}
			}
		}
//System.err.println("SourceImage.constructSourceImage(): largestGray="+largestGray);		

		bufferedImageSource = null;
		
		int nframepixels=width*height;
		int nframesamples=nframepixels*samples;
		int nsamples=nframesamples*nframes;
		int npixels=nframepixels*nframes;
//System.err.println("SourceImage.constructSourceImage(): isGrayscale="+isGrayscale);
//System.err.println("SourceImage.constructSourceImage(): samples="+samples);
//System.err.println("SourceImage.constructSourceImage(): depth="+depth);
//System.err.println("SourceImage.constructSourceImage(): stored="+stored);
		// The following assumes that BitsStored (aka. stored) is always <= BitsAllocated (aka. depth) (checked earlier)
		if ((isGrayscale || isPaletteColor) && samples == 1 && depth > 8) {
//System.err.println("SourceImage.constructSourceImage(): grayscale or palette color > 8 bits");
			// note that imgMin and imgMax are populated on demand when BufferedImages are actually created
			Attribute a = list.get(TagFromName.PixelData);
			if (a instanceof OtherWordAttributeOnDisk) {
//System.err.println("SourceImage.constructSourceImage(): on disk");
				OtherWordAttributeOnDisk owa = (OtherWordAttributeOnDisk)a;
				// keep track of all the intermediaries so we can explicitly close them later (e.g., on finalization)
				memoryMappedFileInputStream = null;
				memoryMappedFileChannel = null;
				try {
					memoryMappedFileInputStream = new FileInputStream(owa.getFile());
					memoryMappedFileChannel = memoryMappedFileInputStream.getChannel();
				}
				catch (FileNotFoundException e) {
					throw new DicomException("Cannot find file to memory map "+owa.getFile()+" "+e);
				}
				// Why repeatedly retry ? See "http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5092131"
				// The key is to make sure that the finalize() method for any uncollected and unfinalized
				// memory mapped buffers is called (and of course that we have specified finalize methods
				// in our BufferedImageSource classes for those that need to null any buffer references)
				// before we try to map another large file; this is presumed to be needed because reaping
				// the small heap objects that are associated with mapped buffers is not a priority for
				// the garbage collector
				memoryMappedByteBuffer = null;
				int retrycount = 100;	// often only takes once or twice, may take more than 10
				int sleepTimeBetweenRetries = 1000;
				int retryBeforeSleeping = 10;
				Exception einside = null;
				while (memoryMappedByteBuffer == null && retrycount-- > 0) {
					if (retryBeforeSleeping-- <= 0) {
						try {
//System.err.println("SourceImage.constructSourceImage(): sleeping");
							Thread.currentThread().sleep(sleepTimeBetweenRetries);
//System.err.println("SourceImage.constructSourceImage(): back from sleep");
						}
						catch (InterruptedException e) {
							e.printStackTrace(System.err);
						}
					}
//System.err.println("SourceImage.constructSourceImage(): retrycount = "+retrycount);
//System.err.println("SourceImage.constructSourceImage(): free  memory = "+Runtime.getRuntime().freeMemory());
//System.err.println("SourceImage.constructSourceImage(): max   memory = "+Runtime.getRuntime().maxMemory());
//System.err.println("SourceImage.constructSourceImage(): total memory = "+Runtime.getRuntime().totalMemory());
					try {
//System.err.println("SourceImage.constructSourceImage(): requesting gc and runFinalization");
						System.gc(); System.runFinalization();
//System.err.println("SourceImage.constructSourceImage(): back from gc and runFinalization");
						memoryMappedByteBuffer = memoryMappedFileChannel.map(FileChannel.MapMode.READ_ONLY,owa.getByteOffset(),nsamples*2);
					}
					catch (Exception e) {
						e.printStackTrace(System.err);
						einside=e;
					}
				}
				if (memoryMappedByteBuffer == null) {
					throw new DicomException("Cannot memory map file "+owa.getFile()+" "+einside);
				}
				memoryMappedByteBuffer.order(owa.isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
				ShortBuffer shortBuffer = memoryMappedByteBuffer.asShortBuffer();
				if (signed) {
					bufferedImageSource = new SignedShortGrayscaleBufferedImageSource(shortBuffer,width,height,mask,signbit,extend,largestGray);
				}
				else {
					bufferedImageSource = new UnsignedShortGrayscaleBufferedImageSource(shortBuffer,width,height,mask,largestGray);
				}
			}
			else {
//System.err.println("SourceImage.constructSourceImage(): not on disk");
				short data[] = a.getShortValues();
				if (signed) {
					imgs=null;
					bufferedImageSource = new SignedShortGrayscaleBufferedImageSource(data,width,height,mask,signbit,extend,largestGray);
				}
				else {
					imgs=null;
					bufferedImageSource = new UnsignedShortGrayscaleBufferedImageSource(data,width,height,mask,largestGray);
				}
			}
		}
		else if ((isGrayscale || isPaletteColor) && samples == 1 && depth <= 8 && depth > 1) {
//System.err.println("SourceImage.constructSourceImage(): grayscale or palette color <= 8 bits");
			byte data[] = null;
			Attribute a = list.get(TagFromName.PixelData);
			if (ValueRepresentation.isOtherByteVR(a.getVR())) {
				byte[] sdata = a.getByteValues();
				data = new byte[nsamples];
				for (int count=0; count<nsamples; ++count) {
					int value=((int)sdata[count])&mask;
					int nonextendedvalue = value;
//System.err.print("value masked = "+Integer.toHexString(value));
					if (signed && (value & signbit) != 0) value=value | extend;
//System.err.println(" extended = "+Integer.toHexString(value));
					data[count]=(byte)value;
					if (nonextendedvalue < useMaskedPadRangeStart || nonextendedvalue > useMaskedPadRangeEnd) {
						if (value > imgMax && value <= largestGray) imgMax=value;
						if (value < imgMin) imgMin=value;
                        //imgSum+=value;
						//imgSumOfSquares+=value*value;
					}
				}
			}
			else {
				short sdata[] = a.getShortValues();
				data = new byte[nsamples];
				int slen=nsamples/2;
				int scount=0;
				int count=0;
				while (scount<slen) {
					int value=((int)sdata[scount++])&0xffff;	// the endianness of the TS has already been accounted for
					int value1=value&mask;						// now just unpack from low part of word first
					int nonextendedvalue1 = value1;
					if (signed && (value1 & signbit) != 0) value1=value1 | extend;
					data[count++]=(byte)value1;
					if (nonextendedvalue1 < useMaskedPadRangeStart || nonextendedvalue1 > useMaskedPadRangeEnd) {
						if (value1 > imgMax && value1 <= largestGray) imgMax=value1;
						if (value1 < imgMin) imgMin=value1;
						//imgSum+=value1;
						//imgSumOfSquares+=value1*value1;
					}
					int value2=(value>>8)&mask;
					int nonextendedvalue2 = value2;
					if (signed && (value2 & signbit) != 0) value2=value2 | extend;
					data[count++]=(byte)value2;
					if (nonextendedvalue2 < useMaskedPadRangeStart || nonextendedvalue2 > useMaskedPadRangeEnd) {
						if (value2 > imgMax && value2 <= largestGray) imgMax=value2;
						if (value2 < imgMin) imgMin=value2;
						//imgSum+=value2;
						//imgSumOfSquares+=value2*value2;
					}
				}
			}
			//imgs=new BufferedImage[nframes];
			//int offset=0;
			//for (int frame=0; frame<nframes; ++frame) {
			//	imgs[frame]=createByteGrayscaleImage(width,height,data,offset);
			//	offset+=nframesamples;
			//}
			imgs=null;
			bufferedImageSource = new ByteGrayscaleBufferedImageSource(data,width,height);
		}
		else if (isGrayscale && samples == 1 && depth == 1) {
//System.err.println("SourceImage.constructSourceImage(): single bit");
			// see also com.pixelmed.dicom.Overlay for similar pattern of extracting bits from OB or OW and making BufferedImage of TYPE_BYTE_BINARY
			imgs=new BufferedImage[nframes];
			IndexColorModel colorModel = null;
			{
				byte[] r = { (byte)0, (byte)255 };
				byte[] g = { (byte)0, (byte)255 };
				byte[] b = { (byte)0, (byte)255 };
				colorModel = new IndexColorModel(
							1 /* bits */,
							2 /* size */,
							r,g,b/*,
							java.awt.Transparency.OPAQUE*/);
			}
			Attribute a = list.get(TagFromName.PixelData);
			int wi=0;
			int bitsRemaining=0;
			int word = 0;
			boolean badBitOrder = false;
			if (ValueRepresentation.isOtherByteVR(a.getVR())) {
//System.err.println("SourceImage.constructSourceImage(): single bit from OB with "+(badBitOrder ? "bad" : "standard")+" bit order");
				byte data[] = a.getByteValues();
				for (int f=0; f<nframes; ++f) {
					imgs[f] = new BufferedImage(width,height,BufferedImage.TYPE_BYTE_BINARY,colorModel);
					Raster raster = imgs[f].getData();
					SampleModel sampleModel = raster.getSampleModel();
					DataBuffer dataBuffer = raster.getDataBuffer();
					for (int row=0; row<height; ++row) {
						for (int column=0; column<width; ++column) {
							if (bitsRemaining <= 0) {
								word = data[wi++];
								bitsRemaining=8;
							}
							int bit = badBitOrder ? (word & 0x0080) : (word & 0x0001);
							if (bit > 0) {
//System.err.println("SourceImage.constructSourceImage(): got a bit set at frame "+f+" row "+row+" column "+column);
								sampleModel.setSample(column,row,0/*bank*/,1,dataBuffer);
							}
							word = badBitOrder ? (word << 1) : (word >> 1);
							--bitsRemaining;
						}
					}
					imgs[f].setData(raster);
				}
			}
			else {
//System.err.println("SourceImage.constructSourceImage(): single bit from OW with "+(badBitOrder ? "bad" : "standard")+" bit order");
				short data[] = a.getShortValues();
				for (int f=0; f<nframes; ++f) {
					imgs[f] = new BufferedImage(width,height,BufferedImage.TYPE_BYTE_BINARY,colorModel);
					Raster raster = imgs[f].getData();
					SampleModel sampleModel = raster.getSampleModel();
					DataBuffer dataBuffer = raster.getDataBuffer();
					for (int row=0; row<height; ++row) {
						for (int column=0; column<width; ++column) {
							if (bitsRemaining <= 0) {
								word = data[wi++];
								bitsRemaining=16;
							}
							int bit = badBitOrder ? (word & 0x8000) : (word & 0x0001);
							if (bit > 0) {
								sampleModel.setSample(column,row,0/*bank*/,1,dataBuffer);
							}
							word = badBitOrder ? (word << 1) : (word >> 1);
							--bitsRemaining;
						}
					}
					imgs[f].setData(raster);
				}
			}
		}
		else if (!isGrayscale && samples == 3 && depth <= 8 && depth > 1) {
//System.err.println("SourceImage.constructSourceImage(): not grayscale, is 3 channel and <= 8 bits");
			byte data[] = null;
			Attribute a = list.get(TagFromName.PixelData);
			if (ValueRepresentation.isOtherByteVR(a.getVR())) {
				data = a.getByteValues();
			}
			else {
				short sdata[] = a.getShortValues();
				data = new byte[nsamples];
				int slen=nsamples/2;
				int scount=0;
				int count=0;
				while (scount<slen) {
					int value=((int)sdata[scount++])&0xffff;	// the endianness of the TS has already been accounted for
					int value1=value&0xff;				// now just unpack from low part of word first
					data[count++]=(byte)value1;
					int value2=(value>>8)&0xff;
					data[count++]=(byte)value2;
				}
			}
			imgs=null;
			
			// Note that we are really lying at this point if Photometric Interpretation is not RGB,
			// e.g., YBR_FULL, in that the ColorModel created next will claim to be RGB (and will need
			// to be converted on display, etc., but this prevents us having to update the source
			// AttributeList to change the Photometric Interpretation attribute.
			if (byplane) {
				bufferedImageSource = new BandInterleavedByteRGBBufferedImageSource(data,width,height);
			}
			else {
				bufferedImageSource = new PixelInterleavedByteRGBBufferedImageSource(data,width,height);
			}
		}
		else {
			throw new DicomException("Unsupported image Photometric Interpretation \""+vPhotometricInterpretation+"\" or Bits Allocated "+depth+"\" or Bits Stored "+stored);
		}
		
		//BufferedImageUtilities.describeImage(imgs[0],System.err);
                
		//imgMean=imgSum/nsamples;
		//imgStandardDeviation=Math.sqrt((imgSumOfSquares-imgSum*imgSum/nsamples)/nsamples);
//System.err.println("SourceImage.constructSourceImage(): imgMin="+imgMin);
//System.err.println("SourceImage.constructSourceImage(): imgMax="+imgMax);
//System.err.println("SourceImage.constructSourceImage(): imgSum="+imgSum);
//System.err.println("SourceImage.constructSourceImage(): imgSumOfSquares="+imgSumOfSquares);
//System.err.println("SourceImage.constructSourceImage(): imgMean="+imgMean);
//System.err.println("SourceImage.constructSourceImage(): imgStandardDeviation="+imgStandardDeviation);

		suvTransform = new SUVTransform(list);
		realWorldValueTransform = new RealWorldValueTransform(list);
		modalityTransform = new ModalityTransform(list);
		voiTransform = new VOITransform(list);
		displayShutter = new DisplayShutter(list);
		overlay = new Overlay(list);

		// Establish a suitable background value to be used during resizing operations PRIOR to windowing
		if (hasPad) {
			backgroundValue = pad;
		}
		else if (isGrayscale) {
			backgroundValue = inverted
				? (signed ? (mask>>1) : mask)					// largest value (will always be +ve)
				: (signed ? (((mask>>1)+1)|extend) : 0);		// smallest value (will be -ve if signed so extend into integer)
			// do NOT anticipate rescale values
		}
		else {
			backgroundValue = 0;
		}
//System.err.println("backgroundValue="+backgroundValue);

//System.err.println("constructSourceImage - end");
	}

	/***/
	public BufferedImage getBufferedImage() { return getBufferedImage(0); }

	/***/
	public BufferedImage getBufferedImage(int i) {
		BufferedImage img = null;
		if (bufferedImageSource == null) {
			img = (imgs == null || i < 0 || i >= imgs.length) ? null : imgs[i];
		}
		else {
			img = bufferedImageSource.getBufferedImage(i);
			// Why reset the min and max ? They may not previously have been set of frame not fetched from memory mapped file yet
			imgMin = bufferedImageSource.getMinimumPixelValueOfMostRecentBufferedImage(imgMin);
			imgMax = bufferedImageSource.getMaximumPixelValueOfMostRecentBufferedImage(imgMax);
//System.err.println("SourceImage.getBufferedImage(): resetting imgMin="+imgMin);
//System.err.println("SourceImage.getBufferedImage(): resetting imgMax="+imgMax);
		}
		return img;
	}

	/***/
	public int getNumberOfBufferedImages() { return nframes; }

	/***/
	public int getWidth() { return width; }
        
	/***/
	public int getHeight() { return height; }
        
	/***/
	public Dimension getDimension() { return new Dimension(width,height); }
        
	/**
	 * @return	the minimum pixel value, excluding any pixels in the padding value range
	 */
	public int getMinimum() { return imgMin; }

	/**
	 * @return	the maximum pixel value, excluding any pixels in the padding value range
	 */
	public int getMaximum() { return imgMax; }

	//public double getMean() { return imgMean; }

	//public double getStandardDeviation() { return imgStandardDeviation; }

	/***/
	public int getMaskValue() { return mask; }

	/***/
	public boolean isSigned() { return signed; }

	/***/
	public boolean isInverted() { return inverted; }

	/***/
	public boolean isPadded() { return hasPad; }

	/***/
	public int getPadValue() { return pad; }

	/***/
	public int getPadRangeLimit() { return padRangeLimit; }

	/***/
	public int getBackgroundValue() { return backgroundValue; }

	/***/
	public boolean isGrayscale() { return isGrayscale; }

	/***/
	public boolean isYBR() { return isYBR; }

	/***/
	public String getTitle() { return title; }

	/***/
	public int getNumberOfFrames() { return nframes; }

	/***/
	public int getPaletteColorLargestGray() { return largestGray; }
	
	/***/
	public int getPaletteColorFirstValueMapped() { return firstValueMapped; }
	
	/***/
	public int getPaletteColorNumberOfEntries() { return numberOfEntries; }
	
	/***/
	public int getPaletteColorBitsPerEntry() { return bitsPerEntry; }
	
	/***/
	public short[] getPaletteColorRedTable() { return redTable; }
	
	/***/
	public short[] getPaletteColorGreenTable() { return greenTable; }
	
	/***/
	public short[] getPaletteColorBlueTable() { return blueTable; }
	
	/***/
	public SUVTransform getSUVTransform() { return suvTransform; }
	
	/***/
	public RealWorldValueTransform getRealWorldValueTransform() { return realWorldValueTransform; }
	
	/***/
	public ModalityTransform getModalityTransform() { return modalityTransform; }
	
	/***/
	public VOITransform getVOITransform() { return voiTransform; }
	
	/***/
	public DisplayShutter getDisplayShutter() { return displayShutter; }
	
	/***/
	public Overlay getOverlay() { return overlay; }
}

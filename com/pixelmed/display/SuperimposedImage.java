/* Copyright (c) 2001-2012, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import com.pixelmed.geometry.GeometryOfSlice;
import com.pixelmed.geometry.GeometryOfVolume;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

/**
 * <p>A class that supports matching the geometry of a superimposed image
 * and an underlying images, and creating BufferedImages suitable for
 * drawing on an underlying image.</p>
 *
 * @see com.pixelmed.display.SingleImagePanel
 *
 * @author	dclunie
 */

public class SuperimposedImage {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/SuperimposedImage.java,v 1.1 2012/03/14 18:36:23 dclunie Exp $";
	
	private static final double CLOSEST_SLICE_TOLERANCE_FACTOR = 0.01d;	// mm
	
	// these are protected so as to be accessible to SuperimposedDicomImage or other potential sub-classes for other formats
	protected SourceImage superimposedSourceImage;
	protected GeometryOfVolume superimposedGeometry;
	
	protected SuperimposedImage() {
		this.superimposedSourceImage = null;
		this.superimposedGeometry = null;
	}

	/**
	 * <p>A class that supports matching the geometry of a superimposed image
	 * and a specified underlying image, and creating a BufferedImage suitable for
	 * drawing on that underlying image.</p>
	 */
	public class AppliedToUnderlyingImage {
		private BufferedImage bufferedImage;
		private double columnOrigin;
		private double rowOrigin;
	
		public BufferedImage getBufferedImage() { return bufferedImage; }

		public double getColumnOrigin() { return columnOrigin; }

		public double getRowOrigin()    { return rowOrigin; }
	
		/**
		 * @param	underlyingGeometry
		 * @param	underlyingFrame	numbered from 0
		 */
		private AppliedToUnderlyingImage(GeometryOfVolume underlyingGeometry,int underlyingFrame) {
			bufferedImage = null;
			columnOrigin = 0;
			rowOrigin = 0;
			
			if (underlyingGeometry != null) {
//System.err.println("SuperimposedImage.AppliedToUnderlyingImage(): underlyingFrame = "+underlyingFrame);
				GeometryOfSlice geometryOfUnderlyingSlice = underlyingGeometry.getGeometryOfSlice(underlyingFrame);
//System.err.println("SuperimposedImage.AppliedToUnderlyingImage(): geometryOfUnderlyingSlice = "+geometryOfUnderlyingSlice);
				if (geometryOfUnderlyingSlice != null && superimposedGeometry != null) {
					int superimposedFrame = superimposedGeometry.findClosestSliceInSamePlane(geometryOfUnderlyingSlice);
//System.err.println("SuperimposedImage.AppliedToUnderlyingImage(): closest superimposed frame = "+superimposedFrame);
					GeometryOfSlice geometryOfSuperimposedSlice = superimposedGeometry.getGeometryOfSlice(superimposedFrame);
//System.err.println("SuperimposedImage.AppliedToUnderlyingImage(): geometryOfSuperimposedSlice = "+geometryOfSuperimposedSlice);
					// closest slice may not be "close enough", so check that normal distance is zero (e.g., Z positions are the same in the axial case)
					double superimposedDistanceAlongNormal = geometryOfSuperimposedSlice.getDistanceAlongNormalFromOrigin();
					double underlyingDistanceAlongNormal = geometryOfUnderlyingSlice.getDistanceAlongNormalFromOrigin();
//System.err.println("SuperimposedImage.AppliedToUnderlyingImage(): superimposedDistanceAlongNormal = "+superimposedDistanceAlongNormal);
//System.err.println("SuperimposedImage.AppliedToUnderlyingImage(): underlyingDistanceAlongNormal = "+underlyingDistanceAlongNormal);
					if (Math.abs(superimposedDistanceAlongNormal - underlyingDistanceAlongNormal) < CLOSEST_SLICE_TOLERANCE_FACTOR) {
//System.err.println("SuperimposedImage.AppliedToUnderlyingImage(): close enough");
						double[] tlhcSuperimposedIn3DSpace = geometryOfSuperimposedSlice.getTLHCArray();
//System.err.println("SuperimposedImage.AppliedToUnderlyingImage(): tlhc of superimposed slice in 3D space = "+java.util.Arrays.toString(tlhcSuperimposedIn3DSpace));
						if (tlhcSuperimposedIn3DSpace != null && tlhcSuperimposedIn3DSpace.length == 3) {
							double[] tlhcSuperimposedInUnderlyingImageSpace = geometryOfUnderlyingSlice.lookupImageCoordinate(tlhcSuperimposedIn3DSpace);
//System.err.println("SuperimposedImage.AppliedToUnderlyingImage(): tlhc of superimposed slice in underlying image space = "+java.util.Arrays.toString(tlhcSuperimposedInUnderlyingImageSpace));
							if (tlhcSuperimposedInUnderlyingImageSpace != null && tlhcSuperimposedInUnderlyingImageSpace.length == 2) {
								columnOrigin  = tlhcSuperimposedInUnderlyingImageSpace[0];
								rowOrigin     = tlhcSuperimposedInUnderlyingImageSpace[1];
								if (superimposedSourceImage != null) {
									BufferedImage originalBufferedImage = superimposedSourceImage.getBufferedImage(superimposedFrame);
//System.err.println("SuperimposedImage.AppliedToUnderlyingImage(): originalBufferedImage = "+originalBufferedImage);
									if (originalBufferedImage != null) {
										// http://docs.oracle.com/javase/tutorial/2d/images/examples/SeeThroughImageApplet.java
										bufferedImage = new BufferedImage(originalBufferedImage.getWidth(),originalBufferedImage.getHeight(),BufferedImage.TYPE_INT_ARGB);
										Graphics g = bufferedImage.getGraphics();
										g.drawImage(originalBufferedImage,0,0,null);
									}
								}
							}
						}
					}
				}
			}
		}
		
		public String toString() {
			return "(bufferedImage="+(bufferedImage == null ? "null" : bufferedImage.toString())+",columnOrigin="+columnOrigin+",rowOrigin="+rowOrigin+")";
		}
	}
	
	/**
	 * @param	underlyingGeometry
	 * @param	underlyingFrame	numbered from 0
	 */
	public AppliedToUnderlyingImage getAppliedToUnderlyingImage(GeometryOfVolume underlyingGeometry,int underlyingFrame) {
		return new AppliedToUnderlyingImage(underlyingGeometry,underlyingFrame);
	}

	/**
	 * @param	superimposedSourceImage
	 * @param	superimposedGeometry
	 */
	public SuperimposedImage(SourceImage superimposedSourceImage,GeometryOfVolume superimposedGeometry) {
		this.superimposedSourceImage = superimposedSourceImage;
		this.superimposedGeometry = superimposedGeometry;
	}
}


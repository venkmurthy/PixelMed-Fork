/* Copyright (c) 2001-2012, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.GeometryOfVolumeFromAttributeList;
import com.pixelmed.dicom.SOPClass;
import com.pixelmed.dicom.TagFromName;

import com.pixelmed.geometry.GeometryOfVolume;

import java.io.IOException;

import java.util.Vector;

/**
 * <p>A class that supports matching the geometry of a superimposed DICOM image
 * and an underlying images, and creating BufferedImages suitable for
 * drawing on an underlying image.</p>
 *
 * @author	dclunie
 */

public class SuperimposedDicomImage extends SuperimposedImage {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/SuperimposedDicomImage.java,v 1.1 2012/03/14 18:36:23 dclunie Exp $";
	
	/**
	 * @param	list
	 */
	public SuperimposedDicomImage(AttributeList list) throws DicomException {
		// no need to call super(), does nothing
		doCommonConstructorStuff(list);
	}
	
	/**
	 * @param	filename
	 */
	public SuperimposedDicomImage(String filename) throws DicomException, IOException {
		// no need to call super(), does nothing
		AttributeList list = new AttributeList();
		list.read(filename);
		doCommonConstructorStuff(list);
	}
	
	/**
	 * @param	list
	 */
	private void doCommonConstructorStuff(AttributeList list) throws DicomException {
		String sopClassUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPClassUID);
		if (SOPClass.isImageStorage(sopClassUID)) {
System.err.println("SuperimposedDicomImage.doCommonConstructorStuff(): is an image");
			superimposedSourceImage = new SourceImage(list);
			if (superimposedSourceImage != null && superimposedSourceImage.getNumberOfBufferedImages() > 0) {
System.err.println("SuperimposedDicomImage.doCommonConstructorStuff(): has a SourceImage and one or more frames");
				superimposedGeometry= new GeometryOfVolumeFromAttributeList(list);
			}
		}
	}
	
	/**
	 * @param	arg	the underlying image file name and the superimposed image file name
	 */
	public static void main(String arg[]) {
		try {
			AttributeList underlyingList = new AttributeList();
			underlyingList.read(arg[0]);
			SourceImage underlyingSourceImage = new SourceImage(underlyingList);
			GeometryOfVolume underlyingGeometry = new GeometryOfVolumeFromAttributeList(underlyingList);
			SingleImagePanel ip = new SingleImagePanel(underlyingSourceImage,null,underlyingGeometry);
			
			SuperimposedImage superimposedImage = new SuperimposedDicomImage(arg[1]);
			Vector<SuperimposedImage> superimposedImages =  new Vector<SuperimposedImage>();
			superimposedImages.add(superimposedImage);
			
			ip.setSuperimposedImages(superimposedImages);
			
			javax.swing.JFrame frame = new javax.swing.JFrame();
			frame.add(ip);
			frame.setSize(underlyingSourceImage.getWidth(),underlyingSourceImage.getHeight());
			frame.setVisible(true);
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
}


/* Copyright (c) 2001-2011, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import com.pixelmed.utils.FileUtilities;

import java.io.File;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * <p>A class to describe a set of frame sets, each of which shares common characteristics suitable for display or analysis as an entity.</p>
 *
 * @author	dclunie
 */
class SetOfFrameSets extends HashSet<FrameSet> {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/SetOfFrameSets.java,v 1.8 2012/03/26 10:24:01 dclunie Exp $";

	/**
	 * <p>Insert a single or multi-frame object into the set of existing frame sets,
	 * creating new frame sets as necessary.</p>
	 *
	 * @param		list			a list of DICOM attributes for an object
	 * @exception	DicomException	if no SOP Instance UID
	 */
	void insertIntoFrameSets(AttributeList list) throws DicomException {
		// partition by rows and columns and SOP Class and Modality
		
		boolean found = false;
		for (FrameSet tryFrameSet : this) {
			if (tryFrameSet.eligible(list)) {
				tryFrameSet.insert(list);
				found=true;
				break;				// only insert it in the first frame set that matches
			}
		}
		if (!found) {
			add(new FrameSet(list));
		}
	}
	
	/**
	 * <p>Return a String representing this object's value.</p>
	 *
	 * @return	a string representation of the value of this object
	 */
	public String toString() {
		StringBuffer strbuf = new StringBuffer();
		int j = 0;
		for (FrameSet f : this) {
			strbuf.append("Frame set [");
			strbuf.append(Integer.toString(j));
			strbuf.append("]:\n");
			strbuf.append(f.toString());
			strbuf.append("\n");
			++j;
		}
		return strbuf.toString();
	}
	
	/**
	 * <p>Create a new set of frame sets, from a set of DICOM files.</p>
	 *
	 * <p>Non-DICOM files and problems parsing files are ignored, rather than causing failure</p>
	 *
	 * @param		files	a set of files
	 */
	private void doCommonConstructorStuff(Set<File> files) {
		for (File f : files) {
			try {
				if (DicomFileUtilities.isDicomOrAcrNemaFile(f)) {
//System.err.println("SetOfFrameSets.doCommonConstructorStuff(): Doing "+f);
					AttributeList list = new AttributeList();
					list.read(f,TagFromName.PixelData);
					insertIntoFrameSets(list);
				}
			}
			catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
	}
	
	/**
	 * <p>Create an empty new set of frame sets.</p>
	 */
	SetOfFrameSets() {
		super();
	}
	
	/**
	 * <p>Create a new set of frame sets, from a set of DICOM files.</p>
	 *
	 * <p>Non-DICOM files and problems parsing files are ignored, rather than causing failure</p>
	 *
	 * @param		files	a set of files
	 */
	SetOfFrameSets(Set<File> files) {
		super();
		doCommonConstructorStuff(files);
	}
	
	/**
	 * <p>Create a new set of existing frame sets, from a set of DICOM files.</p>
	 *
	 * <p>Non-DICOM files and problems parsing files are ignored, rather than causing failure</p>
	 *
	 * @param		paths	a set of paths of filenames and/or folder names of files containing the images to partition
	 */
	SetOfFrameSets(String paths[]) {
		super();
		Set<File> files = new HashSet<File>();
		for (String p : paths) {
			Collection<File> more = FileUtilities.listFilesRecursively(new File(p));
			files.addAll(more);
		}
		doCommonConstructorStuff(files);
	}

	/**
	 * <p>For testing, read all DICOM files and partition them.</p>
	 *
	 * @param	arg	the filenames and/or folder names of files containing the images to partition
	 */
	public static void main(String arg[]) {
		SetOfFrameSets setOfFrameSets = new SetOfFrameSets(arg);
System.err.println("SetOfFrameSets.main(): Result");
		System.err.println(setOfFrameSets.toString());
	}
}


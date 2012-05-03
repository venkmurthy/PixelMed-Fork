/* Copyright (c) 2001-2007, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.File;

/**
 * <p>Store files in a single folder, using the SOP Instance UID as the filename.</p>
 *
 * <p>This is not a good strategy, since having too many files in a single folder degrades performance,
 * or bump up against limits, like Linux ext2 31998 sub-folders per inode,
 * but is acceptable for modest numbers of images.</p>
 *
 * <p>It is the default strategy when not otherwise specified, since it was the original strategy supported in earlier versions of the toolkit.</p>
 *
 * @author	dclunie, jimirrer
 */

public final class StoredFilePathStrategySingleFolder extends StoredFilePathStrategy {

	public StoredFilePathStrategySingleFolder() {}

	public String makeStoredFilePath(String sopInstanceUID) {
	    return sopInstanceUID;
	}

	public String toString() {
	    return "BYSOPINSTANCEUIDINSINGLEFOLDER";
	}

	/**
	 * <p>Perform self test.  If arguments are given, then use then as test UIDs.  If no arguments, then use internal test UIDs.</p>
	 */
	public static void main(String arg[]) {
		BYSOPINSTANCEUIDINSINGLEFOLDER.test(arg);
	}
}


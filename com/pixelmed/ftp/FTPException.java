/* Copyright (c) 2001-2010, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.ftp;

/**
 * @author	dclunie
 */
public class FTPException extends Exception {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/ftp/FTPException.java,v 1.1 2010/11/15 20:30:23 dclunie Exp $";

	/**
	 * @param	msg
	 */
	public FTPException(String msg) {
		super(msg);
	}
}




/* Copyright (c) 2001-2006, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.utils;

import java.io.PrintStream;

/**
 * <p>A class to write log and status messages to a <code>PrintStream</code> such as <code>System.err</code>.</p>
 *
 * @author      dclunie
 */
public class PrintStreamMessageLogger implements MessageLogger {

	protected PrintStream printStream;

	public PrintStreamMessageLogger(PrintStream printStream) {
		this.printStream = printStream;
	}
	
	public void send(String message) {
		printStream.print(message);
	}
		
	public void sendLn(String message) {
		printStream.println(message);
	}
}


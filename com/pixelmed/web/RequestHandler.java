/* Copyright (c) 2004-2005, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.web;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import com.pixelmed.database.DatabaseInformationModel;
import com.pixelmed.utils.CopyStream;

/**
 * @author	dclunie
 */
abstract class RequestHandler {
	static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/web/RequestHandler.java,v 1.4 2005/02/09 02:05:39 dclunie Exp $";

	protected String stylesheetPath;
	protected int webServerDebugLevel;
	
	protected RequestHandler(String stylesheetPath,int webServerDebugLevel) {
		this.stylesheetPath=stylesheetPath;
		this.webServerDebugLevel=webServerDebugLevel;
	}
	
	/**
	 * @param	databaseInformationModel	the database, may be null if not required for the type of request
	 * @param	rootURL				the root to prepend to URL's embedded in responses
	 * @param	requestURI			the URI supplied in the HTTP message
	 * @param	request				the request parsed out of the the URI
	 * @param	requestType			the value XXXX of the <code>"?requestType=XXXX"</code> argument, which may be null
	 * @param	out				where to send the request response
	 * @exception					if cannot send the response
	 */
	abstract protected void generateResponseToGetRequest(DatabaseInformationModel databaseInformationModel,String rootURL,String requestURI,WebRequest request,String requestType,OutputStream out) throws IOException;

	final public void sendHeaderAndBodyOfStream(OutputStream out,InputStream in,String nameToUse,String contentType) throws IOException {
		PrintWriter writer = new PrintWriter(new OutputStreamWriter(out,"UTF-8"));
		writer.print("HTTP/1.1 200 OK\r\n");
		writer.print("Content-Type: "+contentType+"\r\n");
		//writer.print("Content-Length: "+fileLength+"\r\n");
		writer.print("Content-Disposition: filename="+nameToUse+"\r\n");
		writer.print("\r\n");
		writer.flush();
		CopyStream.copy(new BufferedInputStream(in),out);
		out.flush();
	}

	final public void sendHeaderAndBodyOfFile(OutputStream out,File file,String nameToUse,String contentType) throws IOException {
		PrintWriter writer = new PrintWriter(new OutputStreamWriter(out,"UTF-8"));
		long fileLength = file.length();
if (webServerDebugLevel > 0) System.err.println("RequestHandler.sendHeaderAndBodyOfFile(): Length = "+fileLength);
		writer.print("HTTP/1.1 200 OK\r\n");
		writer.print("Content-Type: "+contentType+"\r\n");
		writer.print("Content-Length: "+fileLength+"\r\n");
		writer.print("Content-Disposition: filename="+nameToUse+"\r\n");
		writer.print("\r\n");
		writer.flush();
		CopyStream.copy(new BufferedInputStream(new FileInputStream(file)),out);
		out.flush();
	}

	final public void sendHeaderAndBodyText(OutputStream out,String text,String nameToUse,String contentType) throws IOException {
		PrintWriter writer = new PrintWriter(new OutputStreamWriter(out,"UTF-8"));
		long length = text.length();
		writer.print("HTTP/1.1 200 OK\r\n");
		writer.print("Content-Type: "+contentType+"\r\n");
		writer.print("Content-Length: "+length+"\r\n");
		writer.print("Content-Disposition: filename="+nameToUse+"\r\n");
		writer.print("\r\n");
		writer.print(text);
		writer.flush();
	}

	static final public void send404NotFound(OutputStream out,String message) {
		try {
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(out,"UTF-8"));
			writer.print("HTTP/1.1 404 Not Found - "+message+"\r\n");
			writer.flush();
		}
		catch (IOException e) {
			e.printStackTrace(System.err);
		}
	}
}



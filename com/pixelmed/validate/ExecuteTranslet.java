/* Copyright (c) 2001-2005, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.validate;

import java.io.File;

// JAXP packages
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;

/**
 * <p>Use a translet from an XSL-T source file to transform one XML file to another.</p>
 *
 * @author	dclunie
 */
public class ExecuteTranslet {
	/***/
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/validate/ExecuteTranslet.java,v 1.1 2005/08/21 16:18:51 dclunie Exp $";

	/**
	 * <p>Apply the XSL-T translet.</p>
	 *
	 * @param	arg	the name of the class file containing the XSL-T translet, the name of the imput XML file and the name of the output file
	 */
	public static void main(String arg[]) {
		try {
			Source transformSource = new StreamSource(arg[0]);
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer(transformSource);
			StreamSource inputSource = new StreamSource(arg[1]);
			StreamResult outputResult = new StreamResult(new File(arg[2]));
			transformer.transform(inputSource,outputResult);
			
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
}


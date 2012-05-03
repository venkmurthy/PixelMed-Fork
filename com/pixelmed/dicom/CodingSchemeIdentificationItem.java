/* Copyright (c) 2001-2010, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.*;
import java.util.*;

/**encoded in composite instances within CodingSchemeIdentificationSequence.</p>
 *
 * @author	dclunie
 */
public class CodingSchemeIdentificationItem {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/CodingSchemeIdentificationItem.java,v 1.1 2010/11/28 18:10:16 dclunie Exp $";

	protected String codingSchemeDesignator;
	protected String codingSchemeRegistry;
	protected String codingSchemeUID;
	protected String codingSchemeName;
		
	public CodingSchemeIdentificationItem(String codingSchemeDesignator,String codingSchemeRegistry,String codingSchemeUID,String codingSchemeName) {
		this.codingSchemeDesignator=codingSchemeDesignator;
		this.codingSchemeRegistry=codingSchemeRegistry;
		this.codingSchemeUID=codingSchemeUID;
		this.codingSchemeName=codingSchemeName;
	}
	
	public CodingSchemeIdentificationItem(SequenceItem item) {
		if (item != null) {
			AttributeList list = item.getAttributeList();
			codingSchemeDesignator = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.CodingSchemeDesignator);
			codingSchemeRegistry = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.CodingSchemeRegistry);
			codingSchemeUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.CodingSchemeUID);
			codingSchemeName = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.CodingSchemeName);
		}
	}

	public String getCodingSchemeDesignator() { return codingSchemeDesignator;}
	public String getCodingSchemeRegistry() { return codingSchemeRegistry;}
	public String getCodingSchemeUID() { return codingSchemeUID;}
	public String getCodingSchemeName() { return codingSchemeName;}
	
	public SequenceItem getAsSequenceItem() {
		AttributeList list = new AttributeList();
		try {
		{ Attribute a = new ShortStringAttribute(TagFromName.CodingSchemeDesignator); a.addValue(codingSchemeDesignator); list.put(a); }
		{ Attribute a = new LongStringAttribute(TagFromName.CodingSchemeRegistry); a.addValue(codingSchemeRegistry); list.put(a); }
		{ Attribute a = new UniqueIdentifierAttribute(TagFromName.CodingSchemeUID); a.addValue(codingSchemeUID); list.put(a); }
		{ Attribute a = new ShortTextAttribute(TagFromName.CodingSchemeName); a.addValue(codingSchemeName); list.put(a); }
		}
		catch (DicomException e) {
			e.printStackTrace(System.err);
		}
		
		SequenceItem item = new SequenceItem(list);
		return item;
	}
}


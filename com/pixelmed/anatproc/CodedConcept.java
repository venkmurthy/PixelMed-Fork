/* Copyright (c) 2001-2010, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.anatproc;

import com.pixelmed.dicom.CodedSequenceItem;
import com.pixelmed.dicom.DicomException;

import com.pixelmed.utils.StringUtilities;

/**
 * <p>This class represents a concept that has a coded representation.</p>
 * 
 * @author	dclunie
 */
public class CodedConcept extends Concept {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/anatproc/CodedConcept.java,v 1.3 2010/04/08 22:38:41 dclunie Exp $";
	
	protected String codingSchemeDesignator;			// e.g., "SRT"
	protected String legacyCodingSchemeDesignator;		// e.g.  "SNM3" if what is used in DICOM context groups instead of "SRT"; null if none required (i.e., same as codingSchemeDesignator)
	protected String codingSchemeVersion;				// null if none required
	protected String codeValue;
	protected String codeMeaning;
	
	protected String codeStringEquivalent;
	
	protected String[] synonynms;						// may be null or empty
	
	public CodedConcept(String conceptUniqueIdentifier,String codingSchemeDesignator,String legacyCodingSchemeDesignator,String codingSchemeVersion,String codeValue,String codeMeaning,String codeStringEquivalent,String[] synonynms) {
		super(conceptUniqueIdentifier);
		this.codingSchemeDesignator=codingSchemeDesignator;
		this.legacyCodingSchemeDesignator=legacyCodingSchemeDesignator;
		this.codingSchemeVersion=codingSchemeVersion;
		this.codeValue=codeValue;
		this.codeMeaning=codeMeaning;
		this.codeStringEquivalent=codeStringEquivalent;
		this.synonynms=synonynms;
	}
	
	protected CodedConcept() {};
	
	public String getCodingSchemeDesignator() { return codingSchemeDesignator; }
	
	public String getLegacyCodingSchemeDesignator() { return legacyCodingSchemeDesignator; }
	
	public String getCodingSchemeVersion() { return codingSchemeVersion; }
	
	public String getCodeValue() { return codeValue; }
	
	public String getCodeMeaning() { return codeMeaning; }
	
	public String getCodeStringEquivalent() { return codeStringEquivalent; }
	
	public String[] getSynonyms() { return synonynms; }
	
	public CodedSequenceItem getCodedSequenceItem() throws DicomException {
		CodedSequenceItem item;
		if (codingSchemeVersion != null && codingSchemeVersion.length() > 0) {
			item = new CodedSequenceItem(codeValue,codingSchemeDesignator,codingSchemeVersion,codeMeaning);
		}
		else {
			item = new CodedSequenceItem(codeValue,codingSchemeDesignator,codeMeaning);
		}
		return item;
	}
	
	public String getCodeAsString() {
		StringBuffer buf = new StringBuffer();
		buf.append("(");
		buf.append(codeValue);
		buf.append(",");
		if (legacyCodingSchemeDesignator == null) {
			buf.append(codingSchemeDesignator);
		}
		else {
			buf.append(legacyCodingSchemeDesignator);
			buf.append(" {");
			buf.append(codingSchemeDesignator);
			buf.append("}");
		}
		if (codingSchemeVersion != null) {
			buf.append(" [");
			buf.append(codingSchemeVersion);
			buf.append("]");
		}
		buf.append(",\"");
		buf.append(codeMeaning);
		buf.append("\")");
		return buf.toString();
	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(super.toString());
		buf.append("\tcode: ");
		buf.append(getCodeAsString());
		buf.append("\n");
		buf.append("\tcodeStringEquivalent: ");
		buf.append(codeStringEquivalent);
		buf.append("\n");
		buf.append("\tsynonynms: ");
		buf.append(StringUtilities.toString(synonynms));
		buf.append("\n");
		return buf.toString();
	}
	
	public String toStringBrief() {
		StringBuffer buf = new StringBuffer();
		buf.append("CUI: ");
		buf.append(conceptUniqueIdentifier);
		buf.append(" ");
		buf.append(getCodeAsString());
		return buf.toString();
	}

	//public boolean equals(CodedSequenceItem item) {
	//	if (item != null) {
	//		String itemcsd = item.getCodingSchemeDesignator();
	//		return (itemcsd != null && (legacyCodingSchemeDesignator != null && legacyCodingSchemeDesignator.equals(itemcsd) || codingSchemeDesignator.equals(itemcsd)))
	//		     && codeValue.equals(item.getCodeValue());
	//	}
	//	else {
	//		return false;
	//	}
	//}
}


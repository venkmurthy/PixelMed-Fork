/* Copyright (c) 2001-2007, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.anatproc;

import com.pixelmed.dicom.AttributeList;

/**
 * <p>This class represents anatomic concepts that may be encoded and displayed.</p>
 * 
 * @author	dclunie
 */
public class DisplayableAnatomicConcept extends DisplayableConcept {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/anatproc/DisplayableAnatomicConcept.java,v 1.1 2007/06/22 19:59:11 dclunie Exp $";
	
	protected boolean pairedStructure;					// if true, then Left or Right or Both are permitted, otherwise always only Unpaired laterality
														// Note that ideally this would be a characteristic of an AnatomicConcept perse, regardless of whether it was encodable or displayable

	public DisplayableAnatomicConcept(String conceptUniqueIdentifier,
			boolean pairedStructure,
			String codingSchemeDesignator,String legacyCodingSchemeDesignator,String codingSchemeVersion,String codeValue,String codeMeaning,String codeStringEquivalent,String[] synonynms,
			String[] shortcutMenuEntry,String[] fullyQualifiedMenuEntry
			) {
		super(conceptUniqueIdentifier,codingSchemeDesignator,legacyCodingSchemeDesignator,codingSchemeVersion,codeValue,codeMeaning,codeStringEquivalent,synonynms,shortcutMenuEntry,fullyQualifiedMenuEntry);
		this.pairedStructure=pairedStructure;
	}
	
	protected DisplayableAnatomicConcept() {};
	
	public boolean isPairedStructure() { return pairedStructure; }
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(super.toString());
		buf.append("\tisPairedStructure: ");
		buf.append(pairedStructure);
		buf.append("\n");
		return buf.toString();
	}
	
}


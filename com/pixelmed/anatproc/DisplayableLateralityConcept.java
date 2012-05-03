/* Copyright (c) 2001-2007, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.anatproc;

import com.pixelmed.dicom.AttributeList;

/**
 * <p>This class represents view position concepts that may be encoded and displayed.</p>
 * 
 * @author	dclunie
 */
public class DisplayableLateralityConcept extends DisplayableConcept {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/anatproc/DisplayableLateralityConcept.java,v 1.1 2007/07/10 20:38:49 dclunie Exp $";
	
	public DisplayableLateralityConcept(String conceptUniqueIdentifier,
			String codingSchemeDesignator,String legacyCodingSchemeDesignator,String codingSchemeVersion,String codeValue,String codeMeaning,String codeStringEquivalent,String[] synonynms,
			String[] shortcutMenuEntry,String[] fullyQualifiedMenuEntry
			) {
		super(conceptUniqueIdentifier,codingSchemeDesignator,legacyCodingSchemeDesignator,codingSchemeVersion,codeValue,codeMeaning,codeStringEquivalent,synonynms,shortcutMenuEntry,fullyQualifiedMenuEntry);
	}
	
	protected DisplayableLateralityConcept() {};

}


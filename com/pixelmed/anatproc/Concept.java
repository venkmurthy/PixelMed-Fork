/* Copyright (c) 2001-2011, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.anatproc;

/**
 * <p>This class represents a concpet that has a coded representation.</p>
 * 
 * @author	dclunie
 */
public class Concept {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/anatproc/Concept.java,v 1.2 2011/05/04 11:46:35 dclunie Exp $";
	
	protected String conceptUniqueIdentifier;		// UMLS CUID ?
	
	public String getConceptUniqueIdentifier() { return conceptUniqueIdentifier; }
	
	public Concept(String conceptUniqueIdentifier) {
		this.conceptUniqueIdentifier=conceptUniqueIdentifier;
	}
	
	protected Concept() {};
	
	public boolean equals(Object o) {
		boolean areEqual = false;
		if (this == o) {
			areEqual = true;
		}
		else if (o != null && o instanceof Concept) {
			areEqual = conceptUniqueIdentifier.equals(((Concept)o).conceptUniqueIdentifier);
		}
		return areEqual;
	}
	
	public int hashCode() {
		return conceptUniqueIdentifier.hashCode();
	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("\tCUI: ");
		buf.append(conceptUniqueIdentifier);
		buf.append("\n");
		return buf.toString();
	}
}


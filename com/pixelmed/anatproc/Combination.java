/* Copyright (c) 2001-2011, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.anatproc;

import com.pixelmed.dicom.AttributeList;

/**
 * @author	dclunie
 */
class Combination {
	Concept parent;
	Concept[] children;
		
	Combination(Concept parent,Concept[] children) {
		this.parent = parent;
		this.children = children;
	}
	
	boolean containsOrIsSelf(Concept candidate) {
//System.err.println("Combination.containsOrIsSelf(): comparing self "+parent+" with "+candidate);
		boolean match = false;
		if (parent.equals(candidate)) {
//System.err.println("Combination.containsOrIsSelf(): equals self");
			match = true;
		}
		else {
			for (Concept child : children) {
				if (child.equals(candidate)) {
//System.err.println("Combination.containsOrIsSelf(): found matching child");
					match = true;
					break;
				}
			}
		}
		return match;
	}
}

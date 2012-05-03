/* Copyright (c) 2001-2011, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.anatproc;

import com.pixelmed.dicom.AttributeList;

/**
 * <p>This class supports anatomic concepts that may be combinations of one another.</p>
 * 
 * @author	dclunie
 */
public class CombinedAnatomicConcepts extends DisplayableConcept {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/anatproc/CombinedAnatomicConcepts.java,v 1.1 2011/05/04 11:46:35 dclunie Exp $";
		
	// new Concept("416949008"/*"Abdomen and Pelvis"*/)
	// new Concept("416550000"/*"Chest and Abdomen"*/)
	// new Concept("416775004"/*"Chest, Abdomen and Pelvis"*/)
	// new Concept("774007"   /*"Head and Neck"*/)
	// new Concept("417437006"/*"Neck and Chest"*/)
	// new Concept("416152001"/*"Neck, Chest and Abdomen"*/)
	// new Concept("416319003"/*"Neck, Chest, Abdomen and Pelvis"*/)
	
	// new Concept("12738006"/*"Brain"*/)
	// new Concept("69536005"/*"Head"*/)
	// new Concept("45048000"/*"Neck"*/)
	// new Concept("C0817096"/*"Chest"*/)
	// new Concept("39607008"/*"Lung"*/)
	// new Concept("C0000726"/*"Abdomen"*/)
	// new Concept("12921003"/*"Pelvis"*/)
	
	protected static Concept[] newConceptArray(Concept... values) { return values; }

	// combinations array is searched in successive order, so more stringent matches should come first ...
	protected static Combination[] combinations = {
		// deal with what are essentially synonyms first (to prevent expanding body part due to synonyms in broader combinations) ...
		
		new Combination(new Concept("69536005"/*"Head"*/),	newConceptArray(new Concept("12738006"/*"Brain"*/))),
		new Combination(new Concept("C0817096"/*"Chest"*/),	newConceptArray(new Concept("39607008"/*"Lung"*/))),
		
		// deal with two pair combinations ...
		new Combination(new Concept("416949008"/*"Abdomen and Pelvis"*/), newConceptArray(
				new Concept("C0000726"/*"Abdomen"*/),
				new Concept("12921003"/*"Pelvis"*/)
			)
		),
		new Combination(new Concept("774007"   /*"Head and Neck"*/),      newConceptArray(
				new Concept("12738006"/*"Brain"*/),
				new Concept("69536005"/*"Head"*/),
				new Concept("45048000"/*"Neck"*/)
			)
		),
		new Combination(new Concept("417437006"/*"Neck and Chest"*/),     newConceptArray(
				new Concept("45048000"/*"Neck"*/),
				new Concept("C0817096"/*"Chest"*/),
				new Concept("39607008"/*"Lung"*/)
			)
		),
		new Combination(new Concept("416550000"/*"Chest and Abdomen"*/),  newConceptArray(
				new Concept("C0817096"/*"Chest"*/),
				new Concept("39607008"/*"Lung"*/),
				new Concept("C0000726"/*"Abdomen"*/)
			)
		),
		
		// deal with three pair combinations ...
		new Combination(new Concept("416775004"/*"Chest, Abdomen and Pelvis"*/), newConceptArray(
				new Concept("C0817096"/*"Chest"*/),
				new Concept("39607008"/*"Lung"*/),
				new Concept("C0000726"/*"Abdomen"*/),
				new Concept("12921003"/*"Pelvis"*/),
				new Concept("416550000"/*"Chest and Abdomen"*/),
				new Concept("416949008"/*"Abdomen and Pelvis"*/)
			)
		),
		new Combination(new Concept("416152001"/*"Neck, Chest and Abdomen"*/), newConceptArray(
				new Concept("45048000"/*"Neck"*/),
				new Concept("C0817096"/*"Chest"*/),
				new Concept("39607008"/*"Lung"*/),
				new Concept("C0000726"/*"Abdomen"*/),
				new Concept("417437006"/*"Neck and Chest"*/), 
				new Concept("416550000"/*"Chest and Abdomen"*/)
			)
		),
		
		// deal with four pair combinations ...
		new Combination(new Concept("416319003"/*"Neck, Chest, Abdomen and Pelvis"*/), newConceptArray(
				new Concept("45048000"/*"Neck"*/),
				new Concept("C0817096"/*"Chest"*/),
				new Concept("39607008"/*"Lung"*/),
				new Concept("C0000726"/*"Abdomen"*/),
				new Concept("12921003"/*"Pelvis"*/),
				new Concept("417437006"/*"Neck and Chest"*/), 
				new Concept("416550000"/*"Chest and Abdomen"*/),
				new Concept("416949008"/*"Abdomen and Pelvis"*/),
				new Concept("416775004"/*"Chest, Abdomen and Pelvis"*/),
				new Concept("416152001"/*"Neck, Chest and Abdomen"*/)
			)
		)
	};

	/**
	 * <p>Combine two concepts into a single concept containing both if possible.</p>
	 *
	 * @param	a	one concept
	 * @param	b	another concept
	 * @return		a combined concept if it exists, else null
	 */
	public static Concept getCombinedConcept(Concept a,Concept b) {
//System.err.println("CombinedAnatomicConcepts.getCombinedConcept(): comparing "+a+" with "+b);
		Concept combined = null;
		if (a.equals(b)) {
//System.err.println("CombinedAnatomicConcepts.getCombinedConcept(): both concepts are equal");
			return a;
		}
		else {
			for (Combination combination : combinations) {
				if (combination.containsOrIsSelf(a) && combination.containsOrIsSelf(b)) {
					combined = combination.parent;
//System.err.println("CombinedAnatomicConcepts.getCombinedConcept(): found combination "+combined);
					break;
				}
			}
		}
		
		return combined;
	}

	/**
	 * <p>Combine two concepts into a single concept containing both if possible.</p>
	 *
	 * @param	a		one concept
	 * @param	b		another concept
	 * @param	dict	dictionary of concepts to lookup
	 * @return			a combined concept if it exists and is present in the dictionary, else null
	 */
	public static DisplayableConcept getCombinedConcept(Concept a,Concept b,DictionaryOfConcepts dict) {
//System.err.println("CombinedAnatomicConcepts.getCombinedConcept(): using dictionary of "+dict.getDescriptionOfConcept()+" comparing "+a+" with "+b);
		DisplayableConcept displayableCombined = null;
		if (dict != null) {
			Concept combined = getCombinedConcept(a,b);
			if (combined != null) {
//System.err.println("CombinedAnatomicConcepts.getCombinedConcept(): found combined concept "+combined+" now looking up in dictionary");
				displayableCombined = dict.find(combined);
			}
		}
		return displayableCombined;
	}
}


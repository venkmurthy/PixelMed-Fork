/* Copyright (c) 2001-2010, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.*;
import java.util.*;

/**
 * <p>A class to encapsulate the attributes related to Coding Scheme Identification encoded in composite instances within CodingSchemeIdentificationSequence.</p>
 *
 * <p>Includes a "dictionary" of various commonly used coding schemes and mappings between their CodingSchemeDesignators and OIDs, as well
 * as convenience methods to add and extract what coding schemes are used within an instance.</p>
 *
 * @author	dclunie
 */
public class CodingSchemeIdentification {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/CodingSchemeIdentification.java,v 1.1 2010/11/28 18:10:16 dclunie Exp $";

	protected List<CodingSchemeIdentificationItem> listOfItems;

	public static final String REGISTRY_HL7 = "HL7";
	
	public static final CodingSchemeIdentificationItem SRT = new CodingSchemeIdentificationItem("SRT",REGISTRY_HL7,"2.16.840.1.113883.6.96","SNOMED-CT using SNOMED-RT style values");
	public static final CodingSchemeIdentificationItem I10P = new CodingSchemeIdentificationItem("I10P",REGISTRY_HL7,"2.16.840.1.113883.6.4","ICD-10 Procedure Coding System");

	protected static CodingSchemeIdentificationItem[] knownCodingSchemes = {
		I10P,
		SRT
	};
	
	protected static CodingSchemeIdentificationItem lookupByCodingSchemeDesignator(String csd) {
		CodingSchemeIdentificationItem found = null;
		if (csd != null && csd.length() > 0) {
			for (CodingSchemeIdentificationItem item: knownCodingSchemes) {
				if (item.getCodingSchemeDesignator().equals(csd)) {
					found = item;
					break;
				}
			}
		}
		return found;
	}

	/**
	 * <p>Construct a CodingSchemeIdentification from the CodingSchemeIdentificationSequence in the supplied list.</p>
	 *
	 * @param	list	the list in which to look for the CodingSchemeIdentificationSequence attribute
	 */
	public CodingSchemeIdentification(AttributeList list) {
		if (list != null) {
			Attribute a = list.get(TagFromName.CodingSchemeIdentificationSequence);
			if (a != null && a instanceof SequenceAttribute) {
				SequenceAttribute csis = (SequenceAttribute)a;
				int n = csis.getNumberOfItems();
				if (n > 0) {
					listOfItems = new ArrayList<CodingSchemeIdentificationItem>();
					for (int i=0; i<n; ++i) {
						SequenceItem si = csis.getItem(i);
						CodingSchemeIdentificationItem item = new CodingSchemeIdentificationItem(si);
						listOfItems.add(item);
					}
				}
			}
		}
	}
	
	/**
	 * <p>Construct a CodingSchemeIdentification from a list of CodingSchemeIdentificationItems.</p>
	 *
	 * @param	listOfItems		may be null if none (yet)
	 */
	public CodingSchemeIdentification(List<CodingSchemeIdentificationItem> listOfItems) {
		this.listOfItems = listOfItems;
	}
	
	/**
	 * <p>Get as a CodingSchemeIdentificationSequence attribute.</p>
	 *
	 * @return		a SequenceAttribute with one item per coding scheme, or null if no coding schemes
	 */
	public SequenceAttribute getAsSequenceAttribute() {
		SequenceAttribute a = null;
		if (listOfItems != null) {
			a = new SequenceAttribute(TagFromName.CodingSchemeIdentificationSequence);
			for (CodingSchemeIdentificationItem item: listOfItems) {
				SequenceItem si = item.getAsSequenceItem();
//System.err.println("CodingSchemeIdentificationItem.getAsSequenceAttribute(): adding "+si);
				a.addItem(si);
			}
		}
		return a;
	}
	
	/**
	 * <p>Get details of a particular coding scheme by looking up by CodingSchemeDesignator value.</p>
	 *
	 * @return		a CodingSchemeIdentificationItem, or null if not found
	 */
	public CodingSchemeIdentificationItem getByCodingSchemeDesignator(String codingSchemeDesignator) {
		CodingSchemeIdentificationItem found = null;
		if (listOfItems != null && codingSchemeDesignator != null && codingSchemeDesignator.length() > 0) {
			for (CodingSchemeIdentificationItem item: listOfItems) {
//System.err.println("CodingSchemeIdentificationItem.getByCodingSchemeDesignator(): checking "+item);
				if (item.getCodingSchemeDesignator().equals(codingSchemeDesignator)) {
//System.err.println("CodingSchemeIdentificationItem.getByCodingSchemeDesignator(): found "+item);
					found = item;
				}
			}
		}
		return found;
	}
	
	protected static void recursivelyCollectCodingSchemeDesignators(AttributeList list,Set<String> schemesFound) {
		Iterator i = list.values().iterator();
		while (i.hasNext()) {
			Object o = i.next();
			if (o instanceof SequenceAttribute) {
				SequenceAttribute a = (SequenceAttribute)o;
				Iterator items = a.iterator();
				if (items != null) {
					while (items.hasNext()) {
						SequenceItem item = (SequenceItem)(items.next());
						if (item != null) {
							AttributeList itemAttributeList = item.getAttributeList();
							if (itemAttributeList != null) {
								recursivelyCollectCodingSchemeDesignators(itemAttributeList,schemesFound);
							}
						}
					}
				}
			}
			else {
				Attribute a = (Attribute)o;
				AttributeTag tag = a.getTag();
				if (tag.equals(TagFromName.CodingSchemeDesignator)) {
					String csd = a.getSingleStringValueOrEmptyString();
					if (csd.length() > 0) {
//System.err.println("CodingSchemeIdentificationItem.recursivelyCollectCodingSchemeDesignators(): added "+csd);
						schemesFound.add(csd);
					}
				}
			}
		}
	}

	/**
	 * <p>Build a new CodingSchemeIdentification instance by examing all uses of CodedSequenceItems within the supplied list.</p>
	 *
	 * @param	list	the list in which to look for the CodedSequenceItem attribute
	 * @return		a new CodingSchemeIdentification, or null if none found
	 */
	public static CodingSchemeIdentification getCodingSchemesFromExistingAttributeList(AttributeList list) {
//System.err.print("CodingSchemeIdentificationItem.getCodingSchemesFromExistingAttributeList(): list ="+list);
		Set<String> schemesFound = new TreeSet<String>();
		recursivelyCollectCodingSchemeDesignators(list,schemesFound);
		
		List<CodingSchemeIdentificationItem> listOfItems = null;
		for (String csd: schemesFound) {
//System.err.println("CodingSchemeIdentificationItem.getCodingSchemesFromExistingAttributeList(): found "+csd);
			CodingSchemeIdentificationItem item = lookupByCodingSchemeDesignator(csd);
			if (item != null) {
				if (listOfItems == null) {
					listOfItems = new ArrayList<CodingSchemeIdentificationItem>();
				}
				listOfItems.add(item);
			}
		}
		
		return new CodingSchemeIdentification(listOfItems);
	}


}


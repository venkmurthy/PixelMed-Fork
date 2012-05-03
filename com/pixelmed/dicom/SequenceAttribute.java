/* Copyright (c) 2001-2003, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.*;
import java.util.*;

/**
 * <p>A concrete class specializing {@link com.pixelmed.dicom.Attribute Attribute} for
 * Sequence (SQ) attributes.</p>
 *
 * <p>Though an instance of this class may be created
 * using its constructors, there is also a factory class, {@link com.pixelmed.dicom.AttributeFactory AttributeFactory}.</p>
 *
 * @see com.pixelmed.dicom.SequenceItem
 * @see com.pixelmed.dicom.Attribute
 * @see com.pixelmed.dicom.AttributeFactory
 * @see com.pixelmed.dicom.AttributeList
 *
 * @author	dclunie
 */
public class SequenceAttribute extends Attribute {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/SequenceAttribute.java,v 1.17 2008/02/21 04:46:18 dclunie Exp $";

	private LinkedList itemList;		// each member is a SequenceItem

	/**
	 * <p>Construct an (empty) attribute.</p>
	 *
	 * @param	t	the tag of the attribute
	 */
	public SequenceAttribute(AttributeTag t) {
		super(t);
		itemList=new LinkedList();
		valueLength=0xffffffffl;	// for the benefit of writebase();
	}

	// no constructor for input stream ... done manually elsewhere

	/**
	 * @param	o
	 * @exception	IOException
	 * @exception	DicomException
	 */
	public void write(DicomOutputStream o) throws DicomException, IOException {
		writeBase(o);			// valueLength should be 0xffffffff from constructor

		Iterator i = iterator();
		while (i.hasNext()) {
			SequenceItem item = (SequenceItem)i.next();
			item.write(o);
		}
		
		o.writeUnsigned16(0xfffe);	// Sequence Delimiter
		o.writeUnsigned16(0xe0dd);
		o.writeUnsigned32(0);		// dummy length
	}
	
	/***/
	public String toString(DicomDictionary dictionary) {
		StringBuffer str = new StringBuffer();
		str.append(super.toString(dictionary));
		str.append("\n%seq\n");
		Iterator i = iterator();
		while (i.hasNext()) {
			str.append(((SequenceItem)i.next()).toString(dictionary));
			str.append("\n");
		}
		str.append("%endseq");
		return str.toString();
	}

	/**
	 * @exception	DicomException
	 */
	public void removeValues() {
		itemList=null;
		valueMultiplicity=0;
		valueLength=0;
	}

	/**
	 * Add an item to the sequence (after any existing items).
	 *
	 * @param	item
	 */
	public void addItem(SequenceItem item) {
		itemList.addLast(item);
	}

	/**
	 * Add an item to the sequence (after any existing items).
	 *
	 * @param	item	the list of attributes that comprise the item
	 */
	public void addItem(AttributeList item) {
		itemList.addLast(new SequenceItem(item));
	}

	/**
	 * Add an item to the sequence (after any existing items), keeping tracking of input byte offsets.
	 *
	 * @param	item		the list of attributes that comprise the item
	 * @param	byteOffset	the byte offset in the input stream of the start of the item
	 */
	public void addItem(AttributeList item,long byteOffset) {
		itemList.addLast(new SequenceItem(item,byteOffset));
	}

	/**
	 * Get an {@link java.util.Iterator Iterator} of the items in the sequence.
	 *
	 * @return	a {@link java.util.Iterator Iterator} of items, each encoded as an {@link com.pixelmed.dicom.SequenceItem SequenceItem}
	 */
	public Iterator iterator() {
		return itemList.listIterator(0);
	}

	/**
	 * Get the number of items in the sequence.
	 *
	 * @return	the number of items
	 */
	public int getNumberOfItems() {
		return itemList.size();
	}

	/**
	 * Get particular item in the sequence.
	 *
	 * @param	index	which item to return, numbered from zero
	 * @return		a {@link com.pixelmed.dicom.SequenceItem SequenceItem}, null if no items or no such item
	 */
	public SequenceItem getItem(int index) {
		return (itemList == null || index >= itemList.size()) ? null : (SequenceItem)itemList.get(index);
	}

	/**
	 * <p>Get the value representation of this attribute (SQ).</p>
	 *
	 * @return	'S','Q' in ASCII as a two byte array; see {@link com.pixelmed.dicom.ValueRepresentation ValueRepresentation}
	 */
	public byte[] getVR() { return ValueRepresentation.SQ; }

	/**
	 * <p>Extract the AttributeList of the first item from a sequence.</p>
	 *
	 * @param	sequenceAttribute	the sequence attribute that has one item (may be null in which case returns null)
	 * @return						the AttributeList if found else null
	 */
	public static AttributeList getAttributeListFromWithinSequenceWithSingleItem(SequenceAttribute sequenceAttribute) {
		AttributeList slist = null;
		if (sequenceAttribute != null) {
			// assert sequenceAttribute.getNumberOfItems() >= 1
			// assert sequenceAttribute.getNumberOfItems() == 1
			Iterator sitems = sequenceAttribute.iterator();
			if (sitems.hasNext()) {
				SequenceItem sitem = (SequenceItem)sitems.next();
				if (sitem != null) {
					slist = sitem.getAttributeList();
				}
			}
		}
		return slist;
	}

	/**
	 * <p>Extract the AttributeList of the first item from a specified sequence from within a list of attributes.</p>
	 *
	 * @param	list		the list that contains the sequence (may not be null)
	 * @param	sequenceTag	the tag of the sequence attribute that has one item
	 * @return				the AttributeList if found else null
	 */
	public static AttributeList getAttributeListFromWithinSequenceWithSingleItem(AttributeList list,AttributeTag sequenceTag) {
		SequenceAttribute sequenceAttribute = (SequenceAttribute)list.get(sequenceTag);
		return getAttributeListFromWithinSequenceWithSingleItem(sequenceAttribute);
	}
	
	/**
	 * <p>Extract the specified attribute from the first item of the specified sequence.</p>
	 *
	 * @param	sequenceAttribute	the sequence attribute that has one item (may be null in which case returns null)
	 * @param	namedTag		the tag of the attribute within the item of the sequence
	 * @return				the attribute if found else null
	 */
	public static Attribute getNamedAttributeFromWithinSequenceWithSingleItem(SequenceAttribute sequenceAttribute,AttributeTag namedTag) {
		Attribute a = null;
		if (sequenceAttribute != null) {
			AttributeList slist = getAttributeListFromWithinSequenceWithSingleItem(sequenceAttribute);
			if (slist != null) {
				a=slist.get(namedTag);
			}
		}
		return a;
	}

	/**
	 * <p>Extract the specified attribute from within the first item of the specified sequence from within a list of attributes.</p>
	 *
	 * @param	list		the list that contains the sequence (may not be null)
	 * @param	sequenceTag	the tag of the sequence attribute that has one item
	 * @param	namedTag	the tag of the attribute within the item of the sequence
	 * @return				the attribute if found else null
	 */
	public static Attribute getNamedAttributeFromWithinSequenceWithSingleItem(AttributeList list,AttributeTag sequenceTag,AttributeTag namedTag) {
		SequenceAttribute sequenceAttribute = (SequenceAttribute)list.get(sequenceTag);
		return getNamedAttributeFromWithinSequenceWithSingleItem(sequenceAttribute,namedTag);
	}
	
	/**
	 * <p>Extract the code meaning attribute value from within the first item of the specified code sequence from within a list of attributes.</p>
	 *
	 * @param	list		the list that contains the code sequence (may not be null)
	 * @param	tag			the tag of the code sequence attribute that has one item
	 * @param	dflt		what to return if there is no such sequence attribute or it is empty or has no code meaning attribute
	 */
	public static String getMeaningOfCodedSequenceAttributeOrDefault(AttributeList list,AttributeTag tag,String dflt) {
		String meaning=dflt;
		Attribute a=getNamedAttributeFromWithinSequenceWithSingleItem(list,tag,TagFromName.CodeMeaning);
		if (a != null) {
			meaning = a.getSingleStringValueOrDefault(dflt);
		}
		return meaning;
	}
	

}


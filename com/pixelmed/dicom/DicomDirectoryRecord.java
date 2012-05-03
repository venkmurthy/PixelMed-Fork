/* Copyright (c) 2001-2011, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.util.*;
import javax.swing.tree.*;

/**
 * @author	dclunie
 */
public abstract class DicomDirectoryRecord implements Comparable, TreeNode {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/DicomDirectoryRecord.java,v 1.19 2012/04/05 03:52:12 dclunie Exp $";

	DicomDirectoryRecord parent;
	Collection children;
	TreeNode[] array;
	AttributeList list;
	
	protected String uid;
	protected String stringValue;
	protected int integerValue;

	// Methods to implement Comparable (allows parent to sort)

	/**
	 * @param	o
	 */
	public int compareTo(Object o) {
//System.err.println("DicomDirectoryRecord.compareTo(): comparing classes "+this.getClass()+" with "+o.getClass());
		return compareToThatReturnsZeroOnlyIfSameObject(o);
	}
	
	// Establish that the natural ordering is NOT consistent with equals,
	// otherwise when two records are read that have the same Attributes
	// but different children in the DICOMDIR, one will be overwritten if
	// added to a set (e.g., if the DICOMDIR contains "duplicate"
	// PATIENT records, one for each STUDY, rather than having merged
	// them during creation
	
	// the consequences of natural ordering not being consistent with equals
	// is discussed in the JavaDoc for java.lang.Comparable
	
	// also, be sure that none of the sub-classes of DicomDirectoryRecord in
	// DicomDirectoryRecordFactory do NOT override equals() (though they may
	// override compareTo()

	/**
	 * @param	o
	 */
	public boolean equals(Object o) {
		boolean areEqual = super.equals(o);
//System.err.println("DicomDirectoryRecord.equals(): comparing classes "+this.getClass()+" with "+o.getClass()+" with result "+areEqual);
		return areEqual;
		//return compareTo(o) == 0;
	}

	// Methods to help with Comparable support
	
	/***/
	abstract protected void makeStringValue();

	/***/
	abstract protected void makeIntegerValue();

	/***/
	protected String getStringValue() {
		return stringValue;
	}

	/***/
	protected int getIntegerValue() {
		return integerValue;
	}

	/***/
	protected final String getUIDForComparison() { return uid; }

	/**
	 * @param	o
	 */
	private int compareToThatReturnsZeroOnlyIfSameObject(Object o) {	// private so that this cannot be overridden
//System.err.println("DicomDirectoryRecord.compareToThatReturnsZeroOnlyIfSameObject(): comparing classes "+this.getClass()+" with "+o.getClass());
//new Throwable().printStackTrace();
//System.err.println("DicomDirectoryRecord.compareToThatReturnsZeroOnlyIfSameObject(): super.equals(o) = "+super.equals(o));
		return super.equals(o) ? 0 : 1;	// no particular order unless class is specialized, but can return 0 when identical objects (not otherwise else conflict in Set)
	}
	
	/**
	 * @param	record
	 */
	protected final int compareToByStringValue(DicomDirectoryRecord record,boolean mustBeSameObjectToBeEqual) {
//System.err.println("DicomDirectoryRecord.compareToByStringValue(): comparing classes "+this.getClass()+" with "+record.getClass());
		if (this.getClass().equals(record.getClass())) {
//System.err.println("DicomDirectoryRecord.compareToByStringValue(): same class");
			int strComparison = toString().compareTo(record.toString());
//System.err.println("DicomDirectoryRecord.compareToByStringValue(): strComparison = "+strComparison);
			if (strComparison == 0) {
				int uidComparison = getUIDForComparison().compareTo(record.getUIDForComparison());
//System.err.println("DicomDirectoryRecord.compareToByStringValue(): uidComparison = "+uidComparison);
				if (uidComparison == 0) {
					// same UIDs (or no UIDs)
					return mustBeSameObjectToBeEqual ? compareToThatReturnsZeroOnlyIfSameObject(record) : 0;
				}
				else {
					return uidComparison;	// same string but different UID; distinguish and order consistently
				}
			}
			else {
				return strComparison;
			}
		}
		else {
			return toString().compareTo(record.toString());	// includes name of record type, hence will always be different and no need for compareToThatReturnsZeroOnlyIfSameObject() check
		}
	}

	/**
	 * @param	record
	 */
	protected final int compareToByIntegerValue(DicomDirectoryRecord record,boolean mustBeSameObjectToBeEqual) {
//System.err.println("DicomDirectoryRecord.compareToByIntegerValue(): comparing classes "+this.getClass()+" with "+record.getClass());
		if (this.getClass().equals(record.getClass())) {
			int intComparison = getIntegerValue() - record.getIntegerValue();
			if (intComparison == 0) {
				int strComparison = toString().compareTo(record.toString());
				if (strComparison == 0) {
					int uidComparison = getUIDForComparison().compareTo(record.getUIDForComparison());
					if (uidComparison == 0) {
						// same UIDs (or no UIDs)
						return mustBeSameObjectToBeEqual ? compareToThatReturnsZeroOnlyIfSameObject(record) : 0;
					}
					else {
						return uidComparison;	// same integer and string but different UID; distinguish and order consistently
					}
				}
				else {
					return strComparison;		// same integer values but different string; distinguish and order consistently
				}
			}
			else {
				return intComparison;
			}
		}
		else {
			return toString().compareTo(record.toString());	// includes name of record type, hence will always be different and no need for compareToThatReturnsZeroOnlyIfSameObject() check
		}
	}

	// Methods to implement TreeNode ...

	/**
	 * <p>Returns the parent node of this node.</p>
	 *
	 * @return	the parent node, or null if the root
	 */
	public TreeNode getParent() {
		return parent;
	}

	/**
	 * <p>Returns the child at the specified index.</p>
	 *
	 * @param	index	the index of the child to be returned, numbered from 0
	 * @return		the child <code>TreeNode</code> at the specified index
	 */
	public TreeNode getChildAt(int index) {
		int n=children.size();
		if (array == null) {
			array=(TreeNode[])(children.toArray(new TreeNode[n]));	// explicitly allocated to set returned array type correctly
		}
		return index < n ? array[index] : null;
	}

	/**
	 * <p>Returns the index of the specified child from amongst this node's children, if present.</p>
	 *
	 * @param	child	the child to search for amongst this node's children
	 * @return		the index of the child, or -1 if not present
	 */
	public int getIndex(TreeNode child) {
//System.err.println("getIndexOfChild: looking for "+child);
		if (children != null) {
			int n=children.size();
			if (array == null) {
				array=(TreeNode[])(children.toArray(new TreeNode[n]));	// explicitly allocated to set returned array type correctly
			}
			for (int i=0; i<n; ++i) {
				if (((DicomDirectoryRecord)getChildAt(i)).compareToByStringValue((DicomDirectoryRecord)child,false/*mustBeSameObjectToBeEqual*/) == 0) {	// expensive comparison ? :(; just require string, not object, match
//System.err.println("getIndexOfChild: found "+child);
					return i;
				}
			}
		}
		return -1;
	}

	/**
	 * <p> Always returns true, since children may always be added.</p>
	 *
	 * @return	always true
	 */
	public boolean getAllowsChildren() {
		return true;
	}

	/**
	 * <p> Returns true if the receiver is a leaf (has no children).</p>
	 *
	 * @return	true if the receiver is a leaf
	 */
	public boolean isLeaf() {
		return getChildCount() == 0;
	}

	/**
	 * <p>Return the number of children that this node contains.</p>
	 *
	 * @return	the number of children, 0 if none
	 */
	public int getChildCount() {
		return children == null ? 0 : children.size();
	}

	/**
	 * <p>Returns the children of this node as an {@link java.util.Enumeration Enumeration}.</p>
	 *
	 * @return	the children of this node
	 */
	public Enumeration children() {
		return children == null ? null : new Vector(children).elements();
	}

	// Methods specific to this kind of node ...

	/**
	 * @param	p
	 * @param	l
	 */
	public DicomDirectoryRecord(DicomDirectoryRecord p,AttributeList l) {
		parent=p;
		list=l;
		makeIntegerValue();
		makeStringValue();
	}

	/**
	 * @param	child
	 */
	public void addChild(DicomDirectoryRecord child) {
		if (children == null) children=new TreeSet();	// is sorted
		children.add(child);
		array=null;					// cache is dirty
	}

	/**
	 * @param	child
	 */
	public void removeChild(DicomDirectoryRecord child) {
		children.remove(child);
		array=null;					// cache is dirty
	}

	/**
	 * @param	sibling
	 * @exception	DicomException
	 */
	public void addSibling(DicomDirectoryRecord sibling) throws DicomException {
		if (parent == null) {
			throw new DicomException("Internal error - root node with sibling");
		}
		else {
			parent.addChild(sibling);
		}
	}

	/**
	 * <p>Set the parent node of this node.</p>
	 *
	 * @param	parent
	 */
	public void setParent(DicomDirectoryRecord parent) {
		this.parent = parent;
	}

	/***/
	public AttributeList getAttributeList() { return list; }
}




/* Copyright (c) 2001-2011, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.util.*;
import javax.swing.tree.*;

/**
 * <p>An abstract class for representing a node in an internal representation of a structured reporting
 * tree (an instance of {@link com.pixelmed.dicom.StructuredReport StructuredReport}).</p>
 *
 * <p>The constructor is protected. Instances of specific types of content items should normally be created by using
 * the {@link com.pixelmed.dicom.ContentItemFactory ContentItemFactory}.</p>
 *
 * @see com.pixelmed.dicom.ContentItemFactory
 * @see com.pixelmed.dicom.StructuredReport
 * @see com.pixelmed.dicom.StructuredReportBrowser
 *
 * @author	dclunie
 */
public abstract class ContentItem implements TreeNode {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/ContentItem.java,v 1.22 2011/07/17 19:21:25 dclunie Exp $";

	/***/
	protected String valueType;
	/***/
	protected String relationshipType;
	/***/
	protected CodedSequenceItem conceptName;
	/***/
	protected String referencedContentItemIdentifier;

	ContentItem parent;
	List children;
	AttributeList list;

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
		return (TreeNode)(children.get(index));
	}

	/**
	 * <p>Returns the index of the specified child from amongst this node's children, if present.</p>
	 *
	 * @param	child	the child to search for amongst this node's children
	 * @return		the index of the child, or -1 if not present
	 */
	public int getIndex(TreeNode child) {
//System.err.println("getIndexOfChild: looking for "+child);
		int n=children.size();
		for (int i=0; i<n; ++i) {
			if (children.get(i).equals(child)) {	// expensive comparison ? :(
//System.err.println("getIndexOfChild: found "+child);
				return i;
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

	private void extractCommonAttributes() {
//System.err.println("extractCommonAttributes:");
		valueType=Attribute.getSingleStringValueOrNull(list,TagFromName.ValueType);						// NB. Use null rather than default "" to make symmetric with de novo constructor
//System.err.println("extractCommonAttributes: valueType="+valueType);
		relationshipType=Attribute.getSingleStringValueOrNull(list,TagFromName.RelationshipType);		// NB. Use null rather than default "" to make symmetric with de novo constructor
//System.err.println("extractCommonAttributes: relationshipType="+relationshipType);
		conceptName=CodedSequenceItem.getSingleCodedSequenceItemOrNull(list,TagFromName.ConceptNameCodeSequence);
//System.err.println("extractCommonAttributes: conceptName="+conceptName);
		referencedContentItemIdentifier=Attribute.getDelimitedStringValuesOrEmptyString(list,TagFromName.ReferencedContentItemIdentifier).replace('\\','.');
//System.err.println("extractCommonAttributes: referencedContentItemIdentifier="+referencedContentItemIdentifier);
	}

	/**
	 * <p>Construct a content item for a list of attributes, and add it as a child of the specified parent.</p>
	 *
	 * <p>The constructor is protected. Instances of specific types of content items should normally be created by using
	 * the {@link com.pixelmed.dicom.ContentItemFactory ContentItemFactory}.</p>
	 *
	 * @param	p	the parent
	 * @param	l	the list of attributes
	 */
	protected ContentItem(ContentItem p,AttributeList l) {
		parent=p;
		if (p != null) {
			p.addChild(this);
		}
		list=l;
		extractCommonAttributes();
	}

	/**
	 * <p>Construct a content item of a specified type and relationship, creating a new {@link com.pixelmed.dicom.AttributeList AttributeList}, and add it as a child of the specified parent.</p>
	 *
	 * <p>The constructor is protected. Instances of specific types of content items should normally be created by using
	 * the {@link com.pixelmed.dicom.ContentItemFactory ContentItemFactory}.</p>
	 *
	 * @param	p					the parent
	 * @param	valueType
	 * @param	relationshipType	added only if not null or zero length
	 * @param	conceptName
	 * @throws	DicomException
	 */
	protected ContentItem(ContentItem p,String valueType,String relationshipType,CodedSequenceItem conceptName) throws DicomException {
		parent=p;
		if (p != null) {
			p.addChild(this);
		}
		list = new AttributeList();
		this.valueType = valueType;
		{ Attribute a = new CodeStringAttribute(TagFromName.ValueType); a.addValue(valueType); list.put(a); }
		this.relationshipType = relationshipType;
		if (relationshipType != null && relationshipType.length() > 0) {
			Attribute a = new CodeStringAttribute(TagFromName.RelationshipType); a.addValue(relationshipType); list.put(a);
		}
		this.conceptName = conceptName;
		if (conceptName != null) {
			SequenceAttribute a = new SequenceAttribute(TagFromName.ConceptNameCodeSequence); a.addItem(conceptName.getAttributeList()); list.put(a);
		}
		referencedContentItemIdentifier = null;
	}

	/**
	 * <p>Add a child to this content item.</p>
	 *
	 * @param	child		the child content item to add
	 */
	public void addChild(ContentItem child) {
//System.err.println("ContentItem.addChild(): child = "+child);
		if (children == null) children=new LinkedList();
		children.add(child);
	}

	/**
	 * <p>Add a sibling to this content item (a child to the parent of this content item).</p>
	 *
	 * @param	sibling		the sibling content item to add
	 * @exception	DicomException	thrown if there is no parent
	 */
	public void addSibling(ContentItem sibling) throws DicomException {
		if (parent == null) {
			throw new DicomException("Internal error - root node with sibling");
		}
		else {
			parent.addChild(sibling);
		}
	}

	/**
	 * <p>Get the parent content item of this content item.</p>
	 *
	 * <p>This method saves the caller from having to cast the value returned from {@link javax.swing.tree.TreeNode#getParent() TreeNode.getParent()}.</p>
	 *
	 * @return	the parent content item
	 */
	public ContentItem getParentAsContentItem() { return parent; }

	/**
	 * <p>Get the attribute list of this content item.</p>
	 *
	 * @return	the attribute list of this content item
	 */
	public AttributeList getAttributeList() { return list; }

	/**
	 * <p>Get the value type of this content item.</p>
	 *
	 * @return	the value type (the string used in the DICOM standard in the Value Type attribute)
	 */
	public String getValueType()                { return valueType; }

	/**
	 * <p>Get the relationship type of this content item.</p>
	 *
	 * @return	the relationship type (the string used in the DICOM standard in the Relationship Type attribute)
	 */
	public String getRelationshipType()                { return relationshipType; }

	/**
	 * <p>Get the Referenced SOP Class UID of this content item, if present and applicable.</p>
	 *
	 * @return	the Referenced SOP Class UID, or null
	 */
	public String getReferencedSOPClassUID()    { return null; }

	/**
	 * <p>Get the Referenced SOP Instance UID of this content item, if present and applicable.</p>
	 *
	 * @return	the Referenced SOP Instance UID, or null
	 */
	public String getReferencedSOPInstanceUID() { return null; }

	/**
	 * <p>Get the Graphic Type of this content item, if present and applicable.</p>
	 *
	 * @return	the Graphic Type, or null
	 */
	public String getGraphicType()              { return null; }

	/**
	 * <p>Get the Graphic Data of this content item, if present and applicable.</p>
	 *
	 * @return	the Graphic Data, or null
	 */
	public float[] getGraphicData()             { return null; }

	/**
	 * <p>Get a string representation of the concept name and the value of the concept.</p>
	 *
	 * <p>The exact form of the returned string is specific to the type of ContentItem.</p>
	 *
	 * @return	a String representation of the name and value, or an empty string
	 */
	public String getConceptNameAndValue() {
		return getConceptNameCodeMeaning()+" "+getConceptValue();
	}

	/**
	 * <p>Get a string representation of the value of the concept.</p>
	 *
	 * <p>The exact form of the returned string is specific to the type of ContentItem.</p>
	 *
	 * @return	a String representation of the name and value, or an empty string
	 */
	abstract public String getConceptValue();

	/**
	 * <p>Get the Concept Name.</p>
	 *
	 * @return	the Concept Name
	 */
	public CodedSequenceItem getConceptName()      { return conceptName; }

	/**
	 * <p>Get the value of the code meaning of the Concept Name as a string, if present and applicable.</p>
	 *
	 * @return	the code meaning of the Concept Name, or an empty string
	 */
	public String getConceptNameCodeMeaning()      { return conceptName == null ? "" : conceptName.getCodeMeaning(); }

	/**
	 * <p>Get the value of the code value of the Concept Name as a string, if present and applicable.</p>
	 *
	 * @return	the code value of the Concept Name, or an empty string
	 */
	public String getConceptNameCodeValue()      { return conceptName == null ? "" : conceptName.getCodeValue(); }

	/**
	 * <p>Get the value of the coding scheme designator of the Concept Name as a string, if present and applicable.</p>
	 *
	 * @return	the coding scheme designator of the Concept Name, or an empty string
	 */
	public String getConceptNameCodingSchemeDesignator()      { return conceptName == null ? "" : conceptName.getCodingSchemeDesignator(); }

	/**
	 * <p>Get the Referenced Content Item Identifier, if present.</p>
	 *
	 * @return	the period (not backslash) delimited item references, or an empty string
	 */
	public String getReferencedContentItemIdentifier()      { return referencedContentItemIdentifier == null ? "" : referencedContentItemIdentifier; }

	/**
	 * <p>Get the Referenced Content Item Identifier, if present.</p>
	 *
	 * @return	an array of integers representing the separated components of the Referenced Content Item Identifier, including the first (root) identifier of 1, or null if none or empty
	 */
	public int[] getReferencedContentItemIdentifierArray() {
		int[] intArray = null;
		if (referencedContentItemIdentifier != null && referencedContentItemIdentifier.length() > 0) {
			//String[] itemNumbers = referencedContentItemIdentifier.split("\\\\");
			String[] stringArray = referencedContentItemIdentifier.split("[.]");
			if (stringArray != null && stringArray.length > 0) {
				intArray = new int[stringArray.length];
				for (int i=0; i<stringArray.length; i++) {
					intArray[i] = Integer.parseInt(stringArray[i]);
				}
			}
		}
		return intArray;
	}

	/**
	 * <p>Get a human-readable string representation of the content item.</p>
	 *
	 * @return	the string representation of the content item
	 */
	public String toString() {
		return (referencedContentItemIdentifier == null || referencedContentItemIdentifier.length() == 0 ? "" : "R-")
			+ (relationshipType == null ? "" : relationshipType) + ": "
			+ (valueType == null || valueType.length() == 0 ? "" : (valueType + ": "))
			+ (conceptName == null ? "" : conceptName.getCodeMeaning())
			+ (referencedContentItemIdentifier == null || referencedContentItemIdentifier.length() == 0 ? "" : referencedContentItemIdentifier)
			;
		//return 
		//  (conceptName == null ? "" : conceptName.getCodeMeaning())
		//+ (referencedContentItemIdentifier == null || referencedContentItemIdentifier.length() == 0 ? "" : referencedContentItemIdentifier)
		//;
	}
	
	// Convenience methods
	
	/**
	 * Retrieve the named child as defined by its ConceptName
	 *
	 * @param	codingSchemeDesignator
	 * @param	codeValue
	 * @return							the (first, if multiple) named child, or null if absent
	 */
	public ContentItem getNamedChild(String codingSchemeDesignator,String codeValue) {
		ContentItem child = null;
		if (codingSchemeDesignator != null && codeValue != null) {
			int n = getChildCount();
			for (int i=0; i<n; ++i) {
				ContentItem test = (ContentItem)getChildAt(i);
				if (test != null) {
					String csd = test.getConceptNameCodingSchemeDesignator();
					String cv = test.getConceptNameCodeValue();
					if (csd != null && csd.equals(codingSchemeDesignator) && cv != null && cv.equals(codeValue)) {
						child = test;
						break;
					}
				}
			}
		}
		return child;
	}
	
	/**
	 * Retrieve the named child as defined by its ConceptName
	 *
	 * The code meaning of the concept is ignored, and only the code value and coding scheme designator are compared in the search.
	 *
	 * @param	item
	 * @return							the (first, if multiple) named child, or null if absent
	 */
	public ContentItem getNamedChild(CodedSequenceItem item) {
		String codingSchemeDesignator = item.getCodingSchemeDesignator();
		String codeValue = item.getCodeValue();
		ContentItem child = null;
		if (codingSchemeDesignator != null && codeValue != null) {
			int n = getChildCount();
			for (int i=0; i<n; ++i) {
				ContentItem test = (ContentItem)getChildAt(i);
				if (test != null) {
					String csd = test.getConceptNameCodingSchemeDesignator();
					String cv = test.getConceptNameCodeValue();
					if (csd != null && csd.equals(codingSchemeDesignator) && cv != null && cv.equals(codeValue)) {
						child = test;
						break;
					}
				}
			}
		}
		return child;
	}

	/**
	 * Retrieve the string value of self
	 *
	 * @return							the value , or null if absent
	 */
	public String getSingleStringValueOrNull() {
		String value = null;
		if (this instanceof ContentItemFactory.StringContentItem) {
			value = ((ContentItemFactory.StringContentItem)this).getConceptValue();
		}
		else if (this instanceof ContentItemFactory.CodeContentItem) {
			value = ((ContentItemFactory.CodeContentItem)this).getConceptValue();		// will return CodeMeaning
		}
		else if (this instanceof ContentItemFactory.NumericContentItem) {
			value = ((ContentItemFactory.NumericContentItem)this).getNumericValue();	// NOT getConceptValue(), which includes the units
		}
		return value;
	}

	/**
	 * Retrieve the string value of the named child as defined by its ConceptName
	 *
	 * @param	codingSchemeDesignator
	 * @param	codeValue
	 * @return							the value of the (first, if multiple) named child, or null if absent
	 */
	public String getSingleStringValueOrNullOfNamedChild(String codingSchemeDesignator,String codeValue) {
		String value = null;
		{
			ContentItem child = getNamedChild(codingSchemeDesignator,codeValue);
			if (child != null) {
				value = child.getSingleStringValueOrNull();
			}
		}
		return value;
	}

	/**
	 * Retrieve the string value of the named child as defined by its ConceptName
	 *
	 * @param	parent
	 * @param	codingSchemeDesignator
	 * @param	codeValue
	 * @return							the value of the (first, if multiple) named child, or null if absent
	 */
	public static String getSingleStringValueOrNullOfNamedChild(ContentItem parent,String codingSchemeDesignator,String codeValue) {
		String value = null;
		if (parent != null) {
			value = parent.getSingleStringValueOrNullOfNamedChild(codingSchemeDesignator,codeValue);
		}
		return value;
	}


	/**
	 * Test if the coded concept name of the content item matches the specified code value and coding scheme designator.
	 *
	 * This is more robust than checking code meaning, which may have synomyms, and there is no need to also test code meaning.
	 *
	 * @param	csdWanted
	 * @param	cvWanted
	 * @return					true if matches
	 */
	public boolean contentItemNameMatchesCodeValueAndCodingSchemeDesignator(String cvWanted,String csdWanted) {
		boolean isMatch = false;
		if (conceptName != null) {
			String csd = conceptName.getCodingSchemeDesignator();
			String cv = conceptName.getCodeValue();
			if (csd != null && csd.trim().equals(csdWanted.trim()) && cv != null && cv.trim().equals(cvWanted.trim())) {
				isMatch = true;
			}
		}
		return isMatch;
	}
	
	/**
	 * Test if the coded concept name of the content item matches the specified code value and coding scheme designator.
	 *
	 * This is more robust than checking code meaning, which may have synomyms, and there is no need to also test code meaning.
	 *
	 * @param	ci
	 * @param	csdWanted
	 * @param	cvWanted
	 * @return					true if matches
	 */
	public static boolean contentItemNameMatchesCodeValueAndCodingSchemeDesignator(ContentItem ci,String cvWanted,String csdWanted) {
		boolean isMatch = false;
		if (ci != null) {
			isMatch = ci.contentItemNameMatchesCodeValueAndCodingSchemeDesignator(cvWanted,csdWanted);
		}
		return isMatch;
	}
}




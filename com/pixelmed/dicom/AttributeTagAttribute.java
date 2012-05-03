/* Copyright (c) 2001-2009, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.*;

import java.text.NumberFormat;

/**
 * <p>A concrete class specializing {@link com.pixelmed.dicom.Attribute Attribute} for
 * Attribute Tag (AT) attributes.</p>
 *
 * <p>Though an instance of this class may be created
 * using its constructors, there is also a factory class, {@link com.pixelmed.dicom.AttributeFactory AttributeFactory}.</p>
 *
 * @see com.pixelmed.dicom.Attribute
 * @see com.pixelmed.dicom.AttributeFactory
 * @see com.pixelmed.dicom.AttributeList
 *
 * @author	dclunie
 */
public class AttributeTagAttribute extends Attribute {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/AttributeTagAttribute.java,v 1.15 2009/12/16 22:10:37 dclunie Exp $";

	int[] groups;
	int[] elements;

	static int bytesPerValue=4;

	/**
	 * <p>Construct an (empty) attribute.</p>
	 *
	 * @param	t	the tag of the attribute
	 */
	public AttributeTagAttribute(AttributeTag t) {
		super(t);
		groups=null;
		elements=null;
	}

	/**
	 * <p>Read an attribute from an input stream.</p>
	 *
	 * @param	t			the tag of the attribute
	 * @param	vl			the value length of the attribute
	 * @param	i			the input stream
	 * @exception	IOException
	 * @exception	DicomException
	 */
	public AttributeTagAttribute(AttributeTag t,long vl,DicomInputStream i) throws IOException, DicomException {
		super(t);
		doCommonConstructorStuff(vl,i);
	}
	
	/**
	 * <p>Read an attribute from an input stream.</p>
	 *
	 * @param	t			the tag of the attribute
	 * @param	vl			the value length of the attribute
	 * @param	i			the input stream
	 * @exception	IOException
	 * @exception	DicomException
	 */
	public AttributeTagAttribute(AttributeTag t,Long vl,DicomInputStream i) throws IOException, DicomException {
		super(t);
		doCommonConstructorStuff(vl.longValue(),i);
	}
	
	/**
	 * @param	vl
	 * @param	i
	 * @exception	IOException
	 * @exception	DicomException
	 */
	private void doCommonConstructorStuff(long vl,DicomInputStream i) throws IOException, DicomException {
		valueLength=vl;
		if (valueLength%bytesPerValue != 0) throw new DicomException("incorrect value length for VR "+getVR());
		int vm=(int)(valueLength/bytesPerValue);

		//valueMultiplicity=vm;
		//values=new short[valueMultiplicity];
		//for (int j=0; j<valueMultiplicity; ++j) values[j]=(short)(i.readUnsigned16());

		groups=null;
		elements=null;
		for (int j=0; j<vm; ++j) {
			int g = ((int)(i.readUnsigned16())&0xffff);
			int e = ((int)(i.readUnsigned16())&0xffff);
			addValue(g,e);
		}
	}

	/**
	 * @param	o
	 * @exception	IOException
	 * @exception	DicomException
	 */
	public void write(DicomOutputStream o) throws DicomException, IOException {
		writeBase(o);
		for (int j=0; j<valueMultiplicity; ++j) {
			o.writeUnsigned16(groups[j]);
			o.writeUnsigned16(elements[j]);
		}
	}
	
	/***/
	public String toString(DicomDictionary dictionary) {
		StringBuffer str = new StringBuffer();
		str.append(super.toString(dictionary));
		str.append(" [");
		try {
			if (groups != null && elements != null) {
				for (int j=0; j<groups.length; ++j) {
					if (j > 0) str.append(",");
					AttributeTag.toString(groups[j],elements[j]);
				}
			}
		}
		catch (Exception e) {
			str.append("XXXX");
		}
		str.append("]");
		return str.toString();
	}

	/**
	 * @param	format		the format to use for each numerical or decimal value
	 * @exception	DicomException
	 */
	public String[] getStringValues(NumberFormat format) throws DicomException {
		// ignore number format
		String sv[] = null;
		if (groups != null && elements != null) {
			sv=new String[groups.length];
			for (int j=0; j<groups.length; ++j) {
				sv[j]=AttributeTag.toString(groups[j],elements[j]);
			}
		}
		return sv;
	}
	
	/**
	 * @exception	DicomException
	 */
	public AttributeTag[] getAttributeTagValues() throws DicomException {
		AttributeTag atv[] = null;
		if (groups != null && elements != null) {
			atv=new AttributeTag[groups.length];
			for (int j=0; j<groups.length; ++j) {
				atv[j]=new AttributeTag(groups[j],elements[j]);
			}
		}
		return atv;
	}

	/**
	 * @param	g
	 * @param	e
	 * @exception	DicomException
	 */
	public void addValue(int g,int e) throws DicomException {
		groups=ArrayCopyUtilities.expandArray(groups);
		groups[valueMultiplicity]=g;
		elements=ArrayCopyUtilities.expandArray(elements);
		elements[valueMultiplicity++]=e;
		valueLength=valueMultiplicity*4;
	}

	/**
	 * @param	t
	 * @exception	DicomException
	 */
	public void addValue(AttributeTag t) throws DicomException {
		addValue(t.getGroup(),t.getElement());
	}

	/**
	 * @param	s	a String of the form returned by {@link com.pixelmed.dicom.AttributeTag#toString() toString()}, i.e., "(0xgggg,0xeeee)" where gggg and eeee are the zero-padded hexadecimal representations of the group and element respectively
	 * @exception	DicomException
	 */
	public void addValue(String s) throws DicomException {
		addValue(new AttributeTag(s));
	}

	/**
	 * @exception	DicomException
	 */
	public void removeValues() {
		groups=null;
		elements=null;
		valueMultiplicity=0;
		valueLength=0;
	}
	
	/**
	 * <p>Get the value representation of this attribute (AT).</p>
	 *
	 * @return	'A','T' in ASCII as a two byte array; see {@link com.pixelmed.dicom.ValueRepresentation ValueRepresentation}
	 */
	public byte[] getVR() { return ValueRepresentation.AT; }

}


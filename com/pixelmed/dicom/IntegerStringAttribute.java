/* Copyright (c) 2001-2009, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.*;

import java.text.NumberFormat;

/**
 * <p>A concrete class specializing {@link com.pixelmed.dicom.Attribute Attribute} for
 * Integer String (IS) attributes.</p>
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
public class IntegerStringAttribute extends StringAttribute {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/IntegerStringAttribute.java,v 1.12 2009/10/22 10:35:07 dclunie Exp $";

	/**
	 * <p>Construct an (empty) attribute.</p>
	 *
	 * @param	t	the tag of the attribute
	 */
	public IntegerStringAttribute(AttributeTag t) {
		super(t);
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
	public IntegerStringAttribute(AttributeTag t,long vl,DicomInputStream i) throws IOException, DicomException {
		super(t,vl,i);
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
	public IntegerStringAttribute(AttributeTag t,Long vl,DicomInputStream i) throws IOException, DicomException {
		super(t,vl.longValue(),i);
	}

	/**
	 * <p>Get the value representation of this attribute (IS).</p>
	 *
	 * @return	'I','S' in ASCII as a two byte array; see {@link com.pixelmed.dicom.ValueRepresentation ValueRepresentation}
	 */
	public byte[] getVR() { return ValueRepresentation.IS; }

        /**
	 * @param	format		the format to use for each numerical or decimal value
         * @exception	DicomException
         */
        public String[] getStringValues(NumberFormat format) throws DicomException {
		String sv[] = null;
		if (format == null) {
			sv=super.getStringValues((NumberFormat)null);
		}
		else {
			long[] v = getLongValues();
			if (v != null) {
				sv=new String[v.length];
				for (int j=0; j<v.length; ++j) {
					sv[j] = format.format(v[j]);
				}
			}
		}
		return sv;
	}

	// do not need to override addValue() for shorter binary integer arguments; super-class methods will never exceed 12 bytes or range -2^31 <= n <= (2^31 -  1).

	/**
	 * @param	v
	 * @exception	DicomException	if value is beyond range permitted for IntegerString
	 */
	public void addValue(long v) throws DicomException {
		if (v < Integer.MIN_VALUE || v > Integer.MAX_VALUE) {
			throw new DicomException("Value "+v+" out of range for Integer String");
		}
		else {
			addValue((int)v);
		}
	}

	/**
	 * @param	v
	 * @exception	DicomException	if value is beyond range permitted for IntegerString
	 */
	public void addValue(float v) throws DicomException {
		if (v < Integer.MIN_VALUE || v > Integer.MAX_VALUE) {
			throw new DicomException("Value "+v+" out of range for Integer String");
		}
		else {
			addValue((int)v);
		}
	}

	/**
	 * @param	v
	 * @exception	DicomException	if value is beyond range permitted for IntegerString
	 */
	public void addValue(double v) throws DicomException {
		if (v < Integer.MIN_VALUE || v > Integer.MAX_VALUE) {
			throw new DicomException("Value "+v+" out of range for Integer String");
		}
		else {
			addValue((int)v);
		}
	}

	private static double[] testValues = {
		0,
		1,
		Double.MAX_VALUE,
		Double.MIN_VALUE,
		Float.MAX_VALUE,
		Float.MIN_VALUE,
		Long.MAX_VALUE,
		Long.MIN_VALUE,
		Integer.MAX_VALUE,
		Integer.MIN_VALUE,
		Short.MAX_VALUE,
		Short.MIN_VALUE,
		Byte.MAX_VALUE,
		Byte.MIN_VALUE
	};
	
	private static String[] testStringSupplied = {
		"0",
		"1",
		"Double.MAX_VALUE",
		"Double.MIN_VALUE",
		"Float.MAX_VALUE",
		"Float.MIN_VALUE",
		"9223372036854775807",
		"-9223372036854775808",
		"2147483647",
		"-2147483648",
		"32767",
		"-32768",
		"127",
		"-128"
	};
		
	private static String[] testStringExpected = {
		"0",
		"1",
		"exception",
		"0",
		"exception",
		"0",
		"exception",
		"exception",
		"2147483647",
		"-2147483648",
		"32767",
		"-32768",
		"127",
		"-128"
	};
		
	public static void main(String arg[]) {
		System.err.println("Test IntegerString.addValue(double):");
		for (int i=0; i< testValues.length; ++i) {
			IntegerStringAttribute a = new IntegerStringAttribute(TagFromName.InstanceNumber);
			String sv = "";
			try {
				a.addValue(testValues[i]);
				sv = Attribute.getSingleStringValueOrEmptyString(a);
			}
			catch (DicomException e) {
				//System.err.println(e);
				sv = "exception";
			}
			int svl = sv.length();
			System.err.println("\t"+(sv.equals(testStringExpected[i]) && svl <= 12 ? "PASS" : "FAIL")+": Supplied <"+testStringSupplied[i]+">\t Got <"+sv+"> (length="+svl+")\t Expected <"+testStringExpected[i]+">\t Double.toString() <"+Double.toString(testValues[i])+">");
		}
		System.err.println("Test IntegerString.addValue(float):");
		for (int i=0; i< testValues.length; ++i) {
			IntegerStringAttribute a = new IntegerStringAttribute(TagFromName.InstanceNumber);
			String sv = "";
			try {
				a.addValue((float)testValues[i]);
				sv = Attribute.getSingleStringValueOrEmptyString(a);
			}
			catch (DicomException e) {
				//System.err.println(e);
				sv = "exception";
			}
			int svl = sv.length();
			System.err.println("\t"+(sv.equals(testStringExpected[i]) && svl <= 12 ? "PASS" : "FAIL")+": Supplied <"+testStringSupplied[i]+">\t Got <"+sv+"> (length="+svl+")\t Expected <"+testStringExpected[i]+">\t Double.toString() <"+Double.toString(testValues[i])+">");
		}
		System.err.println("Test IntegerString.addValue(long):");
		for (int i=0; i< testValues.length; ++i) {
			IntegerStringAttribute a = new IntegerStringAttribute(TagFromName.InstanceNumber);
			String sv = "";
			try {
				a.addValue((long)testValues[i]);
				sv = Attribute.getSingleStringValueOrEmptyString(a);
			}
			catch (DicomException e) {
				//System.err.println(e);
				sv = "exception";
			}
			int svl = sv.length();
			System.err.println("\t"+(sv.equals(testStringExpected[i]) && svl <= 12 ? "PASS" : "FAIL")+": Supplied <"+testStringSupplied[i]+">\t Got <"+sv+"> (length="+svl+")\t Expected <"+testStringExpected[i]+">\t Double.toString() <"+Double.toString(testValues[i])+">");
		}
	}
}


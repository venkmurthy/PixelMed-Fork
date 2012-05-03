/* Copyright (c) 2001-2011, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.*;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * <p>A concrete class specializing {@link com.pixelmed.dicom.Attribute Attribute} for
 * Decimal String (DS) attributes.</p>
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
public class DecimalStringAttribute extends StringAttribute {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/DecimalStringAttribute.java,v 1.12 2012/02/01 18:29:00 dclunie Exp $";

	/**
	 * <p>Construct an (empty) attribute.</p>
	 *
	 * @param	t	the tag of the attribute
	 */
	public DecimalStringAttribute(AttributeTag t) {
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
	public DecimalStringAttribute(AttributeTag t,long vl,DicomInputStream i) throws IOException, DicomException {
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
	public DecimalStringAttribute(AttributeTag t,Long vl,DicomInputStream i) throws IOException, DicomException {
		super(t,vl,i);
	}

	/**
	 * <p>Get the value representation of this attribute (DS).</p>
	 *
	 * @return	'D','S' in ASCII as a two byte array; see {@link com.pixelmed.dicom.ValueRepresentation ValueRepresentation}
	 */
	public byte[] getVR() { return ValueRepresentation.DS; }

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
			double[] v = getDoubleValues();
			if (v != null) {
				sv=new String[v.length];
				for (int j=0; j<v.length; ++j) {
					sv[j] = format.format(v[j]);
				}
			}
		}
		return sv;
	}

	// do not need to override addValue() for shorter binary integer arguments; super-class methods will never exceed 16 bytes

	/**
	 * @param	v
	 * @exception	DicomException
	 */
	public void addValue(long v) throws DicomException {
		// need to make sure we create the highest precision value that is no more than 16 bytes (limit on DS)
		addValue(com.pixelmed.utils.FloatFormatter.toStringOfFixedMaximumLength(v,16,false/*allowNonNumbers*/,Locale.US));
	}

	/**
	 * @param	v
	 * @exception	DicomException
	 */
	public void addValue(float v) throws DicomException {
		// need to make sure we create the highest precision value that is no more than 16 bytes (limit on DS)
		addValue(com.pixelmed.utils.FloatFormatter.toStringOfFixedMaximumLength(v,16,false/*allowNonNumbers*/,Locale.US));
	}

	/**
	 * @param	v
	 * @exception	DicomException
	 */
	public void addValue(double v) throws DicomException {
		// need to make sure we create the highest precision value that is no more than 16 bytes (limit on DS)
		addValue(com.pixelmed.utils.FloatFormatter.toStringOfFixedMaximumLength(v,16,false/*allowNonNumbers*/,Locale.US));
	}
	
	// for unit tests of addValue(), see com.pixelmed.utils.FloatFormatter.main()
}


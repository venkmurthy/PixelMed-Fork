/* Copyright (c) 2001-2010, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.test;

import com.pixelmed.dicom.*;

import junit.framework.*;

import java.io.File;

public class TestCodingSchemeIdentification extends TestCase {
	
	// constructor to support adding tests to suite ...
	
	public TestCodingSchemeIdentification(String name) {
		super(name);
	}
	
	// add tests to suite manually, rather than depending on default of all test...() methods
	// in order to allow adding TestCodingSchemeIdentification.suite() in AllTests.suite()
	// see Johannes Link. Unit Testing in Java pp36-47
	
	public static Test suite() {
		TestSuite suite = new TestSuite("TestCodingSchemeIdentification");
		
		suite.addTest(new TestCodingSchemeIdentification("TestCodingSchemeIdentification_ClassMethod"));
		
		return suite;
	}
	
	protected void setUp() {
	}
	
	protected void tearDown() {
	}
	
	public void TestCodingSchemeIdentification_ClassMethod() throws Exception {
		String codeValue1 = "CW3NYZZ";
		String codingSchemeDesignator1 = "I10P";
		String codingSchemeVersion1 = "2011";
		String codeMeaning1 = "PET Whole Body";
		
		String codingSchemeRegistry1 = "HL7";
		String codingSchemeUID1 = "2.16.840.1.113883.6.4";
		String codingSchemeName1 = "ICD-10 Procedure Coding System";
	
		String codeValue2 = "T-A0100";
		String codingSchemeDesignator2 = "SRT";
		String codeMeaning2 = "Brain";
		
		String codingSchemeRegistry2 = "HL7";
		String codingSchemeUID2 = "2.16.840.1.113883.6.96";
		String codingSchemeName2 = "SNOMED-CT using SNOMED-RT style values";
	
		AttributeList list = new AttributeList();
		{
			SequenceAttribute a = new SequenceAttribute(TagFromName.ProcedureCodeSequence);
			CodedSequenceItem item = new CodedSequenceItem(codeValue1,codingSchemeDesignator1,codingSchemeVersion1,codeMeaning1);
			a.addItem(item.getAttributeList());
			list.put(a);
		}
		{
			SequenceAttribute a = new SequenceAttribute(TagFromName.AnatomicRegionSequence);
			CodedSequenceItem item = new CodedSequenceItem(codeValue2,codingSchemeDesignator2,codeMeaning2);
			a.addItem(item.getAttributeList());
			list.put(a);
		}
	
		{
			CodingSchemeIdentification csi = CodingSchemeIdentification.getCodingSchemesFromExistingAttributeList(list);
			assertTrue("Checking CodingSchemeIdentification was constructed",csi != null);
			SequenceAttribute a = csi.getAsSequenceAttribute();
			assertTrue("Checking CodingSchemeIdentification returned SequenceAttribute",a != null);
			assertTrue("Checking CodingSchemeIdentification returned SequenceAttribute with one item",a.getNumberOfItems() == 2);
			if (a != null) {
				list.put(a);
			}
		}

		{
			CodingSchemeIdentification csi = new CodingSchemeIdentification(list);	// extracts the encoded value of CodingSchemeIdentificationSequence
			CodingSchemeIdentificationItem item = csi.getByCodingSchemeDesignator(codingSchemeDesignator1);
			assertTrue("Checking CodingSchemeIdentification contains item",item != null);
			assertEquals("Checking CodingSchemeIdentification item codeValue",codingSchemeDesignator1,item.getCodingSchemeDesignator());
			assertEquals("Checking CodingSchemeIdentification item codingSchemeRegistry",codingSchemeRegistry1,item.getCodingSchemeRegistry());
			assertEquals("Checking CodingSchemeIdentification item codingSchemeUID",codingSchemeUID1,item.getCodingSchemeUID());
			assertEquals("Checking CodingSchemeIdentification item codingSchemeName",codingSchemeName1,item.getCodingSchemeName());
			
			item = csi.getByCodingSchemeDesignator(codingSchemeDesignator2);
			assertTrue("Checking CodingSchemeIdentification contains item",item != null);
			assertEquals("Checking CodingSchemeIdentification item codeValue",codingSchemeDesignator2,item.getCodingSchemeDesignator());
			assertEquals("Checking CodingSchemeIdentification item codingSchemeRegistry",codingSchemeRegistry2,item.getCodingSchemeRegistry());
			assertEquals("Checking CodingSchemeIdentification item codingSchemeUID",codingSchemeUID2,item.getCodingSchemeUID());
			assertEquals("Checking CodingSchemeIdentification item codingSchemeName",codingSchemeName2,item.getCodingSchemeName());
			
		}
	}
	
}

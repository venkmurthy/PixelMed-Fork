/* Copyright (c) 2001-2011, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.test;

import com.pixelmed.anatproc.*;

import junit.framework.*;

import java.io.File;

public class TestAnatomyCombined extends TestCase {
	
	// constructor to support adding tests to suite ...
	
	public TestAnatomyCombined(String name) {
		super(name);
	}
	
	// add tests to suite manually, rather than depending on default of all test...() methods
	// in order to allow adding TestAnatomyCombined.suite() in AllTests.suite()
	// see Johannes Link. Unit Testing in Java pp36-47
	
	public static Test suite() {
		TestSuite suite = new TestSuite("TestAnatomyCombined");
		
		suite.addTest(new TestAnatomyCombined("TestAnatomyCombined_CT"));
		suite.addTest(new TestAnatomyCombined("TestAnatomyCombined_CT_Displayable"));
		
		return suite;
	}
	
	protected void setUp() {
	}
	
	protected void tearDown() {
	}
	
	public void TestAnatomyCombined_CT() throws Exception {
		{	
			Concept a = new Concept("C0000726");			// "Abdomen"
			Concept b = new Concept("12921003");			// "Pelvis"
			Concept combined = new Concept("416949008");	// "Abdomen and Pelvis"
			assertEquals("Checking Abdomen and Pelvis",				combined,CombinedAnatomicConcepts.getCombinedConcept(a,b));
			assertEquals("Checking Abdomen and Pelvis",				combined,CombinedAnatomicConcepts.getCombinedConcept(b,a));
			assertEquals("Checking Abdomen and Pelvis with self",	combined,CombinedAnatomicConcepts.getCombinedConcept(combined,a));
			assertEquals("Checking Abdomen and Pelvis with self",	combined,CombinedAnatomicConcepts.getCombinedConcept(combined,b));
			assertEquals("Checking Abdomen and Pelvis with self",	combined,CombinedAnatomicConcepts.getCombinedConcept(a,combined));
			assertEquals("Checking Abdomen and Pelvis with self",	combined,CombinedAnatomicConcepts.getCombinedConcept(b,combined));
		}
		{	
			Concept a = new Concept("69536005");			// "Head"
			Concept b = new Concept("45048000");			// "Neck"
			Concept combined = new Concept("774007");		// "Head and Neck"
			assertEquals("Checking Head and Neck",					combined,CombinedAnatomicConcepts.getCombinedConcept(a,b));
		}
		{	
			Concept a = new Concept("12738006");			// "Brain"
			Concept b = new Concept("45048000");			// "Neck"
			Concept combined = new Concept("774007");		// "Head and Neck"
			assertEquals("Checking Brain and Neck",					combined,CombinedAnatomicConcepts.getCombinedConcept(a,b));
		}
		{	
			Concept a = new Concept("12738006");			// "Brain"
			Concept b = new Concept("69536005");			// "Head"
			Concept combined = new Concept("69536005");		// "Head"
			assertEquals("Checking Head and Brain merge",			combined,CombinedAnatomicConcepts.getCombinedConcept(a,b));
		}
		{	
			Concept a = new Concept("C0817096");			// "Chest"
			Concept b = new Concept("39607008");			// "Lung"
			Concept combined = new Concept("C0817096");		// "Chest"
			assertEquals("Checking Chest and Lung merge",			combined,CombinedAnatomicConcepts.getCombinedConcept(a,b));
		}
		{	
			Concept a = new Concept("C0817096");			// "Chest"
			Concept b = new Concept("416949008");			// "Abdomen and Pelvis"
			Concept combined = new Concept("416775004");	// "Chest, Abdomen and Pelvis"
			assertEquals("Checking Chest, Abdomen and Pelvis merge",combined,CombinedAnatomicConcepts.getCombinedConcept(a,b));
		}
		{
			Concept a = new Concept("417437006");			// "Neck and Chest"
			Concept b = new Concept("416949008");			// "Abdomen and Pelvis"
			Concept combined = new Concept("416319003");	// "Neck, Chest, Abdomen and Pelvis"
			assertEquals("Checking Neck, Chest, Abdomen and Pelvis merge",combined,CombinedAnatomicConcepts.getCombinedConcept(a,b));
		}
		{
			Concept a = new Concept("45048000");			// "Neck"
			Concept b = new Concept("416775004");			// "Chest, Abdomen and Pelvis"
			Concept combined = new Concept("416319003");	// "Neck, Chest, Abdomen and Pelvis"
			assertEquals("Checking Neck, Chest, Abdomen and Pelvis merge",combined,CombinedAnatomicConcepts.getCombinedConcept(a,b));
		}
	}

	
	public void TestAnatomyCombined_CT_Displayable() throws Exception {
		{	
			Concept a = new Concept("C0000726");			// "Abdomen"
			Concept b = new Concept("12921003");			// "Pelvis"
			Concept combined = new Concept("416949008");	// "Abdomen and Pelvis"
			assertEquals("Checking Abdomen and Pelvis","Abdomen and Pelvis",CombinedAnatomicConcepts.getCombinedConcept(a,b,CTAnatomy.getAnatomyConcepts()).getCodeMeaning());
		}
	}
}

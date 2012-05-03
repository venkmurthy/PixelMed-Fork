/* Copyright (c) 2001-2011, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.test;

import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.SetOfDicomFiles;

import com.pixelmed.network.Association;
import com.pixelmed.network.AssociationStatusHandler;
import com.pixelmed.network.DicomNetworkException;
import com.pixelmed.network.ReceivedObjectHandler;
import com.pixelmed.network.StorageSOPClassSCPDispatcher;
import com.pixelmed.network.StorageSOPClassSCU;

import java.io.File;
import java.io.IOException;

import junit.framework.*;

public class TestCStore extends TestCase {

	protected static final int    waitIntervalWhenSleeping = 10;	// in ms
	protected static final int    port = 11119;
	protected static final String scpAET = "TESTSTORESCP";
	protected static final String scuAET = "TESTSTORESCU";

	// constructor to support adding tests to suite ...
	
	public TestCStore(String name) {
		super(name);
	}
	
	// add tests to suite manually, rather than depending on default of all test...() methods
	// in order to allow adding TestCStore.suite() in AllTests.suite()
	// see Johannes Link. Unit Testing in Java pp36-47
	
	public static Test suite() {
		TestSuite suite = new TestSuite("TestCStore");
		
		suite.addTest(new TestCStore("TestCStore_SendOne"));
		
		return suite;
	}
	
	protected void setUp() {
	}
	
	protected void tearDown() {
	}
	
	protected volatile String lastReceivedDicomFileName;
	
	protected class OurReceivedObjectHandler extends ReceivedObjectHandler {
		public void sendReceivedObjectIndication(String dicomFileName,String transferSyntax,String callingAETitle) throws DicomNetworkException, DicomException, IOException {
//System.err.println("Received: "+dicomFileName+" from "+callingAETitle+" in "+transferSyntax);
			lastReceivedDicomFileName = dicomFileName;
		}
	}

	protected volatile boolean associationReleased;
	
	private class OurAssociationStatusHandler extends AssociationStatusHandler {
		public void sendAssociationReleaseIndication(Association a) throws DicomNetworkException, DicomException, IOException {
			if (a != null) {
//System.err.println("Association "+a.getAssociationNumber()+" from "+a.getCallingAETitle()+" released");
			}
			associationReleased = true;
		}
	}
	
	public void TestCStore_SendOne() throws Exception {
		File savedImagesFolder = new File("./receivedfiles");
		
		//StorageSOPClassSCPDispatcher storageSOPClassSCPDispatcher = new StorageSOPClassSCPDispatcher(
		//	port,
		//	scpAET,
		//	savedImagesFolder,
		//	new OurReceivedObjectHandler(),
		//	0);
			
		StorageSOPClassSCPDispatcher storageSOPClassSCPDispatcher = new StorageSOPClassSCPDispatcher(
			port,
			scpAET,
			savedImagesFolder,
			null/*storedFilePathStrategy*/,
			new OurReceivedObjectHandler(),
			new OurAssociationStatusHandler(),
			null/*queryResponseGeneratorFactory*/,
			null/*retrieveResponseGeneratorFactory*/,
			null/*networkApplicationInformation*/,
			null/*presentationContextSelectionPolicy*/,
			false/*secureTransport*/,
			0/*debugLevel*/); 
			
		Thread storageSOPClassSCPDispatcherThread = new Thread(storageSOPClassSCPDispatcher);
		storageSOPClassSCPDispatcherThread.start();
		while (storageSOPClassSCPDispatcherThread.getState() != Thread.State.RUNNABLE) {
			Thread.currentThread().sleep(waitIntervalWhenSleeping);	// wait until SCP is ready, else later send may fail
		}
		while (!storageSOPClassSCPDispatcher.isReady()) {
			Thread.currentThread().sleep(waitIntervalWhenSleeping);	// wait until SCP is ready, else later send may fail
		}
		
		SetOfDicomFiles fileset = new SetOfDicomFiles();
		fileset.add("testnetworkfile1.dcm");
		
		new StorageSOPClassSCU("localhost",port,scpAET,scuAET,fileset,0/*compressionLevel*/,null/*multipleInstanceTransferStatusHandler*/,0/*debugLevel*/);
		
		while (lastReceivedDicomFileName == null) {
//System.err.println("Waiting for lastReceivedDicomFileName");
			Thread.currentThread().sleep(waitIntervalWhenSleeping);		// wait for it to arrive (needs to be volatile, since set in different thread)
		}

		while (!associationReleased) {
//System.err.println("Waiting for associationReleased");
			Thread.currentThread().sleep(waitIntervalWhenSleeping);		// wait for release (needs to be volatile, since set in different thread)
		}

		storageSOPClassSCPDispatcher.shutdown();
	}
	
}


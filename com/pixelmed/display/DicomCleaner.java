/* Copyright (c) 2001-2012, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Vector;

import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import javax.swing.border.Border;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import javax.swing.tree.TreePath;

import javax.imageio.ImageIO;

import com.pixelmed.database.DatabaseInformationModel;
import com.pixelmed.database.DatabaseTreeBrowser;
import com.pixelmed.database.DatabaseTreeRecord;
import com.pixelmed.database.PatientStudySeriesConcatenationInstanceModel;

import com.pixelmed.dicom.*;

import com.pixelmed.event.ApplicationEventDispatcher;

import com.pixelmed.display.event.StatusChangeEvent;

import com.pixelmed.display.DialogMessageLogger;

import com.pixelmed.network.UnencapsulatedExplicitStoreFindMoveGetPresentationContextSelectionPolicy;
import com.pixelmed.network.DicomNetworkException;
import com.pixelmed.network.MultipleInstanceTransferStatusHandler;
import com.pixelmed.network.MultipleInstanceTransferStatusHandlerWithFileName;
import com.pixelmed.network.NetworkApplicationConfigurationDialog;
import com.pixelmed.network.NetworkApplicationInformation;
import com.pixelmed.network.NetworkApplicationInformationFederated;
import com.pixelmed.network.NetworkApplicationProperties;
import com.pixelmed.network.PresentationAddress;
import com.pixelmed.network.PresentationContext;
import com.pixelmed.network.PresentationContextListFactory;
import com.pixelmed.network.ReceivedObjectHandler;
import com.pixelmed.network.StorageSOPClassSCPDispatcher;
import com.pixelmed.network.StorageSOPClassSCU;
import com.pixelmed.network.TransferSyntaxSelectionPolicy;

import com.pixelmed.query.QueryInformationModel;
import com.pixelmed.query.QueryTreeBrowser;
import com.pixelmed.query.QueryTreeModel;
import com.pixelmed.query.QueryTreeRecord;
import com.pixelmed.query.StudyRootQueryInformationModel;

import com.pixelmed.utils.CopyStream;
import com.pixelmed.utils.MessageLogger;

/**
 * <p>This class is an application for importing or retrieving DICOM studies,
 * cleaning them (i.e., de-identifying them or replacing UIDs, etc.), and
 * sending them elsewhere.</p>
 * 
 * <p>It is configured by use of a properties file that resides in the user's
 * home directory in <code>.com.pixelmed.display.DicomCleaner.properties</code>.</p>
 * 
 * <p>It supports import and network retrieval of uncompressed, deflate and bzip compressed,
 * and baseline JPEG compressed images (but not yet other encapsulated compressed pixel data).</p>
 * 
 * @author	dclunie
 */
public class DicomCleaner extends ApplicationFrame {

	/***/
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/DicomCleaner.java,v 1.38 2012/03/20 15:46:44 dclunie Exp $";

	protected static String resourceBundleName  = "com.pixelmed.display.DicomCleaner";
	protected static String propertiesFileName  = ".com.pixelmed.display.DicomCleaner.properties";
	
	protected static String propertyName_DicomCurrentlySelectedStorageTargetAE = "Dicom.CurrentlySelectedStorageTargetAE";
	protected static String propertyName_DicomCurrentlySelectedQueryTargetAE = "Dicom.CurrentlySelectedQueryTargetAE";
	
	protected static String rootNameForDicomInstanceFilesOnInterchangeMedia = "DICOM";
	protected static String filePrefixForDicomInstanceFilesOnInterchangeMedia = "I";
	protected static String fileSuffixForDicomInstanceFilesOnInterchangeMedia = "";
	protected static String nameForDicomDirectoryOnInterchangeMedia = "DICOMDIR";
	protected static String exportedZipFileName = "export.zip";

	protected static int textFieldLengthForQueryPatientName = 16;
	protected static int textFieldLengthForQueryPatientID = 10;
	protected static int textFieldLengthForQueryStudyDate = 8;

	protected static int textFieldLengthForReplacementPatientName = 16;
	protected static int textFieldLengthForReplacementPatientID = 10;
	protected static int textFieldLengthForReplacementAccessionNumber = 10;

	protected ResourceBundle resourceBundle;
	protected DatabaseInformationModel srcDatabase;
	protected DatabaseInformationModel dstDatabase;
	
	protected JPanel srcDatabasePanel;
	protected JPanel dstDatabasePanel;
	protected JPanel remoteQueryRetrievePanel;
	
	protected JCheckBox removeIdentityCheckBox;
	protected JCheckBox removeDescriptionsCheckBox;
	protected JCheckBox removeSeriesDescriptionsCheckBox;
	protected JCheckBox removeCharacteristicsCheckBox;
	protected JCheckBox removeDeviceIdentityCheckBox;
	protected JCheckBox removeInstitutionIdentityCheckBox;
	protected JCheckBox cleanUIDsCheckBox;
	protected JCheckBox removePrivateCheckBox;
	protected JCheckBox addContributingEquipmentCheckBox;
	protected JCheckBox removeClinicalTrialAttributesBox;
	protected JCheckBox zipExportCheckBox;
	protected JCheckBox hierarchicalExportCheckBox;

	protected JCheckBox replacePatientNameCheckBox;
	protected JCheckBox replacePatientIDCheckBox;
	protected JCheckBox replaceAccessionNumberCheckBox;
	
	protected JTextField replacementPatientNameTextField;
	protected JTextField replacementPatientIDTextField;
	protected JTextField replacementAccessionNumberTextField;

	protected JTextField queryFilterPatientNameTextField;
	protected JTextField queryFilterPatientIDTextField;
	protected JTextField queryFilterStudyDateTextField;
	
	protected JProgressBar progressBar;
	
	protected MessageLogger logger;
	
	protected NetworkApplicationProperties networkApplicationProperties;
	protected NetworkApplicationInformation networkApplicationInformation;
	
	protected QueryInformationModel currentRemoteQueryInformationModel;
	
	protected QueryTreeRecord currentRemoteQuerySelectionQueryTreeRecord;
	protected AttributeList currentRemoteQuerySelectionUniqueKeys;
	protected Attribute currentRemoteQuerySelectionUniqueKey;
	protected String currentRemoteQuerySelectionRetrieveAE;
	protected String currentRemoteQuerySelectionLevel;

	protected String ourCalledAETitle;		// set when reading network properties; used not just in StorageSCP, but also when creating exported meta information headers
	
	protected static boolean haveScannedForCodecs = false;

	protected static boolean haveCheckedForJPEGLosslessCodec = false;
	protected static boolean haveFoundJPEGLosslessCodec = false;
	
	protected boolean haveJPEGLosslessCodec() {
		if (!haveCheckedForJPEGLosslessCodec) {
			if (!haveScannedForCodecs) {
System.err.println("DicomCleaner.haveJPEGLosslessCodec(): Scanning for ImageIO plugin codecs");
				ImageIO.scanForPlugins();
				haveScannedForCodecs=true;
			}
			haveFoundJPEGLosslessCodec = false;
			String readerWanted="jpeg-lossless";
			try {
				javax.imageio.ImageReader reader =  (javax.imageio.ImageReader)(javax.imageio.ImageIO.getImageReadersByFormatName(readerWanted).next());
				if (reader != null) {
System.err.println("DicomCleaner.haveJPEGLosslessCodec(): Found jpeg-lossless reader");
					haveFoundJPEGLosslessCodec = true;
					try {
//System.err.println("DicomCleaner.haveJPEGLosslessCodec(): Calling dispose() on reader");
						reader.dispose();
					}
					catch (Exception e) {
						e.printStackTrace(System.err);
					}
				}
				else {
System.err.println("DicomCleaner.haveJPEGLosslessCodec(): No jpeg-lossless reader");
				}
			}
			catch (Exception e) {
System.err.println("DicomCleaner.haveJPEGLosslessCodec(): No jpeg-lossless reader");
				haveFoundJPEGLosslessCodec = false;
			}
			haveCheckedForJPEGLosslessCodec = true;
		}
		return haveFoundJPEGLosslessCodec;
	}
	
	protected static boolean haveCheckedForJPEG2000Part1Codec = false;
	protected static boolean haveFoundJPEG2000Part1Codec = false;
	
	protected boolean haveJPEG2000Part1Codec() {
		if (!haveCheckedForJPEG2000Part1Codec) {
			if (!haveScannedForCodecs) {
System.err.println("DicomCleaner.haveJPEG2000Part1Codec(): Scanning for ImageIO plugin codecs");
				ImageIO.scanForPlugins();
				haveScannedForCodecs=true;
			}
			haveFoundJPEG2000Part1Codec = false;
			String readerWanted="JPEG2000";
			try {
				javax.imageio.ImageReader reader =  (javax.imageio.ImageReader)(javax.imageio.ImageIO.getImageReadersByFormatName(readerWanted).next());
				if (reader != null) {
System.err.println("DicomCleaner.haveJPEG2000Part1Codec(): Found JPEG2000 reader");
					haveFoundJPEG2000Part1Codec = true;
					try {
//System.err.println("DicomCleaner.haveJPEG2000Part1Codec(): Calling dispose() on reader");
						reader.dispose();
					}
					catch (Exception e) {
						e.printStackTrace(System.err);
					}
				}
				else {
System.err.println("DicomCleaner.haveJPEG2000Part1Codec(): No JPEG2000 reader");
				}
			}
			catch (Exception e) {
System.err.println("DicomCleaner.haveJPEG2000Part1Codec(): No JPEG2000 reader");
				haveFoundJPEG2000Part1Codec = false;
			}
			haveCheckedForJPEG2000Part1Codec = true;
		}
		return haveFoundJPEG2000Part1Codec;
	}
	
	protected void setCurrentRemoteQueryInformationModel(String remoteAEForQuery) {
		currentRemoteQueryInformationModel=null;
		String stringForTitle="";
		if (remoteAEForQuery != null && remoteAEForQuery.length() > 0 && networkApplicationProperties != null && networkApplicationInformation != null) {
			try {
				String              queryCallingAETitle = networkApplicationProperties.getCallingAETitle();
				String               queryCalledAETitle = networkApplicationInformation.getApplicationEntityTitleFromLocalName(remoteAEForQuery);
				PresentationAddress presentationAddress = networkApplicationInformation.getApplicationEntityMap().getPresentationAddress(queryCalledAETitle);
				
				if (presentationAddress == null) {
					throw new Exception("For remote query AE <"+remoteAEForQuery+">, presentationAddress cannot be determined");
				}
				
				String                        queryHost = presentationAddress.getHostname();
				int			      queryPort = presentationAddress.getPort();
				String                       queryModel = networkApplicationInformation.getApplicationEntityMap().getQueryModel(queryCalledAETitle);
				int                     queryDebugLevel = networkApplicationProperties.getQueryDebugLevel();
				
				if (NetworkApplicationProperties.isStudyRootQueryModel(queryModel) || queryModel == null) {
					currentRemoteQueryInformationModel=new StudyRootQueryInformationModel(queryHost,queryPort,queryCalledAETitle,queryCallingAETitle,queryDebugLevel);
					stringForTitle=":"+remoteAEForQuery;
				}
				else {
					throw new Exception("For remote query AE <"+remoteAEForQuery+">, query model "+queryModel+" not supported");
				}
			}
			catch (Exception e) {		// if an AE's property has no value, or model not supported
				e.printStackTrace(System.err);
			}
		}
	}

	private String showInputDialogToSelectNetworkTargetByLocalApplicationEntityName(String message,String title,String defaultSelection) {
		String ae = defaultSelection;
		if (networkApplicationInformation != null) {
			Set localNamesOfRemoteAEs = networkApplicationInformation.getListOfLocalNamesOfApplicationEntities();
			if (localNamesOfRemoteAEs != null) {
				String sta[] = new String[localNamesOfRemoteAEs.size()];
				int i=0;
				Iterator it = localNamesOfRemoteAEs.iterator();
				while (it.hasNext()) {
					sta[i++]=(String)(it.next());
				}
				ae = (String)JOptionPane.showInputDialog(getContentPane(),message,title,JOptionPane.QUESTION_MESSAGE,null,sta,ae);
			}
		}
		return ae;
	}

	protected static void importFileIntoDatabase(DatabaseInformationModel database,String dicomFileName,String fileReferenceType) throws FileNotFoundException, IOException, DicomException {
		ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Importing: "+dicomFileName));
//System.err.println("Importing: "+dicomFileName);
		FileInputStream fis = new FileInputStream(dicomFileName);
		DicomInputStream i = new DicomInputStream(new BufferedInputStream(fis));
		AttributeList list = new AttributeList();
		list.read(i,TagFromName.PixelData);
		i.close();
		fis.close();
		database.insertObject(list,dicomFileName,fileReferenceType);
	}

	protected class OurReceivedObjectHandler extends ReceivedObjectHandler {
		public void sendReceivedObjectIndication(String dicomFileName,String transferSyntax,String callingAETitle)
				throws DicomNetworkException, DicomException, IOException {
			if (dicomFileName != null) {
				String localName = networkApplicationInformation.getLocalNameFromApplicationEntityTitle(callingAETitle);
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Received "+dicomFileName+" from "+callingAETitle+" in "+transferSyntax));
//System.err.println("Received: "+dicomFileName+" from "+callingAETitle+" in "+transferSyntax);
				logger.sendLn("Received "+dicomFileName+" from "+localName+" ("+callingAETitle+")");
				try {
					importFileIntoDatabase(srcDatabase,dicomFileName,DatabaseInformationModel.FILE_COPIED);
					srcDatabasePanel.removeAll();
					new OurSourceDatabaseTreeBrowser(srcDatabase,srcDatabasePanel);
					srcDatabasePanel.validate();
					new File(dicomFileName).deleteOnExit();
				} catch (Exception e) {
					e.printStackTrace(System.err);
				}
			}

		}
	}

	protected File savedImagesFolder;

	protected StorageSOPClassSCPDispatcher storageSOPClassSCPDispatcher;
	
	/**
	 * <p>Start DICOM storage listener for populating source database.</p>
	 *
	 * @exception	DicomException
	 */
	protected void activateStorageSCP() throws DicomException, IOException {
		// Start up DICOM association listener in background for receiving images and responding to echoes ...
		if (networkApplicationProperties != null) {
			{
				int port = networkApplicationProperties.getListeningPort();
				ourCalledAETitle = networkApplicationProperties.getCalledAETitle();
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Starting up DICOM association listener on port "+port+" AET "+ourCalledAETitle));
System.err.println("Starting up DICOM association listener on port "+port+" AET "+ourCalledAETitle);
				int storageSCPDebugLevel = networkApplicationProperties.getStorageSCPDebugLevel();
				int queryDebugLevel = networkApplicationProperties.getQueryDebugLevel();
				storageSOPClassSCPDispatcher = new StorageSOPClassSCPDispatcher(port,ourCalledAETitle,savedImagesFolder,StoredFilePathStrategy.BYSOPINSTANCEUIDINSINGLEFOLDER,new OurReceivedObjectHandler(),
					srcDatabase == null ? null : srcDatabase.getQueryResponseGeneratorFactory(queryDebugLevel),
					srcDatabase == null ? null : srcDatabase.getRetrieveResponseGeneratorFactory(queryDebugLevel),
					networkApplicationInformation,
					new OurPresentationContextSelectionPolicy(),
					false/*secureTransport*/,
					storageSCPDebugLevel);
				new Thread(storageSOPClassSCPDispatcher).start();
			}
		}
	}
	
	class OurPresentationContextSelectionPolicy extends UnencapsulatedExplicitStoreFindMoveGetPresentationContextSelectionPolicy {
		OurPresentationContextSelectionPolicy() {
			super();
			transferSyntaxSelectionPolicy = new OurTransferSyntaxSelectionPolicy();
		}
	}
	
	// we will (grudgingly) accept JPEGBaseline, since we know the JRE can natively decode it without JIIO extensions present,
	// so will work by decompressing during attribute list read for cleaning

	class OurTransferSyntaxSelectionPolicy extends TransferSyntaxSelectionPolicy {
		public LinkedList applyTransferSyntaxSelectionPolicy(LinkedList presentationContexts,int associationNumber,int debugLevel) {
//System.err.println("DicomCleaner.OurTransferSyntaxSelectionPolicy.applyTransferSyntaxSelectionPolicy(): offered "+presentationContexts);
			boolean canUseBzip = PresentationContextListFactory.haveBzip2Support();
			ListIterator pcsi = presentationContexts.listIterator();
			while (pcsi.hasNext()) {
				PresentationContext pc = (PresentationContext)(pcsi.next());
				boolean foundExplicitVRLittleEndian = false;
				boolean foundImplicitVRLittleEndian = false;
				boolean foundExplicitVRBigEndian = false;
				boolean foundDeflated = false;
				boolean foundBzipped = false;
				boolean foundJPEGBaseline = false;
				boolean foundJPEGLossless = false;
				boolean foundJPEGLosslessSV1 = false;
				boolean foundJPEG2000 = false;
				boolean foundJPEG2000Lossless = false;
				List tsuids = pc.getTransferSyntaxUIDs();
				ListIterator tsuidsi = tsuids.listIterator();
				while (tsuidsi.hasNext()) {
					String transferSyntaxUID=(String)(tsuidsi.next());
					if (transferSyntaxUID != null) {
						if      (transferSyntaxUID.equals(TransferSyntax.ImplicitVRLittleEndian)) foundImplicitVRLittleEndian = true;
						else if (transferSyntaxUID.equals(TransferSyntax.ExplicitVRLittleEndian)) foundExplicitVRLittleEndian = true;
						else if (transferSyntaxUID.equals(TransferSyntax.ExplicitVRBigEndian)) foundExplicitVRBigEndian = true;
						else if (transferSyntaxUID.equals(TransferSyntax.DeflatedExplicitVRLittleEndian)) foundDeflated = true;
						else if (transferSyntaxUID.equals(TransferSyntax.PixelMedBzip2ExplicitVRLittleEndian)) foundBzipped = true;
						else if (transferSyntaxUID.equals(TransferSyntax.JPEGBaseline)) foundJPEGBaseline = true;
						else if (transferSyntaxUID.equals(TransferSyntax.JPEGLossless)) foundJPEGLossless = true;
						else if (transferSyntaxUID.equals(TransferSyntax.JPEGLosslessSV1)) foundJPEGLosslessSV1 = true;
						else if (transferSyntaxUID.equals(TransferSyntax.JPEG2000)) foundJPEG2000 = true;
						else if (transferSyntaxUID.equals(TransferSyntax.JPEG2000Lossless)) foundJPEG2000Lossless = true;
					}
				}
				// discard old list and make a new one ...
				pc.newTransferSyntaxUIDs();
				// Policy is prefer bzip then deflate compressed then explicit (little then big) then implicit,
				// then supported image compression transfer syntaxes in the following order and ignore anything else
				// with the intent of having the sender decompress the image compression transfer syntaxes if it provided multiple choices.
				// must only support ONE in response
				if (foundBzipped && canUseBzip) {
					pc.addTransferSyntaxUID(TransferSyntax.PixelMedBzip2ExplicitVRLittleEndian);
				}
				else if (foundDeflated) {
					pc.addTransferSyntaxUID(TransferSyntax.DeflatedExplicitVRLittleEndian);
				}
				else if (foundExplicitVRLittleEndian) {
					pc.addTransferSyntaxUID(TransferSyntax.ExplicitVRLittleEndian);
				}
				else if (foundExplicitVRBigEndian) {
					pc.addTransferSyntaxUID(TransferSyntax.ExplicitVRBigEndian);
				}
				else if (foundImplicitVRLittleEndian) {
					pc.addTransferSyntaxUID(TransferSyntax.ImplicitVRLittleEndian);
				}
				else if (foundJPEGBaseline) {
					pc.addTransferSyntaxUID(TransferSyntax.JPEGBaseline);
				}
				else if (foundJPEGLossless && haveJPEGLosslessCodec()) {
					pc.addTransferSyntaxUID(TransferSyntax.JPEGLossless);
				}
				else if (foundJPEGLosslessSV1 && haveJPEGLosslessCodec()) {
					pc.addTransferSyntaxUID(TransferSyntax.JPEGLosslessSV1);
				}
				else if (foundJPEG2000 && haveJPEG2000Part1Codec()) {
					pc.addTransferSyntaxUID(TransferSyntax.JPEG2000);
				}
				else if (foundJPEG2000Lossless && haveJPEG2000Part1Codec()) {
					pc.addTransferSyntaxUID(TransferSyntax.JPEG2000Lossless);
				}
				else {
					pc.setResultReason((byte)4);				// transfer syntaxes not supported (provider rejection)
				}
			}
//System.err.println("DicomCleaner.OurTransferSyntaxSelectionPolicy.applyTransferSyntaxSelectionPolicy(): accepted "+presentationContexts);
			return presentationContexts;
		}
	}

	/**
	 * <p>Start two databases, one for the "source" instances and one for the "target" instances.</p>
	 *
	 * <p>Neither will persist when the application is closed, so in memory databases
	 *  only are used and instances live in the temporary filesystem.</p>
	 *
	 * @exception	DicomException
	 */
	protected void activateTemporaryDatabases() throws DicomException {
		srcDatabase = new PatientStudySeriesConcatenationInstanceModel("mem:src",null,resourceBundle.getString("DatabaseRootTitleForOriginal"));
		dstDatabase = new PatientStudySeriesConcatenationInstanceModel("mem:dst",null,resourceBundle.getString("DatabaseRootTitleForCleaned"));
	}

	protected DatabaseTreeRecord currentSourceDatabaseSelection;
	protected Vector currentSourceFilePathSelections;

	protected class OurSourceDatabaseTreeBrowser extends DatabaseTreeBrowser {
		public OurSourceDatabaseTreeBrowser(DatabaseInformationModel d,Container content) throws DicomException {
			super(d,content);
		}
		
		protected boolean doSomethingWithSelection(DatabaseTreeRecord selection) {
			currentSourceDatabaseSelection = selection;
			return false;	// still want to call doSomethingWithSelectedFiles()
		}
		
		protected void doSomethingWithSelectedFiles(Vector paths) {
			currentSourceFilePathSelections = paths;
		}
	}
	
	protected DatabaseTreeRecord currentDestinationDatabaseSelection;
	protected Vector currentDestinationFilePathSelections;

	protected class OurDestinationDatabaseTreeBrowser extends DatabaseTreeBrowser {
		public OurDestinationDatabaseTreeBrowser(DatabaseInformationModel d,Container content) throws DicomException {
			super(d,content);
		}
		
		protected boolean doSomethingWithSelection(DatabaseTreeRecord selection) {
			currentDestinationDatabaseSelection = selection;
			return false;	// still want to call doSomethingWithSelectedFiles()
		}
		
		protected void doSomethingWithSelectedFiles(Vector paths) {
			currentDestinationFilePathSelections = paths;
		}
	}
	
	protected static void updateProgressBase(JProgressBar progressBar,int done,int maximum) {
		if (progressBar != null) {
			progressBar.setValue(done);
			progressBar.setMaximum(maximum);
			progressBar.setStringPainted(true);
			progressBar.repaint();
		}
	}
		
	protected void purgeFilesAndDatabaseInformation(DatabaseTreeRecord databaseSelection,MessageLogger logger,JProgressBar progressBar,int done,int maximum) throws DicomException, IOException {
//System.err.println("DicomCleaner.purgeFilesAndDatabaseInformation(): "+databaseSelection);
		if (databaseSelection != null) {
			updateProgressBase(progressBar,done,maximum);
			InformationEntity ie = databaseSelection.getInformationEntity();
//System.err.println("DicomCleaner.purgeFilesAndDatabaseInformation(): ie = "+ie);
			if (ie == null /* the root of the tree, i.e., everything */ || !ie.equals(InformationEntity.INSTANCE)) {
				// Do it one study at a time, in the order in which the patients and studies are sorted in the tree
				Enumeration children = databaseSelection.children();
				if (children != null) {
					maximum+=databaseSelection.getChildCount();
					while (children.hasMoreElements()) {
						DatabaseTreeRecord child = (DatabaseTreeRecord)(children.nextElement());
						if (child != null) {
							purgeFilesAndDatabaseInformation(child,logger,progressBar,done,maximum);
							++done;
						}
					}
				}
				// AFTER we have processed all the children, if any, we can delete ourselves, unless we are the root
				if (ie != null) {
//System.err.println("DicomCleaner.purgeFilesAndDatabaseInformation(): removeFromParent having recursed over children "+databaseSelection);
					logger.sendLn("Purging "+databaseSelection);
					databaseSelection.removeFromParent();
				}
			}
			else {
				// Instance level ... may need to delete files
				String fileName = databaseSelection.getLocalFileNameValue();
				String fileReferenceType = databaseSelection.getLocalFileReferenceTypeValue();
//System.err.println("DicomCleaner.purgeFilesAndDatabaseInformation(): fileReferenceType = "+fileReferenceType+" for file "+fileName);
				if (fileReferenceType != null && fileReferenceType.equals(DatabaseInformationModel.FILE_COPIED)) {
//System.err.println("DicomCleaner.purgeFilesAndDatabaseInformation(): deleting fileName "+fileName);
					try {
						logger.sendLn("Deleting file "+fileName);
						if (!new File(fileName).delete()) {
							logger.sendLn("Failed to delete local copy of file "+fileName);
						}
					}
					catch (Exception e) {
						e.printStackTrace(System.err);
						logger.sendLn("Failed to delete local copy of file "+fileName);
					}
				}
//System.err.println("DicomCleaner.purgeFilesAndDatabaseInformation(): removeFromParent instance level "+databaseSelection);
				logger.sendLn("Purging "+databaseSelection);
				databaseSelection.removeFromParent();
			}
		}
	}

	protected class PurgeWorker implements Runnable {
		//PurgeWorker() {
		//}

		public void run() {
			Cursor was = getCursor();
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			logger.sendLn("Purging started");
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Purging started"));
			progressBar.setStringPainted(true);
			progressBar.repaint();
			try {
				purgeFilesAndDatabaseInformation(currentSourceDatabaseSelection,logger,progressBar,0,1);
				purgeFilesAndDatabaseInformation(currentDestinationDatabaseSelection,logger,progressBar,0,1);
			} catch (Exception e) {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Purging failed: "+e));
				e.printStackTrace(System.err);
			}
			srcDatabasePanel.removeAll();
			dstDatabasePanel.removeAll();
			try {
				new OurSourceDatabaseTreeBrowser(srcDatabase,srcDatabasePanel);
				new OurDestinationDatabaseTreeBrowser(dstDatabase,dstDatabasePanel);
			} catch (Exception e) {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Refresh source database browser failed: "+e));
				e.printStackTrace(System.err);
			}
			srcDatabasePanel.validate();
			progressBar.setValue(progressBar.getMaximum());		// in case anything bad happens, don't want to leave progressBar in intermediate state
			progressBar.setStringPainted(false);
			progressBar.repaint();
			logger.sendLn("Purging complete");
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Done purging"));
			setCursor(was);
		}
	}

	protected class PurgeActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			try {
				activeThread = new Thread(new PurgeWorker());
				activeThread.start();
			} catch (Exception e) {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Purging failed: "+e));
				e.printStackTrace(System.err);
			}
		}
	}
		
	protected void copyFromOriginalToCleanedPerformingAction(Vector paths,MessageLogger logger,JProgressBar progressBar) throws DicomException, IOException {
		if (paths != null) {
			progressBar.setValue(0);
			progressBar.setMaximum(paths.size());
			progressBar.setStringPainted(true);
			progressBar.repaint();
			for (int j=0; j< paths.size(); ++j) {
				String dicomFileName = (String)(paths.get(j));
				if (dicomFileName != null) {
//System.err.println("DicomCleaner.copyFromOriginalToCleanedPerformingAction(): doing file "+dicomFileName);
					ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Cleaning "+dicomFileName));
					// do not log it yet ... wait till we have output file name
//long startTime = System.currentTimeMillis();
					File file = new File(dicomFileName);
					DicomInputStream i = new DicomInputStream(file);
					AttributeList list = new AttributeList();
//long currentTime = System.currentTimeMillis();
//System.err.println("DicomCleaner.copyFromOriginalToCleanedPerformingAction(): reading AttributeList took = "+(currentTime-startTime)+" ms");
//startTime=currentTime;
					list.read(i);
					i.close();

					list.removeGroupLengthAttributes();
					list.correctDecompressedImagePixelModule();
					list.insertLossyImageCompressionHistoryIfDecompressed();
					list.removeMetaInformationHeaderAttributes();
					
					if (removeClinicalTrialAttributesBox.isSelected()) {
						ClinicalTrialsAttributes.removeClinicalTrialsAttributes(list);
					}
					if (removeIdentityCheckBox.isSelected()) {
						ClinicalTrialsAttributes.removeOrNullIdentifyingAttributes(list,ClinicalTrialsAttributes.HandleUIDs.keep,!removeDescriptionsCheckBox.isSelected(),!removeSeriesDescriptionsCheckBox.isSelected(),!removeCharacteristicsCheckBox.isSelected(),!removeDeviceIdentityCheckBox.isSelected(),!removeInstitutionIdentityCheckBox.isSelected());
					}
					if (replacePatientNameCheckBox.isSelected()) {
						String newName = replacementPatientNameTextField.getText().trim();
						{ AttributeTag tag = TagFromName.PatientName; list.remove(tag); Attribute a = new PersonNameAttribute(tag); a.addValue(newName); list.put(tag,a); }
					}
					if (replacePatientIDCheckBox.isSelected()) {
						String newID = replacementPatientIDTextField.getText().trim();
						{ AttributeTag tag = TagFromName.PatientID; list.remove(tag); Attribute a = new LongStringAttribute(tag); a.addValue(newID); list.put(tag,a); }
					}
					if (replaceAccessionNumberCheckBox.isSelected()) {
						String newAccessionNumber = replacementAccessionNumberTextField.getText().trim();
						{ AttributeTag tag = TagFromName.AccessionNumber; list.remove(tag); Attribute a = new ShortStringAttribute(tag); a.addValue(newAccessionNumber); list.put(tag,a); }
					}
					if (removePrivateCheckBox.isSelected()) {
						list.removeUnsafePrivateAttributes();
						{
							Attribute a = list.get(TagFromName.DeidentificationMethod);
							if (a != null) {
								a.addValue("Unsafe private removed");
							}
						}
						{
							SequenceAttribute a = (SequenceAttribute)(list.get(TagFromName.DeidentificationMethodCodeSequence));
							if (a != null) {
								a.addItem(new CodedSequenceItem("113111","DCM","Retain Safe Private Option").getAttributeList());
							}
						}
					}
					else {
						{
							Attribute a = list.get(TagFromName.DeidentificationMethod);
							if (a != null) {
								a.addValue("All private retained");
							}
						}
						{
							SequenceAttribute a = (SequenceAttribute)(list.get(TagFromName.DeidentificationMethodCodeSequence));
							if (a != null) {
								a.addItem(new CodedSequenceItem("210002","99PMP","Retain all private elements").getAttributeList());
							}
						}
					}
					if (cleanUIDsCheckBox.isSelected()) {
						ClinicalTrialsAttributes.remapUIDAttributes(list);
						{
							Attribute a = list.get(TagFromName.DeidentificationMethod);
							if (a != null) {
								a.addValue("UIDs remapped");
							}
						}
						// remove the default Retain UIDs added by ClinicalTrialsAttributes.removeOrNullIdentifyingAttributes() with the ClinicalTrialsAttributes.HandleUIDs.keep option
						{
							SequenceAttribute a = (SequenceAttribute)(list.get(TagFromName.DeidentificationMethodCodeSequence));
							if (a != null) {
								Iterator<SequenceItem> it = a.iterator();
								while (it.hasNext()) {
									SequenceItem item = it.next();
									if (item != null) {
										CodedSequenceItem testcsi = new CodedSequenceItem(item.getAttributeList());
										if (testcsi != null) {
											String cv = testcsi.getCodeValue();
											String csd = testcsi.getCodingSchemeDesignator();
											if (cv != null && cv.equals("113110") && csd != null && csd.equals("DCM")) {	// "Retain UIDs Option"
												it.remove();
											}
										}
									}
								}
							}
						}
						{
							SequenceAttribute a = (SequenceAttribute)(list.get(TagFromName.DeidentificationMethodCodeSequence));
							if (a != null) {
								a.addItem(new CodedSequenceItem("210001","99PMP","Remap UIDs").getAttributeList());
							}
						}
					}
					if (addContributingEquipmentCheckBox.isSelected()) {
						ClinicalTrialsAttributes.addContributingEquipmentSequence(list,
							true,
							new CodedSequenceItem("109104","DCM","De-identifying Equipment"),	// per CP 892
							"PixelMed",														// Manufacturer
							null,															// Institution Name
							null,															// Institutional Department Name
							null		,													// Institution Address
							ourCalledAETitle,												// Station Name
							"DicomCleaner",													// Manufacturer's Model Name
							null,															// Device Serial Number
							getBuildDate(),													// Software Version(s)
							"Cleaned",
							DateTimeAttribute.getFormattedString(new java.util.Date()));
					}
					FileMetaInformation.addFileMetaInformation(list,TransferSyntax.ExplicitVRLittleEndian,ourCalledAETitle);
					list.insertSuitableSpecificCharacterSetForAllStringValues();	// E.g., may have de-identified Kanji name and need new character set
//currentTime = System.currentTimeMillis();
//System.err.println("DicomCleaner.copyFromOriginalToCleanedPerformingAction(): cleaning AttributeList took = "+(currentTime-startTime)+" ms");
//startTime=currentTime;
					File cleanedFile = File.createTempFile("clean",".dcm");
					cleanedFile.deleteOnExit();
					list.write(cleanedFile);
//currentTime = System.currentTimeMillis();
//System.err.println("DicomCleaner.copyFromOriginalToCleanedPerformingAction(): writing AttributeList took = "+(currentTime-startTime)+" ms");
//startTime=currentTime;
					logger.sendLn("Cleaned "+dicomFileName+" into "+cleanedFile.getCanonicalPath());
					dstDatabase.insertObject(list,cleanedFile.getCanonicalPath(),DatabaseInformationModel.FILE_COPIED);
//currentTime = System.currentTimeMillis();
//System.err.println("DicomCleaner.copyFromOriginalToCleanedPerformingAction(): inserting cleaned object in database took = "+(currentTime-startTime)+" ms");
//startTime=currentTime;
				}
				progressBar.setValue(j+1);
				progressBar.setStringPainted(true);
				progressBar.repaint();
			}
		}
	}
	
	protected class CleanWorker implements Runnable {
		Vector sourceFilePathSelections;
		DatabaseInformationModel dstDatabase;
		JPanel dstDatabasePanel;
		
		CleanWorker(Vector sourceFilePathSelections,DatabaseInformationModel dstDatabase,JPanel dstDatabasePanel) {
			this.sourceFilePathSelections=sourceFilePathSelections;
			this.dstDatabase=dstDatabase;
			this.dstDatabasePanel=dstDatabasePanel;
		}

		public void run() {
			Cursor was = getCursor();
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			logger.sendLn("Cleaning started");
			progressBar.setStringPainted(true);
			progressBar.repaint();
			try {
				copyFromOriginalToCleanedPerformingAction(sourceFilePathSelections,logger,progressBar);
			} catch (Exception e) {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Cleaned failed: "+e));
				e.printStackTrace(System.err);
			}
			dstDatabasePanel.removeAll();
			try {
				new OurDestinationDatabaseTreeBrowser(dstDatabase,dstDatabasePanel);
			} catch (Exception e) {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Refresh destination database browser failed: "+e));
				e.printStackTrace(System.err);
			}
			dstDatabasePanel.validate();
			progressBar.setValue(progressBar.getMaximum());		// in case anything bad happens, don't want to leave progressBar in intermediate state
			progressBar.setStringPainted(false);
			progressBar.repaint();
			logger.sendLn("Cleaning complete");
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Done cleaning"));
			setCursor(was);
		}
	}

	protected class CleanActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			try {
				activeThread = new Thread(new CleanWorker(currentSourceFilePathSelections,dstDatabase,dstDatabasePanel));
				activeThread.start();

			} catch (Exception e) {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Cleaned failed: "+e));
				e.printStackTrace(System.err);
			}
		}
	}
	
	protected class OurMediaImporter extends MediaImporter {
		public OurMediaImporter(String mediaDirectoryPath,MessageLogger logger,JProgressBar progressBar) {
			super(mediaDirectoryPath,logger,progressBar);
		}
		protected void doSomethingWithDicomFileOnMedia(String mediaFileName) {
			try {
				importFileIntoDatabase(srcDatabase,mediaFileName,DatabaseInformationModel.FILE_REFERENCED);
			}
			catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
		
		protected boolean canUseBzip = PresentationContextListFactory.haveBzip2Support();

		// override base class isOKToImport(), which rejects unsupported compressed transfer syntaxes
		
		protected boolean isOKToImport(String sopClassUID,String transferSyntaxUID) {
			return sopClassUID != null
				&& (SOPClass.isImageStorage(sopClassUID) || (SOPClass.isNonImageStorage(sopClassUID) && ! SOPClass.isDirectory(sopClassUID)))
				&& transferSyntaxUID != null
				&& (transferSyntaxUID.equals(TransferSyntax.ImplicitVRLittleEndian)
				 || transferSyntaxUID.equals(TransferSyntax.ExplicitVRLittleEndian)
				 || transferSyntaxUID.equals(TransferSyntax.ExplicitVRBigEndian)
				 || transferSyntaxUID.equals(TransferSyntax.DeflatedExplicitVRLittleEndian)
				 || (transferSyntaxUID.equals(TransferSyntax.DeflatedExplicitVRLittleEndian) && canUseBzip)
				 || transferSyntaxUID.equals(TransferSyntax.JPEGBaseline)
				 || haveJPEGLosslessCodec() && (transferSyntaxUID.equals(TransferSyntax.JPEGLossless) || transferSyntaxUID.equals(TransferSyntax.JPEGLosslessSV1))
				 || haveJPEG2000Part1Codec() && (transferSyntaxUID.equals(TransferSyntax.JPEG2000) || transferSyntaxUID.equals(TransferSyntax.JPEG2000Lossless))
				);
		}
	}

	protected class ImportWorker implements Runnable {
		MediaImporter importer;
		DatabaseInformationModel srcDatabase;
		JPanel srcDatabasePanel;
		
		ImportWorker(String path,DatabaseInformationModel srcDatabase,JPanel srcDatabasePanel) {
			importer = new OurMediaImporter(path,logger,progressBar);
			this.srcDatabase=srcDatabase;
			this.srcDatabasePanel=srcDatabasePanel;
		}

		public void run() {
			Cursor was = getCursor();
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			logger.sendLn("Import starting");
			progressBar.setStringPainted(true);
			progressBar.repaint();
			try {
				importer.choosePathAndImportDicomFiles(DicomCleaner.this.getContentPane());
			} catch (Exception e) {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Importing failed: "+e));
				e.printStackTrace(System.err);
			}
			srcDatabasePanel.removeAll();
			try {
				new OurSourceDatabaseTreeBrowser(srcDatabase,srcDatabasePanel);
			} catch (Exception e) {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Refresh source database browser failed: "+e));
				e.printStackTrace(System.err);
			}
			srcDatabasePanel.validate();
			progressBar.setValue(progressBar.getMaximum());		// in case anything bad happens, don't want to leave progressBar in intermediate state
			progressBar.setStringPainted(false);
			progressBar.repaint();
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Done importing"));
			// importer sends its own completion message to log, so do not need another one
			setCursor(was);
		}
	}

	protected class ImportActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			try {
				if (logger instanceof DialogMessageLogger) {
					((DialogMessageLogger)logger).setVisible(true);
				}
				new Thread(new ImportWorker("/",srcDatabase,srcDatabasePanel)).start();
			} catch (Exception e) {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Importing failed: "+e));
				e.printStackTrace(System.err);
			}
		}
	}

	protected String exportDirectoryPath;	// keep around between invocations
	
	protected String makeNewFullyQualifiedInterchangeMediaInstancePathName(int fileCount) throws IOException {
		return new File(
			rootNameForDicomInstanceFilesOnInterchangeMedia,
			filePrefixForDicomInstanceFilesOnInterchangeMedia + Integer.toString(fileCount) + fileSuffixForDicomInstanceFilesOnInterchangeMedia)
			.getPath();
	}

	protected String makeNewFullyQualifiedHierarchicalInstancePathName(String sourceFileName) throws DicomException, IOException {
		AttributeList list = new AttributeList();
		list.read(sourceFileName,TagFromName.PixelData);
		String hierarchicalFileName = MoveDicomFilesIntoHierarchy.makeHierarchicalPathFromAttributes(list);
		return new File(rootNameForDicomInstanceFilesOnInterchangeMedia,hierarchicalFileName).getPath();
	}

	protected class ExportWorker implements Runnable {
		Vector destinationFilePathSelections;
		File exportDirectory;
		
		ExportWorker(Vector destinationFilePathSelections,File exportDirectory) {
			this.destinationFilePathSelections=destinationFilePathSelections;
			this.exportDirectory=exportDirectory;
		}

		public void run() {
			Cursor was = getCursor();
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			logger.sendLn("Export started");
			try {
				int nFiles = destinationFilePathSelections.size();
				progressBar.setMaximum(nFiles+1);		// include DICOMDIR
				progressBar.setStringPainted(true);
				progressBar.repaint();
				String exportFileNames[] = new String[nFiles];
				for (int j=0; j<nFiles; ++j) {
					String databaseFileName = (String)(destinationFilePathSelections.get(j));
					String exportRelativePathName = hierarchicalExportCheckBox.isSelected() ? makeNewFullyQualifiedHierarchicalInstancePathName(databaseFileName) : makeNewFullyQualifiedInterchangeMediaInstancePathName(j);
					File exportFile = new File(exportDirectory,exportRelativePathName);
					ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Exporting "+exportRelativePathName));
					logger.sendLn("Exporting "+databaseFileName+" to "+exportFile.getCanonicalPath());
//System.err.println("DicomCleaner.ExportWorker.run(): copying "+databaseFileName+" to "+exportFile);
					exportFile.getParentFile().mkdirs();
					CopyStream.copy(new File(databaseFileName),exportFile);
					exportFileNames[j] = exportRelativePathName;
					progressBar.setValue(j+1);
					progressBar.repaint();
				}
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Exporting DICOMDIR"));
				logger.sendLn("Exporting DICOMDIR");
//System.err.println("DicomCleaner.ExportWorker.run():  building DICOMDIR");
				DicomDirectory dicomDirectory = new DicomDirectory(exportDirectory,exportFileNames);
//System.err.println("DicomCleaner.ExportWorker.run():  writing DICOMDIR");
				dicomDirectory.write(new File(exportDirectory,nameForDicomDirectoryOnInterchangeMedia).getCanonicalPath());
				progressBar.setValue(nFiles+1);		// include DICOMDIR
				progressBar.repaint();

				if (zipExportCheckBox.isSelected()) {
					ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Zipping exported files"));
					logger.sendLn("Zipping exported files");
					File zipFile = new File(exportDirectory,exportedZipFileName);
					zipFile.delete();
					FileOutputStream fout = new FileOutputStream(zipFile);
					ZipOutputStream zout = new ZipOutputStream(fout);
					zout.setMethod(ZipOutputStream.DEFLATED);
					zout.setLevel(9);

					progressBar.setMaximum(nFiles+1);		// include DICOMDIR
					progressBar.setStringPainted(true);
					progressBar.repaint();
					progressBar.setValue(0);
					for (int j=0; j<nFiles; ++j) {
						String exportRelativePathName = exportFileNames[j];
						File inFile = new File(exportDirectory,exportRelativePathName);
						ZipEntry zipEntry = new ZipEntry(exportRelativePathName);
						//zipEntry.setMethod(ZipOutputStream.DEFLATED);
						zout.putNextEntry(zipEntry);
						FileInputStream in = new FileInputStream(inFile);
						CopyStream.copy(in,zout);
						zout.closeEntry();
						in.close();
						inFile.delete();
						progressBar.setValue(j+1);
						progressBar.repaint();
					}

					{
						File inFile = new File(exportDirectory,nameForDicomDirectoryOnInterchangeMedia);
						ZipEntry zipEntry = new ZipEntry(nameForDicomDirectoryOnInterchangeMedia);
						zipEntry.setMethod(ZipOutputStream.DEFLATED);
						zout.putNextEntry(zipEntry);
						FileInputStream in = new FileInputStream(inFile);
						CopyStream.copy(in,zout);
						zout.closeEntry();
						in.close();
						inFile.delete();
						progressBar.setValue(nFiles+1);		// include DICOMDIR
						progressBar.repaint();
					}
					zout.close();
					fout.close();
					new File(exportDirectory,rootNameForDicomInstanceFilesOnInterchangeMedia).delete();
				}

			} catch (Exception e) {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Export failed: "+e));
				e.printStackTrace(System.err);
			}
			
			progressBar.setValue(progressBar.getMaximum());		// in case anything bad happens, don't want to leave progressBar in intermediate state
			progressBar.setStringPainted(false);
			progressBar.repaint();
			logger.sendLn("Export complete");
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Done exporting to "+exportDirectory));
			setCursor(was);
		}
	}

	protected class ExportActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			if (currentDestinationFilePathSelections != null && currentDestinationFilePathSelections.size() > 0) {
				JFileChooser chooser = new JFileChooser(exportDirectoryPath);
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				if (chooser.showOpenDialog(DicomCleaner.this.getContentPane()) == JFileChooser.APPROVE_OPTION) {
					try {
						//exportDirectoryPath=chooser.getCurrentDirectory().getCanonicalPath();
						exportDirectoryPath = chooser.getSelectedFile().getCanonicalPath();
						File exportDirectory = new File(exportDirectoryPath);
//System.err.println("DicomCleaner.ExportActionListener.actionPerformed(): selected root directory = "+exportDirectory);
//System.err.println("DicomCleaner.ExportActionListener.actionPerformed(): copying files");
						new Thread(new ExportWorker(currentDestinationFilePathSelections,exportDirectory)).start();
					}
					catch (Exception e) {
						ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Export failed: "+e));
						e.printStackTrace(System.err);
					}
				}
				// else user cancelled operation in JOptionPane.showInputDialog() so gracefully do nothing
			}
		}
	}
	
	protected class OurMultipleInstanceTransferStatusHandler extends MultipleInstanceTransferStatusHandlerWithFileName {
		int nFiles;
		
		OurMultipleInstanceTransferStatusHandler(int nFiles) {
			this.nFiles=nFiles;
		}
		
		public void updateStatus(int nRemaining,int nCompleted,int nFailed,int nWarning,String sopInstanceUID,String fileName,boolean success) {
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Remaining "+nRemaining+", completed "+nCompleted+", failed "+nFailed+", warning "+nWarning));
			progressBar.setValue(nFiles - nRemaining);
			progressBar.repaint();
			logger.sendLn((success ? "Sent " : "Failed to send ")+fileName);
		}
	}

	protected class SendWorker implements Runnable {
		String hostname;
		int port;
		String calledAETitle;
		String callingAETitle;
		SetOfDicomFiles setOfDicomFiles;
		
		SendWorker(String hostname,int port,String calledAETitle,String callingAETitle,SetOfDicomFiles setOfDicomFiles) {
			this.hostname=hostname;
			this.port=port;
			this.calledAETitle=calledAETitle;
			this.callingAETitle=callingAETitle;
			this.setOfDicomFiles=setOfDicomFiles;
		}

		public void run() {
			Cursor was = getCursor();
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			logger.sendLn("Send starting");
			try {
				int nFiles = setOfDicomFiles.size();
				progressBar.setMaximum(nFiles);
				progressBar.setStringPainted(true);
				progressBar.repaint();
				new StorageSOPClassSCU(hostname,port,calledAETitle,callingAETitle,setOfDicomFiles,0/*compressionLevel*/,
					new OurMultipleInstanceTransferStatusHandler(nFiles),
					0/*debugLevel*/);
			} catch (Exception e) {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Send failed: "+e));
				logger.sendLn("Send failed");
				e.printStackTrace(System.err);
			}
			progressBar.setValue(progressBar.getMaximum());		// in case anything bad happens, don't want to leave progressBar in intermediate state
			progressBar.setStringPainted(false);
			progressBar.repaint();
			logger.sendLn("Send complete");
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Done sending to "+calledAETitle));
			setCursor(was);
		}
	}

	protected class SendActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			Cursor was = getCursor();
			if (currentDestinationFilePathSelections != null && currentDestinationFilePathSelections.size() > 0) {
				Properties properties = getProperties();
				String ae = properties.getProperty(propertyName_DicomCurrentlySelectedStorageTargetAE);
				ae = showInputDialogToSelectNetworkTargetByLocalApplicationEntityName("Select destination","Send ...",ae);
				if (ae != null && networkApplicationProperties != null) {
					try {
						String                   callingAETitle = networkApplicationProperties.getCallingAETitle();
						String                    calledAETitle = networkApplicationInformation.getApplicationEntityTitleFromLocalName(ae);
						PresentationAddress presentationAddress = networkApplicationInformation.getApplicationEntityMap().getPresentationAddress(calledAETitle);
						String                         hostname = presentationAddress.getHostname();
						int                                port = presentationAddress.getPort();
						
						SetOfDicomFiles setOfDicomFiles = new SetOfDicomFiles(currentDestinationFilePathSelections);
						new Thread(new SendWorker(hostname,port,calledAETitle,callingAETitle,setOfDicomFiles)).start();
					}
					catch (Exception e) {
						e.printStackTrace(System.err);
					}
				}
				// else user cancelled operation in JOptionPane.showInputDialog() so gracefully do nothing
			}
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Done sending."));
		}
	}
	
	protected class OurDicomImageBlackout extends DicomImageBlackout {
	
		OurDicomImageBlackout(String title,String dicomFileNames[],int burnedinflag,String ourAETitle) {
			super(title,dicomFileNames,(DicomImageBlackout.StatusNotificationHandler)null,burnedinflag);
			statusNotificationHandler = new ApplicationStatusChangeEventNotificationHandler();
			this.ourAETitle=ourAETitle;
		}

		public class ApplicationStatusChangeEventNotificationHandler extends StatusNotificationHandler {
			public void notify(int status,String message,Throwable t) {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Blackout "+message));
				logger.sendLn("Blackout "+message);
				System.err.println("DicomImageBlackout.DefaultStatusNotificationHandler.notify(): status = "+status);
				System.err.println("DicomImageBlackout.DefaultStatusNotificationHandler.notify(): message = "+message);
				if (t != null) {
					t.printStackTrace(System.err);
				}
			}
		}
	}
	
	protected class BlackoutActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			Cursor was = getCursor();
			logger.sendLn("Blackout starting");
			if (currentDestinationFilePathSelections != null && currentDestinationFilePathSelections.size() > 0) {
				{
					try {
						setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
						int nFiles = currentDestinationFilePathSelections.size();
						String fileNames[] = new String[nFiles];
						for (int j=0; j< nFiles; ++j) {
							fileNames[j] = (String)(currentDestinationFilePathSelections.get(j));
						}
						new OurDicomImageBlackout("Dicom Image Blackout",fileNames,DicomImageBlackout.BurnedInAnnotationFlagAction.ADD_AS_NO_IF_SAVED,ourCalledAETitle);
					}
					catch (Exception e) {
						e.printStackTrace(System.err);
					}
				}
			}
			// don't need to send StatusChangeEvent("Blackout complete.") ... DicomImageBlackout already does something similar
			// DicomImageBlackout sends its own completion message to log, so do not need another one
			setCursor(was);
		}
	}
	
	protected void setCurrentRemoteQuerySelection(AttributeList uniqueKeys,Attribute uniqueKey,AttributeList identifier) {
		currentRemoteQuerySelectionUniqueKeys=uniqueKeys;
		currentRemoteQuerySelectionUniqueKey=uniqueKey;
		currentRemoteQuerySelectionRetrieveAE=null;
		if (identifier != null) {
			Attribute aRetrieveAETitle=identifier.get(TagFromName.RetrieveAETitle);
			if (aRetrieveAETitle != null) currentRemoteQuerySelectionRetrieveAE=aRetrieveAETitle.getSingleStringValueOrNull();
		}
		if (currentRemoteQuerySelectionRetrieveAE == null) {
			// it is legal for RetrieveAETitle to be zero length at all but the lowest levels of
			// the query model :( (See PS 3.4 C.4.1.1.3.2)
			// (so far the Leonardo is the only one that doesn't send it at all levels)
			// we could recurse down to the lower levels and get the union of the value there
			// but lets just keep it simple and ...
			// default to whoever it was we queried in the first place ...
			if (currentRemoteQueryInformationModel != null) {
				currentRemoteQuerySelectionRetrieveAE=currentRemoteQueryInformationModel.getCalledAETitle();
			}
		}
		currentRemoteQuerySelectionLevel = null;
		if (identifier != null) {
			Attribute a = identifier.get(TagFromName.QueryRetrieveLevel);
			if (a != null) {
				currentRemoteQuerySelectionLevel = a.getSingleStringValueOrNull();
			}
		}
		if (currentRemoteQuerySelectionLevel == null) {
			// QueryRetrieveLevel must have been (erroneously) missing in query response ... see with Dave Harvey's code on public server
			// so try to guess it from unique key in tree record
			// Fixes [bugs.mrmf] (000224) Missing query/retrieve level in C-FIND response causes tree select and retrieve to fail
			if (uniqueKey != null) {
				AttributeTag tag = uniqueKey.getTag();
				if (tag != null) {
					if (tag.equals(TagFromName.PatientID)) {
						currentRemoteQuerySelectionLevel="PATIENT";
					}
					else if (tag.equals(TagFromName.StudyInstanceUID)) {
						currentRemoteQuerySelectionLevel="STUDY";
					}
					else if (tag.equals(TagFromName.SeriesInstanceUID)) {
						currentRemoteQuerySelectionLevel="SERIES";
					}
					else if (tag.equals(TagFromName.SOPInstanceUID)) {
						currentRemoteQuerySelectionLevel="IMAGE";
					}
				}
			}
System.err.println("DicomCleaner.setCurrentRemoteQuerySelection(): Guessed missing currentRemoteQuerySelectionLevel to be "+currentRemoteQuerySelectionLevel);
		}
	}

	protected class OurQueryTreeBrowser extends QueryTreeBrowser {
		/**
		 * @param	q
		 * @param	m
		 * @param	content
		 * @exception	DicomException
		 */
		OurQueryTreeBrowser(QueryInformationModel q,QueryTreeModel m,Container content) throws DicomException {
			super(q,m,content);
		}
		/***/
		protected TreeSelectionListener buildTreeSelectionListenerToDoSomethingWithSelectedLevel() {
			return new TreeSelectionListener() {
				public void valueChanged(TreeSelectionEvent tse) {
					TreePath tp = tse.getNewLeadSelectionPath();
					if (tp != null) {
						Object lastPathComponent = tp.getLastPathComponent();
						if (lastPathComponent instanceof QueryTreeRecord) {
							QueryTreeRecord r = (QueryTreeRecord)lastPathComponent;
							setCurrentRemoteQuerySelection(r.getUniqueKeys(),r.getUniqueKey(),r.getAllAttributesReturnedInIdentifier());
							currentRemoteQuerySelectionQueryTreeRecord=r;
						}
					}
				}
			};
		}
	}

	protected class QueryWorker implements Runnable {
		AttributeList filter;
		
		QueryWorker(AttributeList filter) {
			this.filter=filter;
		}

		public void run() {
			Cursor was = getCursor();
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			String calledAET = currentRemoteQueryInformationModel.getCalledAETitle();
			String localName = networkApplicationInformation.getLocalNameFromApplicationEntityTitle(calledAET);
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Performing query on "+localName));
			logger.sendLn("Query to "+localName+" ("+calledAET+") starting");
			try {
				QueryTreeModel treeModel = currentRemoteQueryInformationModel.performHierarchicalQuery(filter);
				new OurQueryTreeBrowser(currentRemoteQueryInformationModel,treeModel,remoteQueryRetrievePanel);
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Done querying "+localName));
			} catch (Exception e) {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Query to "+localName+" failed "+e));
				logger.sendLn("Query to "+localName+" ("+calledAET+") failed due to"+ e);
				e.printStackTrace(System.err);
			}
			logger.sendLn("Query to "+localName+" ("+calledAET+") complete");
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Done querying  "+localName));
			setCursor(was);
		}
	}

	protected class QueryActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			//new QueryRetrieveDialog("DicomCleaner Query",400,512);
			Properties properties = getProperties();
			String ae = properties.getProperty(propertyName_DicomCurrentlySelectedQueryTargetAE);
			ae = showInputDialogToSelectNetworkTargetByLocalApplicationEntityName("Select remote system","Query ...",ae);
			remoteQueryRetrievePanel.removeAll();
			if (ae != null) {
				setCurrentRemoteQueryInformationModel(ae);
				if (currentRemoteQueryInformationModel == null) {
					ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Cannot query "+ae));
				}
				else {
					try {
						SpecificCharacterSet specificCharacterSet = new SpecificCharacterSet((String[])null);
						AttributeList filter = new AttributeList();
						{
							AttributeTag t = TagFromName.PatientName; Attribute a = new PersonNameAttribute(t,specificCharacterSet);
							String patientName = queryFilterPatientNameTextField.getText().trim();
							if (patientName != null && patientName.length() > 0) {
								a.addValue(patientName);
							}
							filter.put(t,a);
						}
						{
							AttributeTag t = TagFromName.PatientID; Attribute a = new ShortStringAttribute(t,specificCharacterSet);
							String patientID = queryFilterPatientIDTextField.getText().trim();
							if (patientID != null && patientID.length() > 0) {
								a.addValue(patientID);
							}
							filter.put(t,a);
						}
						{ AttributeTag t = TagFromName.PatientBirthDate; Attribute a = new DateAttribute(t); filter.put(t,a); }
						{ AttributeTag t = TagFromName.PatientSex; Attribute a = new CodeStringAttribute(t); filter.put(t,a); }

						{ AttributeTag t = TagFromName.StudyID; Attribute a = new ShortStringAttribute(t,specificCharacterSet); filter.put(t,a); }
						{ AttributeTag t = TagFromName.StudyDescription; Attribute a = new LongStringAttribute(t,specificCharacterSet); filter.put(t,a); }
						{ AttributeTag t = TagFromName.ModalitiesInStudy; Attribute a = new CodeStringAttribute(t); filter.put(t,a); }
						{
							AttributeTag t = TagFromName.StudyDate; Attribute a = new DateAttribute(t);
							String studyDate = queryFilterStudyDateTextField.getText().trim();
							if (studyDate != null && studyDate.length() > 0) {
								a.addValue(studyDate);
							}
							filter.put(t,a);
						}
						{ AttributeTag t = TagFromName.StudyTime; Attribute a = new TimeAttribute(t); filter.put(t,a); }
						{ AttributeTag t = TagFromName.PatientAge; Attribute a = new AgeStringAttribute(t); filter.put(t,a); }

						{ AttributeTag t = TagFromName.SeriesDescription; Attribute a = new LongStringAttribute(t,specificCharacterSet); filter.put(t,a); }
						{ AttributeTag t = TagFromName.SeriesNumber; Attribute a = new IntegerStringAttribute(t); filter.put(t,a); }
						{ AttributeTag t = TagFromName.Modality; Attribute a = new CodeStringAttribute(t); filter.put(t,a); }
						{ AttributeTag t = TagFromName.SeriesDate; Attribute a = new DateAttribute(t); filter.put(t,a); }
						{ AttributeTag t = TagFromName.SeriesTime; Attribute a = new TimeAttribute(t); filter.put(t,a); }

						{ AttributeTag t = TagFromName.InstanceNumber; Attribute a = new IntegerStringAttribute(t); filter.put(t,a); }
						{ AttributeTag t = TagFromName.ContentDate; Attribute a = new DateAttribute(t); filter.put(t,a); }
						{ AttributeTag t = TagFromName.ContentTime; Attribute a = new TimeAttribute(t); filter.put(t,a); }
						{ AttributeTag t = TagFromName.ImageType; Attribute a = new CodeStringAttribute(t); filter.put(t,a); }
						{ AttributeTag t = TagFromName.NumberOfFrames; Attribute a = new IntegerStringAttribute(t); filter.put(t,a); }

						{ AttributeTag t = TagFromName.StudyInstanceUID; Attribute a = new UniqueIdentifierAttribute(t); filter.put(t,a); }
						{ AttributeTag t = TagFromName.SeriesInstanceUID; Attribute a = new UniqueIdentifierAttribute(t); filter.put(t,a); }
						{ AttributeTag t = TagFromName.SOPInstanceUID; Attribute a = new UniqueIdentifierAttribute(t); filter.put(t,a); }
						{ AttributeTag t = TagFromName.SOPClassUID; Attribute a = new UniqueIdentifierAttribute(t); filter.put(t,a); }
						{ AttributeTag t = TagFromName.SpecificCharacterSet; Attribute a = new CodeStringAttribute(t); filter.put(t,a); a.addValue("ISO_IR 100"); }

						activeThread = new Thread(new QueryWorker(filter));
						activeThread.start();
					}
					catch (Exception e) {
						e.printStackTrace(System.err);
						ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Query to "+ae+" failed"));
					}
				}
			}
			remoteQueryRetrievePanel.validate();
		}
	}

	protected void performRetrieve(AttributeList uniqueKeys,String selectionLevel,String retrieveAE) {
		try {
			AttributeList identifier = new AttributeList();
			if (uniqueKeys != null) {
				identifier.putAll(uniqueKeys);
				{ AttributeTag t = TagFromName.QueryRetrieveLevel; Attribute a = new CodeStringAttribute(t); a.addValue(selectionLevel); identifier.put(t,a); }
				currentRemoteQueryInformationModel.performHierarchicalMoveFrom(identifier,retrieveAE);
			}
			// else do nothing, since no unique key to specify what to retrieve
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
	
	protected class RetrieveActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			Cursor was = getCursor();
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			String localName = networkApplicationInformation.getLocalNameFromApplicationEntityTitle(currentRemoteQuerySelectionRetrieveAE);
			if (currentRemoteQuerySelectionLevel == null) {	// they have selected the root of the tree
				QueryTreeRecord parent = currentRemoteQuerySelectionQueryTreeRecord;
				if (parent != null) {
					ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Retrieving everything from "+localName));
					logger.sendLn("Retrieving everything from "+localName+" ("+currentRemoteQuerySelectionRetrieveAE+")");
					Enumeration children = parent.children();
					if (children != null) {
						while (children.hasMoreElements()) {
							QueryTreeRecord r = (QueryTreeRecord)(children.nextElement());
							if (r != null) {
								setCurrentRemoteQuerySelection(r.getUniqueKeys(),r.getUniqueKey(),r.getAllAttributesReturnedInIdentifier());
								ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Retrieving "+currentRemoteQuerySelectionLevel+" "+currentRemoteQuerySelectionUniqueKey.getSingleStringValueOrEmptyString()+" from "+localName));
								logger.sendLn("Retrieving "+currentRemoteQuerySelectionLevel+" "+currentRemoteQuerySelectionUniqueKey.getSingleStringValueOrEmptyString()+" from "+localName+" ("+currentRemoteQuerySelectionRetrieveAE+")");
								performRetrieve(currentRemoteQuerySelectionUniqueKeys,currentRemoteQuerySelectionLevel,currentRemoteQuerySelectionRetrieveAE);
							}
						}
					}
					ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Done sending retrieval request"));
					setCurrentRemoteQuerySelection(null,null,null);
				}
			}
			else {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Retrieving "+currentRemoteQuerySelectionLevel+" "+currentRemoteQuerySelectionUniqueKey.getSingleStringValueOrEmptyString()+" from "+localName));
				logger.sendLn("Request retrieval of "+currentRemoteQuerySelectionLevel+" "+currentRemoteQuerySelectionUniqueKey.getSingleStringValueOrEmptyString()+" from "+localName+" ("+currentRemoteQuerySelectionRetrieveAE+")");
				performRetrieve(currentRemoteQuerySelectionUniqueKeys,currentRemoteQuerySelectionLevel,currentRemoteQuerySelectionRetrieveAE);
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Done sending retrieval request"));
			}
			setCursor(was);
		}
	}

	protected class LogActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			if (logger instanceof DialogMessageLogger) {
				((DialogMessageLogger)logger).setVisible(true);
			}
		}
	}

	protected class ConfigureActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			try {
				storageSOPClassSCPDispatcher.shutdown();
				new NetworkApplicationConfigurationDialog(DicomCleaner.this.getContentPane(),networkApplicationInformation,networkApplicationProperties);
				// should now save properties to file
				networkApplicationProperties.getProperties(getProperties());
				storeProperties("Edited and saved from user interface");
				//getProperties().store(System.err,"Bla");
				activateStorageSCP();
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
	}
	
	Thread activeThread;
	
	protected class CancelActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			try {
				if (activeThread != null) {
					activeThread.interrupt();
				}
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
	}
	
	protected void createGUI() {
//System.err.println("DicomCleaner.createGUI()");
		setBackground(Color.lightGray);
		setInternationalizedFontsForGUI();
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
//System.err.println("DicomCleaner.windowClosing()");
				if (networkApplicationInformation != null && networkApplicationInformation instanceof NetworkApplicationInformationFederated) {
					((NetworkApplicationInformationFederated)networkApplicationInformation).removeAllSources();
				}
				dispose();
				System.exit(0);
			}
		});
	} 

	public DicomCleaner() throws DicomException, IOException {
		super(null,propertiesFileName);
		resourceBundle = ResourceBundle.getBundle(resourceBundleName);
		setTitle(resourceBundle.getString("applicationTitle"));

		activateTemporaryDatabases();
		savedImagesFolder = new File(System.getProperty("java.io.tmpdir"));
		
		try {
			networkApplicationProperties = new NetworkApplicationProperties(getProperties());
		}
		catch (Exception e) {
			networkApplicationProperties = null;
		}
		{
			NetworkApplicationInformationFederated federatedNetworkApplicationInformation = new NetworkApplicationInformationFederated();
			federatedNetworkApplicationInformation.startupAllKnownSourcesAndRegister(networkApplicationProperties);
			networkApplicationInformation = federatedNetworkApplicationInformation;
//System.err.println("networkApplicationInformation ...\n"+networkApplicationInformation);
		}
		activateStorageSCP();

		logger = new DialogMessageLogger("DicomCleaner Log",512,384,false/*exitApplicationOnClose*/,false/*visible*/);

		// ShutdownHook will run regardless of whether Command-Q (on Mac) or window closed ...
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
//System.err.println("DicomImageViewer.ShutdownHook.run()");
				if (networkApplicationInformation != null && networkApplicationInformation instanceof NetworkApplicationInformationFederated) {
					((NetworkApplicationInformationFederated)networkApplicationInformation).removeAllSources();
				}
//System.err.print(TransferMonitor.report());
			}
		});

		srcDatabasePanel = new JPanel();
		dstDatabasePanel = new JPanel();
		remoteQueryRetrievePanel = new JPanel();
	
		srcDatabasePanel.setLayout(new GridLayout(1,1));
		dstDatabasePanel.setLayout(new GridLayout(1,1));
		remoteQueryRetrievePanel.setLayout(new GridLayout(1,1));
		
		DatabaseTreeBrowser srcDatabaseTreeBrowser = new OurSourceDatabaseTreeBrowser(srcDatabase,srcDatabasePanel);
		DatabaseTreeBrowser dstDatabaseTreeBrowser = new OurDestinationDatabaseTreeBrowser(dstDatabase,dstDatabasePanel);

		Border panelBorder = BorderFactory.createEtchedBorder();

		JSplitPane pairOfLocalDatabaseBrowserPanes = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,srcDatabasePanel,dstDatabasePanel);
		pairOfLocalDatabaseBrowserPanes.setOneTouchExpandable(true);
		pairOfLocalDatabaseBrowserPanes.setResizeWeight(0.5);
		
		JSplitPane remoteAndLocalBrowserPanes = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,remoteQueryRetrievePanel,pairOfLocalDatabaseBrowserPanes);
		remoteAndLocalBrowserPanes.setOneTouchExpandable(true);
		remoteAndLocalBrowserPanes.setResizeWeight(0.4);		// you would think 0.33 would be equal, but it isn't
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		buttonPanel.setBorder(panelBorder);
		
		JButton configureButton = new JButton(resourceBundle.getString("configureButtonLabelText"));
		configureButton.setToolTipText(resourceBundle.getString("configureButtonToolTipText"));
		buttonPanel.add(configureButton);
		configureButton.addActionListener(new ConfigureActionListener());
		
		JButton logButton = new JButton(resourceBundle.getString("logButtonLabelText"));
		logButton.setToolTipText(resourceBundle.getString("logButtonToolTipText"));
		buttonPanel.add(logButton);
		logButton.addActionListener(new LogActionListener());
		
		JButton queryButton = new JButton(resourceBundle.getString("queryButtonLabelText"));
		queryButton.setToolTipText(resourceBundle.getString("queryButtonToolTipText"));
		buttonPanel.add(queryButton);
		queryButton.addActionListener(new QueryActionListener());
		
		JButton retrieveButton = new JButton(resourceBundle.getString("retrieveButtonLabelText"));
		retrieveButton.setToolTipText(resourceBundle.getString("retrieveButtonToolTipText"));
		buttonPanel.add(retrieveButton);
		retrieveButton.addActionListener(new RetrieveActionListener());
		
		JButton importButton = new JButton(resourceBundle.getString("importButtonLabelText"));
		importButton.setToolTipText(resourceBundle.getString("importButtonToolTipText"));
		buttonPanel.add(importButton);
		importButton.addActionListener(new ImportActionListener());
		
		JButton cleanButton = new JButton(resourceBundle.getString("cleanButtonLabelText"));
		cleanButton.setToolTipText(resourceBundle.getString("cleanButtonToolTipText"));
		buttonPanel.add(cleanButton);
		cleanButton.addActionListener(new CleanActionListener());
		
		JButton blackoutButton = new JButton(resourceBundle.getString("blackoutButtonLabelText"));
		blackoutButton.setToolTipText(resourceBundle.getString("blackoutButtonToolTipText"));
		buttonPanel.add(blackoutButton);
		blackoutButton.addActionListener(new BlackoutActionListener());
		
		JButton exportButton = new JButton(resourceBundle.getString("exportButtonLabelText"));
		exportButton.setToolTipText(resourceBundle.getString("exportButtonToolTipText"));
		buttonPanel.add(exportButton);
		exportButton.addActionListener(new ExportActionListener());
		
		JButton sendButton = new JButton(resourceBundle.getString("sendButtonLabelText"));
		sendButton.setToolTipText(resourceBundle.getString("sendButtonToolTipText"));
		buttonPanel.add(sendButton);
		sendButton.addActionListener(new SendActionListener());
		
		JButton purgeButton = new JButton(resourceBundle.getString("purgeButtonLabelText"));
		purgeButton.setToolTipText(resourceBundle.getString("purgeButtonToolTipText"));
		buttonPanel.add(purgeButton);
		purgeButton.addActionListener(new PurgeActionListener());
		
		//JButton cancelButton = new JButton(resourceBundle.getString("cancelButtonLabelText"));
		//cancelButton.setToolTipText(resourceBundle.getString("cancelButtonToolTipText"));
		//buttonPanel.add(cancelButton);
		//cancelButton.addActionListener(new CancelActionListener());
		
		JPanel queryFilterTextEntryPanel = new JPanel();
		queryFilterTextEntryPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		queryFilterTextEntryPanel.setBorder(panelBorder);

		JLabel queryIntroduction = new JLabel(resourceBundle.getString("queryIntroductionLabelText"));
		queryFilterTextEntryPanel.add(queryIntroduction);

		JLabel queryFilterPatientNameLabel = new JLabel(resourceBundle.getString("queryPatientNameLabelText"));
		queryFilterPatientNameLabel.setToolTipText(resourceBundle.getString("queryPatientNameToolTipText"));
		queryFilterTextEntryPanel.add(queryFilterPatientNameLabel);
		queryFilterPatientNameTextField = new JTextField("",textFieldLengthForQueryPatientName);
		queryFilterTextEntryPanel.add(queryFilterPatientNameTextField);
		
		JLabel queryFilterPatientIDLabel = new JLabel(resourceBundle.getString("queryPatientIDLabelText"));
		queryFilterPatientIDLabel.setToolTipText(resourceBundle.getString("queryPatientIDToolTipText"));
		queryFilterTextEntryPanel.add(queryFilterPatientIDLabel);
		queryFilterPatientIDTextField = new JTextField("",textFieldLengthForQueryPatientID);
		queryFilterTextEntryPanel.add(queryFilterPatientIDTextField);
		
		JLabel queryFilterStudyDateLabel = new JLabel(resourceBundle.getString("queryStudyDateLabelText"));
		queryFilterStudyDateLabel.setToolTipText(resourceBundle.getString("queryStudyDateToolTipText"));
		queryFilterTextEntryPanel.add(queryFilterStudyDateLabel);
		queryFilterStudyDateTextField = new JTextField("",textFieldLengthForQueryStudyDate);
		queryFilterTextEntryPanel.add(queryFilterStudyDateTextField);
		
		JPanel newTextEntryPanel = new JPanel();
		newTextEntryPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		newTextEntryPanel.setBorder(panelBorder);
		
		JLabel replacementIntroduction = new JLabel(resourceBundle.getString("replacementIntroductionLabelText"));
		newTextEntryPanel.add(replacementIntroduction);

		replacePatientNameCheckBox = new JCheckBox(resourceBundle.getString("replacementPatientNameLabelText"));
		replacePatientNameCheckBox.setSelected(true);
		replacePatientNameCheckBox.setToolTipText(resourceBundle.getString("replacementPatientNameToolTipText"));
		newTextEntryPanel.add(replacePatientNameCheckBox);
		replacementPatientNameTextField = new JTextField(resourceBundle.getString("defaultReplacementPatientName"),textFieldLengthForReplacementPatientName);
		newTextEntryPanel.add(replacementPatientNameTextField);
		
		replacePatientIDCheckBox = new JCheckBox(resourceBundle.getString("replacementPatientIDLabelText"));
		replacePatientIDCheckBox.setSelected(true);
		replacePatientIDCheckBox.setToolTipText(resourceBundle.getString("replacementPatientIDToolTipText"));
		newTextEntryPanel.add(replacePatientIDCheckBox);
		replacementPatientIDTextField = new JTextField(resourceBundle.getString("defaultReplacementPatientID"),textFieldLengthForReplacementPatientID);
		newTextEntryPanel.add(replacementPatientIDTextField);
		
		replaceAccessionNumberCheckBox = new JCheckBox(resourceBundle.getString("replacementAccessionNumberLabelText"));
		replaceAccessionNumberCheckBox.setSelected(true);
		replaceAccessionNumberCheckBox.setToolTipText(resourceBundle.getString("replacementAccessionNumberToolTipText"));
		newTextEntryPanel.add(replaceAccessionNumberCheckBox);
		replacementAccessionNumberTextField = new JTextField(resourceBundle.getString("defaultReplacementAccessionNumber"),textFieldLengthForReplacementAccessionNumber);
		newTextEntryPanel.add(replacementAccessionNumberTextField);

		JPanel checkBoxPanel = new JPanel();
		checkBoxPanel.setLayout(new GridLayout(0,4));	// number of rows is ignored if number of columns is not 0
		checkBoxPanel.setBorder(panelBorder);
				
		removeIdentityCheckBox = new JCheckBox(resourceBundle.getString("removeIdentityLabelText"));
		removeIdentityCheckBox.setSelected(true);
		checkBoxPanel.add(removeIdentityCheckBox);
			
		removeDescriptionsCheckBox = new JCheckBox(resourceBundle.getString("removeDescriptionsLabelText"));
		removeDescriptionsCheckBox.setSelected(false);
		checkBoxPanel.add(removeDescriptionsCheckBox);
		
		removeSeriesDescriptionsCheckBox = new JCheckBox(resourceBundle.getString("removeSeriesDescriptionsLabelText"));
		removeSeriesDescriptionsCheckBox.setSelected(false);
		checkBoxPanel.add(removeSeriesDescriptionsCheckBox);
	
		removeCharacteristicsCheckBox = new JCheckBox(resourceBundle.getString("removeCharacteristicsLabelText"));
		removeCharacteristicsCheckBox.setSelected(false);
		checkBoxPanel.add(removeCharacteristicsCheckBox);

		cleanUIDsCheckBox = new JCheckBox(resourceBundle.getString("cleanUIDsLabelText"));
		cleanUIDsCheckBox.setSelected(true);
		checkBoxPanel.add(cleanUIDsCheckBox);
		
		removePrivateCheckBox = new JCheckBox(resourceBundle.getString("removePrivateLabelText"));
		removePrivateCheckBox.setSelected(true);
		checkBoxPanel.add(removePrivateCheckBox);
		
		removeDeviceIdentityCheckBox = new JCheckBox(resourceBundle.getString("removeDeviceIdentityLabelText"));
		removeDeviceIdentityCheckBox.setSelected(false);
		checkBoxPanel.add(removeDeviceIdentityCheckBox);
		
		removeInstitutionIdentityCheckBox = new JCheckBox(resourceBundle.getString("removeInstitutionIdentityLabelText"));
		removeInstitutionIdentityCheckBox.setSelected(false);
		checkBoxPanel.add(removeInstitutionIdentityCheckBox);
	
		removeClinicalTrialAttributesBox = new JCheckBox(resourceBundle.getString("removeClinicalTrialAttributesLabelText"));
		removeClinicalTrialAttributesBox.setSelected(false);
		checkBoxPanel.add(removeClinicalTrialAttributesBox);
		
		addContributingEquipmentCheckBox = new JCheckBox(resourceBundle.getString("addContributingEquipmentLabelText"));
		addContributingEquipmentCheckBox.setSelected(true);
		checkBoxPanel.add(addContributingEquipmentCheckBox);
			
		zipExportCheckBox = new JCheckBox(resourceBundle.getString("zipExportLabelText"));
		zipExportCheckBox.setSelected(false);
		checkBoxPanel.add(zipExportCheckBox);
			
		hierarchicalExportCheckBox = new JCheckBox(resourceBundle.getString("hierarchicalExportLabelText"));
		hierarchicalExportCheckBox.setSelected(false);
		hierarchicalExportCheckBox.setToolTipText(resourceBundle.getString("hierarchicalExportToolTipText"));
		checkBoxPanel.add(hierarchicalExportCheckBox);
			
		JPanel statusBarPanel = new JPanel();
		{
			GridBagLayout statusBarPanelLayout = new GridBagLayout();
			statusBarPanel.setLayout(statusBarPanelLayout);
			{
				JLabel statusBar = getStatusBar();
				GridBagConstraints statusBarConstraints = new GridBagConstraints();
				statusBarConstraints.weightx = 1;
				statusBarConstraints.fill = GridBagConstraints.BOTH;
				statusBarConstraints.anchor = GridBagConstraints.WEST;
				statusBarConstraints.gridwidth = GridBagConstraints.RELATIVE;
				statusBarPanelLayout.setConstraints(statusBar,statusBarConstraints);
				statusBarPanel.add(statusBar);
			}
			{
				progressBar = new JProgressBar();
				progressBar.setStringPainted(false);
				GridBagConstraints progressBarConstraints = new GridBagConstraints();
				progressBarConstraints.weightx = 0.5;
				progressBarConstraints.fill = GridBagConstraints.BOTH;
				progressBarConstraints.anchor = GridBagConstraints.EAST;
				progressBarConstraints.gridwidth = GridBagConstraints.REMAINDER;
				statusBarPanelLayout.setConstraints(progressBar,progressBarConstraints);
				statusBarPanel.add(progressBar);
			}
		}
		
		JPanel mainPanel = new JPanel();
		{
			GridBagLayout mainPanelLayout = new GridBagLayout();
			mainPanel.setLayout(mainPanelLayout);
			{
				GridBagConstraints remoteAndLocalBrowserPanesConstraints = new GridBagConstraints();
				remoteAndLocalBrowserPanesConstraints.gridx = 0;
				remoteAndLocalBrowserPanesConstraints.gridy = 0;
				remoteAndLocalBrowserPanesConstraints.weightx = 1;
				remoteAndLocalBrowserPanesConstraints.weighty = 1;
				remoteAndLocalBrowserPanesConstraints.fill = GridBagConstraints.BOTH;
				mainPanelLayout.setConstraints(remoteAndLocalBrowserPanes,remoteAndLocalBrowserPanesConstraints);
				mainPanel.add(remoteAndLocalBrowserPanes);
			}
			{
				GridBagConstraints buttonPanelConstraints = new GridBagConstraints();
				buttonPanelConstraints.gridx = 0;
				buttonPanelConstraints.gridy = 1;
				buttonPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
				mainPanelLayout.setConstraints(buttonPanel,buttonPanelConstraints);
				mainPanel.add(buttonPanel);
			}
			{
				GridBagConstraints queryFilterTextEntryPanelConstraints = new GridBagConstraints();
				queryFilterTextEntryPanelConstraints.gridx = 0;
				queryFilterTextEntryPanelConstraints.gridy = 2;
				queryFilterTextEntryPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
				mainPanelLayout.setConstraints(queryFilterTextEntryPanel,queryFilterTextEntryPanelConstraints);
				mainPanel.add(queryFilterTextEntryPanel);
			}
			{
				GridBagConstraints newTextEntryPanelConstraints = new GridBagConstraints();
				newTextEntryPanelConstraints.gridx = 0;
				newTextEntryPanelConstraints.gridy = 3;
				newTextEntryPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
				mainPanelLayout.setConstraints(newTextEntryPanel,newTextEntryPanelConstraints);
				mainPanel.add(newTextEntryPanel);
			}
			{
				GridBagConstraints checkBoxPanelConstraints = new GridBagConstraints();
				checkBoxPanelConstraints.gridx = 0;
				checkBoxPanelConstraints.gridy = 4;
				checkBoxPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
				mainPanelLayout.setConstraints(checkBoxPanel,checkBoxPanelConstraints);
				mainPanel.add(checkBoxPanel);
			}
			{
				GridBagConstraints statusBarPanelConstraints = new GridBagConstraints();
				statusBarPanelConstraints.gridx = 0;
				statusBarPanelConstraints.gridy = 5;
				statusBarPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
				mainPanelLayout.setConstraints(statusBarPanel,statusBarPanelConstraints);
				mainPanel.add(statusBarPanel);
			}
		}
		Container content = getContentPane();
		content.add(mainPanel);
		pack();
		setVisible(true);
	}

	/**
	 * <p>The method to invoke the application.</p>
	 *
	 * @param	arg	none
	 */
	public static void main(String arg[]) {
		try {
			String osName = System.getProperty("os.name");
			if (osName != null && osName.toLowerCase(java.util.Locale.US).startsWith("windows")) {	// see "http://lopica.sourceforge.net/os.html" for list of values
System.err.println("DicomCleaner.main(): detected Windows - using Windows LAF");
				javax.swing.UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
			}
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
		}
		try {
			new DicomCleaner();
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
}

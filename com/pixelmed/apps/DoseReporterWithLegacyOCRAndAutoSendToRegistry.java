/* Copyright (c) 2001-2011, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.apps;

import com.pixelmed.database.DatabaseApplicationProperties;
import com.pixelmed.database.DatabaseInformationModel;
import com.pixelmed.database.MinimalPatientStudySeriesInstanceModel;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.AttributeTag;
import com.pixelmed.dicom.ClinicalTrialsAttributes;
import com.pixelmed.dicom.CodedSequenceItem;
import com.pixelmed.dicom.CodeStringAttribute;
import com.pixelmed.dicom.DateAttribute;
import com.pixelmed.dicom.DateTimeAttribute;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.DicomInputStream;
import com.pixelmed.dicom.FileMetaInformation;
import com.pixelmed.dicom.InformationEntity;
import com.pixelmed.dicom.IntegerStringAttribute;
import com.pixelmed.dicom.SOPClass;
import com.pixelmed.dicom.StoredFilePathStrategy;
import com.pixelmed.dicom.TagFromName;
import com.pixelmed.dicom.TransferSyntax;
import com.pixelmed.dicom.UniqueIdentifierAttribute;
import com.pixelmed.dicom.VersionAndConstants;

import com.pixelmed.dose.CTDose;

import com.pixelmed.doseocr.ExposureDoseSequence;
import com.pixelmed.doseocr.OCR;

import com.pixelmed.ftp.FTPApplicationProperties;
import com.pixelmed.ftp.FTPException;
import com.pixelmed.ftp.FTPFileSender;
import com.pixelmed.ftp.FTPRemoteHost;

import com.pixelmed.network.DicomNetworkException;
import com.pixelmed.network.NetworkApplicationInformation;
import com.pixelmed.network.NetworkApplicationInformationFederated;
import com.pixelmed.network.NetworkApplicationProperties;
import com.pixelmed.network.PresentationAddress;
import com.pixelmed.network.ReceivedObjectHandler;
import com.pixelmed.network.StorageSOPClassSCPDispatcher;

import com.pixelmed.query.QueryInformationModel;
import com.pixelmed.query.QueryTreeModel;
import com.pixelmed.query.QueryTreeRecord;
import com.pixelmed.query.StudyRootQueryInformationModel;

import com.pixelmed.utils.FileUtilities;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import java.security.NoSuchAlgorithmException;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * <p>A class to wait for incoming dose screen images and SRs and send them to pre-configured registry.</p>
 *
 * <p>The class has no public methods other than the constructor and a main method that is useful as a utility.</p>
 *
 * <p>External (unsecure) SQL access to the database is possible if the Application.DatabaseServerName property is specified; further
 * details are described in {@link com.pixelmed.database.DatabaseInformationModel com.pixelmed.database.DatabaseInformationModel}; for example:</p>
 * <pre>
% java -cp lib/additional/hsqldb.jar org.hsqldb.util.DatabaseManagerSwing --url "jdbc:hsqldb:hsql://localhost/testserverdb"
 * </pre>
 *
 * <p>For how to configure the necessary properties file, see:</p>
 *
 * @see com.pixelmed.network.NetworkApplicationProperties
 * @see com.pixelmed.database.DatabaseApplicationProperties
 *
 * @author	dclunie
 */
public class DoseReporterWithLegacyOCRAndAutoSendToRegistry {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/apps/DoseReporterWithLegacyOCRAndAutoSendToRegistry.java,v 1.12 2012/02/01 23:02:08 dclunie Exp $";
	
	protected static String defaultPropertiesFileName = ".com.pixelmed.apps.DoseReporterWithLegacyOCRAndAutoSendToRegistry.properties";
	
	protected static String propertyName_SelectedDoseRegistry                                         = "Application.SelectedDoseRegistry";
	protected static String propertyName_SleepTimeBetweenPassesToProcessReceivedFiles                 = "Application.SleepTimeBetweenPassesToProcessReceivedFiles";
	protected static String propertyName_IntervalAfterLastInstanceReceivedToWaitBeforeProcessingStudy = "Application.IntervalAfterLastInstanceReceivedToWaitBeforeProcessingStudy";
	protected static String propertyName_SleepTimeBetweenPassesToQueryRemoteAEs                       = "Application.SleepTimeBetweenPassesToQueryRemoteAEs";
	protected static String propertyName_DaysBackwardsFromTodayToQuery                                = "Application.DaysBackwardsFromTodayToQuery";
	protected static String propertyName_RetainSourceFilesUsedForSRGeneration                         = "Application.RetainSourceFilesUsedForSRGeneration";
	protected static String propertyName_RetainGeneratedRDSRFiles                                     = "Application.RetainGeneratedRDSRFiles";
	protected static String propertyName_RetainDeidentifiedFiles                                      = "Application.RetainDeidentifiedFiles";
	protected static String propertyName_RemoteAEsForQuery                                            = "Application.RemoteAEsForQuery";
	protected static String propertyName_ApplicationDebugLevel                                        = "Application.DebugLevel";
	protected static String propertyName_OCRDebugLevel                                                = "OCR.DebugLevel";

	protected static String propertyDelimitersForTokenizer_RemoteAEsForQuery = ", ";

	protected boolean retainDeidentifiedFiles;
	protected boolean retainGeneratedRDSRFiles;
	protected boolean retainSourceFilesUsedForSRGeneration;
	
	protected String defaultRetainDeidentifiedFiles              = Boolean.toString(false);
	protected String defaultRetainGeneratedRDSRFiles             = Boolean.toString(false);
	protected String defaultRetainSourceFilesUsedForSRGeneration = Boolean.toString(false);
	
	protected String defaultSleepTimeBetweenPassesToProcessReceivedFiles                 = "60";	// 1 minute
	protected String defaultIntervalAfterLastInstanceReceivedToWaitBeforeProcessingStudy = "60";	// 1 minute
	protected String defaultSleepTimeBetweenPassesToQueryRemoteAEs                       = "60";	// 1 minute
	protected String defaultDaysBackwardsFromTodayToQuery                                 = "0";	// today only
	
	protected static int sleepTimeBetweenPassesToProcessReceivedFiles;					// seconds
	protected static int intervalAfterLastInstanceReceivedToWaitBeforeProcessingStudy;	// seconds
	protected static int sleepTimeBetweenPassesToQueryRemoteAEs;						// seconds
	protected static int daysBackwardsFromTodayToQuery;									// days
	
	protected static final long millisecondsPerDay = 1000 * 60 * 60 * 24;
	
	protected Properties properties;
	
	protected NetworkApplicationProperties networkApplicationProperties;
	protected NetworkApplicationInformationFederated networkApplicationInformation;
	
	protected String ourCalledAETitle;
	
	protected List<String> remoteAEsForQuery;
	
	protected DatabaseInformationModel databaseInformationModel;
	
	protected FTPRemoteHost remoteHost;
	
	protected int ftpClientDebugLevel;
	protected int ocrDebugLevel;
	protected int applicationDebugLevel;

	protected String buildDate = getBuildDate();
	
	protected File savedImagesFolder;
	protected StoredFilePathStrategy storedFilePathStrategy = StoredFilePathStrategy.BYSOPINSTANCEUIDHASHSUBFOLDERS;
	
	protected String studyHasBeenProcessedColumnName          = "PM_STUDYHASBEENPROCESSED";			// needs to be upper case ... indicates that all instances were successfully and study is not to be processed again
	protected String instanceHasBeenSentToRegistryColumnName  = "PM_INSTANCEHASBEENSENTTOREGISTRY";	// needs to be upper case ... indicates that instance was successfully sent
	protected String instanceIsRadiationDoseSRColumnName      = "PM_ISRADIATIONDOSESR";				// needs to be upper case ... indicates that instance is an RDSR SOP Class or SR with correct document title (whether OEM or our own legacy OCR'd)
	protected String instanceIsRadiationDoseScreenColumnName  = "PM_ISRADIATIONDOSESCREEN";			// needs to be upper case ... indicates that instance is a dose screen
	protected String instanceIsExposureDoseSequenceColumnName = "PM_ISEXPOSUREDOSESEQUENCE";		// needs to be upper case ... indicates that instance contains exposure dose sequence
	
	protected String studyInstanceUIDColumnName;
	protected String sopClassUIDColumnName;
	protected String manufacturerColumnName;
	protected String imageTypeColumnName;
	protected String instanceCreatorUIDColumnName;
	protected String sourceApplicationEntityTitleColumnName;
	protected String instanceLocalParentReferenceColumnName;
	protected String instanceLocalFileNameColumnName;
	protected String instanceLocalFileReferenceTypeColumnName;
	protected String instanceLocalPrimaryKeyColumnName;


	/**
	 * <p>Get the date the package was built.</p>
	 *
	 * @return	 the build date
	 */
	// copied from ApplicationFrame - should refactor :(
	protected String getBuildDate() {
		String buildDate = "";
		try {
			buildDate = (new BufferedReader(new InputStreamReader(DoseReporterWithLegacyOCRAndAutoSendToRegistry.class.getResourceAsStream("/BUILDDATE")))).readLine();
		}
		catch (IOException e) {
			e.printStackTrace(System.err);
		}
		return buildDate;
	}

	/**
	 * <p>Load properties.</p>
	 *
	 * @exception	IOException	thrown if properties file is missing
	 */
	protected void loadProperties(String propertiesFileName) throws IOException {
		properties = new Properties(/*defaultProperties*/);
		FileInputStream in = new FileInputStream(propertiesFileName);
		properties.load(in);
		in.close();
	}
	
	// largely copied from DoseUtility - should be refactored :(
	protected String deidentifyFile(String dicomFileName) throws DicomException, IOException {
if (applicationDebugLevel > 1) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.deidentifyFile(): doing file "+dicomFileName);
		File file = new File(dicomFileName);
		DicomInputStream i = new DicomInputStream(file);
		AttributeList list = new AttributeList();
		list.read(i);
		i.close();

		list.removeGroupLengthAttributes();
		list.correctDecompressedImagePixelModule();
		list.insertLossyImageCompressionHistoryIfDecompressed();
		list.removeMetaInformationHeaderAttributes();
					
		list.removeUnsafePrivateAttributes();
		ClinicalTrialsAttributes.removeClinicalTrialsAttributes(list);

		ClinicalTrialsAttributes.removeOrNullIdentifyingAttributes(list,ClinicalTrialsAttributes.HandleUIDs.keep,true/*keepDescriptors*/,true/*keepSeriesDescriptors*/,true/*keepPatientCharacteristics*/,true/*keepDeviceIdentity*/,true/*keepInstitutionIdentity*/);

		ClinicalTrialsAttributes.addContributingEquipmentSequence(list,
			true,
			new CodedSequenceItem("109104","DCM","De-identifying Equipment"),	// per CP 892
			"PixelMed",														// Manufacturer
			null,															// Institution Name
			null,															// Institutional Department Name
			null		,													// Institution Address
			ourCalledAETitle,												// Station Name
			"DoseReporterWithLegacyOCRAndAutoSendToRegistry",				// Manufacturer's Model Name
			null,															// Device Serial Number
			buildDate,														// Software Version(s)
			"De-identified",
			DateTimeAttribute.getFormattedString(new java.util.Date()));

		FileMetaInformation.addFileMetaInformation(list,TransferSyntax.ExplicitVRLittleEndian,ourCalledAETitle);
		list.insertSuitableSpecificCharacterSetForAllStringValues();	// E.g., may have de-identified Kanji name and need new character set
		File deidentifiedFile = File.createTempFile("clean",".dcm");
		String deidentifiedFileName = deidentifiedFile.getCanonicalPath();
if (applicationDebugLevel > 1) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.deidentifyFile(): deidentified file is "+deidentifiedFileName);
		if (!retainDeidentifiedFiles) {
			deidentifiedFile.deleteOnExit();	// will be explicitly deleted anyway by caller, but just in case
		}
		list.write(deidentifiedFile);
		return deidentifiedFileName;
	}
	
	protected void sendFileToRegistry(String fileName) throws DicomException, IOException, NoSuchAlgorithmException, Exception {
		String deidentifiedFileName = deidentifyFile(fileName);
		String[] fileNamesToSend = { deidentifiedFileName };
		new FTPFileSender(remoteHost,fileNamesToSend,true/*generate random remote file names*/,ftpClientDebugLevel,null/*logger*/,null/*progressBar*/);
		// any reason to fail will throw exception, and will only get here to set success if no exception is thrown
if (applicationDebugLevel > 0) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.sendFileToRegistry(): successful send of deidentified version of "+fileName);
		if (!retainDeidentifiedFiles) {
if (applicationDebugLevel > 1) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.sendFileToRegistry(): deleting deidentified file "+deidentifiedFileName);
			try {
				if (!new File(deidentifiedFileName).delete()) {
					throw new DicomException("Failed to delete deidentified file that we sent to registry "+deidentifiedFileName);
				}
			}
			catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
	}
	
	protected void sendFileToRegistry(CandidateFile candidateFile) throws DicomException, IOException, NoSuchAlgorithmException, Exception {
		if (candidateFile.localPrimaryKeyValue == null) {
			// this check should not be necessary since defect that failed to set this in CandidateFile constructor has been fixed, but just in case ...
			throw new Exception("Internal error - did not receive localPrimaryKeyValue - not attempting to send since will not be able to update database with instance has been sent");
		}
		try {
			sendFileToRegistry(candidateFile.fileName);
if (applicationDebugLevel > 1) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.sendFileToRegistry(): setting instance has been sent flag for "+candidateFile.localPrimaryKeyValue+" "+candidateFile.fileName);
			databaseInformationModel.updateSelectedRecord(InformationEntity.INSTANCE,candidateFile.localPrimaryKeyValue,instanceHasBeenSentToRegistryColumnName,"TRUE");
			candidateFile.instanceHasBeenSent = true;	// don't think this is necessary, since it won't be used again this pass, but just in case
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
	
	protected void sendFilesToRegistry(Set<CandidateFile> setOfSRFiles) throws DicomException, IOException, NoSuchAlgorithmException, Exception {
		for (CandidateFile candidateFile : setOfSRFiles) {
			if (!candidateFile.instanceHasBeenSent) {
if (applicationDebugLevel > 1) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.sendFilesToRegistry(): attempting to send = "+candidateFile.fileName);
				sendFileToRegistry(candidateFile);		// if any send fails and throws exception, the remainder will not be processed
if (applicationDebugLevel > 1) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.sendFilesToRegistry(): sending was successful = "+candidateFile.fileName);
			}
			else {
if (applicationDebugLevel > 1) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.sendFilesToRegistry(): already sent so not sending again = "+candidateFile.fileName);
			}
		}
	}

	// derived from LegacyRadiationDoseOCRDicomForwardingService.sendSRFile() .. should refactor :(
	protected void makeSRFileAndSendFileToRegistry(CTDose ctDose) {
		try {
			AttributeList ctDoseList = ctDose.getAttributeList();
			File ctDoseSRFile = storedFilePathStrategy.makeReliableStoredFilePathWithFoldersCreated(savedImagesFolder,Attribute.getSingleStringValueOrDefault(ctDoseList,TagFromName.SOPInstanceUID,"1.1.1.1"));
			String ctDoseSRFileName = ctDoseSRFile.getCanonicalPath();
			ctDose.write(ctDoseSRFileName,ourCalledAETitle,this.getClass().getCanonicalName());	// has side effect of updating list returned by ctDose.getAttributeList(); uncool :(
			try {
if (applicationDebugLevel > 0) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.makeSRFileAndSendFileToRegistry(): sending our own newly created SR file = "+ctDoseSRFileName);
				sendFileToRegistry(ctDoseSRFileName);
			}
			catch (Exception e) {
				e.printStackTrace(System.err);
			}
			if (ctDoseSRFile != null) {
				if (retainGeneratedRDSRFiles) {
					databaseInformationModel.insertObject(ctDoseList,ctDoseSRFileName,DatabaseInformationModel.FILE_COPIED);
					setSelectedDatabaseRecordIsRadiationDoseSR(ctDoseSRFileName);
					setSelectedDatabaseRecordHasBeenSentToRegistry(ctDoseSRFileName);
				}
				else {
					try {
						if (!ctDoseSRFile.delete()) {
							throw new DicomException("Failed to delete RDSR file that we created "+ctDoseSRFileName);
						}
					}
					catch (Exception e) {
						e.printStackTrace(System.err);
					}
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
	
	// something like this method should probably be factored out and added to DatabaseInformationModel, :(
	// but here we specifically want to delete instances and prune series, but NOT prune parent study even
	// if empty, to retain processed flag (? actually necessary to stop at series, since only invoked when
	// both OEM and ours, and OEM won't be deleted?)
	protected void deleteFilesAndDatabaseRecords(Set<CandidateFile> setOfFiles) throws DicomException, IOException {
		Set<String> seriesToDeleteIfEmpty = new HashSet<String>();
		for (CandidateFile candidateFile : setOfFiles) {
if (applicationDebugLevel > 1) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.deleteFilesAndDatabaseRecords(): deleting file "+candidateFile.fileName);
			if (!new File(candidateFile.fileName).delete()) {
				throw new DicomException("Failed to delete file "+candidateFile.fileName);
			}
if (applicationDebugLevel > 1) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.deleteFilesAndDatabaseRecords(): deleting database instance "+candidateFile.localPrimaryKeyValue);
			databaseInformationModel.deleteRecord(InformationEntity.INSTANCE,candidateFile.localPrimaryKeyValue);
			if (candidateFile.localParentReference != null && candidateFile.localParentReference.length() > 0) {
				seriesToDeleteIfEmpty.add(candidateFile.localParentReference);
			}
		}
		for (String seriesLocalPrimaryKeyValue : seriesToDeleteIfEmpty) {
			List<Map<String,String>> childInstances = databaseInformationModel.findAllAttributeValuesForAllRecordsForThisInformationEntityWithSpecifiedParent(InformationEntity.INSTANCE,seriesLocalPrimaryKeyValue);
			if (childInstances == null || childInstances.isEmpty()) {
if (applicationDebugLevel > 1) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.deleteFilesAndDatabaseRecords(): pruning empty series = "+seriesLocalPrimaryKeyValue);
				databaseInformationModel.deleteRecord(InformationEntity.SERIES,seriesLocalPrimaryKeyValue);
			}
			else {
if (applicationDebugLevel > 1) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.deleteFilesAndDatabaseRecords(): surprisingly, series is not empty after deletion of instances = "+seriesLocalPrimaryKeyValue);
			}
		}
	}
	
	protected void deleteFilesAndSetDatabaseRecordsToReferenced(Set<CandidateFile> setOfFiles) throws DicomException, IOException {
		for (CandidateFile candidateFile : setOfFiles) {
if (applicationDebugLevel > 1) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.deleteFilesAndSetDatabaseRecordsToReferenced(): deleting file "+candidateFile.fileName);
			if (!new File(candidateFile.fileName).delete()) {
				throw new DicomException("Failed to delete file "+candidateFile.fileName);
			}
if (applicationDebugLevel > 1) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.deleteFilesAndSetDatabaseRecordsToReferenced(): deleting database instance "+candidateFile.localPrimaryKeyValue);
			databaseInformationModel.updateSelectedRecord(InformationEntity.INSTANCE,candidateFile.localPrimaryKeyValue,instanceLocalFileReferenceTypeColumnName,DatabaseInformationModel.FILE_REFERENCED);
			databaseInformationModel.updateSelectedRecord(InformationEntity.INSTANCE,candidateFile.localPrimaryKeyValue,instanceLocalFileNameColumnName,"NULL");
		}
	}
	
	protected class CandidateFile {
		String localPrimaryKeyValue;
		String localParentReference;
		String fileName;
		long   insertionTime;
		String sopClassUID;
		String instanceCreatorUID;
		String sourceApplicationEntityTitle;
		String manufacturer;
		String imageType;
		boolean instanceHasBeenSent;
		boolean isRadiationDoseSR;
		boolean isRadiationDoseScreen;

		CandidateFile(String localPrimaryKeyValue) throws DicomException {
			this.localPrimaryKeyValue = localPrimaryKeyValue;	// Don't forget this ! Needed to use for later database updates of instanceHasBeenSent status
if (applicationDebugLevel > 2) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.CandidateFile(): localPrimaryKeyValue = "+localPrimaryKeyValue);
			Map<String,String> record = databaseInformationModel.findAllAttributeValuesForSelectedRecord(InformationEntity.INSTANCE,localPrimaryKeyValue);
			localParentReference = record.get(instanceLocalParentReferenceColumnName);
if (applicationDebugLevel > 2) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.CandidateFile(): localParentReference = "+localParentReference);
			fileName = record.get(databaseInformationModel.getLocalFileNameColumnName(InformationEntity.INSTANCE));
if (applicationDebugLevel > 2) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.CandidateFile(): fileName = "+fileName);

			String insertionTimeString = record.get(databaseInformationModel.getLocalRecordInsertionTimeColumnName(InformationEntity.INSTANCE));
			if (insertionTimeString == null) {
				insertionTime = 0;	// lowest value possible
			}
			else {
				try {
					insertionTime = Long.parseLong(insertionTimeString);
				}
				catch (NumberFormatException e) {
					e.printStackTrace(System.err);
					insertionTime = 0;	// lowest value possible
				}
			}
if (applicationDebugLevel > 2) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.CandidateFile(): insertionTime = "+insertionTime);

			sopClassUID = record.get(sopClassUIDColumnName);
if (applicationDebugLevel > 2) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.CandidateFile(): sopClassUID = "+sopClassUID);
			instanceCreatorUID = record.get(instanceCreatorUIDColumnName);
if (applicationDebugLevel > 2) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.CandidateFile(): instanceCreatorUID = "+instanceCreatorUID);
			sourceApplicationEntityTitle = record.get(sourceApplicationEntityTitleColumnName);
if (applicationDebugLevel > 2) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.CandidateFile(): sourceApplicationEntityTitle = "+sourceApplicationEntityTitle);

			manufacturer = record.get(manufacturerColumnName);
if (applicationDebugLevel > 2) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.CandidateFile(): manufacturer = "+manufacturer);
			imageType = record.get(imageTypeColumnName);
if (applicationDebugLevel > 2) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.CandidateFile(): imageType = "+imageType);

			String instanceHasBeenSentValue = record.get(instanceHasBeenSentToRegistryColumnName);
			instanceHasBeenSent = instanceHasBeenSentValue != null && instanceHasBeenSentValue.toUpperCase(java.util.Locale.US).equals("TRUE");
if (applicationDebugLevel > 2) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.CandidateFile(): instanceHasBeenSent = "+instanceHasBeenSent);

			String isRadiationDoseSRValue = record.get(instanceIsRadiationDoseSRColumnName);
			isRadiationDoseSR = isRadiationDoseSRValue != null && isRadiationDoseSRValue.toUpperCase(java.util.Locale.US).equals("TRUE");
if (applicationDebugLevel > 2) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.CandidateFile(): isRadiationDoseSR = "+isRadiationDoseSR);

			String isRadiationDoseScreenValue = record.get(instanceIsRadiationDoseScreenColumnName);
			isRadiationDoseScreen = isRadiationDoseScreenValue != null && isRadiationDoseScreenValue.toUpperCase(java.util.Locale.US).equals("TRUE");
if (applicationDebugLevel > 2) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.CandidateFile(): isRadiationDoseScreen = "+isRadiationDoseScreen);
		}

		boolean isOKToOCR() {
			return isRadiationDoseScreen;
		}

		// must have been flagged as RDSR when received (or legacy extracted), and, just to be safe, confirmed to be an SR file
		boolean isOKToSendToRegistry() {
			return isRadiationDoseSR && (sopClassUID != null && SOPClass.isStructuredReport(sopClassUID));
		}

		boolean wasLocallyCreated() {
			return instanceCreatorUID != null && instanceCreatorUID.equals(VersionAndConstants.instanceCreatorUID)
			 && sourceApplicationEntityTitle != null && sourceApplicationEntityTitle.equals(ourCalledAETitle);
		}
	}
	
	protected long findCandidateFilesToSendToRegistry(InformationEntity ie,String localPrimaryKeyValue,Set<CandidateFile> setOfOriginalSRFiles,Set<CandidateFile> setOfLegacyOCRSRFiles,Set<CandidateFile> setOfDoseScreenFiles,long mostRecentInsertionTime) throws DicomException {
if (applicationDebugLevel > 2) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.findCandidateFilesToSendToRegistry(): mostRecentInsertionTime on entry = "+mostRecentInsertionTime);
		if (ie == InformationEntity.INSTANCE) {
			CandidateFile candidateFile = new CandidateFile(localPrimaryKeyValue);
if (applicationDebugLevel > 2) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.findCandidateFilesToSendToRegistry(): mostRecentInsertionTime = "+mostRecentInsertionTime);
if (applicationDebugLevel > 2) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.findCandidateFilesToSendToRegistry(): candidateFile.insertionTime = "+candidateFile.insertionTime);
			if (candidateFile.insertionTime > mostRecentInsertionTime) {
				mostRecentInsertionTime = candidateFile.insertionTime;
			}
			if (candidateFile.isOKToSendToRegistry()) {
if (applicationDebugLevel > 0) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.findCandidateFilesToSendToRegistry(): is RDSR fileName = "+candidateFile.fileName);
				if (candidateFile.wasLocallyCreated()) {
if (applicationDebugLevel > 0) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.findCandidateFilesToSendToRegistry(): was legacy extracted RDSR fileName = "+candidateFile.fileName);
					setOfLegacyOCRSRFiles.add(candidateFile);
				}
				else {
					setOfOriginalSRFiles.add(candidateFile);
				}	
			}
			else if (candidateFile.isOKToOCR()) {
if (applicationDebugLevel > 0) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.findCandidateFilesToSendToRegistry(): is dose screen fileName = "+candidateFile.fileName);
				setOfDoseScreenFiles.add(candidateFile);
			}
			else {
if (applicationDebugLevel > 0) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.findCandidateFilesToSendToRegistry(): is not RDSR or dose screen fileName = "+candidateFile.fileName);
			}
		}
		else {
			InformationEntity childIE = databaseInformationModel.getChildTypeForParent(ie);
			List<Map<String,String>> returnedRecords = databaseInformationModel.findAllAttributeValuesForAllRecordsForThisInformationEntityWithSpecifiedParent(childIE,localPrimaryKeyValue);
			for (Map<String,String> record : returnedRecords) {
				String childLocalPrimaryKeyValue = record.get(databaseInformationModel.getLocalPrimaryKeyColumnName(childIE));
if (applicationDebugLevel > 2) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.findCandidateFilesToSendToRegistry(): mostRecentInsertionTime before recursing = "+mostRecentInsertionTime);
				mostRecentInsertionTime = findCandidateFilesToSendToRegistry(childIE,childLocalPrimaryKeyValue,setOfOriginalSRFiles,setOfLegacyOCRSRFiles,setOfDoseScreenFiles,mostRecentInsertionTime);
if (applicationDebugLevel > 2) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.findCandidateFilesToSendToRegistry(): mostRecentInsertionTime after recursing = "+mostRecentInsertionTime);
			}
		}
		return mostRecentInsertionTime;
	}
	
	protected boolean findSuitableSRFilesAndSendThemToRegistry(String studyLocalPrimaryKeyValue) throws DicomException, IOException, NoSuchAlgorithmException, Exception {
		boolean processed = false;
		Set<CandidateFile>  setOfOriginalSRFiles = new HashSet<CandidateFile>();
		Set<CandidateFile> setOfLegacyOCRSRFiles = new HashSet<CandidateFile>();
		Set<CandidateFile>  setOfDoseScreenFiles = new HashSet<CandidateFile>();
		long mostRecentInsertionTime = findCandidateFilesToSendToRegistry(InformationEntity.STUDY,studyLocalPrimaryKeyValue,setOfOriginalSRFiles,setOfLegacyOCRSRFiles,setOfDoseScreenFiles,0l);
		
		long currentTimeMillis = System.currentTimeMillis();
if (applicationDebugLevel > 2) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.sendFilesToRegistry(): currentTimeMillis = "+currentTimeMillis);
if (applicationDebugLevel > 2) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.sendFilesToRegistry(): mostRecentInsertionTime = "+mostRecentInsertionTime);
		long secondsSinceMostRecentInsertion = (currentTimeMillis - mostRecentInsertionTime) / 1000;
if (applicationDebugLevel > 1) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.sendFilesToRegistry(): secondsSinceMostRecentInsertion = "+secondsSinceMostRecentInsertion);
		if (secondsSinceMostRecentInsertion > intervalAfterLastInstanceReceivedToWaitBeforeProcessingStudy) {
if (applicationDebugLevel > 0) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.sendFilesToRegistry(): processing, since old enough");

			// prefer to send original manufacturer's files, if any, else ours that already exist, if any
			if (setOfOriginalSRFiles.isEmpty()) {
				if (setOfLegacyOCRSRFiles.isEmpty()) {
					if (!setOfDoseScreenFiles.isEmpty()) {
						// actually do the OCR that was deferred to this point to handle multi-page files
						List<String> fileNames = new LinkedList<String>();
						for (CandidateFile cf : setOfDoseScreenFiles) {
							fileNames.add(cf.fileName);
						}
						OCR ocr = new OCR(fileNames,ocrDebugLevel);
//System.err.print(ocr);
						CTDose ctDose = ocr.getCTDoseFromOCROfDoseScreen(ocr,ocrDebugLevel,null/*eventDataFromImages*/,true/*buildSR*/);
						if (ctDose != null) {
if (applicationDebugLevel > 0) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.sendFilesToRegistry(): sending our newly OCRd legacy extracted SR file (made from "+fileNames.size()+" screens)");
							makeSRFileAndSendFileToRegistry(ctDose);
							if (!retainSourceFilesUsedForSRGeneration) {
								try {
									deleteFilesAndSetDatabaseRecordsToReferenced(setOfDoseScreenFiles);
								}
								catch (Exception e) {
									// trap this, since do not want cleanup to interfere
									e.printStackTrace(System.err);
								}
							}
						}
					}
				}
				else {
if (applicationDebugLevel > 0) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.sendFilesToRegistry(): sending our previously created legacy extracted SR files");
					sendFilesToRegistry(setOfLegacyOCRSRFiles);
				}
			}
			else {
if (applicationDebugLevel > 0) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.sendFilesToRegistry(): sending manufacturer's SR files");
				sendFilesToRegistry(setOfOriginalSRFiles);
			}

			{
				// if any legacy files were created as well, take the opportunity to delete them, and purge them from the database
				if (!retainGeneratedRDSRFiles && !setOfLegacyOCRSRFiles.isEmpty()) {
					try {
						deleteFilesAndDatabaseRecords(setOfLegacyOCRSRFiles);
					}
					catch (Exception e) {
						// trap this, since do not want cleanup to interfere with falling through to setting processed to true
						e.printStackTrace(System.err);
					}
				}
			}
			
			processed = true;	// will be set if there was nothing to be sent, or anything to be sent was sent without exceptions
		}
		else {
if (applicationDebugLevel > 0) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.sendFilesToRegistry(): not processing, since too recent");
		}
		
		return processed;
	}

	protected class WatchDatabaseAndSendToRegistry implements Runnable {
		public void run() {
			boolean interrupted = false;
			while (!interrupted) {
if (applicationDebugLevel > 1) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.WatchDatabaseAndSendToRegistry.run(): Starting or waking up WatchDatabaseAndSendToRegistry ...");
				try {
					List<Map<String,String>> returnedRecords = databaseInformationModel.findAllAttributeValuesForAllRecordsForThisInformationEntity(InformationEntity.STUDY);
					{
						for (Map<String,String> record : returnedRecords) {
							if (applicationDebugLevel > 2) {
								System.err.println("STUDY:");
								for (String key : record.keySet()) {
									System.err.println("\t"+key+" = "+record.get(key));
								}
							}
							String studyLocalPrimaryKeyValue = record.get(databaseInformationModel.getLocalPrimaryKeyColumnName(InformationEntity.STUDY));
							String studyHasBeenProcessedValue = record.get(studyHasBeenProcessedColumnName);
							boolean studyHasBeenProcessed = studyHasBeenProcessedValue != null && studyHasBeenProcessedValue.toUpperCase(java.util.Locale.US).equals("TRUE");
							if (studyHasBeenProcessed) {
if (applicationDebugLevel > 1) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.WatchDatabaseAndSendToRegistry.run(): Already processed "+record.get(studyInstanceUIDColumnName));
							}
							else {
if (applicationDebugLevel > 0) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.WatchDatabaseAndSendToRegistry.run(): Processing "+record.get(studyInstanceUIDColumnName));
								try {
									if(findSuitableSRFilesAndSendThemToRegistry(studyLocalPrimaryKeyValue)) {
										// returned true (success) only if ALL selected files were successfully sent
										databaseInformationModel.updateSelectedRecord(InformationEntity.STUDY,studyLocalPrimaryKeyValue,studyHasBeenProcessedColumnName,"TRUE");										
									}
								}
								catch (Exception e) {
									e.printStackTrace(System.err);
									// do not set study processed to true, since failure may be transient and can try again next time
								}
							}
						}
					}
					

if (applicationDebugLevel > 1) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.WatchDatabaseAndSendToRegistry.run(): sleeping for "+sleepTimeBetweenPassesToProcessReceivedFiles+" seconds");
					Thread.currentThread().sleep(sleepTimeBetweenPassesToProcessReceivedFiles*1000);	// configured value is in seconds, sleep() parameter is in milliseconds
				}
				catch (DicomException e) {
					e.printStackTrace(System.err);
				}
				catch (InterruptedException e) {
if (applicationDebugLevel > 1) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.WatchDatabaseAndSendToRegistry.run(): interrupted: "+e);
					interrupted = true;		// currently this shouldn't happen; i.e., no other thread will interrupt this one whilst sleeping (?)
				}
			}
		}
	}
	
	protected void setSelectedDatabaseRecordIsRadiationDoseSR(String filename) throws DicomException {
if (applicationDebugLevel > 2) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.setSelectedDatabaseRecordIsRadiationDoseSR.run(): filename: "+filename);
		ArrayList<Map<String,String>> records = databaseInformationModel.findAllAttributeValuesForAllRecordsForThisInformationEntityWithSpecifiedKeyValue(
			InformationEntity.INSTANCE,instanceLocalFileNameColumnName,filename);
if (applicationDebugLevel > 2) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.setSelectedDatabaseRecordIsRadiationDoseSR.run(): records.size() = "+records.size());
		for (Map<String,String> record : records) {	// should only be one
			String localPrimaryKeyValue = record.get(instanceLocalPrimaryKeyColumnName);
if (applicationDebugLevel > 2) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.setSelectedDatabaseRecordIsRadiationDoseSR.run(): localPrimaryKeyValue = "+localPrimaryKeyValue);
			if (localPrimaryKeyValue != null && localPrimaryKeyValue.length() > 0) {
				databaseInformationModel.updateSelectedRecord(InformationEntity.INSTANCE,localPrimaryKeyValue,instanceIsRadiationDoseSRColumnName,"TRUE");
			}
		}
	}
	
	protected void setSelectedDatabaseRecordIsRadiationDoseScreen(String filename) throws DicomException {
if (applicationDebugLevel > 2) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.setSelectedDatabaseRecordIsRadiationDoseScreen.run(): filename: "+filename);
		ArrayList<Map<String,String>> records = databaseInformationModel.findAllAttributeValuesForAllRecordsForThisInformationEntityWithSpecifiedKeyValue(
			InformationEntity.INSTANCE,instanceLocalFileNameColumnName,filename);
if (applicationDebugLevel > 2) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.setSelectedDatabaseRecordIsRadiationDoseScreen.run(): records.size() = "+records.size());
		for (Map<String,String> record : records) {	// should only be one
			String localPrimaryKeyValue = record.get(instanceLocalPrimaryKeyColumnName);
if (applicationDebugLevel > 2) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.setSelectedDatabaseRecordIsRadiationDoseScreen.run(): localPrimaryKeyValue = "+localPrimaryKeyValue);
			if (localPrimaryKeyValue != null && localPrimaryKeyValue.length() > 0) {
				databaseInformationModel.updateSelectedRecord(InformationEntity.INSTANCE,localPrimaryKeyValue,instanceIsRadiationDoseScreenColumnName,"TRUE");
			}
		}
	}
	
	protected void setSelectedDatabaseRecordIsExposureDoseSequence(String filename) throws DicomException {
if (applicationDebugLevel > 2) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.setSelectedDatabaseRecordIsExposureDoseSequence.run(): filename: "+filename);
		ArrayList<Map<String,String>> records = databaseInformationModel.findAllAttributeValuesForAllRecordsForThisInformationEntityWithSpecifiedKeyValue(
			InformationEntity.INSTANCE,instanceLocalFileNameColumnName,filename);
if (applicationDebugLevel > 2) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.setSelectedDatabaseRecordIsExposureDoseSequence.run(): records.size() = "+records.size());
		for (Map<String,String> record : records) {	// should only be one
			String localPrimaryKeyValue = record.get(instanceLocalPrimaryKeyColumnName);
if (applicationDebugLevel > 2) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.setSelectedDatabaseRecordIsExposureDoseSequence.run(): localPrimaryKeyValue = "+localPrimaryKeyValue);
			if (localPrimaryKeyValue != null && localPrimaryKeyValue.length() > 0) {
				databaseInformationModel.updateSelectedRecord(InformationEntity.INSTANCE,localPrimaryKeyValue,instanceIsExposureDoseSequenceColumnName,"TRUE");
			}
		}
	}
	
	protected void setSelectedDatabaseRecordHasBeenSentToRegistry(String filename) throws DicomException {
if (applicationDebugLevel > 2) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.setSelectedDatabaseRecordHasBeenSentToRegistry.run(): filename: "+filename);
		ArrayList<Map<String,String>> records = databaseInformationModel.findAllAttributeValuesForAllRecordsForThisInformationEntityWithSpecifiedKeyValue(
			InformationEntity.INSTANCE,instanceLocalFileNameColumnName,filename);
if (applicationDebugLevel > 2) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.setSelectedDatabaseRecordHasBeenSentToRegistry.run(): records.size() = "+records.size());
		for (Map<String,String> record : records) {	// should only be one
			String localPrimaryKeyValue = record.get(instanceLocalPrimaryKeyColumnName);
if (applicationDebugLevel > 2) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.setSelectedDatabaseRecordHasBeenSentToRegistry.run(): localPrimaryKeyValue = "+localPrimaryKeyValue);
			if (localPrimaryKeyValue != null && localPrimaryKeyValue.length() > 0) {
				databaseInformationModel.updateSelectedRecord(InformationEntity.INSTANCE,localPrimaryKeyValue,instanceHasBeenSentToRegistryColumnName,"TRUE");
			}
		}
	}

	protected class ReceivedFileProcessor implements Runnable {
		String receivedFileName;
		AttributeList list;
		
		ReceivedFileProcessor(String receivedFileName) {
			this.receivedFileName = receivedFileName;
		}
		
		public void run() {
			try {
if (applicationDebugLevel > 1) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.ReceivedFileProcessor.run(): receivedFileName = "+receivedFileName);
				FileInputStream fis = new FileInputStream(receivedFileName);
				DicomInputStream i = new DicomInputStream(new BufferedInputStream(fis));
				AttributeList list = new AttributeList();
				list.read(i,TagFromName.PixelData);	// can stop at PixelData, since we may defer OCR until later anyway
				i.close();
				fis.close();
				
				boolean keepReceivedFile = false;
				// compare this logic with DoseUtility and LegacyRadiationDoseOCRDicomForwardingService ... should refactor :(
				String sopClassUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPClassUID);
				if (sopClassUID.equals(SOPClass.XRayRadiationDoseSRStorage)) {
if (applicationDebugLevel > 0) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.ReceivedFileProcessor.run(): is RDSR SOP Class");
					keepReceivedFile = true;			// save it to send to registry later
					setSelectedDatabaseRecordIsRadiationDoseSR(receivedFileName);
				}
				else if (list.isSRDocument() && SOPClass.isStructuredReport(sopClassUID)) {	// e.g., an old Enhanced SOP Class GE RDSR with the right template
if (applicationDebugLevel > 0) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.ReceivedFileProcessor.run(): is SR SOP Class");
					boolean srIsWanted = false;
					CodedSequenceItem documentTitle = CodedSequenceItem.getSingleCodedSequenceItemOrNull(list,TagFromName.ConceptNameCodeSequence);
					if (documentTitle != null) {
						String csd = documentTitle.getCodingSchemeDesignator();
						String cv = documentTitle.getCodeValue();
						if (csd != null && csd.equals("DCM") && cv != null && cv.equals("113701")) {	// X-Ray Radiation Dose Report
if (applicationDebugLevel > 0) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.ReceivedFileProcessor.run(): is SR SOP Class with X-Ray Radiation Dose Report document title");
							keepReceivedFile = true;			// save it to send to registry later
							databaseInformationModel.insertObject(list,receivedFileName,DatabaseInformationModel.FILE_COPIED);			// save it to send to registry later
							setSelectedDatabaseRecordIsRadiationDoseSR(receivedFileName);
						}
					}
				}
				else {
					if (OCR.isDoseScreenInstance(list)) {
if (applicationDebugLevel > 0) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.ReceivedFileProcessor.run(): isDoseScreenInstance - adding to database for deferred OCR");
						// do NOT process it yet ... need to wait until we have entire series for multi-page screens
						keepReceivedFile = true;			// save it to OCR and send to registry later
						databaseInformationModel.insertObject(list,receivedFileName,DatabaseInformationModel.FILE_COPIED);				// save it to send to registry later
						setSelectedDatabaseRecordIsRadiationDoseScreen(receivedFileName);
					}
					else if (ExposureDoseSequence.isPhilipsDoseScreenInstance(list)) {
if (applicationDebugLevel > 0) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.ReceivedFileProcessor.run(): isPhilipsDoseScreenInstance");
						if (retainSourceFilesUsedForSRGeneration) {
							keepReceivedFile = true;
							databaseInformationModel.insertObject(list,receivedFileName,DatabaseInformationModel.FILE_COPIED);
							setSelectedDatabaseRecordIsExposureDoseSequence(receivedFileName);
						}
						CTDose ctDose = ExposureDoseSequence.getCTDoseFromExposureDoseSequence(list,ocrDebugLevel,null/*eventDataFromImages*/,true/*buildSR*/);
						if (ctDose != null) {
							// created SR file needs to survive restart of application or reboot of machine
							AttributeList ctDoseList = ctDose.getAttributeList();
							File ctDoseSRFile = storedFilePathStrategy.makeReliableStoredFilePathWithFoldersCreated(savedImagesFolder,Attribute.getSingleStringValueOrDefault(ctDoseList,TagFromName.SOPInstanceUID,"1.1.1.1"));
							String ctDoseSRFileName = ctDoseSRFile.getCanonicalPath();
if (applicationDebugLevel > 0) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.ReceivedFileProcessor.run(): adding our own newly created SR file = "+ctDoseSRFileName);
							ctDose.write(ctDoseSRFileName,ourCalledAETitle,this.getClass().getCanonicalName());	// has side effect of updating list returned by ctDose.getAttributeList(); uncool :(
							databaseInformationModel.insertObject(ctDoseList,ctDoseSRFileName,DatabaseInformationModel.FILE_COPIED);	// save newly created SR to send to registry later
							setSelectedDatabaseRecordIsRadiationDoseSR(ctDoseSRFileName);
						}
					}
					else {
if (applicationDebugLevel > 0) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.ReceivedFileProcessor.run(): received file that we couldn't extract from "+receivedFileName);
						// do nothing ... will be deleted ... would want to keep if CT SOP Class and wanted to process acquired slices for irradiation events
					}
				}
				
				if (!keepReceivedFile) {
					databaseInformationModel.insertObject(list,"NULL",DatabaseInformationModel.FILE_REFERENCED);				// record that we received it so that we don't keep retrieving it
					// OK to throw exception at this point, which will be trapped anyway, since no further processing to be done
					if (!new File(receivedFileName).delete()) {
						throw new DicomException("Failed to delete received SR file that was not an RDSR "+receivedFileName);
					}
				}
			}
			catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
	}
	
	/**
	 *
	 */
	protected class OurReceivedObjectHandler extends ReceivedObjectHandler {
		/**
		 * @param	dicomFileName
		 * @param	transferSyntax
		 * @param	callingAETitle
		 * @exception	IOException
		 * @exception	DicomException
		 * @exception	DicomNetworkException
		 */
		public void sendReceivedObjectIndication(String dicomFileName,String transferSyntax,String callingAETitle)
				throws DicomNetworkException, DicomException, IOException {
			if (dicomFileName != null) {
if (applicationDebugLevel > 0) System.err.println("Received: "+dicomFileName+" from "+callingAETitle+" in "+transferSyntax);
				try {
					new Thread(new ReceivedFileProcessor(dicomFileName)).start();		// on separate thread, else will block and the C-STORE response will be delayed
				} catch (Exception e) {
					e.printStackTrace(System.err);
				}
			}

		}
	}
	
	protected boolean alreadyHaveIt(AttributeList uniqueKeys) throws DicomException {
		boolean haveIt = false;
		String uid = Attribute.getSingleStringValueOrNull(uniqueKeys,TagFromName.SOPInstanceUID);
		if (uid != null) {
			ArrayList<Map<String,String>> records = databaseInformationModel.findAllAttributeValuesForAllRecordsForThisInformationEntityWithSpecifiedUID(InformationEntity.INSTANCE,uid);
			haveIt = !records.isEmpty();
if (applicationDebugLevel > 1) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.alreadyHaveIt(): Do "+(haveIt ? "" : "not ")+"have INSTANCE "+uid);
		}
		else {
			uid = Attribute.getSingleStringValueOrNull(uniqueKeys,TagFromName.SeriesInstanceUID);
			if (uid != null) {
				ArrayList<Map<String,String>> records = databaseInformationModel.findAllAttributeValuesForAllRecordsForThisInformationEntityWithSpecifiedUID(InformationEntity.SERIES,uid);
				haveIt = !records.isEmpty();
if (applicationDebugLevel > 1) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.alreadyHaveIt(): Do "+(haveIt ? "" : "not ")+"have SERIES "+uid);
			}
			else {
				uid = Attribute.getSingleStringValueOrNull(uniqueKeys,TagFromName.StudyInstanceUID);
				if (uid != null) {
					ArrayList<Map<String,String>> records = databaseInformationModel.findAllAttributeValuesForAllRecordsForThisInformationEntityWithSpecifiedUID(InformationEntity.STUDY,uid);
					haveIt = !records.isEmpty();
if (applicationDebugLevel > 1) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.alreadyHaveIt(): Do "+(haveIt ? "" : "not ")+"have STUDY "+uid);
				}
			}
		}
		return haveIt;
	}

	protected class WatchRemoteAEsForNewDoseInformation implements Runnable {
		public void run() {
			long millisecondsBackwardsFromTodayToQuery = daysBackwardsFromTodayToQuery * millisecondsPerDay;
			boolean interrupted = false;
			while (!interrupted) {
if (applicationDebugLevel > 1) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.WatchRemoteAEsForNewDoseInformation.run(): Starting or waking up ... ");
				try {
					long currentTime = System.currentTimeMillis();
					long earliestTimeToGoBack = currentTime - millisecondsBackwardsFromTodayToQuery;
					for (long time = currentTime; time >= earliestTimeToGoBack; time -= millisecondsPerDay) {
						String studyDateToQueryFor = new SimpleDateFormat("yyyyMMdd").format(new Date(time));
if (applicationDebugLevel > 0) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.WatchRemoteAEsForNewDoseInformation.run(): Query for all studies dated "+studyDateToQueryFor+" on "+remoteAEsForQuery);
						for (String remoteAEForQuery : remoteAEsForQuery) {
							QueryInformationModel remoteQueryInformationModel = null;
							// copied from DoseUtility.setCurrentRemoteQueryInformationModel() ... should refactor :(
							{
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
											remoteQueryInformationModel = new StudyRootQueryInformationModel(queryHost,queryPort,queryCalledAETitle,queryCallingAETitle,queryDebugLevel);
										}
										else {
											throw new Exception("For remote query AE <"+remoteAEForQuery+">, query model "+queryModel+" not supported");
										}

if (applicationDebugLevel > 1) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.WatchRemoteAEsForNewDoseInformation.run(): Performing query on "+remoteQueryInformationModel+" ...");

									}
									catch (Exception e) {		// if an AE's property has no value, or model not supported
										e.printStackTrace(System.err);
									}
								}
							}
						
							if (remoteQueryInformationModel != null) {
								try {
									AttributeList filter = new AttributeList();
									{ AttributeTag t = TagFromName.ModalitiesInStudy; Attribute a = new CodeStringAttribute(t); a.addValue("CT"); a.addValue("SR"); filter.put(t,a); }
									{ AttributeTag t = TagFromName.StudyDate; Attribute a = new DateAttribute(t); a.addValue(studyDateToQueryFor); filter.put(t,a); }
							
									{ AttributeTag t = TagFromName.SeriesNumber; Attribute a = new IntegerStringAttribute(t); filter.put(t,a); }
									{ AttributeTag t = TagFromName.Modality; Attribute a = new CodeStringAttribute(t); filter.put(t,a); }

									{ AttributeTag t = TagFromName.ImageType; Attribute a = new CodeStringAttribute(t); filter.put(t,a); }

									{ AttributeTag t = TagFromName.StudyInstanceUID; Attribute a = new UniqueIdentifierAttribute(t); filter.put(t,a); }
									{ AttributeTag t = TagFromName.SeriesInstanceUID; Attribute a = new UniqueIdentifierAttribute(t); filter.put(t,a); }
									{ AttributeTag t = TagFromName.SOPInstanceUID; Attribute a = new UniqueIdentifierAttribute(t); filter.put(t,a); }
									{ AttributeTag t = TagFromName.SOPClassUID; Attribute a = new UniqueIdentifierAttribute(t); filter.put(t,a); }
							
									QueryTreeModel treeModel = remoteQueryInformationModel.performHierarchicalQuery(filter);
if (applicationDebugLevel > 2) System.err.print("DoseReporterWithLegacyOCRAndAutoSendToRegistry.WatchRemoteAEsForNewDoseInformation.run(): Query result=\n"+treeModel);

									List<QueryTreeRecord> records = DoseUtility.findDoseSeriesRecordsInQueryTree((QueryTreeRecord)(treeModel.getRoot()),new ArrayList<QueryTreeRecord>());
									{
										// similar to DoseUtility.RetrieveActionListener - should refactor :(
										if (records != null) {
											for (QueryTreeRecord r : records) {
												AttributeList uniqueKeys = r.getUniqueKeys();
												Attribute uniqueKey      = r.getUniqueKey();
												AttributeList queryIdentifier = r.getAllAttributesReturnedInIdentifier();
if (applicationDebugLevel > 2) System.err.print  ("DoseReporterWithLegacyOCRAndAutoSendToRegistry.WatchRemoteAEsForNewDoseInformation.run(): uniqueKeys:\n"+uniqueKeys);
if (applicationDebugLevel > 2) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.WatchRemoteAEsForNewDoseInformation.run(): uniqueKey: "+uniqueKey);
if (applicationDebugLevel > 2) System.err.print  ("DoseReporterWithLegacyOCRAndAutoSendToRegistry.WatchRemoteAEsForNewDoseInformation.run(): queryIdentifier:\n"+queryIdentifier);
												String retrieveAE        = DoseUtility.getQueryRetrieveAEFromIdentifier(queryIdentifier,remoteQueryInformationModel);
												String localName         = networkApplicationInformation.getLocalNameFromApplicationEntityTitle(retrieveAE);
												String level             = DoseUtility.getQueryRetrieveLevel(queryIdentifier,uniqueKey);
						
												if (uniqueKeys != null) {
													// in addition to alreadyHaveIt, should we also consider database study already processed flag ?
													if (!alreadyHaveIt(uniqueKeys)) {
														AttributeList retrieveIdentifier = new AttributeList();
														retrieveIdentifier.putAll(uniqueKeys);
														{ AttributeTag t = TagFromName.QueryRetrieveLevel; Attribute a = new CodeStringAttribute(t); a.addValue(level); retrieveIdentifier.put(t,a); }
if (applicationDebugLevel > 1) System.err.print("DoseReporterWithLegacyOCRAndAutoSendToRegistry.WatchRemoteAEsForNewDoseInformation.run(): retrieveIdentifier:\n"+retrieveIdentifier);
														remoteQueryInformationModel.performHierarchicalMoveFrom(retrieveIdentifier,retrieveAE);
													}
													else {
if (applicationDebugLevel > 1) System.err.print("DoseReporterWithLegacyOCRAndAutoSendToRegistry.WatchRemoteAEsForNewDoseInformation.run(): already have it so not retrieving again:\n"+uniqueKeys);
													}
												}
												// else do nothing, since no unique key to specify what to retrieve
											}
										}
									}
								}
								catch (Exception e) {
									e.printStackTrace(System.err);
								}
							}
						}
					}
if (applicationDebugLevel > 1) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.WatchRemoteAEsForNewDoseInformation.run(): sleeping for "+sleepTimeBetweenPassesToQueryRemoteAEs+" seconds");
					Thread.currentThread().sleep(sleepTimeBetweenPassesToQueryRemoteAEs*1000);	// configured value is in seconds, sleep() parameter is in milliseconds
				}
				catch (InterruptedException e) {
if (applicationDebugLevel > 1) System.err.println("DoseReporterWithLegacyOCRAndAutoSendToRegistry.WatchRemoteAEsForNewDoseInformation.run(): interrupted: "+e);
					interrupted = true;		// currently this shouldn't happen; i.e., no other thread will interrupt this one whilst sleeping (?)
				}
			}
		}
	}
	
	protected class OurPatientStudySeriesInstanceModel extends MinimalPatientStudySeriesInstanceModel {
		OurPatientStudySeriesInstanceModel(String databaseFileName,String databaseServerName) throws DicomException {
			super(databaseFileName,databaseServerName);
		}

		protected void extendCreateStatementStringWithUserColumns(StringBuffer b,InformationEntity ie) {
			if (ie == InformationEntity.STUDY) {
				b.append(", "); b.append(studyHasBeenProcessedColumnName); b.append(" "); b.append("BOOLEAN");
			}
			else if (ie == InformationEntity.INSTANCE) {
				b.append(", "); b.append(instanceHasBeenSentToRegistryColumnName); b.append(" "); b.append("BOOLEAN");
				b.append(", "); b.append(instanceIsRadiationDoseSRColumnName); b.append(" "); b.append("BOOLEAN");
				b.append(", "); b.append(instanceIsRadiationDoseScreenColumnName); b.append(" "); b.append("BOOLEAN");
				b.append(", "); b.append(instanceIsExposureDoseSequenceColumnName); b.append(" "); b.append("BOOLEAN");
			}
		}

	}
	
	/**
	 * <p>Wait for incoming dose screen images and SRs and send to registry.</p>
	 *
	 * @param	propertiesFileName
	 */
	public DoseReporterWithLegacyOCRAndAutoSendToRegistry(String propertiesFileName) throws DicomException, DicomNetworkException, IOException, InterruptedException, FTPException {
		loadProperties(propertiesFileName);		// do NOT trap exception; we must have properties

		applicationDebugLevel = Integer.valueOf(properties.getProperty(propertyName_ApplicationDebugLevel,"0")).intValue();
		ocrDebugLevel = Integer.valueOf(properties.getProperty(propertyName_OCRDebugLevel,"0")).intValue();

		retainDeidentifiedFiles              = Boolean.parseBoolean(properties.getProperty(propertyName_RetainDeidentifiedFiles,              defaultRetainDeidentifiedFiles));
		retainGeneratedRDSRFiles             = Boolean.parseBoolean(properties.getProperty(propertyName_RetainGeneratedRDSRFiles,             defaultRetainGeneratedRDSRFiles));
		retainSourceFilesUsedForSRGeneration = Boolean.parseBoolean(properties.getProperty(propertyName_RetainSourceFilesUsedForSRGeneration, defaultRetainSourceFilesUsedForSRGeneration));
		
		sleepTimeBetweenPassesToProcessReceivedFiles                 = Integer.valueOf(properties.getProperty(propertyName_SleepTimeBetweenPassesToProcessReceivedFiles,defaultSleepTimeBetweenPassesToProcessReceivedFiles)).intValue();
		intervalAfterLastInstanceReceivedToWaitBeforeProcessingStudy = Integer.valueOf(properties.getProperty(propertyName_IntervalAfterLastInstanceReceivedToWaitBeforeProcessingStudy,defaultIntervalAfterLastInstanceReceivedToWaitBeforeProcessingStudy)).intValue();
		sleepTimeBetweenPassesToQueryRemoteAEs                       = Integer.valueOf(properties.getProperty(propertyName_SleepTimeBetweenPassesToQueryRemoteAEs,defaultSleepTimeBetweenPassesToQueryRemoteAEs)).intValue();
		daysBackwardsFromTodayToQuery                                = Integer.valueOf(properties.getProperty(propertyName_DaysBackwardsFromTodayToQuery,defaultDaysBackwardsFromTodayToQuery)).intValue();

		DatabaseApplicationProperties databaseApplicationProperties = new DatabaseApplicationProperties(properties);
		savedImagesFolder = databaseApplicationProperties.getSavedImagesFolderCreatingItIfNecessary();
		databaseInformationModel = new OurPatientStudySeriesInstanceModel(databaseApplicationProperties.getDatabaseFileName(),databaseApplicationProperties.getDatabaseServerName());
		
		studyInstanceUIDColumnName               = databaseInformationModel.getDatabaseColumnNameFromDicomTag(TagFromName.StudyInstanceUID);
		sopClassUIDColumnName                    = databaseInformationModel.getDatabaseColumnNameFromDicomTag(TagFromName.SOPClassUID);
		manufacturerColumnName                   = databaseInformationModel.getDatabaseColumnNameFromDicomTag(TagFromName.Manufacturer);
		imageTypeColumnName                      = databaseInformationModel.getDatabaseColumnNameFromDicomTag(TagFromName.ImageType);
		instanceCreatorUIDColumnName             = databaseInformationModel.getDatabaseColumnNameFromDicomTag(TagFromName.InstanceCreatorUID);
		sourceApplicationEntityTitleColumnName   = databaseInformationModel.getDatabaseColumnNameFromDicomTag(TagFromName.SourceApplicationEntityTitle);
		instanceLocalParentReferenceColumnName   = databaseInformationModel.getLocalParentReferenceColumnName(InformationEntity.INSTANCE);
		instanceLocalFileNameColumnName          = databaseInformationModel.getLocalFileNameColumnName(InformationEntity.INSTANCE);
		instanceLocalFileReferenceTypeColumnName = databaseInformationModel.getLocalFileReferenceTypeColumnName(InformationEntity.INSTANCE);
		instanceLocalPrimaryKeyColumnName        = databaseInformationModel.getLocalPrimaryKeyColumnName(InformationEntity.INSTANCE);
		
		FTPApplicationProperties ftpApplicationProperties = new FTPApplicationProperties(properties);
		String registryName = properties.getProperty(propertyName_SelectedDoseRegistry);
		remoteHost = ftpApplicationProperties.getFTPRemoteHostInformation().getRemoteHost(registryName);
		ftpClientDebugLevel = ftpApplicationProperties.getClientDebugLevel();

		networkApplicationProperties = new NetworkApplicationProperties(properties);
		networkApplicationInformation = new NetworkApplicationInformationFederated();
		networkApplicationInformation.startupAllKnownSourcesAndRegister(networkApplicationProperties);
		ourCalledAETitle = networkApplicationProperties.getCalledAETitle();

		{
			remoteAEsForQuery = new ArrayList();
			String remoteAEs = properties.getProperty(propertyName_RemoteAEsForQuery);
			if (remoteAEs != null && remoteAEs.length() > 0) {
				StringTokenizer st = new StringTokenizer(remoteAEs,propertyDelimitersForTokenizer_RemoteAEsForQuery);
				while (st.hasMoreTokens()) {
					String localName=st.nextToken();
					remoteAEsForQuery.add(localName);
				}
			}
		}

		// Start up DICOM association listener in background for receiving images and responding to echoes and queries and retrieves ...
if (applicationDebugLevel > 1) System.err.println("Starting up DICOM association listener ...");
		{
			int port = networkApplicationProperties.getListeningPort();
			int storageSCPDebugLevel = networkApplicationProperties.getStorageSCPDebugLevel();
			int queryDebugLevel = networkApplicationProperties.getQueryDebugLevel();
			new Thread(new StorageSOPClassSCPDispatcher(port,ourCalledAETitle,savedImagesFolder,storedFilePathStrategy,new OurReceivedObjectHandler(),
					this.databaseInformationModel.getQueryResponseGeneratorFactory(queryDebugLevel),
					this.databaseInformationModel.getRetrieveResponseGeneratorFactory(queryDebugLevel),
					networkApplicationInformation,
					false/*secureTransport*/,
					storageSCPDebugLevel)).start();
		}
		
		new Thread(new WatchDatabaseAndSendToRegistry()).start();
		new Thread(new WatchRemoteAEsForNewDoseInformation()).start();
	}

	/**
	 * <p>Wait for incoming dose screen images and SRs and send to registry.</p>
	 *
	 * @param	arg		none
	 */
	public static void main(String arg[]) {
		try {
			String propertiesFileName = arg.length > 0 ? arg[0] : FileUtilities.makePathToFileInUsersHomeDirectory(defaultPropertiesFileName);
			new DoseReporterWithLegacyOCRAndAutoSendToRegistry(propertiesFileName);
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
			System.exit(0);
		}
	}
}


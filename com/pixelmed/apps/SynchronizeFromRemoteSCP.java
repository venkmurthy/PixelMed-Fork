/* Copyright (c) 2001-2011, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.apps;

import com.pixelmed.database.DatabaseInformationModel;
import com.pixelmed.database.PatientStudySeriesConcatenationInstanceModel;

import com.pixelmed.query.QueryInformationModel;
import com.pixelmed.query.QueryTreeModel;
import com.pixelmed.query.QueryTreeRecord;
import com.pixelmed.query.StudyRootQueryInformationModel;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.AttributeTag;
import com.pixelmed.dicom.CodeStringAttribute;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.DicomInputStream;
import com.pixelmed.dicom.InformationEntity;
import com.pixelmed.dicom.LongStringAttribute;
import com.pixelmed.dicom.PersonNameAttribute;
import com.pixelmed.dicom.SOPClass;
import com.pixelmed.dicom.SpecificCharacterSet;
import com.pixelmed.dicom.StoredFilePathStrategy;
import com.pixelmed.dicom.TagFromName;
import com.pixelmed.dicom.UniqueIdentifierAttribute;

import com.pixelmed.network.DicomNetworkException;
import com.pixelmed.network.GetSOPClassSCU;
import com.pixelmed.network.IdentifierHandler;
import com.pixelmed.network.MoveSOPClassSCU;
import com.pixelmed.network.NetworkConfigurationFromMulticastDNS;
import com.pixelmed.network.ReceivedObjectHandler;
import com.pixelmed.network.StorageSOPClassSCPDispatcher;

import java.text.DecimalFormat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * <p>A class for synchronizing the contents of a local database of DICOM objects with a remote SCP.</p>
 *
 * <p>The class has no public methods other than the constructor and a main method that is useful as a utility. The
 * constructor establishes an association, sends hierarchical C-FIND requests at the STUDY, SERIES and IMAGE
 * levels to determine what is available on the remote AE, then attempts to retrieve anything not present
 * locally at the highest level possible. E.g., if a study is not present, a retrieve of the entire study
 * is requested.
 *
 * <p>The main method is also useful in its own right as a command-line utility. For example:</p>
 * <pre>
java -cp ./pixelmed.jar:./lib/additional/hsqldb.jar:./lib/additional/commons-codec-1.3.jar:./lib/additional/jmdns.jar \
	com.pixelmed.apps.SynchronizeFromRemoteSCP \
	/tmp/dicomsync/database /tmp/dicomsync \
	graytoo 4006 GRAYTOO_DIV_4006 \
	11112 US \
	MOVE ALL 1 0
 * </pre>
 *
 * @author	dclunie
 */
public class SynchronizeFromRemoteSCP {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/apps/SynchronizeFromRemoteSCP.java,v 1.11 2012/02/01 23:02:09 dclunie Exp $";
	
	private static int sleepTimeBetweenCheckingForNothingExpectedBeforeExiting = 10000;	// ms
	private static int sleepTimeAfterRegisteringWithBonjour = 10000;	// ms
	private static int inactivityTimeOut = 600000;	// ms
	
	private DatabaseInformationModel databaseInformationModel;
	private File savedInstancesFolder;
	private String remoteHost;
	private int remotePort;
	private String remoteAE;
	private int localPort;
	private String localAE;
	private int verbosityLevel;
	private int debugLevel;
	
	private QueryInformationModel queryInformationModel;
	
	private ReceivedObjectHandler receivedObjectHandler;
	private IdentifierHandler identifierHandler;
	private boolean useGet;
	private boolean queryAll;
	
	private Set setofInstancesExpected;
	private Set setofClassesExpected;
	private int numberOfSOPInstancesReceived;
	private int numberOfValidSOPInstancesReceived;
	private int numberOfUnrequestedSOPInstancesReceived;
	private long inactivityTime;
	
	private long totalDurationOfRetrieval;
	private long totalBytesSaved;
	
	private DecimalFormat commaFormatter = new DecimalFormat("#,###");

	/**
	 * @param	node
	 * @param	parentUniqueKeys
	 * @param	retrieveDone
	 */
	private void walkTreeDownToInstanceLevelAndRetrieve(QueryTreeRecord node,AttributeList parentUniqueKeys,boolean retrieveDone) throws DicomException, DicomNetworkException, IOException {
		InformationEntity ie = node.getInformationEntity();
		Attribute uniqueKey = node.getUniqueKey();
		AttributeList uniqueKeys = null;
		AttributeList retrieveIdentifier = null;
		ArrayList recordsForThisUID = null;

//System.err.println("SynchronizeFromRemoteSCP.walkTreeDownToInstanceLevelAndRetrieve(): node = "+node);
//System.err.print("SynchronizeFromRemoteSCP.walkTreeDownToInstanceLevelAndRetrieve(): getAllAttributesReturnedInIdentifier() =\n"+node.getAllAttributesReturnedInIdentifier());

		if (ie != null && uniqueKey != null) {
			uniqueKeys = new AttributeList();
			if (parentUniqueKeys != null) {
				uniqueKeys.putAll(parentUniqueKeys);
			}
			AttributeTag uniqueKeyTagFromThisLevel = queryInformationModel.getUniqueKeyForInformationEntity(ie);
			String uid = uniqueKey.getSingleStringValueOrNull();	// always a UID, since StudyRoot
			if (uid == null) {
System.err.println("Could not get UID to use");
			}
			else {
if (verbosityLevel > 1) System.err.println("Searching for existing records for "+ie+" "+uid);
				recordsForThisUID = databaseInformationModel.findAllAttributeValuesForAllRecordsForThisInformationEntityWithSpecifiedUID(ie,uid);
				{ Attribute a = new UniqueIdentifierAttribute(uniqueKeyTagFromThisLevel); a.addValue(uid); uniqueKeys.put(a); }
				if (!retrieveDone && recordsForThisUID.size() == 0) {
if (verbosityLevel > 1) System.err.println("No existing records for "+ie+" "+uid);
					if (verbosityLevel > 0) {
						System.err.println("Performing retrieve for "+node+" ("+Attribute.getSingleStringValueOrEmptyString( node.getUniqueKey())+")");
					}
					SpecificCharacterSet specificCharacterSet = new SpecificCharacterSet((String[])null);
					retrieveIdentifier = new AttributeList();
					retrieveIdentifier.putAll(uniqueKeys);
					
					String retrieveLevelName = queryInformationModel.getQueryLevelName(ie);
					{ Attribute a = new CodeStringAttribute(TagFromName.QueryRetrieveLevel); a.addValue(retrieveLevelName); retrieveIdentifier.put(a); }
if (verbosityLevel > 1) System.err.println("Retrieve identifier "+retrieveIdentifier);
					// defer the actual move until the children have been walked and the SOPInstanceUIDs expected added to setofInstancesExpected
					retrieveDone = true;	// but make sure children don't perform any moves
				}
			}
		}
		{
			int n = node.getChildCount();
			if (n > 0) {
				for (int i=0; i<n; ++i) {
					walkTreeDownToInstanceLevelAndRetrieve((QueryTreeRecord)node.getChildAt(i),uniqueKeys,retrieveDone);
				}
			}
			else {
				if (ie != null && ie.equals(InformationEntity.INSTANCE) && recordsForThisUID.size() == 0) {
					// don't already have the instance so we expect it
					AttributeList list = node.getAllAttributesReturnedInIdentifier();
if (verbosityLevel > 2) System.err.print("Adding to set to retrieve: getAllAttributesReturnedInIdentifier() =\n"+list);
					String sopInstanceUID = Attribute.getSingleStringValueOrNull(list,TagFromName.SOPInstanceUID);
if (verbosityLevel > 2) System.err.println("Adding to set to retrieve: SOPInstanceUID = "+sopInstanceUID);
						setofInstancesExpected.add(sopInstanceUID);
					if (useGet) {
						String sopClassUID = Attribute.getSingleStringValueOrNull(list,TagFromName.SOPClassUID);
						if (sopClassUID == null || sopClassUID.length() == 0) {
if (verbosityLevel > 0) System.err.println("Adding to set to retrieve: SOPClassUID is missing or empty in C-FIND response ... trying to guess an appropriate one to use for C-GET");
							// bummer ... SCP didn't return SOPClassUID, which is allowed, since optional for C-FIND SCP
							// try to guess it from parent STUDY node's SOPClassesInStudy, or if absent, then parent SERIES Modality ...
							QueryTreeRecord seriesNode = (QueryTreeRecord)(node.getParent());
							QueryTreeRecord studyNode = (QueryTreeRecord)(seriesNode.getParent());
							AttributeList studyList = studyNode.getAllAttributesReturnedInIdentifier();
							Attribute aSOPClassesInStudy = studyList.get(TagFromName.SOPClassesInStudy);
							if (aSOPClassesInStudy != null && aSOPClassesInStudy.getVM() > 0) {
								String[] sopClassesInStudy = aSOPClassesInStudy.getStringValues();
								for (String sopClassInStudy : sopClassesInStudy) {
if (verbosityLevel > 1) System.err.println("Adding to set to retrieve: SOPClassUID derived from SOPClassesInStudy = "+sopClassInStudy);
									setofClassesExpected.add(sopClassInStudy);
								}
							}
							else {
								AttributeList seriesList = seriesNode.getAllAttributesReturnedInIdentifier();
								String modality = Attribute.getSingleStringValueOrEmptyString(seriesList,TagFromName.Modality);
if (verbosityLevel > 1) System.err.println("Adding to set to retrieve: No SOPClassesInStudy either, trying Modality "+modality);
								String[] sopClassUIDsForModality = SOPClass.getPlausibleStandardSOPClassUIDsForModality(modality);
								if (sopClassUIDsForModality.length > 0) {
									for (String sopClassUIDForModality: sopClassUIDsForModality) {
if (verbosityLevel > 1) System.err.println("Adding to set to retrieve: SOPClassUID derived from Modality "+modality+" = "+sopClassUIDForModality);
										setofClassesExpected.add(sopClassUIDForModality);
									}
								}
								else {
if (verbosityLevel > 1) System.err.println("Adding to set to retrieve: all known storage SOP Classes");
									setofClassesExpected = SOPClass.getSetOfStorageSOPClasses();
								}
							}
						}
						else {
if (verbosityLevel > 1) System.err.println("Adding to set to retrieve: SOPClassUID = "+sopClassUID);
							setofClassesExpected.add(sopClassUID);
						}
					}
				}
			}
		}
		if (retrieveIdentifier != null) {
			try {
				bytesSaved=0;
				long startOfRetrieval=System.currentTimeMillis();
				if (useGet) {
if (verbosityLevel > 0) System.err.println("Retrieving with C-GET");
					GetSOPClassSCU getSOPClassSCU = new GetSOPClassSCU(remoteHost,remotePort,remoteAE,localAE,
						SOPClass.StudyRootQueryRetrieveInformationModelGet,
						retrieveIdentifier,identifierHandler,savedInstancesFolder,StoredFilePathStrategy.BYSOPINSTANCEUIDHASHSUBFOLDERS,receivedObjectHandler,
						setofClassesExpected,false/*theirChoice*/,true/*ourChoice*/,true/*asEncoded*/,debugLevel);
					setofClassesExpected.clear();	// flush list and start afresh for next C-GET
				}
				else {
if (verbosityLevel > 0) System.err.println("Retrieving with C-MOVE");
					bytesSaved=0;
					//queryInformationModel.performHierarchicalMoveFrom(retrieveIdentifier,remoteAE);
					MoveSOPClassSCU moveSOPClassSCU = new MoveSOPClassSCU(remoteHost,remotePort,remoteAE,localAE,localAE,SOPClass.StudyRootQueryRetrieveInformationModelMove,retrieveIdentifier,debugLevel);
					int moveStatus = moveSOPClassSCU.getStatus();
					if (moveStatus != 0x0000) {
						System.err.println("SynchronizeFromRemoteSCP: unsuccessful move status = "+("0x"+Integer.toHexString(moveStatus)));
					}
				}
if (verbosityLevel > 0) {
	long durationOfRetrieval = System.currentTimeMillis() - startOfRetrieval;
	double rate = durationOfRetrieval == 0 ? 0l : ((double)bytesSaved)/1000000/(((double)durationOfRetrieval)/1000);
	System.err.println("Saved "+commaFormatter.format(bytesSaved)+" bytes in "+commaFormatter.format(durationOfRetrieval)+" ms, "+rate+" MB/s");
	totalDurationOfRetrieval += durationOfRetrieval;
	totalBytesSaved += bytesSaved;
}
			}
			catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
	}

	/**
	 */
	private void performQueryAndWalkTreeDownToInstanceLevelAndRetrieve() throws DicomException, DicomNetworkException, IOException {
		SpecificCharacterSet specificCharacterSet = new SpecificCharacterSet((String[])null);
		AttributeList identifier = new AttributeList();
		identifier.putNewAttribute(TagFromName.StudyInstanceUID);
		identifier.putNewAttribute(TagFromName.SeriesInstanceUID);
		identifier.putNewAttribute(TagFromName.SOPInstanceUID);
		if (useGet) {
			identifier.putNewAttribute(TagFromName.SOPClassesInStudy);
			identifier.putNewAttribute(TagFromName.SOPClassUID);
		}
	
		if (verbosityLevel > 0) {
			//identifier.putNewAttribute(TagFromName.PatientName,specificCharacterSet);
			identifier.putNewAttribute(TagFromName.PatientID,specificCharacterSet);

			identifier.putNewAttribute(TagFromName.StudyDate);
			identifier.putNewAttribute(TagFromName.StudyID,specificCharacterSet);
			identifier.putNewAttribute(TagFromName.StudyDescription,specificCharacterSet);
		
			identifier.putNewAttribute(TagFromName.SeriesNumber);
			identifier.putNewAttribute(TagFromName.SeriesDescription,specificCharacterSet);
			identifier.putNewAttribute(TagFromName.Modality);

			identifier.putNewAttribute(TagFromName.InstanceNumber);
		}
		
		String patientNamesAll[] = { "" };
		String patientNamesSelective[] = { "A*", "B*", "C*", "D*", "E*", "F*", "G*", "H*", "I*", "J*", "K*", "L*", "M*", "N*", "O*", "P*", "Q*", "R*", "S*", "T*", "U*", "V*", "W*", "X*", "Y*", "Z*" };
		String patientNames[] = queryAll ? patientNamesAll : patientNamesSelective;
		for (int i=0; i<patientNames.length; ++i) {
			{ Attribute a = new PersonNameAttribute(TagFromName.PatientName,specificCharacterSet); a.addValue(patientNames[i]); identifier.put(a); }
			QueryTreeModel tree = queryInformationModel.performHierarchicalQuery(identifier);
			walkTreeDownToInstanceLevelAndRetrieve((QueryTreeRecord)(tree.getRoot()),null,false);
		}
	}
	
	long bytesSaved;

	/**
	 */
	private class OurReceivedObjectHandler extends ReceivedObjectHandler {		
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
				inactivityTime = 0;
				if (verbosityLevel > 0) {
					System.err.println("Received: "+dicomFileName+" from "+callingAETitle+" in "+transferSyntax);
					bytesSaved += new File(dicomFileName).length();
				}
				++numberOfSOPInstancesReceived;
				try {
					// no need for case insensitive check here ... was locally created
					FileInputStream fis = new FileInputStream(dicomFileName);
					DicomInputStream i = new DicomInputStream(new BufferedInputStream(fis));
					AttributeList list = new AttributeList();
					list.read(i,TagFromName.PixelData);
					i.close();
					fis.close();
					String sopInstanceUID = Attribute.getSingleStringValueOrNull(list,TagFromName.SOPInstanceUID);
if (verbosityLevel > 2) System.err.println("Received: "+dicomFileName+" with SOPInstanceUID "+sopInstanceUID);
					if (sopInstanceUID != null) {
						++numberOfValidSOPInstancesReceived;
						if (setofInstancesExpected.contains(sopInstanceUID)) {
							setofInstancesExpected.remove(sopInstanceUID);
							databaseInformationModel.insertObject(list,dicomFileName,DatabaseInformationModel.FILE_COPIED);
						}
						else {
							++numberOfUnrequestedSOPInstancesReceived;
							databaseInformationModel.insertObject(list,dicomFileName,DatabaseInformationModel.FILE_COPIED);
							throw new DicomException("Unrequested SOPInstanceUID "+sopInstanceUID+" in received object ... stored it anyway");
						}
					}
					else {
						// should probably delete it, but "bad" file may be useful :(
						throw new DicomException("Missing SOPInstanceUID in received object ... not inserting file "+dicomFileName+" in database");
					}
				} catch (Exception e) {
					e.printStackTrace(System.err);
				}
			}

		}
	}
	
	/**
	 * <p>Synchronize the contents of a local database of DICOM objects with a remote SCP.</p>
	 *
	 * <p>Queries the remote SCP for everything it has and retrieves all instances not already present in the specified local database.</p>
	 * @param	databaseInformationModel	the local database (will be created if does not already exist)
	 * @param	savedInstancesFolder		where to save retrieved instances (must already exist)
	 * @param	remoteHost
	 * @param	remotePort
	 * @param	remoteAE
	 * @param	localPort					local port for DICOM listener ... must already be known to remote AE unless C-GET
	 * @param	localAE						local AET for DICOM listener ... must already be known to remote AE unless C-GET
	 * @param	useGet						if true, use C-GET rather than C-MOVE
	 * @param	queryAll					if true query for all patient names at once, rather than selectively by first letter
	 * @param	verbosityLevel
	 * @param	debugLevel
	 */
	public SynchronizeFromRemoteSCP(DatabaseInformationModel databaseInformationModel,File savedInstancesFolder,
				String remoteHost,int remotePort,String remoteAE,int localPort,String localAE,boolean useGet,boolean queryAll,int verbosityLevel,int debugLevel)
			throws DicomException, DicomNetworkException, IOException, InterruptedException {
		this.databaseInformationModel = databaseInformationModel;
		this.savedInstancesFolder = savedInstancesFolder;
		this.remoteHost = remoteHost;
		this.remotePort = remotePort;
		this.remoteAE = remoteAE;
		this.localPort = localPort;
		this.localAE = localAE;
		this.useGet = useGet;
		this.queryAll = queryAll;
		this.verbosityLevel = verbosityLevel;
		this.debugLevel = debugLevel;
		
		if (!savedInstancesFolder.exists() || !savedInstancesFolder.isDirectory()) {
			throw new DicomException("Folder in which to save received instances does not exist or is not a directory - "+savedInstancesFolder);
		}
		
		receivedObjectHandler = new OurReceivedObjectHandler();
		identifierHandler = new IdentifierHandler();
		
		if (useGet) {
			setofClassesExpected = new HashSet();
		}
		else {
			new Thread(new StorageSOPClassSCPDispatcher(localPort,localAE,savedInstancesFolder,StoredFilePathStrategy.BYSOPINSTANCEUIDHASHSUBFOLDERS,receivedObjectHandler,debugLevel)).start();
		}
		setofInstancesExpected = new HashSet();
		numberOfSOPInstancesReceived = 0;
		numberOfValidSOPInstancesReceived = 0;
		numberOfUnrequestedSOPInstancesReceived = 0;
		
		queryInformationModel = new StudyRootQueryInformationModel(remoteHost,remotePort,remoteAE,localAE,debugLevel);
		performQueryAndWalkTreeDownToInstanceLevelAndRetrieve();
		
		inactivityTime = 0;
		while (!setofInstancesExpected.isEmpty() && inactivityTime > inactivityTimeOut) {
System.err.println("Sleeping since "+setofInstancesExpected.size()+" remaining");
			Thread.currentThread().sleep(sleepTimeBetweenCheckingForNothingExpectedBeforeExiting);
			inactivityTime+=sleepTimeBetweenCheckingForNothingExpectedBeforeExiting;
		}
System.err.println("Finished with "+numberOfSOPInstancesReceived+" instances received, of which "+numberOfValidSOPInstancesReceived+" were valid, and "+numberOfUnrequestedSOPInstancesReceived+" were unrequested; requested but never received were "+setofInstancesExpected.size()+" instances");
if (verbosityLevel > 0) {
	double rate = totalDurationOfRetrieval == 0 ? 0l : ((double)totalBytesSaved)/1000000/(((double)totalDurationOfRetrieval)/1000);
	System.err.println("Total saved "+commaFormatter.format(totalBytesSaved)+" bytes in "+commaFormatter.format(totalDurationOfRetrieval)+" ms, "+rate+" MB/s");
}
	}

	/**
	 * <p>Synchronize the contents of a local database of DICOM objects with a remote SCP.</p>
	 *
	 * <p>Queries the remote SCP for everything it has and retrieves all instances not already present in the specified local database.</p>
	 *
	 * <p>Will register the supplied local AE and port with Bonjour if supported (this is specific to the main() method; the constructor of the class itself does not do this).</p>
	 *
	 * @param	arg		array of 7, 8, 9 or 10 strings - the fully qualified path of the database file prefix, the fully qualified path of the saved incoming files folder,
	 *					the remote hostname, remote port, remote AE Title, our port (ignored if GET), our AE Title,
	 *					optionally GET or MOVE (defaults to MOVE),
	 *					optionally query by ALL or SELECTIVE patient name (defaults to ALL),
	 *					optionally a verbosity level, optionally an integer debug level
	 */
	public static void main(String arg[]) {
		try {
			if (arg.length >= 7 && arg.length <= 11) {
				String databaseFileName = arg[0];
				String savedInstancesFolderName = arg[1];
				String remoteHost = arg[2];
				int remotePort = Integer.parseInt(arg[3]);
				String remoteAE = arg[4];
				int localPort = Integer.parseInt(arg[5]);
				String localAE = arg[6];
				boolean useGet = arg.length > 7 ? (arg[7].trim().toUpperCase(java.util.Locale.US).equals("GET") ? true : false) : false;
				boolean queryAll = arg.length > 8 ? (arg[8].trim().toUpperCase(java.util.Locale.US).equals("ALL") ? true : false) : true;
				int verbosityLevel = arg.length > 9 ? Integer.parseInt(arg[9]) : 0;
				int debugLevel = arg.length > 10 ? Integer.parseInt(arg[10]) : 0;
				File savedInstancesFolder = new File(savedInstancesFolderName);
		
				DatabaseInformationModel databaseInformationModel = new PatientStudySeriesConcatenationInstanceModel(databaseFileName);
				
				// attempt to register ourselves in case remote host does not already know us and supports Bonjour ... OK if this fails
				try {
					NetworkConfigurationFromMulticastDNS networkConfigurationFromMulticastDNS = new NetworkConfigurationFromMulticastDNS(debugLevel);
					networkConfigurationFromMulticastDNS.activateDiscovery();
					networkConfigurationFromMulticastDNS.registerDicomService(localAE,localPort,"WSD");
					Thread.currentThread().sleep(sleepTimeAfterRegisteringWithBonjour);		// wait a little while, in case remote host slow to pick up our AE information (else move might fail)
				}
				catch (Exception e) {
					//e.printStackTrace(System.err);
					System.err.println(e);
				}

				new SynchronizeFromRemoteSCP(databaseInformationModel,savedInstancesFolder,remoteHost,remotePort,remoteAE,localPort,localAE,useGet,queryAll,verbosityLevel,debugLevel);
				
				databaseInformationModel.close();	// important, else some received objects may not be registered in the database

				System.exit(0);		// this is untidy, but necessary if we are too lazy to stop the StorageSOPClassSCPDispatcher thread :(
			}
			else {
				System.err.println("Usage: java -cp ./pixelmed.jar:./lib/additional/hsqldb.jar:./lib/additional/commons-codec-1.3.jar:./lib/additional/jmdns.jar com.pixelmed.apps.SynchronizeFromRemoteSCP databasepath savedfilesfolder remoteHost remotePort remoteAET ourPort ourAET [GET|MOVE [ALL|SELECTIVE [verbositylevel [debuglevel]]]]");
			}
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
			System.exit(0);
		}
	}
}





/* Copyright (c) 2001-2008, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

import com.pixelmed.dicom.SetOfDicomFiles;
import com.pixelmed.dicom.TransferSyntax;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

/**
 * <p>A factory object of static methods that can create lists of presentation contexts
 * for initiating associations, from lists of DICOM files based on SOP Class (abstract
 * syntax) and supported transfer syntaxes.</p>
 *
 * @author	dclunie
 */
public class PresentationContextListFactory {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/PresentationContextListFactory.java,v 1.8 2008/09/25 16:35:38 dclunie Exp $";
	
	private static final int presentationContextIDStart = 1;
	private static final int presentationContextIDIncrement = 2;
	private static final int presentationContextIDMaximum = 255;
	
	private static final byte incrementPresentationContextID(byte presentationContextID) throws DicomNetworkException {
		if ((((int)presentationContextID) & 0xff) >= presentationContextIDMaximum) {
			throw new DicomNetworkException("Too many presentation contexts");
		}
		else {
			return (byte)(presentationContextID+presentationContextIDIncrement);
		}
	}

	/**
	 * @return	true if bzip2 codec available
	 */
	public static boolean haveBzip2Support() {
		boolean result=true;
		try {
			Class classToUse = Class.forName("org.apache.excalibur.bzip2.CBZip2InputStream");
		}
		catch (ClassNotFoundException e) {
			result=false;
		}
		return result;
	}
	
	private static boolean haveBzip2Support = haveBzip2Support();
	
	private static String[][] supportedTransferSyntaxes = {
		{
			TransferSyntax.ImplicitVRLittleEndian,
			TransferSyntax.ExplicitVRLittleEndian,
			TransferSyntax.ExplicitVRBigEndian,
		},
		{
			TransferSyntax.ImplicitVRLittleEndian,
			TransferSyntax.ExplicitVRLittleEndian,
			TransferSyntax.ExplicitVRBigEndian,
			TransferSyntax.DeflatedExplicitVRLittleEndian
		},
		{
			TransferSyntax.ImplicitVRLittleEndian,
			TransferSyntax.ExplicitVRLittleEndian,
			TransferSyntax.ExplicitVRBigEndian,
			TransferSyntax.PixelMedBzip2ExplicitVRLittleEndian,
			TransferSyntax.DeflatedExplicitVRLittleEndian
		}
	};
	
	/**
	 * Create lists of presentation contexts for initiating associations, from the specified abstract
	 * syntax and transfer syntax as well as all supported transfer syntaxes.
	 *
	 * @param	abstractSyntax			the SOP Class UID of the data set to be transmitted
	 * @param	transferSyntax			the Transfer Syntax UID in which the data set to be transmitted is encoded, or null if unknown
	 * @param	compressionLevel		0=none,1=propose deflate,2=propose deflate and bzip2
	 * @param	theirChoice			propose a single presentation context with all transfer syntaxes to allow them to choose
	 * @param	ourChoice			propose separate presentation contexts for each transfer syntax to allow us to choose
	 * @param	asEncoded			propose a separate presentation context for the specified transfer syntax in which the data set is known to be encoded
	 * @return					a LinkedList of PresentationContext
	 * @exception	DicomNetworkException		if too many presentation contexts
	 */
	static public LinkedList createNewPresentationContextList(String abstractSyntax,String transferSyntax,int compressionLevel,
			boolean theirChoice,boolean ourChoice,boolean asEncoded) throws DicomNetworkException {
	
		if (!haveBzip2Support && compressionLevel > 1 && (transferSyntax == null || !transferSyntax.equals(TransferSyntax.PixelMedBzip2ExplicitVRLittleEndian))) {
			compressionLevel=1;	// do not propose the bzip2 transfer syntax if the codec is not available
			// note that if the codec is not available and the transfer syntax is bzip2, may fail later if acceptor does not support bzip2 
		}

		LinkedList presentationContexts = new LinkedList();
		byte nextPresentationContextID = (byte)presentationContextIDStart;	// should always be odd numbered, starting with 0x01
		nextPresentationContextID = addPresentationContextsForAbstractSyntax(presentationContexts,nextPresentationContextID,abstractSyntax,transferSyntax,compressionLevel,
			theirChoice,ourChoice,asEncoded);
		return presentationContexts;
	}
	
	/**
	 * Create lists of presentation contexts for initiating associations, from the specified abstract
	 * syntax and transfer syntax as well as all supported transfer syntaxes.
	 *
	 * @param	abstractSyntax			the SOP Class UID of the data set to be transmitted
	 * @param	transferSyntax			the Transfer Syntax UID in which the data set to be transmitted is encoded, or null if unknown
	 * @param	compressionLevel		0=none,1=propose deflate,2=propose deflate and bzip2
	 * @return					a LinkedList of PresentationContext
	 * @exception	DicomNetworkException		if too many presentation contexts
	 */
	static public LinkedList createNewPresentationContextList(String abstractSyntax,String transferSyntax,int compressionLevel) throws DicomNetworkException {
		return createNewPresentationContextList(abstractSyntax,transferSyntax,compressionLevel,true,true,true);
	}
	
	/**
	 * Create lists of presentation contexts for initiating associations, from a set of SOP Class UIDs.
	 *
	 * @param	setOfSOPClassUIDs		the set of <code>String</code> SOP Class UIDs
	 * @param	compressionLevel		0=none,1=propose deflate,2=propose deflate and bzip2
	 * @param	theirChoice			propose a single presentation context with all transfer syntaxes to allow them to choose
	 * @param	ourChoice			propose separate presentation contexts for each transfer syntax to allow us to choose
	 * @param	asEncoded			propose a separate presentation context for the specified transfer syntax in which the data set is known to be encoded
	 * @return					a LinkedList of PresentationContext
	 * @exception	DicomNetworkException		if too many presentation contexts
	 */
	static public LinkedList createNewPresentationContextList(Set setOfSOPClassUIDs,int compressionLevel,
			boolean theirChoice,boolean ourChoice,boolean asEncoded) throws DicomNetworkException {
		
		if (!haveBzip2Support && compressionLevel > 1) {
			compressionLevel=1;	// do not propose the bzip2 transfer syntax if the codec is not available
		}

		LinkedList presentationContexts = new LinkedList();
		byte nextPresentationContextID = (byte)presentationContextIDStart;	// should always be odd numbered, starting with 0x01
		
		Iterator si = setOfSOPClassUIDs.iterator();
		while (si.hasNext()) {
			String sopClassUID = (String)(si.next());
			nextPresentationContextID = addPresentationContextsForAbstractSyntax(presentationContexts,nextPresentationContextID,sopClassUID,null,compressionLevel,
				theirChoice,ourChoice,false);
		}

		return presentationContexts;
	}
	
	/**
	 * Create lists of presentation contexts for initiating associations, from a set of SOP Class UIDs.
	 *
	 * @param	setOfSOPClassUIDs		the set of <code>String</code> SOP Class UIDs
	 * @param	compressionLevel		0=none,1=propose deflate,2=propose deflate and bzip2
	 * @return					a LinkedList of PresentationContext
	 * @exception	DicomNetworkException		if too many presentation contexts
	 */
	static public LinkedList createNewPresentationContextList(Set setOfSOPClassUIDs,int compressionLevel) throws DicomNetworkException {
		return createNewPresentationContextList(setOfSOPClassUIDs,compressionLevel,true,true,true);
	}
	
	/**
	 * Create lists of presentation contexts for initiating associations, from the abstract
	 * syntax and transfer syntax as well as all supported transfer syntaxes for all the
	 * files in the specified set of Dicom files.
	 *
	 * @param	dicomFiles			the set of files with their SOP Class UIDs and the Transfer Syntax UIDs
	 * @param	compressionLevel		0=none,1=propose deflate,2=propose deflate and bzip2
	 * @param	theirChoice			propose a single presentation context with all transfer syntaxes to allow them to choose
	 * @param	ourChoice			propose separate presentation contexts for each transfer syntax to allow us to choose
	 * @param	asEncoded			propose a separate presentation context for the specified transfer syntax in which the data set is known to be encoded
	 * @return					a LinkedList of PresentationContext
	 * @exception	DicomNetworkException		if too many presentation contexts
	 */
	static public LinkedList createNewPresentationContextList(SetOfDicomFiles dicomFiles,int compressionLevel,
			boolean theirChoice,boolean ourChoice,boolean asEncoded) throws DicomNetworkException {
		// For the time being, ignore the transfer syntax that the file happens to be
		// encoded in, and just add the transfer syntaxes that are generally supported
		return createNewPresentationContextList(dicomFiles.getSetOfSOPClassUIDs(),compressionLevel,theirChoice,ourChoice,asEncoded);
	}
	
	/**
	 * Create lists of presentation contexts for initiating associations, from the abstract
	 * syntax and transfer syntax as well as all supported transfer syntaxes for all the
	 * files in the specified set of Dicom files.
	 *
	 * @param	dicomFiles			the set of files with their SOP Class UIDs and the Transfer Syntax UIDs
	 * @param	compressionLevel		0=none,1=propose deflate,2=propose deflate and bzip2
	 * @return					a LinkedList of PresentationContext
	 * @exception	DicomNetworkException		if too many presentation contexts
	 */
	static public LinkedList createNewPresentationContextList(SetOfDicomFiles dicomFiles,int compressionLevel) throws DicomNetworkException {
		return createNewPresentationContextList(dicomFiles,compressionLevel,true,true,true);
	}
	
	/**
	 * Create lists of presentation contexts for initiating associations, from the specified abstract
	 * syntax and transfer syntax as well as all supported transfer syntaxes.
	 *
	 * @param	presentationContexts		the LinkedList of PresentationContext to be extended
	 * @param	presentationContextID		the next available (odd-numbered) Presentation Context ID to add
	 * @param	abstractSyntax			the SOP Class UID of the data set to be transmitted
	 * @param	transferSyntax			the Transfer Syntax UID in which the data set to be transmitted is encoded, or null if unknown
	 * @param	compressionLevel		0=none,1=propose deflate,2=propose deflate and bzip2
	 * @param	theirChoice			propose a single presentation context with all transfer syntaxes to allow them to choose
	 * @param	ourChoice			propose separate presentation contexts for each transfer syntax to allow us to choose
	 * @param	asEncoded			propose a separate presentation context for the specified transfer syntax in which the data set is known to be encoded
	 * @return					the LinkedList of PresentationContext extended
	 * @exception	DicomNetworkException		if too many presentation contexts
	 */
	static private byte addPresentationContextsForAbstractSyntax(
			LinkedList presentationContexts,byte presentationContextID,String abstractSyntax,String transferSyntax,int compressionLevel,
			boolean theirChoice,boolean ourChoice,boolean asEncoded) throws DicomNetworkException {
		// First propose a presentation context with all transfer syntaxes
		// What we get back will indicate the acceptor's preference, in case we want to use their choice ...
//System.err.println("PresentationContextListFactory.addPresentationContextsForAbstractSyntax(): staring presentationContextID = "+(presentationContextID&0xff));
		if (theirChoice) {
			LinkedList tslist = new LinkedList();
			if (transferSyntax != null && transferSyntax.length() > 0) {
				tslist.add(transferSyntax);	// always include the actual transfer syntax in which the input file is already encoded
			}
			for (int i=0; i<supportedTransferSyntaxes[compressionLevel].length; ++i) {
				// Don't want to add the same transfer syntax twice in the same presentation context, hence check ...
				if (transferSyntax == null || !transferSyntax.equals(supportedTransferSyntaxes[compressionLevel][i])) {
					tslist.add(supportedTransferSyntaxes[compressionLevel][i]);
				}
			}
			presentationContexts.add(new PresentationContext(presentationContextID,abstractSyntax,tslist));
		}
			
		// Now propose a presentation context for each transfer syntax
		// What we get back will tell us what the acceptor actually supports, in case we want to choose ourselves ...
			
		if (asEncoded && transferSyntax != null && transferSyntax.length() > 0) {
			// always include a presentation context for the actual transfer syntax in which the input file is already encoded
			presentationContextID=incrementPresentationContextID(presentationContextID);
			presentationContexts.add(new PresentationContext(presentationContextID,abstractSyntax,transferSyntax));
		}
			
		if (ourChoice) {
			for (int i=0; i<supportedTransferSyntaxes[compressionLevel].length; ++i) {
				presentationContextID=incrementPresentationContextID(presentationContextID);
				// Don't want to add the same transfer syntax twice in the same presentation context, hence check ...
				if (transferSyntax == null || !transferSyntax.equals(supportedTransferSyntaxes[compressionLevel][i])) {
					presentationContexts.add(new PresentationContext(presentationContextID,abstractSyntax,supportedTransferSyntaxes[compressionLevel][i]));
				}
			}
		}

		return incrementPresentationContextID(presentationContextID);		// return the next available number
	}

}




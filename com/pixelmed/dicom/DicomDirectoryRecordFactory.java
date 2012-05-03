/* Copyright (c) 2001-2011, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

/**
 * @author	dclunie
 */
public class DicomDirectoryRecordFactory {

	/***/
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/DicomDirectoryRecordFactory.java,v 1.24 2012/04/05 03:52:12 dclunie Exp $";

	/***/
	public class TopDirectoryRecord extends DicomDirectoryRecord {
		/***/
		TopDirectoryRecord() {
			super(null,null);
			uid="";
		}

		/***/
		public String toString() {
			return "Top";
		}

		/***/
		protected void makeStringValue() {
			stringValue="BAD";
		}

		/***/
		protected void makeIntegerValue() {
			integerValue=-1;
		}
	}

	/***/
	public class UnrecognizedDirectoryRecord extends DicomDirectoryRecord {
		/***/
		private String directoryRecordType;

		/***/
		UnrecognizedDirectoryRecord() {
			super(null,null);
			directoryRecordType="Unrecognized";
		}

		/**
		 * @param	parent
		 */
		UnrecognizedDirectoryRecord(DicomDirectoryRecord parent) {
			super(parent,null);
			directoryRecordType="Unrecognized";
		}

		/**
		 * @param	parent
		 * @param	list
		 */
		UnrecognizedDirectoryRecord(DicomDirectoryRecord parent,AttributeList list) {
			super(parent,list);
			directoryRecordType="Unrecognized";
		}

		/**
		 * @param	parent
		 * @param	list
		 * @param	name
		 */
		UnrecognizedDirectoryRecord(DicomDirectoryRecord parent,AttributeList list,String name) {
			super(parent,list);
			uid="";
			directoryRecordType=name;
		}

		/***/
		public String toString() {
			return directoryRecordType;
		}

		/***/
		protected void makeStringValue() {
			stringValue="BAD";
		}

		/***/
		protected void makeIntegerValue() {
			integerValue=-1;
		}
	}

	/***/
	public class PatientDirectoryRecord extends DicomDirectoryRecord {

		/**
		 * @param	parent
		 * @param	list
		 */
		PatientDirectoryRecord(DicomDirectoryRecord parent,AttributeList list) {
			super(parent,list);
			uid="";
		}

		/**
		 * @param	o
		 */
		public int compareTo(Object o) {
			return compareToByStringValue((DicomDirectoryRecord)o,true/*mustBeSameObjectToBeEqual*/);
		}

		/***/
		public String toString() {
			return "Patient "+getStringValue();
		}
	
		/***/
		protected void makeStringValue() {
			StringBuffer buffer = new StringBuffer();
			buffer.append(Attribute.getSingleStringValueOrEmptyString(getAttributeList(),TagFromName.PatientName));
			buffer.append(" ");
			buffer.append(Attribute.getSingleStringValueOrEmptyString(getAttributeList(),TagFromName.PatientID));
			stringValue=buffer.toString();
		}

		/***/
		protected void makeIntegerValue() {
			integerValue=-1;
		}
	}

	/***/
	public class StudyDirectoryRecord extends DicomDirectoryRecord {
		/**
		 * @param	parent
		 * @param	list
		 */
		StudyDirectoryRecord(DicomDirectoryRecord parent,AttributeList list) {
			super(parent,list);
			uid=Attribute.getSingleStringValueOrEmptyString(getAttributeList(),TagFromName.StudyInstanceUID);
		}

		/**
		 * @param	o
		 */
		public int compareTo(Object o) {
			return compareToByStringValue((DicomDirectoryRecord)o,true/*mustBeSameObjectToBeEqual*/);
		}

		/***/
		public String toString() {
			return "Study "+getStringValue();
		}
	
		/***/
		protected void makeStringValue() {
			StringBuffer buffer = new StringBuffer();
			buffer.append(Attribute.getSingleStringValueOrEmptyString(getAttributeList(),TagFromName.StudyDate));	// [bugs.mrmf] (000111) Studies in browser not sorted by date but ID, and don't display date
			buffer.append(" ");
			buffer.append(Attribute.getSingleStringValueOrEmptyString(getAttributeList(),TagFromName.StudyID));
			buffer.append(" ");
			buffer.append(Attribute.getSingleStringValueOrEmptyString(getAttributeList(),TagFromName.StudyDescription));
			stringValue=buffer.toString();
		}

		/***/
		protected void makeIntegerValue() {
			integerValue=-1;
		}
	}

	/***/
	public class SeriesDirectoryRecord extends DicomDirectoryRecord {
		/**
		 * @param	parent
		 * @param	list
		 */
		SeriesDirectoryRecord(DicomDirectoryRecord parent,AttributeList list) {
			super(parent,list);
			uid=Attribute.getSingleStringValueOrEmptyString(getAttributeList(),TagFromName.SeriesInstanceUID);
		}

		/***/
		public String toString() {
			return "Series "+getStringValue();
		}
	
		/**
		 * @param	o
		 */
		public int compareTo(Object o) {
			return compareToByIntegerValue((DicomDirectoryRecord)o,true/*mustBeSameObjectToBeEqual*/);
		}

		/***/
		protected void makeStringValue() {
			stringValue=DescriptionFactory.makeSeriesDescription(getAttributeList());
		}

		/***/
		protected void makeIntegerValue() {
			integerValue=Attribute.getSingleIntegerValueOrDefault(getAttributeList(),TagFromName.SeriesNumber,-1);
		}
	}

	/***/
	public class ConcatenationDirectoryRecord extends DicomDirectoryRecord {
		/**
		 * @param	parent
		 * @param	list
		 */
		ConcatenationDirectoryRecord(DicomDirectoryRecord parent,AttributeList list) {
			super(parent,list);
			uid=Attribute.getSingleStringValueOrEmptyString(getAttributeList(),TagFromName.ConcatenationUID);
//System.err.println("ConcatenationDirectoryRecord:");
//System.err.println(list);
		}

		/***/
		public String toString() {
			return "Concatenation "+getStringValue();
		}
	
		/**
		 * @param	o
		 */
		public int compareTo(Object o) {
			return compareToByIntegerValue((DicomDirectoryRecord)o,true/*mustBeSameObjectToBeEqual*/);
		}

		/***/
		protected void makeStringValue() {
			StringBuffer buffer = new StringBuffer();
			buffer.append(Attribute.getSingleStringValueOrEmptyString(getAttributeList(),TagFromName.InstanceNumber));
			stringValue=buffer.toString();
		}

		/***/
		protected void makeIntegerValue() {
			integerValue=Attribute.getSingleIntegerValueOrDefault(getAttributeList(),TagFromName.InstanceNumber,-1);
		}
	}

	/***/
	public class ImageDirectoryRecord extends DicomDirectoryRecord {
		/***/
		String recordNameFromSOPClass;
		
		/**
		 * @param	parent
		 * @param	list
		 */
		ImageDirectoryRecord(DicomDirectoryRecord parent,AttributeList list) {
			super(parent,list);
			uid=Attribute.getSingleStringValueOrEmptyString(getAttributeList(),TagFromName.ReferencedSOPInstanceUIDInFile);
			String sopClassUID = Attribute.getSingleStringValueOrNull(list,TagFromName.ReferencedSOPClassUIDInFile);
			recordNameFromSOPClass="Image";
			if (sopClassUID != null) {
				if      (SOPClass.isSpectroscopy(sopClassUID)) recordNameFromSOPClass="Spectra";	// non-standard. but used in tests prior to addition of specific directory record
				else if (SOPClass.isRawData(sopClassUID)) recordNameFromSOPClass="Raw Data";		// non-standard. but used in tests prior to addition of specific directory record
			}
		}

		/***/
		public String toString() {
			return recordNameFromSOPClass+" "+getStringValue();
		}
	
		/**
		 * @param	o
		 */
		public int compareTo(Object o) {
			return compareToByIntegerValue((DicomDirectoryRecord)o,true/*mustBeSameObjectToBeEqual*/);
		}

		/***/
		protected void makeStringValue() {
			stringValue=DescriptionFactory.makeImageDescription(getAttributeList());
		}

		/***/
		protected void makeIntegerValue() {
			integerValue=Attribute.getSingleIntegerValueOrDefault(getAttributeList(),TagFromName.InConcatenationNumber,-1);
			if (integerValue == -1) {
				integerValue=Attribute.getSingleIntegerValueOrDefault(getAttributeList(),TagFromName.InstanceNumber,-1);
			}
		}
	}

	/***/
	public class SpectroscopyDirectoryRecord extends DicomDirectoryRecord {
		/**
		 * @param	parent
		 * @param	list
		 */
		SpectroscopyDirectoryRecord(DicomDirectoryRecord parent,AttributeList list) {
			super(parent,list);
			uid=Attribute.getSingleStringValueOrEmptyString(getAttributeList(),TagFromName.ReferencedSOPInstanceUIDInFile);
		}

		/***/
		public String toString() {
			return "Spectra "+getStringValue();
		}
	
		/**
		 * @param	o
		 */
		public int compareTo(Object o) {
			return compareToByIntegerValue((DicomDirectoryRecord)o,true/*mustBeSameObjectToBeEqual*/);
		}

		/***/
		protected void makeStringValue() {
			StringBuffer buffer = new StringBuffer();
			String useNumber=Attribute.getSingleStringValueOrNull(getAttributeList(),TagFromName.InConcatenationNumber);
			if (useNumber == null) useNumber=Attribute.getSingleStringValueOrEmptyString(getAttributeList(),TagFromName.InstanceNumber);
			buffer.append(useNumber);
			buffer.append(" ");
			buffer.append(Attribute.getSingleStringValueOrEmptyString(getAttributeList(),TagFromName.ImageComments));
			stringValue=buffer.toString();
		}

		/***/
		protected void makeIntegerValue() {
			integerValue=Attribute.getSingleIntegerValueOrDefault(getAttributeList(),TagFromName.InConcatenationNumber,-1);
			if (integerValue == -1) {
				integerValue=Attribute.getSingleIntegerValueOrDefault(getAttributeList(),TagFromName.InstanceNumber,-1);
			}
		}
	}

	/***/
	abstract class SimpleInstanceDirectoryRecord extends DicomDirectoryRecord {
		/**
		 * @param	parent
		 * @param	list
		 */
		SimpleInstanceDirectoryRecord(DicomDirectoryRecord parent,AttributeList list) {
			super(parent,list);
			uid=Attribute.getSingleStringValueOrEmptyString(getAttributeList(),TagFromName.ReferencedSOPInstanceUIDInFile);
		}

		/***/
		abstract public String toString();
	
		/**
		 * @param	o
		 */
		public int compareTo(Object o) {
			return compareToByIntegerValue((DicomDirectoryRecord)o,true/*mustBeSameObjectToBeEqual*/);
		}

		/***/
		protected void makeStringValue() {
			StringBuffer buffer = new StringBuffer();
			buffer.append(Attribute.getSingleStringValueOrEmptyString(getAttributeList(),TagFromName.InstanceNumber));
			stringValue=buffer.toString();
		}

		/***/
		protected void makeIntegerValue() {
			integerValue=Attribute.getSingleIntegerValueOrDefault(getAttributeList(),TagFromName.InstanceNumber,-1);
		}
	}

	/***/
	public class RawDataDirectoryRecord extends SimpleInstanceDirectoryRecord {
		/***/
		RawDataDirectoryRecord(DicomDirectoryRecord parent,AttributeList list) {
			super(parent,list);
		}

		/***/
		public String toString() {
			return "Raw Data "+getStringValue();
		}
	}

	/***/
	public class SRDocumentDirectoryRecord extends SimpleInstanceDirectoryRecord {
		/***/
		SRDocumentDirectoryRecord(DicomDirectoryRecord parent,AttributeList list) {
			super(parent,list);
		}

		/***/
		public String toString() {
			return "SR Document "+getStringValue();
		}
	}

	/***/
	public class KODocumentDirectoryRecord extends SimpleInstanceDirectoryRecord {
		/***/
		KODocumentDirectoryRecord(DicomDirectoryRecord parent,AttributeList list) {
			super(parent,list);
		}

		/***/
		public String toString() {
			return "KO Document "+getStringValue();
		}
	}

	/***/
	public class WaveformDirectoryRecord extends SimpleInstanceDirectoryRecord {
		/***/
		WaveformDirectoryRecord(DicomDirectoryRecord parent,AttributeList list) {
			super(parent,list);
		}

		/***/
		public String toString() {
			return "Waveform "+getStringValue();
		}
	}

	/***/
	public class RTDoseDirectoryRecord extends SimpleInstanceDirectoryRecord {
		/***/
		RTDoseDirectoryRecord(DicomDirectoryRecord parent,AttributeList list) {
			super(parent,list);
		}

		/***/
		public String toString() {
			return "RT Dose "+getStringValue();
		}
	}

	/***/
	public class RTStructureSetDirectoryRecord extends SimpleInstanceDirectoryRecord {
		/***/
		RTStructureSetDirectoryRecord(DicomDirectoryRecord parent,AttributeList list) {
			super(parent,list);
		}

		/***/
		public String toString() {
			return "RT Structure Set "+getStringValue();
		}
	}

	/***/
	public class RTPlanDirectoryRecord extends SimpleInstanceDirectoryRecord {
		/***/
		RTPlanDirectoryRecord(DicomDirectoryRecord parent,AttributeList list) {
			super(parent,list);
		}

		/***/
		public String toString() {
			return "RT Plan "+getStringValue();
		}
	}

	/***/
	public class RTTreatmentRecordDirectoryRecord extends SimpleInstanceDirectoryRecord {
		/***/
		RTTreatmentRecordDirectoryRecord(DicomDirectoryRecord parent,AttributeList list) {
			super(parent,list);
		}

		/***/
		public String toString() {
			return "RT Treatment Record "+getStringValue();
		}
	}

	/***/
	public class PresentationStateDirectoryRecord extends SimpleInstanceDirectoryRecord {
		/***/
		PresentationStateDirectoryRecord(DicomDirectoryRecord parent,AttributeList list) {
			super(parent,list);
		}

		/***/
		public String toString() {
			return "PR "+getStringValue();
		}
	}
	
	/***/
	public class RegistrationDirectoryRecord extends SimpleInstanceDirectoryRecord {
		/***/
		RegistrationDirectoryRecord(DicomDirectoryRecord parent,AttributeList list) {
			super(parent,list);
		}

		/***/
		public String toString() {
			return "Reg "+getStringValue();
		}
	}
	
	/***/
	public class FiducialDirectoryRecord extends SimpleInstanceDirectoryRecord {
		/***/
		FiducialDirectoryRecord(DicomDirectoryRecord parent,AttributeList list) {
			super(parent,list);
		}

		/***/
		public String toString() {
			return "Fiducial "+getStringValue();
		}
	}

	/***/
	public class RealWorldValueMappingDirectoryRecord extends SimpleInstanceDirectoryRecord {
		/***/
		RealWorldValueMappingDirectoryRecord(DicomDirectoryRecord parent,AttributeList list) {
			super(parent,list);
		}

		/***/
		public String toString() {
			return "RWV "+getStringValue();
		}
	}
	
	/***/
	public class StereometricRelationshipDirectoryRecord extends SimpleInstanceDirectoryRecord {
		/***/
		StereometricRelationshipDirectoryRecord(DicomDirectoryRecord parent,AttributeList list) {
			super(parent,list);
		}

		/***/
		public String toString() {
			return "Stereo "+getStringValue();
		}
	}
	
	/***/
	public class EncapsulatedDocumentDirectoryRecord extends SimpleInstanceDirectoryRecord {
		/***/
		EncapsulatedDocumentDirectoryRecord(DicomDirectoryRecord parent,AttributeList list) {
			super(parent,list);
		}

		/***/
		public String toString() {
			return "Encap "+getStringValue();
		}
	}
	
	/***/
	public class HL7StructuredDocumentDirectoryRecord extends DicomDirectoryRecord {
		/**
		 * @param	parent
		 * @param	list
		 */
		HL7StructuredDocumentDirectoryRecord(DicomDirectoryRecord parent,AttributeList list) {
			super(parent,list);
			uid=Attribute.getSingleStringValueOrEmptyString(getAttributeList(),TagFromName.ReferencedSOPInstanceUIDInFile);		// ? HL7 Instance Identifier :(
		}

		/***/
		public String toString() {
			return "CDA "+getStringValue();
		}
		/**
		 * @param	o
		 */
		public int compareTo(Object o) {
			return compareToByStringValue((DicomDirectoryRecord)o,true/*mustBeSameObjectToBeEqual*/);
		}

		/***/
		protected void makeStringValue() {
			StringBuffer buffer = new StringBuffer();
			String title = Attribute.getSingleStringValueOrNull(getAttributeList(),TagFromName.DocumentTitle);
			if (title == null) {
				title = SequenceAttribute.getMeaningOfCodedSequenceAttributeOrDefault(list,TagFromName.HL7DocumentTypeCodeSequence,"");
			}
			buffer.append(title);
			stringValue=buffer.toString();
		}

		/***/
		protected void makeIntegerValue() {
			integerValue=Attribute.getSingleIntegerValueOrDefault(getAttributeList(),TagFromName.InstanceNumber,-1);		// InstanceNumber is not in standard directory record definition :(
		}
	}

	
	/**
	 * @param	parent
	 * @param	list
	 */
	public DicomDirectoryRecord getNewDicomDirectoryRecord(DicomDirectoryRecord parent,AttributeList list) {
		DicomDirectoryRecord record = null;
		
		{
			if (list == null) {
				record = new UnrecognizedDirectoryRecord(parent);
			}
			else {
//System.err.println("getNewDicomDirectoryRecord: "+list);
				String directoryRecordType=Attribute.getSingleStringValueOrEmptyString(list,TagFromName.DirectoryRecordType);
				if (directoryRecordType.equals(DicomDirectoryRecordType.patient)) {
					record = new PatientDirectoryRecord(parent,list);
				}
				else if (directoryRecordType.equals(DicomDirectoryRecordType.study)) {
					record = new StudyDirectoryRecord(parent,list);
				}
				else if (directoryRecordType.equals(DicomDirectoryRecordType.series)) {
					record = new SeriesDirectoryRecord(parent,list);
				}
				else if (directoryRecordType.equals(DicomDirectoryRecordType.concatentation)) {
					record = new ConcatenationDirectoryRecord(parent,list);
				}
				else if (directoryRecordType.equals(DicomDirectoryRecordType.image)) {
					record = new ImageDirectoryRecord(parent,list);
				}
				else if (directoryRecordType.equals(DicomDirectoryRecordType.srDocument)) {
					record = new SRDocumentDirectoryRecord(parent,list);
				}
				else if (directoryRecordType.equals(DicomDirectoryRecordType.keyObjectDocument)) {
					record = new KODocumentDirectoryRecord(parent,list);
				}
				else if (directoryRecordType.equals(DicomDirectoryRecordType.waveform)) {
					record = new WaveformDirectoryRecord(parent,list);
				}
				else if (directoryRecordType.equals(DicomDirectoryRecordType.spectroscopy)) {
					record = new SpectroscopyDirectoryRecord(parent,list);
				}
				else if (directoryRecordType.equals(DicomDirectoryRecordType.rawData)) {
					record = new RawDataDirectoryRecord(parent,list);
				}
				else if (directoryRecordType.equals(DicomDirectoryRecordType.rtDose)) {
					record = new RTDoseDirectoryRecord(parent,list);
				}
				else if (directoryRecordType.equals(DicomDirectoryRecordType.rtStructureSet)) {
					record = new RTStructureSetDirectoryRecord(parent,list);
				}
				else if (directoryRecordType.equals(DicomDirectoryRecordType.rtPlan)) {
					record = new RTPlanDirectoryRecord(parent,list);
				}
				else if (directoryRecordType.equals(DicomDirectoryRecordType.rtTreatmentRecord)) {
					record = new RTTreatmentRecordDirectoryRecord(parent,list);
				}
				else if (directoryRecordType.equals(DicomDirectoryRecordType.presentationState)) {
					record = new PresentationStateDirectoryRecord(parent,list);
				}
				else if (directoryRecordType.equals(DicomDirectoryRecordType.registration)) {
					record = new RegistrationDirectoryRecord(parent,list);
				}
				else if (directoryRecordType.equals(DicomDirectoryRecordType.fiducial)) {
					record = new FiducialDirectoryRecord(parent,list);
				}
				else if (directoryRecordType.equals(DicomDirectoryRecordType.realWorldValueMapping)) {
					record = new RealWorldValueMappingDirectoryRecord(parent,list);
				}
				else if (directoryRecordType.equals(DicomDirectoryRecordType.stereometricRelationship)) {
					record = new StereometricRelationshipDirectoryRecord(parent,list);
				}
				else if (directoryRecordType.equals(DicomDirectoryRecordType.encapsulatedDocument)) {
					record = new EncapsulatedDocumentDirectoryRecord(parent,list);
				}
				else if (directoryRecordType.equals(DicomDirectoryRecordType.hl7StructuredDocument)) {
					record = new HL7StructuredDocumentDirectoryRecord(parent,list);
				}
				else {
					record = new UnrecognizedDirectoryRecord(parent,list,directoryRecordType);
				}
			}
		}

		return record;
	}

	/***/
	public TopDirectoryRecord getNewTopDirectoryRecord() {
		return new TopDirectoryRecord();
	}
	
	/***/
	public PatientDirectoryRecord getNewPatientDirectoryRecord(DicomDirectoryRecord parent,AttributeList list) {
		return new PatientDirectoryRecord(parent,list);
	}
	
	/***/
	public StudyDirectoryRecord getNewStudyDirectoryRecord(DicomDirectoryRecord parent,AttributeList list) {
		return new StudyDirectoryRecord(parent,list);
	}
	
	/***/
	public SeriesDirectoryRecord getNewSeriesDirectoryRecord(DicomDirectoryRecord parent,AttributeList list) {
		return new SeriesDirectoryRecord(parent,list);
	}
	
	/***/
	public ImageDirectoryRecord getNewImageDirectoryRecord(DicomDirectoryRecord parent,AttributeList list) {
		return new ImageDirectoryRecord(parent,list);
	}
	
	/***/
	public SRDocumentDirectoryRecord getNewSRDocumentDirectoryRecord(DicomDirectoryRecord parent,AttributeList list) {
		return new SRDocumentDirectoryRecord(parent,list);
	}
	
	/***/
	public KODocumentDirectoryRecord getNewKODocumentDirectoryRecord(DicomDirectoryRecord parent,AttributeList list) {
		return new KODocumentDirectoryRecord(parent,list);
	}
	
	/***/
	public RTDoseDirectoryRecord getNewRTDoseDirectoryRecord(DicomDirectoryRecord parent,AttributeList list) {
		return new RTDoseDirectoryRecord(parent,list);
	}
	
	/***/
	public RTStructureSetDirectoryRecord getNewRTStructureSetDirectoryRecord(DicomDirectoryRecord parent,AttributeList list) {
		return new RTStructureSetDirectoryRecord(parent,list);
	}
	
	/***/
	public RTPlanDirectoryRecord getNewRTPlanDirectoryRecord(DicomDirectoryRecord parent,AttributeList list) {
		return new RTPlanDirectoryRecord(parent,list);
	}
	
	/***/
	public RTTreatmentRecordDirectoryRecord getNewRTTreatmentRecordDirectoryRecord(DicomDirectoryRecord parent,AttributeList list) {
		return new RTTreatmentRecordDirectoryRecord(parent,list);
	}
	
	/***/
	public PresentationStateDirectoryRecord getNewPresentationStateDirectoryRecord(DicomDirectoryRecord parent,AttributeList list) {
		return new PresentationStateDirectoryRecord(parent,list);
	}
	
	/***/
	public SpectroscopyDirectoryRecord getNewSpectroscopyDirectoryRecord(DicomDirectoryRecord parent,AttributeList list) {
		return new SpectroscopyDirectoryRecord(parent,list);
	}
	
	/***/
	public RawDataDirectoryRecord getNewRawDataDirectoryRecord(DicomDirectoryRecord parent,AttributeList list) {
		return new RawDataDirectoryRecord(parent,list);
	}
	
	/***/
	public WaveformDirectoryRecord getNewWaveformDirectoryRecord(DicomDirectoryRecord parent,AttributeList list) {
		return new WaveformDirectoryRecord(parent,list);
	}
	
	/***/
	public RegistrationDirectoryRecord getNewRegistrationDirectoryRecord(DicomDirectoryRecord parent,AttributeList list) {
		return new RegistrationDirectoryRecord(parent,list);
	}
	
	/***/
	public FiducialDirectoryRecord getNewFiducialDirectoryRecord(DicomDirectoryRecord parent,AttributeList list) {
		return new FiducialDirectoryRecord(parent,list);
	}
	
	/***/
	public RealWorldValueMappingDirectoryRecord getNewRealWorldValueMappingDirectoryRecord(DicomDirectoryRecord parent,AttributeList list) {
		return new RealWorldValueMappingDirectoryRecord(parent,list);
	}
	
	/***/
	public StereometricRelationshipDirectoryRecord getNewStereometricRelationshipDirectoryRecord(DicomDirectoryRecord parent,AttributeList list) {
		return new StereometricRelationshipDirectoryRecord(parent,list);
	}
	
	/***/
	public EncapsulatedDocumentDirectoryRecord getNewEncapsulatedDocumentDirectoryRecord(DicomDirectoryRecord parent,AttributeList list) {
		return new EncapsulatedDocumentDirectoryRecord(parent,list);
	}
	
	/***/
	public HL7StructuredDocumentDirectoryRecord getNewHL7StructuredDocumentDirectoryRecord(DicomDirectoryRecord parent,AttributeList list) {
		return new HL7StructuredDocumentDirectoryRecord(parent,list);
	}
}



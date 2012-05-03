/* Copyright (c) 2001-2011, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

public class CompositeInstanceContext {
	
	protected AttributeList list;
	
	public AttributeList getAttributeList() { return list; }
	
	public boolean equals(Object o) {
		if (o instanceof CompositeInstanceContext) {
			return list.equals(((CompositeInstanceContext)o).getAttributeList());
		}
		else {
			return false;
		}
	}
	
	public int hashCode() {
		return list.hashCode();
	}
	
	protected void addOrReplaceIfNotEmptyOtherwiseLeaveUnchanged(AttributeList srcList,AttributeTag tag) {
		Attribute a = srcList.get(tag);
		if (a != null) {
			if (a.getVM() > 0 || (a instanceof SequenceAttribute && ((SequenceAttribute)a).getNumberOfItems() > 0)) {
				if (list.get(tag) == null) {
					list.put(tag,a);	// make sure that an empty attribute is add if not already there
				}
				// else leave existing (possibly empty) value alone
			}
			else {
				list.put(tag,a);	// adds, or replaces existing
			}
		}
	}
	
	public CompositeInstanceContext() {
		list = new AttributeList();
	}
	
	public CompositeInstanceContext(AttributeList srcList) {
		list = new AttributeList();
		updateFromSource(srcList);
	}
	
	protected static AttributeTag[] patientModuleAttributeTags = {
		TagFromName.PatientName,
		TagFromName.PatientID,
		//Macro IssuerOfPatientIDMacro
		TagFromName.IssuerOfPatientID,
		TagFromName.IssuerOfPatientIDQualifiersSequence,
		//EndMacro IssuerOfPatientIDMacro
		TagFromName.PatientBirthDate,
		TagFromName.PatientSex,
		TagFromName.PatientBirthTime,
		TagFromName.ReferencedPatientSequence,
		TagFromName.OtherPatientIDs,
		TagFromName.OtherPatientIDsSequence,
		TagFromName.OtherPatientNames,
		TagFromName.EthnicGroup,
		TagFromName.PatientComments,
		TagFromName.PatientSpeciesDescription,
		TagFromName.PatientSpeciesCodeSequence,
		TagFromName.PatientBreedDescription,
		TagFromName.PatientBreedCodeSequence,
		TagFromName.BreedRegistrationSequence,
		TagFromName.ResponsiblePerson,
		TagFromName.ResponsiblePersonRole,
		TagFromName.ResponsibleOrganization,
		TagFromName.PatientIdentityRemoved,
		TagFromName.DeidentificationMethod,
		TagFromName.DeidentificationMethodCodeSequence
	};
	
	protected static AttributeTag[] clinicalTrialSubjectModuleAttributeTags = {
		TagFromName.ClinicalTrialSubjectID,
		TagFromName.ClinicalTrialSponsorName,
		TagFromName.ClinicalTrialProtocolID,
		TagFromName.ClinicalTrialProtocolName,
		TagFromName.ClinicalTrialSiteID,
		TagFromName.ClinicalTrialSiteName,
		TagFromName.ClinicalTrialSubjectID,
		TagFromName.ClinicalTrialSubjectReadingID
	};
	
	protected static AttributeTag[] generalStudyModuleAttributeTags = {
		TagFromName.StudyInstanceUID,
		TagFromName.StudyDate,
		TagFromName.StudyTime,
		TagFromName.ReferringPhysicianName,
		TagFromName.ReferringPhysicianIdentificationSequence,
		TagFromName.StudyID,
		TagFromName.AccessionNumber,
		TagFromName.IssuerOfAccessionNumberSequence,
		TagFromName.StudyDescription,
		TagFromName.PhysiciansOfRecord,
		TagFromName.PhysiciansOfRecordIdentificationSequence,
		TagFromName.NameOfPhysiciansReadingStudy,
		TagFromName.PhysiciansReadingStudyIdentificationSequence,
		TagFromName.RequestingServiceCodeSequence,
		TagFromName.ReferencedStudySequence,
		TagFromName.ProcedureCodeSequence,
		TagFromName.ReasonForPerformedProcedureCodeSequence
	};

	protected static AttributeTag[] patientStudyModuleAttributeTags = {
		TagFromName.AdmittingDiagnosesDescription,
		TagFromName.AdmittingDiagnosesCodeSequence,
		TagFromName.PatientAge,
		TagFromName.PatientSize,
		TagFromName.PatientWeight,
		TagFromName.PatientSizeCodeSequence,
		TagFromName.Occupation,
		TagFromName.AdditionalPatientHistory,
		TagFromName.AdmissionID,
		TagFromName.IssuerOfAdmissionID,
		TagFromName.IssuerOfAdmissionIDSequence,
		TagFromName.ServiceEpisodeID,
		TagFromName.IssuerOfServiceEpisodeIDSequence,
		TagFromName.ServiceEpisodeDescription,
		TagFromName.PatientSexNeutered
	};
	
	protected static AttributeTag[] generalSeriesModuleAttributeTags = {
		TagFromName.Modality,
		TagFromName.SeriesInstanceUID,
		TagFromName.SeriesNumber,
		TagFromName.Laterality,
		TagFromName.SeriesDate,
		TagFromName.SeriesTime,
		TagFromName.PerformingPhysicianName,
		TagFromName.PerformingPhysicianIdentificationSequence,
		TagFromName.ProtocolName,
		TagFromName.SeriesDescription,
		TagFromName.SeriesDescriptionCodeSequence,
		TagFromName.OperatorsName,
		TagFromName.OperatorIdentificationSequence,
		TagFromName.ReferencedPerformedProcedureStepSequence,
		TagFromName.RelatedSeriesSequence,
		TagFromName.BodyPartExamined,
		TagFromName.PatientPosition,
		//TagFromName.SmallestPixelValueInSeries,
		//TagFromName.LargestPixelValueInSeries,
		TagFromName.RequestAttributesSequence,
		//Macro PerformedProcedureStepSummaryMacro
		TagFromName.PerformedProcedureStepID,
		TagFromName.PerformedProcedureStepStartDate,
		TagFromName.PerformedProcedureStepStartTime,
		TagFromName.PerformedProcedureStepDescription,
		TagFromName.PerformedProtocolCodeSequence,
		TagFromName.CommentsOnThePerformedProcedureStep,
		//EndMacro PerformedProcedureStepSummaryMacro
		TagFromName.AnatomicalOrientationType
	};
	
	protected static AttributeTag[] generalEquipmentModuleAttributeTags = {
		TagFromName.Manufacturer,
		TagFromName.InstitutionName,
		TagFromName.InstitutionAddress,
		TagFromName.StationName,
		TagFromName.InstitutionalDepartmentName,
		TagFromName.ManufacturerModelName,
		TagFromName.DeviceSerialNumber,
		TagFromName.SoftwareVersions,
		TagFromName.GantryID,
		TagFromName.SpatialResolution,
		TagFromName.DateOfLastCalibration,
		TagFromName.TimeOfLastCalibration,
		TagFromName.PixelPaddingValue
	};
	
	protected static AttributeTag[] frameOfReferenceModuleAttributeTags = {
		TagFromName.FrameOfReferenceUID,
		TagFromName.PositionReferenceIndicator
	};
	
	protected static AttributeTag[] sopCommonModuleAttributeTags = {
		TagFromName.SOPClassUID,
		TagFromName.SOPInstanceUID,
		//TagFromName.SpecificCharacterSet,
		TagFromName.InstanceCreationDate,
		TagFromName.InstanceCreationTime,
		TagFromName.InstanceCreatorUID,
		TagFromName.RelatedGeneralSOPClassUID,
		TagFromName.OriginalSpecializedSOPClassUID,
		TagFromName.CodingSchemeIdentificationSequence,
		TagFromName.TimezoneOffsetFromUTC,
		TagFromName.ContributingEquipmentSequence,
		TagFromName.InstanceNumber,
		TagFromName.SOPInstanceStatus,
		TagFromName.SOPAuthorizationDateTime,
		TagFromName.SOPAuthorizationComment,
		TagFromName.AuthorizationEquipmentCertificationNumber,
		//Macro DigitalSignaturesMacro
		//TagFromName.MACParametersSequence,
		//TagFromName.DigitalSignaturesSequence,
		//EndMacro DigitalSignaturesMacro
		//TagFromName.EncryptedAttributesSequence,
		TagFromName.OriginalAttributesSequence,
		TagFromName.HL7StructuredDocumentReferenceSequence
	};
	
	protected static AttributeTag[] generalImageModuleAttributeTags = {
		TagFromName.ContentDate,
		TagFromName.ContentTime
	};
	
	protected static AttributeTag[] srDocumentGeneralModuleAttributeTags = {
		TagFromName.ReferencedRequestSequence,		// cw. RequestAttributesSequence in GeneralSeries
		TagFromName.PerformedProcedureCodeSequence	// cw. ProcedureCodeSequence in GeneralStudy
	};
	
	protected void createReferencedRequestSequenceIfAbsent(AttributeList srcList) {
		try {
			Attribute referencedRequestSequence = list.get(TagFromName.ReferencedRequestSequence);
			Attribute requestAttributesSequence = list.get(TagFromName.RequestAttributesSequence);
			if (referencedRequestSequence == null || !(referencedRequestSequence instanceof SequenceAttribute) || ((SequenceAttribute)referencedRequestSequence).getNumberOfItems() == 0) {
				if (requestAttributesSequence != null && requestAttributesSequence instanceof SequenceAttribute) {
					SequenceAttribute sRequestAttributesSequence = (SequenceAttribute)requestAttributesSequence;
					int nItems = sRequestAttributesSequence.getNumberOfItems();
					if (nItems > 0) {
						SequenceAttribute sReferencedRequestSequence = new SequenceAttribute(TagFromName.ReferencedRequestSequence);
						for (int i=0; i<nItems; ++i) {
							SequenceItem item = sRequestAttributesSequence.getItem(i);
//System.err.println("CompositeInstanceContext.updateFromSource(): copying RequestAttributesSequence to ReferencedRequestSequence item "+item);
							// copy only what is relevant and required ...
							AttributeList requestAttributesSequenceItemList = item.getAttributeList();
							AttributeList referencedRequestSequenceItemList = new AttributeList();

							{
								AttributeTag tag = TagFromName.StudyInstanceUID;
								// Type 1 in ReferencedRequestSequence			Type 3 in RequestAttributesSequence
								String s = Attribute.getSingleStringValueOrEmptyString(requestAttributesSequenceItemList,tag); 
								{ Attribute a = new UniqueIdentifierAttribute(tag); a.addValue(s); referencedRequestSequenceItemList.put(a); }
							}
							{
								AttributeTag tag = TagFromName.ReferencedStudySequence;
								// Type 2 in ReferencedRequestSequence			Type 3 in RequestAttributesSequence
								Attribute a = requestAttributesSequenceItemList.get(tag);
								if (a == null) {
									a = new SequenceAttribute(TagFromName.ReferencedStudySequence);
								}
								referencedRequestSequenceItemList.put(a);
							}
							{
								AttributeTag tag = TagFromName.AccessionNumber;
								// Type 2 in ReferencedRequestSequence			Type 3 in RequestAttributesSequence
								String s = Attribute.getSingleStringValueOrEmptyString(requestAttributesSequenceItemList,tag); 
								{ Attribute a = new ShortStringAttribute(tag); a.addValue(s); referencedRequestSequenceItemList.put(a); }
							}
							{
								AttributeTag tag = TagFromName.IssuerOfAccessionNumberSequence;
								// Type 3 in ReferencedRequestSequence			Type 3 in RequestAttributesSequence
								Attribute a = requestAttributesSequenceItemList.get(tag);
								if (a != null) {
									referencedRequestSequenceItemList.put(a);
								}
							}
							{
								AttributeTag tag = TagFromName.PlacerOrderNumberImagingServiceRequest;
								// Type 2 in ReferencedRequestSequence			Not in RequestAttributesSequence
								String s = Attribute.getSingleStringValueOrEmptyString(requestAttributesSequenceItemList,tag); 
								{ Attribute a = new LongStringAttribute(tag); a.addValue(s); referencedRequestSequenceItemList.put(a); }
							}
							{
								AttributeTag tag = TagFromName.OrderPlacerIdentifierSequence;
								// Type 3 in ReferencedRequestSequence			Not in RequestAttributesSequence
								Attribute a = requestAttributesSequenceItemList.get(tag);
								if (a != null) {
									referencedRequestSequenceItemList.put(a);
								}
							}
							{
								AttributeTag tag = TagFromName.FillerOrderNumberImagingServiceRequest;
								// Type 2 in ReferencedRequestSequence			Not in RequestAttributesSequence
								String s = Attribute.getSingleStringValueOrEmptyString(requestAttributesSequenceItemList,tag); 
								{ Attribute a = new LongStringAttribute(tag); a.addValue(s); referencedRequestSequenceItemList.put(a); }
							}
							{
								AttributeTag tag = TagFromName.OrderFillerIdentifierSequence;
								// Type 3 in ReferencedRequestSequence			Not in RequestAttributesSequence
								Attribute a = requestAttributesSequenceItemList.get(tag);
								if (a != null) {
									referencedRequestSequenceItemList.put(a);
								}
							}
							{
								AttributeTag tag = TagFromName.RequestedProcedureID;
								// Type 2 in ReferencedRequestSequence			Type 1C in RequestAttributesSequence (if procedure was scheduled)
								String s = Attribute.getSingleStringValueOrEmptyString(requestAttributesSequenceItemList,tag); 
								{ Attribute a = new ShortStringAttribute(tag); a.addValue(s); referencedRequestSequenceItemList.put(a); }
							}
							{
								AttributeTag tag = TagFromName.RequestedProcedureDescription;
								// Type 2 in ReferencedRequestSequence			Type 3 in RequestAttributesSequence
								String s = Attribute.getSingleStringValueOrEmptyString(requestAttributesSequenceItemList,tag); 
								{ Attribute a = new LongStringAttribute(tag); a.addValue(s); referencedRequestSequenceItemList.put(a); }
							}
							{
								AttributeTag tag = TagFromName.RequestedProcedureCodeSequence;
								// Type 2 in ReferencedRequestSequence			Type 3 in RequestAttributesSequence
								Attribute a = requestAttributesSequenceItemList.get(tag);
								if (a == null) {
									a = new SequenceAttribute(TagFromName.ReferencedStudySequence);
								}
								referencedRequestSequenceItemList.put(a);
							}
							{
								AttributeTag tag = TagFromName.ReasonForTheRequestedProcedure;
								// Type 3 in ReferencedRequestSequence			Type 3 in RequestAttributesSequence
								String s = Attribute.getSingleStringValueOrEmptyString(requestAttributesSequenceItemList,tag); 
								if (s.length() > 0) {
									Attribute a = new LongStringAttribute(tag); a.addValue(s); referencedRequestSequenceItemList.put(a);
								}
							}
							{
								AttributeTag tag = TagFromName.ReasonForRequestedProcedureCodeSequence;
								// Type 3 in ReferencedRequestSequence			Type 3 in RequestAttributesSequence
								Attribute a = requestAttributesSequenceItemList.get(tag);
								if (a != null) {
									referencedRequestSequenceItemList.put(a);
								}
							}
							
							sReferencedRequestSequence.addItem(referencedRequestSequenceItemList);
						}
						list.put(sReferencedRequestSequence);
					}
				}
			}
		}
		catch (DicomException e) {
			// trap the exception, since not a big deal if we fail
			e.printStackTrace(System.err);
		}
	}

	public void updateFromSource(AttributeList srcList) {
		for (AttributeTag t : patientModuleAttributeTags) { addOrReplaceIfNotEmptyOtherwiseLeaveUnchanged(srcList,t); }
		for (AttributeTag t : clinicalTrialSubjectModuleAttributeTags) { addOrReplaceIfNotEmptyOtherwiseLeaveUnchanged(srcList,t); }
		for (AttributeTag t : generalStudyModuleAttributeTags) { addOrReplaceIfNotEmptyOtherwiseLeaveUnchanged(srcList,t); }
		for (AttributeTag t : patientStudyModuleAttributeTags) { addOrReplaceIfNotEmptyOtherwiseLeaveUnchanged(srcList,t); }
		for (AttributeTag t : generalSeriesModuleAttributeTags) { addOrReplaceIfNotEmptyOtherwiseLeaveUnchanged(srcList,t); }
		for (AttributeTag t : generalEquipmentModuleAttributeTags) { addOrReplaceIfNotEmptyOtherwiseLeaveUnchanged(srcList,t); }
		for (AttributeTag t : frameOfReferenceModuleAttributeTags) { addOrReplaceIfNotEmptyOtherwiseLeaveUnchanged(srcList,t); }
		for (AttributeTag t : sopCommonModuleAttributeTags) { addOrReplaceIfNotEmptyOtherwiseLeaveUnchanged(srcList,t); }
		for (AttributeTag t : generalImageModuleAttributeTags) { addOrReplaceIfNotEmptyOtherwiseLeaveUnchanged(srcList,t); }
		for (AttributeTag t : srDocumentGeneralModuleAttributeTags) { addOrReplaceIfNotEmptyOtherwiseLeaveUnchanged(srcList,t); }

		// handle population of SRDocumentGeneralModule specific attributes from image equivalents
		createReferencedRequestSequenceIfAbsent(srcList);
		{
			Attribute performedProcedureCodeSequence = list.get(TagFromName.PerformedProcedureCodeSequence);
			Attribute procedureCodeSequence = list.get(TagFromName.ProcedureCodeSequence);
			if (performedProcedureCodeSequence == null || !(performedProcedureCodeSequence instanceof SequenceAttribute) || ((SequenceAttribute)performedProcedureCodeSequence).getNumberOfItems() == 0) {
				if (procedureCodeSequence != null && procedureCodeSequence instanceof SequenceAttribute) {
					SequenceAttribute sProcedureCodeSequence = (SequenceAttribute)procedureCodeSequence;
					int nItems = sProcedureCodeSequence.getNumberOfItems();
					if (nItems > 0) {
						SequenceAttribute sPerformedProcedureCodeSequence = new SequenceAttribute(TagFromName.PerformedProcedureCodeSequence);
						for (int i=0; i<nItems; ++i) {
							SequenceItem item = sProcedureCodeSequence.getItem(i);
//System.err.println("CompositeInstanceContext.updateFromSource(): copying ProcedureCodeSequence to PerformedProcedureCodeSequence item "+item);
							sPerformedProcedureCodeSequence.addItem(item);			// re-use of same item without cloning it is fine
						}
						list.put(sPerformedProcedureCodeSequence);
					}
				}
			}
		}
		list.removeGroupLengthAttributes();		// may be present within in Sequences that have been copied
	}

	public static void removePatient(AttributeList list) {
		for (AttributeTag t : patientModuleAttributeTags) { list.remove(t); }
		for (AttributeTag t : clinicalTrialSubjectModuleAttributeTags) { list.remove(t); }
	}
	
	public static void removeAllButPatient(AttributeList list) {
		removeStudy(list);
		removeSeries(list);
		removeEquipment(list);
		removeFrameOfReference(list);
		removeInstance(list);
		removeSRDocumentGeneral(list);
	}
	
	public static void removeAllButPatientAndStudy(AttributeList list) {
		removeSeries(list);
		removeEquipment(list);
		removeFrameOfReference(list);
		removeInstance(list);
		removeSRDocumentGeneral(list);
	}
	
	public static void removeStudy(AttributeList list) {
		for (AttributeTag t : generalStudyModuleAttributeTags) { list.remove(t); }
		for (AttributeTag t : patientStudyModuleAttributeTags) { list.remove(t); }
	}
	
	public static void removeSeries(AttributeList list) {
		for (AttributeTag t : generalSeriesModuleAttributeTags) { list.remove(t); }
	}
	
	public static void removeEquipment(AttributeList list) {
		for (AttributeTag t : generalEquipmentModuleAttributeTags) { list.remove(t); }
	}
	
	public static void removeFrameOfReference(AttributeList list) {
		for (AttributeTag t : frameOfReferenceModuleAttributeTags) { list.remove(t); }
	}
	
	public static void removeInstance(AttributeList list) {
		for (AttributeTag t : sopCommonModuleAttributeTags) { list.remove(t); }
		for (AttributeTag t : generalImageModuleAttributeTags) { list.remove(t); }
	}
	
	public static void removeSRDocumentGeneral(AttributeList list) {
		for (AttributeTag t : srDocumentGeneralModuleAttributeTags) { list.remove(t); }
	}
	
	public void removePatient() {
		removePatient(list);
	}
	
	public void removeAllButPatient() {
		removeAllButPatient(list);
	}
	
	public void removeAllButPatientAndStudy() {
		removeAllButPatientAndStudy(list);
	}
	
	public void removeStudy() {
		removeStudy(list);
	}
	
	public void removeSeries() {
		removeSeries(list);
	}
	
	public void removeEquipment() {
		removeEquipment(list);
	}
	
	public void removeFrameOfReference() {
		removeFrameOfReference(list);
	}
	
	public void removeInstance() {
		removeInstance(list);
	}
	
	public void removeSRDocumentGeneral() {
		removeSRDocumentGeneral(list);
	}
	
	public void put(Attribute a) {
		list.put(a);
	}
	
	public void putAll(AttributeList srcList) {
		list.putAll(srcList);
	}
	
	public String toString() {
		return list.toString();
	}

}


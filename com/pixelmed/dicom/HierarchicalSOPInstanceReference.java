/* Copyright (c) 2001-2010, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * <p>A class to represent the study, series and instance identifiers necessary to retrieve a specific instance using the hierarchical model.</p>
 *
 * <p>Used, for example, when extracting a map of instance uids to hierarchical references from an SR evidence sequence.</p>
 *
 *
 * @author	dclunie
 */
public class HierarchicalSOPInstanceReference {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/HierarchicalSOPInstanceReference.java,v 1.1 2010/08/29 08:25:47 dclunie Exp $";

	protected String studyInstanceUID;
	protected String seriesInstanceUID;
	protected String sopInstanceUID;
	protected String sopClassUID;

	/**
	 * <p>Construct an instance of a reference to an instance, with its hierarchy.</p>
	 *
	 * @param	studyInstanceUID	the Study Instance UID
	 * @param	seriesInstanceUID	the Series Instance UID
	 * @param	sopInstanceUID		the SOP Instance UID
	 * @param	sopClassUID			the SOP Class UID
	 */
	public HierarchicalSOPInstanceReference(String studyInstanceUID,String seriesInstanceUID,String sopInstanceUID,String sopClassUID) {
		this.studyInstanceUID=studyInstanceUID;
		this.seriesInstanceUID=seriesInstanceUID;
		this.sopInstanceUID=sopInstanceUID;
		this.sopClassUID=sopClassUID;
	}

	/**
	 * <p>Get the Study Instance UID.</p>
	 *
	 * @return		the Study Instance UID, or null
	 */
	public String getStudyInstanceUID() { return studyInstanceUID; }

	/**
	 * <p>Get the Series Instance UID.</p>
	 *
	 * @return		the Series Instance UID, or null
	 */
	public String getSeriesInstanceUID() { return seriesInstanceUID; }

	/**
	 * <p>Get the SOP Instance UID.</p>
	 *
	 * @return		the SOP Instance UID, or null
	 */
	public String getSOPInstanceUID() { return sopInstanceUID; }

	/**
	 * <p>Get the SOP Class UID.</p>
	 *
	 * @return		the SOP Class UID, or null
	 */
	public String getSOPClassUID()    { return sopClassUID; }

	/**
	 * <p>Find hierarchical references to instances that may be referenced in the content tree of an SR object.</p>
	 *
	 * <p>Uses the mandatory Current Requested Procedure Evidence Sequence in the top level dataset of an SR object.</p>
	 *
	 * @param	list	the top evel dataset of an SR instances
	 * @return		a {@link java.util.Map Map} of {@link java.lang.String String} SOPInstanceUIDs to {@link com.pixelmed.dicom.HierarchicalSOPInstanceReference HierarchicalSOPInstanceReference}
	 */
	public static Map<String,HierarchicalSOPInstanceReference> findHierarchicalReferencesForSOPInstances(AttributeList list) {
		Map<String,HierarchicalSOPInstanceReference> map = new HashMap<String,HierarchicalSOPInstanceReference>();
		if (list != null) {
			Attribute aEvidence = list.get(TagFromName.CurrentRequestedProcedureEvidenceSequence);
			if (aEvidence != null && aEvidence instanceof SequenceAttribute) {
				SequenceAttribute sEvidence = (SequenceAttribute)aEvidence;
				Iterator studyIterator = sEvidence.iterator();
				while (studyIterator.hasNext()) {
					SequenceItem studyItem = (SequenceItem)(studyIterator.next());
					AttributeList studyList = studyItem.getAttributeList();
					if (studyList != null) {
						String studyInstanceUID = Attribute.getSingleStringValueOrNull(studyList,TagFromName.StudyInstanceUID);
						Attribute aReferencedSeriesSequence = studyList.get(TagFromName.ReferencedSeriesSequence);
						if (studyInstanceUID != null && studyInstanceUID.length() > 0 && aReferencedSeriesSequence != null && aReferencedSeriesSequence instanceof SequenceAttribute) {
							SequenceAttribute sReferencedSeriesSequence = (SequenceAttribute)aReferencedSeriesSequence;
							Iterator seriesIterator = sReferencedSeriesSequence.iterator();
							while (seriesIterator.hasNext()) {
								SequenceItem seriesItem = (SequenceItem)(seriesIterator.next());
								AttributeList seriesList = seriesItem.getAttributeList();
								if (seriesList != null) {
									String seriesInstanceUID = Attribute.getSingleStringValueOrNull(seriesList,TagFromName.SeriesInstanceUID);
									Attribute aReferencedSOPSequence = seriesList.get(TagFromName.ReferencedSOPSequence);
									if (seriesInstanceUID != null && seriesInstanceUID.length() > 0 && aReferencedSOPSequence != null && aReferencedSOPSequence instanceof SequenceAttribute) {
										SequenceAttribute sReferencedSOPSequence = (SequenceAttribute)aReferencedSOPSequence;
										Iterator instanceIterator = sReferencedSOPSequence.iterator();
										while (instanceIterator.hasNext()) {
											SequenceItem instanceItem = (SequenceItem)(instanceIterator.next());
											AttributeList instanceList = instanceItem.getAttributeList();
											String sopClassUID    = Attribute.getSingleStringValueOrNull(instanceList,TagFromName.ReferencedSOPClassUID);
											String sopInstanceUID = Attribute.getSingleStringValueOrNull(instanceList,TagFromName.ReferencedSOPInstanceUID);
											if (sopClassUID != null && sopClassUID.length() > 0 && sopInstanceUID != null && sopInstanceUID.length() > 0) {
												map.put(sopInstanceUID,new HierarchicalSOPInstanceReference(studyInstanceUID,seriesInstanceUID,sopInstanceUID,sopClassUID));
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return map;
	}
	
	public String toString() {
		StringBuffer str = new StringBuffer();
		str.append("Study: ");
		str.append(studyInstanceUID);
		str.append(", ");
		str.append("Series: ");
		str.append(seriesInstanceUID);
		str.append(", ");
		str.append("Instance: ");
		str.append(sopInstanceUID);
		str.append(", ");
		str.append("Class: ");
		str.append(sopClassUID);
		return str.toString();
	}

	
	/**
	 * <p>Dump the references in an SR file.</p>
	 *
	 * @param	arg
	 */
	public static void main(String arg[]) {
		try {
			AttributeList list = new AttributeList();
			list.read(arg[0]);
			Map<String,HierarchicalSOPInstanceReference> map = HierarchicalSOPInstanceReference.findHierarchicalReferencesForSOPInstances(list);
			Iterator<String> i = map.keySet().iterator();
			while (i.hasNext()) {
				String key = i.next();
				HierarchicalSOPInstanceReference ref = map.get(key);
				System.err.println(ref);
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
			System.exit(0);
		}
	}
}


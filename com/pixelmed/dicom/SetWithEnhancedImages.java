/* Copyright (c) 2001-2012, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import com.pixelmed.utils.FileUtilities;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>A class to create a set of instances, which when given unenhanced ("classic") images creates
 * enhanced IODs from them where possible, otherwise leaves them alone but includes them in the set.</p>
 *
 * @author	dclunie
 */
class SetWithEnhancedImages extends HashSet<FrameSet> {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/SetWithEnhancedImages.java,v 1.2 2012/03/27 01:51:43 dclunie Exp $";

	private SetOfFrameSets setOfFrameSets;
	
	private Set<String> setOfUnconvertedSOPInstanceUIDs;
	
	private Map<String,File> filesBySOPInstanceUID = new HashMap<String,File>();
	
	/**
	 * <p>Return a String representing this object's value.</p>
	 *
	 * @return	a string representation of the value of this object
	 */
	public String toString() {
		StringBuffer strbuf = new StringBuffer();
		return strbuf.toString();
	}

	static boolean containsAttributesForPixelMeasuresFunctionalGroup(Set<AttributeTag> attributeTags) {
		return attributeTags.contains(TagFromName.PixelSpacing) || attributeTags.contains(TagFromName.SliceThickness);
	}
	
	static void addPixelMeasuresFunctionalGroup(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done) {
		SequenceAttribute aPixelMeasuresSequence = new SequenceAttribute(TagFromName.PixelMeasuresSequence);
		targetList.put(aPixelMeasuresSequence);
		AttributeList itemList = new AttributeList();
		aPixelMeasuresSequence.addItem(itemList);
		{
			Attribute a = sourceList.get(TagFromName.PixelSpacing);
			if (a != null) {
				itemList.put(a);
				done.add(TagFromName.PixelSpacing);
			}
			// really shouldn't be null
		}
		{
			Attribute a = sourceList.get(TagFromName.SliceThickness);
			if (a != null) {
				itemList.put(a);
				done.add(TagFromName.SliceThickness);
			}
			// really shouldn't be null
		}
	}

	static boolean containsAttributesForPlanePositionFunctionalGroup(Set<AttributeTag> attributeTags) {
		return attributeTags.contains(TagFromName.ImagePositionPatient);
	}
	
	static void addPlanePositionFunctionalGroup(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done) {
		SequenceAttribute aPlanePositionSequence = new SequenceAttribute(TagFromName.PlanePositionSequence);
		targetList.put(aPlanePositionSequence);
		AttributeList itemList = new AttributeList();
		aPlanePositionSequence.addItem(itemList);
		{
			Attribute a = sourceList.get(TagFromName.ImagePositionPatient);
			if (a != null) {
				itemList.put(a);
				done.add(TagFromName.ImagePositionPatient);
			}
			// really shouldn't be null
		}
	}

	static boolean containsAttributesForPlaneOrientationFunctionalGroup(Set<AttributeTag> attributeTags) {
		return attributeTags.contains(TagFromName.ImageOrientationPatient);
	}
	
	static void addPlaneOrientationFunctionalGroup(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done) {
		SequenceAttribute aPlaneOrientationSequence = new SequenceAttribute(TagFromName.PlaneOrientationSequence);
		targetList.put(aPlaneOrientationSequence);
		AttributeList itemList = new AttributeList();
		aPlaneOrientationSequence.addItem(itemList);
		{
			Attribute a = sourceList.get(TagFromName.ImageOrientationPatient);
			if (a != null) {
				itemList.put(a);
				done.add(TagFromName.ImageOrientationPatient);
			}
			// really shouldn't be null
		}
	}

	static boolean containsAttributesForFrameVOILUTFunctionalGroup(Set<AttributeTag> attributeTags) {
		return attributeTags.contains(TagFromName.WindowWidth) || attributeTags.contains(TagFromName.WindowCenter) || attributeTags.contains(TagFromName.WindowCenterWidthExplanation);
	}
	
	static void addFrameVOILUTFunctionalGroup(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done) {
		SequenceAttribute aFrameVOILUTSequence = new SequenceAttribute(TagFromName.FrameVOILUTSequence);
		targetList.put(aFrameVOILUTSequence);
		AttributeList itemList = new AttributeList();
		aFrameVOILUTSequence.addItem(itemList);
		{
			Attribute a = sourceList.get(TagFromName.WindowWidth);
			if (a != null) {
				itemList.put(a);
				done.add(TagFromName.WindowWidth);
			}
			// really shouldn't be null
		}
		{
			Attribute a = sourceList.get(TagFromName.WindowCenter);
			if (a != null) {
				itemList.put(a);
				done.add(TagFromName.WindowCenter);
			}
			// really shouldn't be null
		}
		{
			Attribute a = sourceList.get(TagFromName.WindowCenterWidthExplanation);
			if (a != null) {
				itemList.put(a);
				done.add(TagFromName.WindowCenterWidthExplanation);
			}
		}
	}

	static boolean containsAttributesForPixelValueTransformationFunctionalGroup(Set<AttributeTag> attributeTags) {
		return attributeTags.contains(TagFromName.WindowWidth) || attributeTags.contains(TagFromName.WindowCenter) || attributeTags.contains(TagFromName.WindowCenterWidthExplanation);
	}
	
	static void addPixelValueTransformationFunctionalGroup(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done) {
		SequenceAttribute aPixelValueTransformationSequence = new SequenceAttribute(TagFromName.PixelValueTransformationSequence);
		targetList.put(aPixelValueTransformationSequence);
		AttributeList itemList = new AttributeList();
		aPixelValueTransformationSequence.addItem(itemList);
		{
			Attribute a = sourceList.get(TagFromName.RescaleIntercept);
			if (a != null) {
				itemList.put(a);
				done.add(TagFromName.RescaleIntercept);
			}
			// really shouldn't be null
		}
		{
			Attribute a = sourceList.get(TagFromName.RescaleSlope);
			if (a != null) {
				itemList.put(a);
				done.add(TagFromName.RescaleSlope);
			}
			// really shouldn't be null
		}
		{
			Attribute a = sourceList.get(TagFromName.RescaleType);
			if (a != null) {
				itemList.put(a);
				done.add(TagFromName.RescaleType);
			}
			// else should probably use default of HU for CT unless LOCALIZER :(
		}
	}

	static boolean containsAttributesForRealWorldValueMappingFunctionalGroup(Set<AttributeTag> attributeTags) {
		return attributeTags.contains(TagFromName.RescaleIntercept) || attributeTags.contains(TagFromName.RescaleSlope) || attributeTags.contains(TagFromName.RescaleType);
	}
	
	static void addRealWorldValueMappingFunctionalGroup(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done) throws DicomException {
		SequenceAttribute aRealWorldValueMappingSequence = new SequenceAttribute(TagFromName.RealWorldValueMappingSequence);
		targetList.put(aRealWorldValueMappingSequence);
		AttributeList itemList = new AttributeList();
		aRealWorldValueMappingSequence.addItem(itemList);

		// really should try and add RealWorldValueFirstValueMapped and RealWorldValueLastValueMapped :(
		{
			double value = Attribute.getSingleDoubleValueOrDefault(sourceList,TagFromName.RescaleIntercept,0);
			{ Attribute a = new FloatDoubleAttribute(TagFromName.RealWorldValueIntercept); a.addValue(value); itemList.put(a); }
			done.add(TagFromName.WindowWidth);
		}
		{
			double value = Attribute.getSingleDoubleValueOrDefault(sourceList,TagFromName.RescaleSlope,1);
			{ Attribute a = new FloatDoubleAttribute(TagFromName.RealWorldValueSlope); a.addValue(value); itemList.put(a); }
			done.add(TagFromName.WindowWidth);
		}
		{
			String value = Attribute.getSingleStringValueOrEmptyString(sourceList,TagFromName.RescaleType);
			if (value.length() > 0) {
				{ Attribute a = new ShortStringAttribute(TagFromName.LUTLabel); a.addValue(value); itemList.put(a); }
				done.add(TagFromName.RescaleType);
			}
		}
		// really should try and add MeasurementUnitsCodeSequence for HU except for localizer :(
	}



	private byte[]  bytePixelsForAllFrames;
	private short[] wordPixelsForAllFrames;
	private int currentFrameOffset;
	private int numberOfPixelValuesPerFrame;
	private int numberOfPixelValuesTotal;
	
	private void initializePixelDataCache(int rows,int columns,int samplesPerPixel,int numberOfFrames) {
		numberOfPixelValuesPerFrame = samplesPerPixel*rows*columns;					// consider use of long not int, esp. if use file rt. array (latter requires int in constructor) :(
		numberOfPixelValuesTotal = numberOfPixelValuesPerFrame * numberOfFrames;	// consider use of long not int, esp. if use file rt. array (latter requires int in constructor) :(
		currentFrameOffset = 0;
		bytePixelsForAllFrames = null;	// will instantiate only one of these lazily depending on encountered VR
		wordPixelsForAllFrames = null;
	}
	
	private void addFrameToPixelDataCache(Attribute pixelData) throws DicomException, IOException {
		if (pixelData != null) {
			byte[] vr = pixelData.getVR();
			if (ValueRepresentation.isOtherByteVR(vr)) {
				if (wordPixelsForAllFrames != null) {
					throw new DicomException("Cannot mix OB and OW Pixel Data VR from different frames"+pixelData.getVRAsString());
				}
				if (bytePixelsForAllFrames == null) {
					bytePixelsForAllFrames =  new byte[numberOfPixelValuesTotal];
				}
				byte[] pixels = pixelData.getByteValues();
				System.arraycopy(pixels,0,bytePixelsForAllFrames,currentFrameOffset,pixels.length);
//System.err.println("SetWithEnhancedImages.addFrameToPixelDataCache(): byte[] pixels.length = "+pixels.length);
//System.err.println("SetWithEnhancedImages.addFrameToPixelDataCache(): numberOfPixelValuesPerFrame = "+numberOfPixelValuesPerFrame);
//System.err.println("SetWithEnhancedImages.addFrameToPixelDataCache(): byte[] bytePixelsForAllFrames.length = "+bytePixelsForAllFrames.length);
//System.err.println("SetWithEnhancedImages.addFrameToPixelDataCache(): numberOfPixelValuesTotal = "+numberOfPixelValuesTotal);
//System.err.println("SetWithEnhancedImages.addFrameToPixelDataCache(): currentFrameOffset = "+currentFrameOffset);
				currentFrameOffset += numberOfPixelValuesPerFrame;
			}
			else if (ValueRepresentation.isOtherWordVR(vr)) {
				if (bytePixelsForAllFrames != null) {
					throw new DicomException("Cannot mix OB and OW Pixel Data VR from different frames"+pixelData.getVRAsString());
				}
				if (wordPixelsForAllFrames == null) {
					wordPixelsForAllFrames =  new short[numberOfPixelValuesTotal];
				}
				short[] pixels = pixelData.getShortValues();
//System.err.println("SetWithEnhancedImages.addFrameToPixelDataCache(): short[] pixels.length = "+pixels.length);
//System.err.println("SetWithEnhancedImages.addFrameToPixelDataCache(): numberOfPixelValuesPerFrame = "+numberOfPixelValuesPerFrame);
//System.err.println("SetWithEnhancedImages.addFrameToPixelDataCache(): short[] wordPixelsForAllFrames.length = "+wordPixelsForAllFrames.length);
//System.err.println("SetWithEnhancedImages.addFrameToPixelDataCache(): numberOfPixelValuesTotal = "+numberOfPixelValuesTotal);
//System.err.println("SetWithEnhancedImages.addFrameToPixelDataCache(): currentFrameOffset = "+currentFrameOffset);
				System.arraycopy(pixels,0,wordPixelsForAllFrames,currentFrameOffset,pixels.length);
				currentFrameOffset += numberOfPixelValuesPerFrame;
			}
			else {
				throw new DicomException("Incorrect Pixel Data VR "+pixelData.getVRAsString());
			}
		}
		else {
			throw new DicomException("Missing Pixel Data");
		}
	}
	
	// only need to list things that are in FrameSet.distinguishingAttributeTags here ...
	private static Set<AttributeTag> excludeFromCopyingIntoTopLevel = new HashSet<AttributeTag>();
	{
		excludeFromCopyingIntoTopLevel.add(TagFromName.ImageOrientationPatient);
		excludeFromCopyingIntoTopLevel.add(TagFromName.PixelSpacing);
		excludeFromCopyingIntoTopLevel.add(TagFromName.SliceThickness);
	}
	
	private static Set<AttributeTag> excludeFromCopyingIntoFunctionalGroups = new HashSet<AttributeTag>();
	{
		excludeFromCopyingIntoFunctionalGroups.add(TagFromName.SpecificCharacterSet);
	}
	
	

	/**
	 * <p>Create an enhanced image from a set of DICOM single image files in a FrameSet.</p>
	 *
	 * @param		frameSet				a set of files to convert that have already been determined to be a FrameSet
	 * @param		outputPath				a folder in which to store converted files (which must already exist)
	 * @param		filesBySOPInstanceUID	a map of SOP Instance UIDs to the files that contain them
	 * @exception	DicomException			if an input file cannot be found for a frame, or it cannot be parsed
	 * @exception	IOException				if an input file cannot be read
	 */
	File createEnhancedImageFromFrameSet(FrameSet frameSet,File outputFolder,Map<String,File> filesBySOPInstanceUID) throws DicomException, IOException {
		Set<AttributeTag> distinguishingAttributeTags = frameSet.getDistinguishingAttributeTags();
		Set<AttributeTag> sharedAttributeTags = frameSet.getSharedAttributeTags();
		Set<AttributeTag> perFrameAttributeTags = frameSet.getPerFrameAttributeTags();
		
		AttributeList convertedList = new AttributeList();
		SequenceAttribute aSharedFunctionalGroupsSequence = new SequenceAttribute(TagFromName.SharedFunctionalGroupsSequence);
		convertedList.put(aSharedFunctionalGroupsSequence);
		AttributeList sharedFunctionalGroupsSequenceItemList = new AttributeList();
		aSharedFunctionalGroupsSequence.addItem(sharedFunctionalGroupsSequenceItemList);
		
		SequenceAttribute aPerFrameFunctionalGroupsSequence = new SequenceAttribute(TagFromName.PerFrameFunctionalGroupsSequence);
		convertedList.put(aPerFrameFunctionalGroupsSequence);
				
		List<String> sopInstanceUIDs = frameSet.getSOPInstanceUIDsSortedByFrameOrder();
		boolean firstInstance = true;
		for (String sopInstanceUID : sopInstanceUIDs) {
			File f = filesBySOPInstanceUID.get(sopInstanceUID);
			if (f != null) {
				AttributeList frameSourceList = new AttributeList();
				frameSourceList.read(f);	// do NOT stop at PixelData this time, since we need it
				if (firstInstance) {
					int rows = Attribute.getSingleIntegerValueOrDefault(frameSourceList,TagFromName.Rows,0);
					int columns = Attribute.getSingleIntegerValueOrDefault(frameSourceList,TagFromName.Columns,0);
					int samplesPerPixel = Attribute.getSingleIntegerValueOrDefault(frameSourceList,TagFromName.SamplesPerPixel,1);
					
					int numberOfFrames = frameSet.size();
					{ Attribute a = new IntegerStringAttribute(TagFromName.NumberOfFrames); a.addValue(numberOfFrames); convertedList.put(a); }
					
					initializePixelDataCache(rows,columns,samplesPerPixel,numberOfFrames);
					
					// copy top level dataset shared stuff ... most of this is covered by distinguishingAttributeTags (including Image Pixel Module stuff) and CompositeInstanceContext
					for (AttributeTag t : distinguishingAttributeTags) {
						if (!excludeFromCopyingIntoTopLevel.contains(t)) {
							Attribute a = frameSourceList.get(t);
							if (a != null) {
								convertedList.put(a);
							}
							// really shouldn't be null
						}
					}
					
					{
						// should we bother to check that these are actually all "shared" ? :(
						CompositeInstanceContext cic = new CompositeInstanceContext(frameSourceList);
						convertedList.putAll(cic.getAttributeList());
					}
					
					// may want to add any other "shared" stuff that needs to go into top level dataset rather than functional groups (or be ignored)

					Set<AttributeTag> doneSharedSet = new HashSet<AttributeTag>();
					
					// need to be careful here with functional groups that may contain multiple attributes,
					// some of which may be shared and others per-frame, so check both not per-frame and shared (and distinguished too) ...

					if (!containsAttributesForPixelMeasuresFunctionalGroup(perFrameAttributeTags)
					 && (containsAttributesForPixelMeasuresFunctionalGroup(sharedAttributeTags) || containsAttributesForPixelMeasuresFunctionalGroup(distinguishingAttributeTags))
					) {
						addPixelMeasuresFunctionalGroup(sharedFunctionalGroupsSequenceItemList,frameSourceList,doneSharedSet);
					}
				
					if (!containsAttributesForPlanePositionFunctionalGroup(perFrameAttributeTags)
					 && (containsAttributesForPlanePositionFunctionalGroup(sharedAttributeTags) || containsAttributesForPlanePositionFunctionalGroup(distinguishingAttributeTags))
					) {
						addPlanePositionFunctionalGroup(sharedFunctionalGroupsSequenceItemList,frameSourceList,doneSharedSet);
					}
					
					if (!containsAttributesForPlaneOrientationFunctionalGroup(perFrameAttributeTags)
					 && (containsAttributesForPlaneOrientationFunctionalGroup(sharedAttributeTags) || containsAttributesForPlaneOrientationFunctionalGroup(distinguishingAttributeTags))
					) {
						addPlaneOrientationFunctionalGroup(sharedFunctionalGroupsSequenceItemList,frameSourceList,doneSharedSet);
					}
					
					if (!containsAttributesForFrameVOILUTFunctionalGroup(perFrameAttributeTags)
					 && (containsAttributesForFrameVOILUTFunctionalGroup(sharedAttributeTags) || containsAttributesForFrameVOILUTFunctionalGroup(distinguishingAttributeTags))
					) {
						addFrameVOILUTFunctionalGroup(sharedFunctionalGroupsSequenceItemList,frameSourceList,doneSharedSet);
					}
					
					if (!containsAttributesForRealWorldValueMappingFunctionalGroup(perFrameAttributeTags)
					 && (containsAttributesForRealWorldValueMappingFunctionalGroup(sharedAttributeTags) || containsAttributesForRealWorldValueMappingFunctionalGroup(distinguishingAttributeTags))
					) {
						addRealWorldValueMappingFunctionalGroup(sharedFunctionalGroupsSequenceItemList,frameSourceList,doneSharedSet);
					}
															
					if (!containsAttributesForPixelValueTransformationFunctionalGroup(perFrameAttributeTags)
					 && (containsAttributesForPixelValueTransformationFunctionalGroup(sharedAttributeTags) || containsAttributesForPixelValueTransformationFunctionalGroup(distinguishingAttributeTags))
					) {
						addPixelValueTransformationFunctionalGroup(sharedFunctionalGroupsSequenceItemList,frameSourceList,doneSharedSet);
					}
															
					// now go through shared list, and anything that is not in the convertedList already, should be added to the Unassigned Shared Converted Attributes Sequence
					
					{
						{ Attribute a = new LongStringAttribute(new AttributeTag(0x0021,0x0010)); a.addValue("PixelMed Publishing"); sharedFunctionalGroupsSequenceItemList.put(a); }
						SequenceAttribute aUnassignedSharedConvertedAttributesSequence = new SequenceAttribute(new AttributeTag(0x0021,0x1001));
						sharedFunctionalGroupsSequenceItemList.put(aUnassignedSharedConvertedAttributesSequence);
						AttributeList unassignedList =  new AttributeList();
						aUnassignedSharedConvertedAttributesSequence.addItem(unassignedList);

						for (AttributeTag t : sharedAttributeTags) {
							if (convertedList.get(t) == null && !doneSharedSet.contains(t) && !excludeFromCopyingIntoFunctionalGroups.contains(t)) {		// i.e., not already copied into top level data set or a specific shared functional group
								Attribute a = frameSourceList.get(t);
								if (a != null) {
									unassignedList.put(a);
								}
								// really shouldn't be null
							}
						}
					}
				}
				// else do not need to repeat distinguishing and shared, since FrameSet already guarantees thay are the same values
				
				AttributeList perFrameFunctionalGroupsSequenceItemList = new AttributeList();
				aPerFrameFunctionalGroupsSequence.addItem(perFrameFunctionalGroupsSequenceItemList);
				
				Set<AttributeTag> donePerFrameSet = new HashSet<AttributeTag>();
								
				if (containsAttributesForPixelMeasuresFunctionalGroup(perFrameAttributeTags)) {
					addPixelMeasuresFunctionalGroup(perFrameFunctionalGroupsSequenceItemList,frameSourceList,donePerFrameSet);
				}
				
				if (containsAttributesForPlanePositionFunctionalGroup(perFrameAttributeTags)) {
					addPlanePositionFunctionalGroup(perFrameFunctionalGroupsSequenceItemList,frameSourceList,donePerFrameSet);
				}
				
				if (containsAttributesForPlaneOrientationFunctionalGroup(perFrameAttributeTags)) {
					addPlaneOrientationFunctionalGroup(perFrameFunctionalGroupsSequenceItemList,frameSourceList,donePerFrameSet);
				}
				
				if (containsAttributesForFrameVOILUTFunctionalGroup(perFrameAttributeTags)) {
					addFrameVOILUTFunctionalGroup(perFrameFunctionalGroupsSequenceItemList,frameSourceList,donePerFrameSet);
				}
				
				if (containsAttributesForRealWorldValueMappingFunctionalGroup(perFrameAttributeTags)) {
					addRealWorldValueMappingFunctionalGroup(perFrameFunctionalGroupsSequenceItemList,frameSourceList,donePerFrameSet);
				}
				
				if (containsAttributesForPixelValueTransformationFunctionalGroup(perFrameAttributeTags)) {
					addPixelValueTransformationFunctionalGroup(perFrameFunctionalGroupsSequenceItemList,frameSourceList,donePerFrameSet);
				}
				
				{
					{ Attribute a = new LongStringAttribute(new AttributeTag(0x0021,0x0010)); a.addValue("PixelMed Publishing"); perFrameFunctionalGroupsSequenceItemList.put(a); }
					SequenceAttribute aUnassignedPerFrameConvertedAttributesSequence = new SequenceAttribute(new AttributeTag(0x0021,0x1002));
					perFrameFunctionalGroupsSequenceItemList.put(aUnassignedPerFrameConvertedAttributesSequence);
					AttributeList unassignedList =  new AttributeList();
					aUnassignedPerFrameConvertedAttributesSequence.addItem(unassignedList);

					for (AttributeTag t : perFrameAttributeTags) {
						if (!donePerFrameSet.contains(t) && !excludeFromCopyingIntoFunctionalGroups.contains(t)) {
							Attribute a = frameSourceList.get(t);
							if (a != null) {
								unassignedList.put(a);
							}
							// may be null if not in every frame
						}
					}
				}
			
				// include reference to source UID
				
				addFrameToPixelDataCache(frameSourceList.get(TagFromName.PixelData));
			}
			else {
				throw new DicomException("Missing file for SOP Instance UID "+sopInstanceUID);
			}
			firstInstance = false;
		}
		
		// change to appropriate SOP Class
		// create new Series and SOP Instance UIDs
		
		// add pixel data ...
		if (bytePixelsForAllFrames != null) {
			OtherByteAttribute aPixelData  = new OtherByteAttribute(TagFromName.PixelData);
			aPixelData.setValues(bytePixelsForAllFrames);
			convertedList.put(aPixelData);
		}
		else if (wordPixelsForAllFrames != null) {
			OtherWordAttribute aPixelData  = new OtherWordAttribute(TagFromName.PixelData);
			aPixelData.setValues(wordPixelsForAllFrames);
			convertedList.put(aPixelData);
		}
		
		{

			String studyID = Attribute.getSingleStringValueOrEmptyString(convertedList,TagFromName.StudyID);
			
			String seriesNumber = "7291";
			{ Attribute a = new IntegerStringAttribute(TagFromName.SeriesNumber); a.addValue(seriesNumber); convertedList.put(a); }

			String instanceNumber = "1";
			{ Attribute a = new IntegerStringAttribute(TagFromName.InstanceNumber); a.addValue(instanceNumber); convertedList.put(a); }
			
			UIDGenerator u = new UIDGenerator();	

			{ Attribute a = new UniqueIdentifierAttribute(TagFromName.SOPInstanceUID); a.addValue(u.getNewSOPInstanceUID(studyID,seriesNumber,instanceNumber)); convertedList.put(a); }
			{ Attribute a = new UniqueIdentifierAttribute(TagFromName.SeriesInstanceUID); a.addValue(u.getNewSeriesInstanceUID(studyID,seriesNumber)); convertedList.put(a); }
			
			{ Attribute a = new UniqueIdentifierAttribute(TagFromName.SOPClassUID); a.addValue(SOPClass.EnhancedCTImageStorage); convertedList.put(a); }
		}
		
		convertedList.insertSuitableSpecificCharacterSetForAllStringValues();
		
        FileMetaInformation.addFileMetaInformation(convertedList,TransferSyntax.ExplicitVRLittleEndian,"OURAETITLE");
		
		System.err.println("SetWithEnhancedImages.createEnhancedImageFromFrameSet(): Result");
		System.err.println(convertedList.toString());
		
		File convertedFile = new File(outputFolder,Attribute.getSingleStringValueOrDefault(convertedList,TagFromName.SOPInstanceUID,"NONAME"));
		convertedList.write(convertedFile);
		return convertedFile;
	}

	/**
	 * <p>Create a new set of instances, converting to enhanced images when possible, from a set of DICOM files.</p>
	 *
	 * <p>Non-DICOM files and problems parsing files are ignored, rather than causing failure</p>
	 *
	 * @param		inputPaths		a set of files to convert
	 * @param		outputPath		a folder in which to store converted files (which must already exist)
	 * @exception	DicomException	if folder in which to store converted files does not exist
	 * @exception	IOException		if an input file cannot be read
	 */
	private void doCommonConstructorStuff(Set<File> files,File outputFolder) throws DicomException, IOException {
		if (!outputFolder.isDirectory()) {
			throw new DicomException("Output folder "+outputFolder+" does not exist");
		}
		// Pass 1 ... build a set of frame sets for unenhanced ("classic") images, and a list of those not included
		setOfFrameSets = new SetOfFrameSets();
		setOfUnconvertedSOPInstanceUIDs = new HashSet<String>();
		for (File f : files) {
			try {
				if (DicomFileUtilities.isDicomOrAcrNemaFile(f)) {
//System.err.println("SetWithEnhancedImages.doCommonConstructorStuff(): Doing "+f);
					AttributeList list = new AttributeList();
					list.read(f,TagFromName.PixelData);
					String sopInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPInstanceUID);
					String sopClassUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPClassUID);
					if (sopInstanceUID.length() > 0 && sopClassUID.length() > 0) {
						filesBySOPInstanceUID.put(sopInstanceUID,f);
						if (SOPClass.isImageStorage(sopClassUID) && !list.isEnhanced()) {		// 	do not use list.isImage() since it will fail, because we deliberately did NOT read the PixelData attribute to save time this pass
							setOfFrameSets.insertIntoFrameSets(list);
						}
						else {
System.err.println("SetWithEnhancedImages.doCommonConstructorStuff(): Doing nothing to non-image or already enhanced "+f);
							setOfUnconvertedSOPInstanceUIDs.add(sopInstanceUID);
						}
					}
					else {
						throw new DicomException("Missing SOP Instance or Class UID in file "+f);
					}
				}
			}
			catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
System.err.println("SetWithEnhancedImages.doCommonConstructorStuff(): FrameSets Result");
		System.err.println(setOfFrameSets.toString());

		// Pass 2 ... convert frame sets into enhanced images
		for (FrameSet frameSet : setOfFrameSets) {
			File enhancedImage = createEnhancedImageFromFrameSet(frameSet,outputFolder,filesBySOPInstanceUID);
		}
	}
	
	/**
	 * <p>Create a new set of instances, converting to enhanced images when possible, from a set of DICOM files.</p>
	 *
	 * <p>Non-DICOM files and problems parsing files are ignored, rather than causing failure</p>
	 *
	 * @param		inputPaths		a set of files to convert
	 * @param		outputPath		a folder in which to store converted files (which must already exist)
	 * @exception	DicomException	if folder in which to store converted files does not exist
	 * @exception	IOException		if an input file cannot be read
	 */
	SetWithEnhancedImages(Set<File> inputFiles,File outputFolder) throws DicomException, IOException {
		doCommonConstructorStuff(inputFiles,outputFolder);
	}
	
	/**
	 * <p>Create a new set of instances, converting to enhanced images when possible, from a set of DICOM files.</p>
	 *
	 * <p>Non-DICOM files and problems parsing files are ignored, rather than causing failure</p>
	 *
	 * @param		inputPaths		a set of paths of filenames and/or folder names of files containing the images to convert
	 * @param		outputPath		a path in which to store converted files (which must already exist)
	 * @exception	DicomException	if folder in which to store converted files does not exist
	 * @exception	IOException		if an input file cannot be read
	 */
	SetWithEnhancedImages(String inputPaths[],String outputPath) throws DicomException, IOException {
		Set<File> inputFiles = new HashSet<File>();
		for (String p : inputPaths) {
			Collection<File> more = FileUtilities.listFilesRecursively(new File(p));
			inputFiles.addAll(more);
		}
		doCommonConstructorStuff(inputFiles,new File(outputPath));
	}

	/**
	 * <p>For testing, read all DICOM files and convert them to enhanced images when possible.</p>
	 *
	 * @param	arg	the filenames and/or folder names of files containing the images to partition, followed by the path in which to store the converted instances
	 */
	public static void main(String arg[]) {
		try {
			if (arg.length >= 2) {
				int inputCount = arg.length - 1;
				String outputPath = arg[inputCount];
				String[] inputPaths = new String[inputCount];
				System.arraycopy(arg,0,inputPaths,0,inputCount);
				SetWithEnhancedImages SetWithEnhancedImages = new SetWithEnhancedImages(inputPaths,outputPath);
System.err.println("SetWithEnhancedImages.main(): Result");
				System.err.println(SetWithEnhancedImages.toString());
			}
			else {
				System.err.println("Error: Incorrect number of arguments");
				System.err.println("Usage: SetWithEnhancedImages inputPaths outputPath");
				System.exit(1);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}


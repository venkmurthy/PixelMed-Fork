/* Copyright (c) 2001-2012, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.util.*;
import com.pixelmed.utils.FloatFormatter;

/**
 * <p>A transformation constructed from a DICOM attribute list that extracts
 * those attributes which describe how stored pixel values are translated
 * into real world values (e.g., Hounsfield Units, cm/s).</p>
 *
 * <p>Looks first for a per-frame functional group RealWorldValueMappingSequence
 * then looks in the shared functional groups, otherwise tries to find the
 * Rescale Slope and Intercept values in the top level of the dataset.</p>
 *
 * <p>Note that multiple transformations (for each frame) may be present and are
 * supported.</p>
 *
 * <p>Does not currently support a LUT in the Modality LUT Sequence, only linear
 * rescale values.</p>
 *
 * @author	dclunie
 */
public class RealWorldValueTransform {

	/***/
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/RealWorldValueTransform.java,v 1.12 2012/02/01 23:16:13 dclunie Exp $";
	
	/***/
	private class SingleRealWorldValueTransform {
	
		/***/
		int[] rangeOfValues;	// null if range not constrained
		/***/
		double slope;
		/***/
		double intercept;
		/***/
		String units;
		
		/**
		 * @param	rwvmlist	RealWorldValueMappingSequence item attributes
		 */
		SingleRealWorldValueTransform(AttributeList rwvmlist) {
			if (rwvmlist != null) {
				rangeOfValues=new int[2];
				rangeOfValues[0] = Attribute.getSingleIntegerValueOrDefault(rwvmlist,TagFromName.RealWorldValueFirstValueMapped,0);
				rangeOfValues[1] = Attribute.getSingleIntegerValueOrDefault(rwvmlist,TagFromName.RealWorldValueLastValueMapped,0);
					   slope = Attribute.getSingleDoubleValueOrDefault (rwvmlist,TagFromName.RealWorldValueSlope,0.0);
				       intercept = Attribute.getSingleDoubleValueOrDefault (rwvmlist,TagFromName.RealWorldValueIntercept,0.0);
				           units = SequenceAttribute.getMeaningOfCodedSequenceAttributeOrDefault(rwvmlist,TagFromName.MeasurementUnitsCodeSequence,"");
//System.err.println("SingleRealWorldValueTransform: adding "+rangeOfValues[0]+","+rangeOfValues[1]+","+slope+","+intercept+" "+units);
			}
		}
	
		/**
		 * @param	ptflist		PixelValueTransformationSequence item attributes
		 * @param	vSOPClassUID	to help choose default rescale units if not specified
		 */
		SingleRealWorldValueTransform(AttributeList ptflist,String vSOPClassUID) {
			if (ptflist != null) {
				rangeOfValues=null;
				slope = Attribute.getSingleDoubleValueOrDefault(ptflist,TagFromName.RescaleSlope,1.0);
				intercept = Attribute.getSingleDoubleValueOrDefault(ptflist,TagFromName.RescaleIntercept,0.0);
					     
				String useUnits="??";
				String vRescaleType = Attribute.getSingleStringValueOrNull(ptflist,TagFromName.RescaleType);
				String vUnits = Attribute.getSingleStringValueOrNull(ptflist,TagFromName.Units);
					
				// Ignore Rescale Type even if present when is PET and Units is present - fix [bugs.mrmf] (000223) PET images Units overridden by Rescale Type
				if (vSOPClassUID.equals(SOPClass.PETImageStorage) && vUnits != null && vUnits.length() > 0) {
					useUnits=vUnits;
				}
				else if (vRescaleType != null && vRescaleType.length() > 0) {
					useUnits=vRescaleType;
				}
				else if (vSOPClassUID.equals(SOPClass.CTImageStorage)) {
					useUnits="HU";
				}
				units = useUnits;
//System.err.println("SingleRealWorldValueTransform: adding "+rangeOfValues[0]+","+rangeOfValues[1]+","+slope+","+intercept+" "+units);
			}
		}
		
		boolean isIdentityAndUnitsUnspecified() {
			return slope == 1 && intercept == 0 && (units == null || units.equals("US") || units.equals("??"));
		}
	}
	
	/***/
	private class SingleRealWorldValueTransforms extends ArrayList {
	}
	
	/***/
	private SingleRealWorldValueTransforms[] arrayOfTransforms;	// null if not varying per-frame, if not null will have size == number of frames
	/***/
	private SingleRealWorldValueTransforms commonTransforms;	// in which case this will be used
	
	/**
	 * @param	list	the dataset of an image object to be searched for transformations
	 */
	public RealWorldValueTransform(AttributeList list) {
//System.err.println("RealWorldValueTransform:");
		arrayOfTransforms=null;
		commonTransforms=null;
		String vSOPClassUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPClassUID);
		/*try*/ {
			SequenceAttribute aPerFrameFunctionalGroupsSequence = (SequenceAttribute)list.get(TagFromName.PerFrameFunctionalGroupsSequence);
			if (aPerFrameFunctionalGroupsSequence != null) {
//System.err.println("RealWorldValueTransform: checking PerFrameFunctionalGroupsSequence");
				int nFrames = aPerFrameFunctionalGroupsSequence.getNumberOfItems();
				int frameNumber = 0;
				Iterator pfitems = aPerFrameFunctionalGroupsSequence.iterator();
				while (pfitems.hasNext()) {
					SequenceItem fitem = (SequenceItem)pfitems.next();
					AttributeList flist = fitem.getAttributeList();
					if (flist != null) {
						SequenceAttribute aRealWorldValueMappingSequence = (SequenceAttribute)flist.get(TagFromName.RealWorldValueMappingSequence);
						if (aRealWorldValueMappingSequence != null && aRealWorldValueMappingSequence.getNumberOfItems() >= 1) {
//System.err.println("RealWorldValueTransform: found RealWorldValueMappingSequence");
							if (arrayOfTransforms == null) arrayOfTransforms = new SingleRealWorldValueTransforms[nFrames];
							if (arrayOfTransforms[frameNumber] == null) arrayOfTransforms[frameNumber] = new SingleRealWorldValueTransforms();
							Iterator rwvmitems = aRealWorldValueMappingSequence.iterator();
							while (rwvmitems.hasNext()) {
								SequenceItem rwvmitem = (SequenceItem)rwvmitems.next();
								AttributeList rwvmlist = rwvmitem.getAttributeList();
								arrayOfTransforms[frameNumber].add(new SingleRealWorldValueTransform(rwvmlist));
							}
						}
						SequenceAttribute aPixelValueTransformationSequence = (SequenceAttribute)flist.get(TagFromName.PixelValueTransformationSequence);
						if (aPixelValueTransformationSequence != null && aPixelValueTransformationSequence.getNumberOfItems() >= 1) {
//System.err.println("RealWorldValueTransform: found PixelValueTransformationSequence");
							if (arrayOfTransforms == null) arrayOfTransforms = new SingleRealWorldValueTransforms[nFrames];
							if (arrayOfTransforms[frameNumber] == null) arrayOfTransforms[frameNumber] = new SingleRealWorldValueTransforms();
							Iterator ptfitems = aPixelValueTransformationSequence.iterator();
							while (ptfitems.hasNext()) {
								SequenceItem ptfitem = (SequenceItem)ptfitems.next();
								AttributeList ptflist = ptfitem.getAttributeList();
								arrayOfTransforms[frameNumber].add(new SingleRealWorldValueTransform(ptflist,vSOPClassUID));
							}
						}
					}
					++frameNumber;
				}
			}
			
			if (arrayOfTransforms == null) {
//System.err.println("RealWorldValueTransform: checking SharedFunctionalGroupsSequence");
				SequenceAttribute aSharedFunctionalGroupsSequence = (SequenceAttribute)list.get(TagFromName.SharedFunctionalGroupsSequence);
				if (aSharedFunctionalGroupsSequence != null) {
					// assert aSharedFunctionalGroupsSequence.getNumberOfItems() == 1
					Iterator sitems = aSharedFunctionalGroupsSequence.iterator();
					if (sitems.hasNext()) {
						SequenceItem sitem = (SequenceItem)sitems.next();
						AttributeList slist = sitem.getAttributeList();
						if (slist != null) {
							SequenceAttribute aRealWorldValueMappingSequence = (SequenceAttribute)slist.get(TagFromName.RealWorldValueMappingSequence);
							if (aRealWorldValueMappingSequence != null && aRealWorldValueMappingSequence.getNumberOfItems() >= 1) {
//System.err.println("RealWorldValueTransform: found RealWorldValueMappingSequence");
								if (commonTransforms == null) commonTransforms = new SingleRealWorldValueTransforms();
								Iterator rwvmitems = aRealWorldValueMappingSequence.iterator();
								while (rwvmitems.hasNext()) {
									SequenceItem rwvmitem = (SequenceItem)rwvmitems.next();
									AttributeList rwvmlist = rwvmitem.getAttributeList();
									commonTransforms.add(new SingleRealWorldValueTransform(rwvmlist));
								}
							}
							SequenceAttribute aPixelValueTransformationSequence = (SequenceAttribute)slist.get(TagFromName.PixelValueTransformationSequence);
							if (aPixelValueTransformationSequence != null && aPixelValueTransformationSequence.getNumberOfItems() >= 1) {
//System.err.println("RealWorldValueTransform: found PixelValueTransformationSequence");
								if (commonTransforms == null) commonTransforms = new SingleRealWorldValueTransforms();
								Iterator ptfitems = aPixelValueTransformationSequence.iterator();
								while (ptfitems.hasNext()) {
									SequenceItem ptfitem = (SequenceItem)ptfitems.next();
									AttributeList ptflist = ptfitem.getAttributeList();
									SingleRealWorldValueTransform transform = new SingleRealWorldValueTransform(ptflist,vSOPClassUID);
									if (!transform.isIdentityAndUnitsUnspecified()) {
										commonTransforms.add(transform);
									}
								}
							}
						}
					}
				}

				// check for "old-fashioned" Modality LUT style attributes
			
				{
					Attribute aRescaleSlope = list.get(TagFromName.RescaleSlope);
					Attribute aRescaleIntercept = list.get(TagFromName.RescaleIntercept);
				
					if (aRescaleSlope != null && aRescaleIntercept != null) {
						if (commonTransforms == null) commonTransforms = new SingleRealWorldValueTransforms();
						SingleRealWorldValueTransform transform = new SingleRealWorldValueTransform(list,vSOPClassUID);
						if (!transform.isIdentityAndUnitsUnspecified()) {
							commonTransforms.add(transform);
						}
					}
				}
			}
			
		}
		//catch (DicomException e) {
		//	e.printStackTrace(System.err);
		//}
	}

        private final int precisionToDisplayDouble = 4;
        private final int maximumIntegerDigits = 8;
        private final int maximumMaximumFractionDigits = 6;

	/**
	 * Given a stored pixel value, return a string containing a description of all
	 * known real world values that can be derived from it.
	 *
	 * @param	frame		numbered from zero; needed to select which transform if frame-specific
	 * @param	storedValue	the actual stored pixel value to look up
	 */
	public String toString(int frame,int storedValue) {
		StringBuffer sbuf = new StringBuffer();
		SingleRealWorldValueTransforms useTransform = (arrayOfTransforms == null) ? commonTransforms : arrayOfTransforms[frame];
		if (useTransform != null) {
			Iterator i = useTransform.iterator();
			while (i.hasNext()) {
				SingleRealWorldValueTransform t = (SingleRealWorldValueTransform)i.next();
				if (t.rangeOfValues == null || (t.rangeOfValues[0] <= storedValue && storedValue <= t.rangeOfValues[1])) {
					if (sbuf.length() > 0) sbuf.append(", ");
					double value=((double)storedValue)*t.slope+t.intercept;
					sbuf.append(FloatFormatter.toString(value,Locale.US));
					sbuf.append(" ");
					sbuf.append(t.units);
				}
			}
		}
		return sbuf.toString();
	}
}


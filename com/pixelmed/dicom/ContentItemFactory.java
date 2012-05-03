/* Copyright (c) 2001-2012, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import com.pixelmed.utils.FloatFormatter;

import java.util.Iterator;
import java.util.Locale;

/**
 * <p>A class with methods for constructing a {@link com.pixelmed.dicom.ContentItem ContentItem} of the appropriate class from a list of attributes.</p>
 *
 * <p>The sub-classes of {@link com.pixelmed.dicom.ContentItem ContentItem} are public internal classes of this class,
 * but specialize the methods, specifically the extractors and the string representation methods.</p>
 *
 * <p>This is not an abstract class, and the content item factory method is not static; an instance of
 * the factory needs to be created.</p>
 *
 * @see com.pixelmed.dicom.ContentItem
 * @see com.pixelmed.dicom.StructuredReport
 * @see com.pixelmed.dicom.StructuredReportBrowser
 *
 * @author	dclunie
 */
public class ContentItemFactory {

	/***/
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/ContentItemFactory.java,v 1.27 2012/03/26 07:32:01 dclunie Exp $";

	/***/
	public class UnrecognizedContentItem extends ContentItem {

		/**
		 * @param	parent
		 */
		public UnrecognizedContentItem(ContentItem parent) {
			super(parent,null);
		}

		/**
		 * @param	parent
		 * @param	list
		 */
		public UnrecognizedContentItem(ContentItem parent,AttributeList list) {
			super(parent,list);
		}

		/**
		 * @param	parent
		 * @param	list
		 * @param	name
		 */
		public UnrecognizedContentItem(ContentItem parent,AttributeList list,String name) {
			super(parent,list);
		}

		/***/
		public String getConceptValue()      { return ""; }
	}

	/***/
	public class ContainerContentItem extends ContentItem {
		/***/
		protected String continuityOfContent;
		/***/
		protected String templateMappingResource;
		/***/
		protected String templateIdentifier;

		/**
		 * @param	parent
		 * @param	list
		 */
		public ContainerContentItem(ContentItem parent,AttributeList list) {
			super(parent,list);
			continuityOfContent=Attribute.getSingleStringValueOrEmptyString(list,TagFromName.ContinuityOfContent);
			AttributeList contentTemplateSequenceItemAttributeList = SequenceAttribute.getAttributeListFromWithinSequenceWithSingleItem(list,TagFromName.ContentTemplateSequence);
			if (contentTemplateSequenceItemAttributeList != null) {
				templateMappingResource=Attribute.getSingleStringValueOrEmptyString(contentTemplateSequenceItemAttributeList,TagFromName.MappingResource); 
				templateIdentifier=Attribute.getSingleStringValueOrEmptyString(contentTemplateSequenceItemAttributeList,TagFromName.TemplateIdentifier);
			}
			if (templateMappingResource == null) templateMappingResource="";		// just for consistency with other string content items
			if (templateIdentifier == null) templateIdentifier="";
		}

		/**
		 * @param	parent
		 * @param	relationshipType
		 * @param	conceptName
		 * @param	continuityOfContentIsSeparate	true if SEPARATE, false if CONTINUOUS
		 * @param	templateMappingResource
		 * @param	templateIdentifier
		 * @throws	DicomException
		 */
		public ContainerContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,boolean continuityOfContentIsSeparate,
				String templateMappingResource,String templateIdentifier) throws DicomException {
			super(parent,"CONTAINER",relationshipType,conceptName);
			continuityOfContent = continuityOfContentIsSeparate ? "SEPARATE" : "CONTINUOUS";
			{ Attribute a = new CodeStringAttribute(TagFromName.ContinuityOfContent); a.addValue(continuityOfContent); list.put(a); }
			this.templateMappingResource = templateMappingResource;
			this.templateIdentifier = templateIdentifier;
			if (this.templateMappingResource != null || templateIdentifier != null) {
				SequenceAttribute contentTemplateSequence = new SequenceAttribute(TagFromName.ContentTemplateSequence);
				AttributeList contentTemplateSequenceItemAttributeList = new AttributeList();
				if (templateMappingResource != null) {
					Attribute a = new CodeStringAttribute(TagFromName.MappingResource); a.addValue(templateMappingResource.toUpperCase(java.util.Locale.US)); contentTemplateSequenceItemAttributeList.put(a);
				}
				if (templateIdentifier != null) {
					Attribute a = new CodeStringAttribute(TagFromName.TemplateIdentifier); a.addValue(templateIdentifier.toUpperCase(java.util.Locale.US)); contentTemplateSequenceItemAttributeList.put(a);
				}
				contentTemplateSequence.addItem(contentTemplateSequenceItemAttributeList);
				list.put(contentTemplateSequence);
			}
		}

		/**
		 * @param	parent
		 * @param	relationshipType
		 * @param	conceptName
		 * @param	continuityOfContentIsSeparate	true if SEPARATE, false if CONTINUOUS
		 * @throws	DicomException
		 */
		public ContainerContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,boolean continuityOfContentIsSeparate) throws DicomException {
			this(parent,relationshipType,conceptName,true,null,null);
		}

		/**
		 * @param	parent
		 * @param	relationshipType
		 * @param	conceptName
		 * @throws	DicomException
		 */
		public ContainerContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName) throws DicomException {
			this(parent,relationshipType,conceptName,true);
		}

		/***/
		public String getConceptValue()      { return ""; }

		/***/
		public String getContinuityOfContent()      { return continuityOfContent; }

		/***/
		public String getTemplateMappingResource()      { return templateMappingResource; }

		/***/
		public String getTemplateIdentifier()      { return templateIdentifier; }

		/***/
		public String toString() {
			return super.toString()
				   +(continuityOfContent != null && continuityOfContent.length() > 0 ? " ["+continuityOfContent+"]" : "")
				   +(templateIdentifier  != null && templateIdentifier.length()  > 0 ? " ("+templateMappingResource+","+templateIdentifier+")" : "")
				   ;
		}
	}
	
	/**
	 * @param	parent
	 * @param	relationshipType
	 * @param	conceptName
	 * @param	continuityOfContentIsSeparate	true if SEPARATE, false if CONTINUOUS
	 * @param	templateMappingResource
	 * @param	templateIdentifier
	 * @throws	DicomException
	 */
	public ContainerContentItem makeContainerContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,boolean continuityOfContentIsSeparate,
				String templateMappingResource,String templateIdentifier) throws DicomException {
		return new ContainerContentItem(parent,relationshipType,conceptName,continuityOfContentIsSeparate,templateMappingResource,templateIdentifier);
	}

	/***/
	public class CompositeContentItem extends ContentItem {

		/***/
		protected AttributeList referencedSOPSequenceItemAttributeList;		// subclasses will use this to extract or to add macro-specific attributes
		/***/
		protected String referencedSOPClassUID;
		/***/
		protected String referencedSOPInstanceUID;

		/**
		 * @param	parent
		 * @param	list
		 */
		public CompositeContentItem(ContentItem parent,AttributeList list) {
			super(parent,list);
			referencedSOPSequenceItemAttributeList = SequenceAttribute.getAttributeListFromWithinSequenceWithSingleItem(list,TagFromName.ReferencedSOPSequence);
			if (referencedSOPSequenceItemAttributeList != null) {
				referencedSOPClassUID=Attribute.getSingleStringValueOrEmptyString(referencedSOPSequenceItemAttributeList,TagFromName.ReferencedSOPClassUID);
				referencedSOPInstanceUID=Attribute.getSingleStringValueOrEmptyString(referencedSOPSequenceItemAttributeList,TagFromName.ReferencedSOPInstanceUID);
			}
			if (referencedSOPClassUID == null) referencedSOPClassUID="";		// just for consistency with other string content items
			if (referencedSOPInstanceUID == null) referencedSOPInstanceUID="";
		}

		/**
		 * @param	parent
		 * @param	relationshipType
		 * @param	conceptName
		 * @param	referencedSOPClassUID
		 * @param	referencedSOPInstanceUID
		 * @throws	DicomException
		 */
		public CompositeContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,
				String referencedSOPClassUID,String referencedSOPInstanceUID) throws DicomException {
			super(parent,"COMPOSITE",relationshipType,conceptName);
			doCommonConstructorStuff(referencedSOPClassUID,referencedSOPInstanceUID);
		}
	
		/**
		 * @param	parent
		 * @param	valueType
		 * @param	relationshipType
		 * @param	conceptName
		 * @param	referencedSOPClassUID
		 * @param	referencedSOPInstanceUID
		 * @throws	DicomException
		 */
		protected CompositeContentItem(ContentItem parent,String valueType,String relationshipType,CodedSequenceItem conceptName,
				String referencedSOPClassUID,String referencedSOPInstanceUID) throws DicomException {
			super(parent,valueType,relationshipType,conceptName);
			doCommonConstructorStuff(referencedSOPClassUID,referencedSOPInstanceUID);
		}
	
		/**
		 * @param	referencedSOPClassUID
		 * @param	referencedSOPInstanceUID
		 * @throws	DicomException
		 */
		protected void doCommonConstructorStuff(String referencedSOPClassUID,String referencedSOPInstanceUID) throws DicomException {
			referencedSOPSequenceItemAttributeList = new AttributeList();
			this.referencedSOPClassUID = referencedSOPClassUID;
			if (referencedSOPClassUID != null) {
				Attribute a = new UniqueIdentifierAttribute(TagFromName.ReferencedSOPClassUID); a.addValue(referencedSOPClassUID); referencedSOPSequenceItemAttributeList.put(a);
			}
			this.referencedSOPInstanceUID = referencedSOPInstanceUID;
			if (referencedSOPInstanceUID != null) {
				Attribute a = new UniqueIdentifierAttribute(TagFromName.ReferencedSOPInstanceUID); a.addValue(referencedSOPInstanceUID); referencedSOPSequenceItemAttributeList.put(a);
			}
			SequenceAttribute referencedSOPSequence = new SequenceAttribute(TagFromName.ReferencedSOPSequence);
			list.put(referencedSOPSequence);
			referencedSOPSequence.addItem(referencedSOPSequenceItemAttributeList);
		}
	
		/***/
		public String getConceptValue()      { return ""; }

		/***/
		public String toString() {
			return super.toString()+" = "+referencedSOPClassUID+" : "+referencedSOPInstanceUID;
		}

		/***/
		public String getReferencedSOPClassUID()    { return referencedSOPClassUID; }
		/***/
		public String getReferencedSOPInstanceUID() { return referencedSOPInstanceUID; }
	}


	/**
	 * @param	parent
	 * @param	relationshipType
	 * @param	conceptName
	 * @param	referencedSOPClassUID
	 * @param	referencedSOPInstanceUID
	 * @throws	DicomException
	 */
	public CompositeContentItem makeCompositeContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,
			String referencedSOPClassUID,String referencedSOPInstanceUID) throws DicomException {
		return new CompositeContentItem(parent,relationshipType,conceptName,
			referencedSOPClassUID,referencedSOPInstanceUID);
	}
	/***/
	public class ImageContentItem extends CompositeContentItem {

		/***/
		protected int referencedFrameNumber;
		/***/
		protected int referencedSegmentNumber;
		/***/
		protected String presentationStateSOPClassUID;
		/***/
		protected String presentationStateSOPInstanceUID;
		/***/
		protected String realWorldValueMappingSOPClassUID;
		/***/
		protected String realWorldValueMappingSOPInstanceUID;

		/**
		 * @param	parent
		 * @param	list
		 */
		public ImageContentItem(ContentItem parent,AttributeList list) {
			super(parent,list);
			if (referencedSOPSequenceItemAttributeList != null) {
				referencedFrameNumber=Attribute.getSingleIntegerValueOrDefault(referencedSOPSequenceItemAttributeList,TagFromName.ReferencedFrameNumber,0);
				referencedSegmentNumber=Attribute.getSingleIntegerValueOrDefault(referencedSOPSequenceItemAttributeList,TagFromName.ReferencedSegmentNumber,0);

				{
					AttributeList psl = SequenceAttribute.getAttributeListFromWithinSequenceWithSingleItem(referencedSOPSequenceItemAttributeList,TagFromName.ReferencedSOPSequence);
					if (psl != null) {
						presentationStateSOPClassUID=Attribute.getSingleStringValueOrEmptyString(psl,TagFromName.ReferencedSOPClassUID);
						presentationStateSOPInstanceUID=Attribute.getSingleStringValueOrEmptyString(psl,TagFromName.ReferencedSOPInstanceUID);
					}
					if (presentationStateSOPClassUID == null) presentationStateSOPClassUID="";
					if (presentationStateSOPInstanceUID == null) presentationStateSOPInstanceUID="";
				}
				{
					AttributeList rwvl = SequenceAttribute.getAttributeListFromWithinSequenceWithSingleItem(referencedSOPSequenceItemAttributeList,TagFromName.ReferencedRealWorldValueMappingInstanceSequence);
					if (rwvl != null) {
						realWorldValueMappingSOPClassUID=Attribute.getSingleStringValueOrEmptyString(rwvl,TagFromName.ReferencedSOPClassUID);
						realWorldValueMappingSOPInstanceUID=Attribute.getSingleStringValueOrEmptyString(rwvl,TagFromName.ReferencedSOPInstanceUID);
					}
					if (realWorldValueMappingSOPClassUID == null) realWorldValueMappingSOPClassUID="";
					if (realWorldValueMappingSOPInstanceUID == null) realWorldValueMappingSOPInstanceUID="";
				}
				// forget about Icon Image Sequence for now :(
			}
		}

		/**
		 * @param	parent
		 * @param	relationshipType
		 * @param	conceptName
		 * @param	referencedSOPClassUID
		 * @param	referencedSOPInstanceUID
		 * @param	referencedFrameNumber		if < 1, not added
		 * @param	referencedSegmentNumber		if < 1, not added
		 * @param	presentationStateSOPClassUID
		 * @param	presentationStateSOPInstanceUID
		 * @param	realWorldValueMappingSOPClassUID
		 * @param	realWorldValueMappingSOPInstanceUID
		 * @throws	DicomException
		 */
		public ImageContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,
				String referencedSOPClassUID,String referencedSOPInstanceUID,
				int referencedFrameNumber,int referencedSegmentNumber,
				String presentationStateSOPClassUID,String presentationStateSOPInstanceUID,
				String realWorldValueMappingSOPClassUID,String realWorldValueMappingSOPInstanceUID) throws DicomException {
			super(parent,"IMAGE",relationshipType,conceptName,referencedSOPClassUID,referencedSOPInstanceUID);
			this.referencedFrameNumber = referencedFrameNumber < 1 ? 0 : referencedFrameNumber;
			if (referencedFrameNumber >= 1) {
				Attribute a = new IntegerStringAttribute(TagFromName.ReferencedFrameNumber); a.addValue(referencedFrameNumber); referencedSOPSequenceItemAttributeList.put(a);
			}
			this.referencedSegmentNumber = referencedSegmentNumber < 1 ? 0 : referencedSegmentNumber;
			if (referencedSegmentNumber >= 1) {
				Attribute a = new IntegerStringAttribute(TagFromName.ReferencedSegmentNumber); a.addValue(referencedSegmentNumber); referencedSOPSequenceItemAttributeList.put(a);
			}
			this.presentationStateSOPClassUID = presentationStateSOPClassUID;
			this.presentationStateSOPInstanceUID = presentationStateSOPInstanceUID;
			if (presentationStateSOPClassUID != null && presentationStateSOPClassUID.length() > 0
			 && presentationStateSOPInstanceUID != null && presentationStateSOPInstanceUID.length() > 0) {
				SequenceAttribute presentationStateReferencedSOPSequence = new SequenceAttribute(TagFromName.ReferencedSOPSequence);
				referencedSOPSequenceItemAttributeList.put(presentationStateReferencedSOPSequence);
				AttributeList presentationStateReferencedSOPSequenceList = new AttributeList();
				presentationStateReferencedSOPSequence.addItem(presentationStateReferencedSOPSequenceList);
				{ Attribute a = new UniqueIdentifierAttribute(TagFromName.ReferencedSOPClassUID); a.addValue(presentationStateSOPClassUID); presentationStateReferencedSOPSequenceList.put(a); }
				{ Attribute a = new UniqueIdentifierAttribute(TagFromName.ReferencedSOPInstanceUID); a.addValue(presentationStateSOPInstanceUID); presentationStateReferencedSOPSequenceList.put(a); }
			}
			this.realWorldValueMappingSOPClassUID = realWorldValueMappingSOPClassUID;
			this.realWorldValueMappingSOPInstanceUID = realWorldValueMappingSOPInstanceUID;
			if (realWorldValueMappingSOPClassUID != null && realWorldValueMappingSOPClassUID.length() > 0
			 && realWorldValueMappingSOPInstanceUID != null && realWorldValueMappingSOPInstanceUID.length() > 0) {
				SequenceAttribute referencedRealWorldValueMappingInstanceSequence = new SequenceAttribute(TagFromName.ReferencedRealWorldValueMappingInstanceSequence);
				referencedSOPSequenceItemAttributeList.put(referencedRealWorldValueMappingInstanceSequence);
				AttributeList referencedRealWorldValueMappingInstanceSequenceList = new AttributeList();
				referencedRealWorldValueMappingInstanceSequence.addItem(referencedRealWorldValueMappingInstanceSequenceList);
				{ Attribute a = new UniqueIdentifierAttribute(TagFromName.ReferencedSOPClassUID); a.addValue(realWorldValueMappingSOPClassUID); referencedRealWorldValueMappingInstanceSequenceList.put(a); }
				{ Attribute a = new UniqueIdentifierAttribute(TagFromName.ReferencedSOPInstanceUID); a.addValue(realWorldValueMappingSOPInstanceUID); referencedRealWorldValueMappingInstanceSequenceList.put(a); }
			}
		}
	
		/***/
		public String toString() {
			return super.toString()
				+ (referencedFrameNumber == 0 ? "" : ("[Frame "+Integer.toString(referencedFrameNumber)+"]"))
				+ (referencedSegmentNumber == 0 ? "" : ("[Segment "+Integer.toString(referencedSegmentNumber)+"]"))
				+ (presentationStateSOPInstanceUID == null || presentationStateSOPInstanceUID.length() == 0 ? "" : (" (PS "+presentationStateSOPClassUID+" : "+presentationStateSOPInstanceUID+")"))
				+ (realWorldValueMappingSOPInstanceUID == null || realWorldValueMappingSOPInstanceUID.length() == 0 ? "" : (" (RWV "+realWorldValueMappingSOPClassUID+" : "+realWorldValueMappingSOPInstanceUID+")"))
				;
		}

		/***/
		public int getReferencedFrameNumber()    { return referencedFrameNumber; }
		/***/
		public int getReferencedSegmentNumber()    { return referencedSegmentNumber; }
		/***/
		public String getPresentationStateSOPClassUID()    { return presentationStateSOPClassUID; }
		/***/
		public String getPresentationStateSOPInstanceUID() { return presentationStateSOPInstanceUID; }
		/***/
		public String getRealWorldValueMappingSOPClassUID()    { return realWorldValueMappingSOPClassUID; }
		/***/
		public String getRealWorldValueMappingSOPInstanceUID() { return realWorldValueMappingSOPInstanceUID; }
	}

	/**
	 * @param	parent
	 * @param	relationshipType
	 * @param	conceptName
	 * @param	referencedSOPClassUID
	 * @param	referencedSOPInstanceUID
	 * @param	referencedFrameNumber		if < 1, not added
	 * @param	referencedSegmentNumber		if < 1, not added
	 * @param	presentationStateSOPClassUID
	 * @param	presentationStateSOPInstanceUID
	 * @param	realWorldValueMappingSOPClassUID
	 * @param	realWorldValueMappingSOPInstanceUID
	 * @throws	DicomException
	 */
	public ImageContentItem makeImageContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,
			String referencedSOPClassUID,String referencedSOPInstanceUID,
			int referencedFrameNumber,int referencedSegmentNumber,
			String presentationStateSOPClassUID,String presentationStateSOPInstanceUID,
			String realWorldValueMappingSOPClassUID,String realWorldValueMappingSOPInstanceUID) throws DicomException {
		return new ImageContentItem(parent,relationshipType,conceptName,
			referencedSOPClassUID,referencedSOPInstanceUID,
			referencedFrameNumber,referencedSegmentNumber,
			presentationStateSOPClassUID,presentationStateSOPInstanceUID,
			realWorldValueMappingSOPClassUID,realWorldValueMappingSOPInstanceUID);
	}

	/***/
	public class WaveformContentItem extends CompositeContentItem {

		/***/
		protected int[] referencedWaveformChannels;

		/**
		 * @param	parent
		 * @param	list
		 */
		public WaveformContentItem(ContentItem parent,AttributeList list) {
			super(parent,list);
			if (referencedSOPSequenceItemAttributeList != null) {
				referencedWaveformChannels=Attribute.getIntegerValues(referencedSOPSequenceItemAttributeList,TagFromName.ReferencedWaveformChannels);
			}
		}

		/***/
		public String toString() {
			StringBuffer str = new StringBuffer();
			str.append(super.toString());
			str.append(" = [");
			if (referencedWaveformChannels != null) {
				for (int j=0; j<referencedWaveformChannels.length; ++j) {
					if (j > 0) str.append(",");
					str.append(referencedWaveformChannels[j]);
				}
			}
			str.append("]");
			return str.toString();
		}

		/***/
		public int[] getReferencedWaveformChannels()    { return referencedWaveformChannels; }
	}

	/***/
	public class SpatialCoordinatesContentItem extends ContentItem {

		/***/
		protected String graphicType;
		/***/
		protected float[] graphicData;

		/**
		 * @param	parent
		 * @param	list
		 */
		public SpatialCoordinatesContentItem(ContentItem parent,AttributeList list) {
			super(parent,list);
			graphicType=Attribute.getSingleStringValueOrDefault(list,TagFromName.GraphicType,"");
			try {
				Attribute a = list.get(TagFromName.GraphicData);
				if (a != null) {
					graphicData = a.getFloatValues();
				}
			}
			catch (DicomException e) {
			}
		}

		/**
		 * @param	parent
		 * @param	relationshipType
		 * @param	conceptName
		 * @param	graphicType
		 * @param	graphicData
		 * @throws	DicomException
		 */
		public SpatialCoordinatesContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,String graphicType,float[] graphicData) throws DicomException {
			super(parent,"SCOORD",relationshipType,conceptName);
			this.graphicType=graphicType;
			if (graphicType != null) {
				Attribute a = new CodeStringAttribute(TagFromName.GraphicType); a.addValue(graphicType); list.put(a);
			}
			this.graphicData=graphicData;
			if (graphicData != null) {
				Attribute a = new FloatSingleAttribute(TagFromName.GraphicData);
				for (int j=0; j<graphicData.length; ++j) {	// should be a single method in FloatSingleAttribute to add the whole array :(
					a.addValue(graphicData[j]);
				}
				list.put(a);
			}
		}

		/***/
		public String getConceptValue()      { return ""; }

		/***/
		public String toString() {
			StringBuffer str = new StringBuffer();
			str.append(super.toString());
			str.append(" = ");
			str.append(graphicType);
			str.append(" (");
			if (graphicData != null) {
				for (int j=0; j<graphicData.length; ++j) {
					if (j > 0) str.append(",");
					str.append(FloatFormatter.toStringOfFixedMaximumLength(graphicData[j],16/* maximum size of DS */,false/* non-numbers already handled */,Locale.US));
				}
			}
			str.append(")");
			return str.toString();
		}

		/***/
		public String getGraphicType()              { return graphicType; }
		/***/
		public float[] getGraphicData()             { return graphicData; }
	}

	/**
	 * @param	parent
	 * @param	relationshipType
	 * @param	conceptName
	 * @param	graphicType
	 * @param	graphicData
	 * @throws	DicomException
	 */
	public SpatialCoordinatesContentItem makeSpatialCoordinatesContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,String graphicType,float[] graphicData) throws DicomException {
		return new SpatialCoordinatesContentItem(parent,relationshipType,conceptName,graphicType,graphicData);
	}

	/***/
	public class TemporalCoordinatesContentItem extends ContentItem {

		/***/
		protected String temporalRangeType;
		/***/
		protected int[] referencedSamplePositions;
		/***/
		protected float[] referencedTimeOffsets;
		/***/
		protected String[] referencedDateTimes;

		/**
		 * @param	parent
		 * @param	list
		 */
		public TemporalCoordinatesContentItem(ContentItem parent,AttributeList list) {
			super(parent,list);
			temporalRangeType=Attribute.getSingleStringValueOrDefault(list,TagFromName.TemporalRangeType,"");
			try {
				{
					Attribute a = list.get(TagFromName.ReferencedSamplePositions);
					if (a != null) {
						referencedSamplePositions = a.getIntegerValues();
					}
				}
				{
					Attribute a = list.get(TagFromName.ReferencedTimeOffsets);
					if (a != null) {
						referencedTimeOffsets = a.getFloatValues();
					}
				}
				{
					Attribute a = list.get(TagFromName.ReferencedDateTime);
					if (a != null) {
						referencedDateTimes = a.getStringValues();
					}
				}
			}
			catch (DicomException e) {
			}
		}

		/***/
		public String getConceptValue()      { return ""; }

		/***/
		public String toString() {
			StringBuffer str = new StringBuffer();
			str.append(super.toString());
			str.append(" = ");
			str.append(temporalRangeType);
			if (referencedSamplePositions != null) {
				str.append(" Sample Positions (");
				for (int j=0; j<referencedSamplePositions.length; ++j) {
					if (j > 0) str.append(",");
					str.append(referencedSamplePositions[j]);
				}
				str.append(")");
			}
			if (referencedTimeOffsets != null) {
				str.append(" Time Offsets (");
				for (int j=0; j<referencedTimeOffsets.length; ++j) {
					if (j > 0) str.append(",");
					str.append(referencedTimeOffsets[j]);
				}
				str.append(")");
			}
			if (referencedDateTimes != null) {
				str.append(" DateTimes (");
				for (int j=0; j<referencedDateTimes.length; ++j) {
					if (j > 0) str.append(",");
					str.append(referencedDateTimes[j]);
				}
				str.append(")");
			}
			return str.toString();
		}

		/***/
		public String getTemporalRangeType()		{ return temporalRangeType; }
		/***/
		public int[] getReferencedSamplePositions()	{ return referencedSamplePositions; }
		/***/
		public float[] getReferencedTimeOffsets()	{ return referencedTimeOffsets; }
		/***/
		public String[] getReferencedDateTimes()	{ return referencedDateTimes; }
	}

	/***/
	public class NumericContentItem extends ContentItem {

		/***/
		protected String numericValue;
		/***/
		protected CodedSequenceItem units;
		/***/
		protected CodedSequenceItem qualifier;

		/**
		 * @param	parent
		 * @param	list
		 */
		public NumericContentItem(ContentItem parent,AttributeList list) {
			super(parent,list);
			SequenceAttribute a=(SequenceAttribute)(list.get(TagFromName.MeasuredValueSequence));
			if (a != null) {
//System.err.println("NumericContentItem: MeasuredValueSequence="+a);
				Iterator i = a.iterator();
				if (i.hasNext()) {
					SequenceItem item = ((SequenceItem)i.next());
					if (item != null) {
//System.err.println("NumericContentItem: item="+item);
						AttributeList l = item.getAttributeList();
						numericValue=Attribute.getSingleStringValueOrEmptyString(l,TagFromName.NumericValue);
						units=CodedSequenceItem.getSingleCodedSequenceItemOrNull(l,TagFromName.MeasurementUnitsCodeSequence);
					}
				}
			}
			if (numericValue == null) numericValue="";	// just for consistency with other string content items
			
			qualifier=CodedSequenceItem.getSingleCodedSequenceItemOrNull(list,TagFromName.NumericValueQualifierCodeSequence);
		}

		/**
		 * @param	parent
		 * @param	relationshipType
		 * @param	conceptName
		 * @param	numericValue		will be converted to string
		 * @param	units
		 * @throws	DicomException
		 */
		public NumericContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,double numericValue,CodedSequenceItem units) throws DicomException {
			super(parent,"NUM",relationshipType,conceptName);
//System.err.println("NumericContentItem(): constructor checking for need for qualifiers for "+numericValue);
			if (numericValue == Double.NaN || Double.isNaN(numericValue)) {			// the constant match does not seem to work, hence the method call
//System.err.println("NumericContentItem(): matches NaN");
				constructOnlyQualifier(new CodedSequenceItem("114000","DCM","Not a number"));
			}
			else if (numericValue == Double.NEGATIVE_INFINITY) {
				constructOnlyQualifier(new CodedSequenceItem("114001","DCM","Negative Infinity"));
			}
			else if (numericValue == Double.POSITIVE_INFINITY) {
				constructOnlyQualifier(new CodedSequenceItem("114002","DCM","Positive Infinity"));
			}
			else {
				doCommonConstructorStuff(FloatFormatter.toStringOfFixedMaximumLength(numericValue,16/* maximum size of DS */,false/* non-numbers already handled */,Locale.US),units,null/*no qualifier*/);
			}
		}

		/**
		 * @param	parent
		 * @param	relationshipType
		 * @param	conceptName
		 * @param	numericValue
		 * @param	units
		 * @throws	DicomException
		 */
		public NumericContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,String numericValue,CodedSequenceItem units) throws DicomException {
			this(parent,relationshipType,conceptName,numericValue,units,null);
		}
		
		/**
		 * @param	parent
		 * @param	relationshipType
		 * @param	conceptName
		 * @param	numericValue
		 * @param	units
		 * @param	qualifier
		 * @throws	DicomException
		 */
		public NumericContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,String numericValue,CodedSequenceItem units,CodedSequenceItem qualifier) throws DicomException {
			super(parent,"NUM",relationshipType,conceptName);
			doCommonConstructorStuff(numericValue,units,qualifier);
		}

		/**
		 * <p>Construct numeric content item with empty <code>MeasuredValueSequence</code> with qualifier explaining why it is empty.</p>
		 *
		 * @param	parent
		 * @param	relationshipType
		 * @param	conceptName
		 * @param	qualifier
		 * @throws	DicomException
		 */
		public NumericContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,CodedSequenceItem qualifier) throws DicomException {
			super(parent,"NUM",relationshipType,conceptName);
			constructOnlyQualifier(qualifier);
		}
		
		protected void doCommonConstructorStuff(String numericValue,CodedSequenceItem units,CodedSequenceItem qualifier) throws DicomException {
			SequenceAttribute mvs = new SequenceAttribute(TagFromName.MeasuredValueSequence); list.put(mvs);
			AttributeList mvl = new AttributeList();
			mvs.addItem(mvl); 
			
			if (numericValue == null) {
				this.numericValue = "";		// just for consistency with other string content items
			}
			else {
				this.numericValue=numericValue;
				Attribute a = new DecimalStringAttribute(TagFromName.NumericValue); a.addValue(numericValue); mvl.put(a);
			}
			this.units=units;
			if (units != null) {
				SequenceAttribute a = new SequenceAttribute(TagFromName.MeasurementUnitsCodeSequence); a.addItem(units.getAttributeList()); mvl.put(a);
			}
			this.qualifier=qualifier;
			if (qualifier != null) {
				SequenceAttribute a = new SequenceAttribute(TagFromName.NumericValueQualifierCodeSequence); a.addItem(qualifier.getAttributeList()); list.put(a); // list, not mvl !
			}
		}
		
		protected void constructOnlyQualifier(CodedSequenceItem qualifier) {
			SequenceAttribute mvs = new SequenceAttribute(TagFromName.MeasuredValueSequence); list.put(mvs);
			this.qualifier=qualifier;
			if (qualifier != null) {
				SequenceAttribute a = new SequenceAttribute(TagFromName.NumericValueQualifierCodeSequence); a.addItem(qualifier.getAttributeList()); list.put(a); // list, not mvl !
			}
			this.numericValue = "";		// rather than null, just for consistency with other string content items
		}
		
		/***/
		public CodedSequenceItem getQualifier()		{ return qualifier; }

		/***/
		public CodedSequenceItem getUnits()		{ return units; }

		/***/
		public String getNumericValue()			{ return numericValue; }

		/***/
		public String getConceptValue() {
			return numericValue+" "+(units == null ? "" : units.getCodeMeaning());
		}

		/***/
		public String getConceptNameAndValue() {
			return getConceptNameCodeMeaning()+" = "+numericValue+" "+(units == null ? "" : units.getCodeMeaning())+" "+(qualifier == null ? "" : qualifier.getCodeMeaning());
		}

		/***/
		public String toString() {
			return super.toString()+" = "+numericValue+" "+(units == null ? "" : units.getCodeMeaning())+" "+(qualifier == null ? "" : qualifier.getCodeMeaning());
		}
	}

	/**
	 * @param	parent
	 * @param	relationshipType
	 * @param	conceptName
	 * @param	numericValue
	 * @param	units
	 * @param	qualifier
	 * @throws	DicomException
	 */
	public NumericContentItem makeNumericContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,String numericValue,CodedSequenceItem units,CodedSequenceItem qualifier) throws DicomException {
		return new NumericContentItem(parent,relationshipType,conceptName,numericValue,units,qualifier);
	}

	/**
	 * @param	parent
	 * @param	relationshipType
	 * @param	conceptName
	 * @param	numericValue
	 * @param	units
	 * @throws	DicomException
	 */
	public NumericContentItem makeNumericContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,double numericValue,CodedSequenceItem units) throws DicomException {
		return new NumericContentItem(parent,relationshipType,conceptName,numericValue,units);
	}

	/***/
	public class CodeContentItem extends ContentItem {

		/***/
		protected CodedSequenceItem conceptCode;

		/**
		 * @param	parent
		 * @param	list
		 */
		public CodeContentItem(ContentItem parent,AttributeList list) {
			super(parent,list);
			conceptCode=CodedSequenceItem.getSingleCodedSequenceItemOrNull(list,TagFromName.ConceptCodeSequence);
		}

		/**
		 * @param	parent
		 * @param	relationshipType
		 * @param	conceptName
		 * @param	conceptCode
		 * @throws	DicomException
		 */
		public CodeContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,CodedSequenceItem conceptCode) throws DicomException {
			super(parent,"CODE",relationshipType,conceptName);
			this.conceptCode=conceptCode;
			if (conceptCode != null) {
				SequenceAttribute a = new SequenceAttribute(TagFromName.ConceptCodeSequence); a.addItem(conceptCode.getAttributeList()); list.put(a);
			}
		}

		/***/
		public String getConceptValue() {
			return (conceptCode == null ? "" : conceptCode.getCodeMeaning());
		}

		/***/
		public String toString() {
			return super.toString()+" = "+(conceptCode == null ? "" : conceptCode.getCodeMeaning());
		}
		
		/***/
		public CodedSequenceItem getConceptCode()    { return conceptCode; }

		/**
		 * Test if the coded value of the code content item matches the specified code value and coding scheme designator.
		 *
		 * This is more robust than checking code meaning, which may have synomyms, and there is no need to also test code meaning.
		 *
		 * @param	csdWanted
		 * @param	cvWanted
		 * @return					true if matches
		 */
		public boolean contentItemValueMatchesCodeValueAndCodingSchemeDesignator(String cvWanted,String csdWanted) {
			boolean isMatch = false;
			if (conceptCode != null) {
				String csd = conceptCode.getCodingSchemeDesignator();
				String cv = conceptCode.getCodeValue();
				if (csd != null && csd.trim().equals(csdWanted.trim()) && cv != null && cv.trim().equals(cvWanted.trim())) {
					isMatch = true;
				}
			}
			return isMatch;
		}
	}
	
	/**
	 * @param	parent
	 * @param	relationshipType
	 * @param	conceptName
	 * @param	conceptCode
	 * @throws	DicomException
	 */
	public CodeContentItem makeCodeContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,CodedSequenceItem conceptCode) throws DicomException {
		return new CodeContentItem(parent,relationshipType,conceptName,conceptCode);
	}
	
	/**
	 * Test if the coded value of the code content item matches the specified code value and coding scheme designator.
	 *
	 * This is more robust than checking code meaning, which may have synomyms, and there is no need to also test code meaning.
	 *
	 * @param	ci
	 * @param	csdWanted
	 * @param	cvWanted
	 * @return					true if matches
	 */
	public static boolean codeContentItemValueMatchesCodeValueAndCodingSchemeDesignator(ContentItem ci,String cvWanted,String csdWanted) {
		boolean isMatch = false;
		if (ci != null && ci instanceof ContentItemFactory.CodeContentItem) {
			ContentItemFactory.CodeContentItem cci = ( ContentItemFactory.CodeContentItem)ci;
			isMatch = cci.contentItemValueMatchesCodeValueAndCodingSchemeDesignator(cvWanted,csdWanted);
		}
		return isMatch;
	}

	/***/
	abstract protected class StringContentItem extends ContentItem {

		/***/
		protected String stringValue;

		/**
		 * @param	parent
		 * @param	list
		 * @param	tag
		 */
		public StringContentItem(ContentItem parent,AttributeList list,AttributeTag tag) {
			super(parent,list);
			stringValue=Attribute.getSingleStringValueOrDefault(list,tag,"");
		}

		/**
		 * @param	parent
		 * @param	valueType
		 * @param	relationshipType
		 * @param	conceptName
		 * @param	tagForValue
		 * @param	stringValue
		 * @throws	DicomException
		 */
		public StringContentItem(ContentItem parent,String valueType,String relationshipType,CodedSequenceItem conceptName,AttributeTag tagForValue,String stringValue) throws DicomException {
			super(parent,valueType,relationshipType,conceptName);
			this.stringValue=stringValue;
			if (stringValue != null) {
				Attribute a = AttributeFactory.newAttribute(tagForValue);
				a.addValue(stringValue);
				list.put(a);
			}
		}

		/***/
		public String getConceptValue() {
			return stringValue;
		}

		/**
		 * @param	tagForValue
		 * @param	stringValue	if null, removes the value
		 * @throws	DicomException
		 */
		public void setConceptValue(AttributeTag tagForValue,String stringValue) throws DicomException {
			this.stringValue=stringValue;
			if (stringValue == null) {
				list.remove(tagForValue);
			}
			else {
				Attribute a = AttributeFactory.newAttribute(tagForValue);
				a.addValue(stringValue);
				list.put(a);
			}
		}

		/***/
		public String toString() {
			return super.toString()+" = "+stringValue;
		}
	}

	/***/
	public class DateTimeContentItem extends StringContentItem {

		/**
		 * @param	parent
		 * @param	list
		 */
		public DateTimeContentItem(ContentItem parent,AttributeList list) {
			super(parent,list,TagFromName.DateTime);
		}

		/**
		 * @param	parent
		 * @param	relationshipType
		 * @param	conceptName
		 * @param	stringValue
		 * @throws	DicomException
		 */
		public DateTimeContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,String stringValue) throws DicomException {
			super(parent,"DATETIME",relationshipType,conceptName,TagFromName.DateTime,stringValue);
		}

		/**
		 * @param	stringValue	if null, removes the value
		 * @throws	DicomException
		 */
		public void setConceptValue(String stringValue) throws DicomException {
			setConceptValue(TagFromName.DateTime,stringValue);
		}
	}

	/**
	 * @param	parent
	 * @param	relationshipType
	 * @param	conceptName
	 * @param	stringValue
	 * @throws	DicomException
	 */
	public DateTimeContentItem makeDateTimeContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,String stringValue) throws DicomException {
		return new DateTimeContentItem(parent,relationshipType,conceptName,stringValue);
	}

	/***/
	public class DateContentItem extends StringContentItem {

		/**
		 * @param	parent
		 * @param	list
		 */
		public DateContentItem(ContentItem parent,AttributeList list) {
			super(parent,list,TagFromName.Date);
		}

		/**
		 * @param	parent
		 * @param	relationshipType
		 * @param	conceptName
		 * @param	stringValue
		 * @throws	DicomException
		 */
		public DateContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,String stringValue) throws DicomException {
			super(parent,"DATE",relationshipType,conceptName,TagFromName.Date,stringValue);
		}

		/**
		 * @param	stringValue	if null, removes the value
		 * @throws	DicomException
		 */
		public void setConceptValue(String stringValue) throws DicomException {
			setConceptValue(TagFromName.Date,stringValue);
		}
	}

	/**
	 * @param	parent
	 * @param	relationshipType
	 * @param	conceptName
	 * @param	stringValue
	 * @throws	DicomException
	 */
	public DateContentItem makeDateContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,String stringValue) throws DicomException {
		return new DateContentItem(parent,relationshipType,conceptName,stringValue);
	}

	/***/
	public class TimeContentItem extends StringContentItem {

		/**
		 * @param	parent
		 * @param	list
		 */
		public TimeContentItem(ContentItem parent,AttributeList list) {
			super(parent,list,TagFromName.Time);
		}

		/**
		 * @param	parent
		 * @param	relationshipType
		 * @param	conceptName
		 * @param	stringValue
		 * @throws	DicomException
		 */
		public TimeContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,String stringValue) throws DicomException {
			super(parent,"TIME",relationshipType,conceptName,TagFromName.Time,stringValue);
		}

		/**
		 * @param	stringValue	if null, removes the value
		 * @throws	DicomException
		 */
		public void setConceptValue(String stringValue) throws DicomException {
			setConceptValue(TagFromName.Time,stringValue);
		}
	}

	/**
	 * @param	parent
	 * @param	relationshipType
	 * @param	conceptName
	 * @param	stringValue
	 * @throws	DicomException
	 */
	public TimeContentItem makeTimeContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,String stringValue) throws DicomException {
		return new TimeContentItem(parent,relationshipType,conceptName,stringValue);
	}

	/***/
	public class PersonNameContentItem extends StringContentItem {

		/**
		 * @param	parent
		 * @param	list
		 */
		public PersonNameContentItem(ContentItem parent,AttributeList list) {
			super(parent,list,TagFromName.PersonName);
		}

		/**
		 * @param	parent
		 * @param	relationshipType
		 * @param	conceptName
		 * @param	stringValue
		 * @throws	DicomException
		 */
		public PersonNameContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,String stringValue) throws DicomException {
			super(parent,"PNAME",relationshipType,conceptName,TagFromName.PersonName,stringValue);
		}

		/**
		 * @param	stringValue	if null, removes the value
		 * @throws	DicomException
		 */
		public void setConceptValue(String stringValue) throws DicomException {
			setConceptValue(TagFromName.PersonName,stringValue);
		}
	}

	/**
	 * @param	parent
	 * @param	relationshipType
	 * @param	conceptName
	 * @param	stringValue
	 * @throws	DicomException
	 */
	public PersonNameContentItem makePersonNameContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,String stringValue) throws DicomException {
		return new PersonNameContentItem(parent,relationshipType,conceptName,stringValue);
	}

	/***/
	public class UIDContentItem extends StringContentItem {

		/**
		 * @param	parent
		 * @param	list
		 */
		public UIDContentItem(ContentItem parent,AttributeList list) {
			super(parent,list,TagFromName.UID);
		}

		/**
		 * @param	parent
		 * @param	relationshipType
		 * @param	conceptName
		 * @param	stringValue
		 * @throws	DicomException
		 */
		public UIDContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,String stringValue) throws DicomException {
			super(parent,"UIDREF",relationshipType,conceptName,TagFromName.UID,stringValue);
		}

		/**
		 * @param	stringValue	if null, removes the value
		 * @throws	DicomException
		 */
		public void setConceptValue(String stringValue) throws DicomException {
			setConceptValue(TagFromName.UID,stringValue);
		}
	}

	/**
	 * @param	parent
	 * @param	relationshipType
	 * @param	conceptName
	 * @param	stringValue
	 * @throws	DicomException
	 */
	public UIDContentItem makeUIDContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,String stringValue) throws DicomException {
		return new UIDContentItem(parent,relationshipType,conceptName,stringValue);
	}

	/***/
	public class TextContentItem extends StringContentItem {

		/**
		 * @param	parent
		 * @param	list
		 */
		public TextContentItem(ContentItem parent,AttributeList list) {
			super(parent,list,TagFromName.TextValue);
		}

		/**
		 * @param	parent
		 * @param	relationshipType
		 * @param	conceptName
		 * @param	stringValue
		 * @throws	DicomException
		 */
		public TextContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,String stringValue) throws DicomException {
			super(parent,"TEXT",relationshipType,conceptName,TagFromName.TextValue,stringValue);
		}

		/**
		 * @param	stringValue	if null, removes the value
		 * @throws	DicomException
		 */
		public void setConceptValue(String stringValue) throws DicomException {
			setConceptValue(TagFromName.TextValue,stringValue);
		}
	}

	/**
	 * @param	parent
	 * @param	relationshipType
	 * @param	conceptName
	 * @param	stringValue
	 * @throws	DicomException
	 */
	public TextContentItem makeTextContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,String stringValue) throws DicomException {
		return new TextContentItem(parent,relationshipType,conceptName,stringValue);
	}

	/**
	 * <p>Construct a content item of the appropriate class from a list of attributes.</p>
	 *
	 * @param	parent	the parent to add the content item to
	 * @param	list	a list of attributes that constitute the content item as it is encoded in a DICOM data set
	 * @return		a content item
	 */
	public ContentItem getNewContentItem(ContentItem parent,AttributeList list) {
		ContentItem contentItem = null;

		if (list == null) {
			contentItem = new UnrecognizedContentItem(parent);
		}
		else {
			String valueType=Attribute.getSingleStringValueOrNull(list,TagFromName.ValueType);
			if (valueType == null) {
				contentItem = new UnrecognizedContentItem(parent,list);
			}
			else if (valueType.equals("CONTAINER")) {
				contentItem = new ContainerContentItem(parent,list);
			}
			else if (valueType.equals("CODE")) {
				contentItem = new CodeContentItem(parent,list);
			}
			else if (valueType.equals("NUM")) {
				contentItem = new NumericContentItem(parent,list);
			}
			else if (valueType.equals("DATETIME")) {
				contentItem = new DateTimeContentItem(parent,list);
			}
			else if (valueType.equals("DATE")) {
				contentItem = new DateContentItem(parent,list);
			}
			else if (valueType.equals("TIME")) {
				contentItem = new TimeContentItem(parent,list);
			}
			else if (valueType.equals("PNAME")) {
				contentItem = new PersonNameContentItem(parent,list);
			}
			else if (valueType.equals("UIDREF")) {
				contentItem = new UIDContentItem(parent,list);
			}
			else if (valueType.equals("TEXT")) {
				contentItem = new TextContentItem(parent,list);
			}
			else if (valueType.equals("SCOORD")) {
				contentItem = new SpatialCoordinatesContentItem(parent,list);
			}
			else if (valueType.equals("TCOORD")) {
				contentItem = new TemporalCoordinatesContentItem(parent,list);
			}
			else if (valueType.equals("COMPOSITE")) {
				contentItem = new CompositeContentItem(parent,list);
			}
			else if (valueType.equals("IMAGE")) {
				contentItem = new ImageContentItem(parent,list);
			}
			else if (valueType.equals("WAVEFORM")) {
				contentItem = new WaveformContentItem(parent,list);
			}
			else {
				contentItem = new UnrecognizedContentItem(parent,list,valueType);
			}
		}

		return contentItem;
	}
}



/* Copyright (c) 2001-2011, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.Toolkit;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.File;
import java.io.IOException;

import java.util.Vector;

//import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.Spring;
import javax.swing.SpringLayout;
//import javax.swing.border.EmptyBorder;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.CodeStringAttribute;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.DicomInputStream;
import com.pixelmed.dicom.FileMetaInformation;
import com.pixelmed.dicom.LongStringAttribute;
import com.pixelmed.dicom.SOPClass;
import com.pixelmed.dicom.TagFromName;
import com.pixelmed.dicom.TransferSyntax;

import com.pixelmed.display.event.FrameSelectionChangeEvent;
import com.pixelmed.display.event.GraphicDisplayChangeEvent;

import com.pixelmed.event.ApplicationEventDispatcher;
import com.pixelmed.event.EventContext;
import com.pixelmed.event.SelfRegisteringListener;


import com.pixelmed.utils.FileUtilities;

/**
 * <p>This class displays images and allows the user to black out burned-in annotation, and save the result.</p>
 * 
 * <p>A main method is provided, which can be supplied with a list of file names or pop up a file chooser dialog.</p>
 * 
 * @author	dclunie
 */
public class DicomImageBlackout extends JFrame  {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/DicomImageBlackout.java,v 1.35 2011/09/02 20:40:47 dclunie Exp $";
	
	protected String ourAETitle = "OURAETITLE";		// sub-classes might set this to something meaningful if they are active on the network, e.g., DicomCleaner

	//private static final String helpText = "Buttons: left windows; middle scrolls frames; right drag draws box; right click selects box; delete key discards selection";
	private static final String helpText = "Buttons: left windows; right drag draws box; right click selects box; delete key discards selection";
	
	private static final Dimension maximumMultiPanelDimension = new Dimension(800,600);
	//private static final Dimension maximumMultiPanelDimension = Toolkit.getDefaultToolkit().getScreenSize();
	//private static final int heightWantedForButtons = 50;
	private static final double splitPaneResizeWeight = 0.9;

	protected String[] dicomFileNames;
	protected String currentFileName;
	protected int currentFileNumber;
	protected Box mainPanel;
	protected JPanel multiPanel;
	
	protected SingleImagePanel imagePanel;
	protected AttributeList list;
	protected SourceImage sImg;
	protected boolean changesWereMade;
	
	protected int previousRows;
	protected int previousColumns;
	protected Vector previousPersistentDrawingShapes;

	protected void recordStateOfDrawingShapesForNextFile() {
		previousRows = sImg.getHeight();
		previousColumns =  sImg.getWidth();
		previousPersistentDrawingShapes = imagePanel.getPersistentDrawingShapes();
	}

	protected JPanel cineSliderControlsPanel;
	protected CineSliderChangeListener cineSliderChangeListener;
	protected JSlider cineSlider;
	
	protected JLabel imagesRemainingLabel;
	
	protected EventContext ourEventContext;
	
	protected boolean burnInOverlays;
	
	protected boolean useZeroBlackoutValue;
	protected boolean usePixelPaddingBlackoutValue;
	
	protected JCheckBox useZeroBlackoutValueCheckBox;
	protected JCheckBox usePixelPaddingBlackoutValueCheckBox;
			
	// implement FrameSelectionChangeListener ...
	
	protected OurFrameSelectionChangeListener ourFrameSelectionChangeListener;

	class OurFrameSelectionChangeListener extends SelfRegisteringListener {
	
		public OurFrameSelectionChangeListener(EventContext eventContext) {
			super("com.pixelmed.display.event.FrameSelectionChangeEvent",eventContext);
//System.err.println("DicomImageBlackout.OurFrameSelectionChangeListener():");
		}
		
		/**
		 * @param	e
		 */
		public void changed(com.pixelmed.event.Event e) {
			FrameSelectionChangeEvent fse = (FrameSelectionChangeEvent)e;
//System.err.println("DicomImageBlackout.OurFrameSelectionChangeListener.changed(): event="+fse);
			cineSlider.setValue(fse.getIndex()+1);
		}
	}
	
	/***/
	protected class CineSliderChangeListener implements ChangeListener {
	
		public CineSliderChangeListener() {}	// so that sub-classes of DicomImageBlackout can cnostruct instances of this inner class
		
		/**
		 * @param	e
		 */
		public void stateChanged(ChangeEvent e) {
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new FrameSelectionChangeEvent(ourEventContext,cineSlider.getValue()-1));
		}
	}

	/**
	 * @param	min		minimum frame number, starting from 1
	 * @param	max		number of frames
	 * @param	value	frame number, starting from 1
	 */
	protected void createCineSliderIfNecessary(int min,int max,int value) {
		if (cineSlider == null || min != cineSlider.getMinimum() || max != cineSlider.getMaximum()) {
			cineSliderControlsPanel.removeAll();
			if (max > min) {
				cineSlider = new JSlider(min,max,value);								// don't leave to default, which is 50 and may be outside range
				cineSlider.setLabelTable(cineSlider.createStandardLabels(max-1,min));	// just label the ends
				cineSlider.setPaintLabels(true);
				//cineSliderControlsPanel.add(new JLabel("Frame index:"));
				cineSliderControlsPanel.add(cineSlider);
				cineSlider.addChangeListener(cineSliderChangeListener);
			}
			else {
				cineSlider=null;	// else single frame so no slider
			}
		}
		if (cineSlider != null) {
			cineSlider.setValue(value);
		}
	}
	
	protected void updateDisplayedFileNumber(int current,int total) {
		if (imagesRemainingLabel != null) {
			imagesRemainingLabel.setText(Integer.toString(current+1)+" of "+Integer.toString(total));
		}
	}

	/**
	 *<p>A class of values for the Burned in Annotation action argument of the {@link com.pixelmed.display.DicomImageBlackout#DicomImageBlackout(String,String [],StatusNotificationHandler,int) DicomImageBlackout()} constructor.</p>
	 */
	public abstract class BurnedInAnnotationFlagAction {
		private BurnedInAnnotationFlagAction() {}
		/**
		 *<p>Leave any existing Burned in Annotation attribute value alone.</p>
		 */
		public static final int LEAVE_ALONE = 1;
		/**
		 *<p>Always remove the Burned in Annotation attribute when the file is saved, without replacing it.</p>
		 */
		public static final int ALWAYS_REMOVE = 2;
		/**
		 *<p>Always remove the Burned in Annotation attribute when the file is saved, only replacing it and using a value of NO when regions have been blacked out.</p>
		 */
		public static final int ADD_AS_NO_IF_CHANGED = 3;
		/**
		 *<p>Always remove the Burned in Annotation attribute when the file is saved, always replacing it with a value of NO,
		 * regardless of whether when regions have been blacked out, such as when visual inspection confirms that there is no
		 * burned in annotation.</p>
		 */
		public static final int ADD_AS_NO_IF_SAVED = 4;
	}
	
	protected int burnedinflag;

	/**
	 *<p>An abstract class for the user of to supply a callback notification method,
	 * supplied as an argument of the {@link com.pixelmed.display.DicomImageBlackout#DicomImageBlackout(String,String [],StatusNotificationHandler,int) DicomImageBlackout()} constructor.</p>
	 */
	public abstract class StatusNotificationHandler {
		protected StatusNotificationHandler() {}
		public static final int WINDOW_CLOSED = 1;
		public static final int CANCELLED = 2;
		public static final int COMPLETED = 3;
		public static final int SAVE_FAILED = 4;
		public static final int UNSAVED_CHANGES = 5;
		public static final int SAVE_SUCCEEDED = 6;
		public static final int READ_FAILED = 7;
		public static final int BLACKOUT_FAILED = 8;
		/**
		 * <p>The callback method when status is updated.</p>
		 *
		 * @param	status		a numeric status
		 * @param	message		a description of the status, and in some cases, affected file names
		 * @param	t			the exception that lead to the status notification, if caused by an exception, else null
		 */
		public abstract void notify(int status,String message,Throwable t);
	}

	/**
	 *<p>A default status notification implementation, which just writes everything to stderr.</p>
	 */
	public class DefaultStatusNotificationHandler extends StatusNotificationHandler {
		public void notify(int status,String message,Throwable t) {
			System.err.println("DicomImageBlackout.DefaultStatusNotificationHandler.notify(): status = "+status);
			System.err.println("DicomImageBlackout.DefaultStatusNotificationHandler.notify(): message = "+message);
			if (t != null) {
				t.printStackTrace(System.err);
			}
		}
	}

	protected StatusNotificationHandler statusNotificationHandler;
	
	/**
	 * <p>Load the named DICOM file and display it in the image panel.</p>
	 *
	 * @param	dicomFileName
	 */
	protected void loadDicomFileOrDirectory(String dicomFileName) {
		try {
			File currentFile = FileUtilities.getFileFromNameInsensitiveToCaseIfNecessary(dicomFileName);
			loadDicomFileOrDirectory(currentFile);
		}
		catch (Exception e) {
			//e.printStackTrace(System.err);
			if (statusNotificationHandler != null) {
				statusNotificationHandler.notify(StatusNotificationHandler.READ_FAILED,"Read failed",e);
			}
			dispose();
		}
	}
	
	/**
	 * <p>Load the named DICOM file and display it in the image panel.</p>
	 *
	 * @param	currentFile
	 */
	protected void loadDicomFileOrDirectory(File currentFile) {
		changesWereMade = false;
		SingleImagePanel.deconstructAllSingleImagePanelsInContainer(multiPanel);
		multiPanel.removeAll();
		multiPanel.revalidate();		// needed because contents have changed
		multiPanel.repaint();			// because if one dimension of the size does not change but the other shrinks, then the old image is left underneath, not overwritten by background (000446)
		//multiPanel.paintImmediately(new Rectangle(multiPanel.getSize(null)));
		{
			Cursor was = getCursor();	// doesn't work ... may get wait cursor since still revalidating
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			try {
System.err.println("DicomImageBlackout.loadDicomFileOrDirectory(): Open "+currentFile);
				currentFileName = currentFile.getAbsolutePath();		// set to what we actually used, used for later save
				DicomInputStream i = new DicomInputStream(currentFile);
				list = new AttributeList();
				list.read(i);
				i.close();
				String useSOPClassUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPClassUID);
				if (SOPClass.isImageStorage(useSOPClassUID)) {
					sImg = new SourceImage(list);
					imagePanel = new SingleImagePanelWithRegionDrawing(sImg,ourEventContext);
					imagePanel.setShowOverlays(burnInOverlays);
					imagePanel.setApplyShutter(false);	// we do not want to "hide" from view any identification information hidden behind shutters (000607)
					addSingleImagePanelToMultiPanelAndEstablishLayout();
					createCineSliderIfNecessary(1,Attribute.getSingleIntegerValueOrDefault(list,TagFromName.NumberOfFrames,1),1);
					setCursor(was);					// needs to be here and not later, else interferes with cursor in repaint() of  SingleImagePanel
					showUIComponents();				// will pack, revalidate, etc, perhaps for the first time
					
					if (previousPersistentDrawingShapes != null) {
						if (previousRows == sImg.getHeight() && previousColumns == sImg.getWidth()) {
							imagePanel.setPersistentDrawingShapes(previousPersistentDrawingShapes);
						}
						else {
							previousRows = 0;
							previousColumns = 0;
							previousPersistentDrawingShapes = null;
						}
					}
				}
				else {
					throw new DicomException("unsupported SOP Class "+useSOPClassUID);
				}
			} catch (Exception e) {
				//e.printStackTrace(System.err);
				if (statusNotificationHandler != null) {
					statusNotificationHandler.notify(StatusNotificationHandler.READ_FAILED,"Read failed",e);
				}
				dispose();
			}
		}
	}

	protected class ApplyActionListener implements ActionListener {
		DicomImageBlackout application;

		public ApplyActionListener(DicomImageBlackout application) {
			this.application=application;
		}
		
		public void actionPerformed(ActionEvent event) {
//System.err.println("DicomImageBlackout.ApplyActionListener.actionPerformed()");
//System.err.println("DicomImageBlackout.ApplyActionListener.actionPerformed(): burnInOverlays = "+application.burnInOverlays);
//System.err.println("DicomImageBlackout.ApplyActionListener.actionPerformed(): useZeroBlackoutValue = "+application.useZeroBlackoutValue);
//System.err.println("DicomImageBlackout.ApplyActionListener.actionPerformed(): usePixelPaddingBlackoutValue = "+application.usePixelPaddingBlackoutValue);
			recordStateOfDrawingShapesForNextFile();
			Cursor was = application.getCursor();
			application.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			if (application.imagePanel != null && application.sImg != null && application.list != null) {
				if (application.imagePanel != null) {
					Vector shapes = application.imagePanel.getPersistentDrawingShapes();
					if (shapes != null || application.burnInOverlays) {
						changesWereMade = true;
						try {
							ImageEditUtilities.blackout(application.sImg,application.list,shapes,application.burnInOverlays,application.usePixelPaddingBlackoutValue,application.useZeroBlackoutValue,0);
							application.sImg = new SourceImage(application.list);	// remake SourceImage, in case blackout() change the AttributeList (e.g., removed overlays)
							application.imagePanel.dirty(application.sImg);
							application.imagePanel.repaint();
						}
						catch (DicomException e) {
							//e.printStackTrace(System.err);
							if (application.statusNotificationHandler != null) {
								application.statusNotificationHandler.notify(StatusNotificationHandler.BLACKOUT_FAILED,"Blackout failed",e);
							}
							application.dispose();
						}
					}
					else {
//System.err.println("DicomImageBlackout.ApplyActionListener.actionPerformed(): no shapes or burning in of overlays to do");
					}
				}
			}
			else {
//System.err.println("DicomImageBlackout.ApplyActionListener.actionPerformed(): no panel or image or list to do");
			}
			application.setCursor(was);
		}
	}

	protected class SaveActionListener implements ActionListener {
		DicomImageBlackout application;

		public SaveActionListener(DicomImageBlackout application) {
			this.application=application;
		}
		
		public void actionPerformed(ActionEvent event) {
//System.err.println("DicomImageBlackout.SaveActionListener.actionPerformed()");
			recordStateOfDrawingShapesForNextFile();
			Cursor was = application.getCursor();
			application.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			boolean success = true;
			try {
				application.sImg.close();		// in case memory-mapped pixel data open; would inhibit Windows rename or copy/reopen otherwise
				application.sImg = null;
			}
			catch (Throwable t) {
//System.err.println("DicomImageBlackout.SaveActionListener.actionPerformed(): unable to rename "+currentFileName+" to "+backupFile+ " - not saving modifications");
				if (application.statusNotificationHandler != null) {
					application.statusNotificationHandler.notify(StatusNotificationHandler.SAVE_FAILED,
						"Save failed - unable to close image - not saving modifications",t);
				}
				success=false;
			}
			File currentFile = new File(currentFileName);
//System.err.println("DicomImageBlackout.SaveActionListener.actionPerformed(): currentFile = "+currentFile);
			File backupFile = new File(currentFileName+".bak");
//System.err.println("DicomImageBlackout.SaveActionListener.actionPerformed(): backupFile = "+backupFile);
			if (success) {
				try {
					FileUtilities.renameElseCopyTo(currentFile,backupFile);
					list.setFileUsedByOnDiskAttributes(backupFile);
				}
				catch (IOException e) {
//System.err.println("DicomImageBlackout.SaveActionListener.actionPerformed(): unable to rename "+currentFileName+" to "+backupFile+ " - not saving modifications");
					if (application.statusNotificationHandler != null) {
						application.statusNotificationHandler.notify(StatusNotificationHandler.SAVE_FAILED,
							"Save failed - unable to rename or copy "+currentFileName+" to "+backupFile+ " - not saving modifications",e);
					}
					success=false;
				}
			}
			if (success) {
				try {
					list.correctDecompressedImagePixelModule();
					if (burnedinflag != BurnedInAnnotationFlagAction.LEAVE_ALONE) {
						list.remove(TagFromName.BurnedInAnnotation);
						if (burnedinflag == BurnedInAnnotationFlagAction.ADD_AS_NO_IF_SAVED
						|| (burnedinflag == BurnedInAnnotationFlagAction.ADD_AS_NO_IF_CHANGED && changesWereMade)) {
							Attribute a = new CodeStringAttribute(TagFromName.BurnedInAnnotation); a.addValue("NO"); list.put(a);
						}
					}
					if (changesWereMade) {
						String existingDeidentificationMethod = Attribute.getSingleStringValueOrNull(list,TagFromName.DeidentificationMethod);
						list.remove(TagFromName.DeidentificationMethod);
						Attribute a = new LongStringAttribute(TagFromName.DeidentificationMethod);
						a.addValue(
							(existingDeidentificationMethod == null ? "" : (existingDeidentificationMethod+"; "))
							+ "Burned in text blacked out" + (application.burnInOverlays ? "; overlays burned in" : ""));
						list.put(a);
					}
					list.removeGroupLengthAttributes();
					list.removeMetaInformationHeaderAttributes();
					list.remove(TagFromName.DataSetTrailingPadding);
					FileMetaInformation.addFileMetaInformation(list,TransferSyntax.ExplicitVRLittleEndian,ourAETitle);
					list.write(currentFile,TransferSyntax.ExplicitVRLittleEndian,true,true);
					backupFile.delete();
					changesWereMade = false;
					if (application.statusNotificationHandler != null) {
						application.statusNotificationHandler.notify(StatusNotificationHandler.SAVE_SUCCEEDED,"Save of "+currentFileName+" succeeded",null);
					}
				}
				catch (DicomException e) {
					//e.printStackTrace(System.err);
					if (application.statusNotificationHandler != null) {
						application.statusNotificationHandler.notify(StatusNotificationHandler.SAVE_FAILED,"Save failed",e);
					}
					backupFile.renameTo(currentFile);
				}
				catch (IOException e) {
					//e.printStackTrace(System.err);
					if (application.statusNotificationHandler != null) {
						application.statusNotificationHandler.notify(StatusNotificationHandler.SAVE_FAILED,"Save failed",e);
					}
					backupFile.renameTo(currentFile);
				}
			}
			loadDicomFileOrDirectory(currentFile);
			application.setCursor(was);
		}
	}
	
	protected class NextActionListener implements ActionListener {
		DicomImageBlackout application;

		public NextActionListener(DicomImageBlackout application) {
			this.application=application;
		}
		
		public void actionPerformed(ActionEvent event) {
//System.err.println("DicomImageBlackout.NextActionListener.actionPerformed()");
			recordStateOfDrawingShapesForNextFile();
			if (changesWereMade) {
				if (application.statusNotificationHandler != null) {
					application.statusNotificationHandler.notify(StatusNotificationHandler.UNSAVED_CHANGES,
						"Changes were applied to "+dicomFileNames[currentFileNumber]+" but were discarded and not saved",null);
				}
			}
			++currentFileNumber;
			if (dicomFileNames != null && currentFileNumber < dicomFileNames.length) {
				updateDisplayedFileNumber(currentFileNumber,dicomFileNames.length);
				loadDicomFileOrDirectory(dicomFileNames[currentFileNumber]);
			}
			else {
				if (application.statusNotificationHandler != null) {
					application.statusNotificationHandler.notify(StatusNotificationHandler.COMPLETED,"Normal completion",null);
				}
				application.dispose();
			}
		}
	}
	
	protected ApplyActionListener applyActionListener;
	protected SaveActionListener saveActionListener;
	protected NextActionListener nextActionListener;

	protected JButton blackoutApplyButton;
	protected JButton blackoutSaveButton;
	protected JButton blackoutNextButton;

	protected class ApplySaveAllActionListener implements ActionListener {
		DicomImageBlackout application;

		public ApplySaveAllActionListener(DicomImageBlackout application) {
			this.application=application;
		}
		
		public void actionPerformed(ActionEvent event) {
//System.err.println("DicomImageBlackout.ApplySaveAllActionListener.actionPerformed()");
			do {
				applyActionListener.actionPerformed(null);
				saveActionListener.actionPerformed(null);
				nextActionListener.actionPerformed(null);
				//blackoutApplyButton.doClick();
				//blackoutSaveButton.doClick();
				//blackoutNextButton.doClick();
			} while (dicomFileNames != null && currentFileNumber < dicomFileNames.length);
		}
	}
	
	protected class CancelActionListener implements ActionListener {
		DicomImageBlackout application;

		public CancelActionListener(DicomImageBlackout application) {
			this.application=application;
		}
		
		public void actionPerformed(ActionEvent event) {
//System.err.println("DicomImageBlackout.CancelActionListener.actionPerformed()");
			if (application.statusNotificationHandler != null) {
				application.statusNotificationHandler.notify(StatusNotificationHandler.CANCELLED,"Cancelled",null);
			}
			application.dispose();
		}
	}
	
	protected class OverlaysChangeListener implements ChangeListener {
		DicomImageBlackout application;
		EventContext eventContext;

		public OverlaysChangeListener(DicomImageBlackout application,EventContext eventContext) {
			this.application=application;
			this.eventContext=eventContext;
		}
		
		public void stateChanged(ChangeEvent e) {
//System.err.println("DicomImageBlackout.OverlaysChangeListener.stateChanged(): event = "+e);
			if (e != null && e.getSource() instanceof JCheckBox) {
				application.burnInOverlays = ((JCheckBox)(e.getSource())).isSelected();
//System.err.println("DicomImageBlackout.OverlaysChangeListener.stateChanged(): burnInOverlays = "+application.burnInOverlays);
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new GraphicDisplayChangeEvent(eventContext,application.burnInOverlays));
			}
		}
	}
	
	protected class ZeroBlackoutValueChangeListener implements ChangeListener {
		DicomImageBlackout application;
		EventContext eventContext;

		public ZeroBlackoutValueChangeListener(DicomImageBlackout application,EventContext eventContext) {
			this.application=application;
			this.eventContext=eventContext;
		}
		
		public void stateChanged(ChangeEvent e) {
//System.err.println("DicomImageBlackout.ZeroBlackoutValueChangeListener.stateChanged(): event = "+e);
			if (e != null && e.getSource() instanceof JCheckBox) {
				application.useZeroBlackoutValue = ((JCheckBox)(e.getSource())).isSelected();
				if (application.useZeroBlackoutValue) {
					application.usePixelPaddingBlackoutValue=false;
					application.usePixelPaddingBlackoutValueCheckBox.setSelected(application.usePixelPaddingBlackoutValue);
				}
//System.err.println("DicomImageBlackout.ZeroBlackoutValueChangeListener.stateChanged(): useZeroBlackoutValue = "+application.useZeroBlackoutValue);
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new GraphicDisplayChangeEvent(eventContext,application.useZeroBlackoutValue));
			}
		}
	}
	
	protected class PixelPaddingBlackoutValueChangeListener implements ChangeListener {
		DicomImageBlackout application;
		EventContext eventContext;

		public PixelPaddingBlackoutValueChangeListener(DicomImageBlackout application,EventContext eventContext) {
			this.application=application;
			this.eventContext=eventContext;
		}
		
		public void stateChanged(ChangeEvent e) {
//System.err.println("DicomImageBlackout.PixelPaddingBlackoutValueChangeListener.stateChanged(): event = "+e);
			if (e != null && e.getSource() instanceof JCheckBox) {
				application.usePixelPaddingBlackoutValue = ((JCheckBox)(e.getSource())).isSelected();
				if (application.usePixelPaddingBlackoutValue) {
					application.useZeroBlackoutValue=false;
					application.useZeroBlackoutValueCheckBox.setSelected(application.useZeroBlackoutValue);
				}
//System.err.println("DicomImageBlackout.PixelPaddingBlackoutValueChangeListener.stateChanged(): usePixelPaddingBlackoutValue = "+application.usePixelPaddingBlackoutValue);
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new GraphicDisplayChangeEvent(eventContext,application.usePixelPaddingBlackoutValue));
			}
		}
	}
	
	protected double getScaleFactorToFitInMaximumAvailable(double useWidth,double useHeight,double maxWidth,double maxHeight) {
		double sx = maxWidth/useWidth;
//System.err.println("DicomImageBlackout.getScaleFactorToFitInMaximumAvailable(): sx = "+sx);
		double sy = maxHeight/useHeight;
//System.err.println("DicomImageBlackout.getScaleFactorToFitInMaximumAvailable(): sy = "+sy);
		// always choose smallest, regardless of whether scaling up or down
		double useScaleFactor = sx < sy ? sx : sy;
//System.err.println("DicomImageBlackout.getScaleFactorToFitInMaximumAvailable(): useScaleFactor = "+useScaleFactor);
		return useScaleFactor;
	}
	
	protected Dimension changeDimensionToFitInMaximumAvailable(Dimension useDimension,Dimension maxDimension,boolean onlySmaller) {
//System.err.println("DicomImageBlackout.changeDimensionToFitInMaximumAvailable(): have dimension "+useDimension);
//System.err.println("DicomImageBlackout.changeDimensionToFitInMaximumAvailable(): maximum dimension "+maxDimension);
		double useWidth = useDimension.getWidth();
		double useHeight = useDimension.getHeight();
		double maxWidth = maxDimension.getWidth();
		double maxHeight = maxDimension.getHeight();
		double useScaleFactor = getScaleFactorToFitInMaximumAvailable(useWidth,useHeight,maxWidth,maxHeight);
		if (useScaleFactor < 1 || !onlySmaller) {
			useWidth = useWidth*useScaleFactor;
			useHeight = useHeight*useScaleFactor;
		}
		useDimension = new Dimension((int)useWidth,(int)useHeight);
//System.err.println("DicomImageBlackout.changeDimensionToFitInMaximumAvailable(): use new dimension "+useDimension);
		return useDimension;
	}
	
	protected Dimension reduceDimensionToFitInMaximumAvailable(Dimension useDimension) {
		return changeDimensionToFitInMaximumAvailable(useDimension,maximumMultiPanelDimension,true);
	}

	protected class CenterMaximumAfterInitialSizeLayout implements LayoutManager {
		public CenterMaximumAfterInitialSizeLayout() {}

		public void addLayoutComponent(String name, Component comp) {}
		
		public void layoutContainer(Container parent) {
			synchronized (parent.getTreeLock()) {
				Insets insets = parent.getInsets();
				int componentCount = parent.getComponentCount();
				Dimension parentSize = parent.getSize();
//System.err.println("CenterMaximumAfterInitialSizeLayout.layoutContainer(): parentSize = "+parentSize);
				
				int sumOfComponentWidths  = 0;
				int sumOfComponentHeights = 0;
				for (int c=0; c<componentCount; ++c) {
					Component component = parent.getComponent(c);
					Dimension componentSize = component.getPreferredSize();
//System.err.println("CenterMaximumAfterInitialSizeLayout.layoutContainer(): component "+c+" preferred size = "+componentSize);
					sumOfComponentWidths  += componentSize.getWidth();
					sumOfComponentHeights += componentSize.getHeight();
				}
				
				int availableWidth  = parentSize.width  - (insets.left+insets.right);
				int availableHeight = parentSize.height - (insets.top+insets.bottom);
				
				int leftOffset = 0;
				int topOffset  = 0;

				boolean useScale = false;
				double useScaleFactor = 1;
				if (sumOfComponentWidths == availableWidth && sumOfComponentHeights <= availableHeight
				 || sumOfComponentWidths <= availableWidth && sumOfComponentHeights == availableHeight) {
					// First time, the sum of either the widths or the heights will equal what
					// is available, since the parent size was derived from calls to minimumLayoutSize()
					// and preferredLayoutSize(), hence no scaling is required or should be performed ...
//System.err.println("CenterMaximumAfterInitialSizeLayout.layoutContainer(): not scaling since once size matches and fits inside");
					leftOffset = (availableWidth  - sumOfComponentWidths ) / 2;
					topOffset  = (availableHeight - sumOfComponentHeights) / 2;
				}
				else {
					// Subsequently, if a resize on the parent has been performed, we should ALWAYS pay
					// attention to it ...
//System.err.println("CenterMaximumAfterInitialSizeLayout.layoutContainer(): scaling");
					useScale = true;
					useScaleFactor = getScaleFactorToFitInMaximumAvailable(sumOfComponentWidths,sumOfComponentHeights,availableWidth,availableHeight);
					leftOffset = (int)((availableWidth  - sumOfComponentWidths*useScaleFactor ) / 2);
					topOffset  = (int)((availableHeight - sumOfComponentHeights*useScaleFactor) / 2);
				}
//System.err.println("CenterMaximumAfterInitialSizeLayout.layoutContainer(): useScale = "+useScale);
//System.err.println("CenterMaximumAfterInitialSizeLayout.layoutContainer(): useScaleFactor = "+useScaleFactor);
//System.err.println("CenterMaximumAfterInitialSizeLayout.layoutContainer(): leftOffset = "+leftOffset);
//System.err.println("CenterMaximumAfterInitialSizeLayout.layoutContainer(): topOffset  = "+topOffset);
				for (int c=0; c<componentCount; ++c) {
					Component component = parent.getComponent(c);
					Dimension componentSize = component.getPreferredSize();
					int w = componentSize.width;
					int h = componentSize.height;
					if (useScale) {
						w = (int)(w * useScaleFactor);
						h = (int)(h * useScaleFactor);
					}
					component.setBounds(leftOffset,topOffset,w,h);
					leftOffset += w;
					topOffset  += h;
				}
			}
		}
		
		public Dimension minimumLayoutSize(Container parent) {
			synchronized (parent.getTreeLock()) {
				Insets insets = parent.getInsets();
				int componentCount = parent.getComponentCount();
				int w = insets.left+insets.right;
				int h = insets.top+insets.bottom;
				for (int c=0; c<componentCount; ++c) {
					Component component = parent.getComponent(c);
					Dimension componentSize = component.getMinimumSize();
					w += componentSize.getWidth();
					h += componentSize.getHeight();
				}
//System.err.println("CenterMaximumAfterInitialSizeLayout.minimumLayoutSize() = "+w+","+h);
				return new Dimension(w,h);
			}
		}
		
		public Dimension preferredLayoutSize(Container parent) {
			synchronized (parent.getTreeLock()) {
				Insets insets = parent.getInsets();
				int componentCount = parent.getComponentCount();
				int w  = insets.left+insets.right;
				int h = insets.top+insets.bottom;
				for (int c=0; c<componentCount; ++c) {
					Component component = parent.getComponent(c);
					Dimension componentSize = component.getPreferredSize();
					w += componentSize.getWidth();
					h += componentSize.getHeight();
				}
//System.err.println("CenterMaximumAfterInitialSizeLayout.preferredLayoutSize() = "+w+","+h);
				return new Dimension(w,h);
			}
		}
		
		public void removeLayoutComponent(Component comp) {}
 	}
	
	protected void addSingleImagePanelToMultiPanelAndEstablishLayout() {
//System.err.println("DicomImageBlackout.addSingleImagePanelToMultiPanelAndEstablishLayout(): start sImg.getDimension() = "+sImg.getDimension());
//System.err.println("DicomImageBlackout.addSingleImagePanelToMultiPanelAndEstablishLayout(): start multiPanel.getPreferredSize() = "+multiPanel.getPreferredSize());
//System.err.println("DicomImageBlackout.addSingleImagePanelToMultiPanelAndEstablishLayout(): start multiPanel.getMinimumSize() = "+multiPanel.getMinimumSize());
//System.err.println("DicomImageBlackout.addSingleImagePanelToMultiPanelAndEstablishLayout(): start multiPanel.getMaximumSize() = "+multiPanel.getMaximumSize());
//System.err.println("DicomImageBlackout.addSingleImagePanelToMultiPanelAndEstablishLayout(): start imagePanel.getPreferredSize() = "+imagePanel.getPreferredSize());
//System.err.println("DicomImageBlackout.addSingleImagePanelToMultiPanelAndEstablishLayout(): start imagePanel.getMinimumSize() = "+imagePanel.getMinimumSize());
//System.err.println("DicomImageBlackout.addSingleImagePanelToMultiPanelAndEstablishLayout(): start imagePanel.getMaximumSize() = "+imagePanel.getMaximumSize());
		// Need to have some kind of layout manager, else imagePanel does not resize when frame is resized by user
		addSingleImagePanelToMultiPanelAndEstablishLayoutWithCenterMaximumAfterInitialSizeLayout();
//System.err.println("DicomImageBlackout.addSingleImagePanelToMultiPanelAndEstablishLayout(): end multiPanel.getPreferredSize() = "+multiPanel.getPreferredSize());
//System.err.println("DicomImageBlackout.addSingleImagePanelToMultiPanelAndEstablishLayout(): end multiPanel.getMinimumSize() = "+multiPanel.getMinimumSize());
//System.err.println("DicomImageBlackout.addSingleImagePanelToMultiPanelAndEstablishLayout(): end multiPanel.getMaximumSize() = "+multiPanel.getMaximumSize());
//System.err.println("DicomImageBlackout.addSingleImagePanelToMultiPanelAndEstablishLayout(): end imagePanel.getPreferredSize() = "+imagePanel.getPreferredSize());
//System.err.println("DicomImageBlackout.addSingleImagePanelToMultiPanelAndEstablishLayout(): end imagePanel.getMinimumSize() = "+imagePanel.getMinimumSize());
//System.err.println("DicomImageBlackout.addSingleImagePanelToMultiPanelAndEstablishLayout(): end imagePanel.getMaximumSize() = "+imagePanel.getMaximumSize());
	}
	
	protected void addSingleImagePanelToMultiPanelAndEstablishLayoutWithCenterMaximumAfterInitialSizeLayout() {
		Dimension useDimension = reduceDimensionToFitInMaximumAvailable(sImg.getDimension());
	
		imagePanel.setPreferredSize(useDimension);
		imagePanel.setMinimumSize(useDimension);	// this is needed to force initial size to be large enough; will be reset to null later to allow resize to change

		multiPanel.setPreferredSize(useDimension);	// this seems to be needed as well
		multiPanel.setMinimumSize(useDimension);	// this seems to be needed as well

		CenterMaximumAfterInitialSizeLayout layout = new CenterMaximumAfterInitialSizeLayout();
		multiPanel.setLayout(layout);
		multiPanel.setBackground(Color.black);
		
		multiPanel.add(imagePanel);
	}


	protected void showUIComponents() {
		remove(mainPanel);					// in case not the first time
		add(mainPanel);
		pack();
		//multiPanel.revalidate();
		validate();
		setVisible(true);
		imagePanel.setMinimumSize(null);	// this is needed to prevent later resize being limited to initial size ...
		multiPanel.setMinimumSize(null);	// this is needed to prevent later resize being limited to initial size ...
	}
	
	protected void buildUIComponents() {
		ourEventContext = new EventContext("Blackout Panel");

		multiPanel = new JPanel();

		JPanel blackoutButtonsPanel = new JPanel();
		// don't set button panel height, else interacts with validate during showUIComponents() needed for no initial image resizing, and cuts off button panel
		//blackoutButtonsPanel.setPreferredSize(new Dimension((int)multiPanel.getPreferredSize().getWidth(),heightWantedForButtons));
		
		blackoutButtonsPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		
		burnInOverlays = false;
		useZeroBlackoutValue = false;
		usePixelPaddingBlackoutValue = true;
		
		JCheckBox keepOverlaysCheckBox = new JCheckBox("Overlays",burnInOverlays);
		keepOverlaysCheckBox.setToolTipText("Toggle whether or not to display overlays, and if displayed burn them into the image and remove them from the header");
		keepOverlaysCheckBox.setMnemonic(KeyEvent.VK_O);
		blackoutButtonsPanel.add(keepOverlaysCheckBox);
		keepOverlaysCheckBox.addChangeListener(new OverlaysChangeListener(this,ourEventContext));
		
		// application scope not local, since change listener needs access to make mutually exclusive with useZeroBlackoutValueCheckBox
		usePixelPaddingBlackoutValueCheckBox = new JCheckBox("Use Padding",usePixelPaddingBlackoutValue);
		usePixelPaddingBlackoutValueCheckBox.setToolTipText("Toggle whether or not to use the pixel padding value for blackout pixels, rather than the default minimum possible pixel value based on signedness and bit depth");
		usePixelPaddingBlackoutValueCheckBox.setMnemonic(KeyEvent.VK_P);
		blackoutButtonsPanel.add(usePixelPaddingBlackoutValueCheckBox);
		usePixelPaddingBlackoutValueCheckBox.addChangeListener(new PixelPaddingBlackoutValueChangeListener(this,ourEventContext));
	
		// application scope not local, since change listener needs access to make mutually exclusive with usePixelPaddingBlackoutValueCheckBox
		useZeroBlackoutValueCheckBox = new JCheckBox("Use Zero",useZeroBlackoutValue);
		useZeroBlackoutValueCheckBox.setToolTipText("Toggle whether or not to use a zero value for blackout pixels, rather than the pixel padding value or default minimum possible pixel value based on signedness and bit depth");
		useZeroBlackoutValueCheckBox.setMnemonic(KeyEvent.VK_Z);
		blackoutButtonsPanel.add(useZeroBlackoutValueCheckBox);
		useZeroBlackoutValueCheckBox.addChangeListener(new ZeroBlackoutValueChangeListener(this,ourEventContext));
		
		blackoutApplyButton = new JButton("Apply");
		blackoutApplyButton.setToolTipText("Blackout the regions");
		blackoutButtonsPanel.add(blackoutApplyButton);
		applyActionListener = new ApplyActionListener(this);
		blackoutApplyButton.addActionListener(applyActionListener);
		
		blackoutSaveButton = new JButton("Save");
		blackoutSaveButton.setToolTipText("Save the blacked-out image");
		blackoutButtonsPanel.add(blackoutSaveButton);
		saveActionListener = new SaveActionListener(this);
		blackoutSaveButton.addActionListener(saveActionListener);
		
		blackoutNextButton = new JButton("Next");
		blackoutNextButton.setToolTipText("Move to the next, skipping this image, if not already saved");
		blackoutButtonsPanel.add(blackoutNextButton);
		nextActionListener = new NextActionListener(this);
		blackoutNextButton.addActionListener(nextActionListener);
		
		JButton blackoutApplySaveAllButton = new JButton("Apply All & Save");
		blackoutApplySaveAllButton.setToolTipText("Blackout the regions and save the blacked-out image for this and all remaining selected images");
		blackoutButtonsPanel.add(blackoutApplySaveAllButton);
		blackoutApplySaveAllButton.addActionListener(new ApplySaveAllActionListener(this));
		
		imagesRemainingLabel = new JLabel("0 of 0");
		blackoutButtonsPanel.add(imagesRemainingLabel);

		JButton blackoutCancelButton = new JButton("Cancel");
		blackoutCancelButton.setToolTipText("Cancel work on this image, if not already saved, and skip all remaining images");
		blackoutButtonsPanel.add(blackoutCancelButton);
		blackoutCancelButton.addActionListener(new CancelActionListener(this));

		cineSliderControlsPanel = new JPanel();
		blackoutButtonsPanel.add(cineSliderControlsPanel);
		cineSliderChangeListener = new CineSliderChangeListener();

		ourFrameSelectionChangeListener = new OurFrameSelectionChangeListener(ourEventContext);	// context needs to match SingleImagePanel to link events

		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,multiPanel,blackoutButtonsPanel);
		splitPane.setOneTouchExpandable(false);
		splitPane.setResizeWeight(splitPaneResizeWeight);

		JLabel helpBar = new JLabel(helpText);

		mainPanel = new Box(BoxLayout.Y_AXIS);
		mainPanel.add(splitPane);
		mainPanel.add(helpBar);
	}

	/**
	 * <p>Opens a window to display the supplied list of DICOM files to allow them to have burned in annotation blacked out.</p>
	 *
	 * <p>Each file will be processed sequentially, with the edited pixel data overwriting the original file.</p>
	 *
	 * @param	title				the string to use in the title bar of the window
	 * @param	dicomFileNames		the list of file names to process, if null a file chooser dialog will be raised
	 * @param	snh					an instance of {@link StatusNotificationHandler StatusNotificationHandler}; if null, a default handler will be used that writes to stderr
	 * @param	burnedinflag		whether or not and under what circumstances to to add/change BurnedInAnnotation attribute; takes one of the values of {@link BurnedInAnnotationFlagAction BurnedInAnnotationFlagAction}
	 * @exception	DicomException
	 */
	public DicomImageBlackout(String title,String dicomFileNames[],StatusNotificationHandler snh,int burnedinflag) {
		super(title);
		this.statusNotificationHandler = snh == null ? new DefaultStatusNotificationHandler() : snh;
		this.burnedinflag = burnedinflag;
		setBackground(Color.lightGray);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				if (statusNotificationHandler != null) {
					statusNotificationHandler.notify(StatusNotificationHandler.WINDOW_CLOSED,"Window closed",null);
				}
				dispose();
			}
		});
		
		buildUIComponents();
		
		if (dicomFileNames == null || dicomFileNames.length == 0) {
			JFileChooser chooser = new JFileChooser();
			if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
				String chosenDicomFileNames[] = { chooser.getSelectedFile().getAbsolutePath() };
				dicomFileNames = chosenDicomFileNames;
			}
		}
		if (dicomFileNames != null && dicomFileNames.length > 0) {
			this.dicomFileNames = dicomFileNames;
			currentFileNumber = 0;
			updateDisplayedFileNumber(currentFileNumber,dicomFileNames.length);
			loadDicomFileOrDirectory(dicomFileNames[currentFileNumber]);
		}
	}

	public void deconstruct() {
//System.err.println("DicomImageBlackout.deconstruct()");
		// avoid "listener leak"
		if (ourFrameSelectionChangeListener != null) {
//System.err.println("DicomImageBlackout.deconstruct(): removing ourFrameSelectionChangeListener");
			ApplicationEventDispatcher.getApplicationEventDispatcher().removeListener(ourFrameSelectionChangeListener);
			ourFrameSelectionChangeListener=null;
		}
		if (multiPanel != null) {
//System.err.println("DicomImageBlackout.deconstruct(): call deconstructAllSingleImagePanelsInContainer in case any listeners hanging around");
			SingleImagePanel.deconstructAllSingleImagePanelsInContainer(multiPanel);
		}
	}
	
	public void dispose() {
//System.err.println("DicomImageBlackout.dispose()");
		deconstruct();		// just in case wasn't already called, and garbage collection occurs
		super.dispose();
	}
	
	protected void finalize() throws Throwable {
//System.err.println("DicomImageBlackout.finalize()");
		deconstruct();		// just in case wasn't already called, and garbage collection occurs
		super.finalize();
	}

	/**
	 * <p>The method to invoke the application.</p>
	 *
	 * @param	arg	optionally, a list of files; if absent a file dialog is presented
	 */
	public static void main(String arg[]) {
		new DicomImageBlackout("Dicom Image Blackout",arg,null,BurnedInAnnotationFlagAction.ADD_AS_NO_IF_SAVED);
	}
}


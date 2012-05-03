/* Copyright (c) 2001-2010, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import java.util.*;
import java.io.*;

import com.pixelmed.utils.JTreeWithAdditionalKeyStrokeActions;

/**
 * @author	dclunie
 */
public class DicomDirectoryBrowser {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/DicomDirectoryBrowser.java,v 1.20 2010/01/31 11:59:06 dclunie Exp $";

	private JTree tree;
	private DicomDirectory treeModel;
	private String parentFilePath;

	private HashSet defaultExcludeList;
	private HashSet patientExcludeList;
	private HashSet studyExcludeList;
	private HashSet seriesExcludeList;
	private HashSet imageExcludeList;
	private HashSet srExcludeList;

	/**
	 * @param	list
	 * @param	parentFilePath
	 * @exception	DicomException
	 */
	public DicomDirectoryBrowser(AttributeList list,String parentFilePath) throws DicomException {
//long startTime = System.currentTimeMillis();
		this.parentFilePath=parentFilePath;
		treeModel=new DicomDirectory(list);
		tree=new JTreeWithAdditionalKeyStrokeActions(treeModel);
		tree.addTreeSelectionListener(buildTreeSelectionListenerToDoSomethingWithSelectedFiles(parentFilePath));
		tree.addMouseListener(buildMouseListenerToDetectDoubleClickEvents());
//long currentTime = System.currentTimeMillis();
//System.err.println("DicomDirectoryBrowser(): took = "+(currentTime-startTime)+" ms");
	}

	/**
	 * @param	list
	 * @param	parentFilePath
	 * @param	treeBrowserScrollPane
	 * @param	attributeBrowserScrollPane
	 * @exception	DicomException
	 */
	public DicomDirectoryBrowser(AttributeList list,String parentFilePath,JScrollPane treeBrowserScrollPane,JScrollPane attributeBrowserScrollPane) throws DicomException {
//long startTime = System.currentTimeMillis();
		this.parentFilePath=parentFilePath;
		treeModel=new DicomDirectory(list);
		tree=new JTreeWithAdditionalKeyStrokeActions(treeModel);
		treeBrowserScrollPane.setViewportView(tree);
		tree.addTreeSelectionListener(buildTreeSelectionListenerToDoSomethingWithSelectedFiles(parentFilePath));
		createExcludeLists();
		tree.addTreeSelectionListener(buildTreeSelectionListenerToDisplayAttributesOfSelectedRecord(attributeBrowserScrollPane));
		tree.addMouseListener(buildMouseListenerToDetectDoubleClickEvents());
//long currentTime = System.currentTimeMillis();
//System.err.println("DicomDirectoryBrowser(): took = "+(currentTime-startTime)+" ms");
	}

	/**
	 * @param	list
	 * @param	parentFilePath
	 * @param	frame
	 * @exception	DicomException
	 */
	public DicomDirectoryBrowser(AttributeList list,String parentFilePath,JFrame frame) throws DicomException {
//long startTime = System.currentTimeMillis();
		Attribute a = list.get(TagFromName.FileSetID);
		if (a != null) {
			String descriptors[] = a.getStringValues();
			if (descriptors != null && descriptors.length > 0) frame.setTitle(descriptors[0]);
		}
		this.parentFilePath=parentFilePath;
		treeModel=new DicomDirectory(list);
		tree=new JTreeWithAdditionalKeyStrokeActions(treeModel);
		
		Container content = frame.getContentPane();
		JScrollPane treeScrollPane = new JScrollPane(tree);
		JScrollPane attributeBrowserScrollPane = new JScrollPane();
		//JSplitPane splitPane=new JSplitPane(JSplitPane.VERTICAL_SPLIT,treeScrollPane,attributeBrowserScrollPane);
		//splitPane.setResizeWeight(0.7);
		//content.add(splitPane);
		attributeBrowserScrollPane.setPreferredSize(new Dimension(0,76));	// width is irrelevant
		JPanel panel=new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(treeScrollPane,BorderLayout.CENTER);
		panel.add(attributeBrowserScrollPane,BorderLayout.SOUTH);
		content.add(panel);

		tree.addTreeSelectionListener(buildTreeSelectionListenerToDoSomethingWithSelectedFiles(parentFilePath));
		createExcludeLists();
		tree.addTreeSelectionListener(buildTreeSelectionListenerToDisplayAttributesOfSelectedRecord(attributeBrowserScrollPane));
		tree.addMouseListener(buildMouseListenerToDetectDoubleClickEvents());
//long currentTime = System.currentTimeMillis();
//System.err.println("DicomDirectoryBrowser(): took = "+(currentTime-startTime)+" ms");
	}
	
	//protected void setCellRenderer(TreeCellRenderer cellRenderer) {
	//	tree.setCellRenderer(cellRenderer);
	//}
	
	/**
	 * @param	font
	 */
	protected void setFont(Font font) {
		tree.setFont(font);
	}

	/**
	 * @param	parentFilePath
	 */
	protected TreeSelectionListener buildTreeSelectionListenerToDoSomethingWithSelectedFiles(final String parentFilePath) {
		return new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent tse) {
				TreePath tp = tse.getNewLeadSelectionPath();
				if (tp != null) {
//System.err.println("DicomDirectoryBrowser.TreeSelectionListener.valueChanged(): Selected: "+tp.getLastPathComponent());
//System.err.println("DicomDirectoryBrowser.TreeSelectionListener.valueChanged(): Selected: "+tp);
					Object lastPathComponent = tp.getLastPathComponent();
					if (lastPathComponent instanceof DicomDirectoryRecord) {
						Vector names=DicomDirectory.findAllContainedReferencedFileNames((DicomDirectoryRecord)lastPathComponent,parentFilePath);
						if (names != null) doSomethingWithSelectedFiles(names);
					}
				}
			}
		};
	}
	
	/**
	 * @param	attributeBrowserScrollPane
	 */
	protected TreeSelectionListener buildTreeSelectionListenerToDisplayAttributesOfSelectedRecord(final JScrollPane attributeBrowserScrollPane) {
		return new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent tse) {
				TreePath tp = tse.getNewLeadSelectionPath();
				if (tp != null) {
//System.err.println("Selected: "+tp.getLastPathComponent());
//System.err.println("Selected: "+tp);
					Object lastPathComponent = tp.getLastPathComponent();
					if (lastPathComponent instanceof DicomDirectoryRecord) {
						DicomDirectoryRecord dirRecord = (DicomDirectoryRecord)lastPathComponent;
						HashSet includeList = null;
						HashSet excludeList = chooseExcludeList(dirRecord);
						AttributeListTableBrowser table = new AttributeListTableBrowser(dirRecord.getAttributeList(),includeList,excludeList);
						table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);		// Otherwise horizontal scroll doesn't work
						table.setColumnWidths();
						attributeBrowserScrollPane.setViewportView(table);
					}
				}
			}
		};
	}
	

	/**
	 */
	protected MouseListener buildMouseListenerToDetectDoubleClickEvents() {
		return new MouseAdapter() {
			public void mousePressed(MouseEvent me) {
				if (me != null) {
					if (me.getClickCount() == 2) {
//System.err.println("DicomDirectoryBrowser.MouseAdapter.mousePressed(): Detected double-click");
						doSomethingMoreWithWhateverWasSelected();
					}
				}
			}
		};
	}


	/**
	 * @param	dirRecord
	 */
	protected HashSet chooseExcludeList(DicomDirectoryRecord dirRecord) {
		HashSet excludeList=null;
		AttributeList list = dirRecord.getAttributeList();
		String directoryRecordType = Attribute.getSingleStringValueOrNull(list,TagFromName.DirectoryRecordType);
		if (directoryRecordType == null) {
			excludeList = defaultExcludeList;
		}
		else if (directoryRecordType.equals("PATIENT")) {
			excludeList = patientExcludeList;
		}
		else if (directoryRecordType.equals("STUDY")) {
			excludeList = studyExcludeList;
		}
		else if (directoryRecordType.equals("SERIES")) {
			excludeList = seriesExcludeList;
		}
		else if (directoryRecordType.equals("IMAGE")) {
			excludeList = imageExcludeList;
		}
		else if (directoryRecordType.equals("SR DOCUMENT")) {
			excludeList = srExcludeList;
		}
		return excludeList;
	}
	
	/***/
	protected void createExcludeLists() {
		defaultExcludeList = new HashSet();
		defaultExcludeList.add(TagFromName.OffsetOfTheNextDirectoryRecord);
		defaultExcludeList.add(TagFromName.RecordInUseFlag);
		defaultExcludeList.add(TagFromName.OffsetOfReferencedLowerLevelDirectoryEntity);
		defaultExcludeList.add(TagFromName.DirectoryRecordType);
		
		patientExcludeList =  new HashSet(defaultExcludeList);
		
		studyExcludeList =  new HashSet(defaultExcludeList);
		
		seriesExcludeList =  new HashSet(defaultExcludeList);
		
		imageExcludeList =  new HashSet(defaultExcludeList);
		//imageExcludeList.add(TagFromName.ReferencedFileID);
		//imageExcludeList.add(TagFromName.ReferencedSOPClassUIDInFile);
		//imageExcludeList.add(TagFromName.ReferencedSOPInstanceUIDInFile);
		//imageExcludeList.add(TagFromName.ReferencedTransferSyntaxUIDInFile);
		
		srExcludeList =  new HashSet(defaultExcludeList);
	}

	/***/
	public DicomDirectory getDicomDirectory() {
		treeModel.getMapOfSOPInstanceUIDToReferencedFileName(this.parentFilePath);	// initializes map using parentFilePath, ignore return value
		return treeModel;
	}

	/***/
	public String getParentFilePath() { return parentFilePath; }

	// Override these next methods in derived classes to do something useful

	/**
	 * @param	paths
	 */
	protected void doSomethingWithSelectedFiles(Vector paths) {
		if (paths != null) {
			Iterator i = paths.iterator();
			while (i.hasNext()) {
				System.err.println((String)i.next());
			}
		}
	}

	/**
	 */
	protected void doSomethingMoreWithWhateverWasSelected() {
		System.err.println("DicomDirectoryBrowser.doSomethingMoreWithWhateverWasSelected(): Double click on current selection");
	}

	/**
	 * @param	arg
	 */
	public static void main(String arg[]) {
		AttributeList list = new AttributeList();
		try {
			final String suppliedFileName=arg[0];
			final String parentFilePath = new File(suppliedFileName).getParent();

			System.err.println("test reading DICOMDIR");
//long startTime = System.currentTimeMillis();
			list.read(suppliedFileName);
//long currentTime = System.currentTimeMillis();
//System.err.println("read took = "+(currentTime-startTime)+" ms");
//startTime=currentTime;
			final JFrame frame = new JFrame();
			frame.setSize(400,800);
			frame.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					frame.dispose();
					System.exit(0);
				}
			});

			System.err.println("building tree");
			DicomDirectoryBrowser tree = new DicomDirectoryBrowser(list,parentFilePath,frame);
//currentTime = System.currentTimeMillis();
//System.err.println("tree building took = "+(currentTime-startTime)+" ms");
//startTime=currentTime;
			System.err.println("display tree");
			frame.setVisible(true);
//currentTime = System.currentTimeMillis();
//System.err.println("tree display took = "+(currentTime-startTime)+" ms");
//startTime=currentTime;
		} catch (Exception e) {
			e.printStackTrace(System.err);
			System.exit(0);
		}
	}
}







/* Copyright (c) 2001-2009, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import com.pixelmed.utils.MessageLogger;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import javax.swing.border.Border;

import javax.swing.text.Caret;
import javax.swing.text.DefaultCaret;

/**
 * <p>A class to write log and status messages to a scrolling text area in a dialog box.</p>
 *
 * @author      dclunie
 */
public class DialogMessageLogger implements MessageLogger {

	protected JDialog outputDialog;
	protected JScrollPane outputScrollPane;
	protected JTextArea outputTextArea;

	protected class ClearActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			//outputTextArea.replaceRange(null,0,0);
			outputTextArea.setText("");
			outputScrollPane.repaint();
		}
	}

	/**
	 * <p>Construct a logger and make it immediately visible.</p>
	 *
	 * @param	titleMessage			for the title bar of the dialog box
	 * @param	width					initial width of the resizeable dialog box
	 * @param	height					initial height of the resizeable dialog box
	 * @param	exitApplicationOnClose	if true, when the dialog box is closed (X-d out), will exit the application with success status
	 */
	public DialogMessageLogger(String titleMessage,int width,int height,boolean exitApplicationOnClose) {
		this(titleMessage,width,height,exitApplicationOnClose,true);
	}

	/**
	 * <p>Construct a logger.</p>
	 *
	 * @param	titleMessage			for the title bar of the dialog box
	 * @param	width					initial width of the resizeable dialog box
	 * @param	height					initial height of the resizeable dialog box
	 * @param	exitApplicationOnClose	if true, when the dialog box is closed (X-d out), will exit the application with success status
	 * @param	visible					if true, will be made visible after construction
	 */
	public DialogMessageLogger(String titleMessage,int width,int height,boolean exitApplicationOnClose,boolean visible) {
		Border panelBorder = BorderFactory.createEtchedBorder();
		
		outputTextArea = new JTextArea();
		{
			// if one does not do this, the default behavior is scroll to end when append() on event disptaching thread, but not from other threads
			Caret caret = outputTextArea.getCaret();
			if (caret != null && caret instanceof DefaultCaret) {
				((DefaultCaret)caret).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);	// i.e. jump to end after append() regardless of whether in same thread or not
			}
		}
		outputScrollPane = new JScrollPane(outputTextArea);
		outputScrollPane.setBorder(panelBorder);
		
		outputDialog = new JDialog();
		//outputDialog.setSize(width,height);
		outputDialog.setPreferredSize(new Dimension(width,height));
		outputDialog.setTitle(titleMessage);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		buttonPanel.setBorder(panelBorder);

		JButton clearButton = new JButton("Clear");
		clearButton.setToolTipText("Clear log");
		buttonPanel.add(clearButton);
		clearButton.addActionListener(new ClearActionListener());

		Container mainPanel = outputDialog.getContentPane();
		{
			GridBagLayout mainPanelLayout = new GridBagLayout();
			mainPanel.setLayout(mainPanelLayout);
			{
				GridBagConstraints outputScrollPaneConstraints = new GridBagConstraints();
				outputScrollPaneConstraints.gridx = 0;
				outputScrollPaneConstraints.gridy = 0;
				outputScrollPaneConstraints.weightx = 1;
				outputScrollPaneConstraints.weighty = 1;
				outputScrollPaneConstraints.fill = GridBagConstraints.BOTH;
				mainPanelLayout.setConstraints(outputScrollPane,outputScrollPaneConstraints);
				mainPanel.add(outputScrollPane);
			}
			{
				GridBagConstraints buttonPanelConstraints = new GridBagConstraints();
				buttonPanelConstraints.gridx = 0;
				buttonPanelConstraints.gridy = 1;
				buttonPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
				mainPanelLayout.setConstraints(buttonPanel,buttonPanelConstraints);
				mainPanel.add(buttonPanel);
			}
		}
		
		outputDialog.pack();
		outputDialog.setVisible(visible);
		
		if (exitApplicationOnClose) {
			outputDialog.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
//System.err.println("DialogMessageLogger.WindowAdapter.windowClosing()");
					//dispose();
					System.exit(0);
				}
			});
		}
	}
	
	public void setVisible(boolean visible) {
		outputDialog.setVisible(visible);
	}
	
	public void send(String message) {
		outputTextArea.append(message);
	}
		
	public void sendLn(String message) {
		outputTextArea.append(message+"\n");
	}
}


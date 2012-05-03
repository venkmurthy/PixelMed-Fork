/* Copyright (c) 2001-2005, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import javax.swing.JLabel; 

import com.pixelmed.event.ApplicationEventDispatcher;
import com.pixelmed.event.Event; 
import com.pixelmed.event.SelfRegisteringListener; 
import com.pixelmed.display.event.StatusChangeEvent; 
import com.pixelmed.display.event.WindowCenterAndWidthChangeEvent; 

class StatusBarManager {
	/***/
	private OurStatusChangeListener ourStatusChangeListener;

	class OurStatusChangeListener extends SelfRegisteringListener {
		private JLabel statusBar;
		
		public OurStatusChangeListener(JLabel statusBar) {
			super("com.pixelmed.display.event.StatusChangeEvent",null/*Any EventContext*/);
//System.err.println("StatusBarManager.OurStatusChangeListener():");
			this.statusBar=statusBar;
		}
		
		/**
		 * @param	e
		 */
		public void changed(Event e) {
			StatusChangeEvent sce = (StatusChangeEvent)e;
//System.err.println("StatusBarManager.OurStatusChangeListener.changed(): new status message is:"+sce.getStatusMessage());
			statusBar.setText(sce.getStatusMessage());
			statusBar.revalidate();
			statusBar.paintImmediately(statusBar.getVisibleRect());
		}
	}
	
	/***/
	private OurWindowCenterAndWidthChangeListener ourWindowCenterAndWidthChangeListener;

	class OurWindowCenterAndWidthChangeListener extends SelfRegisteringListener {
	
		public OurWindowCenterAndWidthChangeListener() {
			super("com.pixelmed.display.event.WindowCenterAndWidthChangeEvent",null/*Any EventContext*/);
//System.err.println("StatusBarManager.OurWindowCenterAndWidthChangeListener():");
		}
		
		/**
		 * @param	e
		 */
		public void changed(Event e) {
			WindowCenterAndWidthChangeEvent wcwe = (WindowCenterAndWidthChangeEvent)e;
//System.err.println("StatusBarManager.OurWindowCenterAndWidthChangeListener.changed(): event="+wcwe);
			StringBuffer sbuf = new StringBuffer();
			sbuf.append("C ");
			sbuf.append(wcwe.getWindowCenter());
			sbuf.append(" W ");
			sbuf.append(wcwe.getWindowWidth());
			
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent(sbuf.toString()));
		}
	}


	/***/
	private JLabel statusBar;
	
	public StatusBarManager(String initialMessage) {
		// The width of the initial text seems to set the (preferred ?) size to be when the packing is done, so use blanks, the setText()
		statusBar = new JLabel("                                                                                                                      ");
		statusBar.setText(initialMessage);
		ourStatusChangeListener = new OurStatusChangeListener(statusBar);			// registers itself with application dispatcher
		ourWindowCenterAndWidthChangeListener = new OurWindowCenterAndWidthChangeListener();	// registers itself with application dispatcher
	}
	
	public JLabel getStatusBar() {
		return statusBar;
	}

}


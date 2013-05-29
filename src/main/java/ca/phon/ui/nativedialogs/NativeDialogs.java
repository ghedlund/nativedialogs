/*
 * Phon - An open source tool for research in phonology.
 * Copyright (C) 2008 The Phon Project, Memorial University <http://phon.ling.mun.ca>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ca.phon.ui.nativedialogs;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import ca.phon.ui.dialogs.JFontDialog;

/**
 * Dialog wrapper methods.  These methods will use
 * native Cocoa dialogs/sheets in mac and the default
 * Java dialogs for other operating systems.
 *
 */
public class NativeDialogs {
	
	private final static Logger LOGGER = Logger.getLogger(NativeDialogs.class.getName());
	
	/** The native library name */
	private final static String _PHONNATIVE_LIB_NAME = "nativedialogs";
	
	// assume library is there unless we get an exception
	private static boolean libraryFound = false;
	
	static {
		try {
			NativeUtilities.loadLibrary(_PHONNATIVE_LIB_NAME);
			libraryFound = true;
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Unable to load native dialogs library, will fallback to Swing", e);
		}
	}
	
	/** 
	 * Method for displaying the native open file dialog.
	 * 
	 * @param parentWindow unsed in Win32 but essential for Cocoa
	 * @param listener the listener to call when an event occurs
	 * @param startDir the starting directory
	 * @param defaultExt the default extension
	 * @param filters the array of filters.
	 * @param title the title of the window
	 */
	private native static void nativeBrowseForFile(
			Window parentWindow,
			NativeDialogListener listener,
			String startDir,
			String defaultExt,
			FileFilter[] filters,
			String title);
	
	/** 
	 * Method for displaying the swing open file dialog.
	 * 
	 * @param parentWindow unsed in Win32 but essential for Cocoa
	 * @param listener the listener to call when an event occurs
	 * @param startDir the starting directory
	 * @param defaultExt the default extension
	 * @param filters the array of filters.
	 * @param title the title of the window
	 */
	private static void swingBrowseForFile(
			Window parentWindow,
			NativeDialogListener listener,
			String startDir,
			String defaultExt,
			FileFilter[] filters,
			String title) {
		
		final Runnable task = new BrowseForFileTask(
				parentWindow, listener, startDir, defaultExt,
				filters, title);
		
		if(SwingUtilities.isEventDispatchThread()) {
			// don't block awt thread
			task.run();
		} else {
			SwingUtilities.invokeLater(task);
		}
	}
	
	private static class BrowseForFileTask implements Runnable {
		private Window parentWindow;
		private NativeDialogListener listener;
		private String startDir;
		private String defaultExt;
		private FileFilter[] filters;
		private String title;
		
		public BrowseForFileTask(Window parentWindow,
				NativeDialogListener listener,
				String startDir,
				String defaultExt,
				FileFilter[] filters,
				String title) {
			this.parentWindow = parentWindow;
			this.listener = listener;
			this.startDir = startDir;
			this.defaultExt = defaultExt;
			this.filters = filters;
			this.title = title;
		}
		
		@Override
		public void run() {
			JFileChooser chooser = new JFileChooser();
			
			if(startDir != null)
				chooser.setCurrentDirectory(new File(startDir));
			chooser.setDialogTitle(title);
			chooser.setMultiSelectionEnabled(false);
			
			if(filters != null && filters.length > 0)
				chooser.setFileFilter(filters[0]);
			
			NativeDialogEvent evt = null;
			int retVal = chooser.showOpenDialog(parentWindow);
			if(retVal == JFileChooser.APPROVE_OPTION) {
				// create the dialog event
				evt = new NativeDialogEvent(NativeDialogEvent.OK_OPTION, chooser.getSelectedFile());
			} else {
				evt = new NativeDialogEvent(NativeDialogEvent.CANCEL_OPTION, null);
			}
			
			listener.nativeDialogEvent(evt);
		}
	}
	
	/** 
	 * Method for displaying the open file dialog.
	 * 
	 * @param parentWindow unsed in Win32 but essential for Cocoa
	 * @param listener the listener to call when an event occurs
	 * @param startDir the starting directory
	 * @param defaultExt the default extension
	 * @param filters the array of filters.
	 * @param title the title of the window
	 */
	public static void browseForFile(
			Window parentWindow,
			NativeDialogListener listener,
			String startDir,
			String defaultExt,
			FileFilter[] filters,
			String title) {
//		if(startDir == null) {
//			startDir = PhonUtilities.getPhonWorkspace().getAbsolutePath();
//		}
		if(OSInfo.isMacOs() && libraryFound) {
			nativeBrowseForFile(
					parentWindow, listener, 
					startDir,
					defaultExt, filters, title);
		} else {
			swingBrowseForFile(
					parentWindow, listener, startDir,
					defaultExt, filters, title);
		}
	}
	
	/**
	 * Method for displaying the native open dir dialog.
	 * 
	 * @param parentWindows unused for Win32 but essential for Cocoa
	 * @param listener the listener to call when an event occurs
	 * @param startDir the starting Directory
	 * @param title the title of the window
	 */
	private native static void nativeBrowseForDirectory(
			Window parentWindow,
			NativeDialogListener listener,
			String startDir,
			String title);
	
	private static void swingBrowseForDirectory(
			Window parentWindow,
			NativeDialogListener listener,
			String startDir,
			String title) {
		final Runnable task = new BrowseForDirectoryTask(
				parentWindow, listener,
				startDir, title);
		
		if(SwingUtilities.isEventDispatchThread()) {
			// don't block awt thread
			task.run();
		} else {
			SwingUtilities.invokeLater(task);
		}
	}
	
	private static class BrowseForDirectoryTask implements Runnable {
		private Window parentWindow;
		private NativeDialogListener listener;
		private String startDir;
		private String title;
		
		public BrowseForDirectoryTask(
				Window parentWindow,
				NativeDialogListener listener,
				String startDir,
				String title) {
			this.parentWindow = parentWindow;
			this.listener = listener;
			this.startDir = startDir;
			this.title = title;
		}
		
		@Override
		public void run() {
			JFileChooser chooser = new JFileChooser();
			
			if(startDir != null)
				chooser.setCurrentDirectory(new File(startDir));
			
			chooser.setDialogTitle(title);
			chooser.setMultiSelectionEnabled(false);
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			
			NativeDialogEvent evt = null;
			int retVal = chooser.showOpenDialog(parentWindow);
			if(retVal == JFileChooser.APPROVE_OPTION) {
				// create the dialog event
				evt = new NativeDialogEvent(NativeDialogEvent.OK_OPTION, chooser.getSelectedFile());
			} else {
				evt = new NativeDialogEvent(NativeDialogEvent.CANCEL_OPTION, null);
			}
			
			listener.nativeDialogEvent(evt);
		}
	}
	
	/**
	 * Method for displaying the native open dir dialog.
	 * 
	 * @param parentWindows unused for Win32 but essential for Cocoa
	 * @param listener the listener to call when an event occurs
	 * @param startDir the starting Directory
	 * @param title the title of the window
	 */
	public static void browseForDirectory(
			Window parentWindow,
			NativeDialogListener listener,
			String startDir,
			String title) {
//		if(startDir == null) {
//			startDir = PhonUtilities.getPhonWorkspace().getAbsolutePath();
//		}
		
		if(libraryFound) {
			nativeBrowseForDirectory(
					parentWindow, listener,
					startDir, title);
		} else {
			swingBrowseForDirectory(
					parentWindow, listener,
					startDir, title);
		}
	}
	
	/**
	 * Task to browse for a phon project.
	 * 
	 */
	private static class BrowseForProjectTask implements Runnable {

		private Window parentWindow;
		private NativeDialogListener listener;
		private String startDir;
		private String title;
		
		public BrowseForProjectTask(
				Window parentWindow,
				NativeDialogListener listener,
				String startDir,
				String title) {
			this.parentWindow = parentWindow;
			this.listener = listener;
			this.startDir = startDir;
			this.title = title;
		}
		
		@Override
		public void run() {
			JFileChooser chooser = new JFileChooser();
			
			if(startDir != null)
				chooser.setCurrentDirectory(new File(startDir));
			
			chooser.setDialogTitle(title);
			chooser.setMultiSelectionEnabled(false);
			chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			
				javax.swing.filechooser.FileFilter projectFilter = new javax.swing.filechooser.FileFilter() {
	
					@Override
					public boolean accept(File file) {
						boolean isPhonProject = false;
						
						if(file.isDirectory()) {
							if(!OSInfo.isMacOs()) {
								// accept all directories on non-mac systems
								isPhonProject = true;
							} else {
								File projectXml = new File(file, "project.xml");
								if(!projectXml.exists())
									isPhonProject = false;
								else
									isPhonProject = true;
							}
						} else {
							if(file.getName().endsWith(".phon")) {
								isPhonProject = true;
							}
						}
						
						return isPhonProject;
					}
	
					@Override
					public String getDescription() {
						return "Phon Projects";
					}
				};
				chooser.setFileFilter(projectFilter);
			
			
			NativeDialogEvent evt = null;
			int retVal = chooser.showOpenDialog(parentWindow);
			if(retVal == JFileChooser.APPROVE_OPTION) {
				// create the dialog event
				evt = new NativeDialogEvent(NativeDialogEvent.OK_OPTION, chooser.getSelectedFile());
			} else {
				evt = new NativeDialogEvent(NativeDialogEvent.CANCEL_OPTION, null);
			}
			
			listener.nativeDialogEvent(evt);
		}
		
	}
	
	/**
	 * Show a dialog for selecting phon project.  Phon project are
	 * either a directory with a project.xml file inside, or a file
	 * with a .phon extension.
	 * 
	 * @param parentWindow
	 * @param listener
	 * @param startDir
	 * @param title
	 */
	public static void browseForProject(
			Window parentWindow,
			NativeDialogListener listener,
			String startDir,
			String title)
	{
//		if(startDir == null) {
//			startDir = PhonUtilities.getPhonWorkspace().getAbsolutePath();
//		}
		
		// no native version yet
		swingBrowseForProject(parentWindow, listener, startDir, title);
	}
	
	private static void swingBrowseForProject(
			Window parentWindow,
			NativeDialogListener listener,
			String startDir,
			String title)
	{
		final Runnable task = new BrowseForProjectTask(parentWindow, listener, 
				startDir, title);
		
		if(SwingUtilities.isEventDispatchThread()) 
			task.run();
		else
			SwingUtilities.invokeLater(task);
	}
	
	public static String browseForProjectBlocking(
			Window parentWindow,
			String startDir,
			String title)
	{
	
//		if(startDir == null) {
//			startDir = PhonUtilities.getPhonWorkspace().getAbsolutePath();
//		}
		
		MessageWaitListener mwl = new MessageWaitListener();
		browseForProject(parentWindow, mwl, startDir, title);
		mwl.waitLoop();
		
		if(mwl.getEvent().getDialogResult() == NativeDialogEvent.OK_OPTION)
			return mwl.getEvent().getDialogData().toString();
		else
			return null;
	}
	
	/**
	 * Method for displaying a save file dialog.
	 * 
	 * @param parentWindow unused for Win32 but essential for Cocoa
	 * @param listener the listener to call when an event occurs
	 * @param startDir the starting directory
	 * @param title the title of the window
	 * @param defaultExt the default file extension
	 */
	private native static void nativeShowSaveFileDialog(
			Window parentWindow,
			NativeDialogListener listener,
			String startDir,
			String fileName,
			String defaultExt,
			FileFilter[] filters,
			String title);
	
	private static void swingShowSaveFileDialog(
			Window parentWindow,
			NativeDialogListener listener,
			String startDir,
			String fileName,
			String defaultExt,
			FileFilter[] filters,
			String title) {
		final Runnable task = 
				new ShowSaveFileTask(
						parentWindow, listener,
						startDir, fileName, defaultExt, filters, title);
		
		if(SwingUtilities.isEventDispatchThread()) {
			// don't block awt thread
			task.run();
		} else {
			SwingUtilities.invokeLater(task);
		}
	}
	
	private static class ShowSaveFileTask implements Runnable {
		private Window parentWindow;
		private NativeDialogListener listener;
		private String startDir;
		private String fileName;
		private String defaultExt;
		private FileFilter[] filters;
		private String title;
		
		public ShowSaveFileTask(Window parentWindow,
				NativeDialogListener listener,
				String startDir,
				String fileName,
				String defaultExt,
				FileFilter[] filters,
				String title) {
			this.parentWindow = parentWindow;
			this.listener = listener;
			this.startDir = startDir;
			this.defaultExt = defaultExt;
			this.filters = filters;
			this.title = title;
		}
		
		@Override
		public void run() {
			JFileChooser chooser = new JFileChooser();
			
			if(startDir != null)
				chooser.setCurrentDirectory(new File(startDir));
			
			chooser.setDialogTitle(title);
			chooser.setMultiSelectionEnabled(false);
			
			if(filters.length > 0)
				chooser.setFileFilter(filters[0]);
			
			if(fileName != null) {
				File selectedFile = new File(startDir, fileName);
				chooser.setSelectedFile(selectedFile);
			}
			
			NativeDialogEvent evt = null;
			int retVal = chooser.showSaveDialog(parentWindow);
			if(retVal == JFileChooser.APPROVE_OPTION) {
				// create the dialog event
				File selectedFile = chooser.getSelectedFile();
				if(!selectedFile.getName().endsWith(defaultExt)) {
					selectedFile = new File(selectedFile.getAbsolutePath()
							 + defaultExt);
				}
				
				int rVal = NativeDialogEvent.OK_OPTION;
				if(selectedFile.exists()) {
					rVal = showYesNoDialogBlocking(parentWindow, "", "File exists", "Overwrite file '" + selectedFile.getAbsolutePath() + "'?");
					rVal = (rVal == NativeDialogEvent.YES_OPTION ? NativeDialogEvent.OK_OPTION : NativeDialogEvent.CANCEL_OPTION);
				}
				
				evt = new NativeDialogEvent(rVal, selectedFile);
			} else {
				evt = new NativeDialogEvent(NativeDialogEvent.CANCEL_OPTION, null);
			}
			
			listener.nativeDialogEvent(evt);
		}
	}
	
	/**
	 * Method for displaying a save file dialog.
	 * 
	 * @param parentWindow unused for Win32 but essential for Cocoa
	 * @param listener the listener to call when an event occurs
	 * @param startDir the starting directory
	 * @param title the title of the window
	 * @param defaultExt the default file extension
	 */
	public static void showSaveFileDialog(
			Window parentWindow,
			NativeDialogListener listener,
			String startDir,
			String defaultExt,
			FileFilter[] filters,
			String title) {
		showSaveFileDialog(parentWindow, listener, startDir, "", defaultExt, filters, title);
	}
	
	/**
	 * Method for displaying a save file dialog.
	 * 
	 * @param parentWindow unused for Win32 but essential for Cocoa
	 * @param listener the listener to call when an event occurs
	 * @param startDir the starting directory
	 * @param title the title of the window
	 * @param defaultExt the default file extension
	 */
	public static void showSaveFileDialog(
			Window parentWindow,
			NativeDialogListener listener,
			String startDir,
			String fileName,
			String defaultExt,
			FileFilter[] filters,
			String title) {
		if(libraryFound) {
			nativeShowSaveFileDialog(
					parentWindow, listener,
					startDir, fileName, defaultExt, filters, title);
		} else {
			swingShowSaveFileDialog(
					parentWindow, listener,
					startDir, fileName, defaultExt, filters, title);
		}
	}
	
	/**
	 * Displays a (alert) dialog with with yes,no and cancel
	 * buttons.
	 * 
	 * @param msg1 the message text
	 * @param msg2 the 'informative' text
	 */
	private native static void nativeShowYesNoCancelDialog(
			Window parentWindow,
			NativeDialogListener listener,
			String iconFile,
			String msg1,
			String msg2);
	
	private static void swingShowYesNoCancelDialog(
			Window parentWindow,
			NativeDialogListener listener,
			String iconFile,
			String msg1,
			String msg2) {
		final Runnable task = new ShowYesNoCancelTask(
				parentWindow, listener,
				iconFile, msg1, msg2);
		
		if(SwingUtilities.isEventDispatchThread()) {
			// don't block awt thread
			task.run();
		} else {
			SwingUtilities.invokeLater(task);
		}
	}
	
	private static class ShowYesNoCancelTask implements Runnable {
		private Window parentWindow;
		private NativeDialogListener listener;
		private String iconFile;
		private String msg1;
		private String msg2;
		
		public ShowYesNoCancelTask(Window parentWindow,
				NativeDialogListener listener,
				String iconFile,
				String msg1,
				String msg2) {
			this.parentWindow = parentWindow;
			this.listener = listener;
			this.iconFile = iconFile;
			this.msg1 = msg1;
			this.msg2 = "<html>" + msg2 + "</html>";
		}
		
		@Override
		public void run() {
			int retVal = JOptionPane.showConfirmDialog(parentWindow,
					msg2, msg1, JOptionPane.YES_NO_CANCEL_OPTION);
			
			NativeDialogEvent evt = null;
			if(retVal == JOptionPane.YES_OPTION) {
				evt = new NativeDialogEvent(NativeDialogEvent.YES_OPTION, null);
			} else if(retVal == JOptionPane.NO_OPTION) {
				evt = new NativeDialogEvent(NativeDialogEvent.NO_OPTION, null);
			} else {
				evt = new NativeDialogEvent(NativeDialogEvent.CANCEL_OPTION, null);
			}
			
			listener.nativeDialogEvent(evt);
		}
	}
	
	/**
	 * Displays a (alert) dialog with with yes,no and cancel
	 * buttons.
	 * 
	 * @param msg1 the message text
	 * @param msg2 the 'informative' text
	 */
	public static void showYesNoCancelDialog(
			Window parentWindow,
			NativeDialogListener listener,
			String iconFile,
			String msg1,
			String msg2) {
		if(libraryFound) {
			nativeShowYesNoCancelDialog(
					parentWindow, listener,
					iconFile, msg1, msg2);
		} else {
			swingShowYesNoCancelDialog(
					parentWindow, listener,
					iconFile, msg1, msg2);
		}
	}
	
	/**
	 * Displays a (alert) dialog with yes and no buttons.
	 * 
	 * @param msg1 the message text
	 * @param msg2 the 'informative' text
	 */
	private native static void nativeShowYesNoDialog(
			Window parentWindow,
			NativeDialogListener listener,
			String iconFile,
			String msg1,
			String msg2);
	
	private static void swingShowYesNoDialog(
			Window parentWindow,
			NativeDialogListener listener,
			String iconFile,
			String msg1,
			String msg2) {
		final Runnable task = new ShowYesNoTask(
				parentWindow, listener,
				iconFile, msg1, msg2);
		
		if(SwingUtilities.isEventDispatchThread()) {
			// don't block awt thread
			task.run();
		} else {
			SwingUtilities.invokeLater(task);
		}
	}
	
	private static class ShowYesNoTask implements Runnable {
		private Window parentWindow;
		private NativeDialogListener listener;
		private String iconFile;
		private String msg1;
		private String msg2;
		
		public ShowYesNoTask(
				Window parentWindow,
				NativeDialogListener listener,
				String iconFile,
				String msg1,
				String msg2) {
			this.parentWindow = parentWindow;
			this.listener = listener;
			this.iconFile = iconFile;
			this.msg1 = msg1;
			this.msg2 = msg2;
		}
		
		@Override
		public void run() {
			int retVal = JOptionPane.showConfirmDialog(parentWindow, 
					msg2, msg1, JOptionPane.YES_NO_OPTION);
			
			NativeDialogEvent evt = null;
			if(retVal == JOptionPane.YES_OPTION) {
				evt = new NativeDialogEvent(NativeDialogEvent.YES_OPTION, null);
			} else {
				evt = new NativeDialogEvent(NativeDialogEvent.NO_OPTION, null);
			}
			
			listener.nativeDialogEvent(evt);
		}
	}
	
	/**
	 * Displays a (alert) dialog with yes and no buttons.
	 * 
	 * @param msg1 the message text
	 * @param msg2 the 'informative' text
	 */
	public static void showYesNoDialog(
			Window parentWindow,
			NativeDialogListener listener,
			String iconFile,
			String msg1,
			String msg2) {
		if(libraryFound) {
			nativeShowYesNoDialog(
					parentWindow, listener,
					iconFile, msg1, msg2);
		} else {
			swingShowYesNoDialog(
					parentWindow, listener,
					iconFile, msg1, msg2);
		}
	}
	
	/**
	 * Displays a (alert) dialog with ok and cancel buttons.
	 * 
	 * @param msg1 the message text
	 * @param msg2 the 'informative' text
	 */
	private native static void nativeShowOkCancelDialog(
			Window parentWindow,
			NativeDialogListener listener,
			String iconFile,
			String msg1,
			String msg2);
	
	private static void swingShowOkCancelDialog(
			Window parentWindow,
			NativeDialogListener listener,
			String iconFile,
			String msg1,
			String msg2) {
		final Runnable task = new ShowOkCancelTask(
				parentWindow, listener,
				iconFile, msg1, msg2);
		
		if(SwingUtilities.isEventDispatchThread()) {
			// don't block awt thread
			task.run();
		} else {
			SwingUtilities.invokeLater(task);
		}
	}
	
	private static class ShowOkCancelTask implements Runnable {
		private Window parentWindow;
		private NativeDialogListener listener;
		private String iconFile;
		private String msg1;
		private String msg2;
		
		public ShowOkCancelTask(Window parentWindow,
				NativeDialogListener listener,
				String iconFile,
				String msg1,
				String msg2) {
			this.parentWindow = parentWindow;
			this.listener = listener;
			this.iconFile = iconFile;
			this.msg1 = msg1;
			this.msg2 = msg2;
		}
		
		@Override
		public void run() {
			int retVal = JOptionPane.showConfirmDialog(parentWindow,
					msg2, msg1, JOptionPane.OK_CANCEL_OPTION);
			
			NativeDialogEvent evt = null;
			if(retVal == JOptionPane.OK_OPTION) {
				evt = new NativeDialogEvent(NativeDialogEvent.YES_OPTION, null);
			} else {
				evt = new NativeDialogEvent(NativeDialogEvent.CANCEL_OPTION, null);
			}
			
			listener.nativeDialogEvent(evt);
		}
	}
	
	/**
	 * Displays a (alert) dialog with ok and cancel buttons.
	 * 
	 * @param msg1 the message text
	 * @param msg2 the 'informative' text
	 */
	public static void showOkCancelDialog(
			Window parentWindow,
			NativeDialogListener listener,
			String iconFile,
			String msg1,
			String msg2) {
		if(libraryFound) {
			nativeShowOkCancelDialog(
					parentWindow, listener,
					iconFile, msg1, msg2);
		} else {
			swingShowOkCancelDialog(
					parentWindow, listener,
					iconFile, msg1, msg2);
		}
	}
	
	/**
	 * Displays a message dialog with an ok button.
	 * 
	 * @param msg1 the message text
	 * @param msg2 the 'informative' text
	 */
	private native static void nativeShowMessageDialog(
			Window parentWindow,
			NativeDialogListener listener,
			String iconFile,
			String msg1,
			String msg2);
	
	private static void swingShowMessageDialog(
			Window parentWindow,
			NativeDialogListener listener,
			String iconFile,
			String msg1,
			String msg2) {
		final Runnable task = new ShowMessageTask(
				parentWindow, listener,
				iconFile, msg1, msg2);
		
		if(SwingUtilities.isEventDispatchThread()) {
			// don't block awt thread
			task.run();
		} else {
			SwingUtilities.invokeLater(task);
		}
	}
	
	private static class ShowMessageTask implements Runnable {
		private Window parentWindow;
		private NativeDialogListener listener;
		private String iconFile;
		private String msg1;
		private String msg2;
		
		public ShowMessageTask(
				Window parentWindow,
				NativeDialogListener listener,
				String iconFile,
				String msg1,
				String msg2) {
			this.parentWindow = parentWindow;
			this.listener = listener;
			this.iconFile = iconFile;
			this.msg1 = msg1;
			this.msg2 = msg2;
		}
		
		@Override
		public void run() {
				JOptionPane.showMessageDialog(parentWindow, this.msg2, this.msg1, JOptionPane.INFORMATION_MESSAGE);
			
			NativeDialogEvent evt = null;
//			if(retVal == JOptionPane.OK_OPTION) {
				evt = new NativeDialogEvent(NativeDialogEvent.OK_OPTION, null);
//			} else {
//				evt = new NativeDialogEvent(NativeDialogEvent.CANCEL_OPTION, null);
//			}
			
			listener.nativeDialogEvent(evt);
		}
	}
	
	/**
	 * Displays a message dialog with an ok button.
	 * 
	 * @param msg1 the message text
	 * @param msg2 the 'informative' text
	 */
	public static void showMessageDialog(
			Window parentWindow,
			NativeDialogListener listener,
			String iconFile,
			String msg1,
			String msg2) {
		if(libraryFound) {
			nativeShowMessageDialog(
					parentWindow, listener,
					iconFile, msg1, msg2);
		} else {
			swingShowMessageDialog(
					parentWindow, listener,
					iconFile, msg1, msg2);
		}
	}
	
	/**
	 * Show a font dialog.
	 * 
	 * @param fontName
	 * @param fontSize
	 * @param fontStyle
	 * @param listener
	 */
	private native static void nativeShowFontSelectionDialog(
			Window parentWindow,
			NativeDialogListener listener,
			String fontName,
			int fontSize,
			int fontStyle
			);
	
	private static void swingShowFontSelectionDialog(
			Window parentWindow,
			NativeDialogListener listener,
			String fontName,
			int fontSize,
			int fontStyle) {
		final Runnable task = new ShowFontSelectionTask(
				parentWindow, listener,
				fontName, fontSize, fontStyle);
		
		if(SwingUtilities.isEventDispatchThread()) {
			// don't block awt thread
			task.run();
		} else {
			SwingUtilities.invokeLater(task);
		}
	}
	
	private static class ShowFontSelectionTask implements Runnable {
		private Window parentWindow;
		private NativeDialogListener listener;
		private String fontName;
		private int fontSize;
		private int fontStyle;
		
		public ShowFontSelectionTask(
				Window parentWindow,
				NativeDialogListener listener,
				String fontName,
				int fontSize,
				int fontStyle) {
			super();
			
			this.parentWindow = parentWindow;
			this.listener = listener;
			this.fontName = fontName;
			this.fontSize = fontSize;
			this.fontStyle = fontStyle;
		}
		
		@Override
		public void run() {
//			if(!(parentWindow instanceof JFrame)) {
//				NativeDialogEvent evt = new NativeDialogEvent();
//				evt.setDialogData(null);
//				evt.setDialogResult(NativeDialogEvent.CANCEL_OPTION);
//				
//				listener.nativeDialogEvent(evt);
//				return;
//			}
//			JFrame frame = (JFrame)parentWindow;
			
			JFrame frame = (parentWindow instanceof JFrame ? (JFrame)parentWindow : null);
			
			// create a new dialog and display
			final JFontDialog fontDialog = new JFontDialog(frame, true);
//			fontDialog.pack();
			fontDialog.setSize(new Dimension(500, 400));
			fontDialog.setResizable(false);
			fontDialog.setVisible(true);
			
			NativeDialogEvent evt = new NativeDialogEvent();
			
			if(fontDialog.isOk()) {
				evt.setDialogResult(NativeDialogEvent.OK_OPTION);
				evt.setDialogData(fontDialog.getSelectedFont());
			} else {
				evt.setDialogData(null);
				evt.setDialogResult(NativeDialogEvent.CANCEL_OPTION);
			}
			
			listener.nativeDialogEvent(evt);
		}
				
	}
	
	/**
	 * Show a font dialog.
	 * 
	 * @param fontName
	 * @param fontSize
	 * @param fontStyle
	 * @param listener
	 */
	public static void showFontSelectionDialog(
			Window parentWindow,
			NativeDialogListener listener,
			String fontName,
			int fontSize,
			int fontStyle
			) {
//		if(PhonUtilities.isMacOs() && libraryFound) {
//			nativeShowFontSelectionDialog(
//					parentWindow, listener,
//					fontName, fontSize, fontStyle);
//		} else {
			swingShowFontSelectionDialog(
					parentWindow, listener,
					fontName, fontSize, fontStyle);
//		}
	}
	
	/**
	 * Show a colour selection dialog
	 * 
	 * @param parentWindow
	 * @param listener
	 * @param currentColor as a 24-bit hex string
	 */
	private native static void nativeShowColorSelectionDialog(
			Window parentWindow,
			NativeDialogListener listener,
			int red, int green,
			int blue, int alpha);
	
	private static void swingShowColorSelectionDialog(
			Window parentWindow,
			NativeDialogListener listener,
			int red, int green,
			int blue, int alpha) {
		final Runnable task = new ShowColorSelectionTask(
				parentWindow, listener,
				red, green, blue, alpha);
		
		if(SwingUtilities.isEventDispatchThread()) {
			// don't block awt thread
			task.run();
		} else {
			SwingUtilities.invokeLater(task);
		}
	}
	
	private static class ShowColorSelectionTask implements Runnable {
		private Window parentWindow;
		private NativeDialogListener listener;
		private int red;
		private int green;
		private int blue;
		private int alpha;
		
		public ShowColorSelectionTask(
				Window parentWindow,
				NativeDialogListener listener,
				int red, int green,
				int blue, int alpha) {
			super();
			
			this.parentWindow = parentWindow;
			this.listener = listener;
			this.red = red;
			this.green = green;
			this.blue = blue;
			this.alpha = alpha;
		}
		
		@Override
		public void run() {
			Color startColor = new Color(red, green, blue, alpha);
			
//			JColorChooser chooser = new JColorChooser();
			Color retVal = JColorChooser.showDialog(parentWindow, "Select Color", startColor);
			
			NativeDialogEvent evt = null;
			if(retVal != null) {
				evt = new NativeDialogEvent(NativeDialogEvent.OK_OPTION, retVal);
			} else {
				evt = new NativeDialogEvent(NativeDialogEvent.CANCEL_OPTION, null);
			}
			
			listener.nativeDialogEvent(evt);
		}
	}
	
	/**
	 * Show a colour selection dialog
	 * 
	 * @param parentWindow
	 * @param listener
	 * @param currentColor as a 24-bit hex string
	 */
	public static void showColorSelectionDialog(
			Window parentWindow,
			NativeDialogListener listener,
			int red, int green,
			int blue, int alpha) {
		if(libraryFound) {
			nativeShowColorSelectionDialog(
					parentWindow, listener,
					red, green, blue, alpha);
		} else {
			swingShowColorSelectionDialog(
					parentWindow, listener,
					red, green, blue, alpha);
		}
	}
	
	
	/** 
	 * Method for displaying the native open file dialog.
	 * 
	 * @param parentWindow unsed in Win32 but essential for Cocoa
	 * @param listener the listener to call when an event occurs
	 * @param startDir the starting directory
	 * @param defaultExt the default extension
	 * @param filters the array of filters.
	 * @param title the title of the window
	 */
	public static String browseForFileBlocking(
			Window parentWindow,
			String startDir,
			String defaultExt,
			FileFilter[] filters,
			String title)
	{
//		if(startDir == null) {
//			startDir = PhonUtilities.getPhonWorkspace().getAbsolutePath();
//		}
		
		MessageWaitListener mwl = new MessageWaitListener();
		browseForFile(parentWindow, mwl, startDir, defaultExt, filters, title);
		mwl.waitLoop();
		
		if(mwl.getEvent().getDialogResult() == NativeDialogEvent.OK_OPTION)
			return mwl.getEvent().getDialogData().toString();
		else
			return null;
	}
	
	/**
	 * Method for displaying the native open dir dialog.
	 * 
	 * @param parentWindows unused for Win32 but essential for Cocoa
	 * @param listener the listener to call when an event occurs
	 * @param startDir the starting Directory
	 * @param title the title of the window
	 */
	public static String browseForDirectoryBlocking(
			Window parentWindow,
			String startDir,
			String title)
	{
//		if(startDir == null) {
//			startDir = PhonUtilities.getPhonWorkspace().getAbsolutePath();
//		}
		
		MessageWaitListener mwl = new MessageWaitListener();
		browseForDirectory(parentWindow, mwl, startDir, title);
		mwl.waitLoop();
		
		if(mwl.getEvent().getDialogResult() == NativeDialogEvent.OK_OPTION)
			return mwl.getEvent().getDialogData().toString();
		else
			return null;
	}
	
	/**
	 * Method for displaying a save file dialog.
	 * 
	 * @param parentWindow unused for Win32 but essential for Cocoa
	 * @param listener the listener to call when an event occurs
	 * @param startDir the starting directory
	 * @param title the title of the window
	 * @param defaultExt the default file extension
	 */
	public static String showSaveFileDialogBlocking(
			Window parentWindow,
			String startDir,
			String defaultExt,
			FileFilter[] filters,
			String title)
	{
		return showSaveFileDialogBlocking(parentWindow, startDir, "", defaultExt, filters, title);
	}
	
	/**
	 * Method for displaying a save file dialog.
	 * 
	 * @param parentWindow unused for Win32 but essential for Cocoa
	 * @param listener the listener to call when an event occurs
	 * @param startDir the starting directory
	 * @param title the title of the window
	 * @param defaultExt the default file extension
	 */
	public static String showSaveFileDialogBlocking(
			Window parentWindow,
			String startDir,
			String fileName,
			String defaultExt,
			FileFilter[] filters,
			String title)
	{
		MessageWaitListener mwl = new MessageWaitListener();
		showSaveFileDialog(parentWindow, mwl, startDir, fileName, defaultExt, filters, title);
		mwl.waitLoop();
		
		if(mwl.getEvent().getDialogResult() == NativeDialogEvent.OK_OPTION) {
			final String saveTo = mwl.getEvent().getDialogData().toString();
			return saveTo;
		} else {
			return null;
		}
	}
	
	/**
	 * Displays a (alert) dialog with with yes,no and cancel
	 * buttons.
	 * 
	 * @param msg1 the message text
	 * @param msg2 the 'informative' text
	 */
	public static int showYesNoCancelDialogBlocking(
			Window parentWindow,
			String iconFile,
			String msg1,
			String msg2)
	{
		MessageWaitListener mwl = new MessageWaitListener();
		showYesNoCancelDialog(parentWindow, mwl, iconFile, msg1, msg2);
		mwl.waitLoop();
		
		return mwl.getEvent().getDialogResult();
	}
	
	/**
	 * Displays a (alert) dialog with yes and no buttons.
	 * 
	 * @param msg1 the message text
	 * @param msg2 the 'informative' text
	 */
	public static int showYesNoDialogBlocking(
			Window parentWindow,
			String iconFile,
			String msg1,
			String msg2)
	{
		MessageWaitListener mwl = new MessageWaitListener();
		showYesNoDialog(parentWindow, mwl, iconFile, msg1, msg2);
		mwl.waitLoop();
		
		return mwl.getEvent().getDialogResult();
	}
	
	/**
	 * Displays a (alert) dialog with ok and cancel buttons.
	 * 
	 * @param msg1 the message text
	 * @param msg2 the 'informative' text
	 */
	public static int showOkCancelDialogBlocking(
			Window parentWindow,
			String iconFile,
			String msg1,
			String msg2)
	{
		MessageWaitListener mwl = new MessageWaitListener();
		showOkCancelDialog(parentWindow, mwl, iconFile, msg1, msg2);
		mwl.waitLoop();
		
		return mwl.getEvent().getDialogResult();
	}
	
	/**
	 * Displays a message dialog with an ok button.
	 * 
	 * @param msg1 the message text
	 * @param msg2 the 'informative' text
	 */
	public static void showMessageDialogBlocking(
			Window parentWindow,
			String iconFile,
			String msg1,
			String msg2)
	{
		MessageWaitListener mwl = new MessageWaitListener();
		showMessageDialog(parentWindow, mwl, iconFile, msg1, msg2);
		mwl.waitLoop();
	}
	
	public static Font showFontSelectionDialogBlocking(
			Window parentWindow,
			String fontName,
			int fontSize,
			int fontStyle)
	{
		MessageWaitListener mwl = new MessageWaitListener();
		showFontSelectionDialog(parentWindow, mwl, fontName, fontSize, fontStyle);
		mwl.waitLoop();
		
		return (Font)mwl.getEvent().getDialogData();
	}
	
	public static Color showColorSelectionDialogBlocking(
			Window parentWindow,
			int red, int green,
			int blue, int alpha)
	{
		MessageWaitListener mwl = new MessageWaitListener();
		showColorSelectionDialog(parentWindow, mwl, red, green, blue, alpha);
		mwl.waitLoop();
		
		return (Color)mwl.getEvent().getDialogData();
	}
	
	
	
	/**
	 * Simple listener to wait for a native message dialog to close.
	 */
	private static class MessageWaitListener implements NativeDialogListener {
		private boolean finished = false;
		private NativeDialogEvent event = null;
		
		@Override
		public void nativeDialogEvent(NativeDialogEvent evt) {
			if(evt.getDialogResult() != NativeDialogEvent.UNKNOWN) {
				finished = true;
				event = evt;
			}
		}
		
		public void waitLoop() {
			while (!finished) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					LOGGER.log(Level.SEVERE, e.getMessage(), e);
				}
			}
			
		}
		
		public NativeDialogEvent getEvent() {
			return event;
		}
	}
	
	public static void main(String[] args) {
		NativeDialogs.showFontSelectionDialogBlocking(null, null, 0, 0);
	}
}

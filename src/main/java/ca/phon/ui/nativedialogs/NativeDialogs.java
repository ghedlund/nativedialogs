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
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

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
	 * Show dialog based on given properties.
	 * 
	 * @param properties
	 * 
	 * @return the result of running the dialog.  See the specific
	 *  showXDialog method for expected return values.
	 */
	public static Object showDialog(NativeDialogProperties props) {
		Object retVal = null;
		
		if(props instanceof OpenDialogProperties) {
			retVal = showOpenDialog((OpenDialogProperties)props);
		} else if(props instanceof SaveDialogProperties) {
			retVal = showSaveDialog((SaveDialogProperties)props);
		} else if(props instanceof MessageDialogProperties) {
			retVal = showMessageDialog((MessageDialogProperties)props);
		} else if(props instanceof FontDialogProperties) {
			retVal = showFontDialog((FontDialogProperties)props);
		} else if(props instanceof ColorDialogProperties) {
			retVal = showColorDialog((ColorDialogProperties)props);
		}
		
		return retVal;
	}
	
	/** 
	 * Method for displaying the native open file dialog.
	 * 
	 * @param properties
	 */
	private native static void nativeShowOpenDialog(OpenDialogProperties properties);
	
	/** 
	 * Method for displaying the swing open file dialog.
	 * 
	 * @param properties
	 */
	private static void swingShowOpenDialog(OpenDialogProperties properties) {
		final Runnable task = new ShowOpenDialogTask(properties);
		
		if(SwingUtilities.isEventDispatchThread()) {
			// don't block awt thread
			task.run();
		} else {
			SwingUtilities.invokeLater(task);
		}
	}
	
	private static class ShowOpenDialogTask implements Runnable {
		private OpenDialogProperties properties;
		
		public ShowOpenDialogTask(OpenDialogProperties props) {
			this.properties = props;
		}
		
		@Override
		public void run() {
			JFileChooser chooser = new JFileChooser();
			chooser.setDialogType(JFileChooser.CUSTOM_DIALOG);
			
			if(properties.getInitialFolder() != null)
				chooser.setCurrentDirectory(new File(properties.getInitialFolder()));
			
			if(properties.getTitle() != null)
				chooser.setDialogTitle(properties.getTitle());
			
			if(properties.getFileFilter() != null)
				chooser.setFileFilter(properties.getFileFilter());
			
			if(properties.getInitialFile() != null) {
				File selectedFile = new File(properties.getInitialFolder(), properties.getInitialFile());
				chooser.setSelectedFile(selectedFile);
			}
			
			if(properties.getPrompt() != null)
				chooser.setApproveButtonText(properties.getPrompt());
			else
				chooser.setApproveButtonText("Open");
			
			chooser.setFileHidingEnabled(!properties.isShowHidden());
			
			if(properties.getMessage() != null) {
				final JLabel msgLbl = new JLabel(properties.getMessage());
				chooser.setAccessory(msgLbl);
			}
			
			if(properties.isCanChooseFiles() && properties.isCanChooseDirectories()) {
				chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			} else if(properties.isCanChooseFiles()) {
				chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			} else if(properties.isCanChooseDirectories()) {
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			}
			chooser.setMultiSelectionEnabled(properties.isAllowMultipleSelection());
			
			NativeDialogEvent evt = null;
			int retVal = chooser.showDialog(properties.getParentWindow(), null);
			if(retVal == JFileChooser.APPROVE_OPTION) {
				Object evtData = null;
				
				if(properties.isAllowMultipleSelection()) {
					final File[] selectedFiles = chooser.getSelectedFiles();
					final String[] selectedPaths = new String[selectedFiles.length];
					for(int i = 0; i < selectedFiles.length; i++) {
						selectedPaths[i] = selectedFiles[i].getAbsolutePath();
					}
				} else {
					evtData = 
							(chooser.getSelectedFile() != null ? chooser.getSelectedFile().getAbsolutePath() : null);
				}
				
				evt = new NativeDialogEvent(NativeDialogEvent.OK_OPTION, evtData);
			} else {
				evt = new NativeDialogEvent(NativeDialogEvent.CANCEL_OPTION, null);
			}
			
			properties.getListener().nativeDialogEvent(evt);
		}
	}
	
	/**
	 * Show open dialog
	 * 
	 * @param properties
	 * 
	 * @return the list of selected files/folders if properties.isRunAsync()
	 *  is <code>false</code>, <code>null</code> otherwise
	 */
	@SuppressWarnings("unchecked")
	public static List<String> showOpenDialog(OpenDialogProperties properties) {
		List<String> retVal = null;
		if(!properties.isRunAsync()) {
			final MessageWaitListener mwl = new MessageWaitListener();
			properties.setListener(mwl);
		}
		if(libraryFound && !properties.isForceUseSwing()) {
			try {
				nativeShowOpenDialog(properties);
			} catch (UnsatisfiedLinkError e) {
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
				swingShowOpenDialog(properties);
			}
		} else {
			swingShowOpenDialog(properties);
		}
		if(!properties.isRunAsync()) {
			final MessageWaitListener mwl = (MessageWaitListener)properties.getListener();
			mwl.waitLoop();
			
			final NativeDialogEvent evt = mwl.getEvent();
			
			if(evt.getDialogResult() == NativeDialogEvent.OK_OPTION) {
				if(properties.isAllowMultipleSelection()) {
					final String[] pathList = (String[])evt.getDialogData();
					retVal = Arrays.asList(pathList);
				} else {
					retVal = Arrays.asList((String)evt.getDialogData());
				}
			}
		}
		return retVal;
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
	 * 
	 * @deprecated
	 */
	public static void browseForFile(
			Window parentWindow,
			NativeDialogListener listener,
			String startDir,
			String defaultExt,
			FileFilter[] filters,
			String title) {
		final OpenDialogProperties props = new OpenDialogProperties();
		props.setParentWindow(parentWindow);
		props.setCanCreateDirectories(true);
		props.setInitialFolder(startDir);
		props.setListener(listener);
		final FileFilter filter = new FileFilter(filters);
		if(defaultExt != null)
			filter.setDefaultExtension(defaultExt);
		props.setTitle(title);
		props.setCanChooseDirectories(false);
		props.setCanChooseFiles(true);
		props.setAllowMultipleSelection(false);
		
		showOpenDialog(props);
	}
	
	/**
	 * Method for displaying the native open dir dialog.
	 * 
	 * @param parentWindows unused for Win32 but essential for Cocoa
	 * @param listener the listener to call when an event occurs
	 * @param startDir the starting Directory
	 * @param title the title of the window
	 * 
	 * @deprecated
	 */
	public static void browseForDirectory(
			Window parentWindow,
			NativeDialogListener listener,
			String startDir,
			String title) {
		final OpenDialogProperties props = new OpenDialogProperties();
		props.setParentWindow(parentWindow);
		props.setCanCreateDirectories(true);
		props.setListener(listener);
		props.setInitialFolder(startDir);
		props.setTitle(title);
		props.setCanChooseFiles(false);
		props.setCanChooseDirectories(true);
		props.setAllowMultipleSelection(false);
		
		showOpenDialog(props);
	}
	
//	/**
//	 * Task to browse for a phon project.
//	 * 
//	 */
//	private static class BrowseForProjectTask implements Runnable {
//
//		private Window parentWindow;
//		private NativeDialogListener listener;
//		private String startDir;
//		private String title;
//		
//		public BrowseForProjectTask(
//				Window parentWindow,
//				NativeDialogListener listener,
//				String startDir,
//				String title) {
//			this.parentWindow = parentWindow;
//			this.listener = listener;
//			this.startDir = startDir;
//			this.title = title;
//		}
//		
//		@Override
//		public void run() {
//			JFileChooser chooser = new JFileChooser();
//			
//			if(startDir != null)
//				chooser.setCurrentDirectory(new File(startDir));
//			
//			chooser.setDialogTitle(title);
//			chooser.setMultiSelectionEnabled(false);
//			chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
//			
//				javax.swing.filechooser.FileFilter projectFilter = new javax.swing.filechooser.FileFilter() {
//	
//					@Override
//					public boolean accept(File file) {
//						boolean isPhonProject = false;
//						
//						if(file.isDirectory()) {
//							if(!OSInfo.isMacOs()) {
//								// accept all directories on non-mac systems
//								isPhonProject = true;
//							} else {
//								File projectXml = new File(file, "project.xml");
//								if(!projectXml.exists())
//									isPhonProject = false;
//								else
//									isPhonProject = true;
//							}
//						} else {
//							if(file.getName().endsWith(".phon")) {
//								isPhonProject = true;
//							}
//						}
//						
//						return isPhonProject;
//					}
//	
//					@Override
//					public String getDescription() {
//						return "Phon Projects";
//					}
//				};
//				chooser.setFileFilter(projectFilter);
//			
//			
//			NativeDialogEvent evt = null;
//			int retVal = chooser.showOpenDialog(parentWindow);
//			if(retVal == JFileChooser.APPROVE_OPTION) {
//				// create the dialog event
//				evt = new NativeDialogEvent(NativeDialogEvent.OK_OPTION, chooser.getSelectedFile());
//			} else {
//				evt = new NativeDialogEvent(NativeDialogEvent.CANCEL_OPTION, null);
//			}
//			
//			listener.nativeDialogEvent(evt);
//		}
//		
//	}
//	
//	/**
//	 * Show a dialog for selecting phon project.  Phon project are
//	 * either a directory with a project.xml file inside, or a file
//	 * with a .phon extension.
//	 * 
//	 * @param parentWindow
//	 * @param listener
//	 * @param startDir
//	 * @param title
//	 */
//	public static void browseForProject(
//			Window parentWindow,
//			NativeDialogListener listener,
//			String startDir,
//			String title)
//	{
////		if(startDir == null) {
////			startDir = PhonUtilities.getPhonWorkspace().getAbsolutePath();
////		}
//		
//		// no native version yet
//		swingBrowseForProject(parentWindow, listener, startDir, title);
//	}
//	
//	private static void swingBrowseForProject(
//			Window parentWindow,
//			NativeDialogListener listener,
//			String startDir,
//			String title)
//	{
//		final Runnable task = new BrowseForProjectTask(parentWindow, listener, 
//				startDir, title);
//		
//		if(SwingUtilities.isEventDispatchThread()) 
//			task.run();
//		else
//			SwingUtilities.invokeLater(task);
//	}
//	
//	public static String browseForProjectBlocking(
//			Window parentWindow,
//			String startDir,
//			String title)
//	{
//	
////		if(startDir == null) {
////			startDir = PhonUtilities.getPhonWorkspace().getAbsolutePath();
////		}
//		
//		MessageWaitListener mwl = new MessageWaitListener();
//		browseForProject(parentWindow, mwl, startDir, title);
//		mwl.waitLoop();
//		
//		if(mwl.getEvent().getDialogResult() == NativeDialogEvent.OK_OPTION)
//			return mwl.getEvent().getDialogData().toString();
//		else
//			return null;
//	}
	
	/**
	 * Method for displaying a save file dialog.
	 * 
	 * @param properties
	 */
	private native static void nativeShowSaveDialog(SaveDialogProperties properties);
	
	private static void swingShowSaveDialog(SaveDialogProperties properties) {
		final Runnable task = 
				new ShowSaveFileTask(properties);
		
		if(SwingUtilities.isEventDispatchThread()) {
			// don't block awt thread
			task.run();
		} else {
			SwingUtilities.invokeLater(task);
		}
	}
	
	private static class ShowSaveFileTask implements Runnable {
		private final SaveDialogProperties properties;
		
		public ShowSaveFileTask(SaveDialogProperties properties) {
			this.properties = properties;
		}
		
		@Override
		public void run() {
			JFileChooser chooser = new JFileChooser();
			chooser.setDialogType(JFileChooser.CUSTOM_DIALOG);
			
			if(properties.getInitialFolder() != null)
				chooser.setCurrentDirectory(new File(properties.getInitialFolder()));
			
			if(properties.getTitle() != null)
				chooser.setDialogTitle(properties.getTitle());
			
			if(properties.getFileFilter() != null)
				chooser.setFileFilter(properties.getFileFilter());
			
			if(properties.getInitialFile() != null) {
				File selectedFile = new File(properties.getInitialFolder(), properties.getInitialFile());
				chooser.setSelectedFile(selectedFile);
			}
			
			if(properties.getPrompt() != null)
				chooser.setApproveButtonText(properties.getPrompt());
			else
				chooser.setApproveButtonText("Save");
			
			chooser.setFileHidingEnabled(!properties.isShowHidden());
			
			if(properties.getMessage() != null) {
				final JLabel msgLbl = new JLabel(properties.getMessage());
				chooser.setAccessory(msgLbl);
			}
			
			NativeDialogEvent evt = null;
			int retVal = chooser.showDialog(properties.getParentWindow(), null);
			if(retVal == JFileChooser.APPROVE_OPTION) {
				// create the dialog event
				File selectedFile = chooser.getSelectedFile();
	
				if(properties.getFileFilter() != null) {
					boolean hasValidExt = false;
					for(String validExt:properties.getFileFilter().getAllExtensions()) {
						if(selectedFile.getName().endsWith("." + validExt)) {
							hasValidExt = true;
							break;
						}
					}
					if(!hasValidExt) {
						selectedFile = new File(selectedFile.getAbsolutePath()
								 + "." + properties.getFileFilter().getDefaultExtension());
					}
				}
				
				int rVal = NativeDialogEvent.OK_OPTION;
				if(selectedFile.exists()) {
					rVal = showYesNoDialogBlocking(properties.getParentWindow(), "", "File exists", "Overwrite file '" + selectedFile.getAbsolutePath() + "'?");
					rVal = (rVal == 0 ? NativeDialogEvent.OK_OPTION : NativeDialogEvent.CANCEL_OPTION);
				}
				
				evt = new NativeDialogEvent(rVal, selectedFile.getAbsolutePath());
			} else {
				evt = new NativeDialogEvent(NativeDialogEvent.CANCEL_OPTION, null);
			}
			
			properties.getListener().nativeDialogEvent(evt);
		}
	}
	
	/**
	 * Method for displaying a save file dialog.
	 * 
	 * @param properties
	 * 
	 * @return the save file path if properties.isRunAsync() 
	 *  is <code>false</code>, <code>null</code> otherwise
	 */
	public static String showSaveDialog(SaveDialogProperties properties) {
		String retVal = null;
		if(!properties.isRunAsync()) {
			final MessageWaitListener mwl = new MessageWaitListener();
			properties.setListener(mwl);
		}
		if(libraryFound && !properties.isForceUseSwing()) {
			try {
				nativeShowSaveDialog(properties);
			} catch (UnsatisfiedLinkError e) {
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
				swingShowSaveDialog(properties);
			}
		} else {
			swingShowSaveDialog(properties);
		}
		if(!properties.isRunAsync()) {
			final MessageWaitListener mwl = (MessageWaitListener)properties.getListener();
			mwl.waitLoop();
			
			final NativeDialogEvent evt = mwl.getEvent();
			
			if(evt.getDialogResult() == NativeDialogEvent.OK_OPTION) {
				retVal = (String)evt.getDialogData();
			}
		}
		return retVal;
	}

	/**
	 * Method for displaying a save file dialog.
	 * 
	 * @param parentWindow unused for Win32 but essential for Cocoa
	 * @param listener the listener to call when an event occurs
	 * @param startDir the starting directory
	 * @param title the title of the window
	 * @param defaultExt the default file extension
	 * 
	 * @deprecated
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
	 * 
	 * @deprecated
	 */
	public static void showSaveFileDialog(
			Window parentWindow,
			NativeDialogListener listener,
			String startDir,
			String fileName,
			String defaultExt,
			FileFilter[] filters,
			String title) {
		final SaveDialogProperties props = new SaveDialogProperties();
		props.setParentWindow(parentWindow);
		props.setCanCreateDirectories(true);
		props.setInitialFolder(startDir);
		props.setInitialFile(fileName);
		final FileFilter filter = new FileFilter(filters);
		if(defaultExt != null)
			filter.setDefaultExtension(defaultExt);
		props.setFileFilter(filter);
		props.setTitle(title);
		
		props.setListener(listener);
		
		showSaveDialog(props);
	}
	
	/**
	 * Displays a (alert) dialog with with yes,no and cancel
	 * buttons.
	 * 
	 * @param msg1 the message text
	 * @param msg2 the 'informative' text
	 * 
	 * @deprecated
	 */
	public static void showYesNoCancelDialog(
			Window parentWindow,
			NativeDialogListener listener,
			String iconFile,
			String msg1,
			String msg2) {
		final MessageDialogProperties props = new MessageDialogProperties();
		props.setOptions(MessageDialogProperties.yesNoCancelOptions);
		props.setListener(listener);
		props.setParentWindow(parentWindow);
		props.setHeader(msg1);
		props.setMessage(msg2);
		props.setRunAsync(true);
		
		showMessageDialog(props);
	}
	
	/**
	 * Displays a (alert) dialog with yes and no buttons.
	 * 
	 * @param msg1 the message text
	 * @param msg2 the 'informative' text
	 * 
	 * @deprecated
	 */
	public static void showYesNoDialog(
			Window parentWindow,
			NativeDialogListener listener,
			String iconFile,
			String msg1,
			String msg2) {
		final MessageDialogProperties props = new MessageDialogProperties();
		props.setOptions(MessageDialogProperties.yesNoOptions);
		props.setListener(listener);
		props.setParentWindow(parentWindow);
		props.setHeader(msg1);
		props.setMessage(msg2);
		props.setRunAsync(true);
		
		showMessageDialog(props);
	}
	
	/**
	 * Displays a (alert) dialog with ok and cancel buttons.
	 * 
	 * @param msg1 the message text
	 * @param msg2 the 'informative' text
	 * 
	 * @deprecated
	 */
	public static void showOkCancelDialog(
			Window parentWindow,
			NativeDialogListener listener,
			String iconFile,
			String msg1,
			String msg2) {
		final MessageDialogProperties props = new MessageDialogProperties();
		props.setOptions(MessageDialogProperties.okCancelOptions);
		props.setListener(listener);
		props.setParentWindow(parentWindow);
		props.setHeader(msg1);
		props.setMessage(msg2);
		props.setRunAsync(true);
		
		showMessageDialog(props);
	}
	
	/**
	 * Displays a message dialog with an ok button.
	 * 
	 * @param properties
	 */
	private native static void nativeShowMessageDialog(MessageDialogProperties properties);
	
	private static void swingShowMessageDialog(MessageDialogProperties properties) {
		final Runnable task = new ShowMessageTask(properties);
		
		if(SwingUtilities.isEventDispatchThread()) {
			// don't block awt thread
			task.run();
		} else {
			SwingUtilities.invokeLater(task);
		}
	}
	
	private static class ShowMessageTask implements Runnable {
		
		private MessageDialogProperties properties;
		
		public ShowMessageTask(MessageDialogProperties props) {
			this.properties = props;
		}
		
		@Override
		public void run() {
			final String msg = "<html><h3>" + properties.getHeader() + "</h3>\n" + properties.getMessage();
			final Icon icon = (properties.getIcon() != null ? new ImageIcon(properties.getIcon()) : null);
			
			Object msgPanelObjects = null;
			final JCheckBox suppressionBox = new JCheckBox();
			if(properties.isShowSuppressionBox()) {
				suppressionBox.setText(
						(properties.getSuppressionMessage() != null ? properties.getSuppressionMessage() : "Do not show this message again"));
				msgPanelObjects = new Object[] { msg, suppressionBox };
			} else {
				msgPanelObjects = msg;
			}
			
			int result = JOptionPane.showOptionDialog(properties.getParentWindow(), msgPanelObjects, properties.getTitle(), 
					JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, icon,
					properties.getOptions(), properties.getDefaultOption());
			
			final NativeDialogEvent evt = new NativeDialogEvent(result, (properties.isShowSuppressionBox() ? suppressionBox.isSelected() : null));
			properties.getListener().nativeDialogEvent(evt);
		}
	}
	
	/**
	 * Display a message dialog.
	 * 
	 * @param properties
	 * 
	 * @return index of the selected option if properties.isRunAsync() is <code>false</code>,
	 *  <code>null</code> otherwise
	 */
	public static Integer showMessageDialog(MessageDialogProperties properties) {
		Integer retVal = null;
		if(!properties.isRunAsync()) {
			final MessageWaitListener mwl = new MessageWaitListener();
			properties.setListener(mwl);
		}
		if(libraryFound && !properties.isForceUseSwing()) {
			try {
				nativeShowMessageDialog(properties);
			} catch (UnsatisfiedLinkError e) {
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
				swingShowMessageDialog(properties);
			}
		} else {
			swingShowMessageDialog(properties);
		}
		if(!properties.isRunAsync()) {
			final MessageWaitListener mwl = (MessageWaitListener)properties.getListener();
			mwl.waitLoop();
			
			final NativeDialogEvent evt = mwl.getEvent();
			retVal = evt.getDialogResult();
		}
		return retVal;
	}
	
	/**
	 * Displays a message dialog with an ok button.
	 * 
	 * @param msg1 the message text
	 * @param msg2 the 'informative' text
	 * 
	 * @deprecated
	 */
	public static void showMessageDialog(
			Window parentWindow,
			NativeDialogListener listener,
			String iconFile,
			String msg1,
			String msg2) {
		final MessageDialogProperties props = new MessageDialogProperties();
		props.setOptions(MessageDialogProperties.okOptions);
		props.setListener(listener);
		props.setParentWindow(parentWindow);
		props.setHeader(msg1);
		props.setMessage(msg2);
		props.setRunAsync(true);
		
		showMessageDialog(props);
	}
	
	/**
	 * Show a font dialog.
	 * 
	 * @param properties
	 */
	private native static void nativeShowFontDialog(FontDialogProperties properties);
	
	private static void swingShowFontDialog(FontDialogProperties properties) {
		final Runnable task = new ShowFontSelectionTask(properties);
		
		if(SwingUtilities.isEventDispatchThread()) {
			// don't block awt thread
			task.run();
		} else {
			SwingUtilities.invokeLater(task);
		}
	}
	
	private static class ShowFontSelectionTask implements Runnable {
		private FontDialogProperties properties;
		
		public ShowFontSelectionTask(FontDialogProperties properties) {
			super();
			
			this.properties = properties;
		}
		
		@Override
		public void run() {
			Font f = properties.createFont();
			f = (f != null ? f : UIManager.getFont("TextPane.font"));
			final JFontDialog fontDialog = new JFontDialog(
					(properties.getParentWindow() != null && properties.getParentWindow() instanceof JFrame ? (JFrame)properties.getParentWindow() : null), true);
			fontDialog.setSize(new Dimension(500, 400));
			if(properties.getTitle() != null)
				fontDialog.setTitle(properties.getTitle());
			if(properties.getParentWindow() != null)
				fontDialog.setLocationRelativeTo(properties.getParentWindow());
			if(f != null)
				fontDialog.setSelectedFont(f);
			fontDialog.setVisible(true);
			
			final NativeDialogEvent evt = new NativeDialogEvent();
			if(fontDialog.isOk()) {
				evt.setDialogResult(NativeDialogEvent.OK_OPTION);
				evt.setDialogData(fontDialog.getSelectedFont());
			} else {
				evt.setDialogData(null);
				evt.setDialogResult(NativeDialogEvent.CANCEL_OPTION);
			}
			
			properties.getListener().nativeDialogEvent(evt);
		}
				
	}
	
	/**
	 * Show a font dialog
	 * 
	 * @param properties
	 */
	public static Font showFontDialog(FontDialogProperties properties) {
		Font retVal = null;
		if(!properties.isRunAsync()) {
			final MessageWaitListener mwl = new MessageWaitListener();
			properties.setListener(mwl);
		}
		if(libraryFound && !properties.isForceUseSwing()) {
			try {
				nativeShowFontDialog(properties);
			} catch (UnsatisfiedLinkError e) {
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
				swingShowFontDialog(properties);
			}
		} else {
			swingShowFontDialog(properties);
		}
		if(!properties.isRunAsync()) {
			final MessageWaitListener mwl = (MessageWaitListener)properties.getListener();
			mwl.waitLoop();
			
			final NativeDialogEvent evt = mwl.getEvent();
			if(evt.getDialogResult() == NativeDialogEvent.OK_OPTION)
				retVal = (Font)evt.getDialogData();
		}
		return retVal;
	}
	
	/**
	 * Show a font dialog.
	 * 
	 * @param fontName
	 * @param fontSize
	 * @param fontStyle
	 * @param listener
	 * 
	 * @deprecated
	 */
	public static void showFontSelectionDialog(
			Window parentWindow,
			NativeDialogListener listener,
			String fontName,
			int fontSize,
			int fontStyle
			) {
		final FontDialogProperties props = new FontDialogProperties();
		props.setParentWindow(parentWindow);
		props.setListener(listener);
		props.setFontName(fontName);
		props.setFontSize(fontSize);
		props.setBold((fontStyle & Font.BOLD) != 0);
		props.setItalic((fontStyle & Font.ITALIC) != 0);
		showFontDialog(props);
	}
	
	/**
	 * Show a colour selection dialog
	 * 
	 * @param parentWindow
	 * @param listener
	 * @param currentColor as a 24-bit hex string
	 */
	private native static void nativeShowColorDialog(ColorDialogProperties props);
	
	private static void swingShowColorDialog(ColorDialogProperties props) {
		final Runnable task = new ShowColorSelectionTask(props);
		
		if(SwingUtilities.isEventDispatchThread()) {
			// don't block awt thread
			task.run();
		} else {
			SwingUtilities.invokeLater(task);
		}
	}
	
	/**
	 * Show color selection dialog
	 * 
	 * @param properties
	 *
	 * @return selected color if props.isRunAsync() is false, <code>null</code> otherwise
	 */
	public static Color showColorDialog(ColorDialogProperties properties) {
		Color retVal = null;
		if(!properties.isRunAsync()) {
			final MessageWaitListener mwl = new MessageWaitListener();
			properties.setListener(mwl);
		}
		if(libraryFound && !properties.isForceUseSwing()) {
			try {
				nativeShowColorDialog(properties);
			} catch (UnsatisfiedLinkError e) {
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
				swingShowColorDialog(properties);
			}
		} else {
			swingShowColorDialog(properties);
		}
		if(!properties.isRunAsync()) {
			final MessageWaitListener mwl = (MessageWaitListener)properties.getListener();
			mwl.waitLoop();
			
			final NativeDialogEvent evt = mwl.getEvent();
			if(evt.getDialogResult() == NativeDialogEvent.OK_OPTION)
				retVal = (Color)evt.getDialogData();
		}
		return retVal;
	}
	
	private static class ShowColorSelectionTask implements Runnable {
		private ColorDialogProperties properties;
		
		public ShowColorSelectionTask(ColorDialogProperties properties) {
			super();
			
			this.properties = properties;
		}
		
		@Override
		public void run() {
			Color startColor = properties.getColor();
			
			Color retVal = JColorChooser.showDialog(properties.getParentWindow(), properties.getTitle(), startColor);
			
			NativeDialogEvent evt = null;
			if(retVal != null) {
				evt = new NativeDialogEvent(NativeDialogEvent.OK_OPTION, retVal);
			} else {
				evt = new NativeDialogEvent(NativeDialogEvent.CANCEL_OPTION, null);
			}
			
			properties.getListener().nativeDialogEvent(evt);
		}
	}
	
	/**
	 * Show a colour selection dialog
	 * 
	 * @param parentWindow
	 * @param listener
	 * @param red
	 * @param green
	 * @param blue
	 * @param alpha
	 * 
	 * @deprecated
	 */
	public static void showColorSelectionDialog(
			Window parentWindow,
			NativeDialogListener listener,
			int red, int green,
			int blue, int alpha) {
		final ColorDialogProperties props = new ColorDialogProperties();
		props.setParentWindow(parentWindow);
		props.setListener(listener);
		props.setRed(red);
		props.setGreen(green);
		props.setBlue(blue);
		props.setAlpha(alpha);
		
		showColorDialog(props);
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
	 * 
	 * @deprecated
	 */
	public static String browseForFileBlocking(
			Window parentWindow,
			String startDir,
			String defaultExt,
			FileFilter[] filters,
			String title)
	{
		final OpenDialogProperties props = new OpenDialogProperties();
		props.setParentWindow(parentWindow);
		props.setCanCreateDirectories(true);
		props.setRunAsync(false);
		props.setInitialFolder(startDir);
		final FileFilter filter = new FileFilter(filters);
		if(defaultExt != null)
			filter.setDefaultExtension(defaultExt);
		props.setTitle(title);
		props.setCanChooseDirectories(false);
		props.setCanChooseFiles(true);
		props.setAllowMultipleSelection(false);
		
		List<String> retVal = showOpenDialog(props);
		
		if(retVal != null && retVal.size() > 0)
			return retVal.get(0);
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
	 * 
	 * @deprecated
	 */
	public static String browseForDirectoryBlocking(
			Window parentWindow,
			String startDir,
			String title)
	{
		final OpenDialogProperties props = new OpenDialogProperties();
		props.setParentWindow(parentWindow);
		props.setCanCreateDirectories(true);
		props.setRunAsync(false);
		props.setInitialFolder(startDir);
		props.setTitle(title);
		props.setCanChooseDirectories(true);
		props.setCanChooseFiles(false);
		props.setAllowMultipleSelection(false);
		
		List<String> retVal = showOpenDialog(props);
		
		if(retVal != null && retVal.size() > 0)
			return retVal.get(0);
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
	 * 
	 * @deprecated
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
	 * 
	 * @deprecated
	 */
	public static String showSaveFileDialogBlocking(
			Window parentWindow,
			String startDir,
			String fileName,
			String defaultExt,
			FileFilter[] filters,
			String title)
	{
		final SaveDialogProperties props = new SaveDialogProperties();
		props.setParentWindow(parentWindow);
		props.setCanCreateDirectories(true);
		props.setRunAsync(false);
		props.setInitialFolder(startDir);
		props.setInitialFile(fileName);
		final FileFilter filter = new FileFilter(filters);
		if(defaultExt != null)
			filter.setDefaultExtension(defaultExt);
		props.setFileFilter(filter);
		props.setTitle(title);
		
		return showSaveDialog(props);
	}
	
	/**
	 * Displays a (alert) dialog with with yes,no and cancel
	 * buttons.
	 * 
	 * @param msg1 the message text
	 * @param msg2 the 'informative' text
	 * 
	 * @deprecated
	 */
	public static int showYesNoCancelDialogBlocking(
			Window parentWindow,
			String iconFile,
			String msg1,
			String msg2)
	{
		final MessageDialogProperties props = new MessageDialogProperties();
		props.setOptions(MessageDialogProperties.yesNoCancelOptions);
		props.setParentWindow(parentWindow);
		props.setHeader(msg1);
		props.setMessage(msg2);
		props.setRunAsync(false);
		
		return showMessageDialog(props);
	}
	
	/**
	 * Displays a (alert) dialog with yes and no buttons.
	 * 
	 * @param msg1 the message text
	 * @param msg2 the 'informative' text
	 * 
	 * @deprecated
	 */
	public static int showYesNoDialogBlocking(
			Window parentWindow,
			String iconFile,
			String msg1,
			String msg2)
	{
		final MessageDialogProperties props = new MessageDialogProperties();
		props.setOptions(MessageDialogProperties.yesNoOptions);
		props.setParentWindow(parentWindow);
		props.setHeader(msg1);
		props.setMessage(msg2);
		props.setRunAsync(false);
		
		return showMessageDialog(props);
	}
	
	/**
	 * Displays a (alert) dialog with ok and cancel buttons.
	 * 
	 * @param msg1 the message text
	 * @param msg2 the 'informative' text
	 * 
	 * @deprecated
	 */
	public static int showOkCancelDialogBlocking(
			Window parentWindow,
			String iconFile,
			String msg1,
			String msg2)
	{
		final MessageDialogProperties props = new MessageDialogProperties();
		props.setOptions(MessageDialogProperties.okCancelOptions);
		props.setParentWindow(parentWindow);
		props.setHeader(msg1);
		props.setMessage(msg2);
		props.setRunAsync(false);
		
		return showMessageDialog(props);
	}
	
	/**
	 * Displays a message dialog with an ok button.
	 * 
	 * @param msg1 the message text
	 * @param msg2 the 'informative' text
	 * 
	 * @deprecated
	 */
	public static void showMessageDialogBlocking(
			Window parentWindow,
			String iconFile,
			String msg1,
			String msg2)
	{
		final MessageDialogProperties props = new MessageDialogProperties();
		props.setOptions(MessageDialogProperties.okOptions);
		props.setParentWindow(parentWindow);
		props.setHeader(msg1);
		props.setMessage(msg2);
		props.setRunAsync(false);
		
		showMessageDialog(props);
	}
	
	/**
	 * Display font selection dialog
	 * 
	 * @param parentWindow
	 * @param fontName
	 * @param fontSize
	 * @param fontStyle
	 * 
	 * @return
	 *
	 * @deprecated
	 */
	public static Font showFontSelectionDialogBlocking(
			Window parentWindow,
			String fontName,
			int fontSize,
			int fontStyle)
	{
		final FontDialogProperties props = new FontDialogProperties();
		props.setParentWindow(parentWindow);
		props.setFontName(fontName);
		props.setFontSize(fontSize);
		props.setBold((fontStyle & Font.BOLD) != 0);
		props.setItalic((fontStyle & Font.ITALIC) != 0);
		props.setRunAsync(false);
		return showFontDialog(props);
	}
	
	/**
	 * 
	 * @param parentWindow
	 * @param red
	 * @param green
	 * @param blue
	 * @param alpha
	 * @return selected color
	 * 
	 * @deprecated
	 */
	public static Color showColorSelectionDialogBlocking(
			Window parentWindow,
			int red, int green,
			int blue, int alpha)
	{
		final ColorDialogProperties props = new ColorDialogProperties();
		props.setParentWindow(parentWindow);
		props.setRunAsync(false);
		props.setRed(red);
		props.setGreen(green);
		props.setBlue(blue);
		props.setAlpha(alpha);
		
		return showColorDialog(props);
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
	
}

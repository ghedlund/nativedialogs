/*
 * Copyright (C) 2012-2018 Gregory Hedlund
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 *    http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.phon.ui.nativedialogs;

import java.util.Map;

/**
 * Properties for the save file dialog.
 *
 */
public class SaveDialogProperties extends NativeDialogProperties {
	
	private static final long serialVersionUID = -7421844774940413702L;

	/**
	 * Initial folder
	 */
	public final static String INITIAL_FOLDER = "initial_folder";
	
	/**
	 * Initial filename
	 */
	public final static String INITIAL_FILE = "initial_file";
	
	/**
	 * File filter
	 * Sets up all allowed extensions and default extension
	 */
	public final static String FILE_FILTER = "file_filter";
	
	/**
	 * Can the dialog create new directories?
	 * 
	 * OPTIONAL: The implementing framework may not support
	 *  this option
	 */
	public final static String CAN_CREATE_DIRECTORIES = "can_create_directories";
	
//	/**
//	 * Treat file packages as directories
//	 * 
//	 * MacOSX only
//	 */
//	public final static String TREAT_PACAKGES_AS_DIRECTORIES = "treat_packages_as_directories";
	
	/**
	 * Set prompt text on default button
	 * 
	 * OPTIONAL
	 */
	public final static String PROMPT = "prompt";
	
	/**
	 * Set text displayed in front of name text field
	 * 
	 * OPTIONAL
	 */
	public final static String NAME_FIELD_LABEL = "name_field_label";
	
	/**
	 * Message
	 * 
	 * OPTIONAL
	 */
	public final static String MESSAGE = "message";
	
	/**
	 * Show hidden files
	 */
	public final static String SHOW_HIDDEN = "show_hidden";
	
//	/**
//	 * Hide extension
//	 * 
//	 * Mac Only
//	 */
//	public final static String HIDE_EXTENSION = "hide_extension";
//	
//	/**
//	 * Can select hide extension
//	 * 
//	 * Mac Only
//	 */
//	public final static String CAN_SELECT_HIDE_EXTENSION = "can_select_hide_extension";
	
	/**
	 * Constructor
	 */
	public SaveDialogProperties() {
		super();
	}
	
	public SaveDialogProperties(Map<String, Object> props) {
		super(props);
	}
	
	public FileFilter getFileFilter() {
		FileFilter retVal = null;
		if(get(FILE_FILTER) != null) {
			retVal = (FileFilter)get(FILE_FILTER);
		}
		return retVal;
	}
	
	public void setFileFilter(FileFilter filter) {
		put(FILE_FILTER, filter);
	}
	
	public boolean isCanCreateDirectories() {
		Boolean retVal = Boolean.FALSE;
		if(get(CAN_CREATE_DIRECTORIES) != null) {
			retVal = Boolean.valueOf(get(CAN_CREATE_DIRECTORIES).toString());
		}
		return retVal;
	}
	
	public void setCanCreateDirectories(boolean canCreateDirectories) {
		put(CAN_CREATE_DIRECTORIES, canCreateDirectories);
	}
	
	public String getInitialFile() {
		String retVal = null;
		if(get(INITIAL_FILE) != null) {
			retVal = get(INITIAL_FILE).toString();
		}
		return retVal;
	}
	
	public void setInitialFile(String initialFile) {
		put(INITIAL_FILE, initialFile);
	}
	
	public String getInitialFolder() {
		String retVal = null;
		if(get(INITIAL_FOLDER) != null) {
			retVal = get(INITIAL_FOLDER).toString();
		}
		return retVal;
	}
	
	public void setInitialFolder(String initialFolder) {
		put(INITIAL_FOLDER, initialFolder);
	}
	
	public boolean isShowHidden() {
		Boolean retVal = Boolean.FALSE;
		if(get(SHOW_HIDDEN) != null) {
			retVal = Boolean.valueOf(get(SHOW_HIDDEN).toString());
		}
		return retVal;
	}
	
	public void setShowHidden(boolean showHidden) {
		put(SHOW_HIDDEN, showHidden);
	}
	
//	public boolean treatPackagesAsDirectories() {
//		Boolean retVal = Boolean.FALSE;
//		if(get(TREAT_PACAKGES_AS_DIRECTORIES) != null) {
//			retVal = Boolean.valueOf(get(TREAT_PACAKGES_AS_DIRECTORIES).toString());
//		}
//		return retVal;
//	}
//	
//	public void setTreatPackagesAsDirectories(boolean treatPackagesAsDirectories) {
//		put(TREAT_PACAKGES_AS_DIRECTORIES, treatPackagesAsDirectories);
//	}
	
	public String getPrompt() {
		String retVal = null;
		if(get(PROMPT) != null) {
			retVal = get(PROMPT).toString();
		}
		return retVal;
	}
	
	public void setPrompt(String prompt) {
		put(PROMPT, prompt);
	}
	
	public String getNameFieldLabel() {
		String retVal = null;
		if(get(NAME_FIELD_LABEL) != null) {
			retVal = get(NAME_FIELD_LABEL).toString();
		}
		return retVal;
	}
	
	public void setNameFieldLabel(String nameFieldLabel) {
		put(NAME_FIELD_LABEL, nameFieldLabel);
	}
	
	public String getMessage() {
		String retVal = null;
		if(get(MESSAGE) != null) {
			retVal = get(MESSAGE).toString();
		}
		return retVal;
	}
	
	public void setMessage(String message) {
		put(MESSAGE, message);
	}
	
//	public boolean isHideExtension() {
//		Boolean retVal = Boolean.FALSE;
//		if(get(HIDE_EXTENSION) != null) {
//			retVal = Boolean.valueOf(get(HIDE_EXTENSION).toString());
//		}
//		return retVal;
//	}
//
//	public void setHideExtension(boolean hideExtension) {
//		put(HIDE_EXTENSION, hideExtension);
//	}
//	
//	public boolean isCanSelectHideExtension() {
//		Boolean retVal = Boolean.FALSE;
//		if(get(CAN_SELECT_HIDE_EXTENSION) != null) {
//			retVal = Boolean.valueOf(get(CAN_SELECT_HIDE_EXTENSION).toString());
//		}
//		return retVal;
//	}
//	
//	public void setCanSelectHideExtension(boolean canSelectHideExtension) {
//		put(CAN_SELECT_HIDE_EXTENSION, canSelectHideExtension);
//	}
}
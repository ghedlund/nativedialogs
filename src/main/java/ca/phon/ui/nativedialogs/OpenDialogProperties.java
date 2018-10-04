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

public class OpenDialogProperties extends SaveDialogProperties {

	private static final long serialVersionUID = -1152927514203658096L;

	/**
	 * Allow multiple selection
	 */
	public static final String ALLOW_MULTIPLE_SELECTION = "allow_multiple_selection";
	
	/**
	 * Can choose files
	 */
	public static final String CAN_CHOOSE_FILES = "can_choose_files";
	
	/**
	 * Can choose directories
	 */
	public static final String CAN_CHOOSE_DIRECTORIES = "can_choose_directories";
	
	/**
	 * Constructor
	 */
	public OpenDialogProperties() {
		super();
		put(ALLOW_MULTIPLE_SELECTION, Boolean.FALSE);
		put(CAN_CHOOSE_FILES, Boolean.TRUE);
		put(CAN_CHOOSE_DIRECTORIES, Boolean.FALSE);
	}
	
	public OpenDialogProperties(Map<String, Object> props) {
		super(props);
	}
	
	public boolean isAllowMultipleSelection() {
		Boolean retVal = Boolean.FALSE;
		if(get(ALLOW_MULTIPLE_SELECTION) != null) {
			retVal = Boolean.valueOf(get(ALLOW_MULTIPLE_SELECTION).toString());
		}
		return retVal;
	}
	
	public void setAllowMultipleSelection(boolean allowMultipleSelection) {
		put(ALLOW_MULTIPLE_SELECTION, allowMultipleSelection);
	}
	
	public boolean isCanChooseFiles() {
		Boolean retVal = Boolean.FALSE;
		if(get(CAN_CHOOSE_FILES) != null) {
			retVal = Boolean.valueOf(get(CAN_CHOOSE_FILES).toString());
		}
		return retVal;
	}
	
	public void setCanChooseFiles(boolean canChooseFiles) {
		put(CAN_CHOOSE_FILES, canChooseFiles);
	}
	
	public boolean isCanChooseDirectories() {
		Boolean retVal = Boolean.FALSE;
		if(get(CAN_CHOOSE_DIRECTORIES) != null) {
			retVal = Boolean.valueOf(get(CAN_CHOOSE_DIRECTORIES).toString());
		}
		return retVal;
	}
	
	public void setCanChooseDirectories(boolean canChooseDirectories) {
		put(CAN_CHOOSE_DIRECTORIES, canChooseDirectories);
	}
}

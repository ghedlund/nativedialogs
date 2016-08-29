/*
 * This file is part of nativedialogs for java
 * Copyright (C) 2016 Gregory Hedlund &lt;ghedlund@mun.ca&gt;
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

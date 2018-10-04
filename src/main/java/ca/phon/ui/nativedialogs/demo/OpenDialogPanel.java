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
package ca.phon.ui.nativedialogs.demo;

import javax.swing.JCheckBox;

import ca.phon.ui.nativedialogs.NativeDialogProperties;
import ca.phon.ui.nativedialogs.OpenDialogProperties;

public class OpenDialogPanel extends SaveDialogPanel {

	private static final long serialVersionUID = -9043090109668356124L;
	
	/*
	 * UI components
	 */
	private JCheckBox canSelectFilesBox;
	
	private JCheckBox canSelectFoldersBox;
	
	private JCheckBox allowMultipleSelectionBox;

	public OpenDialogPanel() {
		super();
		
		init();
	}
	
	private void init() {
		canSelectFilesBox = new JCheckBox("Can select files");
		addRow("Selection:", null, canSelectFilesBox);
		
		canSelectFoldersBox = new JCheckBox("Can select folders");
		addRow("", null, canSelectFoldersBox);
		
		allowMultipleSelectionBox = new JCheckBox("Allow multiple selections");
		addRow("", null, allowMultipleSelectionBox);
	}
	
	@Override
	public NativeDialogProperties getProperties() {
		final OpenDialogProperties retVal = new OpenDialogProperties(super.getProperties());
		retVal.setCanChooseDirectories(canSelectFoldersBox.isSelected());
		retVal.setCanChooseFiles(canSelectFilesBox.isSelected());
		retVal.setAllowMultipleSelection(allowMultipleSelectionBox.isSelected());
		return retVal;
	}
	
}

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

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
import javax.swing.JTextField;

import ca.phon.ui.nativedialogs.FontDialogProperties;
import ca.phon.ui.nativedialogs.NativeDialogProperties;

public class FontDialogPanel extends NativeDialogPanel {

	private static final long serialVersionUID = 8681082246010800006L;
	
	private JTextField fontNameField;
	
	private JTextField fontSizeField;
	
	private JCheckBox boldBox;
	
	private JCheckBox italicBox;

	public FontDialogPanel() {
		super();
		
		init();
	}
	
	private void init() {
		fontNameField = new JTextField();
		addRow("Font name:", null, fontNameField);
		
		fontSizeField = new JTextField();
		addRow("Font size:", null, fontSizeField);
		
		boldBox = new JCheckBox("Bold");
		italicBox = new JCheckBox("Italic");
		addRow("Style:", boldBox, italicBox);
	}
	
	@Override
	public NativeDialogProperties getProperties() {
		final FontDialogProperties retVal = new FontDialogProperties(super.getProperties());
		if(fontNameField.getText().length() > 0)
			retVal.setFontName(fontNameField.getText());
		if(fontSizeField.getText().length() > 0) 
			retVal.setFontSize(Integer.parseInt(fontSizeField.getText()));
		retVal.setBold(boldBox.isSelected());
		retVal.setItalic(italicBox.isSelected());
		return retVal;
	}
	
}

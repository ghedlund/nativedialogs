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

import ca.phon.ui.nativedialogs.MessageDialogProperties;
import ca.phon.ui.nativedialogs.NativeDialogProperties;

public class MessageDialogPanel extends NativeDialogPanel {

	private static final long serialVersionUID = 2561160374176776133L;
	
	/*
	 * UI
	 */
	private JTextField headerField;
	
	private JTextField messageField;
	
	private JTextField optionsField;
	
	private JTextField defaultOptionField;
	
	private JCheckBox showSuppressionBox;
	
	private JTextField suppressionMessageField;

	public MessageDialogPanel() {
		super();
		
		init();
	}
	
	private void init() {
		headerField = new JTextField();
		addRow("Header:", null, headerField);
		
		messageField = new JTextField();
		addRow("Message:", null, messageField);
		
		optionsField = new JTextField();
		addRow("Options:", null, optionsField);
		
		defaultOptionField = new JTextField();
		addRow("Default option:", null, defaultOptionField);
		
		showSuppressionBox = new JCheckBox("Show suppression box");
		addRow("Suppression box:", null, showSuppressionBox);
		
		suppressionMessageField = new JTextField();
		addRow("Suppression message:", null, suppressionMessageField);
	}
	
	@Override
	public NativeDialogProperties getProperties() {
		final MessageDialogProperties retVal = new MessageDialogProperties(super.getProperties());
		retVal.setHeader(headerField.getText());
		retVal.setMessage(messageField.getText());
		
		if(optionsField.getText().length() > 0) {
			final String[] options = optionsField.getText().split(";");
			retVal.setOptions(options);
		}
		
		if(defaultOptionField.getText().length() > 0) {
			retVal.setDefaultOption(defaultOptionField.getText());
		}
		
		retVal.setShowSuppressionBox(showSuppressionBox.isSelected());
		
		if(suppressionMessageField.getText().length() > 0) {
			retVal.setSuppressionMessage(suppressionMessageField.getText());
		}
		
		return retVal;
	}
	
}

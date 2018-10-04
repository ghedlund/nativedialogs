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

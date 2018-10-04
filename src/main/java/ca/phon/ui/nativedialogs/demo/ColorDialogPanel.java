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

import javax.swing.JTextField;

import ca.phon.ui.nativedialogs.ColorDialogProperties;
import ca.phon.ui.nativedialogs.NativeDialogProperties;

public class ColorDialogPanel extends NativeDialogPanel {

	private static final long serialVersionUID = -5166541786675684650L;

	/*
	 * UI
	 */
	private JTextField redField;
	
	private JTextField greenField;
	
	private JTextField blueField;
	
	private JTextField alphaField;
	
	public ColorDialogPanel() {
		super();
		
		init();
	}
	
	private void init() {
		redField = new JTextField();
		addRow("Red:", null, redField);
		
		greenField = new JTextField();
		addRow("Green:", null, greenField);
		
		blueField = new JTextField();
		addRow("Blue:", null, blueField);
		
		alphaField = new JTextField();
		addRow("Alpha:", null, alphaField);
	}
	
	@Override
	public NativeDialogProperties getProperties() {
		final ColorDialogProperties retVal = new ColorDialogProperties(super.getProperties());
		if(redField.getText().length() > 0)
			retVal.setRed(Integer.parseInt(redField.getText()));
		if(greenField.getText().length() > 0)
			retVal.setGreen(Integer.parseInt(greenField.getText()));
		if(blueField.getText().length() > 0)
			retVal.setBlue(Integer.parseInt(blueField.getText()));
		if(alphaField.getText().length() > 0)
			retVal.setAlpha(Integer.parseInt(alphaField.getText()));
		return retVal;
	}
	
}

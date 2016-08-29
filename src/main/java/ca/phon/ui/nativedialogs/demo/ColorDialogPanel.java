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

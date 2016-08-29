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

import java.awt.Color;
import java.util.Map;

/**
 * Color dialog propeties
 *
 */
public class ColorDialogProperties extends NativeDialogProperties {

	private static final long serialVersionUID = -6584547167111975110L;

	/*
	 * Color values
	 */
	private final static String RED = "red";
	
	private final static String GREEN = "green";
	
	private final static String BLUE = "blue";
	
	private final static String ALPHA = "alpha";
	
	public ColorDialogProperties() {
		super();
	}
	
	public ColorDialogProperties(Map<String, Object> props) {
		super(props);
	}
	
	public int getRed() {
		int retVal = 0;
		if(get(RED) != null) {
			retVal = Integer.parseInt(get(RED).toString());
		}
		return retVal;
	}
	
	public void setRed(int red) {
		put(RED, red);
	}
	
	public int getGreen() {
		int retVal = 0;
		if(get(GREEN) != null) {
			retVal = Integer.parseInt(get(GREEN).toString());
		}
		return retVal;
	}
	
	public void setGreen(int green) {
		put(GREEN, green);
	}
	
	public int getBlue() {
		int retVal = 0;
		if(get(BLUE) != null) {
			retVal = Integer.parseInt(get(BLUE).toString());
		}
		return retVal;
	}
	
	public void setBlue(int blue) {
		put(BLUE, blue);
	}
	
	public int getAlpha() {
		int retVal = 0;
		if(get(ALPHA) != null) {
			retVal = Integer.parseInt(get(ALPHA).toString());
		}
		return retVal;
	}
	
	public void setAlpha(int alpha) {
		put(ALPHA, alpha);
	}
	
	public void setColor(Color c) {
		setRed(c.getRed());
		setGreen(c.getGreen());
		setBlue(c.getBlue());
		setAlpha(c.getAlpha());
	}
	
	public Color getColor() {
		return new Color(getRed(), getGreen(), getBlue(), getAlpha());
	}
}

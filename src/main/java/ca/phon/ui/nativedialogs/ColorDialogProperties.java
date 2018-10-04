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

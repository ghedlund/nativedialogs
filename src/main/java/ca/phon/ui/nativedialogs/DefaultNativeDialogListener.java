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

import java.util.logging.Logger;

public class DefaultNativeDialogListener implements NativeDialogListener {

	private final static Logger LOGGER = Logger.getLogger(DefaultNativeDialogListener.class.getName());
	
	@Override
	public void nativeDialogEvent(NativeDialogEvent event) {
		LOGGER.fine(event.getClass().getName() + ",result=" + event.getDialogResult() + ",value=" + event.getDialogData());
	}

}

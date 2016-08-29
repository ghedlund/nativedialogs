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

import java.awt.Window;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Base class for native dialogs configuration.
 */
public class NativeDialogProperties extends HashMap<String, Object> {
	
	final private static Logger LOGGER = Logger.getLogger(NativeDialogProperties.class.getName());

	private static final long serialVersionUID = 9063069448733408216L;

	/**
	 * Should the dialog block the current thread
	 * 
	 */
	public static final String RUN_ASYNC = "run_async";
	
	/**
	 * The listener property
	 */
	public static final String LISTENER = "listener";
	
	/**
	 * Dialog title
	 */
	public static final String TITLE = "title";
	
	/**
	 * Parent window
	 */
	public static final String PARENT_WINDOW = "parent_window";
	
	/**
	 * Force use of swing dialog
	 */
	public static final String FORCE_USE_SWING = "force_use_swing";
	
	/**
	 * Constructor
	 */
	public NativeDialogProperties() {
		super();
		setListener(new DefaultNativeDialogListener());
	}
	
	public NativeDialogProperties(Map<String, Object> props) {
		super(props);
		setListener(new DefaultNativeDialogListener());
	}
	
	public boolean isRunAsync() {
		Boolean retVal = Boolean.TRUE;
		if(get(RUN_ASYNC) != null) {
			retVal = Boolean.valueOf(get(RUN_ASYNC).toString());
		}
		return retVal;
	}
	
	public void setRunAsync(boolean async) {
		put(RUN_ASYNC, async);
	}
	
	public NativeDialogListener getListener() {
		NativeDialogListener retVal = new DefaultNativeDialogListener();
		if(get(LISTENER) != null) {
			retVal = (NativeDialogListener)get(LISTENER);
		}
		return retVal;
	}
	
	public void setListener(NativeDialogListener listener) {
		put(LISTENER, listener);
	}
	
	public String getTitle() {
		String retVal = "";
		if(get(TITLE) != null) {
			retVal = get(TITLE).toString();
		}
		return retVal;
	}
	
	public void setTitle(String title) {
		put(TITLE, title);
	}
	
	public Window getParentWindow() {
		Window retVal = null;
		if(get(PARENT_WINDOW) != null) {
			retVal = (Window)get(PARENT_WINDOW);
		}
		return retVal;
	}
	
	public void setParentWindow(Window window) {
		put(PARENT_WINDOW, window);
	}
	
	public boolean isForceUseSwing() {
		Boolean retVal = Boolean.FALSE;
		if(get(FORCE_USE_SWING) != null) {
			retVal = Boolean.valueOf(get(FORCE_USE_SWING).toString());
		}
		return retVal;
	}
	
	public void setForceUseSwing(boolean forceUseSwing) {
		put(FORCE_USE_SWING, forceUseSwing);
	}
	
}

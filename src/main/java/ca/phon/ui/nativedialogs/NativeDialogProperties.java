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

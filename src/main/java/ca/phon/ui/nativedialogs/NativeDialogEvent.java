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

/**
 * Native dialogs will fire this
 * event when a user closes the dialog.
 * 
 *
 */
public class NativeDialogEvent {
	// class
	/** The OK Option */
	public static final int OK_OPTION = 0x01;
	/** The Cancel Option */
	public static final int CANCEL_OPTION = 0x02;
	
	/** The YES option */
	public static final int YES_OPTION = 0x01;
	/** The NO option */
	public static final int NO_OPTION = 0x03;
	
	/** Unknown */
	public static final int UNKNOWN = 0x04;

	// instance
	/** The Result */
	private int dialogResult;
	/** Data */
	private Object dialogData;
	
	/** Constructor */
	public NativeDialogEvent() {
		this(UNKNOWN);
	}
	
	public NativeDialogEvent(int result) {
		this(result, null);
	}
	
	public NativeDialogEvent(int result, Object data) {
		super();
		
		this.dialogResult = result;
		this.dialogData = data;
	}

	public Object getDialogData() {
		return dialogData;
	}

	public void setDialogData(Object dialogData) {
		this.dialogData = dialogData;
	}

	public int getDialogResult() {
		return dialogResult;
	}

	public void setDialogResult(int dialogResult) {
		this.dialogResult = dialogResult;
	}
	
	
}

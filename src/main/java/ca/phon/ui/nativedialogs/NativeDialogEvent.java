/*
 * Phon - An open source tool for research in phonology.
 * Copyright (C) 2008 The Phon Project, Memorial University <http://phon.ling.mun.ca>
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

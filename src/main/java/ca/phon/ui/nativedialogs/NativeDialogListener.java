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
 * Interface to listen for native dialog events.
 * An instance of a listener should be instanstiated
 * and sent to the native dialog method.
 * 
 * The method <CODE>nativeDialogEvent(NativeDialogEvent event)</CODE>
 * is called when a button on the dialog is pressed.
 * 
 *
 */
public interface NativeDialogListener {
	
	/**
	 * The method is called when an action on the dialog
	 * is performed by the user. (Such as clicking a button).
	 * 
	 * @param NativeDialogEvent event the dialog event.
	 */
	public void nativeDialogEvent(NativeDialogEvent event);
}

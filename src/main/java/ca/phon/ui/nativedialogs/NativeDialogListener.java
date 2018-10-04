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

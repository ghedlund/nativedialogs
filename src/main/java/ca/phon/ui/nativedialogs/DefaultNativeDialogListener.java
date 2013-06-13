package ca.phon.ui.nativedialogs;

import java.util.logging.Logger;

public class DefaultNativeDialogListener implements NativeDialogListener {

	private final static Logger LOGGER = Logger.getLogger(DefaultNativeDialogListener.class.getName());
	
	@Override
	public void nativeDialogEvent(NativeDialogEvent event) {
		LOGGER.fine(event.getClass().getName() + ",result=" + event.getDialogResult() + ",value=" + event.getDialogData());
	}

}

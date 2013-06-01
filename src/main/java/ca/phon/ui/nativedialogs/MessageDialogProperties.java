package ca.phon.ui.nativedialogs;

import java.awt.Image;
import java.lang.reflect.Array;

/**
 * Message dialogs have a header, message
 * and up to three buttons: default, other and alternate.
 * 
 * Button 'default' usually has 'Ok' or 'Yes' as text
 * Button 'other' usually has 'Cancel' or 'No' as text
 * button 'alternate' usually has 'Cancel' as text when visible
 * 
 */
public class MessageDialogProperties extends NativeDialogProperties {

	private static final long serialVersionUID = 402686740289620899L;

	/**
	 * Header text
	 */
	public final static String HEADER = "header";
	
	/**
	 * Message text
	 */
	public final static String MESSAGE = "message";
	
	/**
	 * Button options - array String values
	 */
	public final static String OPTIONS = "options";
	
	/**
	 * Default option
	 */
	public final static String DEFAULT_OPTION = "default_option";
	
	/**
	 * Show supression checkbox
	 */
	public final static String SHOW_SUPRESSION_BOX = "show_supression_box";
	
	/**
	 * Supression box message
	 */
	public final static String SUPRESSION_MESSAGE = "supression_message";
	
	/**
	 * Icon
	 */
	public final static String ICON = "icon";
	
	/**
	 * Options 
	 */
	public final static String[] okOptions = new String[] { "Ok" };
	
	public final static String[] okCancelOptions = new String[] { "Ok", "Cancel" };
	
	public final static String[] yesNoOptions = new String[] { "Yes", "No" };
	
	public final static String[] yesNoCancelOptions = new String[] { "Yes", "No", "Cancel" };
	
	/**
	 * Constructor
	 */
	public MessageDialogProperties() {
		super();
	}
	
	public String getHeader() {
		String retVal = "";
		if(get(HEADER) != null) {
			retVal = get(HEADER).toString();
		}
		return retVal;
	}
	
	public void setHeader(String header) {
		put(HEADER, header);
	}
	
	public String getMessage() {
		String retVal = "";
		if(get(MESSAGE) != null) {
			retVal = get(MESSAGE).toString();
		}
		return retVal;
	}
	
	public void setMessage(String message) {
		put(MESSAGE, message);
	}
	
	public String[] getOptions() {
		String[] retVal = new String[] { "Ok" };
		if(get(OPTIONS) != null && get(OPTIONS) instanceof Array) {
			retVal = (String[])get(OPTIONS);
		}
		return retVal;
	}
	
	public void setOptions(String[] options) {
		put(OPTIONS, options);
	}
	
	public String getDefaultOption() {
		String retVal = (getOptions().length > 0 ? getOptions()[0] : null);
		if(get(DEFAULT_OPTION) != null) {
			retVal = get(DEFAULT_OPTION).toString();
		}
		return retVal;
	}
	
	public void setDefaultOption(String defaultOption) {
		put(DEFAULT_OPTION, defaultOption);
	}
	
	public boolean isShowSupressionBox() {
		Boolean retVal = Boolean.FALSE;
		if(get(SHOW_SUPRESSION_BOX) != null) {
			retVal = Boolean.valueOf(get(SHOW_SUPRESSION_BOX).toString());
		}
		return retVal;
	}
	
	public void setShowSupressionBox(boolean showSupressionBox) {
		put(SHOW_SUPRESSION_BOX, showSupressionBox);
	}
	
	public String getSupressionMessage() {
		String retVal = null;
		if(get(SUPRESSION_MESSAGE) != null) {
			retVal = get(SUPRESSION_MESSAGE).toString();
		}
		return retVal;
	}
	
	public void setSupressionMessage(String supressionMessage) {
		put(SUPRESSION_MESSAGE, supressionMessage);
	}
	
	public Image getIcon() {
		Image retVal = null;
		if(get(ICON) != null && get(ICON) instanceof Image) {
			retVal = (Image)get(ICON);
		}
		return retVal;
	}
	
	public void setIcon(Image icon) {
		put(ICON, icon);
	}
}


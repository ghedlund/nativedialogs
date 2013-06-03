package ca.phon.ui.nativedialogs;

import java.awt.Font;
import java.util.Map;

public class FontDialogProperties extends NativeDialogProperties {

	private static final long serialVersionUID = 3587841806667155174L;
	
	/**
	 * Font name
	 */
	private final static String FONT_NAME = "font_name";
	
	/**
	 * Size
	 */
	private final static String FONT_SIZE = "font_size";
	
	/**
	 * Bold
	 */
	private final static String BOLD = "bold";
	
	/**
	 * Italic
	 */
	private final static String ITALIC = "italic";
	
	public FontDialogProperties() {
		super();
	}
	
	public FontDialogProperties(Map<String, Object> props) {
		super(props);
	}

	public String getFontName() {
		String retVal = "default";
		if(get(FONT_NAME) != null) {
			retVal = get(FONT_NAME).toString();
		}
		return retVal;
	}
	
	public void setFontName(String fontName) {
		put(FONT_NAME, fontName);
	}
	
	public int getFontSize() {
		int retVal = 10;
		if(get(FONT_SIZE) != null) {
			retVal = Integer.parseInt(get(FONT_SIZE).toString());
		}
		return retVal;
	}
	
	public void setFontSize(int size) {
		put(FONT_SIZE, size);
	}
	
	public boolean isBold() {
		boolean retVal = false;
		if(get(BOLD) != null) {
			retVal = Boolean.valueOf(get(BOLD).toString());
		}
		return retVal;
	}
	
	public void setBold(boolean b) {
		put(BOLD, b);
	}
	
	public boolean isItalic() {
		boolean retVal = false;
		if(get(ITALIC) != null) {
			retVal = Boolean.valueOf(get(ITALIC).toString());
		}
		return retVal;
	}
	
	public void setItalic(boolean i) {
		put(ITALIC, i);
	}
	
	/**
	 * Helper method to create the java font described
	 * by the above properties.
	 */
	public Font createFont() {
		int style = 
				(isBold() ? Font.BOLD : 0) |
				(isItalic() ? Font.ITALIC : 0);
		
		final Font retVal = new Font(getFontName(), style, getFontSize());
		return retVal;
	}
}

package ca.phon.ui.nativedialogs.demo;

import javax.swing.JCheckBox;
import javax.swing.JTextField;

import ca.phon.ui.nativedialogs.FontDialogProperties;
import ca.phon.ui.nativedialogs.NativeDialogProperties;

public class FontDialogPanel extends NativeDialogPanel {

	private static final long serialVersionUID = 8681082246010800006L;
	
	private JTextField fontNameField;
	
	private JTextField fontSizeField;
	
	private JCheckBox boldBox;
	
	private JCheckBox italicBox;

	public FontDialogPanel() {
		super();
		
		init();
	}
	
	private void init() {
		fontNameField = new JTextField();
		addRow("Font name:", null, fontNameField);
		
		fontSizeField = new JTextField();
		addRow("Font size:", null, fontSizeField);
		
		boldBox = new JCheckBox("Bold");
		italicBox = new JCheckBox("Italic");
		addRow("Style:", boldBox, italicBox);
	}
	
	@Override
	public NativeDialogProperties getProperties() {
		final FontDialogProperties retVal = new FontDialogProperties(super.getProperties());
		if(fontNameField.getText().length() > 0)
			retVal.setFontName(fontNameField.getText());
		if(fontSizeField.getText().length() > 0) 
			retVal.setFontSize(Integer.parseInt(fontSizeField.getText()));
		retVal.setBold(boldBox.isSelected());
		retVal.setItalic(italicBox.isSelected());
		return retVal;
	}
	
}

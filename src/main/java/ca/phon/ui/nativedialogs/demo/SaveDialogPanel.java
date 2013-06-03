package ca.phon.ui.nativedialogs.demo;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import ca.phon.ui.nativedialogs.FileFilter;
import ca.phon.ui.nativedialogs.NativeDialogProperties;
import ca.phon.ui.nativedialogs.SaveDialogProperties;

public class SaveDialogPanel extends NativeDialogPanel {
	
	private static final long serialVersionUID = 1377784587071739470L;

	/*
	 * UI components
	 */
	private JCheckBox canCreateFoldersBox;
	
	private JTextField allowedTypesField;
	
	private JTextField initialFolderField;
	
	private JTextField initialFileField;
	
	private JTextField promptField;
	
	private JTextField messageField;
	
	private JTextField nameField;
	
	private JCheckBox showHiddenBox;
	
//	private JCheckBox hideExtBox;
//	
//	private JCheckBox canSelectHideExtBox;
//	
//	private JCheckBox tpadBox;
	
	public SaveDialogPanel() {
		super();
		
		init();
	}
	
	private void init() {
		nameField = new JTextField();
		addRow("Name field label: ", null, nameField);

		messageField = new JTextField();
		addRow("Message:", null, messageField);
		
		promptField = new JTextField();
		addRow("Prompt:", null, promptField);
		
		initialFolderField = new JTextField();
		addRow("Initial folder:", null, initialFolderField);
		
		initialFileField = new JTextField();
		addRow("Initial filename:", null, initialFileField);
		
		allowedTypesField = new JTextField();
		addRow("Allowed types:", null, allowedTypesField);
	
		canCreateFoldersBox = new JCheckBox("Can create folders");
//		tpadBox = new JCheckBox("Treat packages as folders (mac)");
		addRow("Folders:", null, canCreateFoldersBox);
//		addRow("", null, tpadBox);
		
		showHiddenBox = new JCheckBox("Show hidden files");
		addRow("Hidden files:", null, showHiddenBox);
		
//		hideExtBox = new JCheckBox("Hide extension");
//		canSelectHideExtBox = new JCheckBox("Can select hide extension");
//		addRow("Extensions:", null, hideExtBox);
//		addRow("", null, canSelectHideExtBox);
		
		
	}
	
	@Override
	public NativeDialogProperties getProperties() {
		final SaveDialogProperties retVal = new SaveDialogProperties(super.getProperties());
		if(nameField.getText().length() > 0) {
			retVal.setNameFieldLabel(nameField.getText());
		}
		if(messageField.getText().length() > 0) {
			retVal.setMessage(messageField.getText());
		}
		if(promptField.getText().length() > 0) {
			retVal.setPrompt(promptField.getText());
		}
		if(initialFolderField.getText().length() > 0) {
			retVal.setInitialFolder(initialFolderField.getText());
		}
		if(initialFileField.getText().length() > 0) {
			retVal.setInitialFile(initialFileField.getText());
		}
		if(allowedTypesField.getText().length() > 0) {
			final FileFilter filter = new FileFilter("Custom", allowedTypesField.getText());
			retVal.setFileFilter(filter);
		}
		retVal.setCanCreateDirectories(canCreateFoldersBox.isSelected());
//		retVal.setTreatPackagesAsDirectories(tpadBox.isSelected());
		retVal.setShowHidden(showHiddenBox.isSelected());
//		retVal.setHideExtension(hideExtBox.isSelected());
//		retVal.setCanSelectHideExtension(canSelectHideExtBox.isSelected());
		
		return retVal;
	}
}

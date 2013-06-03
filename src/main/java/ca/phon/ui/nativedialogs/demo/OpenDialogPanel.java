package ca.phon.ui.nativedialogs.demo;

import javax.swing.JCheckBox;

import ca.phon.ui.nativedialogs.NativeDialogProperties;
import ca.phon.ui.nativedialogs.OpenDialogProperties;

public class OpenDialogPanel extends SaveDialogPanel {

	private static final long serialVersionUID = -9043090109668356124L;
	
	/*
	 * UI components
	 */
	private JCheckBox canSelectFilesBox;
	
	private JCheckBox canSelectFoldersBox;
	
	private JCheckBox allowMultipleSelectionBox;

	public OpenDialogPanel() {
		super();
		
		init();
	}
	
	private void init() {
		canSelectFilesBox = new JCheckBox("Can select files");
		addRow("Selection:", null, canSelectFilesBox);
		
		canSelectFoldersBox = new JCheckBox("Can select folders");
		addRow("", null, canSelectFoldersBox);
		
		allowMultipleSelectionBox = new JCheckBox("Allow multiple selections");
		addRow("", null, allowMultipleSelectionBox);
	}
	
	@Override
	public NativeDialogProperties getProperties() {
		final OpenDialogProperties retVal = new OpenDialogProperties(super.getProperties());
		retVal.setCanChooseDirectories(canSelectFoldersBox.isSelected());
		retVal.setCanChooseFiles(canSelectFilesBox.isSelected());
		retVal.setAllowMultipleSelection(allowMultipleSelectionBox.isSelected());
		return retVal;
	}
	
}

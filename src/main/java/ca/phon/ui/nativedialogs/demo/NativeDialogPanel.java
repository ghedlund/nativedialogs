package ca.phon.ui.nativedialogs.demo;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.lang.ref.WeakReference;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

import ca.phon.ui.nativedialogs.NativeDialogEvent;
import ca.phon.ui.nativedialogs.NativeDialogListener;
import ca.phon.ui.nativedialogs.NativeDialogProperties;
import ca.phon.ui.nativedialogs.NativeDialogs;

public class NativeDialogPanel extends JPanel {

	private static final long serialVersionUID = 1001534407995695438L;

	/*
	 * UI components
	 */
	private JCheckBox attachToFrameBox;
	
	private JCheckBox useSwingBox;
	
	private JTextField titleField;
	
	private JTextArea resultArea;
	
	private JButton showDialogButton;
	
	private JPanel formPanel;
	
	private final NativeDialogProperties props = new NativeDialogProperties();
	
	public NativeDialogPanel() {
		super();
		
		init();
	}
	
	
	private void init() {
		setLayout(new BorderLayout());
		
		// button bar
		final ShowDialogAction showDialogAct = new ShowDialogAction();
		showDialogButton = new JButton(showDialogAct);
		
		formPanel = new JPanel(new GridBagLayout());
		setupForm();
		
		final JScrollPane formScroller = new JScrollPane(formPanel);
		add(formScroller, BorderLayout.CENTER);
		
		final JPanel btmPanel = new JPanel(new BorderLayout());
		resultArea = new JTextArea();
		resultArea.setRows(3);
		final JScrollPane resultScroller = new JScrollPane(resultArea);
		btmPanel.add(resultScroller, BorderLayout.CENTER);
		
		final JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		btnPanel.add(showDialogButton);
		
		btmPanel.add(btnPanel, BorderLayout.SOUTH);
		add(btmPanel, BorderLayout.SOUTH);
	}
	
	private void setupForm() {
		titleField = new JTextField();
		addRow("Title:", null, titleField);
		
		attachToFrameBox = new JCheckBox("Make current frame parent of dialog. (Display as sheet on Mac OS X)");
		attachToFrameBox.setSelected(true);
		addRow("Attach to frame:", null, attachToFrameBox);
		
		useSwingBox = new JCheckBox("Force use of swing dialog.");
		addRow("Use Swing(TM):", null, useSwingBox);
	}
	
	/**
	 * Add a row to this panel's form
	 * 
	 * @param label
	 * @param comp1
	 * @param comp2
	 */
	public void addRow(String title, Component middle, Component end) {
		final FormUtility formUtility = new FormUtility();
		formUtility.addLabel(title, formPanel);
		if(middle != null)
			formUtility.addMiddleField(middle, formPanel);
		formUtility.addLastField(end, formPanel);
	}
	
	public NativeDialogProperties getProperties() {
		final NativeDialogProperties retVal = new NativeDialogProperties();
		retVal.setRunAsync(false);
		retVal.setTitle(titleField.getText());
		retVal.setListener(listener);
		retVal.setForceUseSwing(useSwingBox.isSelected());
		if(attachToFrameBox.isSelected()) {
			// get parent frame
			final JFrame parentFrame = (JFrame)SwingUtilities.getAncestorOfClass(JFrame.class, this);
			if(parentFrame != null) {
				retVal.setParentWindow(parentFrame);
			}
		}
		return retVal;
	}
	
	private final NativeDialogListener listener = new NativeDialogListener() {
		
		@Override
		public void nativeDialogEvent(NativeDialogEvent event) {
			final StringBuilder resultBuilder = new StringBuilder();
			resultBuilder.append("Result Code: ");
			resultBuilder.append(event.getDialogResult());
			if(event.getDialogData() != null) {
				resultBuilder.append("\nValue: ");
				resultBuilder.append(event.getDialogData().toString());
			}
			
			resultArea.setText(resultBuilder.toString());
		}
	};
	
	public class ShowDialogAction extends AbstractAction {

		private static final long serialVersionUID = 1491167350268153370L;

		public ShowDialogAction() {
			super();
			super.putValue(NAME, "Show Dialog");
			super.putValue(SHORT_DESCRIPTION, "Show dialog...");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			NativeDialogs.showDialog(getProperties());
		}

	}
}

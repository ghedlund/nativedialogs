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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Shows a frame with buttons for displaying the 
 * various dialogs in the NativeDialogs class
 * 
 *
 */
public class NativeDialogsTest extends JFrame {
	/** The Buttons */
	private JButton browseForFileButton;
	private JButton browseForDirButton;
	private JButton saveAsButton;
	private JButton yesNoCancelButton;
	private JButton yesNoButton;
	private JButton okCancelButton;
	private JButton messageButton;
	private JButton fontSelectionButton;
	private JButton colorSelectionButton;
	
	/** Extra interface stuff */
	private JLabel fileField;
	private JLabel colourLabel;
	private JLabel fontLabel;
	
	/** Constructor */
	public NativeDialogsTest() {
		super("Native Dialogs Demo");
		
		initDisplay();
	}
	
	private void initDisplay() {
		// setup layout
		setLayout(new BorderLayout());
		
		final Action browseAct = new AbstractAction() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				final FileFilter[] filters = { FileFilter.xmlFilter, FileFilter.jsFilter };
				
				final NativeDialogListener listener = new NativeDialogListener() {
					
					@Override
					public void nativeDialogEvent(NativeDialogEvent event) {
						System.out.println(event.getDialogResult());
						if(event.getDialogData() != null) {
							System.out.println("L:" + event.getDialogData());
						}
					}
				};
				
				final SaveDialogProperties props = new SaveDialogProperties();
				props.setRunAsync(false);
				props.setParentWindow(NativeDialogsTest.this);
				props.setListener(listener);
				props.setInitialFolder("/Users/ghedlund/Desktop");
				props.setInitialFile("hello");
				props.setFileFilter(new FileFilter(filters));
				props.setTitle("Save as...");
				props.setPrompt("Save Query");
				props.setCanCreateDirectories(true);
				props.setShowHidden(false);
				props.setMessage("Hello world");
				props.setForceUseSwing(true);
				
//				props.setHideExtension(true);
//				props.setCanSelectHideExtension(true);
				//props.setTreatPackagesAsDirectories(true);
				
				String path = NativeDialogs.showSaveDialog(props);
				if(path != null) {
					System.out.println("R:" + path);
				}
				
				final MessageDialogProperties messageProps = new MessageDialogProperties();
				messageProps.setParentWindow(NativeDialogsTest.this);
				messageProps.setOptions(MessageDialogProperties.yesNoCancelOptions);
				messageProps.setRunAsync(false);
				messageProps.setHeader("Hello world");
				messageProps.setMessage("This is a test!");
				
				
				int retVal = NativeDialogs.showMessageDialog(messageProps);
				System.out.println(retVal);
			}
		};
		browseAct.putValue(Action.NAME, "Browse for file...");
		
		final JButton btn = new JButton(browseAct);
		add(btn, BorderLayout.CENTER);
	}

	public static void main(String[] args) {
		final Runnable onEDT = new Runnable() {
			
			@Override
			public void run() {
				final NativeDialogsTest testFrame = new NativeDialogsTest();
				testFrame.pack();
				testFrame.setVisible(true);
			}
		};
		SwingUtilities.invokeLater(onEDT);
	}
}

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
package ca.phon.ui.nativedialogs.demo;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

/**
 * Demo frame for native dialogs.
 *
 */
public class NativeDialogsDemo extends JFrame {
	
	private static final long serialVersionUID = 6418535207662766158L;
	
	private SaveDialogPanel saveDialogPanel;
	
	private OpenDialogPanel openDialogPanel;
	
	private MessageDialogPanel messageDialogPanel;
	
	private FontDialogPanel fontDialogPanel;
	
	private ColorDialogPanel colorDialogPanel;

	public NativeDialogsDemo() {
		super("Native Dialogs Demo");
		
		init();
		
		setDefaultCloseOperation(EXIT_ON_CLOSE);
	}
	
	private void init() {
		final JTabbedPane tabPane = new JTabbedPane();
		
		saveDialogPanel = new SaveDialogPanel();
		tabPane.addTab("Save Dialog", saveDialogPanel);
		
		openDialogPanel = new OpenDialogPanel();
		tabPane.addTab("Open Dialog", openDialogPanel);
		
		messageDialogPanel = new MessageDialogPanel();
		tabPane.addTab("Message Dialog", messageDialogPanel);
		
		fontDialogPanel = new FontDialogPanel();
		tabPane.addTab("Font Dialog", fontDialogPanel);
		
		colorDialogPanel = new ColorDialogPanel();
		tabPane.addTab("Color Dialog", colorDialogPanel);
		
		setLayout(new BorderLayout());
		add(tabPane, BorderLayout.CENTER);
	}
	
	/**
	 * main
	 */
	public static void main(String[] args) {
		final Runnable onEDT = new Runnable() {
			
			@Override
			public void run() {
				final NativeDialogsDemo demo = new NativeDialogsDemo();
				demo.pack();
				demo.setVisible(true);
			}
		};
		SwingUtilities.invokeLater(onEDT);
	}
	
}

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
package ca.phon.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;

public class JFontDialog extends JDialog {
	
	private static final long serialVersionUID = 9031934456118714570L;

	/** The font panel */
	private JFontPanel fontPanel;
	
	/** ok button */
	private JButton okButton;
	
	/** cancel button */
	private JButton cancelButton;
	
	/** Ended ok */
	private boolean ok = false;
	
	public JFontDialog(JFrame frame, boolean modal) {
		this(frame, modal, UIManager.getDefaults().getFont("TextPane.font"));
	}
	
	private JFontDialog(JFrame frame, boolean modal, Font f) {
		super(frame, modal);
		
		init(f);
	}

	public Font getSelectedFont() {
		return fontPanel.getSelectedFont();
	}
	
	public void setSelectedFont(Font f) {
		fontPanel.setSelectedFont(f);
	}
	
	private void init(Font f) {
		
		fontPanel = new JFontPanel(f);
		
		okButton = new JButton("Ok");
		okButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				ok = true;
				dispose();
			}
			
		});
		
		cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
			
		});
		
		final JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonBar.add(cancelButton);
		buttonBar.add(okButton);
		getRootPane().setDefaultButton(okButton);
		
		setLayout(new BorderLayout());
		add(fontPanel, BorderLayout.CENTER);
		add(buttonBar, BorderLayout.SOUTH);
	}

	public boolean isOk() {
		return ok;
	}
}

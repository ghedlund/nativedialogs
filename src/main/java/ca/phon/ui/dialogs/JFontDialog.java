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
package ca.phon.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.UIManager;

import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class JFontDialog extends JDialog {
	
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
	
	public JFontDialog(JFrame frame, boolean modal, Font f) {
		super(frame, modal);
		
		init(f);
	}

	public Font getSelectedFont() {
		return fontPanel.getSelectedFont();
	}
	
	private void init(Font f) {
		FormLayout layout = new FormLayout(
				"5dlu, fill:pref:grow, 5dlu",
				"5dlu, pref, 3dlu, fill:pref:grow, 3dlu, pref, 5dlu");
		getContentPane().setLayout(new BorderLayout());
		CellConstraints cc = new CellConstraints();
		
//		DialogHeader header = new DialogHeader("Choose Font", "");
		
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
		
		JComponent buttonBar = 
			ButtonBarFactory.buildOKCancelBar(okButton, cancelButton);
		
//		add(header, BorderLayout.NORTH);
		add(fontPanel, BorderLayout.CENTER);
		add(buttonBar, BorderLayout.SOUTH);
		
//		add(header, cc.xy(2, 2));
//		add(fontPanel, cc.xy(2, 4));
//		add(buttonBar, cc.xy(2, 6));
	}

	public boolean isOk() {
		return ok;
	}
}

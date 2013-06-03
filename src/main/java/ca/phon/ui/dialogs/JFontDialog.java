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

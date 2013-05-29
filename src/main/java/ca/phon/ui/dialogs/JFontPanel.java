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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jdesktop.swingx.JXList;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;


public class JFontPanel extends JPanel {
	
	/** The font name list */
	private JXList fontNameList;
	
	/** The font size list */
	private JXList fontSizeList;
	
	/** Show all fonts? */
	private JCheckBox suggestedFontsBox;
	
	/** The list of font sizes */
	protected Integer fontSizes[] = { 8, 10, 11, 12, 14, 16, 18,
			20, 24, 30, 36, 40, 48, 60, 72 };
	
	protected String suggestedFonts[] = {
			"Arial", "Arial Unicode MS",
			"Charis SIL", "Courier New", "Doulos SIL",
			"Times New Roman" };
	
	/** Check boxes */
	private JCheckBox boldBox;
	private JCheckBox italicBox;
	
	/** The font property */
	public static final String FONT_PROP = "_font_";
	
	/** The currently selected font */
	private Font selectedFont;
	
	/** Control firing of property changes events. */
	private boolean firePropertyChange = true;
	
	/** The sample display */
	private JLabel sampleDisplay;
	
	private final static String PREVIEW_TEXT = "\u00f0\u0259 \u02c8kw\u026ak \u02c8b\u0279a\u028an \u02c8f\u0251ks \u02c8d\u0292\u028cmps \u02c8o\u028av\u025a \u00f0\u0259 \u02c8le\u026a\u02cczi\u02d0 \u02c8d\u0251\u0261";
	
//	Filter filters[] = {
//			new Filter() {
//				private ArrayList<Integer> toPrevious =
//					 new ArrayList<Integer>();
//				
//				@Override
//				protected void filter() {
//					int inputSize = getInputSize();
//					int current = 0;
//					for(int i = 0; i < inputSize; i++) {
//						if(test(i)) {
//							toPrevious.add(i);
//							
//							fromPrevious[i] = current++;
//						}
//					}
//				}
//				
//				private boolean test(int row) {
//					if(!adapter.isTestable(getColumnIndex()))
//						return false;
//					
//					Object textObj = getInputValue(row, getColumnIndex());
//					if(textObj == null) return false;
//					
//					String text = textObj.toString();
//					
//					boolean retVal = false;
//					for(String s:suggestedFonts) {
//						if(s.equals(text)) {
//							retVal = true;
//							break;
//						}
//					}
//					return retVal;
//				}
//
//				@Override
//				public int getSize() {
//					return toPrevious.size();
//				}
//
//				@Override
//				protected void init() {
//					toPrevious = new ArrayList<Integer>();
//				}
//
//				@Override
//				protected int mapTowardModel(int arg0) {
//					return toPrevious.get(arg0);
//				}
//
//				@Override
//				protected void reset() {
//					toPrevious.clear();
//			        int inputSize = getInputSize();
//			        fromPrevious = new int[inputSize];  // fromPrevious is inherited protected
//			        for (int i = 0; i < inputSize; i++) {
//			            fromPrevious[i] = -1;
//			        }
//				}
//				
//			}
//	};
//	FilterPipeline fp = new FilterPipeline(filters);
	
	/**
	 * Construction
	 */
	public JFontPanel() {
		this(new Font("Courier New", Font.PLAIN, 12));
	}
	
	/**
	 * Setup panel with an initial font.
	 * @param initialFont
	 */
	public JFontPanel(Font initialFont) {
		super();
		
		init(initialFont);
	}
	
	private void init(Font font) {
		GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
		
		String[] fonts = env.getAvailableFontFamilyNames();
		fontNameList = new JXList(fonts);
//		fontNameList.setFilterEnabled(true);
//		fontNameList.setFilters(fp);
		fontNameList.setSelectedValue(font.getFamily(), true);
		fontNameList.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				updateSelectedFont();
			}
			
		});
		
		fontSizeList = new JXList(fontSizes);
		fontSizeList.setSelectedValue(font.getSize(), true);
		fontSizeList.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				updateSelectedFont();
			}
			
		});
		
		boldBox = new JCheckBox("Bold");
		boldBox.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				updateSelectedFont();
			}
			
		});
		boldBox.setSelected(font.isBold());
		
		italicBox = new JCheckBox("Italic");
		italicBox.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				updateSelectedFont();
			}
			
		});
		italicBox.setSelected(font.isItalic());
		
		suggestedFontsBox = new JCheckBox("Only show suggested fonts");
		suggestedFontsBox.setSelected(true);
		suggestedFontsBox.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				//fontNameList.setFilterEnabled(suggestedFontsBox.isSelected());
				if(!suggestedFontsBox.isSelected()) {
//					fontNameList.setFilters(null);
				} else {
//					fontNameList.setFilters(fp);
					fontNameList.revalidate();
				}
			}
			
		});
		
		FormLayout layout = new FormLayout(
				"fill:min(200px;pref):grow, 3dlu, pref, 3dlu, pref:nogrow",
				"pref, pref, 3dlu, pref, 3dlu, fill:pref:grow, pref, fill:50px");
		setLayout(layout);
		
//		SystemProperties props = UserPrefManager.getUserPreferences();
//		if(props.getProperty("pref_font_test_string") == null)
//			props = UserPrefManager.getDefaultUserPreferences();
//		String sampleText = props.getProperty("pref_font_test_string").toString();
		
		sampleDisplay = new SampleLabel();
		sampleDisplay.setBackground(Color.white);
		sampleDisplay.setOpaque(true);
		sampleDisplay.setText(PREVIEW_TEXT);
		sampleDisplay.setHorizontalAlignment(SwingConstants.CENTER);
		sampleDisplay.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
//		sampleDisplay.setMaximumSize(new Dimension(600, 50));
		
		CellConstraints cc = new CellConstraints();
		
//		add(new JLabel("Font Name:"), cc.xy(1, 1));
		add(new JScrollPane(fontNameList), cc.xywh(1,2,1,5));
//		add(new JLabel("Size:"), cc.xy(3, 1));
		add(new JScrollPane(fontSizeList), cc.xywh(3,2,1,5));
//		add(suggestedFontsBox, cc.xy(1, 7));
		add(boldBox, cc.xy(5,2));
		add(italicBox, cc.xy(5, 4));
		add(new JScrollPane(sampleDisplay), cc.xyw(1, 8, 4));
		
		updateSelectedFont();
	}
	
	private void updateSelectedFont() {
		// return if called during the setSelectedFont() method
		if(!firePropertyChange) return;
		
		if(italicBox == null || boldBox == null) return;
		
		Font oldFont = selectedFont;
		
		if(fontNameList.getSelectedValue() == null) return;
		String fontName = fontNameList.getSelectedValue().toString();
		
		if(fontSizeList.getSelectedValue() == null) return;
		Integer fontSize = (Integer)fontSizeList.getSelectedValue();
		
		int flags = 0;
		if(boldBox.isSelected())
			flags |= Font.BOLD;
		if(italicBox.isSelected())
			flags |= Font.ITALIC;
		
		selectedFont = new Font(fontName, flags, fontSize);
		
		if(selectedFont == null)
			selectedFont = super.getFont();
		
		sampleDisplay.setFont(selectedFont);
		sampleDisplay.repaint();
		
		super.firePropertyChange(FONT_PROP, oldFont, selectedFont);
	}

	public Font getSelectedFont() {
		return selectedFont;
	}

	public void setSelectedFont(Font selectedFont) {
		this.selectedFont = selectedFont;
		
		firePropertyChange = false;
		fontNameList.setSelectedValue(selectedFont.getFamily(), true);
		fontSizeList.setSelectedValue(selectedFont.getSize(), true);
		boldBox.setSelected(selectedFont.isBold());
		italicBox.setSelected(selectedFont.isItalic());
		sampleDisplay.setFont(selectedFont);
		sampleDisplay.repaint();
		firePropertyChange = true;
	}
	
	private class SampleLabel extends JLabel implements Scrollable {

		@Override
		public Dimension getPreferredScrollableViewportSize() {
			return getPreferredSize();
		}

		@Override
		public int getScrollableBlockIncrement(Rectangle visibleRect,
				int orientation, int direction) {
			return 60;
		}

		@Override
		public boolean getScrollableTracksViewportHeight() {
			return false;
		}

		@Override
		public boolean getScrollableTracksViewportWidth() {
			return false;
		}

		@Override
		public int getScrollableUnitIncrement(Rectangle visibleRect,
				int orientation, int direction) {
			return 20;
		}
		
	}
}

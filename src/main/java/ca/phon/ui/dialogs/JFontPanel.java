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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;


public class JFontPanel extends JPanel {
	
	private static final long serialVersionUID = 2744815709681620887L;

	/** The font name list */
	private JList fontNameList;
	
	/** The font size list */
	private JList fontSizeList;
	
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
	
	private final static String PREVIEW_TEXT = 
			"\u00f0\u0259 \u02c8kw\u026ak \u02c8b\u0279a\u028an \u02c8f\u0251ks \u02c8d\u0292\u028cmps \u02c8o\u028av\u025a \u00f0\u0259 \u02c8le\u026a\u02cczi\u02d0 \u02c8d\u0251\u0261";
	
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
		fontNameList = new JList(fonts);
//		fontNameList.setFilterEnabled(true);
//		fontNameList.setFilters(fp);
		fontNameList.setSelectedValue(font.getFamily(), true);
		fontNameList.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				updateSelectedFont();
			}
			
		});
		
		fontSizeList = new JList(fontSizes);
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
		
		sampleDisplay = new JLabel();
		sampleDisplay.setBackground(Color.white);
		sampleDisplay.setOpaque(true);
		sampleDisplay.setText(PREVIEW_TEXT);
		sampleDisplay.setHorizontalAlignment(SwingConstants.CENTER);
		
		final GridBagLayout layout = new GridBagLayout();
		setLayout(layout);
		
		GridBagConstraints gbc = new GridBagConstraints();
		
		// fontNameList
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridheight = 3;
		gbc.gridwidth = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		final JScrollPane fontNameScroller = new JScrollPane(fontNameList);
		fontNameScroller.setBorder(BorderFactory.createTitledBorder("Font name:"));
		add(fontNameScroller, gbc);
		
		final JScrollPane fontSizeScroller = new JScrollPane(fontSizeList);
		fontSizeScroller.setBorder(BorderFactory.createTitledBorder("Font size:"));
		gbc = (GridBagConstraints)gbc.clone();
		gbc.gridx = 1;
		add(fontSizeScroller, gbc);
		
		gbc = (GridBagConstraints)gbc.clone();
		gbc.gridx = 2;
		gbc.gridheight = 1;
		gbc.weighty = 0.0;
		add(boldBox, gbc);
		
		gbc = (GridBagConstraints)gbc.clone();
		gbc.gridy = 1;
		add(italicBox, gbc);
		
		final JScrollPane sampleScroller = new JScrollPane(sampleDisplay);
		sampleScroller.setBorder(BorderFactory.createTitledBorder("Preview"));
		sampleScroller.setPreferredSize(sampleDisplay.getPreferredSize());
		gbc = (GridBagConstraints)gbc.clone();
		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.gridwidth = 3;
		gbc.gridheight = 1;
		gbc.weighty = 1.0;
		add(sampleScroller, gbc);
		
//		updateSelectedFont();
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
	
//	private class SampleLabel extends JLabel implements Scrollable {
//
//		private static final long serialVersionUID = -1606948190545345132L;
//
//		@Override
//		public Dimension getPreferredScrollableViewportSize() {
//			return getPreferredSize();
//		}
//
//		@Override
//		public int getScrollableBlockIncrement(Rectangle visibleRect,
//				int orientation, int direction) {
//			return 60;
//		}
//
//		@Override
//		public boolean getScrollableTracksViewportHeight() {
//			return false;
//		}
//
//		@Override
//		public boolean getScrollableTracksViewportWidth() {
//			return false;
//		}
//
//		@Override
//		public int getScrollableUnitIncrement(Rectangle visibleRect,
//				int orientation, int direction) {
//			return 20;
//		}
//		
//	}
}

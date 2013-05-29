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

import java.io.File;

public class FileFilter extends javax.swing.filechooser.FileFilter {
	public String desc;
	public String extensions;
	public FileFilter(String desc, String extensions) {
		super();
		
		this.desc = desc;
		this.extensions = extensions;
	}

	public final static FileFilter phonFilter = 
		new FileFilter("Phon Project Files (*.phon)", "*.phon");
	public final static FileFilter talkbankFilter =
		new FileFilter("Talkbank Files (*.tb;*.xml)", "*.tb;*.xml");
	public final static FileFilter allFilesFilter =
		new FileFilter("All Files (*.*)", "*.*");
	public final static FileFilter mediaFilter = 
		new FileFilter("Media Files", "*.aif;*.mov;*.mpg;*.mpeg;*.mp4;*.wav;*.mp3;*.aiff;*.m4a");
	public final static FileFilter csvFilter = 
		new FileFilter("CSV Files (*.csv;*.txt)", "*.csv;*.txt");
	public final static FileFilter htmlFilter =
		new FileFilter("HTML Files (*.htm;*.html)", "*.htm;*.html");
	public final static FileFilter xmlFilter = 
		new FileFilter("XML Files (*.xml)", "*.xml");
	public final static FileFilter jarFilter =
		new FileFilter("Java Archive Files (*.jar;*.zip)", "*.jar;*.zip");
	public final static FileFilter zipFilter =
		new FileFilter("ZIP Files (*.zip)", "*.zip");
	public final static FileFilter excelFilter = 
		new FileFilter("Excel Files (*.xls)", "*.xls");
	public final static FileFilter jsFilter = 
		new FileFilter("Javascript Files (*.js)", "*.js");
	public final static FileFilter wavFilter = 
		new FileFilter("Wav files (*.wav)", "*.wav");
	
	@Override
	public boolean accept(File f) {
		
		if(f.isDirectory() || this == allFilesFilter) return true;
		
		boolean retVal = false;
		
		String[] validExts = extensions.split(";");
		
		for(String validExt:validExts) {
			if(f.getName().endsWith(
					validExt.substring(validExt.indexOf('.'), validExt.length()))) {
				retVal = true;
				break;
			}
		}
		
		return retVal;
	}
	
	@Override
	public String getDescription() {
		return desc;
	}
	
	public String[] exts() {
		String[] validExts = extensions.split(";");
		String[] retVal = new String[validExts.length];
		for(int i = 0; i < validExts.length; i++)
			retVal[i] = validExts[i].substring(1);
		return retVal;
	}
}

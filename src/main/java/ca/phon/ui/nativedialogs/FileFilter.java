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
package ca.phon.ui.nativedialogs;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileFilter extends javax.swing.filechooser.FileFilter {
	
	public final static FileFilter phonFilter = 
			new FileFilter("Phon Project Files (*.phon)", "phon");
	public final static FileFilter talkbankFilter =
		new FileFilter("Talkbank Files (*.tb;*.xml)", "tb;xml");
	public final static FileFilter allFilesFilter =
		new FileFilter("All Files (*.*)", "*.*");
	public final static FileFilter mediaFilter = 
		new FileFilter("Media Files", "mkv;avi;flv;wmv;mov;mpg;mpeg;mp4;wav;mp3;aif;aiff;m4a");
	public final static FileFilter csvFilter = 
		new FileFilter("CSV Files (csv;txt)", "csv;txt");
	public final static FileFilter htmlFilter =
		new FileFilter("HTML Files (*.htm;*.html)", "htm;html");
	public final static FileFilter xmlFilter = 
		new FileFilter("XML Files (*.xml)", "xml");
	public final static FileFilter jarFilter =
		new FileFilter("Java Archive Files (*.jar;*.zip)", "jar;zip");
	public final static FileFilter zipFilter =
		new FileFilter("ZIP Files (*.zip)", "zip");
	public final static FileFilter excelFilter = 
		new FileFilter("Excel Files (*.xls)", "xls");
	public final static FileFilter jsFilter = 
		new FileFilter("Javascript Files (*.js)", "js");
	public final static FileFilter wavFilter = 
		new FileFilter("Wav files (*.wav)", "wav");
	
	/**
	 * Description (user readable)
	 */
	private String desc;
	
	/**
	 * Extensions allowed in a 
	 */
	private List<String> extensions = new ArrayList<String>();
	
	/**
	 * Default extension
	 */
	private String defaultExtension;
	
	/**
	 * List of sub-filters
	 */
	private List<FileFilter> subFilters = new ArrayList<FileFilter>();
	
	public FileFilter(String desc, String extensions) {
		super();
		
		this.desc = desc;
		
		String[] exts = extensions.split(";");
		this.extensions = Arrays.asList(exts);
	}
	
	public FileFilter(String desc, String extensions, FileFilter[] subfilters) {
		super();
		
		this.desc = desc;
		
		String[] exts = extensions.split(";");
		this.extensions = Arrays.asList(exts);
		
		subFilters.addAll(Arrays.asList(subfilters));
	}
	
	public FileFilter(FileFilter[] subfilters) {
		super();
		
		this.desc = "";
		subFilters.addAll(Arrays.asList(subfilters));
	}
	
	public void addSubFilter(FileFilter subFilter) {
		subFilters.add(subFilter);
	}
	
	public void removeSubFilter(FileFilter subFilter) {
		subFilters.remove(subFilter);
	}
	
	public List<FileFilter> getSubFilters() {
		return this.subFilters;
	}
	
	@Override
	public boolean accept(File f) {
		
		if(f.isDirectory() || this == allFilesFilter) return true;
		
		boolean retVal = false;
		
		for(String validExt:extensions) {
			if(f.getName().toLowerCase().endsWith(validExt.toLowerCase())) {
				retVal = true;
				break;
			}
		}
		
		if(!retVal) {
			// check sub-filters if any
			for(FileFilter subFilter:subFilters) {
				if(subFilter.accept(f)) {
					retVal = true;
					break;
				}
			}
		}
		
		return retVal;
	}
	
	@Override
	public String getDescription() {
		final StringBuilder builder = new StringBuilder(desc);
		
		for(FileFilter subFilter:subFilters) {
			builder.append(
					(builder.length() > 0 ? "; " : "") + subFilter.getDescription());
		}
		
		return builder.toString();
	}
	
	public String getDefaultExtension() {
		if(this.defaultExtension == null) {
			return getAllExtensions().get(0);
		} else {
			return this.defaultExtension;
		}
	}
	
	public void setDefaultExtension(String ext) {
		this.defaultExtension = ext;
		if(!getAllExtensions().contains(defaultExtension)) {
			this.extensions.add(0, defaultExtension);
		}
	}
	
	public List<String> getExtensions() {
		return this.extensions;
	}
	
	public List<String> getAllExtensions() {
		final List<String> retVal = new ArrayList<String>(extensions);
		for(FileFilter subFilter:subFilters) {
			retVal.addAll(subFilter.getAllExtensions());
		}
		return retVal;
	}
	
}

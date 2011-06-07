/*
 * Copyright (C) 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package tgnourse.diveguide;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.os.Environment;

/**
 * @author tgnourse@google.com (Thomas Nourse)
 */
public class ExternalStorageFile {

	private BufferedWriter out;
	private String name;
	private String extension;
	
	ExternalStorageFile(String name, String extension) {
		out = null;
		this.name = name;
		this.extension = extension;
	}
	
	void open() {
		try {
		    File root = Environment.getExternalStorageDirectory();
		    if (root.canWrite()){
		        out = new BufferedWriter(new FileWriter(new File(root, name + "_" + System.currentTimeMillis() + "." + extension), true));
		    } else {
		    	Util.log("Could not write file to root.");
		    }
		} catch (IOException e) {
		    Util.log("Could not write file " + e.getMessage());
		}
	}
	
	void write(String line) {
		if (out != null) {
			try {
				out.write(line);
				out.newLine();
			} catch (IOException e) {
			    Util.log("Could not write to file " + e.getMessage());
			}
		}
	}
	
	void close() {
		if (out != null) {
		try {
			out.close();
		} catch (IOException e) {
		    Util.log("Could not close file " + e.getMessage());
		}
		}
	}
}

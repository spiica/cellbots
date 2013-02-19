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

package com.cellbots.logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Gets the names of all the files in a directory.
 * 
 * @author birmiwal@google.com (Shishir Birmiwal)
 */
public class FileListFetcher {
    /**
     * Returns all the files and directories in the directory.
     * 
     * @param dirname the directory to look into
     * @return an list of file names
     */
    List<String> getFilesAndDirectoriesInDir(String dirname) {
        List<String> files = new ArrayList<String>();
        File dir = new File(dirname);
        String[] entries = dir.list();
        if (entries == null) {
            return files;
        }
        for (String entry : entries) {
            File f = new File(dir, entry);
            files.add(f.getAbsolutePath());
            if (f.isDirectory()) {
                files.addAll(getFilesAndDirectoriesInDir(f.getAbsolutePath()));
            }
        }
        return files;
    }
}

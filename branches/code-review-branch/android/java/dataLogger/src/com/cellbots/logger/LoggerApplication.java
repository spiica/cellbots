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

import android.app.Application;
import android.os.Environment;

import java.util.Date;

public class LoggerApplication extends Application {
    /**
     * A date value is used as a unique identifier for file paths.
     */
    private String filePathUniqueIdentifier;

    @Override
    public void onCreate() {
        super.onCreate();
        generateNewFilePathUniqueIdentifier();
    }

    public void generateNewFilePathUniqueIdentifier() {
        Date date = new Date();
        filePathUniqueIdentifier = date.toGMTString().replaceAll(" ", "_").replaceAll(":", "-");
    }

    public void resetFilePathUniqueIdentifier() {
        filePathUniqueIdentifier = null;
    }

    /**
     * Returns the filePathUniqueIdentifier that can be used for saving files.
     * @throw IllegalStateException if the filePathUniqueIdentifier hasn't been initialized.
     */
    public String getFilePathUniqueIdentifier() {
        if (filePathUniqueIdentifier == null) {
            throw new IllegalStateException(
                  "filePathUniqueIdentifier has not been initialized for the app.");
        }
        return filePathUniqueIdentifier;
    }

    public String getLoggerPathPrefix() {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + "/SmartphoneLoggerData/"
            + getFilePathUniqueIdentifier() + "/";
    }

    public String getDataLoggerPath() {
        return getLoggerPathPrefix() + "data/";
    }

    public String getVideoFilepath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath()
              + "/SmartphoneLoggerData/" + filePathUniqueIdentifier
              + "/video-" + filePathUniqueIdentifier + ".mp4";
    }

    public String getPicturesDirectoryPath() {
        return Environment.getExternalStorageDirectory() + "/SmartphoneLoggerData/"
              + filePathUniqueIdentifier + "/pictures/";
    }
}

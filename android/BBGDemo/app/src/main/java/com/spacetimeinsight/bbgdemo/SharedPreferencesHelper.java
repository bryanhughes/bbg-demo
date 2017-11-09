/*
 * Copyright 2017, SpaceTime-Insight, Inc.
 *
 * This code is supplied as an example of how to use the SpaceTime Warp IoT Nucleus SDK. It is
 * intended solely to demonstrate usage of the SDK and its features, and as a learning by example.
 * This code is not intended for production or commercial use as-is.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this work except
 * in compliance with the License. You may obtain a copy of the License in the LICENSE file, or at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.spacetimeinsight.bbgdemo;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;

import java.io.*;

public class SharedPreferencesHelper {
    private static final String NAME = "SimpleDemo";
    private static final String API_KEY = "API_KEY";
    private static final String API_TOKEN = "API_TOKEN";
    private static final String RENEW_TOKEN = "RENEW_TOKEN";
    private static final String ACCOUNT_TOKEN = "ACCOUNT_TOKEN";
    private static final String ACCOUNT_ID = "ACCOUNT_ID";
    private static final String COMPANY = "COMPANY";
    private static final String PARITION_NAME = "PARITION_NAME";
    private static final String SCREEN_NAME = "SCREEN_NAME";
    private static final String PROFILE_IMAGE = "PROFILE_IMAGE";

    private SharedPreferences sharedPreferences;

    public SharedPreferencesHelper(Context context) {
        sharedPreferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    private void putString(String key, String value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value).apply();
    }

    private String getString(String key) {
        return sharedPreferences.getString(key, "");
    }

    public void clearAll() {
        sharedPreferences.edit().clear().apply();
    }

    public void putAPIKey(String value) {
        putString(API_KEY, value);
    }

    public String getAPIKey() {
        return getString(API_KEY);
    }

    public void putAPIToken(String value) {
        putString(API_TOKEN, value);
    }

    public String getAPIToken() {
        return getString(API_TOKEN);
    }

    public void putRenewToken(String value) {
        putString(RENEW_TOKEN, value);
    }

    public String getRenewToken() {
        return getString(RENEW_TOKEN);
    }

    public void putAccountToken(String value) {
        putString(ACCOUNT_TOKEN, value);
    }

    public String getAccountToken() {
        return getString(ACCOUNT_TOKEN);
    }

    public void putAccountID(String value) {
        putString(ACCOUNT_ID, value);
    }

    public String getAccountID() {
        return getString(ACCOUNT_ID);
    }

    public void putCompanyName(String value) {
        putString(COMPANY, value);
    }

    public String getCompanyName() {
        return getString(COMPANY);
    }

    public void putPartitionName(String value) {
        putString(PARITION_NAME, value);
    }

    public String getParitionName() {
        return getString(PARITION_NAME);
    }

    public void putScreenName(String value) {
        putString(SCREEN_NAME, value);
    }

    public String getScreenName() {
        return getString(SCREEN_NAME);
    }

    public byte[] getProfileImage(final Context context) {
        File dir = context.getDir("profile", Context.MODE_PRIVATE);
        if( ! dir.exists()) {
            return null;
        }
        File file = new File(dir, "profile_image");
        FileInputStream inStream = null;
        try {
            inStream = new FileInputStream(file);
            return getBytes(inStream);
        }
        catch ( FileNotFoundException e ) {
            // Might be first time...
            return null;
        }
        catch ( IOException e ) {
            e.printStackTrace();
            BBGDemoApplication.showToast(context, "Failed to read profile image.");
            return null;
        }
        finally {
            if ( inStream != null ) {
                try {
                    inStream.close();
                }
                catch ( IOException e ) {
                    e.printStackTrace();
                    BBGDemoApplication.showToast(context, "Failed to close profile image.");
                }
            }
        }
    }

    private byte[] getBytes(InputStream is) throws IOException {
        int len;
        int size = 1024;
        byte[] buf;

        if (is instanceof ByteArrayInputStream) {
            size = is.available();
            buf = new byte[size];
        }
        else {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            buf = new byte[size];
            while ((len = is.read(buf, 0, size)) != -1) {
                bos.write(buf, 0, len);
            }
            buf = bos.toByteArray();
        }
        return buf;
    }

    public void putProfileImage(final Activity currentActivity, Bitmap bitmap) {
        File dir = currentActivity.getApplicationContext().getDir("profile", Context.MODE_PRIVATE);
        if( ! dir.exists() ){
            if ( ! dir.mkdir() ) {
                BBGDemoApplication.showToast(currentActivity.getApplicationContext(), "Failed to make directory");
                return;
            }
        }
        File file = new File(dir, "profile_image");
        FileOutputStream outStream = null;
        try {
            outStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            outStream.flush();
            outStream.close();
        }
        catch (IOException e){
            e.printStackTrace();
            BBGDemoApplication.showToast(currentActivity.getApplicationContext(), "Failed to write profile image.");
        }
        finally{
            try {
                if ( outStream != null ) {
                    outStream.close();
                }
            }
            catch ( IOException e ) {
                e.printStackTrace();
                BBGDemoApplication.showToast(currentActivity.getApplicationContext(), "Failed to close profile image.");
            }
        }
    }
}

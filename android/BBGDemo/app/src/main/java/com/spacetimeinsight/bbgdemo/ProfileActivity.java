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

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.util.Log;
import android.widget.Toast;

import com.spacetimeinsight.nucleus.android.NucleusService;
import com.spacetimeinsight.nucleuslib.DeviceService;
import com.spacetimeinsight.nucleuslib.core.ClientDevice;
import com.spacetimeinsight.nucleuslib.responsehandlers.GeneralResponseHandler;
import com.spacetimeinsight.nucleuslib.types.OperationStatus;
import com.spacetimeinsight.bbgdemo.R;
import com.spacetimeinsight.bbgdemo.SharedPreferencesHelper;
import com.spacetimeinsight.bbgdemo.BBGDemoApplication;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ProfileActivity extends Activity {

    private static final int REQUEST_TAKE_PHOTO = 1;
    private static final String LOG_TAG = ProfileActivity.class.getName();

    private String currentPhotoPath;
    private EditText screenName;
    private ImageView profileView;
    private Bitmap bitmap;
    private boolean hasEdit = false;
    private boolean fromSession = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        final BBGDemoApplication app = (BBGDemoApplication) getApplication();
        app.setCurrentActivity(this);

        fromSession = getIntent().getBooleanExtra("FROM_SESSION", false);

        final NucleusService nucleusService = BBGDemoApplication.getNucleusService();
        final ClientDevice clientDevice = nucleusService.getClientDevice();

        screenName = (EditText) findViewById(R.id.screen_name);
        screenName.setText(clientDevice.getScreenName());

        screenName.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                hasEdit = true;
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        profileView = (ImageView) findViewById(R.id.profile_image);

        byte[] imageData = clientDevice.getProfileImage();
        if ( imageData != null ) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
            profileView.setImageBitmap(bitmap);
        }
        else {
            Context context = getApplicationContext();
            Drawable drawable = getResources().getDrawable(getResources().getIdentifier("profile_missing", "mipmap",
                                                                                        getPackageName()),
                                                           context.getTheme());
            profileView.setImageDrawable(drawable);
        }
    }

    public void saveProfile(View view) {
        Log.i(LOG_TAG, "Saving profile..." + screenName.getText().toString());

        BitmapDrawable drawable = (BitmapDrawable) profileView.getDrawable();
        final Bitmap bitmap = drawable.getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] imageInByte = stream.toByteArray();

        SharedPreferencesHelper helper = new SharedPreferencesHelper(getApplicationContext());
        helper.putScreenName(screenName.getText().toString());
        helper.putProfileImage(ProfileActivity.this, bitmap);

        NucleusService nucleusService = BBGDemoApplication.getNucleusService();
        ClientDevice clientDevice =  nucleusService.getClientDevice();
        clientDevice.setScreenName(screenName.getText().toString());
        clientDevice.setProfileImage(imageInByte, "jpg");

        DeviceService deviceService = nucleusService.getDeviceService();
        deviceService.setProfile(new GeneralResponseHandler() {
            @Override
            public void onSuccess() {
                finish();
            }

            @Override
            public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage) {
                BBGDemoApplication app = (BBGDemoApplication) getApplication();
                app.showAlert("Error", "Failed to save profile");
                Log.e(LOG_TAG, "Failed to save profile - " + operationStatus + "(" + statusCode + ") " + errorMessage);
                finish();
            }
        });
    }

    public void pickImage(View view) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, ImagePickActivity.REQUEST_CODE);
    }

    public void takePhoto(View view) {
        if ( ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(ProfileActivity.this, Manifest.permission.CAMERA) ) {
                BBGDemoApplication app = (BBGDemoApplication) getApplication();
                app.showAlert("Need Permission", "You need to grant permission to Simple Demo to access the camera.");
            }
            else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(ProfileActivity.this,
                                                  new String[]{Manifest.permission.CAMERA},
                                                  REQUEST_TAKE_PHOTO);
            }
        }
        else {
            dispatchTakePictureIntent();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_TAKE_PHOTO: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    dispatchTakePictureIntent();
                }
                else {
                    BBGDemoApplication app = (BBGDemoApplication) getApplication();
                    app.showAlert("Failed", "This application does not have permission.");
                }
            }
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            }
            catch (IOException ex) {
                Log.e(LOG_TAG, "Failed to create image file. " + ex.getLocalizedMessage());
                BBGDemoApplication.showToast(ProfileActivity.this, "Failed to create image");
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                                                          "com.example.android.fileprovider",
                                                          photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
        else {
            Log.i(LOG_TAG, "Failed to launch camera app");
            BBGDemoApplication.showToast(ProfileActivity.this, "Failed to launch camera app");
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName,  /* prefix */
                                         ".jpg",         /* suffix */
                                         storageDir      /* directory */
                                        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Get the dimensions of the View
        int targetW = profileView.getWidth();
        int targetH = profileView.getHeight();

        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {

            // Get the dimensions of the bitmap
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(currentPhotoPath, bmOptions);
            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;

            // Determine how much to scale down the image
            int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

            // Decode the image file into a Bitmap sized to fill the View
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;

            if ( bitmap != null ) {
                bitmap.recycle();
            }
            bitmap = BitmapFactory.decodeFile(currentPhotoPath, bmOptions);
            profileView.setImageBitmap(bitmap);
        }
        else if ( requestCode == ImagePickActivity.REQUEST_CODE && resultCode == Activity.RESULT_OK ) {
            InputStream stream = null;
            try {
                stream = getContentResolver().openInputStream(data.getData());

                BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                bmOptions.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(stream, null, bmOptions);
                int photoW = bmOptions.outWidth;
                int photoH = bmOptions.outHeight;

                int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

                bmOptions.inJustDecodeBounds = false;
                bmOptions.inSampleSize = scaleFactor;

                if ( bitmap != null ) {
                    bitmap.recycle();
                }
                stream = getContentResolver().openInputStream(data.getData());
                bitmap = BitmapFactory.decodeStream(stream, null, bmOptions);
                photoW = bmOptions.outWidth;
                photoH = bmOptions.outHeight;

                Bitmap b = Bitmap.createBitmap(bitmap,
                                               (photoW/2) - (targetW/2),
                                               (photoH/2) - (targetH/2),
                                               targetW, targetH);

                profileView.setImageBitmap(b);
            }
            catch ( FileNotFoundException e ) {
                e.printStackTrace();
                Toast.makeText(ProfileActivity.this, "Failed to pick picture", Toast.LENGTH_LONG).show();
            }
            finally {
                if ( stream != null ) {
                    try {
                        stream.close();
                    }
                    catch ( IOException e ) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        if ( ! hasEdit ) {
            super.onBackPressed();
        }
        else {
            AlertDialog alertDialog = new AlertDialog.Builder(ProfileActivity.this).setPositiveButton("OK", null)
                                                                                   .setTitle("Notice")
                                                                                   .setMessage("You have unsaved changes.")
                                                                                   .create();
            alertDialog.show();
        }
    }
}

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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class ImagePickActivity extends Activity {
    public static final int REQUEST_CODE = 100;
    private ImageView imageView;
    private Bitmap bitmap;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_pick);
        imageView = (ImageView) findViewById(R.id.pick_result);

        BBGDemoApplication app = (BBGDemoApplication) getApplication();
        app.setCurrentActivity(this);
        Bitmap bitmap = app.getProfileImage();
        imageView.setImageBitmap(bitmap);
    }

    public void useImage(View View) {
        BBGDemoApplication app = (BBGDemoApplication) getApplication();
        app.setProfileImage(bitmap);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        InputStream stream = null;
        if ( requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK ) {
            try {
                if ( bitmap != null ) {
                    bitmap.recycle();
                }
                stream = getContentResolver().openInputStream(data.getData());
                bitmap = BitmapFactory.decodeStream(stream);

                imageView.setImageBitmap(bitmap);

                Button button = (Button) findViewById(R.id.use_button);
                button.setEnabled(true);
            }
            catch ( FileNotFoundException e ) {
                e.printStackTrace();
                Toast.makeText(ImagePickActivity.this, "Failed to take picture", Toast.LENGTH_LONG).show();
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
}
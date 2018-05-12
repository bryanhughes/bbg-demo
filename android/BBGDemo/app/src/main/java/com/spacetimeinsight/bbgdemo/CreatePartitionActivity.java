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

import android.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.spacetimeinsight.nucleus.android.NucleusService;
import com.spacetimeinsight.nucleuslib.PartitionInfo;
import com.spacetimeinsight.nucleuslib.PartitionService;
import com.spacetimeinsight.nucleuslib.responsehandlers.PartitionCreateResponseHandler;
import com.spacetimeinsight.nucleuslib.responsehandlers.PartitionLookupByNameResponseHandler;
import com.spacetimeinsight.nucleuslib.types.OperationStatus;

import java.io.IOException;

public class CreatePartitionActivity extends AppCompatActivity {
    private static final String LOG_TAG = CreatePartitionActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_partition);

        BBGDemoApplication app = (BBGDemoApplication) getApplication();
        app.setCurrentActivity(this);

        NucleusService nucleusService = BBGDemoApplication.getNucleusService();
        if ( nucleusService.getApiKey() != null ) {
            TextView partitionKeyField = findViewById(R.id.partitionKeyField);
            partitionKeyField.setText(nucleusService.getApiKey());
        }
    }

    public void createPartition(View view) {
        final BBGDemoApplication app = (BBGDemoApplication) getApplication();
        final NucleusService nucleusService = BBGDemoApplication.getNucleusService();

        app.showProgress(CreatePartitionActivity.this, "Creating partition...");

        String apiAccountID = Config.API_ACCOUNTID;
        String apiAccountToken = Config.API_ACCOUNTTOKEN;

        EditText partNameField = findViewById(R.id.partName1Field);
        String partName = partNameField.getText().toString();

        EditText appNameField = findViewById(R.id.appNameField);
        String appName = appNameField.getText().toString();

        EditText supportEmailField = findViewById(R.id.supportEmailField);
        String supportEmail = supportEmailField.getText().toString();

        EditText companyNameField = findViewById(R.id.companyNameField);
        String companyName = companyNameField.getText().toString();

        if ( partName.isEmpty() || appName.isEmpty() || companyName.isEmpty() ) {
            app.showAlert("Notice", "Fields are required and can not be empty.");
        }
        else {
            PartitionService partitionService = nucleusService.getPartitionService();
            partitionService.createPartition(apiAccountID, apiAccountToken, partName, appName, companyName, supportEmail,
                                             new PartitionCreateResponseHandler() {
                                                 @Override
                                                 public void onSuccess(String apiKey, String apiToken) {
                                                     handleSuccess(apiKey, apiToken, nucleusService);
                                                 }

                                                 @Override
                                                 public void onFailure(OperationStatus operationStatus,
                                                                       int statusCode,
                                                                       final String errorMessage,
                                                                       boolean retryable) {
                                                     handleFailure(operationStatus, statusCode, errorMessage);
                                                 }
                                             });
        }
    }

    private void handleFailure(OperationStatus operationStatus, int statusCode, final String errorMessage) {
        Log.e(LOG_TAG, "Failed to create partition. " + operationStatus +
                       ", (" + statusCode + ") " + errorMessage);

        BBGDemoApplication app = (BBGDemoApplication) getApplication();
        app.dismissProgress();
        AlertDialog alertDialog;
        String message = "Sorry! Failed to create or join partition - " + errorMessage;
        alertDialog = new AlertDialog.Builder(CreatePartitionActivity.this).setMessage(message)
                                                                  .setTitle("ERROR")
                                                                  .setPositiveButton("OK", null)
                                                                  .create();
        alertDialog.show();
    }

    private void handleSuccess(final String apiKey, String apiToken, NucleusService nucleusService) {
        BBGDemoApplication app = (BBGDemoApplication) getApplication();
        app.dismissProgress();
        Toast.makeText(CreatePartitionActivity.this, "Successfully created partition", Toast.LENGTH_LONG).show();

        TextView partitionKeyField = findViewById(R.id.partitionKeyField);
        partitionKeyField.setText(apiKey);

        SharedPreferencesHelper helper = new SharedPreferencesHelper(CreatePartitionActivity.this);
        helper.putAPIKey(apiKey);
        helper.putAPIToken(apiToken);

        try {
            nucleusService.setActivePartition(apiKey, apiToken);
            finish();
            app.startSession(null);
        }
        catch ( IOException e ) {
            e.printStackTrace();
            Log.e(LOG_TAG, e.getLocalizedMessage(), e);
            nucleusService.handleOnError(0, "Internal exception - " +
                                           e.getLocalizedMessage());
        }
    }

    public void findPartition(View view) {
        final NucleusService nucleusService = BBGDemoApplication.getNucleusService();

        String apiAccountID = Config.API_ACCOUNTID;
        String apiAccountToken = Config.API_ACCOUNTTOKEN;

        EditText partNameField = findViewById(R.id.partName2Field);
        String partName = partNameField.getText().toString();

        PartitionService partitionService = nucleusService.getPartitionService();
        partitionService.lookupPartitionsByName(apiAccountID,
                                                apiAccountToken,
                                                partName,
                                                new PartitionLookupByNameResponseHandler() {
                                                    @Override
                                                    public void onSuccess(PartitionInfo partitionInfo) {
                                                        if ( partitionInfo != null ) {
                                                            handleSuccess(partitionInfo.getApiKey(),
                                                                          partitionInfo.getApiToken(),
                                                                          nucleusService);
                                                        }
                                                        else {
                                                            handleFailure(OperationStatus.PARTITION_NOTFOUND, 200,
                                                                          "Partition not found.");
                                                        }
                                                    }

                                                    @Override
                                                    public void onFailure(OperationStatus operationStatus,
                                                                          int statusCode,
                                                                          String errorMessage,
                                                                          boolean retryable) {
                                                        handleFailure(operationStatus, statusCode, errorMessage);
                                                    }
                                                });
    }
}

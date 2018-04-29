package com.spacetimeinsight.bbgdemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import com.spacetimeinsight.nucleus.android.NucleusService;
import com.spacetimeinsight.nucleuslib.Channel;
import com.spacetimeinsight.nucleuslib.ChannelService;
import com.spacetimeinsight.nucleuslib.responsehandlers.ChannelFindByNameResponseHandler;
import com.spacetimeinsight.nucleuslib.types.OperationStatus;

public class ChannelActivity extends AppCompatActivity {
    private static final String LOG_TAG = ChannelActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel);

        BBGDemoApplication app = (BBGDemoApplication) getApplication();
        app.setCurrentActivity(this);

        final NucleusService nucleusService = BBGDemoApplication.getNucleusService();

        Button button = findViewById(R.id.setChannelButton);
        button.setOnClickListener(v -> {
            EditText channelName = findViewById(R.id.channelNameText);
            final String cname = channelName.getText().toString();

            final ChannelService channelService = nucleusService.getChannelService();
            channelService.findChannelByName(cname, null, new ChannelFindByNameResponseHandler() {
                @Override
                public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage,
                                      boolean retryable) {
                    Log.i(LOG_TAG, "Failed to find by name. " + operationStatus + ", statusCode = " +
                            statusCode + ", errorMessage = " + errorMessage);
                    System.exit(-1);
                }

                @Override
                public void onSuccess(final String channelRef) {
                    BBGDemoApplication app1 = (BBGDemoApplication) getApplication();
                    if ( channelRef == null ) {
                        app1.showAlert("Failed", "Channel name `" + cname + "`does not exist.");
                    }
                    else {
                        Log.i(LOG_TAG, "Found channel by name. channelRef = " + channelRef);
                        app1.joinChannel(channelRef);
                    }
                }
            });
        });

        Channel channel = nucleusService.getCurrentChannel();
        if ( channel != null ) {
            EditText channelName = findViewById(R.id.channelNameText);
            channelName.setText(channel.getName());
        }
    }
}

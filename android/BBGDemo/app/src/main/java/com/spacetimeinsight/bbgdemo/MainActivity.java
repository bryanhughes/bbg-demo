package com.spacetimeinsight.bbgdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.spacetimeinsight.nucleus.android.NucleusService;
import com.spacetimeinsight.nucleuslib.Channel;
import com.spacetimeinsight.nucleuslib.ChannelService;
import com.spacetimeinsight.nucleuslib.NucleusException;
import com.spacetimeinsight.nucleuslib.datamapped.MimeMessage;
import com.spacetimeinsight.nucleuslib.datamapped.MimePart;
import com.spacetimeinsight.nucleuslib.responsehandlers.ChannelPublishMessageResponseHandler;
import com.spacetimeinsight.nucleuslib.responsehandlers.GeneralResponseHandler;
import com.spacetimeinsight.nucleuslib.types.OperationStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static android.support.v4.content.PermissionChecker.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = MainActivity.class.getName();
    private static final int PERMISSION_REQUEST_LOCATION = 0;

    private static String[] PERMISSION_LOCATION = {android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION};

    private SeekBar redBar;
    private SeekBar greenBar;
    private SeekBar blueBar;

    private TextView hView;
    private TextView tView;
    private TextView tsView;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            Intent intent;

            switch (item.getItemId()) {
                case R.id.navigation_home:
                    intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);
                    return true;
                case R.id.navigation_chat:
                    intent = new Intent(getApplicationContext(), ChatActivity.class);
                    startActivity(intent);
                    return true;
                case R.id.navigation_channel:
                    intent = new Intent(getApplicationContext(), ChannelActivity.class);
                    startActivity(intent);
                    return true;
                case R.id.navigation_partition:
                    intent = new Intent(getApplicationContext(), CreatePartitionActivity.class);
                    startActivity(intent);
                    return true;
                case R.id.navigation_profile:
                    // Show profile activity
                    intent = new Intent(getApplicationContext(), ProfileActivity.class);
                    intent.putExtra("FROM_SESSION", false);
                    startActivity(intent);
                    return true;
            }
            return false;
        }

    };

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ( intent.getAction().equals(BBGDemoApplication.BROADCAST_PROPERTY_ACTION) ) {
                int rVal = intent.getIntExtra("red", 0);
                int gVal = intent.getIntExtra("green", 0);
                int bVal = intent.getIntExtra("blue", 0);

                redBar.setProgress(rVal);
                greenBar.setProgress(gVal);
                blueBar.setProgress(bVal);
            }
            else if ( intent.getAction().equals(BBGDemoApplication.BROADCAST_SENSOR_ACTION) ) {
                double h = intent.getDoubleExtra("h", 0);
                double t = intent.getDoubleExtra("t", 0);
                long ts = intent.getLongExtra("ts", 0);

                hView.setText(String.format(Locale.getDefault(), "%.2f %%", h));
                tView.setText(String.format(Locale.getDefault(), "%.1f C", t));
                tsView.setText(String.valueOf(ts));
            }
        }
    };

    private Handler ensureStarted = new Handler();
    private Runnable ensureRunnable = new Runnable() {
        @Override
        public void run() {
            final BBGDemoApplication app = (BBGDemoApplication) getApplication();

            // The NucleusService should be bound to at this point...
            NucleusService nucleusService = BBGDemoApplication.getNucleusService();
            if ( nucleusService != null ) {
                try {
                    SharedPreferencesHelper helper = new SharedPreferencesHelper(getApplicationContext());
                    final String apiKey = helper.getAPIKey();
                    final String apiToken = helper.getAPIToken();

                    // If we have an empty apiKey, then we need to create or find and join a partition. Otherwise,
                    // just start our session
                    if ( apiKey.isEmpty() ) {
                        Intent i = new Intent(getApplicationContext(), CreatePartitionActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(i);
                    }
                    else {
                        nucleusService.setActivePartition(apiKey, apiToken);
                        app.startSession();
                    }
                }
                catch ( NucleusException e ) {
                    e.printStackTrace();
                    Log.e(LOG_TAG, e.getLocalizedMessage(), e);
                    nucleusService.handleOnError(0, "Internal exception - " + e.getLocalizedMessage());
                }
            }
            else {
                ensureStarted.postDelayed(ensureRunnable, 100);
            }
        }
    };

    private SeekBar.OnSeekBarChangeListener onSeekChangeBarListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            final NucleusService nucleusService = BBGDemoApplication.getNucleusService();
            Channel channel = nucleusService.getCurrentChannel();
            final String ledStr = getLEDString();
            channel.setProperty("led", ledStr, new GeneralResponseHandler() {
                @Override
                public void onSuccess() {
                    Log.i(LOG_TAG, "Successfully set channel property - " + ledStr);
                }

                @Override
                public void onFailure(OperationStatus operationStatus, int statusCode, String errorMsg) {
                    Log.e(LOG_TAG, "Failed to set channel property - " + ledStr + ". " + operationStatus + " (" +
                            statusCode + ") - " + errorMsg);
                }
            });
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BBGDemoApplication app = (BBGDemoApplication) getApplication();
        app.setCurrentActivity(this);

        redBar = (SeekBar) findViewById(R.id.redSeekBar);
        greenBar = (SeekBar) findViewById(R.id.greenSeekBar);
        blueBar = (SeekBar) findViewById(R.id.blueSeekBar);

        redBar.setOnSeekBarChangeListener(onSeekChangeBarListener);
        greenBar.setOnSeekBarChangeListener(onSeekChangeBarListener);
        blueBar.setOnSeekBarChangeListener(onSeekChangeBarListener);

        Button sendButton = (Button) findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NucleusService nucleusService = BBGDemoApplication.getNucleusService();
                ChannelService channelService = nucleusService.getChannelService();

                EditText messageText = (EditText) findViewById(R.id.messageText);
                final String message = messageText.getText().toString();

                List<MimePart> parts = new ArrayList<>();
                parts.add(new MimePart("text/plain", "", message.getBytes()));
                MimeMessage mimeMessage = new MimeMessage(parts);
                channelService.publish(nucleusService.getCurrentChannelRef(),
                                       mimeMessage,
                                       new ChannelPublishMessageResponseHandler() {
                                           @Override
                                           public void onSuccess(long offset, long eventID) {
                                               Log.i(LOG_TAG, "Successfully published message to channel - " + message);
                                           }

                                           @Override
                                           public void onFailure(OperationStatus operationStatus, int statusCode, String errMsg) {
                                               Log.e(LOG_TAG, "Failed to publish message to channel - " + message + ". " +
                                                       operationStatus + " (" + statusCode + ") - " + errMsg);
                                           }
                                       });
            }
        });

        Button shutdownButton = (Button) findViewById(R.id.shutdownButton);
        shutdownButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NucleusService nucleusService = BBGDemoApplication.getNucleusService();
                Channel channel = nucleusService.getCurrentChannel();

                channel.setProperty("shutdown", "now", new GeneralResponseHandler() {
                    @Override
                    public void onSuccess() {
                        BBGDemoApplication.showToast(getApplicationContext(), "Device will now shutdown");
                    }

                    @Override
                    public void onFailure(OperationStatus operationStatus, int statusCode, String errorMsg) {
                        Log.e(LOG_TAG, "Failed to set shutdown property. " + operationStatus + " : (" + statusCode + ") - " +
                                errorMsg);
                    }
                });
            }
        });

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }

    @Override
    protected void onStart() {
        super.onStart();
        BBGDemoApplication app = (BBGDemoApplication) getApplication();

        redBar.setProgress(app.getRedLED());
        greenBar.setProgress(app.getGreenLED());
        blueBar.setProgress(app.getBlueLED());

        hView = (TextView) findViewById(R.id.humidityTextView);
        hView.setText(String.valueOf(0));

        tView = (TextView) findViewById(R.id.tempTextView);
        tView.setText(String.valueOf(0));

        tsView = (TextView) findViewById(R.id.timestampTextView);
        tsView.setText(String.valueOf(0));

        ensureStarted.postDelayed(ensureRunnable, 1000);
    }

    @Override
    protected void onResume() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BBGDemoApplication.BROADCAST_PROPERTY_ACTION);
        filter.addAction(BBGDemoApplication.BROADCAST_SENSOR_ACTION);
        registerReceiver(receiver, filter);
        super.onResume();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(receiver);
        super.onPause();
    }

    private void checkPermissions(NucleusService nucleusService) {
        if ( (ContextCompat.checkSelfPermission(getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(getApplicationContext(),
                        android.Manifest.permission.ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED) ) {
            ActivityCompat.requestPermissions(this, PERMISSION_LOCATION, PERMISSION_REQUEST_LOCATION);
        }
        else {
            nucleusService.enableLocationServices(true);
        }
    }

    public String getLEDString() {
        return String.format(Locale.getDefault(), "%d,%d,%d",
                             redBar.getProgress(), greenBar.getProgress(), blueBar.getProgress());
    }
}

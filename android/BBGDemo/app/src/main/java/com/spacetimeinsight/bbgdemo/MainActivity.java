package com.spacetimeinsight.bbgdemo;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.spacetimeinsight.bbgdemo.chat.ChatActivity;
import com.spacetimeinsight.bbgdemo.map.MapsActivity;
import com.spacetimeinsight.nucleus.android.NucleusService;
import com.spacetimeinsight.nucleuslib.Channel;
import com.spacetimeinsight.nucleuslib.NucleusException;
import com.spacetimeinsight.nucleuslib.responsehandlers.GeneralResponseHandler;
import com.spacetimeinsight.nucleuslib.types.OperationStatus;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

import static android.support.v4.content.PermissionChecker.PERMISSION_GRANTED;

public class MainActivity extends Activity {
    private static final String LOG_TAG = MainActivity.class.getName();

    private static final String REQUIRED_PERMISSIONS[] = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private SeekBar redBar;
    private SeekBar greenBar;
    private SeekBar blueBar;

    private TextView hView;
    private TextView tView;
    private TextView tsView;
    private TextView aView;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener = item -> {
                Intent intent;

                switch (item.getItemId()) {
                    case R.id.navigation_home:
                        intent = new Intent(getApplicationContext(), MainActivity.class);
                        startActivity(intent);
                        return true;
                    case R.id.navigation_map:
                        intent = new Intent(getApplicationContext(), MapsActivity.class);
                        startActivity(intent);
                        return true;
                    case R.id.navigation_chat:
                        intent = new Intent(getApplicationContext(), ChatActivity.class);
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
            };

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), BBGDemoApplication.BROADCAST_PROPERTY_ACTION)) {
                int rVal = intent.getIntExtra("red", 0);
                int gVal = intent.getIntExtra("green", 0);
                int bVal = intent.getIntExtra("blue", 0);

                redBar.setProgress(rVal);
                greenBar.setProgress(gVal);
                blueBar.setProgress(bVal);
            }
            else if (Objects.equals(intent.getAction(), BBGDemoApplication.BROADCAST_SENSOR_ACTION)) {
                double h = intent.getDoubleExtra("h", 0);
                double t = intent.getDoubleExtra("t", 0);
                long ts = intent.getLongExtra("ts", 0);
                double x = intent.getDoubleExtra("x", 0);
                double y = intent.getDoubleExtra("y", 0);
                double z = intent.getDoubleExtra("z", 0);

                hView.setText(String.format(Locale.getDefault(), "%.2f %%", h));
                tView.setText(String.format(Locale.getDefault(), "%.1f C", t));
                tsView.setText(String.valueOf(ts));
                aView.setText(String.format(Locale.getDefault(), "%.3f, %.3f, %.3f", x, y, z));
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
                SharedPreferencesHelper helper = new SharedPreferencesHelper(getApplicationContext());
                final String apiKey = helper.getAPIKey();
                final String apiToken = helper.getAPIToken();

                try {
                    nucleusService.setActivePartition(apiKey, apiToken);

                    // If we have an empty apiKey, then we need to create or find and join a partition. Otherwise,
                    // just start our session
                    Intent i;
                    if ( apiKey.isEmpty() ) {
                        i = new Intent(getApplicationContext(), CreatePartitionActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(i);
                    }
                    else {
                        app.startSession();
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                    Log.e(LOG_TAG, "Failed to start session. " + e.getLocalizedMessage(), e);
                    app.showAlert("Error", "Failed to start session.");
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
            if ( channel == null ) {
                BBGDemoApplication app = (BBGDemoApplication) getApplication();
                app.showAlert("Error", "You must first either create a new channel, or join an existing one.");
            }
            else {
                final String ledStr = getLEDString();
                channel.setProperty("led", ledStr, new GeneralResponseHandler() {
                    @Override
                    public void onSuccess() {
                        Log.i(LOG_TAG, "Successfully set channel property - " + ledStr);
                    }

                    @Override
                    public void onFailure(OperationStatus operationStatus, int statusCode, String errorMsg,
                                          boolean retryable) {
                        Log.e(LOG_TAG, "Failed to set channel property - " + ledStr + ". " + operationStatus + " (" +
                                statusCode + ") - " + errorMsg);
                    }
                });
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BBGDemoApplication app = (BBGDemoApplication) getApplication();
        app.setCurrentActivity(this);

        redBar = findViewById(R.id.redSeekBar);
        greenBar = findViewById(R.id.greenSeekBar);
        blueBar = findViewById(R.id.blueSeekBar);

        redBar.setOnSeekBarChangeListener(onSeekChangeBarListener);
        greenBar.setOnSeekBarChangeListener(onSeekChangeBarListener);
        blueBar.setOnSeekBarChangeListener(onSeekChangeBarListener);

        Button sendButton = findViewById(R.id.sendButton);
        sendButton.setOnClickListener(v -> {
            NucleusService nucleusService = BBGDemoApplication.getNucleusService();
            EditText messageText = findViewById(R.id.messageText);
            final String message = messageText.getText().toString();

            Channel channel = nucleusService.getCurrentChannel();
            if ( channel == null ) {
                BBGDemoApplication app1 = (BBGDemoApplication) getApplication();
                app1.showAlert("Error", "You must first either create a new channel, or join an existing one.");
            }
            else {
                channel.setProperty("display", message, new GeneralResponseHandler() {
                    @Override
                    public void onSuccess() {
                        Log.i(LOG_TAG, "Successfully set channel property - " + message);
                    }

                    @Override
                    public void onFailure(OperationStatus operationStatus, int statusCode, String errorMsg,
                                          boolean retryable) {
                        Log.e(LOG_TAG, "Failed to set channel property - " + message + ". " + operationStatus + " (" +
                                statusCode + ") - " + errorMsg);
                    }
                });
            }
        });

        Button shutdownButton = findViewById(R.id.shutdownButton);
        shutdownButton.setOnClickListener(v -> {
            NucleusService nucleusService = BBGDemoApplication.getNucleusService();
            Channel channel = nucleusService.getCurrentChannel();
            if ( channel == null ) {
                BBGDemoApplication app12 = (BBGDemoApplication) getApplication();
                app12.showAlert("Error", "You must first either create a new channel, or join an existing one.");
            }
            else {
                channel.setProperty("shutdown", "now", new GeneralResponseHandler() {
                    @Override
                    public void onSuccess() {
                        BBGDemoApplication.showToast(getApplicationContext(), "Device will now shutdown");
                    }

                    @Override
                    public void onFailure(OperationStatus operationStatus, int statusCode, String errorMsg,
                                          boolean retryable) {
                        Log.e(LOG_TAG, "Failed to set shutdown property. " + operationStatus + " : (" + statusCode + ") - " +
                                errorMsg);
                    }
                });
            }
        });

        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        checkPermission();
    }

    private void checkPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if(grantResults.length > 0) {
            if (grantResults[0] != PERMISSION_GRANTED){
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);

                builder.setTitle("Error");
                builder.setMessage("BBGDemo needs access to your phones GPS for location tracking. Without these " +
                                           "permissions, this demo may not function propertly.");
                builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    dialog.dismiss();
                    checkPermission();
                });

                builder.create().show();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        BBGDemoApplication app = (BBGDemoApplication) getApplication();

        redBar.setProgress(app.getRedLED());
        greenBar.setProgress(app.getGreenLED());
        blueBar.setProgress(app.getBlueLED());

        hView = findViewById(R.id.humidityTextView);
        hView.setText(String.valueOf(0));

        tView = findViewById(R.id.tempTextView);
        tView.setText(String.valueOf(0));

        tsView = findViewById(R.id.timestampTextView);
        tsView.setText(String.valueOf(0));

        aView = findViewById(R.id.accelTextView);
        aView.setText(String.valueOf(0));

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

    public String getLEDString() {
        return String.format(Locale.getDefault(), "%d,%d,%d",
                             redBar.getProgress(), greenBar.getProgress(), blueBar.getProgress());
    }
}

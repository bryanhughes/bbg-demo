package com.spacetimeinsight.bbgdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.SeekBar;

import com.spacetimeinsight.nucleus.android.NucleusService;
import com.spacetimeinsight.nucleuslib.NucleusException;

import static android.support.v4.content.PermissionChecker.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = MainActivity.class.getName();
    private static final int PERMISSION_REQUEST_LOCATION = 0;

    private static String[] PERMISSION_LOCATION = {android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION};

    private SeekBar redBar;
    private SeekBar greenBar;
    private SeekBar blueBar;

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
                case R.id.navigation_channel:
                    // Show channel selector activity
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
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void onReceive(Context context, Intent intent) {
            if ( intent.getAction().equals(BBGDemoApplication.BROADCAST_PROPERTY_ACTION) ) {
                int rVal = intent.getIntExtra("red", 0);
                int gVal = intent.getIntExtra("green", 0);
                int bVal = intent.getIntExtra("blue", 0);

                redBar.setProgress(rVal, true);
                greenBar.setProgress(gVal, true);
                blueBar.setProgress(bVal, true);
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        redBar = (SeekBar) findViewById(R.id.redSeekBar);
        greenBar = (SeekBar) findViewById(R.id.greenSeekBar);
        blueBar = (SeekBar) findViewById(R.id.blueSeekBar);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        BBGDemoApplication app = (BBGDemoApplication) getApplication();
        app.setCurrentActivity(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        BBGDemoApplication app = (BBGDemoApplication) getApplication();

        redBar.setProgress(app.getRedLED());
        greenBar.setProgress(app.getGreenLED());
        blueBar.setProgress(app.getBlueLED());

        ensureStarted.postDelayed(ensureRunnable, 1000);
    }

    @Override
    protected void onResume() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BBGDemoApplication.BROADCAST_PROPERTY_ACTION);
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
}

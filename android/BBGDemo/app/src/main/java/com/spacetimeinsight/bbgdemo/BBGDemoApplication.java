package com.spacetimeinsight.bbgdemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.spacetimeinsight.bbgdemo.chat.ChatActivity;
import com.spacetimeinsight.bbgdemo.chat.ChatArrayAdapter;
import com.spacetimeinsight.nucleus.android.AndroidCachePolicy;
import com.spacetimeinsight.nucleus.android.NucleusService;
import com.spacetimeinsight.nucleuslib.Channel;
import com.spacetimeinsight.nucleuslib.ChannelService;
import com.spacetimeinsight.nucleuslib.GeoCircle;
import com.spacetimeinsight.nucleuslib.NucleusException;
import com.spacetimeinsight.nucleuslib.PartitionInfo;
import com.spacetimeinsight.nucleuslib.PartitionService;
import com.spacetimeinsight.nucleuslib.core.ClientDevice;
import com.spacetimeinsight.nucleuslib.datamapped.ChannelMessage;
import com.spacetimeinsight.nucleuslib.datamapped.Datapoint;
import com.spacetimeinsight.nucleuslib.datamapped.EnvData;
import com.spacetimeinsight.nucleuslib.datamapped.Member;
import com.spacetimeinsight.nucleuslib.datamapped.NucleusLocation;
import com.spacetimeinsight.nucleuslib.datamapped.Property;
import com.spacetimeinsight.nucleuslib.listeners.NucleusChannelListener;
import com.spacetimeinsight.nucleuslib.listeners.NucleusClientListener;
import com.spacetimeinsight.nucleuslib.listeners.NucleusMemberListener;
import com.spacetimeinsight.nucleuslib.listeners.NucleusMessageListener;
import com.spacetimeinsight.nucleuslib.listeners.NucleusTelemetryListener;
import com.spacetimeinsight.nucleuslib.responsehandlers.ChannelCreateResponseHandler;
import com.spacetimeinsight.nucleuslib.responsehandlers.ChannelFindByNameResponseHandler;
import com.spacetimeinsight.nucleuslib.responsehandlers.ChannelJoinResponseHandler;
import com.spacetimeinsight.nucleuslib.responsehandlers.DeviceSessionResponseHandler;
import com.spacetimeinsight.nucleuslib.responsehandlers.GeneralResponseHandler;
import com.spacetimeinsight.nucleuslib.responsehandlers.PartitionReadResponseHandler;
import com.spacetimeinsight.nucleuslib.types.ChangeType;
import com.spacetimeinsight.nucleuslib.types.HealthType;
import com.spacetimeinsight.nucleuslib.types.JoinOption;
import com.spacetimeinsight.nucleuslib.types.ListenerType;
import com.spacetimeinsight.nucleuslib.types.MessageType;
import com.spacetimeinsight.nucleuslib.types.OperationStatus;
import com.spacetimeinsight.nucleuslib.types.TelemetryType;

import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;


/**
 * (c) 2017 Space Time Insight
 */

public class BBGDemoApplication extends Application {
    private static final String LOG_TAG = BBGDemoApplication.class.getName();
    public static String BROADCAST_PROPERTY_ACTION = "com.spacetimeinsight.bbgdemo.PROPERTY_CHANGE";
    public static String BROADCAST_SENSOR_ACTION = "com.spacetimeinsight.bbgdemo.SENSOR_CHANGE";
    private static NucleusService nucleusService = null;
    private static ProgressDialog progress;
    private static Bitmap systemImage;

    private Activity currentActivity;
    private Bitmap profileImage;
    private int redLED = 0;
    private int greenLED = 0;
    private int blueLED = 0;

    /*
     * The SDK does not filter errors and in most cases will work to retry the command. This means it is possible for
     * the client to get the same error in succession. To keep it from flooding the UI with alerts, we only want to
     * display the error the first time we see it. When command eventually succeeds, the SDK will call onErrorReset().
     * This is where we clear our seen error set.
     */
    private Set<Integer> seenErrorSet = new HashSet<>();

    private GeoCircle myLocation = new GeoCircle(37.751685, -122.447621, 100);
    private String channelName = "bbgdemo01";
    private String shortDescription = "BBG Demo";
    private String longDescription = "2020 Third Street, San Francisco, CA";
    private ChatArrayAdapter chatArrayAdapter;
    private boolean isDisplaying502Alert = false;

    @Override
    public void onCreate() {
        super.onCreate();

        NucleusService.bind(getApplicationContext(), Config.API_ACCOUNTID, Config.API_ACCOUNTTOKEN, "BBGDemo",
                            new ServiceConnection(){
                    public void onServiceConnected(ComponentName className, IBinder service) {
                        Log.i(LOG_TAG, "Service connected...");
                        NucleusService.LocalBinder binder = (NucleusService.LocalBinder) service;
                        nucleusService = binder.getService();
                        nucleusService.setServerTarget(Config.PROTOCOL, Config.HOSTNAME, Config.PORT);

                        nucleusService.addListener(ListenerType.TELEMETRY, new NucleusTelemetryListener() {
                            @Override
                            public void onDatapointChange(Datapoint datapoint) {
                                if ( (datapoint != null) && (datapoint.getTelemetry() != null) &&
                                        TelemetryType.ENV_DATA.equals(datapoint.getTelemetryType())) {
                                    EnvData envData = (EnvData) datapoint.getTelemetry();

                                    Intent broadcast = new Intent();
                                    broadcast.setAction(BROADCAST_SENSOR_ACTION);
                                    broadcast.putExtra("h", envData.getHumidity());
                                    broadcast.putExtra("t", envData.getTemperature());
                                    broadcast.putExtra("ts", envData.getTimestamp());

                                    sendBroadcast(broadcast);
                                }
                            }
                        });

                        nucleusService.addListener(ListenerType.MEMBERS, new NucleusMemberListener() {
                            @Override
                            public void handleBoot(String channelRef, Member bootedBy) {
                                Log.e(LOG_TAG, "OMG - I have was booted by " + bootedBy + " from channel " + channelRef);
                                showAlert("Booted!", "Whoops! You have been booted from the channel by " + bootedBy.getScreenName());
                            }

                            @Override
                            public void onProfileChange(Member member) {
                                if ( currentActivity instanceof ChatActivity) {
                                    chatArrayAdapter.notifyDataSetChanged();
                                }
                            }

                            @Override
                            public void onStatusChange(Member member) {
                                // If our chat view is showing, we want to refresh the display icons
                                if ( currentActivity instanceof ChatActivity ) {
                                    chatArrayAdapter.notifyDataSetChanged();
                                }
                            }

                            @Override
                            public void onPresenceChange(Member member) {
                                System.out.println("member=" + member);
                                // If our chat view is showing, we want to refresh the display icons
                                if ( currentActivity instanceof ChatActivity ) {
                                    chatArrayAdapter.notifyDataSetChanged();
                                }
                            }

                            @Override
                            public void onLocationChange(Member member, NucleusLocation nucleusLocation) {

                            }
                        });

                        nucleusService.addListener(ListenerType.MESSAGES, new NucleusMessageListener() {
                            @Override
                            public void onMessageChange(ChannelMessage message) {
                                @SuppressLint("SimpleDateFormat") DateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
                                format.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));

                                Channel channel = nucleusService.getCurrentChannel();
                                Member member = channel.getMember(message.getDelegateID());
                                System.out.println("Member=" + member);

                                if ( message.getMessageType().equals(MessageType.MTMimeMessage) ) {
                                    chatArrayAdapter.add(message);
                                    chatArrayAdapter.notifyDataSetChanged();
                                }
                            }
                        });

                        nucleusService.addListener(ListenerType.CHANNEL, new NucleusChannelListener() {
                            @Override
                            public void onPropertyChange(String channelRef, Property property) {
                                setProperty(property);
                            }

                            @Override
                            public void onChannelHealthChange(String channelRef, HealthType healthType, HealthType healthType1) {

                            }

                            @Override
                            public void onChange(String channelRef, List<ChangeType> list) {

                            }
                        });

                        nucleusService.addListener(ListenerType.CLIENT, new NucleusClientListener() {
                            @Override
                            public void handleUnsupportedVersion(String minVersion) {
                                Log.i(LOG_TAG, "handleUnsupportedVersion = " + minVersion);
                                AlertDialog alertDialog;
                                DialogInterface.OnClickListener onClick = (dialog, which) -> {
                                    // Need to get the Google Play store link from the server
                                    PartitionService partitionService = nucleusService.getPartitionService();
                                    partitionService.readPartition(nucleusService.getApiAccountID(),
                                                                   nucleusService.getApiAccountToken(),
                                                                   nucleusService.getApiKey(),
                                                                   nucleusService.getApiToken(),
                                                                   new PartitionReadResponseHandler() {
                                                                       @Override
                                                                       public void onSuccess(PartitionInfo partitionInfo) {
                                                                           String urlString = partitionInfo.getGoogleUrl();
                                                                           Log.i(LOG_TAG, "urlString = " + urlString);
                                                                           if ( urlString != null ) {
                                                                               Intent httpIntent = new Intent(Intent.ACTION_VIEW);
                                                                               httpIntent.setData(Uri.parse(urlString));
                                                                               startActivity(httpIntent);
                                                                           }
                                                                       }

                                                                       @Override
                                                                       public void onFailure(OperationStatus operationStatus,
                                                                                             int statusCode,
                                                                                             String errorMessage,
                                                                                             boolean retryable) {
                                                                           showAlert("Error", errorMessage);
                                                                       }
                                                                   });
                                };
                                alertDialog = new AlertDialog.Builder(currentActivity).setMessage("You are running on an " +
                                                                                                          "unsupported version of this app. " +
                                                                                                          "You will be redirected to the Google Play store to update.")
                                                                                      .setTitle("Upgrade Now")
                                                                                      .setPositiveButton("OK", onClick)
                                                                                      .create();
                                alertDialog.show();

                            }

                            @Override
                            public void handleOldVersion(String currentVersion) {
                                Log.i(LOG_TAG, "handleOldVersion = " + currentVersion);
                                showAlert("Notice", "You are running on an older version of this app. " +
                                        "Please update to the current version.");

                            }

                            @Override
                            public void onConnected(boolean b) {

                            }

                            @Override
                            public void handleServerMessage(String serverMessage) {
                                Log.i(LOG_TAG, "handleServerMessage = " + serverMessage);
                                showAlert("Notice", serverMessage);
                            }

                            @Override
                            public void handleServerRequest(URL url) {
                                Log.i(LOG_TAG, "handleServerRequest = " + url.toExternalForm());
                                Intent httpIntent = new Intent(Intent.ACTION_VIEW);
                                httpIntent.setData(Uri.parse(url.toExternalForm()));
                                startActivity(httpIntent);
                            }

                            @Override
                            public void handleRequestError(String command,
                                                           OperationStatus operationStatus,
                                                           int statusCode,
                                                           String errorMessage) {
                                if ( operationStatus.equals(OperationStatus.INVALID_DEVICE_TOKEN) ) {
                                    renewSession();
                                }
                                else if ( operationStatus.equals(OperationStatus.INVALID_CREDENTIALS) ) {
                                    Intent i = new Intent(getApplicationContext(), CreatePartitionActivity.class);
                                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(i);
                                }
                                else if ( operationStatus.equals(OperationStatus.SERVER_EXCEPTION) && (statusCode == 502) ) {
                                    // We do not want to display this again if it is already displayed and the user just has not responded
                                    // to it...
                                    if ( ! isDisplaying502Alert ) {
                                        AlertDialog alertDialog;
                                        alertDialog = new AlertDialog.Builder(getBaseContext()).setMessage("Doh! Encountered a server exception (502). Will keep retrying...")
                                                                                               .setTitle("Error")
                                                                                               .setPositiveButton("OK",
                                                                                                                  (dialog, which) -> isDisplaying502Alert = false)
                                                                                               .create();
                                        alertDialog.show();
                                    }
                                }
                                else {
                                    Log.e(LOG_TAG, "(" + statusCode + ") " + errorMessage);
                                    nucleusService.handleOnError(0, errorMessage);
                                }
                            }

                            @Override
                            public void handleException(final Throwable throwable) {
                                if ( progress != null ) {
                                    progress.dismiss();
                                    progress = null;
                                }

                                AlertDialog alertDialog = new AlertDialog.Builder(currentActivity).setPositiveButton("OK", null)
                                                                                                  .setTitle("Error")
                                                                                                  .setMessage("Handled an internal exception - " +
                                                                                                              throwable.getLocalizedMessage())
                                                                                                  .create();
                                alertDialog.show();

                                throwable.printStackTrace();

                                Log.e(LOG_TAG, throwable.getLocalizedMessage(), throwable);
                            }

                            @Override
                            public void onError(final int errorCode, final String errorMessage) {
                                // We only want to show the first error instance. For messages that are retried, this method will be called
                                // on each failure and retry. This gives the developer a lot of opportunity to deal with multiple failures versus
                                // intermittent failures.
                                //
                                // Lets use a set to keep track of errors we have already seen and only show the message on the first instance
                                // of the message.

                                if ( ! seenErrorSet.contains(errorCode) ) {
                                    seenErrorSet.add(errorCode);
                                    if ( progress != null ) {
                                        progress.dismiss();
                                        progress = null;
                                    }

                                    Log.i(LOG_TAG, "Handling error - (" + errorCode + ") " + errorMessage);
                                    AlertDialog alertDialog = new AlertDialog.Builder(currentActivity).setMessage(errorMessage)
                                                                                                      .setTitle("Error")
                                                                                                      .setPositiveButton("OK", null)
                                                                                                      .create();
                                    alertDialog.show();
                                }
                            }

                            @Override
                            public void onErrorReset() {
                                seenErrorSet.clear();
                            }

                            @Override
                            public void onInactiveExpiration() {

                            }

                            @Override
                            public void onLowPowerNotification() {

                            }

                            @Override
                            public void onInternetActive(boolean b) {

                            }

                            @Override
                            public void onDeviceLocationChange(NucleusLocation nucleusLocation) {

                            }
                        });

                        chatArrayAdapter = new ChatArrayAdapter(getApplicationContext(), R.layout.content_chat_message);

                        // Get our stashed profile information
                        SharedPreferencesHelper helper = new SharedPreferencesHelper(getApplicationContext());

                        ClientDevice clientDevice = nucleusService.getClientDevice();
                        clientDevice.setScreenName(helper.getScreenName());

                        byte[] imageData = helper.getProfileImage(getApplicationContext());
                        clientDevice.setProfileImage(imageData, "jpg");
                    }

                    public void onServiceDisconnected(ComponentName arg0) {
                        Log.i(LOG_TAG, "Service disconnected...");
                        nucleusService = null;
                    }
                }, AndroidCachePolicy.getDefaultPolicy());
    }
    public static NucleusService getNucleusService() {
        return nucleusService;
    }

    public void setCurrentActivity(Activity currentActivity) {
        this.currentActivity = currentActivity;
    }

    public Activity getCurrentActivity() {
        return currentActivity;
    }

    public Bitmap getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(Bitmap profileImage) {
        this.profileImage = profileImage;
    }

    public int getRedLED() {
        return redLED;
    }

    public int getGreenLED() {
        return greenLED;
    }

    public int getBlueLED() {
        return blueLED;
    }

    public void showProgress(Context context, String message) {
        if ( progress != null ) {
            progress.dismiss();
        }

        progress = new ProgressDialog(context);
        progress.setMessage(message);
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setIndeterminate(true);
        progress.show();
    }

    public void dismissProgress() {
        if ( progress != null ) {
            progress.dismiss();
            progress = null;
        }
    }

    public static Bitmap getSystemImage(Context context) {
        if ( systemImage == null ) {
            systemImage = BitmapFactory.decodeResource(context.getResources(), R.mipmap.system_profile_50x50);
        }
        return systemImage;
    }

    static public void showToast(final Context context, final String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    public void showAlert(final String title, final String message) {
        AlertDialog alertDialog = new AlertDialog.Builder(currentActivity).setMessage(message)
                .setTitle(title)
                .setPositiveButton("OK", null)
                .create();
        alertDialog.show();
    }

    private DeviceSessionResponseHandler deviceSessionResponseHandler = new DeviceSessionResponseHandler() {
        @Override
        public void onSuccess(boolean needsProfile, List<String> activeMemberships) {
            Log.i("Device Service Success", "needsprofile " + needsProfile);
            if ( needsProfile && (currentActivity != null) ) {
                Context context = getApplicationContext();
                Intent intent = new Intent(context, ProfileActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }

            // These are asynchronous calls, so they may or may not complete before the process moves onto the next
            // lines of code.
            joinOrCreateChannel();
        }

        @Override
        public void onFailure(OperationStatus operationStatus, int statusCode, final String errorMessage,
                              boolean retryable) {
            Log.e(LOG_TAG, "( " + statusCode + ") " + errorMessage);

            // If we are in a session call, this is done on app startup and there is a chance that we do not
            // have a connection. So if not, give the user the option to stop trying to start the app
            if ( operationStatus.equals(OperationStatus.COMMUNICATION_ERROR) ) {

                // Only show on the first error since communication errors are retried over and over and over again
                if ( nucleusService.getErrorCount() == 1 ) {
                    DialogInterface.OnClickListener exitClick = (dialog, which) -> {
                        currentActivity.moveTaskToBack(true);
                        android.os.Process.killProcess(android.os.Process.myPid());
                        System.exit(1);
                    };

                    AlertDialog alertDialog;
                    alertDialog = new AlertDialog.Builder(currentActivity).setMessage(errorMessage +
                            ".\n\nDo you want to exit or keep trying?")
                            .setTitle("Failed")
                            .setPositiveButton("KEEP TRYING", null)
                            .setNegativeButton("EXIT", exitClick)
                            .create();
                    alertDialog.show();
                }
            }
            else if ( operationStatus.equals(OperationStatus.PARTITION_NOTFOUND) ) {
                DialogInterface.OnClickListener createClick = (dialog, which) -> {
                    Intent i = new Intent(getApplicationContext(), CreatePartitionActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                };

                DialogInterface.OnClickListener exitClick = (dialog, which) -> {
                    currentActivity.moveTaskToBack(true);
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(1);
                };

                AlertDialog alertDialog;
                alertDialog = new AlertDialog.Builder(currentActivity).setMessage(errorMessage +
                        ".\n\nWould you like to create a new one?")
                        .setTitle("Failed")
                        .setPositiveButton("CREATE PARTITION", createClick)
                        .setNegativeButton("EXIT", exitClick)
                        .create();
                alertDialog.show();
            }

            // The error is also handled in the onRequestError() callback. That is where we implement general messaging
            // and handling. For all other errors, lets handle them there
        }
    };

    public void startSession() {
        nucleusService.getDeviceService().startSession(deviceSessionResponseHandler);
    }

    private void renewSession() {
        nucleusService.getDeviceService().renewSession(deviceSessionResponseHandler);
    }

    private void joinOrCreateChannel() {
        Log.i(LOG_TAG, "Find channel by: " + channelName);
        final ChannelService channelService = nucleusService.getChannelService();
        channelService.findChannelByName(channelName, null, new ChannelFindByNameResponseHandler() {

            @Override
            public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage,
                                  boolean retryable) {
                Log.i(LOG_TAG, "Failed to find by name. " + operationStatus + ", statusCode = " +
                                           statusCode + ", errorMessage = " + errorMessage);
                System.exit(-1);
            }

            @Override
            public void onSuccess(final String channelRef) {
                Log.i(LOG_TAG, "Found channel by name. channelRef = " + channelRef);

                if ( channelRef == null ) {
                    createChannel();
                }
                else {
                    joinChannel(channelRef);
                }
            }
        });
    }

    private void createChannel() {
        final ChannelService channelService = nucleusService.getChannelService();
        channelService.createChannel(myLocation, -1, -1, channelName, null, "Site", false, "Site",
                                     null, shortDescription, longDescription, new ChannelCreateResponseHandler() {
                    @Override
                    public void onSuccess(String channelRef) {
                        // Creating a channel automatically joins you to it, but does not automatically switch
                        // you into it.
                        Log.i(LOG_TAG, "Successfully created channel. channelRef = " + channelRef);
                        joinChannel(channelRef);
                    }

                    @Override
                    public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage,
                                          boolean retryable) {
                        Log.e(LOG_TAG, "Failed to create channel. " + operationStatus + ", statusCode = " +
                                                   statusCode + ", errorMessage = " + errorMessage);
                        System.exit(-1);
                    }
                });
    }

    void joinChannel(final String channelRef) {
        final ChannelService channelService = nucleusService.getChannelService();

        Map<JoinOption, Object> joinOptions = JoinOption.getLastNChanges(1);
        joinOptions.put(JoinOption.INCLUDE_TELEMETRY, true);

        final long stime = System.currentTimeMillis();
        channelService.joinChannel(channelRef, joinOptions, new ChannelJoinResponseHandler() {
            @Override
            public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage,
                                  boolean retryable) {
                Log.e(LOG_TAG, "Failed to join channel. channelRef=" + channelRef +
                                           " : " + operationStatus + " : statusCode = " +
                                           statusCode + " : errorMessage = " + errorMessage);
            }

            @Override
            public void onSuccess(final String channelRef) {
                try {
                    channelService.switchChannel(channelRef, new GeneralResponseHandler() {
                        @Override
                        public void onSuccess() {
                            Log.i(LOG_TAG, "Took " + (System.currentTimeMillis() - stime) + "ms to join channel " + channelRef);

                            // Make sure we set our initial state!

                            Channel channel = nucleusService.getChannel(channelRef);
                            Property property = channel.getProperty("led");
                            if ( property != null ) {
                                setProperty(property);
                            }

                            chatArrayAdapter.setChannel(channel);

                            startPolling();
                        }

                        @Override
                        public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage,
                                              boolean retryable) {
                            Log.e(LOG_TAG, "Failed to create channel. " + operationStatus + ", statusCode = " +
                                    statusCode + ", errorMessage = " + errorMessage);
                            System.exit(-1);
                        }
                    });
                }
                catch (NucleusException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    void setProperty(Property property) {
        // We got a property change. For our BeagleBone demo, the property is the LED light
        if ( property.getName().equals("led") ) {
            String valueStr = property.getValue();
            String[] parts = valueStr.split(",");
            if ( parts.length == 3 ) {
                redLED = Integer.parseInt(parts[0]);
                greenLED = Integer.parseInt(parts[1]);
                blueLED = Integer.parseInt(parts[2]);

                Log.i(LOG_TAG, "Broadcasting property change " + redLED + ", " + greenLED + ", " + blueLED);

                Intent broadcast = new Intent();
                broadcast.setAction(BROADCAST_PROPERTY_ACTION);
                broadcast.putExtra("red", redLED);
                broadcast.putExtra("green", greenLED);
                broadcast.putExtra("blue", blueLED);

                sendBroadcast(broadcast);
            }
        }
    }

    void startPolling() {
        nucleusService.enablePolling(true);
    }

    public ChatArrayAdapter getChatAdapter() {
        return chatArrayAdapter;
    }
}

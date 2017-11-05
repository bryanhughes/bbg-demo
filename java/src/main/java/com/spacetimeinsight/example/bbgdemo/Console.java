package com.spacetimeinsight.example.bbgdemo;

import com.spacetimeinsight.nucleus.java.NucleusClient;
import com.spacetimeinsight.nucleuslib.*;
import com.spacetimeinsight.nucleuslib.core.ClientDevice;
import com.spacetimeinsight.nucleuslib.core.NucleusFactory;
import com.spacetimeinsight.nucleuslib.datamapped.*;
import com.spacetimeinsight.nucleuslib.responsehandlers.*;
import com.spacetimeinsight.nucleuslib.types.ChangeType;
import com.spacetimeinsight.nucleuslib.types.DeviceType;
import com.spacetimeinsight.nucleuslib.types.HealthType;
import com.spacetimeinsight.nucleuslib.types.OperationStatus;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.net.URL;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Console implements NucleusClientListener {
    private static final String LOG_TAG = Console.class.getName();
    private static final Logger LOGGER = Logger.getLogger(LOG_TAG);

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static void main(String[] args) throws Exception {
        LOGGER.info("Starting up beaglebone demo console...");
        String screenName = (args[0] == null ? "Someone" : args[0]);
        Console console = new Console(screenName);
        console.run();
    }

    private Console(String screenName) throws IOException {
        // If we dont have a serial number, then generate a random UUID
        String serialNo = null;
        File f = new File("serial.dat");
        if( !f.exists() && !f.isDirectory()) {
            serialNo = UUID.randomUUID().toString();
            FileWriter fileWriter = new FileWriter("serial.dat");
            fileWriter.write(serialNo);
        }
        else {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(f));
            serialNo = bufferedReader.readLine();
        }

        Driver.manufacturer = "Seeed";
        Driver.serialNumber = serialNo;
        Driver.deviceType = DeviceType.COMPUTER.getValue();
        Driver.modelIdentifier = "MacBook";
        Driver.os = "macOS";
        Driver.osVersion = "10.12.6";
        Driver.screenName = screenName;

        LOGGER.info("Using the following properties:" +
                            "\n  Screen Name:   " + Driver.screenName +
                            "\n  Location:      " + Driver.myLocation +
                            "\n  Serial Number: " + Driver.serialNumber);
    }

    private void run() throws NucleusException {
        String namespace = "com." + Driver.manufacturer + "." +
                (Driver.serialNumber == null ? UUID.randomUUID().toString() : Driver.serialNumber);

        // Sadly, there is a bug in nucleus right now so all dashes need to be underbars
        String deviceID = UUID.nameUUIDFromBytes(namespace.getBytes()).toString().replaceAll("-", "_");
        ClientDevice device = NucleusFactory.clientDevice(deviceID, DeviceType.discoverMatchingEnum(Driver.deviceType),
                                                          Driver.manufacturer, Driver.modelIdentifier, Driver.serialNumber,
                                                          Driver.osVersion, Driver.os);
        Driver.nucleusClient = new NucleusClient(device, Driver.apiAccountID, Driver.apiAccountToken);
        Driver.nucleusClient.setServerTarget("http", Driver.SERVER_URL, Driver.SERVER_PORT);
        Driver.nucleusClient.setActivePartition(Driver.apiKey, Driver.apiToken);
        Driver.nucleusClient.addListener(this);

        // Must be done after the client has been initialized
        device.setScreenName(Driver.screenName);
        device.setCurrentLocation(Driver.myLocation, new DeviceSetDatapointResponseHandler() {
            @Override
            public void onSuccess(Datapoint updatedDatapoint) {
                LOGGER.info("Successfully set my location to: " + Driver.myLocation);
            }

            @Override
            public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage) {
                LOGGER.severe("FAILED TO SET LOCATION. (" + statusCode + "), " + operationStatus + ", " + errorMessage);
            }
        });

        startSession();

        LOGGER.info("Exiting!");
    }

    private void startSession() throws NucleusException {
        final DeviceService deviceService = Driver.nucleusClient.getDeviceService();
        deviceService.startSession(new DeviceSessionResponseHandler() {
            @Override
            public void onSuccess(boolean needsProfile, List<String> activeMemberships) {
                LOGGER.info("startSession.onSuccess:" + activeMemberships.size());

                if ( needsProfile ) {
                    ClientDevice clientDevice = Driver.nucleusClient.getClientDevice();
                    if (Driver.imageData != null) {
                        clientDevice.setProfileImage(DatatypeConverter.parseBase64Binary(Driver.imageData), Driver.imageType);
                    }
                    deviceService.setProfile(new GeneralResponseHandler() {
                        @Override
                        public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage) {
                            LOGGER.severe("Failed to update device profile. (" + statusCode + "), " + operationStatus + ", " + errorMessage);
                        }

                        @Override
                        public void onSuccess() {
                            LOGGER.info("Successfully updated profile!");
                        }
                    });
                }

                // These are asynchronous calls, so they may or may not complete before the process moves onto the next
                // lines of code.
                joinOrCreateChannel(Driver.channelName, "");

                Scanner scanner = new Scanner(System.in);

                final Runnable timer = () -> {
                    try {
                        System.out.print("Enter your message: ");

                        String message = scanner.next();
                        System.out.print("Sending message...");
                    }
                    catch( Exception e ) {
                        e.printStackTrace();
                        LOGGER.severe("Caught exception: " + e.getMessage());
                    }
                };
                scheduler.scheduleAtFixedRate(timer, 0, 1, TimeUnit.SECONDS);
            }

            @Override
            public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage) {
                LOGGER.info("Failed to start device session. " + operationStatus + ", statusCode = " +
                                    statusCode + ", errorMessage = " + errorMessage);
                System.exit(-1);
            }
        });
    }

    private void joinOrCreateChannel(final String channelName, final String password) {
        LOGGER.info("Find channel by: " + channelName + ", password: " + ((password != null) ? password : ""));
        final ChannelService channelService = Driver.nucleusClient.getChannelService();
        channelService.findChannelByName(channelName, password, new ChannelFindByNameResponseHandler() {

            @Override
            public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage) {
                LOGGER.info("Failed to find by name. " + operationStatus + ", statusCode = " +
                                    statusCode + ", errorMessage = " + errorMessage);
                System.exit(-1);
            }

            @Override
            public void onSuccess(final String channelRef) {
                LOGGER.info("Found channel by name. channelRef = " + channelRef);

                if ( channelRef == null ) {
                    createChannel(channelName, password);
                }
                else {
                    try {
                        joinChannel(channelRef);
                    } catch ( NucleusException e ) {
                        LOGGER.severe("Failed to join channel. channelRef = " + channelRef);
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void createChannel(String channelName, String password) {
        final ChannelService channelService = Driver.nucleusClient.getChannelService();
        GeoCircle circle = new GeoCircle(Driver.myLocation.getLatitude(), Driver.myLocation.getLongitude(), 100);
        channelService.createChannel(circle, 0, 0, channelName, password, "Site", false,
                                     null, Driver.shortDescription, Driver.longDescription, new ChannelCreateResponseHandler() {
                    @Override
                    public void onSuccess(String channelRef) {
                        // Creating a channel automatically joins you to it, but does not automatically switch
                        // you into it.
                        LOGGER.info("Successfully created channel. channelRef = " + channelRef);
                        channelService.switchChannel(channelRef, new GeneralResponseHandler() {
                            @Override
                            public void onSuccess() {
                                // Now that we have our channel created, we want to enable polling on it so that we
                                // can respond to any chat messages to display on our OLED
                                Driver.nucleusClient.enablePolling(true);
                            }

                            @Override
                            public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage) {
                                LOGGER.info("Failed to create channel. " + operationStatus + ", statusCode = " +
                                                    statusCode + ", errorMessage = " + errorMessage);
                                System.exit(-1);
                            }
                        });
                    }

                    @Override
                    public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage) {
                        LOGGER.info("Failed to create channel. " + operationStatus + ", statusCode = " +
                                            statusCode + ", errorMessage = " + errorMessage);
                        System.exit(-1);
                    }
                });
    }

    private void joinChannel(final String channelRef) throws NucleusException {
        final ChannelService channelService = Driver.nucleusClient.getChannelService();
        channelService.joinChannel(channelRef, new ChannelJoinResponseHandler() {
            @Override
            public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage) {
                LOGGER.info("Failed to join channel. channelRef=" + channelRef +
                                    ", " + operationStatus + ", statusCode = " +
                                    statusCode + ", errorMessage = " + errorMessage);
                System.exit(-1);
            }

            @Override
            public void onSuccess(final String channelRef, List<TopicOffset> offsets) {
                LOGGER.info("joinChannel.onSuccess");
                channelService.switchChannel(channelRef, new GeneralResponseHandler() {
                    @Override
                    public void onSuccess() {
                        // Now that we have our channel created, we want to enable polling on it so that we
                        // can respond to any chat messages to display on our OLED
                        Driver.nucleusClient.enablePolling(true);
                    }

                    @Override
                    public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage) {
                        LOGGER.info("Failed to create channel. " + operationStatus + ", statusCode = " +
                                            statusCode + ", errorMessage = " + errorMessage);
                        System.exit(-1);
                    }
                });
            }
        });
    }

    @Override
    public void handleUnsupportedVersion(String s) {

    }

    @Override
    public void handleOldVersion(String s) {

    }

    @Override
    public void onConnected(boolean b) {

    }

    @Override
    public void handleServerMessage(String s) {

    }

    @Override
    public void handleServerRequest(URL url) {

    }

    @Override
    public void handleBoot(String s, Member member) {

    }

    @Override
    public void handleRequestError(String s, OperationStatus operationStatus, int i, String s1) {

    }

    @Override
    public void handleException(Throwable throwable) {

    }

    @Override
    public void onError(int i, String s) {

    }

    @Override
    public void onErrorReset() {

    }

    @Override
    public void onInactiveExpiration() {

    }

    @Override
    public void onLowPowerNotification() {

    }

    @Override
    public void onChange(String s, List<ChangeType> list) {

    }

    @Override
    public void onPropertyChange(String s, Property property) {

    }

    @Override
    public void onMessageRemoved(ChannelMessage channelMessage) {

    }

    @Override
    public void onMessageChange(ChannelMessage channelMessage) {

    }

    @Override
    public void onChannelHealthChange(String s, HealthType healthType, HealthType healthType1) {

    }

    @Override
    public void onInternetActive(boolean b) {

    }

    @Override
    public void onMemberDatapointChange(Member member) {

    }

    @Override
    public void onMemberProfileChange(Member member) {

    }

    @Override
    public void onMemberStatusChange(Member member) {

    }

    @Override
    public void onMemberPresenceChange(Member member) {

    }

    @Override
    public void onMemberLocationChange(Member member, NucleusLocation nucleusLocation) {

    }
}

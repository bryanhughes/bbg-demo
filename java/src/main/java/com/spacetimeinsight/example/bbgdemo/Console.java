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

import java.io.*;
import java.util.Base64;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.*;

import static com.spacetimeinsight.example.bbgdemo.Driver.nucleusClient;

public class Console implements NucleusClientListener {
    private static final String LOG_TAG = Console.class.getName();
    private static final Logger LOGGER = Logger.getLogger(LOG_TAG);

    private ExecutorService executor;
    private Scanner scanner;

    public static void main(String[] args) throws Exception {
        LOGGER.info("Starting up beaglebone demo console...");
        Console console = new Console(args);
        console.startSession();
    }

    private Console(String[] args) throws IOException {
        Level level = Level.SEVERE;
        if ( (args.length > 1) && "-level".equals(args[0]) ) {
            String l = args[1].toLowerCase();
            System.out.println("Setting log level to " + l);
            switch (l) {
                case "off": level = Level.OFF; break;
                case "severe": level = Level.SEVERE; break;
                case "warning": level = Level.WARNING; break;
                case "info": level = Level.INFO; break;
                case "fine": level = Level.FINE; break;
                case "finer": level = Level.FINER; break;
                case "finest": level = Level.FINEST; break;
                default: level = Level.SEVERE;
            }
        }

        Logger rootLogger = LogManager.getLogManager().getLogger("");
        rootLogger.setLevel(level);
        for (Handler h : rootLogger.getHandlers()) {
            h.setLevel(level);
        }

        scanner = new Scanner(System.in);
        scanner.useDelimiter("\n");

        // If we dont have a serial number, then generate a random UUID
        String serialNo;
        File f = new File("serial.dat");
        if( !f.exists() && !f.isDirectory()) {
            serialNo = UUID.randomUUID().toString();
            System.out.println("Running for the first time! Generated serial number: " + serialNo);
            FileWriter fileWriter = new FileWriter("serial.dat");
            fileWriter.write(serialNo);
            fileWriter.close();
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
        Driver.screenName = "console";

        String namespace = "com." + Driver.manufacturer + "." +
                (serialNo == null ? UUID.randomUUID().toString() : Driver.serialNumber);

        // Sadly, there is a bug in nucleus right now so all dashes need to be underbars
        String deviceID = UUID.nameUUIDFromBytes(namespace.getBytes()).toString().replaceAll("-", "_");

        LOGGER.info("Using the following properties:" +
                            "\n  Device ID:     " + deviceID +
                            "\n  Screen Name:   " + Driver.screenName +
                            "\n  Location:      " + Driver.myLocation +
                            "\n  Serial Number: " + Driver.serialNumber);

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

        executor = Executors.newCachedThreadPool();
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
                        clientDevice.setProfileImage(Base64.getDecoder().decode(Driver.imageData), Driver.imageType);
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
                joinOrCreateChannel(Driver.channelName);
            }

            @Override
            public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage) {
                LOGGER.severe("Failed to start device session. " + operationStatus + ", statusCode = " +
                                    statusCode + ", errorMessage = " + errorMessage);
                System.exit(-1);
            }
        });
    }

    private void renewSession()  {
        System.out.println("Renewing session...");
        DeviceService deviceService = nucleusClient.getDeviceService();
        try {
            deviceService.renewSession(new DeviceSessionResponseHandler() {
                @Override
                public void onSuccess(boolean needsProfile, List<String> activeMemberships) {
                    System.out.println("Successfully renewed device session.");
                }

                @Override
                public void onFailure(OperationStatus operationStatus, int statusCode, String errorMsg) {
                    System.out.println("Failed to renew device session. (" + statusCode + ") - " + errorMsg);
                }
            });
        } catch ( NucleusException e ) {
            e.printStackTrace();
        }
    }

    private void joinOrCreateChannel(final String channelName) {
        LOGGER.info("Find channel by: " + channelName);
        final ChannelService channelService = Driver.nucleusClient.getChannelService();
        channelService.findChannelByName(channelName, null, new ChannelFindByNameResponseHandler() {

            @Override
            public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage) {
                LOGGER.severe("Failed to find by name. " + operationStatus + ", statusCode = " +
                                    statusCode + ", errorMessage = " + errorMessage);
                System.exit(-1);
            }

            @Override
            public void onSuccess(final String channelRef) {
                LOGGER.info("Found channel by name. channelRef = " + channelRef);

                if ( channelRef == null ) {
                    createChannel(channelName);
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

    private void createChannel(String channelName) {
        final ChannelService channelService = Driver.nucleusClient.getChannelService();
        GeoCircle circle = new GeoCircle(Driver.myLocation.getLatitude(), Driver.myLocation.getLongitude(), 100);
        channelService.createChannel(circle, -1, -1, channelName, null, "Site", false,
                                     null, Driver.shortDescription, Driver.longDescription, new ChannelCreateResponseHandler() {
                    @Override
                    public void onSuccess(String channelRef) {
                        // Creating a channel automatically joins you to it, but does not automatically switch
                        // you into it.
                        System.out.println("Successfully created channel. channelRef = " + channelRef);
                        channelService.switchChannel(channelRef, new GeneralResponseHandler() {
                            @Override
                            public void onSuccess() {
                                scanLoop(channelRef);
                            }

                            @Override
                            public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage) {
                                LOGGER.severe("Failed to create channel. " + operationStatus + ", statusCode = " +
                                                    statusCode + ", errorMessage = " + errorMessage);
                                System.exit(-1);
                            }
                        });
                    }

                    @Override
                    public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage) {
                        LOGGER.severe("Failed to create channel. " + operationStatus + ", statusCode = " +
                                            statusCode + ", errorMessage = " + errorMessage);
                        System.exit(-1);
                    }
                });
    }

    private void scanLoop(String channelRef) {
        Channel channel = Driver.nucleusClient.getCurrentChannel();
        System.out.println("Console Device ID: " + Driver.nucleusClient.getClientDevice().getDeviceID());
        System.out.println("There are currently " + channel.getMembersCount() + " members in this channel.\n");
        System.out.println("Enter command: (d <display message> | l <led comma separated values R,G,B> | q <ask question> | s <shutdown level>");
        Runnable r = () -> {
            while (!Thread.currentThread().isInterrupted()) {
                scan(channelRef);
            }
        };
        executor.execute(r);
    }

    private void scan(String channelRef) {
        ChannelService channelService = Driver.nucleusClient.getChannelService();

        String message = scanner.next();
        char c = message.charAt(0);
        if ( c == 'q' ) {
            String m = message.substring(1).trim();
            List<MimePart> mimeParts = new ArrayList<>();
            mimeParts.add(new MimePart("text/plain", "", m.getBytes()));
            MimeMessage mimeMessage = new MimeMessage(mimeParts);
            channelService.publish(channelRef, mimeMessage, new ChannelPublishMessageResponseHandler() {
                @Override
                public void onSuccess(long offset, long eventID) {
                    System.out.println("[ok]");
                }

                @Override
                public void onFailure(OperationStatus operationStatus, int statusCode, String errorMsg) {
                    System.out.println("!!!! Failed to send question - (" + statusCode + ") " + errorMsg);
                }
            });
        }
        else if ( c == 'd' ) {
            String m = message.substring(1).trim();
            Channel channel = Driver.nucleusClient.getChannel(channelRef);
            channel.setProperty("display", m, new GeneralResponseHandler() {
                @Override
                public void onSuccess() {
                    System.out.println("[ok]");
                }

                @Override
                public void onFailure(OperationStatus operationStatus, int statusCode, String errorMsg) {
                    System.out.println("!!!! Failed to set property - (" + statusCode + ") " + errorMsg);
                }
            });
        }
        else if ( c == 'l' ) {
            String m = message.substring(1).trim();
            Channel channel = Driver.nucleusClient.getChannel(channelRef);
            channel.setProperty("led", m, new GeneralResponseHandler() {
                @Override
                public void onSuccess() {
                    System.out.println("[ok]");
                }

                @Override
                public void onFailure(OperationStatus operationStatus, int statusCode, String errorMsg) {
                    System.out.println("!!!! Failed to set property - (" + statusCode + ") " + errorMsg);
                }
            });
        }
        else if ( c == 's' ) {
            String m = message.substring(1).trim();
            if ( m.isEmpty() ) {
                m = "now";
            }
            Channel channel = Driver.nucleusClient.getChannel(channelRef);
            channel.setProperty("shutdown", m, new GeneralResponseHandler() {
                @Override
                public void onSuccess() {
                    System.out.println("[ok] Device will shutdown");
                }

                @Override
                public void onFailure(OperationStatus operationStatus, int statusCode, String errorMsg) {
                    System.out.println("!!!! Failed to set property - (" + statusCode + ") " + errorMsg);
                }
            });
        }
        else {
            System.out.println("commands are");
            System.out.println("   d <display message>");
            System.out.println("   l x, y, z (LED values 0-255)");
            System.out.println("   q <question to ask>");
            System.out.println("   s now | <time in seconds> : shutdown the device");
        }
    }

    private void joinChannel(final String channelRef) throws NucleusException {
        final ChannelService channelService = Driver.nucleusClient.getChannelService();
        channelService.joinChannel(channelRef, new ChannelJoinResponseHandler() {
            @Override
            public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage) {
                LOGGER.severe("Failed to join channel. channelRef=" + channelRef +
                                    ", " + operationStatus + ", statusCode = " +
                                    statusCode + ", errorMessage = " + errorMessage);
                System.exit(-1);
            }

            @Override
            public void onSuccess(final String channelRef, List<TopicOffset> offsets) {
                System.out.println("Successfully joined channel. channelRef=" + channelRef);
                channelService.switchChannel(channelRef, new GeneralResponseHandler() {
                    @Override
                    public void onSuccess() {
                        scanLoop(channelRef);
                    }

                    @Override
                    public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage) {
                        LOGGER.severe("Failed to create channel. " + operationStatus + ", statusCode = " +
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
    public void handleRequestError(String command, OperationStatus operationStatus, int statusCode, String errorMsg) {
        System.out.println("Handling request error. Command = " + command +
                                   ", operationStatus = " + operationStatus.toString() +
                                   ", statusCode = " + statusCode +
                                   ", errorMessage = " + errorMsg);

        if ( OperationStatus.INVALID_DEVICE_TOKEN.equals(operationStatus) ) {
            renewSession();
        }
        else {
            System.out.println("Handling request error. " + command + " (" + statusCode + ") " + errorMsg);
        }
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
    public void onDatapointChange(Datapoint datapoint) {

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

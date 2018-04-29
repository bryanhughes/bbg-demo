package com.spacetimeinsight.example.bbgdemo;

import com.spacetimeinsight.nucleus.java.NucleusClient;
import com.spacetimeinsight.nucleuslib.*;
import com.spacetimeinsight.nucleuslib.core.CachePolicy;
import com.spacetimeinsight.nucleuslib.core.ClientDevice;
import com.spacetimeinsight.nucleuslib.core.NucleusFactory;
import com.spacetimeinsight.nucleuslib.datamapped.*;
import com.spacetimeinsight.nucleuslib.listeners.NucleusChannelListener;
import com.spacetimeinsight.nucleuslib.listeners.NucleusClientListener;
import com.spacetimeinsight.nucleuslib.responsehandlers.*;
import com.spacetimeinsight.nucleuslib.types.*;

import java.io.*;
import java.util.*;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.*;

import static com.spacetimeinsight.example.bbgdemo.Driver.nucleusClient;
import static java.lang.System.exit;

public class Console {
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
        String deviceID = UUID.nameUUIDFromBytes(namespace.getBytes()).toString();

        LOGGER.info("Using the following properties:" +
                            "\n  Device ID:     " + deviceID +
                            "\n  Screen Name:   " + Driver.screenName +
                            "\n  Location:      " + Driver.myLocation +
                            "\n  Serial Number: " + Driver.serialNumber);

        ClientDevice device = NucleusFactory.clientDevice(deviceID, DeviceType.discoverMatchingEnum(Driver.deviceType),
                                                          Driver.manufacturer, Driver.modelIdentifier, 
                                                          Driver.serialNumber, Driver.osVersion, Driver.os, 
                                                          NucleusClient.NullLocation);
        Driver.nucleusClient = new NucleusClient(device, Driver.apiAccountID, Driver.apiAccountToken,
                                                 System.getProperty("user.dir"), new CachePolicy());
        Driver.nucleusClient.setServerTarget("http", Driver.SERVER_URL, Driver.SERVER_PORT);
        Driver.nucleusClient.setActivePartition(Driver.apiKey, Driver.apiToken);

        Driver.nucleusClient.addListener(ListenerType.CLIENT, new NucleusClientListener() {
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
            public void handleRequestError(String command, OperationStatus operationStatus, int statusCode,
                                           String errorMsg) {
                System.out.println(
                        "Handling request error. Command = " + command + ", operationStatus = " + operationStatus.toString() + ", statusCode = " + statusCode + ", errorMessage = " + errorMsg);

                if (OperationStatus.INVALID_DEVICE_TOKEN.equals(operationStatus)) {
                    renewSession();
                } else {
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
            public void onInternetActive(boolean b) {

            }

            @Override
            public void onDeviceLocationChange(NucleusLocation nucleusLocation) {

            }
        });

        Driver.nucleusClient.addListener(ListenerType.CHANNEL, new NucleusChannelListener() {
            @Override
            public void onPropertyChange(String channerRef, Property property) {
                System.out.print("   > ack " + property.getName() + " = " + property.getValue());
            }

            @Override
            public void onChannelHealthChange(String s, HealthType healthType, HealthType healthType1) {

            }

            @Override
            public void onChange(String s, List<ChangeType> list) {

            }
        });

        // Must be done after the client has been initialized
        device.setScreenName(Driver.screenName);
        device.setCurrentLocation(Driver.myLocation, new DeviceSetDatapointResponseHandler() {
            @Override
            public void onSuccess(Datapoint updatedDatapoint) {
                LOGGER.info("Successfully set my location to: " + Driver.myLocation);
            }

            @Override
            public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage, 
                                  boolean retryable) {
                LOGGER.severe("FAILED TO SET LOCATION. (" + statusCode + "), " + operationStatus + ", " + errorMessage);
            }
        });

        executor = Executors.newCachedThreadPool();
    }

    private void startSession() {
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
                        public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage,
                                              boolean retryable) {
                            LOGGER.severe("Failed to update device profile. (" + statusCode + "), " + 
                                          operationStatus + ", " + errorMessage);
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
            public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage, boolean retryable) {
                System.out.println("Session failed (" + statusCode + "). Retrying...");
            }
        });
    }

    private void renewSession()  {
        System.out.println("Renewing session...");
        DeviceService deviceService = nucleusClient.getDeviceService();
        deviceService.renewSession(new DeviceSessionResponseHandler() {
            @Override
            public void onSuccess(boolean needsProfile, List<String> activeMemberships) {
                System.out.println("Successfully renewed device session.");
            }

            @Override
            public void onFailure(OperationStatus operationStatus, int statusCode, String errorMsg,
                                  boolean retryable) {
                System.out.println("Failed to renew device session. (" + statusCode + ") - " + errorMsg);
            }
        });
    }

    private void joinOrCreateChannel(final String channelName) {
        LOGGER.info("Find channel by: " + channelName);
        final ChannelService channelService = Driver.nucleusClient.getChannelService();
        channelService.findChannelByName(channelName, null, new ChannelFindByNameResponseHandler() {

            @Override
            public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage,
                                  boolean retryable) {
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
                    joinChannel(channelRef);
                }
            }
        });
    }

    private void createChannel(String channelName) {
        final ChannelService channelService = Driver.nucleusClient.getChannelService();
        GeoCircle circle = new GeoCircle(Driver.myLocation.getLatitude(), Driver.myLocation.getLongitude(), 100);
        channelService.createChannel(circle, -1, -1, channelName, null, "Site", false, "Site",
                                     null, Driver.shortDescription, Driver.longDescription, new ChannelCreateResponseHandler() {
                    @Override
                    public void onSuccess(String channelRef) {
                        // Creating a channel automatically joins you to it, but does not automatically switch
                        // you into it.
                        System.out.println("Successfully created channel. channelRef = " + channelRef);
                        try {
                            channelService.switchChannel(channelRef, new GeneralResponseHandler() {
                                @Override
                                public void onSuccess() {
                                    scanLoop(channelRef);
                                }
    
                                @Override
                                public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage,
                                                      boolean retryable) {
                                    LOGGER.severe("Failed to create channel. " + operationStatus + ", statusCode = " +
                                                        statusCode + ", errorMessage = " + errorMessage);
                                    System.exit(-1);
                                }
                            });
                        }
                        catch (NucleusException e) {
                            e.printStackTrace();
                            LOGGER.severe("Failed to switch channel.");
                            System.exit(-1);
                        }
                    }

                    @Override
                    public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage,
                                          boolean retryable) {
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
            mimeParts.add(new MimePart(MimeType.TEXT_PLAIN, "", m.getBytes()));
            MimeMessage mimeMessage = new MimeMessage(mimeParts);
            channelService.publish(channelRef, mimeMessage, new ChannelPublishMessageResponseHandler() {
                @Override
                public void onSuccess(long offset, long eventID) {
                    System.out.println("[ok]");
                }

                @Override
                public void onFailure(OperationStatus operationStatus, int statusCode, String errorMsg,
                                      boolean retryable) {
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
                public void onFailure(OperationStatus operationStatus, int statusCode, String errorMsg,
                                      boolean retryable) {
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
                public void onFailure(OperationStatus operationStatus, int statusCode, String errorMsg,
                                      boolean retryable) {
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
                public void onFailure(OperationStatus operationStatus, int statusCode, String errorMsg,
                                      boolean retryable) {
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

    private void joinChannel(final String channelRef) {
        final ChannelService channelService = nucleusClient.getChannelService();

        Map<JoinOption, Object> joinOptions = JoinOption.getLastNChanges(1);
        channelService.joinChannel(channelRef, joinOptions, new ChannelJoinResponseHandler() {
            @Override
            public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage,
                                  boolean retryable) {
                LOGGER.severe("Failed to join channel. channelRef=" + channelRef +
                              " : " + operationStatus + " : statusCode = " +
                              statusCode + " : errorMessage = " + errorMessage);
                exit(-1);
            }

            @Override
            public void onSuccess(final String channelRef) {
                try {
                    channelService.switchChannel(channelRef, new GeneralResponseHandler() {
                        @Override
                        public void onSuccess() {
                            Channel channel = nucleusClient.getChannel(channelRef);
                            System.out.println("Successfully joined channel " + channel.getName() + ". channelRef=" + channelRef);
                            scanLoop(channelRef);
                        }
    
                        @Override
                        public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage,
                                              boolean retryable) {
                            LOGGER.severe("Failed to create channel. " + operationStatus + ", statusCode = " +
                                          statusCode + ", errorMessage = " + errorMessage);
                            exit(-1);
                        }
                    });
                }
                catch (NucleusException e) {
                    e.printStackTrace();
                    LOGGER.severe("Failed to switch channel.");
                    exit(-1);
                }
            }
        });
    }
}

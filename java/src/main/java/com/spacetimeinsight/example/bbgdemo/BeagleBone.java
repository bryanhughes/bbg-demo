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
package com.spacetimeinsight.example.bbgdemo;

import com.spacetimeinsight.nucleus.java.NucleusClient;
import com.spacetimeinsight.nucleuslib.*;
import com.spacetimeinsight.nucleuslib.core.ClientDevice;
import com.spacetimeinsight.nucleuslib.core.NucleusFactory;
import com.spacetimeinsight.nucleuslib.datamapped.*;
import com.spacetimeinsight.nucleuslib.responsehandlers.*;
import com.spacetimeinsight.nucleuslib.types.*;
import com.spacetimeinsight.protobuf.nano.EnvDataProto;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Demonstration BeagleBone
 */
public class BeagleBone implements NucleusClientListener
{
    private static String MESSAGES_FILENAME = "/tmp/message.dat";
    private static String SENSOR_FILENAME = "/tmp/sensor.dat";
    private static String LED_FILENAME = "/tmp/led.dat";

    protected BeagleBone(String[] args) throws IOException {

        Level level = Level.INFO;
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
                default: level = Level.INFO;
            }
        }

        Logger rootLogger = LogManager.getLogManager().getLogger("");
        rootLogger.setLevel(level);
        for (Handler h : rootLogger.getHandlers()) {
            h.setLevel(level);
        }

        Runtime rt = Runtime.getRuntime();
        String[] commands = {"./show-serial.sh"};
        Process proc = rt.exec(commands);

        BufferedReader stdInputReader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        String stdInputStr = stdInputReader.readLine();

        Driver.manufacturer = "Seeed";
        Driver.serialNumber = stdInputStr;
        Driver.deviceType = DeviceType.MICRO_CONTROLLER.getValue();
        Driver.modelIdentifier = "BeagleBone Black";
        Driver.os = "Debian";
        Driver.osVersion = "8.7";
        Driver.screenName = stdInputStr;

        System.out.println("Using the following properties:" +
                            "\n  Screen Name:   " + Driver.screenName +
                            "\n  Location:      " + Driver.myLocation +
                            "\n  Serial Number: " + Driver.serialNumber);
    }

    private void joinOrCreateChannel(final String channelName) {
        System.out.println("Find channel by: " + channelName);
        final ChannelService channelService = Driver.nucleusClient.getChannelService();
        channelService.findChannelByName(channelName, null, new ChannelFindByNameResponseHandler() {

            @Override
            public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage) {
                System.out.println("Failed to find by name. " + operationStatus + ", statusCode = " +
                            statusCode + ", errorMessage = " + errorMessage);
                System.exit(-1);
            }

            @Override
            public void onSuccess(final String channelRef) {
                System.out.println("Found channel by name. channelRef = " + channelRef);

                if ( channelRef == null ) {
                    createChannel(channelName);
                }
                else {
                    try {
                        joinChannel(channelRef);
                    } catch ( NucleusException e ) {
                        System.out.println("Failed to join channel. channelRef = " + channelRef);
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void createChannel(String channelName) {
        final ChannelService channelService = Driver.nucleusClient.getChannelService();
        GeoCircle circle = new GeoCircle(Driver.myLocation.getLatitude(), Driver.myLocation.getLongitude(), 100);
        channelService.createChannel(circle, 0, 0, channelName, null, "Site", false,
                                     null, Driver.shortDescription, Driver.longDescription, new ChannelCreateResponseHandler() {
                    @Override
                    public void onSuccess(String channelRef) {
                        // Creating a channel automatically joins you to it, but does not automatically switch
                        // you into it.
                        System.out.println("Successfully created channel. channelRef = " + channelRef);
                        channelService.switchChannel(channelRef, new GeneralResponseHandler() {
                            @Override
                            public void onSuccess() {
                                // Now that we have our channel created, we want to enable polling on it so that we
                                // can respond to any chat messages to display on our OLED
                                Driver.nucleusClient.enablePolling(true);
                                sendLoop();
                            }

                            @Override
                            public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage) {
                                System.out.println("Failed to create channel. " + operationStatus + ", statusCode = " +
                                            statusCode + ", errorMessage = " + errorMessage);
                                System.exit(-1);
                            }
                        });
                    }

                    @Override
                    public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage) {
                        System.out.println("Failed to create channel. " + operationStatus + ", statusCode = " +
                                    statusCode + ", errorMessage = " + errorMessage);
                        System.exit(-1);
                    }
                });
    }

    private void joinChannel(final String channelRef) throws NucleusException {
        final ChannelService channelService = Driver.nucleusClient.getChannelService();

        Map<JoinOption, Object> joinOptions = new HashMap<>();
        List<TopicOffset> offsets = new ArrayList<>();
        offsets.add(new TopicOffset(channelRef, TopicType.EChannelMessage, -1, 1));
        joinOptions.put(JoinOption.OFFSET_LIST, offsets);

        channelService.joinChannel(channelRef, joinOptions, new ChannelJoinResponseHandler() {
            @Override
            public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage) {
                System.out.println("Failed to join channel. channelRef=" + channelRef +
                            " : " + operationStatus + " : statusCode = " +
                            statusCode + " : errorMessage = " + errorMessage);
                System.exit(-1);
            }

            @Override
            public void onSuccess(final String channelRef, List<TopicOffset> offsets) {
                System.out.println("joinChannel.onSuccess");
                channelService.switchChannel(channelRef, new GeneralResponseHandler() {
                    @Override
                    public void onSuccess() {
                        // Now that we have our channel created, we want to enable polling on it so that we
                        // can respond to any chat messages to display on our OLED
                        Driver.nucleusClient.enablePolling(true);
                        sendLoop();
                    }

                    @Override
                    public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage) {
                        System.out.println("Failed to create channel. " + operationStatus + ", statusCode = " +
                                    statusCode + ", errorMessage = " + errorMessage);
                        System.exit(-1);
                    }
                });
            }
        });
    }

    private void sendLoop() {
        final Runnable timer = () -> {
            try {
                sendData();
            }
            catch( Exception e ) {
                e.printStackTrace();
                System.out.println("Caught exception: " + e.getMessage());
            }
        };
        scheduler.scheduleAtFixedRate(timer, 0, 1, TimeUnit.SECONDS);
    }

    private void startSession() throws NucleusException {
        final DeviceService deviceService = Driver.nucleusClient.getDeviceService();
        deviceService.startSession(new DeviceSessionResponseHandler() {
            @Override
            public void onSuccess(boolean needsProfile, List<String> activeMemberships) {
                System.out.println("startSession.onSuccess:" + activeMemberships.size());

                if ( needsProfile ) {
                    ClientDevice clientDevice = Driver.nucleusClient.getClientDevice();
                    if (Driver.imageData != null) {
                        clientDevice.setProfileImage(DatatypeConverter.parseBase64Binary(Driver.imageData), Driver.imageType);
                    }
                    deviceService.setProfile(new GeneralResponseHandler() {
                        @Override
                        public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage) {
                            System.out.println("Failed to update device profile. (" + statusCode + "), " + operationStatus + ", " + errorMessage);
                        }

                        @Override
                        public void onSuccess() {
                            System.out.println("Successfully updated profile!");
                        }
                    });
                }

                // These are asynchronous calls, so they may or may not complete before the process moves onto the next
                // lines of code.
                joinOrCreateChannel(Driver.channelName);
            }

            @Override
            public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage) {
                System.out.println("Failed to start device session. " + operationStatus + ", statusCode = " +
                            statusCode + ", errorMessage = " + errorMessage);
                System.exit(-1);
            }
        });
    }

    private void sendData() {
        final IOBridge ioBridge = new IOBridge();
        com.spacetimeinsight.example.bbgdemo.IOBridge.SensorData sensorData = ioBridge.getSensorData();

        EnvData envData = new EnvData(sensorData.timestamp, sensorData.temperature, 0, sensorData.humidity, 0);

        EnvDataProto.EnvData envDataProto = envData.toProtoBuffer();
        DeviceService deviceService = Driver.nucleusClient.getDeviceService();
        Datapoint datapoint = deviceService.newDatapoint("weather", 0, "EnvData", EnvDataProto.EnvData.toByteArray(envDataProto));
        System.out.println("<<< SENDING DATAPOINT : " + envData);
        deviceService.setDatapoint(datapoint, new DeviceSetDatapointResponseHandler() {
            @Override
            public void onSuccess(Datapoint updatedDatapoint) {
                System.out.println(">>> Successfully set sensor data: "  + envData.toString());
            }

            @Override
            public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage) {
                System.out.println("FAILED to set datapoint. operationStatus = " + operationStatus +
                              ", statusCode = " + statusCode +
                              ", errorMessage = " + errorMessage);
            }
        });
    }

    /**
     *
     */
    private void execute() throws NucleusException, IllegalAccessException, InstantiationException {
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
                System.out.println("Successfully set my location to: " + Driver.myLocation);
            }

            @Override
            public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage) {
                System.out.println("FAILED TO SET LOCATION. (" + statusCode + "), " + operationStatus + ", " + errorMessage);
            }
        });

        startSession();
        System.out.println("Exiting!");
    }

    private void renewSession()  {
        System.out.println("Renewing session...");
        DeviceService deviceService = Driver.nucleusClient.getDeviceService();
        try {
            deviceService.renewSession(new DeviceSessionResponseHandler() {
                @Override
                public void onSuccess(boolean needsProfile, List<String> activeMemberships) {
                    System.out.println("Successfully renewed device session.");
                }

                @Override
                public void onFailure(OperationStatus operationStatus, int i, String s) {

                }
            });
        } catch ( NucleusException e ) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Starting up beaglebone green demo...");
        BeagleBone beagleBone = new BeagleBone(args);
        beagleBone.execute();
    }


    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    //

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
    public void handleRequestError(String command, OperationStatus operationStatus, int statusCode, String errorMessage) {
        System.out.println("Handling request error. Command = " + command +
                      ", operationStatus = " + operationStatus.toString() +
                      ", statusCode = " + statusCode +
                      ", errorMessage = " + errorMessage);

        if ( OperationStatus.INVALID_DEVICE_TOKEN.equals(operationStatus) ) {
            renewSession();
        }
        else {
            System.out.println("Handling request error. " + command + " (" + statusCode + ") " + errorMessage);
        }
    }

    @Override
    public void handleException(Throwable throwable) {

    }

    @Override
    public void onError(int errorNo, String errorMessage) {
        System.out.println("Handling error. errorNo = " + errorNo + ", errorMessage = " + errorMessage);
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
    public void onChange(String channelRef, List<ChangeType> changeTypes) {

    }

    @Override
    public void onPropertyChange(String channelRef, Property property) {
        // We got a property change. For our BeagleBone demo, the property is the LED light
        if ( property.getName().equals("led") ) {
            String valueStr = property.getValue();
            try {
                FileWriter fileWriter = new FileWriter(LED_FILENAME);
                fileWriter.write(valueStr);
                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onMessageRemoved(ChannelMessage channelMessage) {

    }

    @Override
    public void onMessageChange(ChannelMessage channelMessage) {
        // We got a new channel message, now display it on the OLED. It is important to remember that the actual
        // device control is being handled by the Python scripts. We write our message to a shared file that the
        // scripts are listening to


        MimeMessage mimeMessage = channelMessage.getMimeMessage();
        List<MimePart> parts = mimeMessage.getMimeParts();
        MimePart part = parts.get(0);

        String message = new String(part.getContent());
        System.out.println(">>> Handling channel message " + message + " >> " + MESSAGES_FILENAME);
        try {
            FileWriter fileWriter = new FileWriter(MESSAGES_FILENAME);
            fileWriter.write(message);
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onChannelHealthChange(String channelRef, HealthType oldHealthType, HealthType newHealthType) {

    }

    @Override
    public void onInternetActive(boolean flag) {

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
    public void onMemberLocationChange(Member member, NucleusLocation location) {

    }

    @Override
    public void onMemberPresenceChange(Member member) {

    }
}

/*
 * Copyright 2013 Go Factory LLC
 * Created on July 13, 2013 by gsc
 */

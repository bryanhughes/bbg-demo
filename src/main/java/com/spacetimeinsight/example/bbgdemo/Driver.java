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
import com.spacetimeinsight.nucleuslib.types.ChangeType;
import com.spacetimeinsight.nucleuslib.types.DeviceType;
import com.spacetimeinsight.nucleuslib.types.HealthType;
import com.spacetimeinsight.nucleuslib.types.OperationStatus;
import com.spacetimeinsight.protobuf.nano.EnvDataProto;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Demonstration Driver
 */
public class Driver implements NucleusClientListener
{
    private static final String LOG_TAG = Driver.class.getName();
    private static final Logger LOGGER = Logger.getLogger(LOG_TAG);

    private Driver() {
        Properties localProperties = loadProperties();
        if ( localProperties == null ) {
            System.out.println("Please set the local.properties with the following:\n");
            System.out.println("apiAccountID=<API Account ID>\n" +
                               "apiAccountToken=<API Account Token>\n" +
                               "apiKey=<partition key>\n" +
                               "apiToken=<partition token>\n" +
                               "channelName=<channel name>\n" +
                               "lat=<latitude of device>\n" +
                               "lng=<longitude of device>\n" +
                               "shortDescription=<short description>\n" +
                               "longDescription=<long description>\n" +
                               "deviceType=<device type>\n" +
                               "manufacturer=<manufacturer>\n" +
                               "serialNumber=<serial number>\n" +
                               "modelIdentifier=<model identifier>\n" +
                               "os=<OS of device>\n" +
                               "osVersion=<version of device os>\n"
                              );
            System.exit(-1);
        }

        apiAccountID = localProperties.getProperty("apiAccountID");
        apiAccountToken = localProperties.getProperty("apiAccountToken");
        apiKey = localProperties.getProperty("apiKey");
        apiToken = localProperties.getProperty("apiToken");
        channelName = localProperties.getProperty("channelName");

        double lat = Double.valueOf(localProperties.getProperty("lat"));
        double lng = Double.valueOf(localProperties.getProperty("lng"));

        myLocation = new NucleusLocation(lat, lng);

        screenName = localProperties.getProperty("screenName");
        if ( screenName == null ) {
            screenName = "A Weathercape";
        }

        shortDescription = localProperties.getProperty("shortDescription");
        longDescription = localProperties.getProperty("longDescription");

        SERVER_URL = localProperties.getProperty("serverHost");
        SERVER_PORT = Integer.valueOf(localProperties.getProperty("serverPort"));

        deviceType = localProperties.getProperty("deviceType");
        manufacturer = localProperties.getProperty("manufacturer");
        serialNumber = localProperties.getProperty("serialNumber");

        LOGGER.info("Using the following properties:" +
                    "\n  Screen Name:   " + screenName +
                    "\n  Location:      " + myLocation +
                    "\n  Serial Number: " + serialNumber);


        imageData = localProperties.getProperty("ProfileImage");
        imageType = localProperties.getProperty("ImageType");

        if ( (deviceType == null) || (manufacturer == null) || (serialNumber == null) ) {
            System.out.println("deviceType, manufacturer, and serialNumber must not be null");
            System.exit(-1);
        }

        modelIdentifier = localProperties.getProperty("modelIdentifier");
        os = localProperties.getProperty("os");
        osVersion = localProperties.getProperty("osVersion");
    }

    private void joinOrCreateChannel(final String channelName, final String password) {
        LOGGER.info("Find channel by: " + channelName + ", password: " + ((password != null) ? password : ""));
        final ChannelService channelService = nucleusClient.getChannelService();
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
        final ChannelService channelService = nucleusClient.getChannelService();
        GeoCircle circle = new GeoCircle(myLocation.getLatitude(), myLocation.getLongitude(), 100);
        channelService.createChannel(circle, 0, 0, channelName, password, "Site", false,
                                     null, shortDescription, longDescription, new ChannelCreateResponseHandler() {
                    @Override
                    public void onSuccess(String channelRef) {
                        // Creating a channel automatically joins you to it, but does not automatically switch
                        // you into it.
                        LOGGER.info("Successfully created channel. channelRef = " + channelRef);
                        channelService.switchChannel(channelRef, new GeneralResponseHandler() {
                            @Override
                            public void onSuccess() {
                                sendMessage();
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
        final ChannelService channelService = nucleusClient.getChannelService();
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
                        sendMessage();
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

    private void startSession() throws NucleusException {
        final DeviceService deviceService = nucleusClient.getDeviceService();
        deviceService.startSession(new DeviceSessionResponseHandler() {
            @Override
            public void onSuccess(boolean needsProfile, List<String> activeMemberships) {
                if ( needsProfile ) {
                    ClientDevice clientDevice = nucleusClient.getClientDevice();
                    if (imageData != null) {
                        clientDevice.setProfileImage(DatatypeConverter.parseBase64Binary(imageData), imageType);
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
                LOGGER.info("startSession.onSuccess:" + activeMemberships.size());
                joinOrCreateChannel(channelName, "");
            }

            @Override
            public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage) {
                LOGGER.info("Failed to start device session. " + operationStatus + ", statusCode = " +
                            statusCode + ", errorMessage = " + errorMessage);
                System.exit(-1);
            }
        });
    }

    private void sendMessage() {
        final IOBridge ioBridge = new IOBridge();
        com.spacetimeinsight.example.bbgdemo.IOBridge.SensorData sensorData = ioBridge.getSensorData();

        EnvData envData = new EnvData(sensorData.timestamp, sensorData.temperature, 0, sensorData.humidity, 0);

        EnvDataProto.EnvData envDataProto = envData.toProtoBuffer();
        DeviceService deviceService = nucleusClient.getDeviceService();
        Datapoint datapoint = deviceService.newDatapoint("weather", 0, "EnvData", EnvDataProto.EnvData.toByteArray(envDataProto));
        LOGGER.info("<<< SENDING DATAPOINT : " + envData);
        deviceService.setDatapoint(datapoint, new DeviceSetDatapointResponseHandler() {
            @Override
            public void onSuccess(Datapoint updatedDatapoint) {
                LOGGER.info(">>> Successfully set sensor data: "  + envData.toString());
            }

            @Override
            public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage) {
                LOGGER.severe("FAILED to set datapoint. operationStatus = " + operationStatus +
                              ", statusCode = " + statusCode +
                              ", errorMessage = " + errorMessage);
            }
        });
    }

    /**
     *
     */
    private void execute() throws NucleusException, IllegalAccessException, InstantiationException {
        String namespace = "com." + manufacturer + "." +
                           (serialNumber == null ? UUID.randomUUID().toString() : serialNumber);

        // Sadly, there is a bug in nucleus right now so all dashes need to be underbars
        String deviceID = UUID.nameUUIDFromBytes(namespace.getBytes()).toString().replaceAll("-", "_");
        ClientDevice device = NucleusFactory.clientDevice(deviceID, DeviceType.discoverMatchingEnum(deviceType),
                                                          manufacturer, modelIdentifier, serialNumber,
                                                          osVersion, os);
        nucleusClient = new NucleusClient(device, apiAccountID, apiAccountToken);
        nucleusClient.setServerTarget("http", SERVER_URL, SERVER_PORT);
        nucleusClient.setActivePartition(apiKey, apiToken);
        nucleusClient.addListener(this);

        // Must be done after the client has been initialized
        device.setScreenName(screenName);
        device.setCurrentLocation(myLocation, new DeviceSetDatapointResponseHandler() {
            @Override
            public void onSuccess(Datapoint updatedDatapoint) {
                LOGGER.info("Successfully set my location to: " + myLocation);
            }

            @Override
            public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage) {
                LOGGER.severe("FAILED TO SET LOCATION. (" + statusCode + "), " + operationStatus + ", " + errorMessage);
            }
        });

        startSession();

        // Create our scheduler...
        final Runnable timer = () -> {
            try {
                sendMessage();
            }
            catch( Exception e ) {
                e.printStackTrace();
                LOGGER.severe("Caught exception: " + e.getMessage());
            }
        };
        scheduler.scheduleAtFixedRate(timer, 0, 1, TimeUnit.SECONDS);

        // Now block

        LOGGER.info("Exiting!");
    }

    private void renewSession()  {
        LOGGER.info("Renewing session...");
        DeviceService deviceService = nucleusClient.getDeviceService();
        try {
            deviceService.renewSession(new DeviceSessionResponseHandler() {
                @Override
                public void onSuccess(boolean needsProfile, List<String> activeMemberships) {
                    LOGGER.info("Successfully renewed device session.");
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
        LOGGER.info("Starting up weather station...");
        Driver driver = new Driver();
        driver.execute();
    }

    private Properties loadProperties() {
        Properties props = null;
        InputStream is;

        // First try loading from the current directory
        try {
            File f = new File("local.properties");
            is = new FileInputStream(f);
        } catch (Exception e) {
            is = null;
        }

        try {
            if (is == null) {
                is = getClass().getResourceAsStream("local.properties");
            }

            if (is != null) {
                props = new Properties();
                props.load(is);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return props;
    }


    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private NucleusClient nucleusClient;

    private int SERVER_PORT = 8080;
    private String SERVER_URL = "localhost";

    private String apiAccountToken = "cKqg0k8kdALI+vGpjLFNAbYXfNQc7w6/oPITUYnkzWLrUIYF41s+RRAvHk18yBG47lucnX2CVSl0";
    private String apiAccountID = "a46b024b8_8024_35b2_982e_5e1958f71f2c";

    private String apiKey = "a8dffd93d_9f7d_3b25_a01b_e9ac15167253";
    private String apiToken = "0544cb10-4caa-4418-b890-1396522a8a2c";

    private NucleusLocation myLocation = new NucleusLocation(37.555446, -122.279257);

    private String screenName = "A Weathercape";
    private String shortDescription = "Intersection 23";
    private String longDescription = "176 Edgewater Boulevard, Foster City, CA";
    private String channelName = "TestExampleChannel";

    private String imageData = null;
    private String imageType = null;

    private String deviceType = "traffic-sensor";
    private String manufacturer = "Siemens";
    private String serialNumber = "1023x32309sfd45345deadbeef";
    private String modelIdentifier = "X253";
    private String os = "RTOS";
    private String osVersion = "4.3.3";
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
        LOGGER.severe("Handling request error. Command = " + command +
                      ", operationStatus = " + operationStatus.toString() +
                      ", statusCode = " + statusCode +
                      ", errorMessage = " + errorMessage);

        if ( OperationStatus.INVALID_DEVICE_TOKEN.equals(operationStatus) ) {
            renewSession();
        }
        else {
            LOGGER.severe("Handling request error. " + command + " (" + statusCode + ") " + errorMessage);
        }
    }

    @Override
    public void handleException(Throwable throwable) {

    }

    @Override
    public void onError(int errorNo, String errorMessage) {
        LOGGER.severe("Handling error. errorNo = " + errorNo + ", errorMessage = " + errorMessage);
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

    }

    @Override
    public void onMessageRemoved(ChannelMessage channelMessage) {

    }

    @Override
    public void onMessageChange(ChannelMessage channelMessage) {

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

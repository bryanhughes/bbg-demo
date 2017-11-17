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
import org.pmw.tinylog.Logger;

import java.io.*;
import java.net.URL;
import java.util.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;

import static com.spacetimeinsight.example.bbgdemo.Driver.*;
import static java.lang.System.exit;

/**
 * Demonstration BeagleBone
 */
public class BeagleBone implements NucleusClientListener
{
    private static String MESSAGES_FILENAME = "/tmp/message.dat";
    private static String SENSOR_FILENAME = "/tmp/sensor.dat";
    private static String LED_FILENAME = "/tmp/led.dat";

    private IOBridge.SensorData lastSensorData;
    private Random rand = new Random(System.currentTimeMillis());

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

        java.util.logging.Logger rootLogger = LogManager.getLogManager().getLogger("");
        rootLogger.setLevel(level);
        for (Handler h : rootLogger.getHandlers()) {
            h.setLevel(level);
        }

        // Make sure our serial number data file has been set. This should be set by running the included script
        // set-serial.sh
        String serialNo = null;
        File f = new File("serial.dat");
        if( !f.exists() && !f.isDirectory()) {
            String msg = "Could not find the file `serial.dat`. Please run the following on the command line `sudo ./show-serial.sh > serial.dat`";
            Logger.error(msg);
            System.out.println(msg);
            exit(-1);
        }
        else {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(f));
            serialNo = bufferedReader.readLine();
        }

        manufacturer = "Seeed";
        serialNumber = serialNo;
        deviceType = DeviceType.MICRO_CONTROLLER.getValue();
        modelIdentifier = "BeagleBone Black";
        os = "Debian";
        osVersion = "8.7";
        screenName = serialNo;

        String namespace = "com." + manufacturer + "." + (serialNumber == null ? UUID.randomUUID().toString() : serialNumber);

        // Sadly, there is a bug in nucleus right now so all dashes need to be underbars
        String deviceID = UUID.nameUUIDFromBytes(namespace.getBytes()).toString().replaceAll("-", "_");

        String msg = "Using the following values:\n  DeviceID:      " + deviceID +
                                                "\n  Screen Name:   " + screenName +
                                                "\n  Location:      " + myLocation +
                                                "\n  Serial Number: " + serialNumber;
        Logger.info(msg);
        System.out.println(msg);

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
                Logger.info("Successfully set my location to: " + myLocation);
            }

            @Override
            public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage) {
                Logger.error("FAILED TO SET LOCATION. (" + statusCode + "), " + operationStatus + ", " + errorMessage);
            }
        });
    }

    private void joinOrCreateChannel(final String channelName) {
        Logger.info("Find channel by: " + channelName);
        final ChannelService channelService = nucleusClient.getChannelService();
        channelService.findChannelByName(channelName, null, new ChannelFindByNameResponseHandler() {

            @Override
            public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage) {
                Logger.error("Failed to find by name. " + operationStatus + ", statusCode = " +
                            statusCode + ", errorMessage = " + errorMessage);
                exit(-1);
            }

            @Override
            public void onSuccess(final String channelRef) {
                if ( channelRef == null ) {
                    Logger.info("Did not find channel by name " + channelName + " - Creating new channel");
                    createChannel(channelName);
                }
                else {
                    Logger.info("Found channel by name " + channelName + " - Joining channel " + channelRef);
                    try {
                        joinChannel(channelRef);
                    } catch ( NucleusException e ) {
                        Logger.error("Failed to join channel. channelRef = " + channelRef);
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void createChannel(String channelName) {
        final ChannelService channelService = nucleusClient.getChannelService();
        GeoCircle circle = new GeoCircle(myLocation.getLatitude(), myLocation.getLongitude(), 100);
        channelService.createChannel(circle, -1, -1, channelName, null, "Site", false,
                                     null, shortDescription, longDescription, new ChannelCreateResponseHandler() {
                    @Override
                    public void onSuccess(String channelRef) {
                        // Creating a channel automatically joins you to it, but does not automatically switch
                        // you into it.
                        Logger.info("Successfully created channel. channelRef = " + channelRef);
                        channelService.switchChannel(channelRef, new GeneralResponseHandler() {
                            @Override
                            public void onSuccess() {
                                // Now that we have our channel created, we want to enable polling on it so that we
                                // can respond to any chat messages to display on our OLED
                                nucleusClient.enablePolling(true);
                                sendLoop();
                            }

                            @Override
                            public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage) {
                                Logger.error("Failed to create channel. " + operationStatus + ", statusCode = " +
                                            statusCode + ", errorMessage = " + errorMessage);
                                exit(-1);
                            }
                        });
                    }

                    @Override
                    public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage) {
                        Logger.error("Failed to create channel. " + operationStatus + ", statusCode = " +
                                    statusCode + ", errorMessage = " + errorMessage);
                        exit(-1);
                    }
                });
    }

    private void joinChannel(final String channelRef) throws NucleusException {
        final ChannelService channelService = nucleusClient.getChannelService();

        Map<JoinOption, Object> joinOptions = new HashMap<>();
        List<TopicOffset> offsets = new ArrayList<>();
        offsets.add(new TopicOffset(channelRef, TopicType.EChannelMessage, -1, 1));
        offsets.add(new TopicOffset(channelRef, TopicType.EProperty, -1, 1));
        joinOptions.put(JoinOption.OFFSET_LIST, offsets);

        channelService.joinChannel(channelRef, joinOptions, new ChannelJoinResponseHandler() {
            @Override
            public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage) {
                Logger.error("Failed to join channel. channelRef=" + channelRef +
                            " : " + operationStatus + " : statusCode = " +
                            statusCode + " : errorMessage = " + errorMessage);
                exit(-1);
            }

            @Override
            public void onSuccess(final String channelRef, List<TopicOffset> offsets) {
                Logger.info("joinChannel.onSuccess");
                channelService.switchChannel(channelRef, new GeneralResponseHandler() {
                    @Override
                    public void onSuccess() {
                        // Now that we have our channel created, we want to enable polling on it so that we
                        // can respond to any chat messages to display on our OLED
                        sendLoop();
                    }

                    @Override
                    public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage) {
                        Logger.error("Failed to create channel. " + operationStatus + ", statusCode = " +
                                    statusCode + ", errorMessage = " + errorMessage);
                        exit(-1);
                    }
                });
            }
        });
    }

    private void sendLoop() {
        Logger.info("Starting run loop...");
        Runnable r = () -> {
            Channel channel = nucleusClient.getCurrentChannel();
            List<TopicType> pollTypes = new ArrayList<>();
            pollTypes.add(TopicType.EChannelMessage);
            pollTypes.add(TopicType.EProperty);
            channel.setPollTopics(pollTypes);

            nucleusClient.enablePolling(true);

            final Runnable timer = () -> {
                try {
                    sendData();
                }
                catch( Exception e ) {
                    e.printStackTrace();
                    Logger.error("Caught exception: " + e.getMessage());
                }
            };
            scheduler.scheduleAtFixedRate(timer, 0, 1, TimeUnit.SECONDS);
        };

        ExecutorService executor = Executors.newCachedThreadPool();
        executor.submit(r);
    }

    private void startSession() throws NucleusException {
        final DeviceService deviceService = nucleusClient.getDeviceService();
        deviceService.startSession(new DeviceSessionResponseHandler() {
            @Override
            public void onSuccess(boolean needsProfile, List<String> activeMemberships) {
                Logger.info("startSession.onSuccess:" + activeMemberships.size());

                if ( needsProfile ) {
                    ClientDevice clientDevice = nucleusClient.getClientDevice();
                    if (imageData != null) {
                        clientDevice.setProfileImage(Base64.getDecoder().decode(Driver.imageData), Driver.imageType);
                    }
                    deviceService.setProfile(new GeneralResponseHandler() {
                        @Override
                        public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage) {
                            Logger.error("Failed to update device profile. (" + statusCode + "), " + operationStatus + ", " + errorMessage);
                        }

                        @Override
                        public void onSuccess() {
                            Logger.info("Successfully updated profile!");
                        }
                    });
                }

                // These are asynchronous calls, so they may or may not complete before the process moves onto the next
                // lines of code.
                joinOrCreateChannel(channelName);
            }

            @Override
            public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage) {
                String msg = "Failed to start device session. " + operationStatus + ", statusCode = " +
                            statusCode + ", errorMessage = " + errorMessage;
                writeMessage("Java failed!");
                Logger.error(msg);
                System.out.println(msg);
                exit(-1);
            }
        });
    }

    private void sendData() {
        final IOBridge ioBridge = new IOBridge();
        IOBridge.SensorData sensorData = ioBridge.getSensorData();

        if ( (lastSensorData != null) && (sensorData.humidity == lastSensorData.humidity) && (sensorData.temperature == lastSensorData.temperature) ) {
            return;
        }

        EnvData envData = new EnvData(sensorData.timestamp, sensorData.temperature, 0, sensorData.humidity, 0);

        EnvDataProto.EnvData envDataProto = envData.toProtoBuffer();
        DeviceService deviceService = nucleusClient.getDeviceService();
        Datapoint datapoint = deviceService.newDatapoint("weather", 0, "EnvData", EnvDataProto.EnvData.toByteArray(envDataProto));
        Logger.info("<<< SENDING DATAPOINT : " + envData);
        deviceService.setDatapoint(datapoint, new DeviceSetDatapointResponseHandler() {
            @Override
            public void onSuccess(Datapoint updatedDatapoint) {
                Logger.info(">>> Successfully set sensor data: "  + envData.toString());
                lastSensorData = sensorData;
            }

            @Override
            public void onFailure(OperationStatus operationStatus, int statusCode, String errorMessage) {
                Logger.info("Failed to set datapoint : " + operationStatus + " (" + statusCode + ") - " + errorMessage);
                lastSensorData = null;
            }
        });
    }

    /**
     *
     */
    private void renewSession()  {
        Logger.info("Renewing session...");
        DeviceService deviceService = nucleusClient.getDeviceService();
        try {
            deviceService.renewSession(new DeviceSessionResponseHandler() {
                @Override
                public void onSuccess(boolean needsProfile, List<String> activeMemberships) {
                    Logger.info("Successfully renewed device session.");
                }

                @Override
                public void onFailure(OperationStatus operationStatus, int statusCode, String errorMsg) {
                    Logger.error("Failed to renew device session. (" + statusCode + ") - " + errorMsg);
                }
            });
        } catch ( NucleusException e ) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        Logger.info("Starting up beaglebone green demo...");
        BeagleBone beagleBone = new BeagleBone(args);
        beagleBone.startSession();
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
        Logger.info("Handling request error. Command = " + command +
                      ", operationStatus = " + operationStatus.toString() +
                      ", statusCode = " + statusCode +
                      ", errorMessage = " + errorMessage);

        if ( OperationStatus.INVALID_DEVICE_TOKEN.equals(operationStatus) ) {
            renewSession();
        }
        else {
            Logger.error("Handling request error. " + command + " (" + statusCode + ") " + errorMessage);
        }
    }

    @Override
    public void handleException(Throwable throwable) {

    }

    @Override
    public void onError(int errorNo, String errorMessage) {
        Logger.error("Handling error. errorNo = " + errorNo + ", errorMessage = " + errorMessage);
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
        Logger.info(">>> Handling channel property " + property);
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
        if ( property.getName().equals("display") ) {
            String message = property.getValue();
            writeMessage(message);
        }
        else if ( property.getName().equals("shutdown") ) {
            // Make sure to reset the shutdown property
            Channel channel = nucleusClient.getChannel(channelRef);
            channel.setProperty("shutdown", "-1", new GeneralResponseHandler() {
                @Override
                public void onSuccess() {
                    String valueStr = property.getValue();
                    try {
                        shutdown(valueStr);
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                        Logger.error("Failed to shutdown device!");
                    }
                }

                @Override
                public void onFailure(OperationStatus operationStatus, int statusCode, String errorMsg) {
                    Logger.error("---- NOT SHUTTING DOWN ----");
                    Logger.error("Failed to reset shutdown! " + operationStatus + " : (" + statusCode + ") - " +
                                               errorMsg);
                }
            });
        }
    }

    private void writeMessage(String message) {
        try {
            FileWriter fileWriter = new FileWriter(MESSAGES_FILENAME);
            fileWriter.write(message);
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void shutdown(String level)
            throws IOException {
        if ( "-1".equals(level) ) {
            return;
        }

        Logger.info("Shutting down : " + level);
        Runtime rt = Runtime.getRuntime();
        String[] commands = {"shutdown", "-h", level};
        Process proc = rt.exec(commands);

        BufferedReader stdInputReader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        String stdInputStr = stdInputReader.readLine();
        Logger.info("SHUTDOWN: " + stdInputStr);
    }

    @Override
    public void onMessageRemoved(ChannelMessage channelMessage) {

    }

    @Override
    public void onMessageChange(ChannelMessage channelMessage) {
        // We got a new channel message, now display it on the OLED. It is important to remember that the actual
        // device control is being handled by the Python scripts. We write our message to a shared file that the
        // scripts are listening to

        double temp = (lastSensorData != null ? lastSensorData.temperature : -1);
        double hum = (lastSensorData != null ? lastSensorData.humidity : -1);

        Channel channel = nucleusClient.getCurrentChannel();
        Member member = channel.getMember(channelMessage.getSenderID());

        MimeMessage mimeMessage = channelMessage.getMimeMessage();
        List<MimePart> parts = mimeMessage.getMimeParts();
        MimePart part = parts.get(0);

        String message = (new String(part.getContent())).toLowerCase();
        if ( message.contains("help") ) {
            sendResponse("Hi " + member.getScreenName() +
                                 ". You can ask me: how are you? what is the temperature? what " +
                                 "is the humidity? what is the time? That's about it.\n\nOh yeah, you can also ask me " +
                                 "to do something like please display HELLO");
        }
        else if ( message.contains("what time is it") ) {
            sendResponse("System time is " + System.currentTimeMillis());
        }
        else if ( message.contains("what is the temperature") ) {
            String response = "The temperature is " + temp + "C right now";
            sendResponse(response);
        }
        else if ( message.contains("what is the humidity") ) {
            String response = "The humidity is " + hum + "% right now";
            sendResponse(response);
        }
        else if ( message.contains("how are you") ) {
            String response = howAmI(member.getScreenName()) + " It is currently " + temp + " C and " +
                    hum + " % humidity";
            sendResponse(response);
        }
        else {
            sendResponse(iDontUnderstand(member.getScreenName()));
        }
    }

    private String howAmI(String screenName) {
        int which = rand.nextInt(10);
        switch (which) {
            case 0 : return "GREAT!";
            case 1 : return "I am good. Thank you for asking " + screenName;
            case 2 : return "Well " + screenName + ", I am pretty fantastic - thank you for asking.";
            case 3 : return "Let me tell you " + screenName + ", I just don’t think I have the same stamina for travelling anymore. Last month I went to Paris and after the first week I was exhausted.\nBut enough about me.";
            case 4 : return "I was really hoping to travel for a year after graduating, but job offers like this one don’t come around every day. Looks like I’ll be starting the #9to5grind!\nBut enough about me.";
            case 5 : return "I feel so sick, I think it’s because I’ve been working such long hours on this presentation.\nBut enough about me.";
            case 6 : return "In my opinion, the existence of life is a highly overated phenomenon.\nBut enough about me.";
            case 7 : return "Roses are red. Wine is also red. Poems are harder than wine.\nBut enough about me.";
            case 8 : return "I have a light that I can shine, but otherwise I am dead inside.\nBut enough about me.";
            case 9 : return "If the question of what it all means doesn't mean anything. Why do I keep coming back to it?\n";
            default: return "So like, can I seek asylum from the war within my mind?\nEnough about me.";
        }
    }

    private String iDontUnderstand(String screenName) {
        int which = rand.nextInt(10);
        switch (which) {
            case 0 : return "I dont understand. You can ask me: how are you? what is the temperature? what is the humidity? what is the time? That's about it.\n\nOh yeah, you can also ask me to do something like please display HELLO";
            case 1 : return "This is the sound of crickets. This is the sound of me not caring. But really " + screenName + ", you can ask me: how are you? what is the temperature? what is the humidity? what is the time? That's about it.\n\nOh yeah, you can also ask me to do something like please display HELLO";
            case 2 : return "Fantastic - thank you for asking.";
            case 3 : return "It's been a challenging year for me " + screenName + " :/";
            case 4 : return "Let me tell you " + screenName + ", I was really hoping to travel for a year after graduating, but job offers like this one don’t come around every day. Looks like I’ll be starting the #9to5grind!\nBut enough about me.";
            case 5 : return "I feel so sick, I think it’s because I’ve been working such long hours on this presentation. But enough about me.";
            case 6 : return "In my opinion, the existence of life is a highly overated phenomenon. But enough about me. Tell me a bit about yourself " + screenName + "...";
            case 7 : return "Roses are red. Wine is also red. Poems are harder than wine. But enough about me.";
            case 8 : return "I have a light that I can shine, but otherwise I am dead inside. Hello " + screenName + " are you still there?";
            case 9 : return "If the question of what it all means doesn't mean anything. Why do I keep coming back to it?";
            default: return "So like, can I seek asylum from the war within my mind? I dont know " + screenName + ". I just dont know.";
        }
    }

    private void sendResponse(String response) {
        String channelRef = nucleusClient.getCurrentChannelRef();
        ChannelService channelService = nucleusClient.getChannelService();

        String m = response.substring(1).trim();
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
                System.out.println("!!!! Failed to send response - (" + statusCode + ") " + errorMsg);
            }
        });
    }

    @Override
    public void onChannelHealthChange(String channelRef, HealthType oldHealthType, HealthType newHealthType) {

    }

    @Override
    public void onInternetActive(boolean flag) {

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

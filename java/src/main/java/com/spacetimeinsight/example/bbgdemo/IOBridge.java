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

import java.io.*;
import java.util.logging.Logger;

/**
 * Read current weather and return formatted message
 */
public class IOBridge {

    private static final String MESSAGE_FILENAME = "/tmp/message.dat";
    private static final String SENSOR_FILENAME = "/tmp/sensor.dat";
    private static final String LED_FILENAME = "/tmp/led.dat";

    private static final String LOG_TAG = IOBridge.class.getName();
    private static final Logger LOGGER = Logger.getLogger(LOG_TAG);

    public class SensorData {
        double humidity;
        double temperature;
        int timestamp;
    }

    SensorData getSensorData() {
        SensorData sd = new SensorData();
        String sensorData = readSensor(SENSOR_FILENAME);
        LOGGER.info("sensor data = " + sensorData);
        if ( sensorData != null ) {
            String[] parts = sensorData.split(",");
            if ( parts.length == 3 ) {
                sd.temperature = Double.parseDouble(parts[0]);
                sd.humidity = Double.parseDouble(parts[1]);
                sd.timestamp = Integer.parseInt(parts[2]);
            }
        }
        return sd;
    }

    void writeMessage(String message) throws IOException {
        FileWriter file = new FileWriter(MESSAGE_FILENAME);
        file.write(message);
        file.close();
    }

    void writeLED(int r, int g, int b) throws IOException {
        String message = r + "," + g + "," + b;
        FileWriter file = new FileWriter(LED_FILENAME);
        file.write(message);
        file.close();
    }

    private String readSensor(String fileName) {
        String buffer = null;
        BufferedReader br = null;

        try {
            br = new BufferedReader(new FileReader(fileName));

            String line;
            while ( (line = br.readLine()) != null ) {
                System.out.println(line);
                buffer = line;
            }
        }
        catch ( IOException exception ) {
            if ( !(exception instanceof java.io.FileNotFoundException) ) {
                exception.printStackTrace();
            }
        }
        finally {
            try {
                if ( br != null ) {
                    br.close();
                }
            }
            catch ( IOException exception ) {
                //empty
            }
        }

        LOGGER.fine("readSensor:" + buffer + ":" + fileName);

        return (buffer);
    }
}


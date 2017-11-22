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

import com.spacetimeinsight.nucleuslib.datamapped.NucleusLocation;

import java.io.*;
import java.util.logging.Logger;

/**
 * The actual software that drives the Beaglebone is written in Python. It writes the sensor data to a four files. This
 * class will read the files into the associated objects
 */
interface IOBridge {
    String LOG_TAG = IOBridge.class.getName();
    Logger LOGGER = Logger.getLogger(LOG_TAG);

    String MESSAGE_FILENAME = "/tmp/message.dat";
    String SENSOR_FILENAME = "/tmp/sensor.dat";
    String LED_FILENAME = "/tmp/led.dat";
    String GPS_FILENAME = "/tmp/gps.dat";

    class SensorData {
        double humidity;
        double temperature;
        int timestamp;

        SensorData() {
            String sensorData = readFile(SENSOR_FILENAME);
            LOGGER.info("sensor data = " + sensorData);
            if ( sensorData != null ) {
                String[] parts = sensorData.split(",");
                if ( parts.length == 3 ) {
                    this.temperature = Double.parseDouble(parts[0]);
                    this.humidity = Double.parseDouble(parts[1]);
                    this.timestamp = Integer.parseInt(parts[2]);
                }
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            SensorData that = (SensorData) o;

            return Double.compare(that.humidity, humidity) == 0 &&
                    Double.compare(that.temperature, temperature) == 0 &&
                    timestamp == that.timestamp;
        }

        @Override
        public int hashCode() {
            int result;
            long temp;
            temp = Double.doubleToLongBits(humidity);
            result = (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(temperature);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            result = 31 * result + timestamp;
            return result;
        }
    }

    class GPSData {
        double lat;
        double lng;
        int fix;
        int nsats;
        double timestamp;
        double alt;

        GPSData() {
            String gpsDataStr = readFile(GPS_FILENAME);
            LOGGER.info("gps data = " + gpsDataStr);
            if ( gpsDataStr != null ) {
                String[] parts = gpsDataStr.split(",");
                if ( parts.length == 6 ) {
                    this.timestamp = Double.parseDouble(parts[0]);
                    this.lat = Double.parseDouble(parts[1]);
                    this.lng = Double.parseDouble(parts[2]);
                    this.fix = Integer.parseInt(parts[3]);
                    this.nsats = Integer.parseInt(parts[4]);
                    this.alt = Double.parseDouble(parts[5]);
                }
            }
        }
        
        NucleusLocation getLocation() {
            return new NucleusLocation(lat,  lng, 0, 0, System.currentTimeMillis(), 0, 0, (int) alt,
                                       nsats, 0, 0, 0, null, null, null);

        }

        @SuppressWarnings("SimplifiableIfStatement")
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            GPSData gpsData = (GPSData) o;

            if (Double.compare(gpsData.lat, lat) != 0) {
                return false;
            }
            if (Double.compare(gpsData.lng, lng) != 0) {
                return false;
            }
            if (fix != gpsData.fix) {
                return false;
            }
            if (nsats != gpsData.nsats) {
                return false;
            }
            if (Double.compare(gpsData.timestamp, timestamp) != 0) {
                return false;
            }
            return Double.compare(gpsData.alt, alt) == 0;
        }

        @Override
        public int hashCode() {
            int result;
            long temp;
            temp = Double.doubleToLongBits(lat);
            result = (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(lng);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            result = 31 * result + fix;
            result = 31 * result + nsats;
            temp = Double.doubleToLongBits(timestamp);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(alt);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            return result;
        }
    }

    static void writeMessage(String message) throws IOException {
        FileWriter file = new FileWriter(MESSAGE_FILENAME);
        file.write(message);
        file.close();
    }

    static void writeLED(String values) throws IOException {
        FileWriter file = new FileWriter(LED_FILENAME);
        file.write(values);
        file.close();
    }

    static String readFile(String fileName) {
        String buffer = null;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(fileName));
            String line;
            while ( (line = br.readLine()) != null ) {
                LOGGER.finest("readLine=" + line);
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
                exception.printStackTrace();
            }
        }
        LOGGER.finest("readFile:" + buffer + ":" + fileName);
        return (buffer);
    }
}


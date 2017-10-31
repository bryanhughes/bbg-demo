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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.logging.Logger;

/**
 * Read current weather and return formatted message
 */
public class WeatherReader {

    /**
     * @return true if weather sensors exist
     */
    boolean fileTest() {
        File file = new File(HUMIDITY_FILE_NAME);
        return file.exists() && file.canRead();
    }

    Double getHumidity() {
        String temp = readSensor(HUMIDITY_FILE_NAME);
        if ( temp != null ) {
            int rawHumidity = Integer.parseInt(temp);
            return (rawHumidity / 1000.0);
        }

        return (0.0);
    }

    Integer getLux() {
        String temp = readSensor(LUX_FILE_NAME);
        if ( temp != null ) {
            return (Integer.parseInt(temp));
        }

        return (0);
    }

    Double getPressure() {
        String temp = readSensor(PRESSURE_FILE_NAME);
        if ( temp != null ) {
            int rawPressure = Integer.parseInt(temp);
            return (rawPressure / 1000.0);
        }

        return (0.0);
    }

    Double getTemperature() {
        String temp = readSensor(TEMPERATURE_FILE_NAME);
        if ( temp != null ) {
            int rawTemperature = Integer.parseInt(temp);
            return (rawTemperature / 10.0);
        }

        return (0.0);
    }

    String getMessage() {
        Date date = new Date(System.currentTimeMillis());
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);

        String message = "sample:" + df.format(date) + " " + getTemperature() + "C " + getHumidity() + "% " + getPressure() + "mB " + getLux() + " lux";
        LOGGER.fine(message);
        return (message);
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
            exception.printStackTrace();
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

    public static void main(String args[]) {
        LOGGER.entering(LOG_TAG, "main");

        WeatherReader weatherReader = new WeatherReader();
        String message = weatherReader.getMessage();
        System.out.println(message);

        LOGGER.exiting(LOG_TAG, "main");
    }


    //
    private static final String BASE_FILE_NAME = "/sys/bus/i2c/devices";
    private static final String HUMIDITY_FILE_NAME = BASE_FILE_NAME + "/1-0040/humidity1_input";
    private static final String LUX_FILE_NAME = BASE_FILE_NAME + "/1-0039/lux1_input";
    private static final String PRESSURE_FILE_NAME = BASE_FILE_NAME + "/1-0077/pressure0_input";
    private static final String TEMPERATURE_FILE_NAME = BASE_FILE_NAME + "/1-0077/temp0_input";

    //
    private static final String LOG_TAG = WeatherReader.class.getName();
    private static final Logger LOGGER = Logger.getLogger(LOG_TAG);
}

/*
 * Copyright 2013 Go Factory LLC
 * Created on July 23, 2013 by gsc
 */

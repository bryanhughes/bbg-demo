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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * enable/disable LED
 */
public class LightControl {

    /**
     * @param flag true, LED on else off
     */
    void setLed(boolean flag) {
        LOGGER.entering(LOG_TAG, "setLed");

        File file = new File(LED_FILE_NAME);
        FileWriter fw = null;

        if ( file.exists() && file.canWrite() ) {
            try {
                fw = new FileWriter(file);

                if ( flag ) {
                    fw.write('1');
                }
                else {
                    fw.write('0');
                }
            }
            catch ( IOException exception ) {
                exception.printStackTrace();
            }
            finally {
                try {
                    if ( fw != null ) {
                        fw.close();
                    }
                }
                catch ( IOException exception ) {
                    //empty
                }
            }
        }
        else {
            LOGGER.info("missing LED file");
        }

        LOGGER.exiting(LOG_TAG, "setLed");
    }

   
    public static void main(String args[]) {
        LOGGER.entering(LOG_TAG, "main");

        LightControl lightControl = new LightControl();
        lightControl.setLed(true);

        try {
            Thread.sleep(2000L);
        }
        catch ( Exception exception ) {
            //empty
        }

        lightControl.setLed(false);

        LOGGER.exiting(LOG_TAG, "main");
    }

    //
    private static final String BASE_FILE_NAME = "/sys/class/gpio";
    private static final String LED_FILE_NAME = BASE_FILE_NAME + "/gpio60/value";

    //
    private static final String LOG_TAG = LightControl.class.getName();
    private static final Logger LOGGER = Logger.getLogger(LOG_TAG);
}

/*
 * Copyright 2013 Go Factory LLC
 * Created on July 23, 2013 by gsc
 */

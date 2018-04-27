#!/usr/bin/python
#
import logging

import datetime
import time

import grove_dht
import grove_gps
import grove_accel
import grove_led
import oled_display
import iot_displaythread
import iot_ledthread

THRESHOLD_TEMPERATURE = 22.0
SENSOR_FILENAME = "/tmp/sensor.dat"
GPS_FILENAME = "/tmp/gps.dat"


def main():
    fmt = '%(asctime)-15s %(message)s'
    logging.basicConfig(format=fmt, level=logging.INFO)
    logger = logging.getLogger('iot_demo')

    logger.info("[iot_demo] ------------ STARTING UP ------------")

    rgb_led = grove_led.ChainableLED(grove_led.CLK_PIN, grove_led.DATA_PIN, grove_led.NUMBER_OF_LEDS)
    rgb_led.setColorRGB(0, 255, 255, 255)

    for x in range(0, 3):
        rgb_led.setColorRGB(0, 255, 0, 0)
        rgb_led.setColorRGB(0, 0, 255, 0)
        rgb_led.setColorRGB(0, 0, 0, 255)
        time.sleep(.5)

    time.sleep(1)

    logger.info("[iot_demo] Starting display thread")
    display = oled_display.OLEDDisplay()
    iot_displaythread.run(display)

    logger.info("[iot_demo] Starting led thread")
    iot_ledthread.run()

    humidity0 = 0
    temperature0 = 0
    axes0 = {}
    lat0 = 0.0
    lng0 = 0.0
    sats0 = 0
    counter = 0
    total_time = 0

    gps = grove_gps.GPS()
    adxl345 = grove_accel.ADXL345()

    while True:
        # First, attempt to read from our GPS
        stime = datetime.datetime.now()
        try:
            gps.read()  # Read from GPS
            [t, fix, sats1, alt, lat1, lat_ns, lng1, lng_ew] = gps.vals()  # Get the individual values

            if lat_ns == "N":
                lat = gps.decimal_degrees(float(lat1))
            else:
                lat = -gps.decimal_degrees(float(lat1))

            if lng_ew == "W":
                lng = -gps.decimal_degrees(float(lng1))
            else:
                lng = gps.decimal_degrees(float(lng1))

            logger.info("[iot_demo] lat >>>" + str(int(float(lat1))))
            logger.info("[iot_demo] lng >>>" + str(int(float(lng1))))

            if (lat0 != lat1) and (lng0 != lng1) and (sats0 != sats1):
                gps_str = t + "," + str(lat) + "," + str(lng) + "," + fix + "," + sats1 + "," + alt
                gps_file = open(GPS_FILENAME, "w")
                gps_file.write(gps_str)
                gps_file.close()
                logger.info("[iot_demo] GPS = %s", gps_str)

                lat0 = lat1
                lng0 = lng1
                sats0 = sats1
        except IndexError:
            logger.error("[iot_demo] Unable to read GPS")
        except Exception as e:
            logger.error("[iot_demo] Failed to read GPS - %s", e.message)

        # Write out the env_data to a single file....

        humidity1, temperature1 = grove_dht.read()
        axes1 = adxl345.get_axes(True)

        # If nothing is different, then dont report

        change_flag = False
        if (humidity0 != humidity1) or (temperature0 != temperature1) or (axes0 != axes1):
            # We will open the file and overwrite it on every write...
            sensor_file = open(SENSOR_FILENAME, "w")
            sensor_file.write('{0:0d},{1:0.1f},{2:0.1f},{3:0d},{4:0.4f},{5:0.4f},{6:0.4f}'.format(int(counter),
                                                                                                  temperature1,
                                                                                                  humidity1,
                                                                                                  int(time.time()),
                                                                                                  axes1['x'],
                                                                                                  axes1['y'],
                                                                                                  axes1['z']))
            sensor_file.close()
            change_flag = True

        counter = counter + 1
        humidity0 = humidity1
        temperature0 = temperature1
        axes0 = axes1

        display.setValues(temperature0, humidity0, lat0, lng0)

        delta = datetime.datetime.now() - stime
        run_time = int(delta.total_seconds() * 1000)
        total_time = total_time + run_time
        avg_time = total_time / counter

        if change_flag:
            logger.info("[iot_demo] {0:d} : Temp={1:0.1f}*, Humidity={2:0.1f}%, "
                        "Accel={3:0.4f}, {4:0.4f}, {5:0.4f} - Took {6:0.3f}ms / Avg {7:0.3f}ms".format(counter,
                                                                                                       temperature1,
                                                                                                       humidity1,
                                                                                                       axes1['x'],
                                                                                                       axes1['y'],
                                                                                                       axes1['z'],
                                                                                                       run_time,
                                                                                                       avg_time))
        else:
            logger.info("[iot_demo] {0:d} : No change - Took {1:0.3f} ms / Avg {2:0.3f}ms".format(counter,
                                                                                                  run_time, avg_time))


if __name__ == "__main__":
    main()

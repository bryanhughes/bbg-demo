import time
import os
import traceback

import grove_dht
import grove_oled
import grove_led
import grove_gps

THRESHOLD_TEMPERATURE = 22.0
MESSAGE_FILENAME = "/tmp/message.dat"
LED_FILENAME = "/tmp/led.dat"
SENSOR_FILENAME = "/tmp/sensor.dat"
GPS_FILENAME = "/tmp/gps.dat"

if __name__=="__main__":
    rgb_led = grove_led.ChainableLED(grove_led.CLK_PIN, grove_led.DATA_PIN, grove_led.NUMBER_OF_LEDS)
    rgb_led.setColorRGB(0, 255, 255, 255)

    print('Starting up...')

    for x in range(0, 3):
        rgb_led.setColorRGB(0, 255, 0, 0)
        rgb_led.setColorRGB(0, 0, 255, 0)
        rgb_led.setColorRGB(0, 0, 0, 255)
        time.sleep(.5)

    time.sleep(1)

    grove_oled.oled_init()
    grove_oled.oled_setNormalDisplay()
    grove_oled.oled_clearDisplay()

    msgCountDown = 0
    ledCountDown = 0
    clear = 1

    humidity0 = 0
    temperature0 = 0

    lat0 = 0.0
    lng0 = 0.0
    sats0 = 0
    c = '+'

    lat = 0.0
    lng = 0.0
    latY = 0
    lngY = 0

    g = grove_gps.GPS()

    while True:
        # First, attempt to read from our GPS
        try:
            x = g.read()  # Read from GPS
            [t, fix, sats1, alt, lat1, lat_ns, lng1, lng_ew] = g.vals()  # Get the individual values

            if lat_ns == "N":
                lat = g.decimal_degrees(float(lat1))
                # HACK! Can not seem to get format() to work properly on the OLED display
                if int(float(lat1)) <= 9999:
                    latY = 2
                else:
                    latY = 1                
            else:
                lat = -g.decimal_degrees(float(lat1))
                if (int(float(lat1)) <= 9999):
                    latY = 1

            if lng_ew == "W":
                lng = -g.decimal_degrees(float(lng1))
                if int(float(lng1)) <= 9999:
                    lngY = 1
            else:
                lng = g.decimal_degrees(float(lng1))
                if int(float(lng1)) <= 9999:
                    lngY = 2
                else:
                    lngY = 1

            print "lat >>>" + str(int(float(lat1)))
            print "lng >>>" + str(int(float(lng1)))

            if ( lat0 != lat1) and (lng0 != lng1) and (sats0 != sats1):
                gps_str = t + "," + str(lat) + "," + str(lng) + "," + fix + "," + sats1 + "," + alt
                gps_file = open(GPS_FILENAME, "w")
                gps_file.write(gps_str)
                gps_file.close()
                print('GPS = ' + gps_str)

                lat0 = lat1
                lng0 = lng1
                sats0 = sats1
        except IndexError:
            print "Unable to read GPS"
        except Exception as e:
            print "Failed to read GPS - " + e.message

        humidity1, temperature1 = grove_dht.read()

        if (humidity0 != humidity1) or (temperature0 != temperature1):
            # We will open the file and overwrite it on every write...
            sensor_file = open(SENSOR_FILENAME, "w")
            sensor_file.write('{0:0.1f},{1:0.1f},{2:0d}'.format(temperature1, humidity1, int(time.time())))
            sensor_file.close()
            print('Temp={0:0.1f}*  Humidity={1:0.1f}%'.format(temperature1, humidity1))

        humidity0 = humidity1
        temperature0 = temperature1

        # We are going to look to a file for any message to display on our OLED
        if msgCountDown == 0:
            try:
                message_file = open(MESSAGE_FILENAME, "r")
            except IOError:
                if clear == 1:
                    grove_oled.oled_clearDisplay()
                    clear = 0

                grove_oled.oled_setTextXY(0, 0)
                grove_oled.oled_putString('Temp:{0:0.1f}C'.format(temperature1))
                grove_oled.oled_setTextXY(1, 0)
                grove_oled.oled_putString('Hum: {0:0.1f}%'.format(humidity1))
                grove_oled.oled_setTextXY(2, 0)
                grove_oled.oled_putString("            ")
                grove_oled.oled_setTextXY(3, latY)
                grove_oled.oled_putString('{:>3.6f}'.format(lat))
                grove_oled.oled_setTextXY(4, lngY)
                grove_oled.oled_putString('{:>3.6f}'.format(lng))
            else:
                message = message_file.readline()
                print('message = ' + message)
                message_file.close()
                os.remove(MESSAGE_FILENAME)

                grove_oled.oled_clearDisplay()
                time.sleep(2)

                grove_oled.oled_setTextXY(0, 0)
                grove_oled.oled_putString("            ")
                grove_oled.oled_setTextXY(0, 0)
                grove_oled.oled_putString(message)
                grove_oled.oled_setTextXY(1, 0)
                grove_oled.oled_putString("            ")
                grove_oled.oled_setTextXY(2, 0)
                grove_oled.oled_putString("            ")
                grove_oled.oled_setTextXY(3, 0)
                grove_oled.oled_putString("            ")
                grove_oled.oled_setTextXY(4, 0)
                grove_oled.oled_putString("            ")
                msgCountDown = 10
                clear = 1
        else:
            msgCountDown = msgCountDown - 1
            if msgCountDown < 0:
                msgCountDown = 0

        if c == '+':
            c = '-'
        else:
            c = '+'
        grove_oled.oled_setTextXY(6, 0)
        grove_oled.oled_putString("SpaceTime")
        grove_oled.oled_setTextXY(6, 10)
        grove_oled.oled_putString(c)

        # The same for what to do with our LED
        try:
            led_file = open(LED_FILENAME, "r")
            led_values = led_file.readline()
            led_file.close()
            os.remove(LED_FILENAME)
            try:
                parts = [x.strip() for x in led_values.split(',')]
                print "led_values = [{}, {}, {}] - len = {}".format(int(parts[0]), int(parts[1]), int(parts[2]), len(parts))
                rgb_led.setColorRGB(0, int(parts[0]), int(parts[1]), int(parts[2]))
            except:
                print('An error occured setting LED RGB.')
        except:
            pass

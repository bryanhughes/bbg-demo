import time
import os
import grove_dht
import grove_oled
import grove_led

THRESHOLD_TEMPERATURE = 22.0
MESSAGE_FILENAME = "/tmp/message.dat"
LED_FILENAME = "/tmp/led.dat"
SENSOR_FILENAME = "/tmp/sensor.dat"

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

    countdown = 0
    clear = 1

    while True:
        humidity, temperature = grove_dht.read()

        # We will open the file and overwrite it on every write...
        sensor_file = open(SENSOR_FILENAME, "w")
        sensor_file.write('{0:0.1f},{1:0.1f},{2:0d}'.format(temperature, humidity, int(time.time())))
        sensor_file.close()

        # We are going to look to a file for any message to display on our OLED
        try:
            message_file = open(MESSAGE_FILENAME, "r")
        except IOError:
            if (countdown == 0):
                if ( clear == 1 ):
                    grove_oled.oled_clearDisplay()
                    clear = 0

                grove_oled.oled_setTextXY(0, 0)
                grove_oled.oled_putString('Temp:{0:0.1f}C'.format(temperature))
                grove_oled.oled_setTextXY(1, 0)
                grove_oled.oled_putString('Hum: {0:0.1f}%'.format(humidity))
                grove_oled.oled_setTextXY(5, 0)
                grove_oled.oled_putString("SpaceTime")
            else:
                countdown = countdown - 1
        else:
            message = message_file.readline()
            print('message = ' + message)
            message_file.close()
            os.remove(MESSAGE_FILENAME)

            grove_oled.oled_clearDisplay()
            grove_oled.oled_setTextXY(0, 0)
            grove_oled.oled_putString(message)
            grove_oled.oled_setTextXY(1, 0)
            grove_oled.oled_putString("           ")
            countdown = 10
            clear = 1

        # The same for what to do with our LED
        try:
            led_file = open(LED_FILENAME, "r")
        except IOError:
            rgb_led.setColorRGB(0, 0, 0, 0)
        else:
            led_values = led_file.readline(1)
            parts = [x.strip() for x in led_values.split(',')]
            if ( len(parts) == 3 ):
                rgb_led.setColorRGB(0, parts[0], parts[1], parts[2])

        print('Temp={0:0.1f}*  Humidity={1:0.1f}%'.format(temperature, humidity))

        time.sleep(1)

import time
import grove_dht
import grove_oled
import grove_led

THRESHOLD_TEMPERATURE = 22.0
FILENAME = "output.dat"

if __name__=="__main__":
    rgb_led = grove_led.ChainableLED(grove_led.CLK_PIN, grove_led.DATA_PIN, grove_led.NUMBER_OF_LEDS)
    rgb_led.setColorRGB(0, 255, 255, 255)

    print('Starting up...')

    for x in range(0, 5):
        rgb_led.setColorRGB(0, 255, 0, 0)
        rgb_led.setColorRGB(0, 0, 255, 0)
        rgb_led.setColorRGB(0, 0, 0, 255)

    time.sleep(1)

    grove_oled.oled_init()
    grove_oled.oled_setNormalDisplay()
    grove_oled.oled_clearDisplay()

    while True:
        humidity, temperature = grove_dht.read()

        if temperature >= THRESHOLD_TEMPERATURE :
            rgb_led.setColorRGB(0, 255, 0, 0)
        else:
            rgb_led.setColorRGB(0, 0, 0, 255)

        # We will open the file and overwrite it on every write...
        file = open(FILENAME, "w")
        file.write('{0:0.1f},{1:0.1f},{2:0d}'.format(temperature, humidity, time.time()))

        time.sleep(2)

        grove_oled.oled_setTextXY(0,0)
        grove_oled.oled_putString('Temp:{0:0.1f}C'.format(temperature))
        grove_oled.oled_setTextXY(1,0)
        grove_oled.oled_putString('Hum: {0:0.1f}%'.format(humidity))
        grove_oled.oled_setTextXY(5,0)
        grove_oled.oled_putString("SpaceTime")

        print('Temp={0:0.1f}*  Humidity={1:0.1f}%'.format(temperature, humidity))

        time.sleep(1)

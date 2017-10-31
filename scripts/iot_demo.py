import time
import grove_dht
import grove_oled
import grove_led

THRESHOLD_TEMPERATURE = 22

if __name__=="__main__":
    grove_oled.oled_init()
    grove_oled.oled_setNormalDisplay()
    grove_oled.oled_clearDisplay()

    rgb_led = grove_led.ChainableLED(grove_led.CLK_PIN, grove_led.DATA_PIN, grove_led.NUMBER_OF_LEDS)

    while True:
        # GPIO.output(led,GPIO.HIGH)
        # time.sleep(1)
        # GPIO.output(led,GPIO.LOW)
        # time.sleep(1)
        # print 'led'
        humidity, temperature = grove_dht.read()
        
        grove_oled.oled_setTextXY(0,0)
        grove_oled.oled_putString('Temp:{0:0.1f} C'.format(temperature))
        grove_oled.oled_setTextXY(1,0)
        grove_oled.oled_putString('Humidity={0:0.1f} %'.format(humidity))
        grove_oled.oled_setTextXY(5,0)
        grove_oled.oled_putString("SpaceTime")

        print('Temp={0:0.1f}*  Humidity={1:0.1f}%'.format(temperature, humidity))

        if temperature >= THRESHOLD_TEMPERATURE :
            rgb_led.setColorRGB(0, 255, 0, 0)
        else:
            rgb_led.setColorRGB(0, 0, 0, 255)
        time.sleep(1)
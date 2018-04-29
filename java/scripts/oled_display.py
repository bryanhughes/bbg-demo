#!/usr/bin/python
#
import logging
import os
import time
import grove_oled

MESSAGE_FILENAME = "/tmp/message.dat"


class OLEDDisplay:
    def __init__(self):
        fmt = '%(asctime)-15s %(message)s'
        logging.basicConfig(format=fmt, level=logging.INFO)
        self.logger = logging.getLogger('iot_display')
        self.logger.info("[iot_display] Initializing display")

        grove_oled.oled_init()
        grove_oled.oled_setNormalDisplay()
        grove_oled.oled_clearDisplay()

        self.c = '+'
        self.temp = 0.0
        self.humidity = 0.0
        self.lat = 0.0
        self.lng = 0.0

    def set_temphumidity(self, temp, humidity):
        self.temp = temp
        self.humidity = humidity

    def set_latlng(self, lat, lng):
        self.lat = lat
        self.lng = lng

    def display(self):
        try:
            message_file = open(MESSAGE_FILENAME, "r")
        except IOError:
            self.display_values()
        else:
            message = message_file.readline()
            self.logger.info("[iot_display] message = %s", message)
            message_file.close()
            os.remove(MESSAGE_FILENAME)
            self.display_message(message)
            time.sleep(10)

    def display_values(self):


        self.logger.info("[iot_display] lat = %s, lng = %s", self.lat, self.lng)

        grove_oled.oled_setTextXY(0, 0)
        grove_oled.oled_putString('Temp:{0:0.1f}C'.format(self.temp))
        grove_oled.oled_setTextXY(1, 0)
        grove_oled.oled_putString('Hum: {0:0.1f}%'.format(self.humidity))
        grove_oled.oled_setTextXY(2, 0)
        grove_oled.oled_putString("            ")
        grove_oled.oled_setTextXY(3, 0)
        grove_oled.oled_putString('%11.6f' % self.lat)
        grove_oled.oled_setTextXY(4, 0)
        grove_oled.oled_putString('%11.6f' % self.lng)
        self.pulse()

    def display_message(self, message):
        grove_oled.oled_clearDisplay()
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
        self.pulse()

    def pulse(self):
        if self.c == '+':
            self.c = '-'
        else:
            self.c = '+'
        grove_oled.oled_setTextXY(6, 0)
        grove_oled.oled_putString("SpaceTime")
        grove_oled.oled_setTextXY(6, 10)
        grove_oled.oled_putString(self.c)

#!/usr/bin/python
#
import logging
import os
import traceback

import time

import grove_led
import thread

LED_FILENAME = "/tmp/led.dat"

def led_loop():
    rgb_led = grove_led.ChainableLED(grove_led.CLK_PIN, grove_led.DATA_PIN, grove_led.NUMBER_OF_LEDS)
    rgb_led.setColorRGB(0, 255, 255, 255)

    logger = logging.getLogger('iot_ledthread')
    while True:
        try:
            led_file = open(LED_FILENAME, "r")
        except KeyboardInterrupt:
            logger.info("[iot_ledthread] Caught keyboard interrupt. Bye!")
            exit()
        except IOError:
            continue
        except Exception as e:
            logger.error("[iot_ledthread] Caught exception - %s", e.message)
            traceback.print_exc()
        else:
            led_values = led_file.readline()
            led_file.close()
            os.remove(LED_FILENAME)
            time.sleep(2)

            parts = [x.strip() for x in led_values.split(',')]
            logger.info("[iot_ledthread] led_values = [{}, {}, {}] - len = {}".format(int(parts[0]),
                                                                                      int(parts[1]),
                                                                                      int(parts[2]),
                                                                                      len(parts)))
            rgb_led.setColorRGB(0, int(parts[0]), int(parts[1]), int(parts[2]))


def run():
    thread.start_new_thread(led_loop, ())

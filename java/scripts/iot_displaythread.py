#!/usr/bin/python
#
import logging
import traceback

import thread


def display_loop(display):
    logger = logging.getLogger('iot_displaythread')
    while True:
        try:
            display.display()
        except KeyboardInterrupt:
            logger.info("[iot_displaythread] Caught keyboard interrupt. Bye!")
            exit()
        except Exception as e:
            logger.error("[iot_displaythread] Caught exception - %s", e.message)
            traceback.print_exc()


def run(display):
    thread.start_new_thread(display_loop, (display,))

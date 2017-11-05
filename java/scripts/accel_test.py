#!/usr/bin/python
#
#
import time
import pyupm_adxl345 as adxl345


adxl = adxl345.Adxl345(1)

print('Printing X, Y, Z axis values, press Ctrl-C to quit...')
while True:
    # Read the X, Y, Z axis acceleration values and print them.
    adxl.update()
    raw = adxl.getRawValues()

    print('X={0}, Y={1}, Z={2}'.format(raw[0], raw[1], raw[2]))
    # Wait half a second and repeat.
    time.sleep(0.5)


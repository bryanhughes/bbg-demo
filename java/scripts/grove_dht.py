#!/usr/bin/python
#
#
import time
import Adafruit_DHT

def read():
    pin = 2
    sensor = Adafruit_DHT.DHT22

    humidity2, temperature2 = Adafruit_DHT.read_retry(sensor, pin)

    if humidity2 is None or temperature2 is None:
        humidity = 0
        temperature = 0
    else:
        humidity = humidity2
        temperature = temperature2

    return (humidity, temperature)

if __name__ == '__main__':
    while True:
        humidity, temperature = read()
        print('Temp={0:0.1f}*  Humidity={1:0.1f}%'.format(temperature, humidity))

        # Wait half a second and repeat.
        time.sleep(0.5)
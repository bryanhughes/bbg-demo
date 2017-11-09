import time
import pyupm_grove as grove
import mraa

# Create the temperature sensor object using AIO pin 0
def TemperatureRead():
    temp = grove.GroveTemp(1)
    return temp

if __name__ == "__main__":
    while True:
        print(TemperatureRead())
        time.sleep(1)
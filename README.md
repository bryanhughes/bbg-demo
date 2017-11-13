## Beaglebone Green Demo

<br>
NOTE: This README assumes you are familiar with the Beaglebone Green Wireless.

This project demonstrates how easy it is to build a Beaglebone Green IoT device and connect to the SpaceTime IoT Warp platform. This code has been tested with a Beaglebone Green Wireless, with a Grove Chainable RBG LED and Grove Temperature Sensor.

If you are starting from scratch, please follow the instructions for [Getting Started](http://beagleboard.org/getting-started). Once you have your Beaglebone connected and accessible, go ahead and SSH onto the device.

This project is composed of several Python scripts, Java components, and an Android app. The Python scripts do the actual
controlling of the Beaglebone sensors. The main script is `iot_demo.py`. This needs to be run as `sudo` in the background.
The Java components include the java application that implements the Nucleus SDK that provides connectivity and reads sensor data
and writes out control changes. Their also a Java Console application that allows you to send messages and LED changes
to the Beaglebone. You can run this from any computer. The Android application is a very simple mobile application that allows
you to control the Beaglebone.

#### What Exactly Can You Do?
This project demonstrates how to stream telemetry data, temperature and humidity, from the Beaglebone Green to the SpaceTime IoT Warp Platform via the
SDK. It also demonstrates how to send back messages to the device to control it. Their are two elements on the Beaglebone that
you can control, the LED light and the message displayed on the OLED screen. When there is no specific message being displayed,
or after 5 minutes of displaying the custom message, the device switches back to display the temperature and humidity.


### Connecting the Hardware
The parts to build this project include:

1) [Beaglebone Green Wireless Development Board](https://www.seeedstudio.com/Beaglebone-Green-Wireless-Development-Board%EF%BC%88TI-AM335x-WiFi%2BBT%EF%BC%89-p-2650.html)
2) [Grove Base Cape for Beaglebone v2.0](https://www.seeedstudio.com/Grove-Base-Cape-for-Beaglebone-v2.0-p-2644.html)
3) [Temperature & Humidity Sensor Pro](https://www.seeedstudio.com/Grove-Temperature%26Humidity-Sensor-Pro%EF%BC%88AM2302%EF%BC%89-p-838.html)
5) [Seeedstudio Grove Starter Kit for Beaglebone Green](https://www.amazon.com/gp/product/B018FNOJUK/ref=oh_aui_detailpage_o07_s00?ie=UTF8&psc=1)

![image_2](docs/images/image_2.jpg) 
The Starter Kit

![image_1](docs/images/image_1.jpg) 
The OLED Display and LED Chainable Sensor are part of the Starter Kit

![assembled_off_top_1](docs/images/assembled_off_top_1.jpg)
The assembled device


#### Logging on using screen
When you connect the Beaglebone to your laptop using the USB, you have two options of connecting. The first is to use screen. 
The second is to use SSH. Sometimes the usb network connection does not always get set up correctly, so the fallback
is always the `screen` command. In either case, to confirm that the Beaglebone is mounted to your laptop, you should
find the tty in dev.

```
$ ls /dev/tty*usb*
```
Sometimes the device will show up as a usbmodem, othertimes it will show up as as usbserial. Here is an example of logging
in using screen where the device is identified as `tty.usbmodem1425`

```
$ screen /dev/tty.usbmodem1425 115200
```

#### Logging on using ssh
As the documentation explains, the mounted device should set up a USB network between the laptop and the device. The
address of the device will be 192.168.7.2.

#### Configuring the Wifi
There are instructions for configuring the wifi on the Beaglebone by selecting the AP and then configuring the
correct SID and passphrase. This does not always work. If it doesnt, the you will need to configure it manually
by logging onto the device and type the following

	$ connmanctl (invoke utility)
	connmanctl> tether wifi off (disable tethering)
	connmanctl> enable wifi (enable wifi radio)
	connmanctl> scan wifi (scan for AP, might take a few seconds)
	connmanctl> services (display detected AP)
	connmanctl> agent on (enable connection agent)
	connmanctl> connect wifi_*_managed_psk (connect to selected AP, might take some time, will prompt for password)
	connmanctl> quit
	
Test your connection...

    $ ping yahoo.com	

### Update the Software
When you're in, update your software:

```
apt-get update
apt-get upgrade
```
Now update your kernel (you should be running Debian by default) and reboot:

```
cd /opt/scripts/tools/
./update_kernel.sh
reboot
```

Your SSH connection should drop, but you can pull it back up as soon as the Beaglebone finishes rebooting.

> Note: These commands should all work if you are SSHed in as the root user. If you're working as the debian user, you may need to add "sudo" to the front of the commands. The default password for the Beaglebone is "temppwd".

Next, you will need to have Java 8 JDK [installed on your BBG](http://beagleboard.org/project/java/). 


## Building your Project
Everything you need to build, or rebuild the project, is included in this project. The project uses Gradle and includes a `build.gradle`. If you are using Eclipse or IntelliJ, simply import the project from sources.

The tasks to build the project are: 

	./gradle clean build release

After you have built the project, navigate to the `release` directory to find the `bbg-demo-1.0.zip` file. You will need to
secure copy this file to your Beaglebone Green. It is recommended that you make a bbgdemo directory on the Beaglebone Green
under the home directory.

    $ scp release/bbg-demo-1.0.zip debian@<ip-addresss>:bbgdemo

Back on the device, unzip the file. You will see the executable JAR and supporting libraries.

Next, edit the local.properties file and set the api_accountid, api_accounttoken, api_key, and api_token to the values 
of your account and partition from the nucleus application console. For the out of the box demo, simply copy the 
`local-template.properties` file to `local.properties`. This will have the necessary credentials and tokens to connect
to the SpaceTime demo server.

Also, if you want your device to be at a specific location in the world, be sure to edit the `local.properties` file and 
set the latitude and longitude of your station, along with its name, and a site name.

To find you location, simply open [https://www.google.com/maps](https://www.google.com/maps) and find your location on the map. Zoom in to get a precise location (or enter the address). Click and hold for a moment and you will get the latitude and longitude coordinates of that location. 

You will need two terminal windows open. In the first window, start up the Python scripts that will drive the sensors 

	$ sudo python iot_demo.py
	
In the second window, start up that Java application

    $ sudo ./run_bbg
    
Your Beaglebone Green should now be reporting the temperature and humidity of your location and should be displaying it on
the OLED screen. Please note that the LED flashes on startup. It does take a minute to startup.

![assembled_off_top_1](docs/images/assembled_on_top.jpg)
The assembled device running

To control the LED or send a message, back on your laptop, you can either unzip the `bbg-demo-1.0.zip` in the release directory, 
or move it to another temp directory. In it, you will find another script called `bbg_console.sh`. If you run this, you can
now send messages to your Beaglebone.

Finally, if you have an Android development phone and environment available, build the Android APK and deploy it onto your
phone. This modest app will demonstrate temperature and humidity sensor data ingestion as well as how to control your 
Beaglebone with your phone. You can change the LED values of the light (to turn it off give 0,0,0) as well as push a message
to the OLED display.

![assembled_off_top_1](docs/images/device_with_app_1.jpg)
The assembled device running






 
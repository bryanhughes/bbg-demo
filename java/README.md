## BeagleBone Green Demo

<br>
NOTE: This README assumes you are familiar with the BeagleBone Green Wireless.

This project demonstrates how easy it is to build a BeagleBone Green IoT device and connect to the SpaceTime IoT Warp platform. This code has been tested with a BeagleBone Green Wireless, with a Grove Chainable RBG LED and Grove Temperature Sensor.

If you are starting from scratch, please follow the instructions for [Getting Started](http://beagleboard.org/getting-started). Once you have your BeagleBone connected and accessible, go ahead and SSH onto the device.

###Connecting the Hardware

####Logging on using screen
When you connect the BeagleBone to your laptop using the USB, you have two options of connecting. The first is to use screen. 
The second is to use SSH. Sometimes the usb network connection does not always get set up correctly, so the fallback
is always the `screen` command. In either case, to confirm that the BeagleBone is mounted to your laptop, you should
find the tty in dev.

```
$ ls /dev/tty*usb*
```
Sometimes the device will show up as a usbmodem, othertimes it will show up as as usbserial. Here is an example of logging
in using screen where the device is identified as `tty.usbmodem1425`

```
$ screen /dev/tty.usbmodem1425 115200
```

####Logging on using ssh
As the documentation explains, the mounted device should set up a USB network between the laptop and the device. The
address of the device will be 192.168.7.2.

####Configuring the Wifi
There are instructions for configuring the wifi on the BeagleBone by selecting the AP and then configuring the
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

###Update the Software
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

Everything you need to build, or rebuild the project, is included in this project. The project uses Gradle and includes a `build.gradle`. If you are using Eclipse or IntelliJ, simply import the project from sources.

The tasks to build the project are: 

	./gradle clean build release

After you have built the project, navigate to the `release` directory to find the weatherstation-1.0.zip file.

Move this to a desired directory where you will unzip to. You will see the executable JAR and supporting libraries.

Next, edit the local.properties file and set the api_accountid, api_accounttoken, api_key, and api_token to the values of your account and partition from the nucleus application console.

Also, set the latitude and longitude of your station, along with its name, and a site name.

To find you location, simply open [https://www.google.com/maps](https://www.google.com/maps) and find your location on the map. Zoom in to get a precise location (or enter the address). Click and hold for a moment and you will get the latitude and longitude coordinates of that location. 

Finally, type 

	./run_bbg.sh





 
#!/bin/bash

echo "--- Installing BBG IoT Demo ---"
if [ ! -w /etc/init.d ]; then
  echo "You need to run this script as sudo."
  exit -1
fi

echo "Writing device serial to serial.dat"
./show-serial.sh > serial.dat

echo ""
echo "Copying local.properties. If you are not using the examples_app partition"
echo "in the SpaceTime demo server, you will need to change this."

cp local-template.properties local.properties

echo ""
echo "Installing rc.local so relay and bridge start on boot..."
cp rc.local /etc

echo ""
echo "Installing crontab to keep logs from filling up device..."
echo sudo "0 0 * * * root bash /home/debian/bbgdemo/remove_old_logs.sh" > /etc/cron.d/bbgdemo.sh

echo ""
echo "Done"



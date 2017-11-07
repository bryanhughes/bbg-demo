#!/usr/bin/env bash
python logo.py
echo ""
echo "BeagleBone Green IoT Demo"
echo "----------------------------------------------------------------"
echo "NOTE: This must be run as sudo"
echo ""
echo "Starting up python scripts..."
nohup python iot_demo.py >/dev/null 2>&1 &

sleep 5s

echo "Starting up java app..."
nohup ./run_bbg_java.sh >/dev/null 2>&1 &
echo "Done (both apps are running in the background with nohup)"
echo ""
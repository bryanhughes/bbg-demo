#!/usr/bin/env bash

sudo rm /var/log/daemon.log.?.gz &>> /var/log/bbg_cron.out
sudo rm /var/log/syslog.?.gz &>> /var/log/bbg_cron.out
sudo rm /var/log/bbg_python.log.? &>> /var/log/bbg_cron.out
sudo rm /var/log/bbg_java.log.? &>> /var/log/bbg_cron.out
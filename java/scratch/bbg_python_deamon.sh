#!/bin/bash
# chkconfig: 345 99 05
# description: Java deamon script
#
# A non-SUSE Linux start/stop script for python daemons.
#
# Derived from -
# Home page: http://www.source-code.biz
# License:   GNU/LGPL (http://www.gnu.org/licenses/lgpl.html)
# Copyright 2006 Christian d'Heureuse, Inventec Informatik AG, Switzerland.
#
# History:
# 2009-03-04 Josh Davis: Ubuntu/Redhat version.
# 2006-06-27 Christian d'Heureuse: Script created.
# 2006-07-02 chdh: Minor improvements.
# 2006-07-10 chdh: Changes for SUSE 10.0.

applDir="/home/debian/bbgdemo"                             # home directory of the service application
serviceNameLo="bbg_python"                                 # service name with the first letter in lowercase
serviceUser="root"                                         # This must be run as root
serviceUserHome="$applDir"                                 # home directory of the service user
serviceGroup="root"                                        # OS group name for the service
serviceLogFile="/var/log/$serviceNameLo.log"               # log file for StdOut/StdErr
maxShutdownTime=15                                         # maximum number of seconds to wait for the daemon to terminate normally
pidFile="/var/run/$serviceNameLo.pid"                      # name of PID file (PID = process ID number)
cmdCommandLine="python iot_demo.py"                        # command line to start the Java service application
cmdCommandLineKeyword="BBGDemoPython"                      # a keyword that occurs on the commandline, used to detect an already running service process and to distinguish it from others
rcFileBaseName="rc$serviceNameLo"                          # basename of the "rc" symlink file for this script
rcFileName="/usr/local/sbin/$rcFileBaseName"               # full path of the "rc" symlink file for this script
etcInitDFile="/etc/init.d/$serviceNameLo"                  # symlink to this script from /etc/init.d

# Makes the file $1 writable by the group $serviceGroup.
function makeFileWritable {
   local filename="$1"
   touch $filename || return 1
   chgrp $serviceGroup $filename || return 1
   chmod g+w $filename || return 1
   return 0; }

# Returns 0 if the process with PID $1 is running.
function checkProcessIsRunning {
   local pid="$1"
   if [ -z "$pid" -o "$pid" == " " ]; then return 1; fi
   if [ ! -e /proc/$pid ]; then return 1; fi
   return 0; }

# Returns 0 if the process with PID $1 is our Python service process.
function checkProcessIsOurService {
   local pid="$1"
   local cmd="$(ps -p $pid --no-headers -o comm)"
   if [ "$cmd" != "python" -a "$cmd" != "python.bin" ]; then return 1; fi
   grep -q --binary -F "$cmdCommandLineKeyword" /proc/$pid/cmdline
   if [ $? -ne 0 ]; then return 1; fi
   return 0; }

# Returns 0 when the service is running and sets the variable $servicePid to the PID.
function getServicePid {
   if [ ! -f $pidFile ]; then return 1; fi
   servicePid="$(<$pidFile)"
   checkProcessIsRunning $servicePid || return 1
   checkProcessIsOurService $servicePid || return 1
   return 0; }

function startServiceProcess {
   cd $applDir || return 1
   rm -f $pidFile
   makeFileWritable $pidFile || return 1
   makeFileWritable $serviceLogFile || return 1
   cmd="nohup $cmdCommandLine >>$serviceLogFile 2>&1 & echo \$! >$pidFile"
   # Don't forget to add -H so the HOME environment variable will be set correctly.
   sudo -u $serviceUser -H $SHELL -c "$cmd" || return 1
   sleep 0.1
   pid="$(<$pidFile)"
   if checkProcessIsRunning $pid; then :; else
      echo -ne "\n$serviceNameLo start failed, see logfile."
      return 1
   fi
   return 0; }

function stopServiceProcess {
   kill $servicePid || return 1
   for ((i=0; i<maxShutdownTime*10; i++)); do
      checkProcessIsRunning $servicePid
      if [ $? -ne 0 ]; then
         rm -f $pidFile
         return 0
         fi
      sleep 0.1
      done
   echo -e "\n$serviceNameLo did not terminate within $maxShutdownTime seconds, sending SIGKILL..."
   kill -s KILL $servicePid || return 1
   local killWaitTime=15
   for ((i=0; i<killWaitTime*10; i++)); do
      checkProcessIsRunning $servicePid
      if [ $? -ne 0 ]; then
         rm -f $pidFile
         return 0
         fi
      sleep 0.1
      done
   echo "Error: $serviceNameLo could not be stopped within $maxShutdownTime+$killWaitTime seconds!"
   return 1; }

function runInConsoleMode {
   getServicePid
   if [ $? -eq 0 ]; then echo "$serviceNameLo is already running"; return 1; fi
   cd $applDir || return 1
   sudo -u $serviceUser $cmdCommandLine || return 1
   return 0; }

function startService {
   getServicePid
   if [ $? -eq 0 ]; then echo -n "$serviceNameLo is already running"; RETVAL=0; return 0; fi
   echo -n "Starting $serviceNameLo   "
   startServiceProcess
   echo "started PID=$servicePid"
   if [ $? -ne 0 ]; then RETVAL=1; echo "failed"; return 1; fi
   RETVAL=0
   return 0; }

function stopService {
   getServicePid
   if [ $? -ne 0 ]; then echo -n "$serviceNameLo is not running"; RETVAL=0; echo ""; return 0; fi
   echo -n "Stopping $serviceNameLo   "
   stopServiceProcess
   if [ $? -ne 0 ]; then RETVAL=1; echo "failed"; return 1; fi
   echo "stopped PID=$servicePid"
   RETVAL=0
   return 0; }

function checkServiceStatus {
   echo -n "Checking for $serviceNameLo:   "
   if getServicePid; then
	echo "running PID=$servicePid"
	RETVAL=0
   else
	echo "stopped"
	RETVAL=3
   fi
   return 0; }


function main {
   RETVAL=0
   case "$1" in
      start)                                               # starts the Groovy program as a Linux service
         startService
         ;;
      stop)                                                # stops the Groovy program service
         stopService
         ;;
      restart)                                             # stops and restarts the service
         stopService && startService
         ;;
      status)                                              # displays the service status
         checkServiceStatus
         ;;
      *)
         echo "Usage: $0 {start|stop|restart|status}"
         exit 1
         ;;
      esac
   exit $RETVAL
}

main $1
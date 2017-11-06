#!/bin/sh
# SET THIS ACCORDING TO YOUR ENVIRONMENT
PATH=$JAVA_HOME/bin:$PATH; export PATH
#
java -cp bbg-demo-1.0.jar:nucleus-java-sdk-2.0.3.jar com.spacetimeinsight.example.bbgdemo.Launcher console -level severe
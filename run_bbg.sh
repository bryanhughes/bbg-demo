#!/bin/sh
# SET THIS ACCORDING TO YOUR ENVIRONMENT
PATH=$JAVA_HOME/bin:$PATH; export PATH
#
echo "run bbg-demo script"
#
java -cp bbg-demo-1.0.jar:nucleus-java-sdk-2.0.3.jar com.spacetimeinsight.example.bbg-demo.Driver
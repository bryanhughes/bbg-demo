package com.spacetimeinsight.example.bbgdemo;

import com.spacetimeinsight.nucleus.java.NucleusClient;
import com.spacetimeinsight.nucleuslib.datamapped.NucleusLocation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

class Driver {
    static NucleusClient nucleusClient;
    static int SERVER_PORT;
    static String SERVER_URL;
    static String apiAccountToken;
    static String apiAccountID;
    static String apiKey;
    static String apiToken;
    static NucleusLocation myLocation;
    static String shortDescription;
    static String longDescription;
    static String channelName;

    static String screenName;
    static String imageData;
    static String imageType;
    static String deviceType;
    static String manufacturer;
    static String serialNumber;
    static String modelIdentifier;
    static String os;
    static String osVersion;

    static {
        Properties localProperties = loadProperties();
        if ( localProperties == null ) {
            System.out.println("Please set the local.properties with the following:\n");
            System.out.println("apiAccountID=<API Account ID>\n" +
                                       "apiAccountToken=<API Account Token>\n" +
                                       "apiKey=<partition key>\n" +
                                       "apiToken=<partition token>\n" +
                                       "channelName=<channel name>\n" +
                                       "lat=<latitude of device>\n" +
                                       "lng=<longitude of device>\n" +
                                       "shortDescription=<short description>\n" +
                                       "longDescription=<long description>\n" +
                                       "deviceType=<device type>\n" +
                                       "manufacturer=<manufacturer>\n" +
                                       "serialNumber=<serial number>\n" +
                                       "modelIdentifier=<model identifier>\n" +
                                       "os=<OS of device>\n" +
                                       "osVersion=<version of device os>\n"
            );
            System.exit(-1);
        }

        Driver.apiAccountID = localProperties.getProperty("apiAccountID");
        Driver.apiAccountToken = localProperties.getProperty("apiAccountToken");
        Driver.apiKey = localProperties.getProperty("apiKey");
        Driver.apiToken = localProperties.getProperty("apiToken");
        Driver.channelName = localProperties.getProperty("channelName");

        double lat = Double.valueOf(localProperties.getProperty("lat"));
        double lng = Double.valueOf(localProperties.getProperty("lng"));

        Driver.myLocation = new NucleusLocation(lat, lng);

        Driver.screenName = localProperties.getProperty("screenName");
        if ( Driver.screenName == null ) {
            Driver.screenName = "A Weathercape";
        }

        Driver.shortDescription = localProperties.getProperty("shortDescription");
        Driver.longDescription = localProperties.getProperty("longDescription");

        Driver.SERVER_URL = localProperties.getProperty("serverHost");
        Driver.SERVER_PORT = Integer.valueOf(localProperties.getProperty("serverPort"));

        Driver.imageData = localProperties.getProperty("ProfileImage");
        Driver.imageType = localProperties.getProperty("ImageType");
    }

    private static Properties loadProperties() {
        Properties props = null;
        InputStream is;

        // First try loading from the current directory
        try {
            File f = new File("local.properties");
            is = new FileInputStream(f);
        } catch (Exception e) {
            is = null;
        }

        try {
            if (is == null) {
                is = ClassLoader.getSystemClassLoader().getResourceAsStream("local.properties");
            }

            if (is != null) {
                props = new Properties();
                props.load(is);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return props;
    }
}

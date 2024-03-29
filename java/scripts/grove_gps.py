import serial, time
import sys

# Must load BB-UART4 dtbo - see http://www.erdahl.io/2016/08/talking-serial-over-beaglebone-green.html
ser = serial.Serial('/dev/ttyO4', 9600, timeout=0)  # Open the serial port at 9600 baud
ser.flush()


class GPS():
    # The GPS module used is a Grove GPS module http://www.seeedstudio.com/depot/Grove-GPS-p-959.html
    inp = []
    # Refer to SIM28 NMEA spec file http://www.seeedstudio.com/wiki/images/a/a0/SIM28_DATA_File.zip
    GGA = []

    # Read data from the GPS
    def read(self):
        while True:
            GPS.inp = ser.readline()
            # print ("GPS In: ", GPS.inp)
            if GPS.inp[:6] == '$GPGGA':  # GGA data , packet 1, has all the data we need
                break
            time.sleep(0.1)
        try:
            ind = GPS.inp.index('$GPGGA', 5, len(
                GPS.inp))  # Sometimes multiple GPS data packets come into the stream. Take the data only after the last '$GPGGA' is seen
            GPS.inp = GPS.inp[ind:]
        except ValueError:
            pass
        GPS.GGA = GPS.inp.split(",")  # Split the stream into individual parts
        return [GPS.GGA]

    # Split the data into individual elements
    def vals(self):
        time = GPS.GGA[1]
        lat = GPS.GGA[2]
        lat_ns = GPS.GGA[3]
        long = GPS.GGA[4]
        long_ew = GPS.GGA[5]
        fix = GPS.GGA[6]
        sats = GPS.GGA[7]
        alt = GPS.GGA[9]
        return [time, fix, sats, alt, lat, lat_ns, long, long_ew]

    def decimal_degrees(self, raw_degrees):
        degrees = float(raw_degrees) // 100
        d = float(raw_degrees) % 100 / 60
        return degrees + d


if __name__ == '__main__':
    g = GPS()
    # f = open("gps_data.csv", 'w')  # Open file to log the data
    # f.write("name,latitude,longitude\n")  # Write the header to the top of the file
    ind = 0
    while True:
        try:
            x = g.read()  # Read from GPS
            [t, fix, sats, alt, lat, lat_ns, long, long_ew] = g.vals()  # Get the individial values
            print "Time:", t, "Fix status:", fix, "Sats in view:", sats, "Altitude", alt, "Lat:", lat, lat_ns, "Long:", long, long_ew
            if lat:
                s = str(t) + "," + str(float(lat) / 100) + "," + str(float(long) / 100) + "\n"
                # f.write(s)  # Save to file
                time.sleep(2)
        except IndexError:
            print "Unable to read"
        except KeyboardInterrupt:
            # f.close()
            print "Exiting"
            sys.exit(0)

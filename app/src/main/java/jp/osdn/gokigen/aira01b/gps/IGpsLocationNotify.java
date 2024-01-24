package jp.osdn.gokigen.aira01b.gps;

import android.location.Location;

/**
 *   GPSの位置情報を通知する
 *
 */
public interface IGpsLocationNotify
{
    void gpsLocationUpdate(long timestamp, Location location, String nmeaLocation);
    void gpsLocationFixed();
}

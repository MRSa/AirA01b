package jp.osdn.gokigen.aira01b.gps;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import androidx.core.content.ContextCompat;
import jp.osdn.gokigen.aira01b.olycamerawrapper.IOlyCameraProperty;
import jp.osdn.gokigen.aira01b.olycamerawrapper.IOlyCameraPropertyProvider;

/**
 *
 *
 */
public class GpsLocationPicker implements IGpsLocationPicker, LocationListener, GpsStatus.NmeaListener
{
    private final String TAG = toString();
    private final Context context;
    private final IGpsLocationNotify notifier;
    private boolean isAvailableGps = false;
    private boolean isTrackingGps = false;
    private boolean isFixedGps = false;
    private final LocationManager locationManager;
    private IOlyCameraPropertyProvider propertyProvider = null;
    private Location currentLocation = null;
    private String lastGPGGAstring = null;
    private String lastGPRMCstring = null;

    private static final long MINIMUM_INTERVAL = 3 * 1000;   // 3 * 1000 msec = 3sec
    private static final float MINIMUM_DISTANCE = 30.0f;  // 30m

    /**
     *    コンストラクタ
     *
     */
    public GpsLocationPicker(Context context, IGpsLocationNotify target)
    {
        this.context = context;
        notifier = target;
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    /**
     *   GPS機能使用のための準備
     *
     * @return true : 使用可能 / false : 使用不可
     */
    @Override
    public boolean prepare(IOlyCameraPropertyProvider propertyProvider)
    {
        if (locationManager == null)
        {
            Log.v(TAG, "GPS is not Available");
            isAvailableGps = false;
            return (false);
        }
        this.propertyProvider = propertyProvider;
        isAvailableGps = true;
        return (true);
    }

    /**
     *   GPSトラッキングの開始、終了を指示する
     *
     * @param isStart  : true 開始 / false 終了
     */
    public void controlGps(final boolean isStart)
    {
        if (locationManager == null)
        {
            Log.v(TAG, "GPS is not Available");
            isAvailableGps = false;
            return;
        }
        if ((ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED))
        {
            Log.v(TAG, "GPS is not Available (Permission)");
            isAvailableGps = false;
            return;
        }

        if (isStart)
        {
            // GPSトラッキングの開始 (トラッキング直後は未確定）
            try
            {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MINIMUM_INTERVAL, MINIMUM_DISTANCE, this);
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MINIMUM_INTERVAL, MINIMUM_DISTANCE, this);
                locationManager.addNmeaListener(this);
                currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                isTrackingGps = true;
                isFixedGps = false;

                // 位置情報記録ON
                if (propertyProvider != null)
                {
                    propertyProvider.setCameraPropertyValue(IOlyCameraProperty.GPS, IOlyCameraProperty.GPS_PROPERTY_ON);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            // GPSトラッキングの終了
            locationManager.removeNmeaListener(this);
            locationManager.removeUpdates(this);
            isTrackingGps = false;
            isFixedGps = false;

            // 位置情報の記録OFF
            if (propertyProvider != null)
            {
                propertyProvider.setCameraPropertyValue(IOlyCameraProperty.GPS, IOlyCameraProperty.GPS_PROPERTY_OFF);
            }

            // GPS情報の非表示化
            notifier.gpsLocationUpdate(0, null, "");
        }
    }

    /**
     *   GPS機能が使用可能か？
     *
     * @return  true : 使用可能 / false : 使用不可
     */
    @Override
    public boolean hasGps()
    {
        return (isAvailableGps);
    }

    /**
     *   位置をトラッキングしている状態か？
     *
     * @return  true : トラッキング中 / false : 停止中
     */
    @Override
    public boolean isTracking()
    {
        return (isTrackingGps);
    }


    /**
     *   位置情報が確定しているか？
     *
     * @return  true : 確定 / false : 未確定
     */
    @Override
    public boolean isFixedLocation()
    {
        return (isFixedGps);
    }


    /**
     *   インタフェース LocationListener の実装
     *
     */
    @Override
    public void onLocationChanged(Location location)
    {
        Log.v(TAG,"GpsLocationPicker::onLocationChanged() ");

        // GPSの位置情報をクリアする
        lastGPGGAstring = null;
        lastGPRMCstring = null;
        currentLocation = location;
        isFixedGps = true;
        notifier.gpsLocationFixed();
    }

    /**
     *   インタフェース LocationListener の実装
     *
     */
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras)
    {
        Log.v(TAG,"GpsLocationPicker::onStatusChanged() : " + provider + " (" + status + ")");
    }

    /**
     *   インタフェース LocationListener の実装
     *
     */
    @Override
    public void onProviderEnabled(String provider)
    {
        Log.v(TAG, "GpsLocationPicker::onProviderEnabled() : " + provider);
    }

    /**
     *   インタフェース LocationListener の実装
     *
     */
    @Override
    public void onProviderDisabled(String provider)
    {
        Log.v(TAG, "GpsLocationPicker::onProviderDisabled() : " + provider);
    }

    /**
     *   GPS情報を受信した！
     *
     */
    @Override
    public void onNmeaReceived(long timestamp, String nmea)
    {
       if (!isFixedGps)
        {
            // まだ位置情報がFixしていない。
            //Log.v(TAG,"GpsLocationPicker::onNmeaReceived() : not fixed location");
            return;
        }

        // 位置情報が確定していたときは、そのデータを送る
        //Log.v(TAG,"GpsLocationPicker::onNmeaReceived() " + nmea);
        if (nmea.contains("$GPRMC"))
        {
            lastGPRMCstring = nmea;
        }
        if (nmea.contains("$GPGGA"))
        {
            lastGPGGAstring = nmea;
        }
        if ((lastGPRMCstring != null)&&(lastGPGGAstring != null))
        {
            notifier.gpsLocationUpdate(timestamp, currentLocation, (lastGPGGAstring + lastGPRMCstring));
        }
    }
}

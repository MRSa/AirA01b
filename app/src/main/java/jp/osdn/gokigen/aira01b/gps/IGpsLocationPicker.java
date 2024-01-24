package jp.osdn.gokigen.aira01b.gps;

import jp.osdn.gokigen.aira01b.olycamerawrapper.IOlyCameraPropertyProvider;

/**
 *
 *
 */
public interface IGpsLocationPicker
{
    boolean prepare(IOlyCameraPropertyProvider propertyProvider);
    void controlGps(boolean isStart);

    boolean hasGps();
    boolean isTracking();
    boolean isFixedLocation();

}

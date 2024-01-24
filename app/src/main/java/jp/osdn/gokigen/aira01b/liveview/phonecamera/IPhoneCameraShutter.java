package jp.osdn.gokigen.aira01b.liveview.phonecamera;

import android.location.Location;

public interface IPhoneCameraShutter
{
    void setCurrentLocation(Location location);
    void onPressedPhoneShutter(boolean withGeolocation);
    void onTouchedPreviewArea();
    void onSavedPicture(boolean isSuccess);
}

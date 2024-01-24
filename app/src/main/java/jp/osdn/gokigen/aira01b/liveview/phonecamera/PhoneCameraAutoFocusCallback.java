package jp.osdn.gokigen.aira01b.liveview.phonecamera;

import android.hardware.Camera;
import android.util.Log;

public class PhoneCameraAutoFocusCallback implements Camera.AutoFocusCallback
{
    private final String TAG = toString();

    PhoneCameraAutoFocusCallback()
    {

    }

    @Override
    public void onAutoFocus(boolean b, Camera camera)
    {
        Log.v(TAG, "PhoneCameraAutoFocusCallback::onAutoFocus() : " + b);
    }
}

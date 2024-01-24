package jp.osdn.gokigen.aira01b.liveview.phonecamera;

import android.hardware.Camera;

public interface IPhoneCameraDataReceiver
{
    void onPreviewFrame(byte[] arg0, Camera arg1);
}

package jp.osdn.gokigen.aira01b.olycamerawrapper;

import android.util.Log;
import jp.co.olympus.camerakit.OLYCamera;
import jp.co.olympus.camerakit.OLYCameraKitException;
import jp.osdn.gokigen.aira01b.preference.ICameraPropertyAccessor;

/**
 *   Olympusカメラとの切断処理
 *
 */
public class CameraDisconnectSequence implements Runnable
{
    private final String TAG = this.toString();

    private final OLYCamera camera;
    private final ICameraPropertyAccessor propertyAccessor;
    private final boolean powerOff;

    /**
     *   コンストラクタ
     *
     */
    CameraDisconnectSequence(OLYCamera camera, ICameraPropertyAccessor propertyAccessor, boolean isOff)
    {
        this.camera = camera;
        this.propertyAccessor = propertyAccessor;
        this.powerOff = isOff;
    }

    @Override
    public void run()
    {
        // 現在のカメラの設定値を記憶する
        propertyAccessor.storeCameraSettings(null);

        // カメラをPowerOffして接続を切る
        try
        {
            camera.disconnectWithPowerOff(powerOff);
        }
        catch (OLYCameraKitException e)
        {
            // エラー情報をログに出力する
            Log.w(TAG, "To disconnect from the camera is failed. : " + e.getLocalizedMessage());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}

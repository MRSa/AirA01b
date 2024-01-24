package jp.osdn.gokigen.aira01b.olycamerawrapper;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Map;
import java.util.TreeSet;

import androidx.preference.PreferenceManager;
import jp.co.olympus.camerakit.OLYCamera;
import jp.co.olympus.camerakit.OLYCameraStatusListener;
import jp.osdn.gokigen.aira01b.liveview.ICameraStatusDisplay;
import jp.osdn.gokigen.aira01b.R;
import jp.osdn.gokigen.aira01b.preference.ICameraPropertyAccessor;

/**
 *   OLYCameraStatusListenerの実装
 *   (LiveViewFragment用)
 */
public class CameraStatusListenerImpl implements OLYCameraStatusListener, ICameraStatusSummary
{
    private final String TAG = this.toString();

    public static final String APERTURE_VALUE = "ActualApertureValue";
    public static final String SHUTTER_SPEED = "ActualShutterSpeed";
    public static final String EXPOSURE_COMPENSATION = "ActualExposureCompensation";
    public static final String ISO_SENSITIVITY = "ActualIsoSensitivity";
    public static final String RECORDABLEIMAGES = "RemainingRecordableImages";
    public static final String MEDIA_BUSY = "MediaBusy";
    public static final String MEDIA_ERROR = "MediaError";
    public static final String DETECT_FACES = "DetectedHumanFaces";
    public static final String FOCAL_LENGTH = "ActualFocalLength";
    public static final String ACTUAL_ISO_SENSITIVITY_WARNING = "ActualIsoSensitivityWarning";
    public static final String EXPOSURE_WARNING = "ExposureWarning";
    public static final String EXPOSURE_METERING_WARNING = "ExposureMeteringWarning";
    public static final String HIGH_TEMPERATURE_WARNING = "HighTemperatureWarning";
    public static final String LEVEL_GAUGE = "LevelGauge";
    public static final String LENS_MOUNT_STATUS = "LensMountStatus";
    public static final String MEDIA_MOUNT_STATUS = "MediaMountStatus";
    public static final String REMAINING_RECORDABLE_TIME = "RemainingRecordableTime";
    public static final String MINIMUM_FOCAL_LENGTH = "MinimumFocalLength";
    public static final String MAXIMUM_FOCAL_LENGTH = "MaximumFocalLength";

    // レベルゲージ（デジタル水準器）の情報
    public static final String ORIENTATION_LANDSCAPE = "landscape";
    public static final String ORIENTATION_LANDSCAPE_UPSIDE_DOWN = "landscape_upside_down";
    public static final String ORIENTATION_PORTRAIT_LEFT = "portrait_left";
    public static final String ORIENTATION_PORTRAIT_RIGHT = "portrait_right";
    public static final String ORIENTATION_FACEUP = "faceup";
    public static final String ORIENTATION_FACEDOWN = "facedown";


    private OLYCameraStatusListener delegateListener = null;
    private final ICameraStatusDisplay display;
    private final Context context;

    private final float GAUGE_SENSITIVITY = 0.5f;
    private float prevRoll = 0.0f;
    private float prevPitch = 0.0f;
    private String prevOrientation = "";
    private boolean isCheckLevelGauge = false;

    /**
     *   コンストラクタ
     *
     */
    public CameraStatusListenerImpl(Context context, ICameraStatusDisplay parent)
    {
        this.context = context;
        this.display = parent;

        updateLevelGaugeChecking();
    }

    /**
     *
     *
     * @param delegateListener delegateListener
     */
    public void setDelegateListener(OLYCameraStatusListener delegateListener)
    {
        display.updateCameraStatus(" ");
        this.delegateListener = delegateListener;
    }

    /**
     *   レベルゲージのチェック状態を更新する
     *
     */
    public void updateLevelGaugeChecking()
    {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (preferences != null)
        {
            isCheckLevelGauge = preferences.getBoolean(ICameraPropertyAccessor.LEVEL_GAUGE, false);
        }
        if (!isCheckLevelGauge)
        {
            // レベルゲージを消す
            display.updateLevelGauge("", Float.NaN, Float.NaN);
        }
    }

    @Override
    public void onUpdateStatus(final OLYCamera camera, final String name)
    {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                if (name == null)
                {
                    // name がないとき、何もしない
                    return;
                }
                boolean isShow = true;
                switch (name)
                {
                    case APERTURE_VALUE:
                    case SHUTTER_SPEED:
                    case ISO_SENSITIVITY:
                    case FOCAL_LENGTH:
                    case EXPOSURE_WARNING:
                    case EXPOSURE_METERING_WARNING:
                    case HIGH_TEMPERATURE_WARNING:
                        // ある一定の値が更新された時だけ
                        break;

                    case LEVEL_GAUGE:
                        if (isCheckLevelGauge)
                        {
                            // 水準器の確認
                            checkLevelGauge(camera);
                        }
                        break;

                    default:
                        // 他の値が変わった場合には、ログだけ残して何もしない。
                        Log.v(TAG, "onUpdateStatus() : " + name);
                        isShow = false;
                        break;
                }
                if (delegateListener != null)
                {
                    delegateListener.onUpdateStatus(camera, name);
                }
                else if (isShow)
                {
                    // カメラのステータスを表示する
                    display.updateCameraStatus(geCameraStatusMessage(camera, name));
                }
            }
        });
        try
        {
            thread.start();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     *   レベルゲージの情報確認
     *
     *
     */
    private void checkLevelGauge(OLYCamera camera)
    {
        try
        {
            Map<String, Object> levelGauge = camera.getLevelGauge();
            float roll = (float) levelGauge.get(OLYCamera.LEVEL_GAUGE_ROLLING_KEY);
            float pitch = (float) levelGauge.get(OLYCamera.LEVEL_GAUGE_PITCHING_KEY);
            float sensitivity = GAUGE_SENSITIVITY;
            String orientation = (String) levelGauge.get(OLYCamera.LEVEL_GAUGE_ORIENTATION_KEY);

            // 差動が一定以上あったら報告する
            boolean diffOrientation = prevOrientation.equals(orientation);
            float diffRoll = Math.abs(roll - prevRoll);
            float diffPitch = Math.abs(pitch - prevPitch);
            if ((!diffOrientation)||((!Float.isNaN(roll))&&(diffRoll > sensitivity))||((!Float.isNaN(pitch))&&(diffPitch > sensitivity)))
            {
                // 差動が大きいので変動があったと報告する
                display.updateLevelGauge(orientation, roll, pitch);

                prevOrientation = orientation;
                prevRoll = roll;
                prevPitch = pitch;
            }
            //else
            //{
                // 差動レベルが一定以下の場合は、報告しない
                //Log.v(TAG, "Level Gauge: " + orientation + "[" + roll + "(" + diffRoll + ")" +  "," + pitch + "(" + diffPitch + ")]");
            //}
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     *  表示用のメッセージを生成する
     *
     */
    @Override
    public String geCameraStatusMessage(OLYCamera camera, String name)
    {
        String message = name;
        String warn = "";

        try
        {
            if (delegateListener != null)
            {
                return (" ");
            }

            // 警告メッセージを生成
            if (camera.isHighTemperatureWarning())
            {
                warn = warn + " " + context.getString(R.string.high_temperature_warning);
            }
            if (camera.isExposureMeteringWarning())
            {
                warn = warn + " " + context.getString(R.string.exposure_metering_warning);
            }
            if (camera.isActualIsoSensitivityWarning())
            {
                warn = warn + " " + context.getString(R.string.iso_sensitivity_warning);
            }

            TreeSet<String> treeSet = new TreeSet<>();
            treeSet.add(IOlyCameraProperty.TAKE_MODE);
            treeSet.add(IOlyCameraProperty.WB_MODE);
            treeSet.add(IOlyCameraProperty.AE_MODE);
            treeSet.add(IOlyCameraProperty.EXPOSURE_COMPENSATION);
            Map<String, String> values = camera.getCameraPropertyValues(treeSet);
            //for (Map.Entry<String, String> entry : values.entrySet())
            //{
            //    Log.v(TAG, "STATUS : " + entry.getKey() + " : " + entry.getValue());
            //}
            String takeMode = camera.getCameraPropertyValueTitle(values.get(IOlyCameraProperty.TAKE_MODE));
            String wbMode = camera.getCameraPropertyValueTitle(values.get(IOlyCameraProperty.WB_MODE));
            String aeMode = camera.getCameraPropertyValueTitle(values.get(IOlyCameraProperty.AE_MODE));
            String aperture = camera.getCameraPropertyValueTitle(camera.getActualApertureValue());
            String iso = camera.getCameraPropertyValueTitle(camera.getActualIsoSensitivity());
            String shutter = camera.getCameraPropertyValueTitle(camera.getActualShutterSpeed());
            message = "  ";
            if (takeMode != null)
            {
                message = message + takeMode + " ";
            }
            if (shutter != null)
            {
                message = message + shutter + " ";
            }
            if (aperture != null)
            {
                message = message + "F" + aperture + " ";
            }
            if (iso != null)
            {
                message = message + "ISO" + iso + " ";
            }
            if (wbMode != null)
            {
                message = message + wbMode + " ";
            }
            if (aeMode != null)
            {
                message = message + "[" + aeMode + "] ";
            }
            message = message + warn;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return (message);
    }
}

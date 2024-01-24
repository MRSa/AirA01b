package jp.osdn.gokigen.aira01b.liveview;


import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;

import net.osdn.gokigen.gps.adapter.IGpsLocationNotify;

import jp.osdn.gokigen.aira01b.R;
import jp.osdn.gokigen.aira01b.liveview.phonecamera.IPhoneCameraShutter;
import jp.osdn.gokigen.aira01b.olycamerawrapper.IOlyCameraCoordinator;
import jp.osdn.gokigen.aira01b.preference.ICameraPropertyAccessor;

import static android.content.Context.VIBRATOR_SERVICE;

/**
 *
 *
 */
class OlyCameraLiveViewOnTouchListener  implements View.OnClickListener, View.OnTouchListener, IGpsLocationNotify, View.OnKeyListener
{
    private final String TAG = toString();
    private final FragmentActivity context;
    private IOlyCameraCoordinator camera = null;
    private IPhoneCameraShutter phoneShutter = null;
    private IStatusViewDrawer statusDrawer = null;
    private ILiveImageStatusNotify liveImageView = null;
    private final SharedPreferences preferences;
    private boolean gpsLocationFixed = false;
    private Location currentLocation = null;

    OlyCameraLiveViewOnTouchListener(FragmentActivity context)
    {
        this.context = context;
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    void prepareInterfaces(IOlyCameraCoordinator cameraCoordinator, IPhoneCameraShutter phoneShutter, IStatusViewDrawer statusDrawer, ILiveImageStatusNotify liveImageView)
    {
        this.camera = cameraCoordinator;
        this.phoneShutter = phoneShutter;
        this.statusDrawer = statusDrawer;
        this.liveImageView = liveImageView;
    }

    @Override
    public void onClick(View v)
    {
        boolean isVibration = false;
        int id = v.getId();
        Log.v(TAG, "onClick() : " + id);
        switch (id)
        {
            case R.id.shutterImageView:
                pushShutterButton();
                break;

            case R.id.phoneCameraView:
                //phoneShutter.onTouchedPreviewArea();
                break;

            case R.id.manualFocusImageView:
                camera.toggleManualFocus();
                break;

            case R.id.AutoFocusLockImageView:
                camera.unlockAutoFocus();
                break;

            case R.id.AutoExposureLockImageView:
                camera.toggleAutoExposure();
                isVibration = true;
                break;

            case R.id.buildImageView:
                camera.configure_expert();
                isVibration = true;
                break;

            case R.id.configImageView:
                camera.configure();
                isVibration = true;
                break;

            case R.id.FocusAssistImageView:
                liveImageView.toggleFocusAssist();
                statusDrawer.updateFocusAssistStatus();
                isVibration = true;
                break;

            case R.id.showGridSettingView:
                liveImageView.toggleShowGridFrame();
                statusDrawer.updateGridFrameStatus();
                break;

            case R.id.favoriteSettingsImageView:
                statusDrawer.showFavoriteSettingDialog(context);
                break;

            case R.id.gpsLocationButton:
                statusDrawer.toggleGpsTracking();
                isVibration = true;
                break;

            case R.id.timerShotSettingImageView:
                statusDrawer.toggleTimerStatus();
                break;

            case R.id.liveViewZoomButton:
                liveViewMagnify();
                break;

            default:
                break;
        }
        if (isVibration)
        {
            vibrate();
        }

    }

    /**
     *
     *
     */
    private void vibrate()
    {
        try {
            final Vibrator vibrator = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null)
            {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run()
                    {
                        try
                        {
                            vibrator.vibrate(50);
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                });
                thread.start();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }


    }


    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        int id = v.getId();
        Log.v(TAG, "onTouch() : " + id);
        if (id == R.id.cameraLiveImageView)
        {
            return (camera.driveAutoFocus(event));
        }
        else if (id == R.id.phoneCameraView)
        {
            phoneShutter.onTouchedPreviewArea();
            return (true);
        }
        return (false);
    }

    /**
     *   シャッターボタンが押された！
     *   （現在は、連続撮影モードやムービー撮影についてはまだ非対応）
     */
    private void pushShutterButton()
    {
        // カメラ側のシャッターを押す
        if (camera.isTakeModeMovie())
        {
            //  ムービーモードのとき...撮影開始/終了の切り替え
            camera.movieControl();
            return;
        }

        boolean showToast = true;
        boolean isShootOnlyCamera = false;
        boolean isBracketing = false;
        boolean isCaptureLiveView = false;
        boolean isShareLiveView = false;
        if (preferences != null)
        {
            isShootOnlyCamera = preferences.getBoolean(ICameraPropertyAccessor.SHOOT_ONLY_CAMERA, false);
            isBracketing = preferences.getBoolean(ICameraPropertyAccessor.USE_BRACKETING, false);
            isCaptureLiveView = preferences.getBoolean(ICameraPropertyAccessor.CAPTURE_LIVE_VIEW, false);
            isShareLiveView =  preferences.getBoolean(ICameraPropertyAccessor.SHARE_LIVE_VIEW_IMAGE, false);
            String showSampleImage = preferences.getString(ICameraPropertyAccessor.SHOW_SAMPLE_IMAGE, ICameraPropertyAccessor.SHOW_SAMPLE_IMAGE_DEFAULT_VALUE);
            if (!showSampleImage.equals("0"))
            {
                // 作例表示モードのとき...スマホ内蔵カメラでは撮影しない
                isShootOnlyCamera = true;
            }
        }

        boolean isSingleShot = false;
        if (isBracketing)
        {
            // ブラケッティング撮影を行う
            camera.bracketingControl();
        }
        else
        {
            isSingleShot = camera.isSingleShot();
            if (isSingleShot)
            {
                // １枚撮影する
                camera.singleShot();
            }
            else
            {
                // 連続撮影する
                camera.sequentialShot();
            }
        }
        if (!isShootOnlyCamera)
        {
            // (位置情報を設定して)スマートフォンのシャッターを切る
            phoneShutter.setCurrentLocation(currentLocation);
            phoneShutter.onPressedPhoneShutter(gpsLocationFixed);
            showToast = false;
        }
        if (isCaptureLiveView)
        {
            try
            {
                // ライブビューの画像を保存する
                if (gpsLocationFixed)
                {
                    // 位置情報も格納する
                    liveImageView.captureLiveImage(currentLocation, isShareLiveView);
                }
                else
                {
                    // 位置情報は格納しない
                    liveImageView.captureLiveImage(null, isShareLiveView);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        // 「撮影したよ」通知...スマートフォンのシャッターを切らなかった場合のみ
        if (showToast)
        {
            int id = R.string.shoot_camera;
            if (!isSingleShot)
            {
                // 連続撮影時には、Toastのメッセージを変える
                id = R.string.notify_sequential;
            }
            Toast.makeText(context, id, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     *   GPSの位置情報が更新されたとき...
     *
     */
    @Override
    public void gpsLocationUpdate(long timestamp, Location location, String nmeaLocation)
    {
        //Log.v(TAG, "gpsLocationUpdate() GPS : " + nmeaLocation);
        if ((timestamp == 0)||(nmeaLocation.length() < 0))
        {
            // GPS 位置情報をクリアする
            currentLocation = null;
            camera.clearGeolocation();
            gpsLocationFixed = false;
            liveImageView.getMessageDrawer().setMessageToShow(IMessageDrawer.MessageArea.UPLEFT, Color.argb(0xff, 0xff, 0x99, 0x33), IMessageDrawer.SIZE_STD, "");
            return;
        }
        if (gpsLocationFixed)
        {
            if (location != currentLocation)
            {
                // 捕捉した位置を画面に表示する
                String message = String.format(Locale.getDefault(), "[%3.3f, %3.3f]", location.getLatitude(), location.getLongitude());
                liveImageView.getMessageDrawer().setMessageToShow(IMessageDrawer.MessageArea.UPLEFT, Color.argb(0xff, 0xff, 0x99, 0x00), IMessageDrawer.SIZE_STD, message);

                // GPS位置情報が確定したときのみ、GPS位置情報をカメラに設定する
                camera.setGeolocation(nmeaLocation);
                currentLocation = location;
            }
            else
            {
                // 位置が変わっていない時はGPS位置情報は設定しないこととする
                Log.v(TAG, "GPS LOCATION IS SAME... ");
            }
        }
    }

    /**
     *   GPSの位置情報が確定したとき
     *
     */
    @Override
    public void gpsLocationFixed()
    {
        gpsLocationFixed = true;

        // ボタン等の情報を更新する
        statusDrawer.updateGpsTrackingStatus();
    }


    /**
     *   ライブビューの拡大倍率を変えてみる
     *
     */
    private void liveViewMagnify()
    {
        try
        {
            if ((statusDrawer != null)&&(camera != null))
            {
                float value = camera.changeLiveViewMagnifyScale();
                statusDrawer.updateLiveViewMagnifyScale(camera.isLiveViewMagnifyScaleMax(), value);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     *   ボタンを押したときの対応
     *
     */
    @Override
    public boolean onKey(View view, int keyCode, @NonNull KeyEvent keyEvent)
    {
        Log.v(TAG, "onKey() : " + keyCode);
        try
        {
            if ((keyEvent.getAction() == KeyEvent.ACTION_DOWN)&&
                    ((keyCode == KeyEvent.KEYCODE_VOLUME_UP)||(keyCode == KeyEvent.KEYCODE_CAMERA)))
            {
                pushShutterButton();
                return (true);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return (false);
    }
}

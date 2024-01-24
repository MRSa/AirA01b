package jp.osdn.gokigen.aira01b.olycamerawrapper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PointF;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.view.MotionEvent;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import jp.co.olympus.camerakit.OLYCamera;
import jp.co.olympus.camerakit.OLYCameraConnectionListener;
import jp.co.olympus.camerakit.OLYCameraKitException;
import jp.co.olympus.camerakit.OLYCameraLiveViewListener;
import jp.co.olympus.camerakit.OLYCameraStatusListener;
import jp.osdn.gokigen.aira01b.IAirA01BInterfacesProvider;
import jp.osdn.gokigen.aira01b.R;
import jp.osdn.gokigen.aira01b.liveview.IAutoFocusFrameDisplay;
import jp.osdn.gokigen.aira01b.liveview.ICameraStatusDisplay;
import jp.osdn.gokigen.aira01b.olycamerawrapper.ble.ICameraPowerOn;
import jp.osdn.gokigen.aira01b.olycamerawrapper.ble.PowerOnCamera;
import jp.osdn.gokigen.aira01b.olycamerawrapper.takepicture.AutoFocusControl;
import jp.osdn.gokigen.aira01b.olycamerawrapper.takepicture.BracketingShotControl;
import jp.osdn.gokigen.aira01b.olycamerawrapper.takepicture.MovieRecordingControl;
import jp.osdn.gokigen.aira01b.olycamerawrapper.takepicture.SequentialShotControl;
import jp.osdn.gokigen.aira01b.olycamerawrapper.takepicture.SingleShotControl;
import jp.osdn.gokigen.aira01b.preference.ICameraPropertyAccessor;

/**
 *   OlyCameraCoordinator : Olympus Air との接続、切断の間をとりもつクラス。
 *                         (OLYCameraクラスの実体を保持する)
 *
 *    1. クラスを作成する
 *    2. connectWifi() でカメラと接続する
 *    3. disconnect() でカメラと切断する
 *
 *    X. onDisconnectedByError() でカメラの通信状態が変更されたことを受信する
 *    o. CameraInteractionCoordinator.ICameraCallback でカメラとの接続状態を通知する
 *
 */
public class OlyCameraCoordinator implements OLYCameraConnectionListener, IOlyCameraCoordinator, IIndicatorControl, ICameraRunMode, IOLYCameraObjectProvider
{
    private final String TAG = toString();
    private final Context context;
    private final Executor cameraExecutor = Executors.newFixedThreadPool(1);
    private final BroadcastReceiver connectionReceiver;
    private final OLYCamera camera;
    private final IAirA01BInterfacesProvider interfaceProvider;

    // 本クラスの配下のカメラ制御クラス群
    private final AutoFocusControl autoFocus;
    private final SingleShotControl singleShot;
    private final SequentialShotControl sequentialShot;
    private final MovieRecordingControl movieShot;
    private final BracketingShotControl bracketingShot;
    private final OlyCameraPropertyProxy propertyProxy;
    private final PowerOnCamera powerOnCamera;
    private final LoadSaveCameraProperties loadSaveCameraProperties;

    private boolean isWatchingWifiStatus = false;
    private boolean isManualFocus = false;
    private boolean isAutoFocusLocked = false;
    private boolean isExposureLocked = false;

    private OLYCamera.MagnifyingLiveViewScale liveViewScale = OLYCamera.MagnifyingLiveViewScale.X5;

    /**
     * コンストラクタ
     */
    public OlyCameraCoordinator(AppCompatActivity context, IAirA01BInterfacesProvider interfaceProvider)
    {
        this.interfaceProvider = interfaceProvider;
        this.context = context;

        // OLYMPUS CAMERA クラスの初期化、リスナの設定
        camera = new OLYCamera();
        camera.setContext(context.getApplicationContext());
        camera.setConnectionListener(this);

        // 本クラスの配下のカメラ制御クラス群の設定
        autoFocus = new AutoFocusControl(camera, interfaceProvider, this); // AF制御
        singleShot = new SingleShotControl(camera, interfaceProvider, this);  // 撮影
        sequentialShot = new SequentialShotControl(context, camera, interfaceProvider, this);  // 連続撮影
        movieShot = new MovieRecordingControl(context, camera, interfaceProvider); // 動画撮影
        bracketingShot = new BracketingShotControl(camera, interfaceProvider);  // ブラケッティング撮影
        propertyProxy = new OlyCameraPropertyProxy(camera); // カメラプロパティ

        loadSaveCameraProperties = new LoadSaveCameraProperties(context, propertyProxy, this);

        powerOnCamera = new PowerOnCamera(context, camera);

        connectionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                onReceiveBroadcastOfConnection(context, intent);
            }
        };
    }

    /**
     *   Bluetooth Smart経由でのカメラ起動指示
     *
     * @param callback  接続完了の通知先
     */
    @Override
    public void wakeup(ICameraPowerOn.PowerOnCameraCallback callback)
    {
        powerOnCamera.wakeup(callback);
    }

    /**
     * Wifi接続状態の監視
     * (接続の実処理は onReceiveBroadcastOfConnection() で実施)
     */
    @Override
    public void startWatchWifiStatus(Context context)
    {
        interfaceProvider.getStatusReceiver().onStatusNotify("prepare");

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(connectionReceiver, filter);
        isWatchingWifiStatus = true;
    }

    /**
     * Wifi接続状態の監視終了
     */
    @Override
    public void stopWatchWifiStatus(Context context)
    {
        context.unregisterReceiver(connectionReceiver);
        isWatchingWifiStatus = false;
        disconnect(false);
    }

    /**
     * Wifi接続状態の監視処理を行っているかどうか
     *
     * @return true : 監視中 / false : 停止中
     */
    @Override
    public boolean isWatchWifiStatus() {
        return (isWatchingWifiStatus);
    }

    /**
     *   設定系...
     *
     */
    @Override
    public void configure()
    {
        // カメラの設定画面へ切り替える
        interfaceProvider.getChangeSceneCoordinator().changeSceneToConfiguration();
    }

    /**
     *   詳細設定系...
     */
    @Override
    public void configure_expert()
    {
        // カメラプロパティ一覧画面へ切り替える
        interfaceProvider.getChangeSceneCoordinator().changeSceneToCameraPropertyList();
    }

    /**
     * 　 カメラとの接続を解除する
     *
     * @param powerOff 真ならカメラの電源オフを伴う
     */
    @Override
    public void disconnect(final boolean powerOff) {
        disconnectFromCamera(powerOff);
        interfaceProvider.getStatusReceiver().onCameraDisconnected();
    }

    /**
     * カメラとの再接続を指示する
     */
    @Override
    public void connect() {
        connectToCamera();
    }

    /**
     * カメラの通信状態変化を監視するためのインターフェース
     *
     * @param camera 例外が発生した OLYCamera
     * @param e      カメラクラスの例外
     */
    @Override
    public void onDisconnectedByError(OLYCamera camera, OLYCameraKitException e) {
        // カメラが切れた時に通知する
        interfaceProvider.getStatusReceiver().onCameraDisconnected();
    }

    /**
     * Wifiが使える状態だったら、カメラと接続して動作するよ
     */
    private void onReceiveBroadcastOfConnection(Context context, Intent intent) {
        interfaceProvider.getStatusReceiver().onStatusNotify(context.getString(R.string.connect_check_wifi));

        String action = intent.getAction();
        if ((action != null)&&(action.equals(ConnectivityManager.CONNECTIVITY_ACTION)))
        {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if ((wifiManager != null)&&(wifiManager.isWifiEnabled()))
            {
                // カメラとの接続処理を行う
                connectToCamera();
            }
            /*---------*  Network id は使用していないようなので...接続処理では見なくする
            WifiInfo info = wifiManager.getConnectionInfo();
            if (wifiManager.isWifiEnabled() && info != null && info.getNetworkId() != -1) {
                // カメラとの接続処理を行う
                connectToCamera();
            }
            *---------*/
        }
    }

    /**
     * カメラとの切断処理
     */
    private void disconnectFromCamera(final boolean powerOff) {
        try {
            cameraExecutor.execute(new CameraDisconnectSequence(camera, interfaceProvider.getPropertyAccessor(), powerOff));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * カメラとの接続処理
     */
    private void connectToCamera()
    {
        try {
            cameraExecutor.execute(new CameraConnectSequence(context, camera, interfaceProvider));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ライブビューの設定
     */
    public void setLiveViewListener(OLYCameraLiveViewListener listener) {
        try {
            camera.setLiveViewListener(listener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *   ライブビューの解像度を設定する
     *
     */
    @Override
    public void changeLiveViewSize(String size)
    {
        try {
            camera.changeLiveViewSize(CameraPropertyUtilities.toLiveViewSizeType(size));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *   ライブビューの開始
     *
     */
    @Override
    public void startLiveView()
    {
        try {
            camera.startLiveView();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *   ライブビューの終了
     *
     */
    @Override
    public void stopLiveView()
    {
        try {
            camera.stopLiveView();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *   ライブビューの拡大倍率を変更する
     *
     * @return 拡大倍率(数値)
     */
    @Override
    public float changeLiveViewMagnifyScale()
    {
        float scale = 1.0f;
        try
        {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            String value = preferences.getString(ICameraPropertyAccessor.LIVE_VIEW_SCALE, ICameraPropertyAccessor.LIVE_VIEW_SCALE_DEFAULT_VALUE);
            if (value.equals("STEP"))
            {
                // 段階的に大きくする設定
                return (changeLiveViewMagnifyScaleStep());
            }
            // ワンプッシュで拡大と通常表示を切り替える
            if (!camera.isMagnifyingLiveView())
            {
                // ライブビューの拡大を行っていない場合は、ライブビューの拡大を行う
                liveViewScale = decideLiveViewScale(value);
                camera.startMagnifyingLiveViewAtPoint(new PointF(0.5f, 0.5f), liveViewScale);
                scale = Float.parseFloat(value);
            }
            else
            {
                // ライブビューの拡大をやめる
                liveViewScale = OLYCamera.MagnifyingLiveViewScale.X5;
                camera.stopMagnifyingLiveView();
                scale = 1.0f;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return (scale);
    }

    /**
     *   ライブビューの拡大倍率を返す
     *
     *
     * @param value  拡大倍率
     * @return  拡大倍率（制御用）
     */
    private OLYCamera.MagnifyingLiveViewScale decideLiveViewScale(String value)
    {
        OLYCamera.MagnifyingLiveViewScale scale = OLYCamera.MagnifyingLiveViewScale.X5;
        if (value == null)
        {
            return (scale);
        }
        switch (value)
        {
            case "7.0":
                scale =  OLYCamera.MagnifyingLiveViewScale.X7;
                break;
            case "10.0":
                scale =  OLYCamera.MagnifyingLiveViewScale.X10;
                break;
            case "14.0":
                scale =  OLYCamera.MagnifyingLiveViewScale.X14;
                break;

            case "5.0":
            default:
                break;
        }
        return (scale);
    }

    private float changeLiveViewMagnifyScaleStep()
    {
        float scale = 1.0f;
        try
        {
            // ライブビューの拡大を行っていない場合...
            if (!camera.isMagnifyingLiveView())
            {
                // ライブビューの拡大開始
                liveViewScale = OLYCamera.MagnifyingLiveViewScale.X5;
                camera.startMagnifyingLiveViewAtPoint(new PointF(0.5f, 0.5f), liveViewScale);
                scale = 5.0f;
                return (scale);
            }
            // ライブビューの最大拡大中...
            if (liveViewScale == OLYCamera.MagnifyingLiveViewScale.X14)
            {
                // ライブビューの拡大終了
                liveViewScale = OLYCamera.MagnifyingLiveViewScale.X5;
                camera.stopMagnifyingLiveView();
                scale = 1.0f;
                return (scale);
            }

            // ライブビューの拡大倍率を変えていく  ( x5 → x7 → x10 → x14 )
            if (liveViewScale == OLYCamera.MagnifyingLiveViewScale.X5)
            {
                liveViewScale = OLYCamera.MagnifyingLiveViewScale.X7;
                scale = 7.0f;
            }
            else if (liveViewScale == OLYCamera.MagnifyingLiveViewScale.X7)
            {
                liveViewScale = OLYCamera.MagnifyingLiveViewScale.X10;
                scale = 10.0f;
            }
            else // if (liveViewScale == OLYCamera.MagnifyingLiveViewScale.X10)
            {
                liveViewScale = OLYCamera.MagnifyingLiveViewScale.X14;
                scale = 14.0f;
            }
            camera.changeMagnifyingLiveViewScale(liveViewScale);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return (scale);
    }


    /**
     *   ライブビューの拡大倍率が最大かどうか
     *
     * @return 最大倍率のときは true, そうでない時は false
     */
    @Override
    public boolean isLiveViewMagnifyScaleMax()
    {
        try
        {
            if (camera.isMagnifyingLiveView())
            {
                if (liveViewScale == OLYCamera.MagnifyingLiveViewScale.X14)
                {
                    return (true);
                }

                // ワンプッシュでの拡大表示モード
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                String value = preferences.getString(ICameraPropertyAccessor.LIVE_VIEW_SCALE, ICameraPropertyAccessor.LIVE_VIEW_SCALE_DEFAULT_VALUE);
                if (!value.equals("STEP"))
                {
                    return (camera.isMagnifyingLiveView());
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return (false);
    }

    /**
     * フォーカスロックの実行
     */
    public boolean driveAutoFocus(final MotionEvent event)
    {
        if (event.getAction() != MotionEvent.ACTION_DOWN)
        {
            return (false);
        }
        final IAutoFocusFrameDisplay frameDisplay = interfaceProvider.getAutoFocusFrameInterface();
        if (frameDisplay != null)
        {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    PointF point = frameDisplay.getPointWithEvent(event);
                    if (frameDisplay.isContainsPoint(point))
                    {
                        autoFocus.lockAutoFocus(point);
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
        return (false);
    }

    /**
     * フォーカスロックの解除
     */
    public void unlockAutoFocus()
    {
        autoFocus.unlockAutoFocus();
        IAutoFocusFrameDisplay focusFrame = interfaceProvider.getAutoFocusFrameInterface();
        if (focusFrame != null)
        {
            focusFrame.hideFocusFrame();
        }
        isAutoFocusLocked = false;
    }


    /**
     *   撮影モードを取得する
     *
     * @return true : 1枚撮影 / false : 連続撮影
     */
    @Override
    public boolean isSingleShot()
    {
        boolean ret = false;
        try
        {
            String value = propertyProxy.getCameraPropertyValue(IOlyCameraProperty.DRIVE_MODE);
            ret = value.equals(IOlyCameraProperty.DRIVE_MODE_SINGLE);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return (ret);
    }

    /**
     * 画像を１枚撮影
     */
    @Override
    public void singleShot()
    {
        singleShot.singleShot();
    }

    /**
     *   画像の連続撮影開始・終了
     */
    @Override
    public void sequentialShot()
    {
        sequentialShot.shotControl();
    }

    /**
     *   動画の撮影・撮影停止
     *
     */
    @Override
    public void movieControl()
    {
        movieShot.movieControl();
    }


    /**
     *   ブラケッティング撮影の開始...
     *
     */
    @Override
    public void bracketingControl()
    {
        Log.v(TAG, "bracketingControl() ");

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (preferences.getBoolean(ICameraPropertyAccessor.USE_BRACKETING, false))
        {
            try
            {
                // ブラケッティング撮影を行う
                int bracketingStyle = Integer.parseInt(preferences.getString(ICameraPropertyAccessor.BRACKETING_TYPE, ICameraPropertyAccessor.BRACKETING_TYPE_DEFAULT_VALUE));
                int bracketingCount = Integer.parseInt(preferences.getString(ICameraPropertyAccessor.BRACKETING_COUNT, ICameraPropertyAccessor.BRACKETING_COUNT_DEFAULT_VALUE));
                int bracketingDuration = Integer.parseInt(preferences.getString(ICameraPropertyAccessor.BRACKETING_DURATION, ICameraPropertyAccessor.BRACKETING_DURATION_DEFAULT_VALUE));
                bracketingShot.startShootBracketing(bracketingStyle, bracketingCount, bracketingDuration);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    /**
     *
     *
     */
    @Override
    public void toggleAutoExposure()
    {
        try
        {
            if (isExposureLocked)
            {
                Log.v(TAG, "toggleAutoExposure() : unlockAutoExposure()");
                camera.unlockAutoExposure();
            }
            else
            {
                Log.v(TAG, "toggleAutoExposure() : lockAutoExposure()");
                camera.lockAutoExposure();
            }
            updateIndicatorScreen(false);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void toggleManualFocus()
    {
        try
        {
            boolean isHideFocusFrame = false;
            String property_name = IOlyCameraProperty.FOCUS_STILL;
            String poverty_value = "<" + IOlyCameraProperty.FOCUS_STILL + "/";

            // マニュアルフォーカス切替え
            if (!isManualFocus)
            {
                // AF -> MF  : オートフォーカスを解除して設定する
                Log.v(TAG, "toggleManualFocus() : to " + IOlyCameraProperty.FOCUS_MF);
                poverty_value = poverty_value + IOlyCameraProperty.FOCUS_MF + ">";
                camera.unlockAutoFocus();
                camera.setCameraPropertyValue(property_name, poverty_value);
                isHideFocusFrame = true;
            }
            else
            {
                // MF -> AF
                Log.v(TAG, "toggleManualFocus() : to " + IOlyCameraProperty.FOCUS_SAF);
                poverty_value = poverty_value + IOlyCameraProperty.FOCUS_SAF + ">";
                camera.setCameraPropertyValue(property_name, poverty_value);
            }
            updateIndicatorScreen(isHideFocusFrame);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void updateIndicatorScreen(boolean isHideFocusFrame)
    {
        isManualFocus();
        if (interfaceProvider != null)
        {
            if (isHideFocusFrame)
            {
                isAutoFocusLocked = false;
                IAutoFocusFrameDisplay focusFrame = interfaceProvider.getAutoFocusFrameInterface();
                if (focusFrame != null)
                {
                    focusFrame.hideFocusFrame();
                }
            }
            ICameraStatusDisplay display = interfaceProvider.getCameraStatusInterface();
            if (display != null)
            {
                display.updateCameraStatus();
            }
        }
    }

    @Override
    public boolean isManualFocus()
    {
        isManualFocus = propertyProxy.isManualFocus();
        return (isManualFocus);
    }

    @Override
    public boolean isAFLock()
    {
        return (isAutoFocusLocked);
    }

    @Override
    public boolean isAELock()
    {
        isExposureLocked = propertyProxy.isExposureLocked();
        return (isExposureLocked);
    }

    @Override
    public boolean isTakeModeMovie()
    {
        String takeMode = propertyProxy.getCameraPropertyValue(IOlyCameraProperty.TAKE_MODE);
        return (takeMode.equals(IOlyCameraProperty.TAKE_MODE_MOVIE));
    }

    /**
     *   位置情報を設定する
     *
     * @param nmeaLocation 位置情報 (NMEA0183のGPGGAセンテンスとGPRMCセンテンス)
     */
    @Override
    public void setGeolocation(String nmeaLocation)
    {
        Log.v(TAG, "setGeolocation() : " + nmeaLocation);
        try
        {
            camera.setGeolocation(nmeaLocation);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     *   位置情報をクリアすする
     *
     */
    @Override
    public void clearGeolocation()
    {
        Log.v(TAG, "clearGeolocation()");
        try
        {
             camera.clearGeolocation();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void setCameraStatusListener(OLYCameraStatusListener listener)
    {
        camera.setCameraStatusListener(listener);
    }

    @Override
    public String getCameraStatusSummary(ICameraStatusSummary decoder)
    {
        return (decoder.geCameraStatusMessage(camera, ""));
    }

    @Override
    public void changeRunMode(boolean isRecording)
    {
        OLYCamera.RunMode runMode = (isRecording) ? OLYCamera.RunMode.Recording : OLYCamera.RunMode.Playback;
        Log.v(TAG, "changeRunMode() : " + runMode);
        try
        {
            camera.changeRunMode(runMode);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isRecordingMode()
    {
        boolean isRecordingMode = false;
        try
        {
            OLYCamera.RunMode runMode = camera.getRunMode();
            isRecordingMode =  (runMode == OLYCamera.RunMode.Recording);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return (isRecordingMode);
    }

    @Override
    public IOlyCameraPropertyProvider getCameraPropertyProvider()
    {
        return (propertyProxy);
    }

    @Override
    public ILoadSaveCameraProperties getLoadSaveCameraProperties()
    {
        return (loadSaveCameraProperties);
    }

    @Override
    public ICameraRunMode getChangeRunModeExecutor()
    {
        return (this);
    }

    @Override
    public void onAfLockUpdate(boolean isAfLocked)
    {
        isAutoFocusLocked = isAfLocked;
        updateIndicatorScreen(false);
    }

    @Override
    public OLYCamera getOLYCamera()
    {
        return (camera);
    }
}

package jp.osdn.gokigen.aira01b.olycamerawrapper;

import android.content.Context;
import android.view.MotionEvent;

import jp.co.olympus.camerakit.OLYCameraLiveViewListener;
import jp.co.olympus.camerakit.OLYCameraStatusListener;
import jp.osdn.gokigen.aira01b.olycamerawrapper.ble.ICameraPowerOn;

/**
 *
 *
 */
public interface IOlyCameraCoordinator
{
    // Bluetooth Smartでカメラを起こす
    void wakeup(ICameraPowerOn.PowerOnCameraCallback callback);

    // WIFI 接続系
    void startWatchWifiStatus(Context context);
    void stopWatchWifiStatus(Context context);
    boolean isWatchWifiStatus();

    // Preference設定系 ...本来はここからサービスしないほうがよさ気
    void configure();
    void configure_expert();

    /** カメラ接続系 **/
    void disconnect(final boolean powerOff);
    void connect();

    /** ライブビュー関係 **/
    void changeLiveViewSize(String size);
    void setLiveViewListener(OLYCameraLiveViewListener listener);
    void startLiveView();
    void stopLiveView();
    float changeLiveViewMagnifyScale();
    boolean isLiveViewMagnifyScaleMax();

    /** オートフォーカス機能の実行 **/
    boolean driveAutoFocus(MotionEvent event);
    void unlockAutoFocus();

    /** シングル撮影機能の実行 **/
    boolean isSingleShot();
    void singleShot();

    /** 連続撮影の開始・終了 **/
    void sequentialShot();

    /** 動画撮影の開始・終了 **/
    void movieControl();

    /** ブラケッティング撮影の開始 **/
    void bracketingControl();

    /** AE Lockの設定・解除、 AF/MFの切替え **/
    void toggleAutoExposure();
    void toggleManualFocus();

    /** カメラの状態取得 **/
    boolean isManualFocus();
    boolean isAFLock();
    boolean isAELock();
    boolean isTakeModeMovie();

    /** GPS関連 **/
    void setGeolocation(String nmeaLocation);
    void clearGeolocation();

    /** カメラの状態変化リスナの設定 **/
    void setCameraStatusListener(OLYCameraStatusListener listener);

    /** カメラの状態サマリ(のテキスト情報)を取得する **/
    String getCameraStatusSummary(ICameraStatusSummary decoder);

    // カメラプロパティアクセスインタフェース
    IOlyCameraPropertyProvider getCameraPropertyProvider();

    // カメラプロパティのロード・セーブインタフェース
    ILoadSaveCameraProperties getLoadSaveCameraProperties();

    // カメラの動作モード変更インタフェース
    ICameraRunMode getChangeRunModeExecutor();
}

package jp.osdn.gokigen.aira01b.liveview;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

import net.osdn.gokigen.gps.adapter.IGpsFeatureSwitch;
import net.osdn.gokigen.gps.adapter.IGpsLocationNotify;
import net.osdn.gokigen.gps.adapter.IGpsLocationPicker;
import net.osdn.gokigen.gps.adapter.legacy.LegacyGpsLocationPicker;

import jp.osdn.gokigen.aira01b.MyInterfaceProvider;
import jp.osdn.gokigen.aira01b.R;
import jp.osdn.gokigen.aira01b.gps.GnssStatusPicker;
import jp.osdn.gokigen.aira01b.liveview.bufferedimage.BufferedImagePanel;
import jp.osdn.gokigen.aira01b.liveview.controlpanel.CameraControlPanel;
import jp.osdn.gokigen.aira01b.liveview.phonecamera.PhoneCameraView;
import jp.osdn.gokigen.aira01b.myolycameraprops.LoadSaveMyCameraPropertyDialog;
import jp.osdn.gokigen.aira01b.olycamerawrapper.CameraPropertyLoadSaveOperations;
import jp.osdn.gokigen.aira01b.olycamerawrapper.CameraStatusListenerImpl;
import jp.osdn.gokigen.aira01b.olycamerawrapper.ICameraRunMode;
import jp.osdn.gokigen.aira01b.olycamerawrapper.IOlyCameraCoordinator;
import jp.osdn.gokigen.aira01b.olycamerawrapper.IOlyCameraProperty;
import jp.osdn.gokigen.aira01b.olycamerawrapper.IOlyCameraPropertyProvider;
import jp.osdn.gokigen.aira01b.preference.ICameraPropertyAccessor;

/**
 *  撮影用ライブビュー画面
 *
 */
public class LiveViewFragment extends Fragment implements IStatusViewDrawer, ICameraStatusDisplay, IGpsFeatureSwitch
{
    private final String TAG = this.toString();
    private static final int COMMAND_MY_PROPERTY = 0x00000100;

    private IOlyCameraCoordinator camera = null;
    private MyInterfaceProvider factory = null;
    private ICameraRunMode changeRunModeExecutor = null;
    private OlyCameraLiveViewOnTouchListener onTouchListener = null;
    private CameraLiveViewListenerImpl liveViewListener = null;
    private CameraStatusListenerImpl statusListener = null;
    private IGpsLocationPicker locationPicker = null;

    private TextView statusArea = null;
    private CameraLiveImageView imageView = null;
    //private CameraControlPanel cameraPanel = null;

    private SeekBarScaleHolder upperSeekBarScaleHolder = null;
    private SeekBarScaleHolder lowerSeekBarScaleHolder = null;

    private ImageView manualFocus = null;
    private ImageView afLock = null;
    private ImageView aeLock = null;
    private ImageView focusAssist = null;
    private ImageView showGrid = null;
    private ImageView bracketing = null;

    private ImageButton liveViewZoomButton = null;
    private TextView liveViewMagnify = null;

    private boolean imageViewCreated = false;
    private View myView = null;
    private String messageValue = "";

    /**
     *
     *
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate()");

        try
        {
            if (liveViewListener == null)
            {
                int nofCacheImages = 500;
                try
                {
                    SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    String cacheSize = preference.getString(ICameraPropertyAccessor.NUMBER_OF_CACHE_PICTURES, ICameraPropertyAccessor.NUMBER_OF_CACHE_PICTURES_DEFAULT_VALUE);
                    nofCacheImages = parseInt(cacheSize, nofCacheImages);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                liveViewListener = new CameraLiveViewListenerImpl(nofCacheImages);
            }
            if (onTouchListener == null)
            {
                onTouchListener = new OlyCameraLiveViewOnTouchListener(getActivity());
            }
            if (statusListener == null)
            {
                Context context = getContext();
                if (context != null)
                {
                    context = context.getApplicationContext();
                }
                if (context != null)
                {
                    statusListener = new CameraStatusListenerImpl(context, this);
                }
            }
            if (locationPicker == null)
            {
                Context context = getContext();
                if (context != null)
                {
                    context = context.getApplicationContext();
                    locationPicker = getLocationPicker(context, onTouchListener);
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private IGpsLocationPicker getLocationPicker(@NonNull Context context, @NonNull IGpsLocationNotify target)
    {
        try
        {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
                return (new LegacyGpsLocationPicker(context, target));
            } else {
                return (new GnssStatusPicker(context, target));  // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return (null);
    }

    /**
     *
     *
     */
    @Override
    public void onAttach(@NonNull Context context)
    {
        super.onAttach(context);
        Log.v(TAG, "onAttach()");
    }

    /**
     *
     *
     */
    @Override
    public View onCreateView(@NonNull  LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        super.onCreateView(inflater, container, savedInstanceState);

        Log.v(TAG, "onCreateView()");

        if ((imageViewCreated)&&(myView != null))
        {
            // Viewを再利用。。。
            Log.v(TAG, "onCreateView() : called again, so do nothing...");
            return (myView);
        }
        View view = inflater.inflate(R.layout.fragment_live_view, container, false);
        myView = view;
        imageViewCreated = true;

        imageView = view.findViewById(R.id.cameraLiveImageView);
        imageView.setOnClickListener(onTouchListener);
        imageView.setOnTouchListener(onTouchListener);
        imageView.setActivity(getActivity());

        liveViewListener.setCameraLiveImageView(imageView);
        if (factory != null)
        {
            factory.setAutoFocusFrameDisplay(imageView);
        }

        // 画面下部のスマホカメラ領域
        PhoneCameraView phoneCameraView = view.findViewById(R.id.phoneCameraView);

        // カメラ画像の大きさを動的に調整（したい）
        //phoneCameraView.getViewTreeObserver().addOnGlobalLayoutListener(phoneCameraView);

        ImageView shutter = view.findViewById(R.id.shutterImageView);
        shutter.setOnClickListener(onTouchListener);

        ImageView config = view.findViewById(R.id.configImageView);
        config.setOnClickListener(onTouchListener);

        ImageView build = view.findViewById(R.id.buildImageView);
        build.setOnClickListener(onTouchListener);

        ImageButton gps = view.findViewById(R.id.gpsLocationButton);
        if ((locationPicker != null)&&(locationPicker.prepare(this))&&(locationPicker.hasGps()))
        {
            // GPSボタンの状態を更新しておく
            updateGpsTrackingStatus();

            // GPSが使用可能な状態のとき...ボタンを押せるようにする
            gps.setOnClickListener(onTouchListener);
        }
        else
        {
            // GPSが利用不可のとき、、、ボタンは無効(非表示)にする
            gps.setEnabled(false);
            gps.setVisibility(View.INVISIBLE);
        }

        manualFocus = view.findViewById(R.id.manualFocusImageView);
        manualFocus.setOnClickListener(onTouchListener);

        afLock = view.findViewById(R.id.AutoFocusLockImageView);
        afLock.setOnClickListener(onTouchListener);

        aeLock = view.findViewById(R.id.AutoExposureLockImageView);
        aeLock.setOnClickListener(onTouchListener);

        focusAssist = view.findViewById(R.id.FocusAssistImageView);
        focusAssist.setOnClickListener(onTouchListener);

        showGrid = view.findViewById(R.id.showGridSettingView);
        showGrid.setOnClickListener(onTouchListener);

        ImageView favoriteSetting = view.findViewById(R.id.favoriteSettingsImageView);
        favoriteSetting.setOnClickListener(onTouchListener);

        liveViewZoomButton = view.findViewById(R.id.liveViewZoomButton);
        liveViewZoomButton.setOnClickListener(onTouchListener);
        liveViewMagnify = view.findViewById(R.id.liveViewMagnify);

        bracketing = view.findViewById(R.id.timerShotSettingImageView);
        bracketing.setOnClickListener(onTouchListener);

        statusArea = view.findViewById(R.id.informationMessageTextView);

        onTouchListener.prepareInterfaces(camera, phoneCameraView, this, imageView);

        return (view);
    }

    /**
     *   作例表示モードの画像のURIを応答する
     *
     * @return Uri : 作例表示する画像のURI
     */
    private Uri isSetupSampleImageFile(String fileName)
    {
        try
        {
            File file = new File(fileName);
            if (file.exists())
            {
                Log.v(TAG, "isSetupSampleImageFile() : " + file.toString());
                return (Uri.fromFile(file));
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        Log.v(TAG, "isSetupSampleImageFile() : nothing");
        return (null);
    }

    private int parseInt(String value, int defaultValue)
    {
        int intValue = defaultValue;
        try
        {
            intValue = Integer.parseInt(value);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return (intValue);
    }

    /**
     *   画面下部の表示エリアの用途を切り替える
     *
     */
    private void setupLowerDisplayArea()
    {
        Activity activity = getActivity();
        if (activity == null)
        {
            // Activityが取得できない場合は何もせずに終了する
            return;
        }

        ScalableImageViewPanel sampleImageView = activity.findViewById(R.id.favoriteImageView);
        PhoneCameraView phoneCameraView = activity.findViewById(R.id.phoneCameraView);
        SeekBar upperSeekBar = activity.findViewById(R.id.liveview_upper_seekbar);
        SeekBar lowerSeekBar = activity.findViewById(R.id.liveview_lower_seekbar);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        String value = preferences.getString(ICameraPropertyAccessor.SHOW_SAMPLE_IMAGE, ICameraPropertyAccessor.SHOW_SAMPLE_IMAGE_DEFAULT_VALUE);
        if (value.equals("2"))
        {
            // 操作パネル表示モード
            try
            {
                int sensitivity = Integer.parseInt(preferences.getString(ICameraPropertyAccessor.LOWER_CONTROL_FLICK_SENSITIVITY, ICameraPropertyAccessor.LOWER_CONTROL_FLICK_SENSITIVITY_DEFAULT_VALUE));
                phoneCameraView.setVisibility(View.GONE);
                sampleImageView.setVisibility(View.VISIBLE);
                upperSeekBar.setVisibility(View.GONE);
                lowerSeekBar.setVisibility(View.GONE);
                CameraControlPanel cameraPanel = new CameraControlPanel(sampleImageView, camera.getCameraPropertyProvider());
                cameraPanel.updateVerocityThreshold(sensitivity);
                statusListener.setDelegateListener(cameraPanel);
                sampleImageView.setOnClickListener(cameraPanel);
                sampleImageView.setOnTouchListener(cameraPanel);
                sampleImageView.setOnLongClickListener(cameraPanel);
                sampleImageView.setCameraPanelDrawer(true, cameraPanel);
                sampleImageView.invalidate();
                return;
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        if (value.equals("3"))
        {
            // 画像バッファリングモード （ライブビュー遅延表示モード）
            //String nofCachePictures = preferences.getString(ICameraPropertyAccessor.NUMBER_OF_CACHE_PICTURES, ICameraPropertyAccessor.NUMBER_OF_CACHE_PICTURES_DEFAULT_VALUE);
            try
            {
                if (liveViewListener != null)
                {
                    liveViewListener.updateBufferedImageStatus(true); // イメージのバッファリングを実行する
                    if (lowerSeekBarScaleHolder == null)
                    {
                        lowerSeekBarScaleHolder = new SeekBarScaleHolder(sampleImageView, lowerSeekBar.getMax());
                    }
                }
                phoneCameraView.setVisibility(View.GONE);
                sampleImageView.setVisibility(View.VISIBLE);
                upperSeekBar.setVisibility(View.GONE);
                lowerSeekBar.setVisibility(View.VISIBLE);
                lowerSeekBar.setOnSeekBarChangeListener(lowerSeekBarScaleHolder);
                BufferedImagePanel cameraPanel = new BufferedImagePanel(sampleImageView, liveViewListener, lowerSeekBarScaleHolder);
                if (liveViewListener != null)
                {
                    liveViewListener.setBufferedImageNotify(cameraPanel);
                }
                statusListener.setDelegateListener(cameraPanel);
                sampleImageView.setOnClickListener(cameraPanel);
                sampleImageView.setOnTouchListener(cameraPanel);
                sampleImageView.setOnLongClickListener(cameraPanel);
                sampleImageView.setCameraPanelDrawer(true, cameraPanel);
                sampleImageView.invalidate();
                return;
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        statusListener.setDelegateListener(null);
        sampleImageView.setOnClickListener(null);
        sampleImageView.setOnTouchListener(null);
        sampleImageView.setOnLongClickListener(null);
        sampleImageView.setCameraPanelDrawer(false, null);

        Uri uri = null;
        if (value.equals("1"))
        {
            // 作例表示用の画像を取得
            uri = isSetupSampleImageFile(preferences.getString(ICameraPropertyAccessor.SELECT_SAMPLE_IMAGE, ""));
        }
        if (uri != null)
        {
            // 作例表示モード
            phoneCameraView.setVisibility(View.GONE);
            sampleImageView.setVisibility(View.VISIBLE);
            upperSeekBar.setVisibility(View.GONE);
            lowerSeekBar.setVisibility(View.GONE);
            sampleImageView.setImageURI(uri);
            sampleImageView.invalidate();
        }
        else
        {
            // デュアルカメラモード
            phoneCameraView.setVisibility(View.VISIBLE);
            sampleImageView.setVisibility(View.GONE);
            upperSeekBar.setVisibility(View.GONE);
            lowerSeekBar.setVisibility(View.GONE);

            // カメラの画像にタッチリスナを付与
            phoneCameraView.setOnClickListener(onTouchListener);
            phoneCameraView.setOnTouchListener(onTouchListener);
        }
    }

    /**
     *
     *
     *
     */
    @Override
    public void onStart()
    {
        super.onStart();
        Log.v(TAG, "onStart()");
    }

    /**
     *
     *
     */
    @Override
    public void onResume()
    {
        super.onResume();
        Log.v(TAG, "onResume() Start");

        // 撮影モードかどうかを確認して、撮影モードではなかったら撮影モードに切り替える
        if ((changeRunModeExecutor != null)&&(!changeRunModeExecutor.isRecordingMode()))
        {
            // Runモードを切り替える。（でも切り替えると、設定がクリアされてしまう...。
            changeRunModeExecutor.changeRunMode(true);
        }

        // ステータスの変更を通知してもらう
        camera.setCameraStatusListener(statusListener);

        // 画面下部の表示エリアの用途を切り替える
        setupLowerDisplayArea();

        // propertyを取得
        try
        {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());

            // グリッド・フォーカスアシストの情報を戻す
            imageView.setShowGridFrame(preferences.getBoolean(ICameraPropertyAccessor.SHOW_GRID_STATUS, false));
            imageView.setFocusAssist(preferences.getBoolean(ICameraPropertyAccessor.SHOW_FOCUS_ASSIST_STATUS, false));
            updateCameraPropertyStatus();

            // ステータスの初期情報を表示する
            updateStatusView(camera.getCameraStatusSummary(statusListener));

            // ライブビューの開始
            camera.changeLiveViewSize(preferences.getString(ICameraPropertyAccessor.LIVE_VIEW_QUALITY, ICameraPropertyAccessor.LIVE_VIEW_QUALITY_DEFAULT_VALUE));
            camera.setLiveViewListener(liveViewListener);
            liveViewListener.setCameraLiveImageView(imageView);
            camera.startLiveView();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        // GPSボタンの更新
        updateGpsTrackingStatus();

        // デジタル水準器を有効にするかどうか
        if (statusListener != null)
        {
            statusListener.updateLevelGaugeChecking();
        }

        Log.v(TAG, "onResume() End");
    }

    /**
     *
     *
     */
    @Override
    public void onPause()
    {
        super.onPause();
        Log.v(TAG, "onPause() Start");

        // ライブビューの停止
        camera.stopLiveView();
        camera.setLiveViewListener(null);
        liveViewListener.setCameraLiveImageView(null);

        if (locationPicker != null)
        {
            // GPS監視の終了
            locationPicker.controlGps(false);
            camera.clearGeolocation();
        }
        Log.v(TAG, "onPause() End");
    }

    /**
     * カメラクラスをセットする
     *
     */
    public void setInterfaces(IOlyCameraCoordinator camera, MyInterfaceProvider factory)
    {
        Log.v(TAG, "setInterfaces()");
        this.camera = camera;
        this.factory = factory;
        this.changeRunModeExecutor = camera.getChangeRunModeExecutor();

        factory.setStatusInterface(this);
        factory.setStatusViewDrawer(this);
        //if (imageView != null)
        {
        //    factory.setAutoFocusFrameDisplay(imageView);
        }
    }

    @Override
    public void updateFocusAssistStatus()
    {
        updateCameraPropertyStatus();
    }

    @Override
    public void updateGridFrameStatus()
    {
        updateCameraPropertyStatus();
    }

    @Override
    public void updateTakeMode()
    {
        updateCameraPropertyStatus();
    }

    @Override
    public void updateDriveMode()
    {
        updateCameraPropertyStatus();
    }

    @Override
    public void updateWhiteBalance()
    {
        updateCameraPropertyStatus();
    }

    @Override
    public void updateBatteryLevel()
    {
        updateCameraPropertyStatus();
    }

    @Override
    public void updateAeMode()
    {
        updateCameraPropertyStatus();
    }

    @Override
    public void updateAeLockState()
    {
        updateCameraPropertyStatus();
    }

    @Override
    public void updateCameraStatus()
    {
        updateCameraPropertyStatus();
    }

    @Override
    public void updateCameraStatus(String message)
    {
        updateStatusView(message);
    }

    @Override
    public void updateLevelGauge(String orientation, float roll, float pitch)
    {
        if (imageView == null)
        {
            return;
        }

        // レベルゲージ(デジタル水準器の情報)が更新されたとき
        //Log.v(TAG, String.format(Locale.getDefault(), "LEVEL GAUGE : %s roll: %3.3f pitch: %3.3f", orientation, roll, pitch));
        try
        {
            if ((Float.isNaN(roll))||(Float.isNaN(pitch)))
            {
                // roll と pitch のどちらかがNaNなら、表示を消す
                imageView.getMessageDrawer().setMessageToShow(IMessageDrawer.MessageArea.LOWRIGHT, Color.argb(0xff, 0x6e, 0x6e, 0x6e), IMessageDrawer.SIZE_STD, "");
                imageView.getMessageDrawer().setLevelToShow(IMessageDrawer.LevelArea.LEVEL_HORIZONTAL, Float.NaN);
                imageView.getMessageDrawer().setLevelToShow(IMessageDrawer.LevelArea.LEVEL_VERTICAL, Float.NaN);
                return;
            }

            // 傾きのデータを設定する
            String message = String.format(Locale.getDefault(), "[%3.1f, %3.1f]", roll, pitch);
            imageView.getMessageDrawer().setMessageToShow(IMessageDrawer.MessageArea.LOWRIGHT, Color.argb(0xff, 0x6e, 0x6e, 0x6e), IMessageDrawer.SIZE_STD, message);
            imageView.getMessageDrawer().setLevelToShow(IMessageDrawer.LevelArea.LEVEL_HORIZONTAL, roll);
            imageView.getMessageDrawer().setLevelToShow(IMessageDrawer.LevelArea.LEVEL_VERTICAL, pitch);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void showFavoriteSettingDialog(@NonNull FragmentActivity activity)
    {
        LoadSaveMyCameraPropertyDialog dialog = new LoadSaveMyCameraPropertyDialog();
        dialog.setTargetFragment(this, COMMAND_MY_PROPERTY);
        dialog.setPropertyOperationsHolder(new CameraPropertyLoadSaveOperations(getActivity(), camera.getLoadSaveCameraProperties(), this));
        //dialog.show(getChildFragmentManager(), "my_dialog");

        FragmentManager manager = activity.getSupportFragmentManager();
        if (manager != null)
        {
            dialog.show(manager, "my_dialog");
        }
        else
        {
            Log.v(TAG, "FragmentManager is NULL!!");
        }
     }

    @Override
    public void toggleTimerStatus()
    {
        try {
            boolean isBracketing = !isBracketing();
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(ICameraPropertyAccessor.USE_BRACKETING, isBracketing);
            editor.apply();
            if (bracketing != null) {
                bracketing.setSelected(isBracketing);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private boolean isBracketing()
    {
        boolean isBracketing = false;
        try
        {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            if (preferences != null) {
                isBracketing = preferences.getBoolean(ICameraPropertyAccessor.USE_BRACKETING, false);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return (isBracketing);
    }

    /**
     *
     *
     */
    private void updateCameraPropertyStatus()
    {
        try
        {
            final boolean isManualFocus = camera.isManualFocus();
            final boolean isAfLock = camera.isAFLock();
            final boolean isAeLock = camera.isAELock();
            final boolean isTimerOn = isBracketing();
            boolean checkFocusAssist = false;
            boolean checkShowGrid = false;
            if (imageView != null)
            {
                checkFocusAssist = imageView.isFocusAssist();
                checkShowGrid = imageView.isShowGrid();
            }
            final boolean isFocusAssist = checkFocusAssist;
            final boolean isShowGrid = checkShowGrid;

            runOnUiThread(new Runnable()
            {
                /**
                 * カメラの状態(インジケータ)を更新する
                 */
                @Override
                public void run() {
                    if (camera == null) {
                        return;
                    }
                    Log.v(TAG, "--- UPDATE CAMERA PROPERTY (START) ---");
                    if (manualFocus != null) {
                        manualFocus.setSelected(isManualFocus);
                    }
                    if (afLock != null) {
                        afLock.setSelected(isAfLock);
                    }
                    if (aeLock != null) {
                        aeLock.setSelected(isAeLock);
                    }
                    if ((focusAssist != null) && (imageView != null)) {
                        focusAssist.setSelected(isFocusAssist);
                    }
                    if ((showGrid != null) && (imageView != null)) {
                        showGrid.setSelected(isShowGrid);
                    }
                    if (bracketing != null)
                    {
                        bracketing.setSelected(isTimerOn);
                    }
                    Log.v(TAG, "--- UPDATE CAMERA PROPERTY (END) ---");
                }
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     *
     *
     */
    @Override
    public IMessageDrawer getMessageDrawer()
    {
        return (imageView.getMessageDrawer());
    }

    /**
     *   表示エリアに文字を表示する
     *
     */
    public void updateStatusView(String message)
    {
        messageValue = message;
        runOnUiThread(new Runnable()
        {
            /**
             * カメラの状態(ステータステキスト）を更新する
             * (ステータステキストは、プライベート変数で保持して、書き換える)
             */
            @Override
            public void run()
            {
                if (statusArea != null)
                {
                    statusArea.setText(messageValue);
                }
            }
        });
    }

    @Override
    public void toggleGpsTracking()
    {
        if (locationPicker == null)
        {
            return;
        }
        locationPicker.controlGps(!locationPicker.isTracking());
        updateGpsTrackingStatus();
    }

    @Override
    public void updateGpsTrackingStatus()
    {
        Log.v(TAG, "updateGpsTrackingStatus()");
        if ((myView == null)||(locationPicker == null))
        {
            Log.v(TAG, "updateGpsTrackingStatus() : null");
            return;
        }

        ImageButton gps = myView.findViewById(R.id.gpsLocationButton);
        int id = R.drawable.btn_location_off;
        if (locationPicker.isTracking())
        {
            if (locationPicker.isFixedLocation())
            {
                // 位置が確定している
                id = R.drawable.btn_location_on;
            }
            else
            {
                // 位置検索中だが未確定...
                id = R.drawable.btn_gps_not_fixed;
            }
        }
        else
        {
            // 位置情報をクリアする
            camera.clearGeolocation();
        }
        try
        {
            // ボタンの表示を変える
            gps.setImageResource(id);
            //gps.setImageDrawable(getContext().getResources().getDrawable(id));
            gps.invalidate();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void updateLiveViewMagnifyScale(final boolean isMaxLimit, final float scale)
    {
        if ((liveViewZoomButton == null)||(liveViewMagnify == null))
        {
            // 何もしない
            return;
        }
        try
        {
            // UIスレッドでライブビューの拡大ボタン状態を更新する
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    // ライブビュー拡大ボタンの表示を更新する
                    int id = isMaxLimit ? R.drawable.crop_din : R.drawable.zoom_in;
                    liveViewZoomButton.setImageResource(id);
                    liveViewZoomButton.postInvalidate();

                    // ライブビューの倍率の表示を更新する
                    String magnifyText = "";
                    if (scale > 1.0f)
                    {
                        magnifyText = String.format(Locale.ENGLISH, "x%.1f", scale);
                    }
                    liveViewMagnify.setText(magnifyText);
                    liveViewMagnify.postInvalidate();
                }
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void runOnUiThread(Runnable action)
    {
        Activity activity = getActivity();
        if (activity == null)
        {
            return;
        }
        activity.runOnUiThread(action);
    }


    public boolean handleKeyDown(int keyCode, KeyEvent event)
    {
        if (onTouchListener == null)
        {
            return (false);
        }
        return (onTouchListener.onKey(null, keyCode, event));
    }

    @Override
    public void setGpsFeature(boolean isGps)
    {
        try
        {
            IOlyCameraPropertyProvider propertyProvider = camera.getCameraPropertyProvider();
            if ((propertyProvider != null)&&(camera != null))
            {
                String value = (isGps) ? IOlyCameraProperty.GPS_PROPERTY_ON : IOlyCameraProperty.GPS_PROPERTY_OFF;
                propertyProvider.setCameraPropertyValue(IOlyCameraProperty.GPS, value);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}

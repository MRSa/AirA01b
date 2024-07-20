package jp.osdn.gokigen.aira01b;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.Manifest.permission;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;

import java.util.List;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;
import jp.osdn.gokigen.aira01b.liveview.LiveViewFragment;
import jp.osdn.gokigen.aira01b.logcat.LogCatFragment;
import jp.osdn.gokigen.aira01b.manipulate.ManipulateImageFragment;
import jp.osdn.gokigen.aira01b.olycameraproperty.OlyCameraPropertyListFragment;
import jp.osdn.gokigen.aira01b.olycamerawrapper.ICameraStatusReceiver;
import jp.osdn.gokigen.aira01b.olycamerawrapper.IOLYCameraObjectProvider;
import jp.osdn.gokigen.aira01b.olycamerawrapper.IOlyCameraCoordinator;
import jp.osdn.gokigen.aira01b.olycamerawrapper.OlyCameraCoordinator;
import jp.osdn.gokigen.aira01b.olycamerawrapper.ble.ICameraPowerOn;
import jp.osdn.gokigen.aira01b.playback.ImageGridViewFragment;
import jp.osdn.gokigen.aira01b.preference.ICameraPropertyAccessor;
import jp.osdn.gokigen.aira01b.preference.PreferenceFragment;

/**
 *
 *
 */
public class MainActivity extends AppCompatActivity implements ICameraStatusReceiver, IChangeScene, ICameraPowerOn.PowerOnCameraCallback
{
    /////// OpenCV ///////
    static
    {
        System.loadLibrary("opencv_java3");
    }
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this)
    {
        @Override
        public void onManagerConnected(int status)
        {
            switch (status)
            {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    private final String TAG = this.toString();
    static final int REQUEST_NEED_PERMISSIONS = 1010;
    //static final int REQUEST_NEED_GPS_PERMISSIONS = 1011;

    private IOlyCameraCoordinator olyCameraCoordinator = null;
    private IOLYCameraObjectProvider olyCameraObjectProvider = null;
    private MyInterfaceProvider interfaceFactory = null;

    private LiveViewFragment liveViewFragment = null;
    private LogCatFragment logCatFragment = null;

    /**
     *
     *
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // 画面全体レイアウトの設定
        setContentView(R.layout.activity_main);

        ActionBar bar = getSupportActionBar();
        if (bar != null)
        {
            // タイトルバーは表示しない
            bar.hide();
        }
        try
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        interfaceFactory = new MyInterfaceProvider(this, this, this);
        OlyCameraCoordinator coordinator = new OlyCameraCoordinator(this, interfaceFactory);
        olyCameraCoordinator = coordinator;
        olyCameraObjectProvider = coordinator;
        interfaceFactory.setPropertyProvider(olyCameraCoordinator.getCameraPropertyProvider());

        if ((ContextCompat.checkSelfPermission(this, permission.CAMERA) != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(this, permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)||
                (ContextCompat.checkSelfPermission(this, permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(this, permission.ACCESS_MEDIA_LOCATION) != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(this, permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED)||
                (ContextCompat.checkSelfPermission(this, permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED)||
                (ContextCompat.checkSelfPermission(this, permission.VIBRATE) != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(this, permission.INTERNET) != PackageManager.PERMISSION_GRANTED)||
                (ContextCompat.checkSelfPermission(this, permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(this, permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(this, permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(this, permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED))
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            permission.CAMERA,
                            permission.WRITE_EXTERNAL_STORAGE,
                            permission.ACCESS_NETWORK_STATE,
                            permission.ACCESS_WIFI_STATE,
                            permission.VIBRATE,
                            permission.INTERNET,
                            permission.BLUETOOTH,
                            permission.BLUETOOTH_ADMIN,
                            permission.ACCESS_COARSE_LOCATION,
                            permission.ACCESS_FINE_LOCATION,
                            permission.READ_EXTERNAL_STORAGE,
                            permission.ACCESS_MEDIA_LOCATION,
                    },
                    REQUEST_NEED_PERMISSIONS);
        }

        // ConnectingFragmentを表示する
        changeViewToConnectingFragment();
    }

    /**
     *
     */
    @Override
    protected void onResume()
    {
        super.onResume();
        if (isBlePowerOn())
        {
            // Bluetooth Smart経由でカメラをONにする場合...
            try
            {
                // カメラの電源ONクラスを呼び出しておく (電源ONができたら、コールバックをもらう）
                olyCameraCoordinator.wakeup(this);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            // BLE経由でONしない時は、Wifiの状況を確認に入る。
            if (olyCameraCoordinator != null)
            {
                olyCameraCoordinator.startWatchWifiStatus(this);
            }
        }

        Log.d(TAG, "OpenCV library found inside package. Using it!");
        if (mLoaderCallback != null)
        {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    /**
     *
     */
    @Override
    protected void onPause()
    {
        super.onPause();
        if (olyCameraCoordinator != null)
        {
            olyCameraCoordinator.stopWatchWifiStatus(this);
        }
    }

    /**
     *
     *
     */
    @Override
    public void onStart()
    {
        super.onStart();
    }

    /**
     *
     *
     */
    @Override
    public void onStop()
    {
        super.onStop();
    }


    @Override
    public void onStatusNotify(final String message)
    {
        Log.v(TAG, "onStatusNotify() : " + message);
        try
        {
            List<Fragment> fragments = getSupportFragmentManager().getFragments();
            for (Fragment f : fragments)
            {
                if ((f != null)&&(f.getClass().toString().contains("ConnectingFragment")))
                {
                    final ConnectingFragment target = (ConnectingFragment) f;
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            target.setInformationText(message);
                        }
                    });
                    return;
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onCameraConnected()
    {
        Log.v(TAG, "onCameraConnected()");
        if ((olyCameraCoordinator != null)&&(olyCameraCoordinator.isWatchWifiStatus()))
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    changeViewToLiveViewFragment();
                }
            });
        }
    }

    /**
     *    カメラとの接続が切れた時 ... ConnectingFragmentに切り替える
     *   (CameraCoordinator.ICameraCallback の実装)
     *
     */
    @Override
    public void onCameraDisconnected()
    {
        Log.v(TAG, "onCameraDisconnected()");
        if ((olyCameraCoordinator != null)&&(olyCameraCoordinator.isWatchWifiStatus()))
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    changeViewToConnectingFragment();
                }
            });
        }
    }

    /**
     *    カメラとの接続エラーが発生した時 ... ConnectingFragmentに切り替える
     *   (CameraCoordinator.ICameraCallback の実装)
     *
     * @param message メッセージ
     * @param e  例外
     */
    public void onCameraOccursException(String message, Exception e)
    {
        Log.v(TAG, "onCameraOccursException()");
        alertConnectingFailed(message, e);
        onCameraDisconnected();
    }

    /*
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        Log.v(TAG, "MainActivity::onActivityResult() : " + requestCode);
        if ((requestCode == ICameraPropertyAccessor.CHOICE_SPLASH_SCREEN)&&(resultCode == RESULT_OK)&&(data != null))
        {
            Uri selectedImage = data.getData();
            Log.v(TAG, "Splash Image File : " + selectedImage.toString());
            //setSplashScreenImageFile(selectedImage);
        }
    }
    */

    /**
     *   接続リトライのダイアログを出す
     *
     * @param message 表示用の追加メッセージ
     * @param e 例外
     */
    private void alertConnectingFailed(String message, Exception e)
    {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_title_connect_failed))
                .setMessage(e.getMessage() != null ? "<" + message + "> " + e.getMessage() : message + " : Unknown error")
                .setPositiveButton(getString(R.string.dialog_title_button_retry), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        olyCameraCoordinator.connect();
                    }
                })
                .setNeutralButton(R.string.dialog_title_button_network_settings, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        try
                        {
                            // Wifi 設定画面を表示する
                            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                        }
                        catch (android.content.ActivityNotFoundException ex)
                        {
                            // Activity が存在しなかった...設定画面が起動できなかった
                            Log.v(TAG, "android.content.ActivityNotFoundException...");

                            // この場合は、再試行と等価な動きとする
                            olyCameraCoordinator.connect();
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                });
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                builder.show();
            }
        });
    }

    /**
     *   ConnectingFragmentに表示を切り替える実処理
     */
    private void changeViewToConnectingFragment()
    {
        try
        {
            ConnectingFragment fragment = new ConnectingFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment1, fragment);
            transaction.commitAllowingStateLoss();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     *   LiveViewFragmentに表示を切り替える実処理
     */
    private void changeViewToLiveViewFragment()
    {
        // Activityが再生成されない限りは使いまわすよ。
        //LiveViewFragment liveViewFragment = null;
        if (liveViewFragment == null)
        {
            liveViewFragment = new LiveViewFragment();
        }
        else
        {
            Log.v(TAG, "changeViewToLiveViewFragment() : cancelled");
            return;
        }
        try
        {
            liveViewFragment.setInterfaces(olyCameraCoordinator, interfaceFactory);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment1, liveViewFragment);
            transaction.commitAllowingStateLoss();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     *   OlyCameraPropertyListFragmentに表示を切り替える実処理
     */
    private void changeViewToOlyCameraPropertyListFragment()
    {
        try
        {
            OlyCameraPropertyListFragment fragment = new OlyCameraPropertyListFragment();
            fragment.setInterface(this, interfaceFactory.getPropertyProvider());
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment1, fragment);
            // backstackに追加
            transaction.addToBackStack(null);
            transaction.commit();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     *   PreferenceFragmentに表示を切り替える実処理
     */
    private void changeViewToPreferenceFragment()
    {
        try
        {
            PreferenceFragment fragment = PreferenceFragment.newInstance(this, interfaceFactory, olyCameraCoordinator.getChangeRunModeExecutor());
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment1, fragment);
            // backstackに追加
            transaction.addToBackStack(null);
            transaction.commit();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     *   ImageGridViewFragmentに表示を切り替える実処理
     *
     */
    private void changeViewToImageGridViewFragment()
    {
        try
        {
            ImageGridViewFragment fragment = new ImageGridViewFragment();
            fragment.setCamera(olyCameraObjectProvider.getOLYCamera());
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment1, fragment);
            // backstackに追加
            transaction.addToBackStack(null);
            transaction.commit();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     *   ManipulateImageFragment に表示を切り替える実処理
     *
     */
    private void changeViewToManipulateImageFragment()
    {
        try
        {
            ManipulateImageFragment fragment = new ManipulateImageFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment1, fragment);
            // backstackに追加
            transaction.addToBackStack(null);
            transaction.commit();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void changeSceneToCameraPropertyList()
    {
        changeViewToOlyCameraPropertyListFragment();
    }

    @Override
    public void changeSceneToConfiguration()
    {
        changeViewToPreferenceFragment();
    }

    @Override
    public void changeSceneToPlaybackCamera()
    {
        changeViewToImageGridViewFragment();
    }

    @Override
    public void changeSceneToManipulateImage()
    {
        changeViewToManipulateImageFragment();
    }

    @Override
    public void changeSceneToDebugInformation()
    {
        try
        {
            if (logCatFragment == null)
            {
                logCatFragment = LogCatFragment.newInstance();
            }
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment1, logCatFragment);
            // backstackに追加
            transaction.addToBackStack(null);
            transaction.commit();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void changeSceneToPlaybackPhone()
    {
        // 起動時画面の選択...
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, ICameraPropertyAccessor.CHOICE_SPLASH_SCREEN);
    }

    @Override
    public void exitApplication()
    {
        try
        {
            // カメラの電源をOFFにしたうえで、アプリケーションを終了する。
            olyCameraCoordinator.disconnect(true);
            finish();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    /**
     *  Bluetooth Smartでのカメラ起動シーケンスが終了したとき
     *
     */
    @Override
    public void wakeupExecuted(boolean isExecuted)
    {
        Log.v(TAG, "wakeupExecuted() : " + isExecuted);

        // このタイミングでWifiの状況を確認に入る。
        if (olyCameraCoordinator != null)
        {
            olyCameraCoordinator.startWatchWifiStatus(this);
        }
    }

    /**
     *   BLE経由でカメラの電源を入れるかどうか
     *
     */
    private boolean isBlePowerOn()
    {
        boolean ret = false;
        try
        {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            ret = preferences.getBoolean(ICameraPropertyAccessor.BLE_POWER_ON, false);
            // Log.v(TAG, "isBlePowerOn() : " + ret);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return (ret);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        Log.v(TAG, "onKeyDown()" + " " + keyCode);
        try
        {
            if ((event.getAction() == KeyEvent.ACTION_DOWN)&&
                    ((keyCode == KeyEvent.KEYCODE_VOLUME_UP)||(keyCode == KeyEvent.KEYCODE_CAMERA)))
            {
                if (liveViewFragment != null)
                {
                    return (liveViewFragment.handleKeyDown(keyCode, event));
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return (super.onKeyDown(keyCode, event));
    }
}

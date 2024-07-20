package jp.osdn.gokigen.aira01b

import android.Manifest.permission
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import jp.osdn.gokigen.aira01b.liveview.LiveViewFragment
import jp.osdn.gokigen.aira01b.logcat.LogCatFragment
import jp.osdn.gokigen.aira01b.manipulate.ManipulateImageFragment
import jp.osdn.gokigen.aira01b.olycameraproperty.OlyCameraPropertyListFragment
import jp.osdn.gokigen.aira01b.olycamerawrapper.ICameraStatusReceiver
import jp.osdn.gokigen.aira01b.olycamerawrapper.IOLYCameraObjectProvider
import jp.osdn.gokigen.aira01b.olycamerawrapper.IOlyCameraCoordinator
import jp.osdn.gokigen.aira01b.olycamerawrapper.OlyCameraCoordinator
import jp.osdn.gokigen.aira01b.olycamerawrapper.ble.ICameraPowerOn.PowerOnCameraCallback
import jp.osdn.gokigen.aira01b.playback.ImageGridViewFragment
import jp.osdn.gokigen.aira01b.preference.ICameraPropertyAccessor
import jp.osdn.gokigen.aira01b.preference.PreferenceFragment
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface

/**
 *
 *
 */
class MainActivity : AppCompatActivity(), ICameraStatusReceiver, IChangeScene, PowerOnCameraCallback
{
    private lateinit var olyCameraCoordinator: IOlyCameraCoordinator
    private lateinit var interfaceFactory: MyInterfaceProvider
    private lateinit var olyCameraObjectProvider: IOLYCameraObjectProvider
    private lateinit var liveViewFragment: LiveViewFragment
    private lateinit var logCatFragment: LogCatFragment

    private val mLoaderCallback: BaseLoaderCallback = object : BaseLoaderCallback(this)
    {
        override fun onManagerConnected(status: Int)
        {
            when (status)
            {
                SUCCESS -> {
                    Log.i(TAG, "OpenCV loaded successfully")
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }

    /**
     *
     *
     */
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        // 画面全体レイアウトの設定
        setContentView(R.layout.activity_main)

        val bar = supportActionBar
        bar?.hide()
        try
        {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }

        try
        {
            interfaceFactory = MyInterfaceProvider(this, this, this)
            val coordinator = OlyCameraCoordinator(this, interfaceFactory)
            olyCameraCoordinator = coordinator
            olyCameraObjectProvider = coordinator
            interfaceFactory.propertyProvider = olyCameraCoordinator.getCameraPropertyProvider()
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }

        try
        {
            ///////// SET PERMISSIONS /////////
            Log.v(TAG, " ----- SET PERMISSIONS -----")
            if (!allPermissionsGranted())
            {
                val requestPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {

                    ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_NEED_PERMISSIONS)
                    if(!allPermissionsGranted())
                    {
                        // Abort launch application because required permissions was rejected.
                        Toast.makeText(this, getString(R.string.permission_not_granted), Toast.LENGTH_SHORT).show()
                        Log.v(TAG, "----- APPLICATION LAUNCH ABORTED -----")
                        finish()
                    }
                }
                requestPermission.launch(REQUIRED_PERMISSIONS)
            }
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }

/*
        if ((ContextCompat.checkSelfPermission(
                this,
                permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED) ||
            (ContextCompat.checkSelfPermission(
                this,
                permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED) ||
            (ContextCompat.checkSelfPermission(
                this,
                permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED) ||
            (ContextCompat.checkSelfPermission(
                this,
                permission.ACCESS_MEDIA_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) ||
            (ContextCompat.checkSelfPermission(
                this,
                permission.ACCESS_NETWORK_STATE
            ) != PackageManager.PERMISSION_GRANTED) ||
            (ContextCompat.checkSelfPermission(
                this,
                permission.ACCESS_WIFI_STATE
            ) != PackageManager.PERMISSION_GRANTED) ||
            (ContextCompat.checkSelfPermission(
                this,
                permission.VIBRATE
            ) != PackageManager.PERMISSION_GRANTED) ||
            (ContextCompat.checkSelfPermission(
                this,
                permission.INTERNET
            ) != PackageManager.PERMISSION_GRANTED) ||
            (ContextCompat.checkSelfPermission(
                this,
                permission.BLUETOOTH
            ) != PackageManager.PERMISSION_GRANTED) ||
            (ContextCompat.checkSelfPermission(
                this,
                permission.BLUETOOTH_ADMIN
            ) != PackageManager.PERMISSION_GRANTED) ||
            (ContextCompat.checkSelfPermission(
                this,
                permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) ||
            (ContextCompat.checkSelfPermission(
                this,
                permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED)
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
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
                ),
                REQUEST_NEED_PERMISSIONS
            )
        }
*/
        // ConnectingFragmentを表示する
        changeViewToConnectingFragment()
    }

    private fun allPermissionsGranted() : Boolean
    {
        var result = true
        for (param in REQUIRED_PERMISSIONS)
        {
            if (ContextCompat.checkSelfPermission(
                    baseContext,
                    param
                ) != PackageManager.PERMISSION_GRANTED
            )
            {
                // ----- Permission Denied...
                if ((param == permission.ACCESS_MEDIA_LOCATION)&&(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q))
                {
                    //　この場合は権限付与の判断を除外 (デバイスが (10) よりも古く、ACCESS_MEDIA_LOCATION がない場合）
                }
                else if ((param == permission.READ_EXTERNAL_STORAGE)&&(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU))
                {
                    // この場合は、権限付与の判断を除外 (SDK: 33以上はエラーになる...)
                }
                else if ((param == permission.WRITE_EXTERNAL_STORAGE)&&(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU))
                {
                    // この場合は、権限付与の判断を除外 (SDK: 33以上はエラーになる...)
                }
                else if ((param == permission.BLUETOOTH_SCAN)&&(Build.VERSION.SDK_INT < Build.VERSION_CODES.S))
                {
                    // この場合は、権限付与の判断を除外 (SDK: 31よりも下はエラーになるはず...)
                    Log.v(TAG, "BLUETOOTH_SCAN")
                }
                else if ((param == permission.BLUETOOTH_CONNECT)&&(Build.VERSION.SDK_INT < Build.VERSION_CODES.S))
                {
                    // この場合は、権限付与の判断を除外 (SDK: 31よりも下はエラーになるはず...)
                    Log.v(TAG, "BLUETOOTH_CONNECT")
                }
                else
                {
                    // ----- 権限が得られなかった場合...
                    Log.v(TAG, " Permission: $param : ${Build.VERSION.SDK_INT}")
                    result = false
                }
            }
        }
        return (result)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.v(TAG, "------------------------- onRequestPermissionsResult() ")
        if (requestCode == REQUEST_NEED_PERMISSIONS)
        {
            if (allPermissionsGranted())
            {
                // ----- 権限が有効だった、最初の画面を開く
                Log.v(TAG, "onRequestPermissionsResult()")
                // ConnectingFragmentを表示する
                changeViewToConnectingFragment()
            }
            else
            {
                Log.v(TAG, "----- onRequestPermissionsResult() : false")
                Toast.makeText(this, getString(R.string.permission_not_granted), Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onResume()
    {
        super.onResume()
        try
        {
            if (isBlePowerOn)
            {
                // Bluetooth Smart経由でカメラをONにする場合...
                try
                {
                    // カメラの電源ONクラスを呼び出しておく (電源ONができたら、コールバックをもらう）
                    olyCameraCoordinator.wakeup(this)
                }
                catch (e: Exception)
                {
                    e.printStackTrace()
                }
            }
            else
            {
                // BLE経由でONしない時は、Wifiの状況を確認に入る。
                olyCameraCoordinator.startWatchWifiStatus(this)
            }
            Log.d(TAG, "OpenCV library found inside package. Using it!")
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    /**
     *
     */
    override fun onPause()
    {
        super.onPause()
        try
        {
            olyCameraCoordinator.stopWatchWifiStatus(this)
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
     }

    public override fun onStart() { super.onStart() }

    public override fun onStop() { super.onStop() }

    override fun onStatusNotify(message: String)
    {
        Log.v(TAG, "onStatusNotify() : $message")
        try
        {
            val fragments = supportFragmentManager.fragments
            for (f in fragments)
            {
                if ((f != null) && (f.javaClass.toString().contains("ConnectingFragment")))
                {
                    val target = f as ConnectingFragment
                    runOnUiThread { target.setInformationText(message) }
                    return
                }
            }
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    override fun onCameraConnected()
    {
        Log.v(TAG, "onCameraConnected()")
        try
        {
            if (olyCameraCoordinator.isWatchWifiStatus)
            {
                runOnUiThread { changeViewToLiveViewFragment() }
            }
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    /**
     * カメラとの接続が切れた時 ... ConnectingFragmentに切り替える
     * (CameraCoordinator.ICameraCallback の実装)
     *
     */
    override fun onCameraDisconnected()
    {
        Log.v(TAG, "onCameraDisconnected()")
        try
        {
            if (olyCameraCoordinator.isWatchWifiStatus)
            {
                runOnUiThread { changeViewToConnectingFragment() }
            }
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    /**
     * カメラとの接続エラーが発生した時 ... ConnectingFragmentに切り替える
     * (CameraCoordinator.ICameraCallback の実装)
     *
     * @param message メッセージ
     * @param e  例外
     */
    override fun onCameraOccursException(message: String, e: Exception)
    {
        Log.v(TAG, "onCameraOccursException()")
        try
        {
            alertConnectingFailed(message, e)
            onCameraDisconnected()
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    /**
     * 接続リトライのダイアログを出す
     *
     * @param message 表示用の追加メッセージ
     * @param e 例外
     */
    private fun alertConnectingFailed(message: String, e: Exception)
    {
        val builder = AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_title_connect_failed))
            .setMessage(if (e.message != null) "<" + message + "> " + e.message else "$message : Unknown error")
            .setPositiveButton(
                getString(R.string.dialog_title_button_retry)
            ) { _, _ -> olyCameraCoordinator.connect() }
            .setNeutralButton(
                R.string.dialog_title_button_network_settings
            ) { _, _ ->
                try
                {
                    // Wifi 設定画面を表示する
                    startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                }
                catch (ex: ActivityNotFoundException)
                {
                    // Activity が存在しなかった...設定画面が起動できなかった
                    Log.v(TAG, "android.content.ActivityNotFoundException...")

                    // この場合は、再試行と等価な動きとする
                    olyCameraCoordinator.connect()
                }
                catch (e: Exception)
                {
                    e.printStackTrace()
                }
            }
        runOnUiThread { builder.show() }
    }

    /**
     * ConnectingFragmentに表示を切り替える実処理
     */
    private fun changeViewToConnectingFragment()
    {
        try
        {
            val fragment = ConnectingFragment()
            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment1, fragment)
            transaction.commitAllowingStateLoss()
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    /**
     * LiveViewFragmentに表示を切り替える実処理
     */
    private fun changeViewToLiveViewFragment()
    {
        try
        {
            // Activityが再生成されない限りは使いまわす
            if (!::liveViewFragment.isInitialized)
            {
                liveViewFragment = LiveViewFragment()
            }
            else
            {
                Log.v(TAG, "changeViewToLiveViewFragment() : cancelled")
                return
            }
            liveViewFragment.setInterfaces(olyCameraCoordinator, interfaceFactory)
            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment1, liveViewFragment)
            transaction.commitAllowingStateLoss()
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    /**
     * OlyCameraPropertyListFragmentに表示を切り替える実処理
     */
    private fun changeViewToOlyCameraPropertyListFragment()
    {
        try
        {
            val fragment = OlyCameraPropertyListFragment()
            fragment.setInterface(this, interfaceFactory.propertyProvider)
            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment1, fragment)
            // backstackに追加
            transaction.addToBackStack(null)
            transaction.commit()
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    /**
     * PreferenceFragmentに表示を切り替える実処理
     */
    private fun changeViewToPreferenceFragment()
    {
        try
        {
            val fragment = PreferenceFragment.newInstance(
                this,
                interfaceFactory,
                olyCameraCoordinator.changeRunModeExecutor
            )
            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment1, fragment)
            // backstackに追加
            transaction.addToBackStack(null)
            transaction.commit()
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    /**
     * ImageGridViewFragmentに表示を切り替える実処理
     *
     */
    private fun changeViewToImageGridViewFragment()
    {
        try
        {
            val fragment = ImageGridViewFragment()
            fragment.setCamera(olyCameraObjectProvider.olyCamera)
            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment1, fragment)
            // backstackに追加
            transaction.addToBackStack(null)
            transaction.commit()
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    /**
     * ManipulateImageFragment に表示を切り替える実処理
     *
     */
    private fun changeViewToManipulateImageFragment()
    {
        try
        {
            val fragment = ManipulateImageFragment()
            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment1, fragment)
            // backstackに追加
            transaction.addToBackStack(null)
            transaction.commit()
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    override fun changeSceneToCameraPropertyList()
    {
        changeViewToOlyCameraPropertyListFragment()
    }

    override fun changeSceneToConfiguration()
    {
        changeViewToPreferenceFragment()
    }

    override fun changeSceneToPlaybackCamera()
    {
        changeViewToImageGridViewFragment()
    }

    override fun changeSceneToManipulateImage()
    {
        changeViewToManipulateImageFragment()
    }

    override fun changeSceneToDebugInformation()
    {
        try
        {
            if (!::logCatFragment.isInitialized)
            {
                logCatFragment = LogCatFragment.newInstance()
            }
            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment1, logCatFragment)
            // backstackに追加
            transaction.addToBackStack(null)
            transaction.commit()
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    override fun changeSceneToPlaybackPhone()
    {
        // 起動時画面の選択...
        try
        {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, ICameraPropertyAccessor.CHOICE_SPLASH_SCREEN)
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    override fun exitApplication()
    {
        try
        {
            // カメラの電源をOFFにしたうえで、アプリケーションを終了する。
            olyCameraCoordinator.disconnect(true)
            finish()
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    /**
     * Bluetooth Smartでのカメラ起動シーケンスが終了したとき
     *
     */
    override fun wakeupExecuted(isExecuted: Boolean)
    {
        Log.v(TAG, "wakeupExecuted() : $isExecuted")

        // このタイミングでWifiの状況を確認に入る。
        try
        {
            olyCameraCoordinator.startWatchWifiStatus(this)
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    /**
     * BLE経由でカメラの電源を入れるかどうか
     *
     */
    private val isBlePowerOn: Boolean
        get() {
            var ret = false
            try
            {
                val preferences = PreferenceManager.getDefaultSharedPreferences(this)
                ret = preferences.getBoolean(ICameraPropertyAccessor.BLE_POWER_ON, false)
                // Log.v(TAG, "isBlePowerOn() : " + ret);
            }
            catch (e: Exception)
            {
                e.printStackTrace()
            }
            return (ret)
        }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean
    {
        Log.v(TAG, "onKeyDown() $keyCode")
        try
        {
            if ((event.action == KeyEvent.ACTION_DOWN) && ((keyCode == KeyEvent.KEYCODE_VOLUME_UP) || (keyCode == KeyEvent.KEYCODE_CAMERA)))
            {
                if (::liveViewFragment.isInitialized)
                {
                    return (liveViewFragment.handleKeyDown(keyCode, event))
                }
            }
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
        return (super.onKeyDown(keyCode, event))
    }

    companion object
    {
        private val TAG = MainActivity::class.java.simpleName

        /////// Load OpenCV ///////
        init {
            System.loadLibrary("opencv_java3")
        }

        const val REQUEST_NEED_PERMISSIONS: Int = 1010
        private val REQUIRED_PERMISSIONS = arrayOf(
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
            permission.BLUETOOTH_CONNECT,
            permission.BLUETOOTH_SCAN,
        )
    }
}
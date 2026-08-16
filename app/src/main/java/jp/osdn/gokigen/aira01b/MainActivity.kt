package jp.osdn.gokigen.aira01b

import android.Manifest.permission
import android.content.ActivityNotFoundException
import android.content.Context
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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
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
import org.opencv.android.OpenCVLoader

class MainActivity : AppCompatActivity(), ICameraStatusReceiver, IChangeScene, PowerOnCameraCallback
{
    private lateinit var olyCameraCoordinator: IOlyCameraCoordinator
    private lateinit var interfaceFactory: MyInterfaceProvider
    private lateinit var olyCameraObjectProvider: IOLYCameraObjectProvider

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        setupActivityResultLaunchers()
        setContentView(R.layout.activity_main)

        // OpenCVのロード
        if (OpenCVLoader.initLocal())
        {
            Log.i(TAG, "OpenCV loaded successfully")
        }
        else
        {
            Log.e(TAG, "OpenCV initialization failed!")
            Toast.makeText(this, R.string.load_opencv_failure, Toast.LENGTH_LONG).show()
        }

        supportActionBar?.hide()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setupWindowInset(findViewById(R.id.base_layout))

        val coordinator = OlyCameraCoordinator(this, MyInterfaceProvider(this, this, this).also { interfaceFactory = it })
        olyCameraCoordinator = coordinator
        olyCameraObjectProvider = coordinator
        interfaceFactory.propertyProvider = olyCameraCoordinator.getCameraPropertyProvider()

        if (savedInstanceState == null)
        {
            if (!allPermissionsGranted())
            {
                requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
            }
            else
            {
                changeViewToConnectingFragment()
            }
        }
    }

    private fun setupActivityResultLaunchers()
    {
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            if (allPermissionsGranted())
            {
                Log.v(TAG, "All permissions granted.")
                changeViewToConnectingFragment()
            }
            else
            {
                Toast.makeText(this, getString(R.string.permission_not_granted), Toast.LENGTH_SHORT).show()
                Log.v(TAG, "----- APPLICATION LAUNCH ABORTED -----")
                finish()
            }
        }

        pickImageLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK)
            {
                val data: Intent? = result.data
                Log.v(TAG, "Image picked: ${data?.data}")
            }
        }
    }

    private fun setupWindowInset(view: android.view.View)
    {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            v.updatePadding(
                left = bars.left,
                top = bars.top,
                right = bars.right,
                bottom = bars.bottom,
            )
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun allPermissionsGranted(): Boolean
    {
        for (param in REQUIRED_PERMISSIONS)
        {
            if (ContextCompat.checkSelfPermission(baseContext, param) != PackageManager.PERMISSION_GRANTED)
            {
                val isGrantedByOSVersion = when (param)
                {
                    permission.ACCESS_MEDIA_LOCATION -> Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                    permission.READ_EXTERNAL_STORAGE,
                    permission.WRITE_EXTERNAL_STORAGE -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    permission.BLUETOOTH_SCAN,
                    permission.BLUETOOTH_CONNECT -> Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                    permission.NEARBY_WIFI_DEVICES -> Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                    permission.ACCESS_LOCAL_NETWORK -> Build.VERSION.SDK_INT < Build.VERSION_CODES.CINNAMON_BUN
                    permission.BLUETOOTH -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    permission.BLUETOOTH_ADMIN -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    permission.ACCESS_FINE_LOCATION -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    permission.ACCESS_COARSE_LOCATION -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    else -> false
                }

                if (!isGrantedByOSVersion)
                {
                    Log.v(TAG, " Permission denied: $param (SDK: ${Build.VERSION.SDK_INT})")
                    return false
                }
            }
        }
        return true
    }

    override fun onResume()
    {
        super.onResume()
        if (isBlePowerOn)
        {
            olyCameraCoordinator.wakeup(this)
        }
        else
        {
            olyCameraCoordinator.startWatchWifiStatus(this)
        }
    }

    override fun onPause()
    {
        super.onPause()
        olyCameraCoordinator.stopWatchWifiStatus(this)
    }

    override fun onStatusNotify(message: String)
    {
        Log.v(TAG, "onStatusNotify() : $message")
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment1)
        if (fragment is ConnectingFragment)
        {
            runOnUiThread { fragment.setInformationText(message) }
        }
    }

    override fun onCameraConnected()
    {
        Log.v(TAG, "onCameraConnected()")
        if (olyCameraCoordinator.isWatchWifiStatus)
        {
            runOnUiThread { changeViewToLiveViewFragment() }
        }
    }

    override fun onCameraDisconnected()
    {
        Log.v(TAG, "onCameraDisconnected()")
        if (olyCameraCoordinator.isWatchWifiStatus)
        {
            runOnUiThread { changeViewToConnectingFragment() }
        }
    }

    override fun onCameraOccursException(message: String, e: Exception)
    {
        Log.v(TAG, "onCameraOccursException()")
        alertConnectingFailed(message, e)
        onCameraDisconnected()
    }

    private fun alertConnectingFailed(message: String, e: Exception)
    {
        val builder = AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_title_connect_failed))
            .setMessage(if (e.message != null) "<$message> ${e.message}" else "$message : Unknown error")
            .setPositiveButton(getString(R.string.dialog_title_button_retry)) { _, _ ->
                olyCameraCoordinator.connect()
            }
            .setNeutralButton(R.string.dialog_title_button_network_settings) { _, _ ->
                try
                {
                    startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                }
                catch (_: ActivityNotFoundException)
                {
                    Log.v(TAG, "ActivityNotFoundException for WIFI Settings")
                    olyCameraCoordinator.connect()
                }
            }
        runOnUiThread { builder.show() }
    }

    private fun changeViewToConnectingFragment()
    {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment1, ConnectingFragment())
            .commit()
    }

    private fun changeViewToLiveViewFragment()
    {
        val fragment = (supportFragmentManager.findFragmentByTag(TAG_LIVE_VIEW) as? LiveViewFragment)
            ?: LiveViewFragment()

        fragment.setInterfaces(olyCameraCoordinator, interfaceFactory)

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment1, fragment, TAG_LIVE_VIEW)
            .commit()
    }

    private fun changeViewToOlyCameraPropertyListFragment()
    {
        val fragment = OlyCameraPropertyListFragment()
        fragment.setInterface(this, interfaceFactory.propertyProvider)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment1, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun changeViewToPreferenceFragment()
    {
        val fragment = PreferenceFragment.newInstance(
            this,
            interfaceFactory,
            olyCameraCoordinator.changeRunModeExecutor
        )
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment1, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun changeViewToImageGridViewFragment()
    {
        val fragment = ImageGridViewFragment()
        fragment.setCamera(olyCameraObjectProvider.olyCamera)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment1, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun changeViewToManipulateImageFragment()
    {
        val fragment = ManipulateImageFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment1, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun changeSceneToCameraPropertyList() = changeViewToOlyCameraPropertyListFragment()
    override fun changeSceneToConfiguration() = changeViewToPreferenceFragment()
    override fun changeSceneToPlaybackCamera() = changeViewToImageGridViewFragment()
    override fun changeSceneToManipulateImage() = changeViewToManipulateImageFragment()

    override fun changeSceneToDebugInformation()
    {
        val fragment = (supportFragmentManager.findFragmentByTag(TAG_LOG_CAT) as? LogCatFragment)
            ?: LogCatFragment.newInstance()

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment1, fragment, TAG_LOG_CAT)
            .addToBackStack(null)
            .commit()
    }

    override fun changeSceneToPlaybackPhone()
    {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    override fun exitApplication()
    {
        olyCameraCoordinator.disconnect(true)
        finish()
    }

    override fun wakeupExecuted(isExecuted: Boolean)
    {
        Log.v(TAG, "wakeupExecuted() : $isExecuted")
        olyCameraCoordinator.startWatchWifiStatus(this)
    }

    private val isBlePowerOn: Boolean
        get() {
            val preferences = getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE)
            return preferences.getBoolean(ICameraPropertyAccessor.BLE_POWER_ON, false)
        }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean
    {
        Log.v(TAG, "onKeyDown() $keyCode")
        if (event.action == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_CAMERA))
        {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment1)
            if (currentFragment is LiveViewFragment)
            {
                return currentFragment.handleKeyDown(keyCode, event)
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    companion object
    {
        private val TAG = MainActivity::class.java.simpleName
        private const val TAG_LIVE_VIEW = "TAG_LIVE_VIEW"
        private const val TAG_LOG_CAT = "TAG_LOG_CAT"

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
            permission.ACCESS_LOCAL_NETWORK,
            permission.NEARBY_WIFI_DEVICES
        )
    }
}

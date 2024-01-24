package jp.osdn.gokigen.aira01b.liveview.phonecamera;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PreviewCallback;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import java.util.List;

import jp.osdn.gokigen.aira01b.preference.ICameraPropertyAccessor;

public class PhoneCameraView extends SurfaceView implements SurfaceHolder.Callback, PreviewCallback, IPhoneCameraShutter, Camera.ShutterCallback, ViewTreeObserver.OnGlobalLayoutListener
{
    static final int PREVIEW_WIDTH = 640;
    static final int PREVIEW_HEIGHT = 480;

    private final String TAG = toString();

    private Camera                 camera = null;
    private IPhoneCameraDataReceiver cameraDataReceiver = null;
    private boolean               takingPicture = false;
    private boolean               isWaitShutter = false;
    private boolean               isWaitJpegSave = false;
    private final PhoneCameraJpegSave jpegSaver;
    private final Camera.AutoFocusCallback afCallback;
    private SharedPreferences preferences = null;
    private Location currentLocation = null;

    public PhoneCameraView(Context context)
    {
        super(context);
        initializeSelf(context, null);
        jpegSaver = new PhoneCameraJpegSave(context, this);
        afCallback = new PhoneCameraAutoFocusCallback();
    }

    public PhoneCameraView(Context context, AttributeSet attrs)
    {
        super(context, attrs);        
        initializeSelf(context, attrs);
        jpegSaver = new PhoneCameraJpegSave(context, this);
        afCallback = new PhoneCameraAutoFocusCallback();
    }

    private void initializeSelf(Context context, AttributeSet attrs)
    {
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void surfaceCreated(SurfaceHolder aHolder)
    {
        synchronized (this)
        {
            try
            {
                int cameraId = Integer.parseInt(preferences.getString(ICameraPropertyAccessor.PHONE_CAMERA_ID, ICameraPropertyAccessor.PHONE_CAMERA_ID_DEFAULT_VALUE));
                int rotation = Integer.parseInt(preferences.getString(ICameraPropertyAccessor.PHONE_CAMERA_ROTATION, ICameraPropertyAccessor.PHONE_CAMERA_ROTATION_DEFAULT_VALUE));
                camera = Camera.open(cameraId);
                setCameraDisplayOrientation(rotation, cameraId, camera);
                camera.setPreviewDisplay(aHolder);

                Log.v(TAG, "cameraId: " + cameraId + ", rotation: " + rotation);
                Camera.Parameters parameters = camera.getParameters();
                List<Camera.Size> sizeList = parameters.getSupportedPictureSizes();
                int width = 0;
                int height = 0;
                for (Camera.Size sizes : sizeList)
                {
                    Log.v(TAG, "SIZE : (" + sizes.width + "," + sizes.height + ")");
                    if (sizes.width > width)
                    {
                        width = sizes.width;
                        height = sizes.height;
                    }
                }
                parameters.setPictureSize(width, height);
                parameters.setPreviewSize(PREVIEW_WIDTH, PREVIEW_HEIGHT);
                Log.v(TAG, "(" + getWidth() + "," + getHeight() + ")");

                camera.setParameters(parameters);

                camera.startPreview();
        		camera.setPreviewCallback(this);
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }
    }

    public static void setCameraDisplayOrientation(int rotation, int cameraId, Camera camera)
    {

        CameraInfo info = new CameraInfo();
        Camera.getCameraInfo(cameraId, info);

        int degrees = 0;
        switch (rotation)
        {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
            default: break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
        {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        }
        else
        {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    public void setPreviewCallback(IPhoneCameraDataReceiver callback)
    {
    	cameraDataReceiver = callback;
    }

    public void surfaceChanged(SurfaceHolder aHolder, int format, int width, int height)
    {
        //
    }

    public void surfaceDestroyed(SurfaceHolder aHolder)
    {
        synchronized (this)
        {
            try
            {
                camera.setPreviewCallback(null);
                camera.stopPreview();
                camera.release();
                camera = null;
                cameraDataReceiver = null;
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
            System.gc();
        }
    }

    public void onPreviewFrame(byte[] arg0, Camera arg1)
	{
        if (cameraDataReceiver != null)
        {
        	cameraDataReceiver.onPreviewFrame(arg0, arg1);
        }
	}

    /**
     *   現在の位置情報をもらう
     */
    @Override
    public void setCurrentLocation(Location location)
    {
        currentLocation = location;
    }

    /**
     *   シャッターボタンが押された
     *
     */
    @Override
    public void onPressedPhoneShutter(boolean withGeolocation)
    {
        if (!takingPicture)
        {
            Log.v(TAG, "PhoneCameraView::onPressedPhoneShutter() : takePicture()");

            try
            {
                // 位置情報をつけて保存する
                jpegSaver.setCurrentLocation(currentLocation);
                Camera.Parameters parameters = camera.getParameters();
                if ((withGeolocation)&&(currentLocation != null))
                {
                    parameters.setGpsAltitude(currentLocation.getAltitude());
                    parameters.setGpsLatitude(currentLocation.getLatitude());
                    parameters.setGpsLongitude(currentLocation.getLongitude());
                    parameters.setGpsTimestamp(currentLocation.getTime());
                }
                else
                {
                    parameters.removeGpsData();
                }
                camera.setParameters(parameters);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            camera.takePicture(this, null, null, jpegSaver);
            isWaitJpegSave = true;
            isWaitShutter = true;
            takingPicture = true;
        }
    }

    /**
     *   撮影が終了していたら、プレビューを再開させる
     *
     */
    private void checkTakingPicture()
    {
        takingPicture = (isWaitJpegSave)||(isWaitShutter);
        if ((!takingPicture)&&(camera != null))
        {
            camera.startPreview();
        }
    }

    /**
     *  シャッター処理が終了した
     *
     */
    @Override
    public void onShutter()
    {
        Log.v(TAG, "PhoneCameraView::onShutter()");
        isWaitShutter = false;
        checkTakingPicture();

        currentLocation = null;
    }

    /**
     *   画像保管が終了した
     *
     */
    @Override
    public void onSavedPicture(boolean isSuccess)
    {
        Log.v(TAG, "PhoneCameraView::onSavedPicture() : " + isSuccess);
        isWaitJpegSave = false;
        checkTakingPicture();
    }

    /**
     *   プレビュー領域がタッチされた（オートフォーカスの実行）
     *
     */
    public void onTouchedPreviewArea()
    {
        Log.v(TAG, "PhoneCameraView::onTouchedPreviewArea() ");
        try
        {
            camera.autoFocus(afCallback);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     *   レイアウト調整
     *
     */
    @Override
    public void onGlobalLayout()
    {
        ViewGroup.LayoutParams layoutParams = getLayoutParams();

        // 高さをベースに画像サイズを決定する
        layoutParams.width = getHeight() * PREVIEW_WIDTH / PREVIEW_HEIGHT;
        layoutParams.height = getHeight();

        Log.v(TAG, "onGlobalLayout() [" +  getWidth() + "," + getHeight() + "] => (" +  layoutParams.width + "," + layoutParams.height + ")");

        this.getViewTreeObserver().removeGlobalOnLayoutListener(this);
    }
}

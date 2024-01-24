package jp.osdn.gokigen.aira01b.olycamerawrapper.takepicture;

import android.graphics.RectF;
import android.util.Log;

import java.util.HashMap;
import jp.co.olympus.camerakit.OLYCamera;
import jp.co.olympus.camerakit.OLYCameraAutoFocusResult;
import jp.osdn.gokigen.aira01b.IAirA01BInterfacesProvider;
import jp.osdn.gokigen.aira01b.liveview.IAutoFocusFrameDisplay;
import jp.osdn.gokigen.aira01b.olycamerawrapper.IIndicatorControl;

/**
 *   一枚撮影用のクラス
 *
 * Created by MRSa on 2016/06/18.
 */
public class SingleShotControl implements OLYCamera.TakePictureCallback
{
    private final String TAG = toString();
    private final OLYCamera camera;
    private final IAirA01BInterfacesProvider provider;
    private final IIndicatorControl indicator;

    private IAutoFocusFrameDisplay frameDisplayer = null;

    /**
     *  コンストラクタ
     *
     */
    public SingleShotControl(OLYCamera camera, IAirA01BInterfacesProvider provider, IIndicatorControl indicator)
    {
        this.camera = camera;
        this.provider = provider;
        this.indicator = indicator;
    }

    /**
     *   1枚撮影する
     *
     */
    public void singleShot()
    {
        try
        {
            if (frameDisplayer == null)
            {
                frameDisplayer = provider.getAutoFocusFrameInterface();
            }
            camera.takePicture(new HashMap<String, Object>(), this);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onProgress(OLYCamera olyCamera, OLYCamera.TakingProgress takingProgress, OLYCameraAutoFocusResult olyCameraAutoFocusResult)
    {
        if (takingProgress != OLYCamera.TakingProgress.EndFocusing)
        {
            return;
        }

        String result = olyCameraAutoFocusResult.getResult();
        if (result == null)
        {
            Log.v(TAG, "FocusResult is null.");
        }
        else switch (result)
        {
            case "ok":
                RectF postFocusFrameRect = olyCameraAutoFocusResult.getRect();
                if (postFocusFrameRect != null)
                {
                    showFocusFrame(postFocusFrameRect, IAutoFocusFrameDisplay.FocusFrameStatus.Focused, 0.0);
                }
                break;

            case "none":
            default:
                hideFocusFrame();
                break;
        }
    }

    @Override
    public void onCompleted()
    {
        try
        {
            camera.clearAutoFocusPoint();
            hideFocusFrame();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onErrorOccurred(Exception e)
    {
        try
        {
            camera.clearAutoFocusPoint();
            hideFocusFrame();
        }
        catch (Exception ee)
        {
            ee.printStackTrace();
        }
        e.printStackTrace();
        //control.presentMessage(R.string.shutter_control_take_failed, e.getMessage());
    }

    private void showFocusFrame(RectF rect, IAutoFocusFrameDisplay.FocusFrameStatus status, double duration)
    {
        if (frameDisplayer != null)
        {
            frameDisplayer.showFocusFrame(rect, status, duration);
        }
        indicator.onAfLockUpdate(IAutoFocusFrameDisplay.FocusFrameStatus.Focused == status);
    }

    private void hideFocusFrame()
    {
        if (frameDisplayer != null)
        {
            frameDisplayer.hideFocusFrame();
        }
        indicator.onAfLockUpdate(false);
    }
}

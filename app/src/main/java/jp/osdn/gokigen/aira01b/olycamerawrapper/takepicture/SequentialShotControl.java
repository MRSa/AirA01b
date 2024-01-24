package jp.osdn.gokigen.aira01b.olycamerawrapper.takepicture;

import android.content.Context;
import android.graphics.Color;
import android.graphics.RectF;
import android.util.Log;

import jp.co.olympus.camerakit.OLYCamera;
import jp.co.olympus.camerakit.OLYCameraAutoFocusResult;
import jp.osdn.gokigen.aira01b.IAirA01BInterfacesProvider;
import jp.osdn.gokigen.aira01b.R;
import jp.osdn.gokigen.aira01b.liveview.IAutoFocusFrameDisplay;
import jp.osdn.gokigen.aira01b.liveview.IMessageDrawer;
import jp.osdn.gokigen.aira01b.liveview.IStatusViewDrawer;
import jp.osdn.gokigen.aira01b.olycamerawrapper.IIndicatorControl;

/**
 *   連続撮影用のクラス
 *
 */
public class SequentialShotControl implements OLYCamera.TakePictureCallback
{
    private final String TAG = toString();
    private final Context context;
    private final OLYCamera camera;
    private final IAirA01BInterfacesProvider provider;
    private final IIndicatorControl indicator;
    private IAutoFocusFrameDisplay frameDisplayer = null;

    // 撮影状態の記録
    private enum shootingStatus
    {
        Unknown,
        Starting,
        Stopping,
    }
    private shootingStatus currentStatus = shootingStatus.Unknown;

    /**
     *   コンストラクタ
     *
     */
    public SequentialShotControl(Context context, OLYCamera camera, IAirA01BInterfacesProvider provider, IIndicatorControl indicator)
    {
        this.context = context;
        this.camera = camera;
        this.provider = provider;
        this.indicator = indicator;
    }

    /**
     *   撮影の開始と終了
     *
     */
    public void shotControl()
    {
        if (camera.isRecordingVideo())
        {
            // ビデオ撮影中の場合は、何もしない（モード異常なので）
            return;
        }
        try
        {
            if (frameDisplayer == null)
            {
                frameDisplayer = provider.getAutoFocusFrameInterface();
            }
            if (!camera.isTakingPicture())
            {
                // 連続撮影の開始
                currentStatus = shootingStatus.Starting;
                camera.startTakingPicture(null, this);
            }
            else
            {
                // 連続撮影の終了
                currentStatus = shootingStatus.Stopping;
                camera.stopTakingPicture(this);
            }
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
    public void onProgress(OLYCamera olyCamera, OLYCamera.TakingProgress takingProgress, OLYCameraAutoFocusResult olyCameraAutoFocusResult)
    {
        if (currentStatus == shootingStatus.Stopping)
        {
            // 終了中の時にはなにもしない
            return;
        }

        // 撮影中の更新処理
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

    /**
     *
     *
     */
    @Override
    public void onCompleted()
    {
        Log.v(TAG, "SequentialShotControl::onCompleted()");
        IStatusViewDrawer statusDrawer = provider.getStatusViewDrawer();
        if (statusDrawer != null)
        {
            if (currentStatus == shootingStatus.Starting)
            {
                statusDrawer.getMessageDrawer().setMessageToShow(IMessageDrawer.MessageArea.UPCENTER, Color.GREEN, IMessageDrawer.SIZE_LARGE, context.getString(R.string.taking_picture));
            }
            else
            {
                statusDrawer.getMessageDrawer().setMessageToShow(IMessageDrawer.MessageArea.UPCENTER, Color.WHITE, IMessageDrawer.SIZE_STD, "");
            }
        }
        if (currentStatus != shootingStatus.Stopping)
        {
            // 撮影停止中以外ではなにもしない。
            return;
        }
        try
        {
            camera.clearAutoFocusPoint();
            hideFocusFrame();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        currentStatus = shootingStatus.Unknown;
    }

    /**
     *
     *
     */
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
        currentStatus = shootingStatus.Unknown;
    }

    /**
     *
     *
     */
    private void showFocusFrame(RectF rect, IAutoFocusFrameDisplay.FocusFrameStatus status, double duration)
    {
        if (frameDisplayer != null)
        {
            frameDisplayer.showFocusFrame(rect, status, duration);
        }
        indicator.onAfLockUpdate(IAutoFocusFrameDisplay.FocusFrameStatus.Focused == status);
    }

    /**
     *
     *
     */
    private void hideFocusFrame()
    {
        if (frameDisplayer != null)
        {
            frameDisplayer.hideFocusFrame();
        }
        indicator.onAfLockUpdate(false);
    }

}

package jp.osdn.gokigen.aira01b.olycamerawrapper;

import android.util.Log;

import jp.co.olympus.camerakit.OLYCamera;

/**
 *   ズームレンズの情報を保持する
 *
 */
class ZoomLensHolder implements IZoomLensHolder
{
    private final String TAG = toString();
    private boolean canZoom = false;
    private float minimumLength = 0.0f;
    private float maximumLength = 0.0f;
    private float currentLength = 0.0f;
    private final OLYCamera camera;

    ZoomLensHolder(OLYCamera camera)
    {
        this.camera = camera;
        initialize();
    }

    private void initialize()
    {
        canZoom = ((camera != null)&&(camera.getLensMountStatus()).contains("electriczoom"));
        if (canZoom)
        {
            try
            {
                minimumLength = camera.getMinimumFocalLength();
                maximumLength = camera.getMaximumFocalLength();
                currentLength = camera.getActualFocalLength();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean canZoom()
    {
        return (canZoom);
    }

    @Override
    public void updateStatus()
    {
        initialize();
    }

    @Override
    public float getMaximumFocalLength()
    {
        return (maximumLength);
    }

    @Override
    public float getMinimumFocalLength()
    {
        return (minimumLength);
    }

    @Override
    public float getCurrentFocalLength()
    {
        return (currentLength);
    }

    /**
     * ズームレンズを動作させる
     *
     * @param targetLength  焦点距離
     */
    @Override
    public void driveZoomLens(float targetLength)
    {
        try
        {
            // レンズがサポートする焦点距離と、現在の焦点距離を取得する
            float targetFocalLength = targetLength;

            // 焦点距離が最大値・最小値を超えないようにする
            if (targetFocalLength > maximumLength)
            {
                targetFocalLength = maximumLength;
            }
            if (targetFocalLength < minimumLength)
            {
                targetFocalLength = minimumLength;
            }

            // レンズのスーム操作
            Log.v(TAG, "ZOOM from " + currentLength + "mm to " + targetFocalLength + "mm");

            // ズーム動作中でない時には、レンズをズームさせる
            if (!camera.isDrivingZoomLens())
            {
                camera.startDrivingZoomLensToFocalLength(targetFocalLength);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * 現在ズーム中か確認する
     *
     * @return true : ズーム中  / false : ズーム中でない
     */
    @Override
    public boolean isDrivingZoomLens()
    {
        return  ((camera != null)&&(camera.isDrivingZoomLens()));
    }


}

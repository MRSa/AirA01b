package jp.osdn.gokigen.aira01b.liveview;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Map;

import jp.co.olympus.camerakit.OLYCamera;
import jp.co.olympus.camerakit.OLYCameraLiveViewListener;
import jp.osdn.gokigen.aira01b.CachedImageData;

/**
 *  OLYCameraLiveViewListener の実装
 *  （LiveViewFragment用）
 *
 */
public class CameraLiveViewListenerImpl implements OLYCameraLiveViewListener, IBufferedImageHolder
{
    private final String TAG = this.toString();
    private IImageDataReceiver imageView = null;
    private IBufferedImageNotify bufferedImageNotifyReceiver = null;
    private int maxCachePics;
    private boolean doCachePics = false;
    private ArrayList<CachedImageData> cachePics;
    //private ArrayList<byte[]> cachePics;
    //private ArrayList<Map<String, Object>> cacheMeta;

    /**
     * コンストラクタ
     */
    CameraLiveViewListenerImpl(int maxCachePics)
    {
        cachePics = new ArrayList<>();
        this.maxCachePics = maxCachePics;
        Log.v(TAG, " Cache Images: " + maxCachePics);
    }

    /**
     * 更新するImageViewを拾う
     *
     */
    void setCameraLiveImageView(@Nullable IImageDataReceiver target)
    {
        this.imageView = target;
    }

    void setBufferedImageNotify(@NonNull IBufferedImageNotify receiver)
    {
        this.bufferedImageNotifyReceiver = receiver;
    }

    /**
     * LiveViewの画像データを更新する
     *
     */
    @Override
    public void onUpdateLiveView(OLYCamera camera, byte[] data, Map<String, Object> metadata)
    {
        try
        {
            if (imageView != null)
            {
                imageView.setImageData(data, metadata);
            }
            if (doCachePics)
            {
                cacheImage(data, metadata);
            }
            if (bufferedImageNotifyReceiver != null)
            {
                bufferedImageNotifyReceiver.updateBufferedImage();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void cacheImage(byte[] data, Map<String, Object> metadata)
    {
        try
        {
            if (cachePics != null)
            {
                boolean cacheIsFull = false;
                try
                {
                    cachePics.add(new CachedImageData(data, metadata));
                    if (cachePics.size() > maxCachePics)
                    {
                        cacheIsFull = true;
                    }
                }
                catch (Throwable e)
                {
                    e.printStackTrace();
                    cacheIsFull = true;
                }
                if (cacheIsFull)
                {
                    // リストから一つ取り除く
                    cachePics.remove(0);
                }
            }
        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void updateBufferedImageStatus(boolean onoff)
    {
        this.doCachePics = onoff;
        Log.v(TAG, " --- IMAGE BUFFERING : " + onoff);
    }

    @Override
    public boolean getBufferedImageStatus()
    {
        return (this.doCachePics);
    }

    @Override
    public int getMaxBufferImages()
    {
        return (maxCachePics);
    }

    @Override
    public int getNumberOfBufferedImages()
    {
        return (cachePics.size());
    }

    @Override
    public CachedImageData getBufferedImage(int position)
    {
        CachedImageData image = null;
        try
        {
            //int position = (int) Math.floor(cachePosition * (float) maxCachePics);
            int maxCache = cachePics.size();
            if (maxCache == 0)
            {
                return (null);
            }
            if (position >= maxCache)
            {
                position = maxCache - 1;
            }
            else if (position <= 0)
            {
                position = 0;
            }
            image = cachePics.get(position);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return (image);
    }

    /**
     * 　 CameraLiveImageView
     */
    public interface IImageDataReceiver
    {
        void setImageData(byte[] data, Map<String, Object> metadata);
    }
}

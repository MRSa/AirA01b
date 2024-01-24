package jp.osdn.gokigen.aira01b.liveview;

import java.util.Map;

import jp.osdn.gokigen.aira01b.CachedImageData;

public interface IBufferedImageHolder
{
    void updateBufferedImageStatus(boolean onoff);
    boolean getBufferedImageStatus();
    int getMaxBufferImages();
    int getNumberOfBufferedImages();
    CachedImageData getBufferedImage(int position);
}

package jp.osdn.gokigen.aira01b.liveview.bitmapconvert;

import android.graphics.Bitmap;

/**
 *   ビットマップ変換
 */
public interface IPreviewImageConverter
{
    Bitmap getModifiedBitmap(Bitmap src);
}

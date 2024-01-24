package jp.osdn.gokigen.aira01b.liveview.bitmapconvert;

import android.graphics.Bitmap;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

/**
 *
 *
 */
class ConvertEdgeColorY implements IPreviewImageConverter
{
    /**
     * 変換後のビットマップを応答する
     *
     * @return 変換後のビットマップ
     */
    @Override
    public Bitmap getModifiedBitmap(Bitmap src)
    {

        Bitmap bitmap;
        try {
            // OpenCVのデータ型に変換
            Mat mat = new Mat();
            org.opencv.android.Utils.bitmapToMat(src, mat);

            // エッジ検出処理
            Mat mat2 = new Mat();
            //Imgproc.Sobel(mat, mat2, mat.depth(), 1, 0);  // X方向
            Imgproc.Sobel(mat, mat2, mat.depth(), 0, 1);    // Y方向

            // 元画像と合成
            Core.add(mat, mat2, mat2);

            // OpenCVのデータ型からビットマップへ変換
            bitmap = Bitmap.createBitmap(mat2.width(), mat2.height(), Bitmap.Config.ARGB_8888);
            org.opencv.android.Utils.matToBitmap(mat2, bitmap);

            mat.release();
            mat2.release();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            bitmap = src;
        }
        return (bitmap);
    }
}

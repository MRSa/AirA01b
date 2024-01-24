package jp.osdn.gokigen.aira01b.liveview.bitmapconvert;

import android.graphics.Bitmap;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

/**
 *
 *
 */
class ConvertEdgeLaplacian implements IPreviewImageConverter
{
    /**
     *   変換後のビットマップを応答する
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
            Mat mat2 = new Mat();
            org.opencv.android.Utils.bitmapToMat(src, mat);

            //  グレースケール変換
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY);
            Imgproc.Laplacian(mat, mat2, mat.depth());  // エッジ検出

            // OpenCVのデータ型からビットマップへ変換
            bitmap = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888);
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

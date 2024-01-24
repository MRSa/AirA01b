package jp.osdn.gokigen.aira01b.liveview.bitmapconvert;

import android.graphics.Bitmap;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;


class ConvertEdgeSobel  implements IPreviewImageConverter
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
            Mat mat2 = new Mat();
            Mat mat3 = new Mat();
            org.opencv.android.Utils.bitmapToMat(src, mat);

            //  グレースケール変換
            Imgproc.cvtColor(mat, mat2, Imgproc.COLOR_RGB2GRAY);
            Imgproc.Sobel(mat2, mat3, mat2.depth(), 1, 0);  // エッジ検出

            // 元画像と合成
            //Core.addWeighted(mat, 0.5d, mat2, 0.5d, 0.0d, mat3);
            //Core.add(mat, mat2, mat3);

            // OpenCVのデータ型からビットマップへ変換
            bitmap = Bitmap.createBitmap(mat3.width(), mat3.height(), Bitmap.Config.ARGB_8888);
            org.opencv.android.Utils.matToBitmap(mat3, bitmap);

            mat.release();
            mat2.release();
            mat3.release();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            bitmap = src;
        }
        return (bitmap);
    }
}

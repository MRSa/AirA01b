package jp.osdn.gokigen.aira01b.manipulate;

import android.graphics.Bitmap;
import android.graphics.PointF;

/**
 *   画像加工のインタフェース
 *
 */
interface IImageManipulatorOperator
{
    int MANIPULATE_IMAGE_NONE = -1;         // 加工画像なし
    int COMBINE_LEFT_RIGHT = 0;             // 左右結合
    int COMBINE_UP_DOWN = 1;                // 上下結合
    int COMBINE_LEFT_RIGHT_RESIZED = 2;   // 左右結合（リサイズ）
    int COMBINE_UP_DOWN_RESIZED = 3;      // 上下結合（リサイズ）
    int PICTURE_IN_PICTURE_L = 4;          // ピクチャーインピクチャー（大）
    int PICTURE_IN_PICTURE_S = 5;          // ピクチャーインピクチャー（小）

    Bitmap processCombineUpDown(String upperImageFileName, String lowerImageFileName, boolean isResized);
    Bitmap processCombineLeftRight(String leftImageFileName, String rightImageFileName, boolean isResized);
    Bitmap processPictureInPicture(String leftImageFileName, String rightImageFileName, float resizeRate, PointF center);
    Bitmap updatePictureInPicture(float resizeRate, PointF center);
    void clearPictureInPicture();

}

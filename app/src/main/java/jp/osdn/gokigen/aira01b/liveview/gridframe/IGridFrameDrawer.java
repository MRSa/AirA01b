package jp.osdn.gokigen.aira01b.liveview.gridframe;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/**
 *   撮影補助線の描画クラス
 *
 */
public interface IGridFrameDrawer
{
    void drawFramingGrid(Canvas canvas, RectF rect, Paint paint);
    int getDrawColor();
}

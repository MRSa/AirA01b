package jp.osdn.gokigen.aira01b.liveview.gridframe;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

/**
 *    3x3 のグリッドと対角線の表示
 */
public class GridFrameDrawer5 implements IGridFrameDrawer
{
    /**
     *
     *
     */
    @Override
    public void drawFramingGrid(Canvas canvas, RectF rect, Paint paint)
    {
        float cX = (rect.right + rect.left) / 2.0f;
        float cY = (rect.bottom + rect.top) / 2.0f;

        canvas.drawLine(rect.left, cY, rect.right, cY, paint);
        canvas.drawLine(cX, rect.bottom, cX, rect.top, paint);

        canvas.drawRect(rect, paint);
    }

    @Override
    public int getDrawColor()
    {
        return (Color.argb(130,235,235,235));
    }
}

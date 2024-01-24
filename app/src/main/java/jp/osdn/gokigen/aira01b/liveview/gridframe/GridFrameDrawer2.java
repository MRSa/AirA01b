package jp.osdn.gokigen.aira01b.liveview.gridframe;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;

/**
 *   対角線のグリッド表示
 */
public class GridFrameDrawer2 implements IGridFrameDrawer
{
    /**
     *
     *
     */
    @Override
    public void drawFramingGrid(Canvas canvas, RectF rect, Paint paint)
    {
        canvas.drawLine(rect.left, rect.top, rect.right, rect.bottom, paint);
        canvas.drawLine(rect.left, rect.bottom, rect.right, rect.top, paint);

        canvas.drawRect(rect, paint);
    }

    @Override
    public int getDrawColor()
    {
        return (Color.argb(130,235,235,235));
    }
}

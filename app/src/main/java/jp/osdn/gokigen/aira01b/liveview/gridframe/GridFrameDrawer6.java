package jp.osdn.gokigen.aira01b.liveview.gridframe;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

/**
 *    真ん中四角と中心線の表示
 */
public class GridFrameDrawer6 implements IGridFrameDrawer
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
        float w = (rect.right - rect.left) / 4.0f;
        float h = (rect.bottom - rect.top) / 4.0f;

        canvas.drawRect(rect.left + w, rect.top + h, rect.right - w, rect.bottom - h, paint);

        canvas.drawLine(rect.left, cY, rect.left + w, cY, paint);
        canvas.drawLine(rect.right - w, cY, rect.right, cY, paint);

        canvas.drawLine(cX, rect.top, cX, rect.top + h, paint);
        canvas.drawLine(cX, rect.bottom - h, cX, rect.bottom, paint);
    }

    @Override
    public int getDrawColor()
    {
        return (Color.argb(130,235,235,235));
    }
}

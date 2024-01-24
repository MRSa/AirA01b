package jp.osdn.gokigen.aira01b.liveview;

import android.graphics.PointF;
import android.graphics.RectF;
import android.view.MotionEvent;

/**
 *   フォーカスフレームの表示クラス
 *
 */
public interface IAutoFocusFrameDisplay
{
    // フォーカスフレームの状態
    enum FocusFrameStatus
    {
        Running,
        Focused,
        Failed,
        Errored,
    }

    float getContentSizeWidth();
    float getContentSizeHeight();

    PointF getPointWithEvent(MotionEvent event);
    boolean isContainsPoint(PointF point);

    void showFocusFrame(RectF rect, FocusFrameStatus status, double duration);
    void hideFocusFrame();
}

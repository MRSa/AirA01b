package jp.osdn.gokigen.aira01b.liveview.controlpanel;

import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;
import android.view.View;

import jp.osdn.gokigen.aira01b.R;

class PanelDrawer
{
    private final String TAG = toString();
    private static final float MARGIN = 6.0f;
    private final View parent;

    PanelDrawer(View view)
    {
        this.parent = view;
    }

    /**
     *   指定された枠にバッテリー状態を表示する
     *
     *
     */
    void drawBatteryStatus(Canvas canvas, RectF region, String value)
    {
        if ((canvas == null)||(region == null)||(value == null))
        {
            // 何もせずに折り返す
            return;
        }
        int id;
        switch (value)
        {
            case "<BATTERY_LEVEL/CHARGE>":
                id = R.drawable.tt_icn_battery_charge;
                break;
            case "<BATTERY_LEVEL/EMPTY>":
                id = R.drawable.tt_icn_battery_empty;
                break;
            case "<BATTERY_LEVEL/WARNING>":
                id = R.drawable.tt_icn_battery_half;
                break;
            case "<BATTERY_LEVEL/LOW>":
                id = R.drawable.tt_icn_battery_middle;
                break;
            case "<BATTERY_LEVEL/FULL>":
                id = R.drawable.tt_icn_battery_full;
                break;
            case "<BATTERY_LEVEL/EMPTY_AC>":
                id = R.drawable.tt_icn_battery_supply_empty;
                break;
            case "<BATTERY_LEVEL/SUPPLY_WARNING>":
                id = R.drawable.tt_icn_battery_supply_half;
                break;
            case "<BATTERY_LEVEL/SUPPLY_LOW>":
                id = R.drawable.tt_icn_battery_supply_middle;
                break;
            case "<BATTERY_LEVEL/SUPPLY_FULL>":
                id = R.drawable.tt_icn_battery_supply_full;
                break;
            case "<BATTERY_LEVEL/UNKNOWN>":
            default:
                Log.v(TAG, "BATT: " + value);
                id = R.drawable.tt_icn_battery_unknown;
                break;
        }
        canvas.drawBitmap(BitmapFactory.decodeResource(parent.getResources(), id), null, region, new Paint());
    }

    /**
     *   指定された枠にドライブモードを表示する
     *
     *
     */
    void drawDriveModeStatus(Canvas canvas, RectF region, String value)
    {
        if ((canvas == null)||(region == null)||(value == null))
        {
            // 何もせずに折り返す
            return;
        }
        int id;
        switch (value)
        {
            case "<TAKE_DRIVE/DRIVE_NORMAL>":
                id = R.drawable.rm_icn_drive_setting_single;
                break;
            case "<TAKE_DRIVE/DRIVE_CONTINUE>":
                id = R.drawable.rm_icn_drive_setting_seq_l;
                break;

            default:
                Log.v(TAG, ": " + value);
                id = R.drawable.ic_warning_white_24dp;
                break;
        }
        canvas.drawBitmap(BitmapFactory.decodeResource(parent.getResources(), id), null, region, new Paint());
    }


    /**
     *   指定された枠に測光モードを表示する
     *
     *
     */
    void drawAeModeStatus(Canvas canvas, RectF region, String value)
    {
        if ((canvas == null)||(region == null)||(value == null))
        {
            // 何もせずに折り返す
            return;
        }
        int id;
        switch (value)
        {
            case "<AE/AE_CENTER>":
                id = R.drawable.ic_center_focus_weak_white_24dp;
                break;
            case "<AE/AE_ESP>":
                id = R.drawable.ic_crop_free_white_24dp;
                break;
            case "<AE/AE_PINPOINT>":
                id = R.drawable.ic_center_focus_strong_white_24dp;
                break;

            default:
                Log.v(TAG, ": " + value);
                id = R.drawable.ic_warning_white_24dp;
                break;
        }
        canvas.drawBitmap(BitmapFactory.decodeResource(parent.getResources(), id), null, region, new Paint());
    }

    /**
     *   resion内に文字を表示する
     *
     */
    void drawString(Canvas canvas, RectF region, String target, int color)
    {
        if ((target == null)||(target.length() <= 0))
        {
            return;
        }

        Paint textPaint = new Paint();
        textPaint.setColor(color);
        textPaint.setAntiAlias(true);

        float maxWidth = region.width() - MARGIN;
        float textSize = region.height() - MARGIN;
        textPaint.setTextSize(textSize);
        float textWidth = textPaint.measureText(target);
        while (maxWidth < textWidth)
        {
            // テキストサイズが横幅からあふれるまでループ
            textPaint.setTextSize(--textSize);
            textWidth = textPaint.measureText(target);
        }

        // センタリングするための幅を取得
        float margin = (region.width() - textWidth) / 2.0f;

        // 文字を表示する
        Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
        canvas.drawText(target, (region.left + margin), region.bottom - fontMetrics.descent, textPaint);
        //canvas.drawText(target, (region.left + MARGIN_FULL), region.bottom - fontMetrics.bottom, textPaint);
        //canvas.drawText(target, (region.left + MARGIN_FULL), region.bottom - fontMetrics.descent, textPaint);
        //canvas.drawText(target, (region.left + MARGIN_FULL), region.top + (fontMetrics.descent - fontMetrics.ascent), textPaint);
    }
}

package jp.osdn.gokigen.aira01b.liveview.controlpanel;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

import jp.co.olympus.camerakit.OLYCamera;
import jp.co.olympus.camerakit.OLYCameraStatusListener;
import jp.osdn.gokigen.aira01b.R;
import jp.osdn.gokigen.aira01b.liveview.ICameraPanelDrawer;
import jp.osdn.gokigen.aira01b.olycamerawrapper.IOlyCameraProperty;
import jp.osdn.gokigen.aira01b.olycamerawrapper.IOlyCameraPropertyProvider;
import jp.osdn.gokigen.aira01b.olycamerawrapper.IZoomLensHolder;

/**
 *   カメラのインジケータ表示...ちょー適当...まずい。refactorせねば。
 *
 */
public class CameraControlPanel implements ICameraPanelDrawer, OLYCameraStatusListener, View.OnClickListener, View.OnTouchListener, View.OnLongClickListener, GestureDetector.OnGestureListener
{
    private static final float MARGIN_FULL = 4.0f;
    private static final float MARGIN_HALF = 2.0f;
    private static final float MARGIN_ROUND = 12.0f;
    private static final float VEROCITY_THRESHOLD = 1.4f;
    private static final float MATRIX_X = 5.0f;
    private static final float MATRIX_Y = 8.0f;
    private static final int PANELAREA_LOWER = 0;
    private static final int PANELAREA_UPPER = 1;

    private static final float LENS_SENSITIVITY = 1.15f;


    private String information = "";
    private boolean isElectricZoomControl = false;
    private IZoomLensHolder zoomLensHolder = null;
    private int value = 0;
    private int currentColor = 0;
    private String warning;
    private String actualAperture;
    private String actualShutter;
    private String actualIso;
    private float actualFocal;
    private float targetFocalLength;
    private float verocityThreshold = VEROCITY_THRESHOLD;
    private Map<String, String> cameraValues = null;
    private List<String> propertyValueList = null;
    private int currentPropertyIndex = -1;
    private int defaultPropertyIndex = -1;
    private int panelArea = PANELAREA_LOWER;
    private final View parent;
    private final PanelDrawer drawer;
    private final IOlyCameraPropertyProvider provider;
    private final String TAG = toString();
    //private final GestureDetector gestureDetector;

    public CameraControlPanel(View parent, IOlyCameraPropertyProvider provider)
    {
        this.parent = parent;
        this.provider = provider;
        this.drawer = new PanelDrawer(parent);
        //this.gestureDetector = new GestureDetector(parent.getContext(), this);
    }

    /**
     *   フリックの感度を設定する
     *
     * @param sensitivityLevel  感度レベル
     */
    public void updateVerocityThreshold(int sensitivityLevel)
    {
        float value = (VEROCITY_THRESHOLD * ((float) sensitivityLevel / 3.0f));
        Log.v(TAG, "updateVerocityThreshold : " + value + "(level : " + sensitivityLevel + " )");
        if (value > 0.0f)
        {
            verocityThreshold = value;
        }
        else
        {
            verocityThreshold = VEROCITY_THRESHOLD;
        }
    }

    @Override
    public void onClick(View v)
    {
        Log.v(TAG, "onClick()  ");
    }

    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        boolean ret = true;
        Log.v(TAG, "onTouch() : [" + event.getX() + "," + event.getY() + "] ");

        if (event.getActionMasked() == MotionEvent.ACTION_UP)
        {
            Log.v(TAG, "onTouch() UP : [" + event.getX() + "," + event.getY() + "] ");
            if ((information != null)&&(propertyValueList != null)&&(currentPropertyIndex != defaultPropertyIndex))
            {
                // 最初の値とインデックスが動いていたら、プロパティを更新する
                Log.v(TAG, "UPDATE PROPERTY : " + defaultPropertyIndex + " -> " + currentPropertyIndex);
                try
                {
                    updateProperty(information, propertyValueList.get(currentPropertyIndex));
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            if ((isElectricZoomControl)&&(zoomLensHolder != null)&&(targetFocalLength > 0.0f))
            {
                // ズームレンズを駆動させる
                Log.v(TAG, "DRIVE ZOOM LENS : " + zoomLensHolder.getCurrentFocalLength() + "mm -> " + targetFocalLength + "mm ");
                try
                {
                    zoomLensHolder.driveZoomLens(targetFocalLength);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            // 対象の情報をクリアする
            information = "";
            propertyValueList = null;
            currentPropertyIndex = -1;
            defaultPropertyIndex = -1;
            isElectricZoomControl = false;
            zoomLensHolder = null;
            targetFocalLength = 0.0f;

        }
        else if (event.getActionMasked() == MotionEvent.ACTION_MOVE)
        {
            Log.v(TAG, "onTouch() MOVE : [" + event.getX() + "," + event.getY() + "] ");
            int history = event.getHistorySize() - 1;
            if (history > 0)
            {
                onFling(event, event, event.getX() - event.getHistoricalX(history), event.getY() - event.getHistoricalY(history));  // 逆な気がするが...
                //onFling(event, event, event.getHistoricalX(history) - event.getX(), event.getHistoricalY(history) - event.getY());
            }
        }
        else if (event.getActionMasked() == MotionEvent.ACTION_DOWN)
        {
            Log.v(TAG, "onTouch() DOWN : [" + event.getX() + "," + event.getY() + "] ");
            onDown(event);
        }
        else
        {
            ret = false;
        }
        return (ret);
        //return (gestureDetector.onTouchEvent(event));
    }

    @Override
    public boolean onLongClick(View v)
    {
        Log.v(TAG, "onLongClick()  ");
        return (false);
    }


    @Override
    public void drawCameraPanel(Canvas canvas)
    {
        Log.v(TAG, "drawCameraPanel()");

        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        canvas.drawRect(0.0f, 0.0f, canvas.getWidth(), canvas.getHeight(), paint);

        float height_unit = canvas.getHeight() / MATRIX_Y;
        float width_unit = canvas.getWidth() / MATRIX_X;    // ズーム入れたら6にしたいが...
        float half_unit = width_unit / 2.0f;
        if (height_unit < width_unit)
        {
            half_unit = height_unit / 2.0f;
        }

        Paint rectPaint = new Paint();
        rectPaint.setColor(Color.LTGRAY);
        RectF takeModeRect = new RectF(MARGIN_HALF, MARGIN_HALF, (width_unit * 1.0f) - MARGIN_FULL, (height_unit * 3.0f) - MARGIN_FULL);
        //canvas.drawRoundRect(takeModeRect, MARGIN_ROUND, MARGIN_ROUND, rectPaint);

        RectF shutterRect = new RectF((width_unit * 1.0f) + MARGIN_HALF, MARGIN_HALF, (width_unit * 3.0f) - MARGIN_FULL, (height_unit * 3.0f) - MARGIN_FULL);
        //canvas.drawRoundRect(shutterRect, MARGIN_ROUND, MARGIN_ROUND, rectPaint);

        RectF apertureRect = new RectF((width_unit * 3.0f) + MARGIN_HALF, MARGIN_HALF, (width_unit * 5.0f) - MARGIN_FULL, (height_unit * 3.0f) - MARGIN_FULL);
        //canvas.drawRoundRect(apertureRect, MARGIN_ROUND, MARGIN_ROUND, rectPaint);


        RectF isoRect = new RectF((width_unit * 0.0f) + MARGIN_HALF, (height_unit * 3.0f) + MARGIN_HALF, (width_unit * 2.0f) - MARGIN_FULL, (height_unit * 5.0f) - MARGIN_FULL);
        //canvas.drawRoundRect(isoRect, MARGIN_ROUND, MARGIN_ROUND, rectPaint);

        RectF expRect = new RectF((width_unit * 2.0f) + MARGIN_HALF, (height_unit * 3.0f) + MARGIN_HALF, (width_unit * 4.0f) - MARGIN_FULL, (height_unit * 5.0f) - MARGIN_FULL);
        //canvas.drawRoundRect(expRect, MARGIN_ROUND, MARGIN_ROUND, rectPaint);

        RectF aeRect = new RectF((width_unit * 4.0f) + MARGIN_HALF, (height_unit * 3.0f) + MARGIN_HALF, (width_unit * 5.0f) - MARGIN_FULL, (height_unit * 5.0f) - MARGIN_FULL);
        //canvas.drawRoundRect(aeRect, MARGIN_ROUND, MARGIN_ROUND, rectPaint);

        RectF wbRect = new RectF((width_unit * 0.0f) + MARGIN_HALF, (height_unit * 5.0f) + MARGIN_HALF, (width_unit * 2.0f) - MARGIN_FULL, (height_unit * 7.0f) - MARGIN_FULL);
        //canvas.drawRoundRect(wbRect, MARGIN_ROUND, MARGIN_ROUND, rectPaint);

        RectF filterRect = new RectF((width_unit * 2.0f) + MARGIN_HALF, (height_unit * 5.0f) + MARGIN_HALF, (width_unit * 4.0f) - MARGIN_FULL, (height_unit * 7.0f) - MARGIN_FULL);
        //canvas.drawRoundRect(filterRect, MARGIN_ROUND, MARGIN_ROUND, rectPaint);

        RectF driveRect = new RectF((width_unit * 4.0f) + MARGIN_HALF, (height_unit * 5.0f) + MARGIN_HALF, (width_unit * 5.0f) - MARGIN_FULL, (height_unit * 7.0f) - MARGIN_FULL);
        //canvas.drawRoundRect(driveRect, MARGIN_ROUND, MARGIN_ROUND, rectPaint);


        RectF focalRect = new RectF((width_unit * 0.3f) + MARGIN_HALF, (height_unit * 7.0f) + MARGIN_HALF, (width_unit * 2.0f) - MARGIN_FULL, (height_unit * 8.0f) - MARGIN_FULL);
        //canvas.drawRoundRect(focalRect, MARGIN_ROUND, MARGIN_ROUND, rectPaint);

        RectF warnRect = new RectF((width_unit * 2.0f) + MARGIN_HALF, (height_unit * 7.0f) + MARGIN_HALF, (width_unit * 4.0f) - MARGIN_FULL, (height_unit * 8.0f) - MARGIN_FULL);
        //canvas.drawRoundRect(warnRect, MARGIN_ROUND, MARGIN_ROUND, rectPaint);

        RectF battRect = new RectF((width_unit * 4.0f) + MARGIN_HALF, (height_unit * 7.0f) + MARGIN_HALF, (width_unit * 5.0f) - MARGIN_FULL, (height_unit * 8.0f) - MARGIN_FULL);
        //canvas.drawRoundRect(battRect, MARGIN_ROUND, MARGIN_ROUND, rectPaint);

        int drawColor = Color.LTGRAY;
        if (cameraValues != null)
        {
            String takeMode = provider.getCameraPropertyValueTitle(cameraValues.get(IOlyCameraProperty.TAKE_MODE));
            drawer.drawString(canvas, takeModeRect, takeMode, drawColor);

            String shutter = provider.getCameraPropertyValueTitle(actualShutter);
            drawer.drawString(canvas, shutterRect, shutter, drawColor);

            String aperture = provider.getCameraPropertyValueTitle(actualAperture);
            if (aperture != null)
            {
                drawer.drawString(canvas, apertureRect, "F" + aperture, drawColor);
            }

            String iso = provider.getCameraPropertyValueTitle(cameraValues.get(IOlyCameraProperty.ISO_SENSITIVITY));
            if (iso != null)
            {
                switch (iso)
                {
                    case "Auto":
                        if (actualIso != null)
                        {
                            iso = "iso" + provider.getCameraPropertyValueTitle(actualIso);
                        }
                        else
                        {
                            iso = "iso-A";
                        }
                        break;

                    case "null":
                        iso = "";
                        break;

                    default:
                        iso = "ISO" + iso;
                        break;
                }
                drawer.drawString(canvas, isoRect, iso, drawColor);
            }

            String exp = provider.getCameraPropertyValueTitle(cameraValues.get(IOlyCameraProperty.EXPOSURE_COMPENSATION));
            drawer.drawString(canvas, expRect, exp, drawColor);

            String colorTone;
            if (takeMode.equals("ART"))
            {
                // アートフィルターモードのときには、アートフィルターを表示する
                colorTone = provider.getCameraPropertyValueTitle(cameraValues.get(IOlyCameraProperty.ART_FILTER));
            }
            else
            {
                // アートフィルターモード以外のときは、仕上がり・ピクチャーモードを表示する
                colorTone = provider.getCameraPropertyValueTitle(cameraValues.get(IOlyCameraProperty.COLOR_TONE));
            }
            drawer.drawString(canvas, filterRect, colorTone, drawColor);

            //String drive = provider.getCameraPropertyValueTitle(cameraValues.get(IOlyCameraProperty.DRIVE_MODE));
            //drawString(canvas, driveRect, drive, drawColor);
            drawer.drawDriveModeStatus(canvas, driveRect, cameraValues.get(IOlyCameraProperty.DRIVE_MODE));

            //String ae_Mode = provider.getCameraPropertyValueTitle(cameraValues.get(IOlyCameraProperty.AE_MODE));
            //drawString(canvas, aeRect, ae_Mode, drawColor);
            drawer.drawAeModeStatus(canvas, aeRect, cameraValues.get(IOlyCameraProperty.AE_MODE));

            String wb_Mode = provider.getCameraPropertyValueTitle(cameraValues.get(IOlyCameraProperty.WB_MODE));
            drawer.drawString(canvas, wbRect, wb_Mode, drawColor);

            //String battery = provider.getCameraPropertyValueTitle(cameraValues.get(IOlyCameraProperty.BATTERY_LEVEL));
            //drawString(canvas, battRect, battery, drawColor);
            drawer.drawBatteryStatus(canvas, battRect, cameraValues.get(IOlyCameraProperty.BATTERY_LEVEL));

            String focalLength = String.format(Locale.ENGLISH, "%2.1fmm", actualFocal);
            drawer.drawString(canvas, focalRect, focalLength, drawColor);
        }

        ///////  (警告があったら)警告の表示  /////
        if (warning != null)
        {
            drawer.drawString(canvas, warnRect, warning, Color.argb(0xff, 0xDA, 0xA5, 0x20));
        }

        ///// 動作インジケータ(5回に1回、色を更新) /////
        if (value  % 5 == 0)
        {
            if (currentColor == Color.LTGRAY)
            {
                currentColor = Color.DKGRAY;
            }
            else
            {
                currentColor = Color.LTGRAY;
            }
        }
        rectPaint.setColor(currentColor);
        float radius = half_unit - MARGIN_ROUND;
        canvas.drawCircle((0.0f + radius + MARGIN_HALF), (canvas.getHeight() - radius - MARGIN_HALF), radius, rectPaint);
        value++;

        ///// 操作中パネル表示 /////
        if ((information != null)&&(information.length() > 0))
        {
            drawConsolePanel(canvas, width_unit, height_unit);
        }
        else if ((isElectricZoomControl)&&(zoomLensHolder != null))
        {
            // 電動ズームレンズの操作
            drawZoomPanel(canvas, width_unit, height_unit);
        }
    }

    /**
     *
     *
     *
     */
    private void drawConsolePanel(Canvas canvas, float unit_x, float unit_y)
    {
        String output = information;
        RectF areaRect, outputRect, leftRect, centerRect, rightRect;

        Paint paint = new Paint();
        paint.setColor(Color.LTGRAY);
        if (panelArea == PANELAREA_UPPER)
        {
            //canvas.drawRect((unit_x * 0.1f),  (unit_y * 0.8f), (unit_x * 4.81f), (unit_y * 2.95f), paint);
            areaRect = new RectF((unit_x * 0.1f),  (unit_y * 0.7f), (unit_x * 4.81f), (unit_y * 2.95f));
            canvas.drawRoundRect(areaRect, MARGIN_ROUND, MARGIN_ROUND, paint);
            outputRect = new RectF((unit_x * 0.1f) + MARGIN_HALF, (unit_y * 1.5f) + MARGIN_HALF, (unit_x * 0.9f) - MARGIN_FULL, (unit_y * 2.8f) - MARGIN_FULL);
            leftRect = new RectF((unit_x * 1.0f) + MARGIN_HALF, (unit_y * 1.9f) + MARGIN_HALF, (unit_x * 1.8f) - MARGIN_FULL, (unit_y * 2.8f) - MARGIN_FULL);
            centerRect = new RectF((unit_x * 2.0f) + MARGIN_HALF, (unit_y * 0.8f) + MARGIN_HALF, (unit_x * 3.9f) - MARGIN_FULL, (unit_y * 2.8f) - MARGIN_FULL);
            rightRect = new RectF((unit_x * 4.0f) + MARGIN_HALF, (unit_y * 1.9f) + MARGIN_HALF, (unit_x * 4.8f) - MARGIN_FULL, (unit_y * 2.8f) - MARGIN_FULL);

            // 設定対象値を示す線を引く
            paint.setColor(Color.BLACK);
            canvas.drawLine((unit_x * 2.0f) + MARGIN_HALF, (unit_y * 2.8f), (unit_x * 3.9f) - MARGIN_FULL, (unit_y * 2.8f), paint);
        }
        else   // PANELAREA_LOWER
        {
            //canvas.drawRect((unit_x * 0.1f),  (unit_y * 5.8f), (unit_x * 4.8f), (unit_y * 7.95f), paint);
            areaRect = new RectF((unit_x * 0.1f),  (unit_y * 5.7f), (unit_x * 4.8f), (unit_y * 7.95f));
            canvas.drawRoundRect(areaRect, MARGIN_ROUND, MARGIN_ROUND, paint);
            outputRect = new RectF((unit_x * 0.1f) + MARGIN_HALF, (unit_y * 6.5f) + MARGIN_HALF, (unit_x * 0.9f) - MARGIN_FULL, (unit_y * 7.8f) - MARGIN_FULL);
            leftRect = new RectF((unit_x * 1.0f) + MARGIN_HALF, (unit_y * 6.9f) + MARGIN_HALF, (unit_x * 1.8f) - MARGIN_FULL, (unit_y * 7.8f) - MARGIN_FULL);
            centerRect = new RectF((unit_x * 2.0f) + MARGIN_HALF, (unit_y * 5.8f) + MARGIN_HALF, (unit_x * 3.9f) - MARGIN_FULL, (unit_y * 7.8f) - MARGIN_FULL);
            rightRect = new RectF((unit_x * 4.0f) + MARGIN_HALF, (unit_y * 6.9f) + MARGIN_HALF, (unit_x * 4.8f) - MARGIN_FULL, (unit_y * 7.8f) - MARGIN_FULL);

            // 設定対象値を示す線を引く
            paint.setColor(Color.BLACK);
            canvas.drawLine((unit_x * 2.0f) + MARGIN_HALF, (unit_y * 7.8f), (unit_x * 3.9f) - MARGIN_FULL, (unit_y * 7.8f), paint);
        }

        //  選択中のカメラプロパティ名を表示する
        int outputColor = Color.BLACK;
        drawer.drawString(canvas, outputRect, output, outputColor);

        if ((propertyValueList != null)&&(propertyValueList.size() > 0))
        {
            String previous ="";
            int previousColor = Color.DKGRAY;
            if (currentPropertyIndex > 0)
            {
                int indexToSet = currentPropertyIndex - 1;
                previous = provider.getCameraPropertyValueTitle(propertyValueList.get(indexToSet));
            }
            int centerColor = Color.BLACK;
            String center = provider.getCameraPropertyValueTitle(propertyValueList.get(currentPropertyIndex));

            String next = "";
            int nextColor = Color.DKGRAY;
            if ((currentPropertyIndex + 1) < propertyValueList.size())
            {
                int indexToSet = currentPropertyIndex + 1;
                next = provider.getCameraPropertyValueTitle(propertyValueList.get(indexToSet));
            }
            drawer.drawString(canvas, leftRect, previous, previousColor);
            drawer.drawString(canvas, centerRect, center, centerColor);
            drawer.drawString(canvas, rightRect, next, nextColor);
        }
    }

    /**
     *
     *
     *
     */
    private void drawZoomPanel(Canvas canvas, float unit_x, float unit_y)
    {
        RectF areaRect, leftRect, rightRect, showCurrentLengthRect, showTargetLengthRect;
        float startX, endX, centerX, centerY, topY, bottomY, width_margin, targetX;
        Paint paint = new Paint();
        paint.setColor(Color.LTGRAY);
        paint.setAntiAlias(true);


        float minimum = zoomLensHolder.getMinimumFocalLength();
        float maximum = zoomLensHolder.getMaximumFocalLength();
        float current = zoomLensHolder.getCurrentFocalLength();
        float markerPosition = ((current - minimum) / (maximum - minimum));

        startX = (unit_x * 0.8f);
        endX = (unit_x * 4.1f);
        centerX = markerPosition * (4.1f - 0.8f);
        targetX = ((targetFocalLength - minimum) / (maximum - minimum)) * (4.1f - 0.8f);
        width_margin = 0.6f;

        if (panelArea == PANELAREA_UPPER)
        {
            areaRect = new RectF((unit_x * 0.1f),  (unit_y * 0.7f), (unit_x * 4.81f), (unit_y * 2.95f));
            canvas.drawRoundRect(areaRect, MARGIN_ROUND, MARGIN_ROUND, paint);
            leftRect = new RectF((unit_x * 0.1f) + MARGIN_HALF, (unit_y * 1.75f) + MARGIN_HALF, (unit_x * 0.8f) - MARGIN_HALF, (unit_y * 2.92f) - MARGIN_HALF);
            rightRect = new RectF((unit_x * 4.1f) + MARGIN_HALF, (unit_y * 1.75f) + MARGIN_HALF, (unit_x * 4.8f) - MARGIN_HALF, (unit_y * 2.92f) - MARGIN_HALF);
            if (markerPosition < 0.5f)
            {
                showCurrentLengthRect = new RectF(startX + (unit_x * centerX) + MARGIN_FULL, (unit_y * 2.05f) + MARGIN_HALF, startX + (unit_x * (centerX + width_margin)) - MARGIN_FULL, (unit_y * 2.85f) - MARGIN_HALF);
            }
            else
            {
                showCurrentLengthRect = new RectF(startX + (unit_x * (centerX - width_margin)) + MARGIN_FULL, (unit_y * 2.05f) + MARGIN_HALF, startX + (unit_x * (centerX)) - MARGIN_FULL, (unit_y * 2.85f) - MARGIN_HALF);
            }
            //showTargetLengthRect = new RectF(startX + (unit_x * centerX) + MARGIN_HALF, (unit_y * 0.95f) + MARGIN_HALF, startX + (unit_x * (centerX + width_margin)) - MARGIN_HALF, (unit_y * 1.85f) - MARGIN_HALF);
            showTargetLengthRect = new RectF((unit_x * 4.1f), (unit_y * 0.8f) + MARGIN_HALF, (unit_x * 4.8f), (unit_y * 1.81f) - MARGIN_HALF);


            topY = (unit_y * 1.0f);
            bottomY = (unit_y * 2.9f);

            // 設定対象値を示す線を引く
            //paint.setColor(Color.BLACK);
            //canvas.drawLine((unit_x * 2.0f) + MARGIN_HALF, (unit_y * 2.8f), (unit_x * 3.9f) - MARGIN_FULL, (unit_y * 2.8f), paint);
        }
        else   // PANELAREA_LOWER
        {
            //canvas.drawRect((unit_x * 0.1f),  (unit_y * 5.8f), (unit_x * 4.8f), (unit_y * 7.95f), paint);
            areaRect = new RectF((unit_x * 0.1f),  (unit_y * 5.7f), (unit_x * 4.8f), (unit_y * 7.95f));
            canvas.drawRoundRect(areaRect, MARGIN_ROUND, MARGIN_ROUND, paint);
            leftRect = new RectF((unit_x * 0.1f) + MARGIN_HALF, (unit_y * 6.7f) + MARGIN_HALF, (unit_x * 0.8f) - MARGIN_HALF, (unit_y * 7.6f) - MARGIN_HALF);
            rightRect = new RectF((unit_x * 4.1f) + MARGIN_HALF, (unit_y * 6.7f) + MARGIN_HALF, (unit_x * 4.8f) - MARGIN_HALF, (unit_y * 7.6f) - MARGIN_HALF);
            if (markerPosition < 0.5f)
            {
                showCurrentLengthRect = new RectF(startX + (unit_x * centerX) + MARGIN_FULL, (unit_y * 6.95f) + MARGIN_HALF, startX + (unit_x * (centerX + width_margin)) - MARGIN_FULL, (unit_y * 7.85f) - MARGIN_HALF);
            }
            else
            {
                showCurrentLengthRect = new RectF(startX + (unit_x * (centerX - width_margin)) + MARGIN_FULL, (unit_y * 6.95f) + MARGIN_HALF, startX + (unit_x * (centerX)) - MARGIN_FULL, (unit_y * 7.85f) - MARGIN_HALF);
            }
            showTargetLengthRect = new RectF(startX + (unit_x * targetX) + MARGIN_HALF, (unit_y * 5.8f) + MARGIN_HALF, startX + (unit_x * (targetX + width_margin)) - MARGIN_HALF, (unit_y * 6.8f) - MARGIN_HALF);

            topY = (unit_y * 6.0f);
            bottomY = (unit_y * 7.9f);
        }
        centerY = (topY + bottomY) / 2.0f;

        // 設定対象線を引く
        Paint linePaint = new Paint();
        linePaint.setAntiAlias(true);
        linePaint.setColor(Color.DKGRAY);
        canvas.drawLine(startX, centerY, endX, centerY, linePaint);  // 水平線
        canvas.drawLine(startX, (topY + MARGIN_FULL), startX, (bottomY - MARGIN_FULL), linePaint);  // 左側の縦線
        canvas.drawLine(endX,   (topY + MARGIN_FULL), endX,   (bottomY - MARGIN_FULL), linePaint);  // 右側の縦線

        // 現在の設定値の位置にマークを入れる
        Paint markPaint = new Paint();
        markPaint.setColor(Color.BLACK);
        markPaint.setStyle(Paint.Style.FILL);
        markPaint.setAntiAlias(true);
        Path currentPath = new Path();
        currentPath.moveTo(startX + (unit_x * centerX), centerY);
        currentPath.lineTo(startX + (unit_x * centerX) - MARGIN_FULL, bottomY - MARGIN_FULL);
        currentPath.lineTo(startX + (unit_x * centerX) + MARGIN_FULL, bottomY - MARGIN_FULL);
        currentPath.lineTo(startX + (unit_x * centerX), centerY);
        currentPath.close();
        canvas.drawPath(currentPath, markPaint);

        // 設定したい値の場所にマーキングする
        markPaint.setARGB(0xff, 0xe6, 0x79, 0x28);
        Path path = new Path();
        path.moveTo(startX + (unit_x * targetX), centerY);
        path.lineTo(startX + (unit_x * targetX) - MARGIN_FULL, topY + MARGIN_FULL);
        path.lineTo(startX + (unit_x * targetX) + MARGIN_FULL, topY + MARGIN_FULL);
        path.lineTo(startX + (unit_x * targetX), centerY);
        path.close();
        canvas.drawPath(path, markPaint);

        //  Min <-> Max の焦点距離を表示する
        int outputColor = Color.BLACK;
        String focalLength;
        focalLength = String.format(Locale.ENGLISH, "%2.1fmm", zoomLensHolder.getMinimumFocalLength());
        drawer.drawString(canvas, leftRect, focalLength, outputColor);
        focalLength = String.format(Locale.ENGLISH, "%2.1fmm", zoomLensHolder.getMaximumFocalLength());
        drawer.drawString(canvas, rightRect, focalLength, outputColor);

        // 現在の設定値
        focalLength = String.format(Locale.ENGLISH, "%2.1f", zoomLensHolder.getCurrentFocalLength());
        drawer.drawString(canvas, showCurrentLengthRect, focalLength, outputColor);

        // ターゲットとする値
        outputColor = Color.argb(0xff, 0xe6, 0x79, 0x28);
        focalLength = String.format(Locale.ENGLISH, "%2.1f", targetFocalLength);
        drawer.drawString(canvas, showTargetLengthRect, focalLength, outputColor);
    }

    /**
     *
     */
    private  void updateProperty(final String name, final String value)
    {
        try
        {
            Thread thread = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        provider.setCameraPropertyValue(name, value);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }



    @Override
    public void onUpdateStatus(OLYCamera olyCamera, String s)
    {
        //Log.v(TAG, "onUpdateStatus() : " + s);
        warning = "";
        try
        {
            // 警告メッセージを生成
            if (olyCamera.isMediaError())
            {
                warning = warning + " " +  parent.getContext().getString(R.string.media_error);
            }
            if (olyCamera.isMediaBusy())
            {
                warning = warning + " " +  parent.getContext().getString(R.string.media_busy);
            }
            if (olyCamera.isHighTemperatureWarning())
            {
                warning = warning + " " +  parent.getContext().getString(R.string.high_temperature_warning);
            }
            if ((olyCamera.isExposureMeteringWarning())||(olyCamera.isExposureWarning()))
            {
                warning = warning + " " + parent.getContext().getString(R.string.exposure_metering_warning);
            }
            if (olyCamera.isActualIsoSensitivityWarning())
            {
                warning = warning + " " + parent.getContext().getString(R.string.iso_sensitivity_warning);
            }

            TreeSet<String> treeSet = new TreeSet<>();
            treeSet.add(IOlyCameraProperty.TAKE_MODE);
            treeSet.add(IOlyCameraProperty.WB_MODE);
            treeSet.add(IOlyCameraProperty.AE_MODE);
            treeSet.add(IOlyCameraProperty.APERTURE);
            treeSet.add(IOlyCameraProperty.COLOR_TONE);
            treeSet.add(IOlyCameraProperty.SHUTTER_SPEED);
            treeSet.add(IOlyCameraProperty.ISO_SENSITIVITY);
            treeSet.add(IOlyCameraProperty.EXPOSURE_COMPENSATION);
            treeSet.add(IOlyCameraProperty.BATTERY_LEVEL);
            treeSet.add(IOlyCameraProperty.DRIVE_MODE);
            treeSet.add(IOlyCameraProperty.ART_FILTER);

            cameraValues = olyCamera.getCameraPropertyValues(treeSet);
            actualShutter = olyCamera.getActualShutterSpeed();
            actualAperture = olyCamera.getActualApertureValue();
            actualIso = olyCamera.getActualIsoSensitivity();
            actualFocal = olyCamera.getActualFocalLength();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        parent.postInvalidate();
    }

    /**
     *   タッチしたエリアがどのプロパティなのかを応答する
     *
     */
    private String checkArea(MotionEvent e)
    {
        String property;
        float unitX = parent.getWidth() / MATRIX_X;
        float unitY = parent.getHeight() / MATRIX_Y;

        float x = e.getX();
        float y = e.getY();

        int areaX = (int) Math.floor(x / unitX);
        int areaY = (int) Math.floor(y / unitY);

        isElectricZoomControl = false;
        targetFocalLength = 0.0f;
        panelArea = PANELAREA_LOWER;
        if ((areaX < 1)&&(areaY < 3))
        {
            property = IOlyCameraProperty.TAKE_MODE;
        }
        else if ((areaX < 3)&&(areaY < 3))
        {
            property = IOlyCameraProperty.SHUTTER_SPEED;
        }
        else if ((areaX < 5)&&(areaY < 3))
        {
            property = IOlyCameraProperty.APERTURE;
        }
        else if ((areaX < 2)&&(areaY < 5))
        {
            property = IOlyCameraProperty.ISO_SENSITIVITY;
        }
        else if ((areaX < 4)&&(areaY < 5))
        {
            property = IOlyCameraProperty.EXPOSURE_COMPENSATION;
        }
        else if ((areaX < 5)&&(areaY < 5))
        {
            property = IOlyCameraProperty.AE_MODE;
        }
        else if ((areaX < 2)&&(areaY < 7))
        {
            property = IOlyCameraProperty.WB_MODE;
            panelArea = PANELAREA_UPPER;
        }
        else if ((areaX < 4)&&(areaY < 7))
        {
            property = IOlyCameraProperty.COLOR_TONE;
            if ((cameraValues != null)&&(provider != null))
            {
                if (provider.getCameraPropertyValueTitle(cameraValues.get(IOlyCameraProperty.TAKE_MODE)).equals("ART"))
                {
                    property = IOlyCameraProperty.ART_FILTER;
                }
            }
            panelArea = PANELAREA_UPPER;
        }
        else if ((areaX < 5)&&(areaY < 7))
        {
            property = IOlyCameraProperty.DRIVE_MODE;
            panelArea = PANELAREA_UPPER;
        }
        else if ((areaX < 4)&&(areaY < 8))
        {
            //  レンズが電動ズームかどうか調べる
            if ((provider != null)&&(provider.isElectricZoomLens()))
            {
                zoomLensHolder = provider.getZoomLensHolder();
                if (zoomLensHolder != null)
                {
                    isElectricZoomControl = true;
                    panelArea = PANELAREA_UPPER;
                    targetFocalLength = zoomLensHolder.getCurrentFocalLength();
                    Log.v(TAG, "CHANGE FOCAL LENGTH (ZOOM LENS)");
                }
            }
            property = "";
        }
        else
        {
            property = "";
        }
        Log.v(TAG, "[" + areaX + "," + areaY + "]");

        if (!isElectricZoomControl)
        {
            zoomLensHolder = null;
        }

        // カメラプロパティを読み出して設定
        if (property.length() > 0)
        {
            try
            {
                if ((provider != null)&&(provider.canSetCameraProperty(property)))
                {
                    propertyValueList = provider.getCameraPropertyValueList(property);
                    if (propertyValueList != null)
                    {
                        currentPropertyIndex = propertyValueList.indexOf(provider.getCameraPropertyValue(property));
                        defaultPropertyIndex = currentPropertyIndex;
                    }
                }
                else
                {
                    propertyValueList = null;
                    currentPropertyIndex = -1;
                    defaultPropertyIndex = -1;
                    property = "";
                }
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
                propertyValueList = null;
                currentPropertyIndex = -1;
                defaultPropertyIndex = -1;
                property = "";
            }
        }
        //property = property + " [" + areaX + "," + areaY + "]";
        return (property);
    }


    @Override
    public boolean onDown(MotionEvent event)
    {
        Log.v(TAG, "onDown() ");
        if (parent == null)
        {
            return (false);
        }
        information = checkArea(event);
        return (false);
    }

    @Override
    public void onShowPress(MotionEvent e)
    {
        Log.v(TAG, "onShowPress()  ");
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e)
    {
        return (false);
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
    {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e)
    {

    }

    /**
     *   ズームレンズの駆動
     *
     */
    private void onFlingZoomLens(float velocityX)
    {

        float minimum = zoomLensHolder.getMinimumFocalLength();
        float maximum = zoomLensHolder.getMaximumFocalLength();

        targetFocalLength = targetFocalLength + velocityX * LENS_SENSITIVITY;

        if (minimum > targetFocalLength)
        {
            targetFocalLength = minimum;
        }
        else if (maximum < targetFocalLength)
        {
            targetFocalLength = maximum;
        }

        parent.postInvalidate();
    }

    /**
     *   カメラプロパティの変更
     *
     */
    private void onFlingCameraProperty(float velocityX)
    {
        int direction = 0;
        if (velocityX < 0)
        {
            Log.v(TAG, "onFling()  DOWN");
            if (currentPropertyIndex > 0)
            {
                // ひとつ小さくする
                direction = -1;
            }
        }
        else if (velocityX > 0)
        {
            Log.v(TAG, "onFling()  UP");
            if (currentPropertyIndex < (propertyValueList.size() - 1))
            {
                // ひとつおおきくする
                direction = +1;
            }
        }
        currentPropertyIndex = currentPropertyIndex + direction;
        parent.postInvalidate();
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
    {
        Log.v(TAG, "onFling()  [" + velocityX + "," + velocityY + "]");
        try
        {
            if ((propertyValueList != null)&&(currentPropertyIndex != -1)&&(Math.abs(velocityX) > verocityThreshold))
            {
                onFlingCameraProperty(velocityX);
            }
            else if ((isElectricZoomControl)&&(zoomLensHolder != null))
            {
                // 電動ズームレンズの操作
                onFlingZoomLens(velocityX);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return (true);
    }
}

package jp.osdn.gokigen.aira01b.liveview.bufferedimage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Looper;
import android.os.Vibrator;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.exifinterface.media.ExifInterface;

import java.util.Map;

import jp.co.olympus.camerakit.OLYCamera;
import jp.co.olympus.camerakit.OLYCameraStatusListener;
import jp.osdn.gokigen.aira01b.CachedImageData;
import jp.osdn.gokigen.aira01b.liveview.IBufferedImageHolder;
import jp.osdn.gokigen.aira01b.liveview.IBufferedImageNotify;
import jp.osdn.gokigen.aira01b.liveview.ICameraPanelDrawer;
import jp.osdn.gokigen.aira01b.liveview.ISeekbarPosition;
import jp.osdn.gokigen.aira01b.liveview.IStoreImage;
import jp.osdn.gokigen.aira01b.liveview.StoreImage;

import static android.content.Context.VIBRATOR_SERVICE;
import static jp.osdn.gokigen.aira01b.liveview.CameraLiveImageView.EXIF_ORIENTATION;
import static jp.osdn.gokigen.aira01b.preference.ICameraPropertyAccessor.NUMBER_OF_CACHE_PICTURES_DEFAULT_VALUE_INT;

/**
 *    バッファリングした画像を表示する画面下部のパネル
 *
 *       長押しでバッファリング中断・再開
 *       バッファリング中断中は、左フリックで逆再生、右クリックで順再生
 *
 */
public class BufferedImagePanel implements ICameraPanelDrawer, View.OnClickListener, View.OnTouchListener, View.OnLongClickListener, GestureDetector.OnGestureListener, OLYCameraStatusListener, IBufferedImageNotify
{
    private final String TAG = toString();
    private static final int MARGIN_X = 6;
    private static final int MARGIN_Y = 4;

    private final Context context;
    private final View parent;
    private final IBufferedImageHolder imageHolder;
    private final ISeekbarPosition seekbarPosition;
    private final GestureDetector gestureDetector;
    private final IStoreImage storeImage;

    private int previousPosition = 0;
    private int autoMovePosition = 0;

    public BufferedImagePanel(View parent, IBufferedImageHolder imageHolder, ISeekbarPosition seekbarPosition)
    {
        this.context = parent.getContext();
        this.parent = parent;
        this.imageHolder = imageHolder;
        this.seekbarPosition = seekbarPosition;
        this.gestureDetector = new GestureDetector(parent.getContext(), this);
        this.storeImage = new StoreImage(context);
    }

    @Override
    public void drawCameraPanel(Canvas canvas)
    {
        // Log.v(TAG, " drawCameraPanel");
        if ((canvas == null)||(imageHolder == null)||(seekbarPosition == null))
        {
            // 描画できない状態なので、何もせずに終了する
            Log.v(TAG, " CANNOT DRAW... RETURN.");
            return;
        }

        //  画面を消去
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        canvas.drawRect(0.0f, 0.0f, canvas.getWidth(), canvas.getHeight(), paint);

        //  再生する場所をシークバーの位置から決める
        int seekbarLocation = 0;
        int maxImages = 0;
        try
        {
            maxImages = imageHolder.getMaxBufferImages();
            seekbarLocation = (int) ((float) seekbarPosition.getPosition() / (float) NUMBER_OF_CACHE_PICTURES_DEFAULT_VALUE_INT * (float) maxImages);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        if (autoMovePosition != 0)
        {
            previousPosition = previousPosition + autoMovePosition;
            if (previousPosition < 0)
            {
                // 末端まで再生した
                autoMovePosition = 0;
                previousPosition = 0;
                vibrate(30);
            }
            else if (previousPosition > maxImages)
            {
                // 末端まで再生した
                autoMovePosition = 0;
                previousPosition = maxImages;
                vibrate(30);
            }
        }
        else
        {
            // seekbarの位置にする
            previousPosition = seekbarLocation;
        }
        //  画像を表示
        drawImage(canvas, previousPosition);

        //  文字を表示する
        drawInformationText(canvas);
    }

    private void drawInformationText(Canvas canvas)
    {
        try
        {
            Paint textPaint = new Paint();
            textPaint.setColor(Color.LTGRAY);
            textPaint.setAntiAlias(true);
            textPaint.setTextSize(20);

            int nofBuffer = imageHolder.getNumberOfBufferedImages();
            int maxBuffer = imageHolder.getMaxBufferImages();
            if (nofBuffer < maxBuffer)
            {
                // バッファーがいっぱいではないときは...左上にバッファリングの数値を表示する。
                String bufferingMessage = "BUFFERING : " + nofBuffer + "/" + maxBuffer;

                Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
                canvas.drawText(bufferingMessage, MARGIN_X, canvas.getHeight() - fontMetrics.bottom - MARGIN_Y, textPaint);
            }
            else if (!imageHolder.getBufferedImageStatus())
            {
                // バッファリング停止中表示を左上に表示する
                String pauseMessage = "PAUSE BUFFERING";
                if (autoMovePosition != 0)
                {
                    pauseMessage = (autoMovePosition > 0) ? "FWD" : "REV";
                }
                textPaint.setColor(Color.YELLOW);
                Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
                canvas.drawText(pauseMessage, MARGIN_X, canvas.getHeight() - fontMetrics.bottom - MARGIN_Y, textPaint);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void drawImage(Canvas canvas, int seekbarLocation)
    {
        try
        {
            CachedImageData data = imageHolder.getBufferedImage(seekbarLocation);
            if (data == null)
            {
                // 描画できないので終了する
                return;
            }
            byte[] imageData = data.getImage();
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
            if (bitmap == null)
            {
                // 画像が取得できないので終了する。
                return;
            }

            // キャンバスを回転...
            int rotationDegrees = getRotationDegree(data.getMetadata());
            int centerX = canvas.getWidth() / 2;
            int centerY = canvas.getHeight() / 2;
            canvas.rotate(rotationDegrees, centerX, centerY);

            //  画像の描画エリア決定
            RectF drawArea = decideViewRect(canvas, bitmap, rotationDegrees);
            if (drawArea == null)
            {
                // 画像が異常なので終了する
                return;
            }

            // 画像の描画
            Rect imageRect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
            canvas.drawBitmap(bitmap, imageRect, drawArea, null);

            // キャンバスの回転を戻す
            canvas.rotate(-rotationDegrees, centerX, centerY);
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
    }

    private int getRotationDegree(Map<String, Object> metadata)
    {
        int rotationDegrees = 0;
        if (metadata == null)
        {
            return (rotationDegrees);
        }
        try
        {
            // Acquire a rotation degree of image.
            int orientation = ExifInterface.ORIENTATION_UNDEFINED;
            if (metadata.containsKey(EXIF_ORIENTATION))
            {
                String value = (String) metadata.get(EXIF_ORIENTATION);
                if (value != null)
                {
                    orientation = Integer.parseInt(value);
                }
            }
            switch (orientation)
            {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotationDegrees = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotationDegrees = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotationDegrees = 270;
                    break;
                case ExifInterface.ORIENTATION_NORMAL:
                default:
                    rotationDegrees = 0;
                    break;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return (rotationDegrees);
    }

    private RectF decideViewRect(Canvas canvas, Bitmap bitmapToShow, int degrees)
    {
        final int srcWidth;
        final int srcHeight;
        boolean isRotate = ((degrees != 0)&&(degrees != 180));
        if (isRotate)
        {
            // 回転表示
            srcWidth = bitmapToShow.getHeight();
            srcHeight = bitmapToShow.getWidth();
        }
        else
        {
            // 通常表示
            srcWidth = bitmapToShow.getWidth();
            srcHeight = bitmapToShow.getHeight();
        }
        if ((srcWidth == 0)||(srcHeight == 0))
        {
            // 画像の大きさが異常...
            return (null);
        }

        int maxWidth = canvas.getWidth();
        int maxHeight = canvas.getHeight();

        int centerX = canvas.getWidth() / 2;
        int centerY = canvas.getHeight() / 2;

        float widthRatio = maxWidth / (float) srcWidth;
        float heightRatio = maxHeight / (float) srcHeight;
        float smallRatio = Math.min(widthRatio, heightRatio);

        final int dstWidth;
        final int dstHeight;
        if (widthRatio < heightRatio)
        {
            dstWidth = maxWidth;
            dstHeight = (int)(smallRatio * srcHeight);
        }
        else
        {
            dstHeight = maxHeight;
            dstWidth = (int)(smallRatio * srcWidth);
        }

        final float halfWidth = dstWidth * 0.5f;
        final float halfHeight = dstHeight * 0.5f;
        if (isRotate)
        {
            // 回転表示
            return (new RectF(centerX - halfHeight, centerY - halfWidth, centerX - halfHeight + dstHeight, centerY - halfWidth + dstWidth));
        }
        // 通常表示
        return (new RectF(centerX - halfWidth, centerY - halfHeight, centerX - halfWidth + dstWidth, centerY - halfHeight + dstHeight));
    }

    @Override
    public boolean onDown(MotionEvent e)
    {
        Log.v(TAG, " onDown()");
        return (false);
    }

    @Override
    public void onShowPress(MotionEvent e)
    {
        Log.v(TAG, " onShowPress()");
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e)
    {
        Log.v(TAG, " onSingleTapUp()");
        if (!imageHolder.getBufferedImageStatus())
        {
            // バッファリング停止中にタッチした場合は、再生を停止する
            autoMovePosition = 0;
            vibrate(30);
            return (true);
        }
        return (false);
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
    {
        Log.v(TAG, " onScroll()");
        return (false);
    }

    @Override
    public void onLongPress(MotionEvent e)
    {
        // Log.v(TAG, " onLongPress()");
        int nofBuffer = imageHolder.getNumberOfBufferedImages();
        int maxBuffer = imageHolder.getMaxBufferImages();
        if (nofBuffer < maxBuffer)
        {
            // バッファーがいっぱいではないときは、画像のバッファリングを止めない。
            return;
        }

        // フラグを反転させる（バッファリング中なら止める、停止中ならバッファリングを再開する
        imageHolder.updateBufferedImageStatus(!imageHolder.getBufferedImageStatus());
        autoMovePosition = 0;
        vibrate(70);
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
    {
        Log.v(TAG, "  onFling() ");
        if ((!imageHolder.getBufferedImageStatus())&&(Math.abs(velocityX) > Math.abs(velocityY)))
        {
            // バッファリング停止中に、再生を開始する
            if (velocityX < 0.0f)
            {
                // 逆方向
                autoMovePosition = (autoMovePosition == -1) ? -3 : -1;
            }
            else
            {
                // 順方向
                autoMovePosition = (autoMovePosition == 1) ? 3 : 1;
            }
            vibrate(30);
            return (true);
        }
        else if ((Math.abs(velocityX) < Math.abs(velocityY))&&(velocityY > 50.0f))
        {
            // 画像をキャプチャする
            doCapture();
            vibrate(50);
            return (true);
        }
        return (false);
    }


    /**
     *   画面を保存する
     *
     */
    private void doCapture()
    {
        try
        {
            CachedImageData data = imageHolder.getBufferedImage(previousPosition);
            if (data == null)
            {
                // 描画できないので終了する
                return;
            }
            byte[] imageData = data.getImage();
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
            if (bitmap == null)
            {
                // 画像が取得できないので終了する。
                return;
            }
            storeImage.doStore(bitmap, null, false);
        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v)
    {
        Log.v(TAG, "  onClick() ");
    }

    @Override
    public boolean onLongClick(View v)
    {
        Log.v(TAG, "  onLongClick() ");
        return (false);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        boolean ret = gestureDetector.onTouchEvent(event);
        if (!ret)
        {
            return (v.performClick());
        }
        return (true);
    }

    @Override
    public void onUpdateStatus(OLYCamera olyCamera, String s)
    {
        // Log.v(TAG, " onUpdateStatus() : " + s);
    }

    @Override
    public void updateBufferedImage()
    {
        try
        {
            if (Looper.getMainLooper().getThread() == Thread.currentThread())
            {
                parent.invalidate();
            }
            else
            {
                parent.postInvalidate();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void vibrate(final int milliSeconds)
    {
        try
        {
            final Vibrator vibrator = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null)
            {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run()
                    {
                        try
                        {
                            vibrator.vibrate(milliSeconds);
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                });
                thread.start();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}

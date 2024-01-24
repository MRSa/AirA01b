package jp.osdn.gokigen.aira01b.manipulate;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.exifinterface.media.ExifInterface;
import jp.osdn.gokigen.aira01b.R;

/**
 *  画像イメージの表示調整を行う
 *
 */
class ImageManipulator implements IImageManipulatorOperator
{
    private static final String TAG = ImageManipulator.class.getSimpleName();

    private ProgressDialog loadingDialog = null;
    private Activity  parent = null;

    private static final int LONG_SIDE = 640;

    private static final int BITMAP_MAX_WIDTH = 2048;
    private static final int BITMAP_MAX_HEIGHT = 1600;

    private static final int BITMAP_MIN_WIDTH = 360;
    private static final int BITMAP_MIN_HEIGHT = 240;

    private static final int BITMAP_MAX_COLOR = 4;

    private Bitmap pinpBaseBitmap = null;
    private Bitmap pinpOverlayBitmap = null;

    /**
     *
     *
     */
    ImageManipulator(Activity activity)
    {
        loadingDialog = new ProgressDialog(activity);
        parent = activity;
    }

    /**
     *   画面にイメージを表示する
     *
     */
    void setImage(final ImageView view, final String targetImage)
    {
        Log.v(TAG, "setImage() start : " + targetImage);

        //  プログレスダイアログ（「ロード中...」）を表示する。
        loadingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        loadingDialog.setMessage(parent.getString(R.string.data_loading));
        loadingDialog.setIndeterminate(true);
        loadingDialog.setCancelable(false);
        loadingDialog.show();
        view.setImageResource(android.R.color.transparent); // 表示をクリア

        //　イメージ表示の実処理。。。
        Thread thread = new Thread(new Runnable()
        {
            public void run()
            {
                // URIから画像を設定する...OutOfMemory対策付き
                final Bitmap bitmap = getBitmapFromFile(targetImage, LONG_SIDE);
                parent.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if (bitmap != null)
                        {
                            Log.v(TAG, "BITMAP SIZE : (" + bitmap.getWidth() + "," + bitmap.getHeight() + ")  WIDGET SIZE : (" + view.getWidth() + "," + view.getHeight() + ")");
                            float rate = (float) view.getWidth() / (float) bitmap.getWidth();
                            int width = (int) ((float) bitmap.getWidth() * rate);
                            int height = (int) ((float) bitmap.getHeight() * rate);
                            Bitmap targetBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
                            bitmap.recycle();
                            view.setImageBitmap(targetBitmap);
                            view.invalidate();
                        }
                        else
                        {
                            // ファイルの読み込みが失敗した場合...
                            Toast.makeText(parent, R.string.cannot_load_bitmap, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                Log.v(TAG, "setImage() end : " + targetImage);
                System.gc();
                loadingDialog.dismiss();
            }
        });
        try
        {
            thread.start();
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
    }

    /**
     *   ファイルからビットマップデータを取得する
     *
     */
    private Bitmap getBitmapFromFile(String fileName, int maxPixel)
    {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        int rotation = 0;
        try
        {
            // OutOfMemoryエラー対策...一度読み込んで画像サイズを取得
            opt.inJustDecodeBounds = true;
            //opt.inDither = true;
            BitmapFactory.decodeFile(fileName, opt);
            rotation = getRotation(fileName);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }

        // 画像の縮小サイズを決定する (縦幅、横幅の小さいほうにあわせる)
        int widthBounds = opt.outWidth / maxPixel;
        int heightBounds = opt.outHeight / maxPixel;
        opt.inSampleSize = Math.min(widthBounds, heightBounds);
        opt.inJustDecodeBounds = false;

        // 画像ファイルを応答する
        Bitmap retBitmap = null;
        try
        {
            retBitmap = BitmapFactory.decodeFile(fileName, opt);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }

        // 角度がない、もしくはbitmap解析失敗の時...
        if ((rotation == 0)||(retBitmap == null))
        {
            Log.v(TAG, "BITMAP ");
            return (retBitmap);
        }

        // ビットマップを回転させて応答する
        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);
        Bitmap bmp = Bitmap.createBitmap(retBitmap, 0, 0, retBitmap.getWidth(), retBitmap.getHeight(), matrix, true);
        Log.v(TAG, "BITMAP (" + retBitmap.getWidth() + "," + retBitmap.getHeight() + ") [" + rotation + "]");
        retBitmap.recycle();
        return (bmp);
    }

    /**
     *   出力するビットマップの領域を確保する
     *
     * @param width   幅
     * @param height  高さ
     * @return  ビットマップ領域
     */
    private Bitmap allocateBitmap(int width, int height)
    {
        ActivityManager activityManager = (ActivityManager) parent.getSystemService(Context.ACTIVITY_SERVICE);
        try
        {
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memoryInfo);

            Runtime runtime = Runtime.getRuntime();
            runtime.gc();

            Log.v(TAG, "AVAILABLE MEMORY : " + memoryInfo.availMem + "  " + runtime.freeMemory() + "  [" + runtime.maxMemory() + "] " + runtime.totalMemory());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        // ビットマップ領域として ARGB_8888 で使用するサイズ
        double memoryToUse = width * height * BITMAP_MAX_COLOR;
        //double availableMemory = runtime.freeMemory() * 0.95;

        // 加工可能なビットマップサイズの最大サイズ
        double availableSize = BITMAP_MAX_WIDTH * BITMAP_MAX_HEIGHT * BITMAP_MAX_COLOR * 2;

        Log.v(TAG, "CREATE BITMAP : " + width + "," + height + " size : " + (long) memoryToUse + "(Avail. : " + availableSize + ")");

        if (memoryToUse >= availableSize)
        {
            if ((memoryToUse / 2.0d) >= availableSize)
            {
                // MEMORY OVERFLOW...
                return (null);
            }
            return (Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565));
        }
        return (Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888));
    }

    /**
     *   画像を上下結合
     *
     * @return 上下に結合した画像
     */
    @Override
    public Bitmap processCombineUpDown(String upperImageFileName, String lowerImageFileName, boolean isResized)
    {
        try
        {
            ///**  画像のサイズ等を先読み **/
            BitmapFactory.Options opt1 = new BitmapFactory.Options();
            opt1.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(upperImageFileName, opt1);
            opt1.inJustDecodeBounds = false;

            BitmapFactory.Options opt2 = new BitmapFactory.Options();
            opt2.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(lowerImageFileName, opt2);
            opt2.inJustDecodeBounds = false;

            ///** 画像の回転角度を取得して、画像の縦横サイズを確定する **/
            int rotation1 = getRotation(upperImageFileName);
            int w1 = opt1.outWidth;
            int h1 = opt1.outHeight;
            if ((rotation1 == 90)||(rotation1 == 270))
            {
                w1 = opt1.outHeight;
                h1 = opt1.outWidth;
            }

            int rotation2 = getRotation(lowerImageFileName);
            int w2 = opt2.outWidth;
            int h2 = opt2.outHeight;
            if ((rotation2 == 90)||(rotation2 == 270))
            {
                w2 = opt2.outHeight;
                h2 = opt2.outWidth;
            }

            ///** 画像サイズが大きい場合は、ビットマップの色数を落としてデータサイズを減らす **/
            //if (Math.max(w1, h1) > (BITMAP_MAX_WIDTH * 2))
            if ((w1 * h1) > (BITMAP_MAX_WIDTH * BITMAP_MAX_HEIGHT * 2))
            {
                // サイズが大きすぎるので...ビットマップのデータサイズを半分にする
                opt1.inPreferredConfig = Bitmap.Config.RGB_565;
            }

            //if (Math.max(w2, h2) > (BITMAP_MAX_WIDTH * 2))
            if ((w2 * h2) > (BITMAP_MAX_WIDTH * BITMAP_MAX_HEIGHT * 2))
            {
                // サイズが大きすぎるので...ビットマップのデータサイズを半分にする
                opt2.inPreferredConfig = Bitmap.Config.RGB_565;
            }

            ///**  画像の幅を決める **/
            float outputWidth = BITMAP_MAX_WIDTH;
            if ((w1 >= w2)&&(BITMAP_MAX_WIDTH > w2))
            {
                // 規準出力サイズを w2にする
                outputWidth = w2;

            }
            else if ((w2 >= w1)&&(BITMAP_MAX_WIDTH > w1))
            {
                // 規準出力サイズを w1にする
                outputWidth = w1;
            }
            if (outputWidth < BITMAP_MIN_WIDTH)
            {
                // 規準出力サイズが小さすぎる場合には補正する
                outputWidth = BITMAP_MIN_WIDTH;
            }

            float rate1 = outputWidth / w1;
            if ((!isResized)||(rate1 > 1.0f))
            {
                // リサイズ指示ではない、もしくは画像サイズがオリジナルより大きくなる場合
                rate1 = 1.0f;
            }

            float rate2 = outputWidth / w2;
            if ((!isResized)||(rate2 > 1.0f))
            {
                // リサイズ指示ではない、もしくは画像サイズがオリジナルより大きくなる場合
                rate2 = 1.0f;
            }

/*
            float rate1;
            if (w1 > h1)
            {
                rate1 = (float) BITMAP_MAX_WIDTH / (float) w1;
            }
            else
            {
                rate1 = (float) BITMAP_MAX_HEIGHT / (float) h1;
            }
            if ((!isResized)||(rate1 > 1.0f))
            {
                rate1 = 1.0f;
            }

            float rate2;
            if (w2 > h2)
            {
                rate2 = (float) BITMAP_MAX_WIDTH / (float) w2;
            }
            else
            {
                rate2 = (float) BITMAP_MAX_HEIGHT / (float) h2;
            }
            if ((!isResized)||(rate2 > 1.0f))
            {
                rate2 = 1.0f;
            }
*/
            float dw1 = (float) w1 * rate1;
            float dh1 = (float) h1 * rate1;

            float dw2 = (float) w2 * rate2;
            float dh2 = (float) h2 * rate2;

            Matrix mat1 = new Matrix();
            mat1.postRotate(rotation1);
            mat1.postScale(rate1, rate1);

            Matrix mat2 = new Matrix();
            mat2.postRotate(rotation2);
            mat2.postScale(rate2, rate2);

            float total_height = (dh1 + dh2);
            float max_width = ((dw1 > dw2) ? dw1 : dw2);

            Bitmap target = allocateBitmap((int) max_width, (int) total_height);
            if (target != null)
            {
                Canvas bitmapCanvas = new Canvas(target);
                {
                    Bitmap retBitmap1 = BitmapFactory.decodeFile(upperImageFileName, opt1);
                    Bitmap bmp1 = Bitmap.createBitmap(retBitmap1, 0, 0, retBitmap1.getWidth(), retBitmap1.getHeight(), mat1, true);
                    int horizontalMargin = ((int) max_width - bmp1.getWidth()) / 2;
                    bitmapCanvas.drawBitmap(bmp1, horizontalMargin, 0, null);
                    retBitmap1.recycle();
                    bmp1.recycle();
                }
                {
                    Bitmap retBitmap2 = BitmapFactory.decodeFile(lowerImageFileName, opt2);
                    Bitmap bmp2 = Bitmap.createBitmap(retBitmap2, 0, 0, retBitmap2.getWidth(), retBitmap2.getHeight(), mat2, true);
                    int horizontalMargin = ((int) max_width - bmp2.getWidth()) / 2;
                    bitmapCanvas.drawBitmap(bmp2, horizontalMargin, (int) dh1, null);
                    retBitmap2.recycle();
                    bmp2.recycle();
                }
            }
            else
            {
                parent.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Toast.makeText(parent, parent.getString(R.string.warning_picture_size_exceeded), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            System.gc();

            return (target);
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
        return (null);
    }

    /**
     *   画像を左右結合
     *
     * @param leftImageFileName 左側画像のファイル名
     * @param rightImageFileName 右側画像のファイル名
     * @return 左右に結合した画像
     */
    @Override
    public Bitmap processCombineLeftRight(String leftImageFileName, String rightImageFileName, boolean isResized)
    {
        try
        {
            ///**  画像サイズを知るために事前読み込み **/
            BitmapFactory.Options opt1 = new BitmapFactory.Options();
            opt1.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(leftImageFileName, opt1);
            opt1.inJustDecodeBounds = false;
            //opt1.inDither = true;

            BitmapFactory.Options opt2 = new BitmapFactory.Options();
            opt2.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(rightImageFileName, opt2);
            opt2.inJustDecodeBounds = false;
            //opt2.inDither = true;

            ///** 左側の画像のサイズ **/
            int rotation1 = getRotation(leftImageFileName);
            int w1 = opt1.outWidth;
            int h1 = opt1.outHeight;
            if ((rotation1 == 90)||(rotation1 == 270))
            {
                w1 = opt1.outHeight;
                h1 = opt1.outWidth;
            }

            ///** 右側の画像のサイズ **/
            int rotation2 = getRotation(rightImageFileName);
            int w2 = opt2.outWidth;
            int h2 = opt2.outHeight;
            if ((rotation2 == 90)||(rotation2 == 270))
            {
                w2 = opt2.outHeight;
                h2 = opt2.outWidth;
            }

            ///** 画像サイズが大きい場合は、ビットマップの色数を落としてデータサイズを減らす **/
            //if (Math.max(w1, h1) > (BITMAP_MAX_WIDTH * 2))
            if ((w1 * h1) > (BITMAP_MAX_WIDTH * BITMAP_MAX_HEIGHT * 2))
            {
                // サイズが大きすぎるので...ビットマップのデータサイズを半分にする
                opt1.inPreferredConfig = Bitmap.Config.RGB_565;
            }

            //if (Math.max(w2, h2) > (BITMAP_MAX_WIDTH * 2))
            if ((w2 * h2) > (BITMAP_MAX_WIDTH * BITMAP_MAX_HEIGHT * 2))
            {
                // サイズが大きすぎるので...ビットマップのデータサイズを半分にする
                opt2.inPreferredConfig = Bitmap.Config.RGB_565;
            }

            ///**  画像の高さを決める **/
            float outputHeight = BITMAP_MAX_HEIGHT;
            if ((h1 >= h2)&&(BITMAP_MAX_HEIGHT > h2))
            {
                // 規準出力サイズを h2にする
                outputHeight = h2;

            }
            else if ((h2 >= h1)&&(BITMAP_MAX_HEIGHT > h1))
            {
                // 規準出力サイズを h1にする
                outputHeight = h1;
            }
            if (outputHeight < BITMAP_MIN_HEIGHT)
            {
                // 規準出力サイズが小さすぎる場合には補正する
                outputHeight = BITMAP_MIN_HEIGHT;
            }

            float rate1 = outputHeight / (float) h1;
            /*
            if (w1 > h1)
            {
                rate1 = (float) BITMAP_MAX_WIDTH / (float) w1;
            }
            else
            {
                rate1 = (float) BITMAP_MAX_HEIGHT / (float) h1;
            }
            */

            // オリジナルサイズよりも大きくはしない
            if ((rate1 > 1.0f)||(!isResized))
            {
                rate1 = 1.0f;
            }

            float rate2 = outputHeight / (float) h2;
/*
            if (w2 > h2)
            {
                rate2 = (float) BITMAP_MAX_WIDTH / (float) w2;
            }
            else
            {
                rate2 = (float) BITMAP_MAX_HEIGHT / (float) h2;
            }
*/
            // オリジナルサイズよりも大きくはしない
            if ((rate2 > 1.0f)||(!isResized))
            {
                rate2 = 1.0f;
            }

            float dw1 = (float) w1 * rate1;
            float dh1 = (float) h1 * rate1;

            float dw2 = (float) w2 * rate2;
            float dh2 = (float) h2 * rate2;

            Matrix mat1 = new Matrix();
            mat1.postRotate(rotation1);
            mat1.postScale(rate1, rate1);

            Matrix mat2 = new Matrix();
            mat2.postRotate(rotation2);
            mat2.postScale(rate2, rate2);

            float max_height = (dh1 > dh2) ? dh1 : dh2;
            float total_width =dw1 + dw2;

            Bitmap target = allocateBitmap((int) total_width, (int) max_height);
            if (target != null)
            {
                Canvas bitmapCanvas = new Canvas(target);
                if (isResized)
                {
                    Bitmap bitmap1 = BitmapFactory.decodeFile(leftImageFileName, opt1);
                    Bitmap bmp1 = Bitmap.createBitmap(bitmap1, 0, 0, bitmap1.getWidth(), bitmap1.getHeight(), mat1, true);
                    int verticalMargin1 = ((int) max_height - bmp1.getHeight()) / 2;
                    bitmapCanvas.drawBitmap(bmp1, 0, verticalMargin1, null);
                    bitmap1.recycle();
                    bmp1.recycle();

                    Bitmap bitmap2 = BitmapFactory.decodeFile(rightImageFileName, opt2);
                    Bitmap bmp2 = Bitmap.createBitmap(bitmap2, 0, 0, bitmap2.getWidth(), bitmap2.getHeight(), mat2, true);
                    int verticalMargin2 = ((int) max_height - bmp2.getHeight()) / 2;
                    bitmapCanvas.drawBitmap(bmp2, (int) dw1, verticalMargin2, null);
                    bitmap2.recycle();
                    bmp2.recycle();
                }
                else
                {
                    Bitmap bitmap1 = BitmapFactory.decodeFile(leftImageFileName);
                    Bitmap bmp1 = Bitmap.createBitmap(bitmap1, 0, 0, bitmap1.getWidth(), bitmap1.getHeight(), mat1, true);
                    int verticalMargin1 = ((int) max_height - bmp1.getHeight()) / 2;
                    bitmapCanvas.drawBitmap(bmp1, 0, verticalMargin1, null);
                    bitmap1.recycle();
                    bmp1.recycle();

                    Bitmap bitmap2 = BitmapFactory.decodeFile(rightImageFileName);
                    Bitmap bmp2 = Bitmap.createBitmap(bitmap2, 0, 0, bitmap2.getWidth(), bitmap2.getHeight(), mat2, true);

                    int verticalMargin2 = ((int) max_height - bmp2.getHeight()) / 2;
                    bitmapCanvas.drawBitmap(bmp2, (int) dw1, verticalMargin2, null);
                    bitmap2.recycle();
                    bmp2.recycle();
                }
            }
            else
            {
                parent.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Toast.makeText(parent, parent.getString(R.string.warning_picture_size_exceeded), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            System.gc();
            return (target);
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
        return (null);
    }

    /**
     *   ピクチャーインピクチャー画像を応答する。
     *
     * @param leftImageFileName    大きい画像
     * @param rightImageFileName   小さい画像
     * @param resizeRate            小さい画像の比率
     * @param center                小さい画像の中心座標
     * @return  ビットマップ
     */
    @Override
    public Bitmap processPictureInPicture(String leftImageFileName, String rightImageFileName, float resizeRate, PointF center)
    {
        pinpBaseBitmap = drawBackgroundBitmap(leftImageFileName);
        if (pinpBaseBitmap == null)
        {
            // 画像取得失敗のときはそのまま応答...
            return (null);
        }
        try
        {
            // 縦横の大きさを得る
            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(rightImageFileName, opt);
            opt.inJustDecodeBounds = false;
            //opt1.inDither = true;

            int rotation = getRotation(rightImageFileName);
            int w1 = opt.outWidth;
            int h1 = opt.outHeight;
            if ((rotation == 90) || (rotation == 270))
            {
                w1 = opt.outHeight;
                h1 = opt.outWidth;
            }
            if (Math.max(w1, h1) > (BITMAP_MAX_WIDTH * 2))
            {
                // サイズが大きすぎるので...読み込むビットマップのデータサイズを半分にする
                opt.inPreferredConfig = Bitmap.Config.RGB_565;
            }

            // 縮小サイズが小さすぎる場合は補正する。
            if (resizeRate < 0.001f)
            {
                resizeRate = 0.001f;
            }

            float rate;
            if (w1 > h1) {
                rate = (float) pinpBaseBitmap.getWidth() * resizeRate / (float) w1;
            } else {
                rate = (float) pinpBaseBitmap.getHeight() * resizeRate / (float) h1;
            }
            if (rate > 1.0f)
            {
                rate = 1.0f;
            }
            Log.v(TAG, "pinpOverlayBitmap RATE : (" + pinpBaseBitmap.getWidth() + "," +  pinpBaseBitmap.getHeight() + ") >" + resizeRate + "< (" + w1 + "," + h1 + ")");

            Matrix mat = new Matrix();
            mat.postRotate(rotation);
            mat.postScale(rate, rate);

            float marginX = ((rate * opt.outWidth) / 2.0f);
            float marginY = ((rate * opt.outHeight) / 2.0f);

            float posX = (center.x < 0) ? 0.0f : pinpBaseBitmap.getWidth() * (center.x);
            float posY = (center.y < 0) ? 0.0f : pinpBaseBitmap.getHeight() * (center.y);

            posX = (posX < marginX) ? 0.0f : (posX - marginX);
            posY = (posY < marginY) ? 0.0f : (posY - marginY);


            Bitmap canvasBitmap = pinpBaseBitmap.copy(Bitmap.Config.ARGB_8888, true);
            Canvas bitmapCanvas = new Canvas(canvasBitmap);
            Bitmap bitmap = BitmapFactory.decodeFile(rightImageFileName, opt);
            pinpOverlayBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), mat, true);
            Log.v(TAG, "pinpOverlayBitmap  createBitmap : " + pinpOverlayBitmap.getWidth() + "," + pinpOverlayBitmap.getHeight() + " rate : " + rate);
            bitmapCanvas.drawBitmap(pinpOverlayBitmap, posX, posY, null);
            //bitmap.recycle();
            return (canvasBitmap);
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
        return (pinpBaseBitmap);
    }

    @Override
    public Bitmap updatePictureInPicture(float resizeRate, PointF center)
    {
        if (pinpBaseBitmap == null)
        {
            return (null);
        }
        try
        {
            Log.v(TAG, "pinpOverlayBitmap : " + pinpOverlayBitmap.getWidth() + "," + pinpOverlayBitmap.getHeight());
            float marginX =(pinpOverlayBitmap.getWidth()) / 2.0f;
            float marginY = (pinpOverlayBitmap.getHeight()) / 2.0f;

            float overlayWidth = pinpOverlayBitmap.getWidth() * resizeRate;
            float overlayHeight = pinpOverlayBitmap.getHeight() * resizeRate;
            if (overlayWidth > pinpOverlayBitmap.getWidth())
            {
                overlayWidth = pinpOverlayBitmap.getWidth();
            }
            if (overlayHeight > pinpOverlayBitmap.getHeight())
            {
                overlayHeight = pinpOverlayBitmap.getHeight();
            }
            overlayWidth = (overlayWidth < 10.0f) ? 10.0f : overlayWidth;
            overlayHeight = (overlayHeight < 10.0f) ? 10.0f : overlayHeight;
            Log.v(TAG, "overlayWidth, overlayHeight: " + overlayWidth + "," + overlayHeight);

            float posX = (center.x < 0) ? 0.0f : pinpBaseBitmap.getWidth() * (center.x);
            float posY = (center.y < 0) ? 0.0f : pinpBaseBitmap.getHeight() * (center.y);

            posX = (posX < marginX) ? 0.0f : (posX - marginX);
            posY = (posY < marginY) ? 0.0f : (posY - marginY);

            Bitmap canvasBitmap = pinpBaseBitmap.copy(Bitmap.Config.ARGB_8888, true);
            Canvas bitmapCanvas = new Canvas(canvasBitmap);
            Bitmap overlayBitmap = Bitmap.createScaledBitmap(pinpOverlayBitmap, (int) overlayWidth, (int) overlayHeight, true);
            bitmapCanvas.drawBitmap(overlayBitmap, posX, posY, null);

            return (canvasBitmap);
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
        return (pinpBaseBitmap);
    }

    @Override
    public void clearPictureInPicture()
    {
        // System.gc();
    }

    /**
     *   大きい方の画像を応答する
     *
     * @param imageFileName  ファイル名
     * @return  大きい画像
     */
    private Bitmap drawBackgroundBitmap(String imageFileName)
    {
        Bitmap target = null;
        try {
            BitmapFactory.Options opt1 = new BitmapFactory.Options();
            opt1.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imageFileName, opt1);
            opt1.inJustDecodeBounds = false;
            //opt1.inDither = true;

            int rotation1 = getRotation(imageFileName);
            int w1 = opt1.outWidth;
            int h1 = opt1.outHeight;
            if ((rotation1 == 90) || (rotation1 == 270))
            {
                w1 = opt1.outHeight;
                h1 = opt1.outWidth;
            }

            if (Math.max(w1, h1) > (BITMAP_MAX_WIDTH * 2))
            {
                // サイズが大きすぎるので...読み込むビットマップのデータサイズを半分にする
                opt1.inPreferredConfig = Bitmap.Config.RGB_565;
            }

            float rate1;
            if (w1 > h1) {
                rate1 = (float) BITMAP_MAX_WIDTH / (float) w1;
            } else {
                rate1 = (float) BITMAP_MAX_HEIGHT / (float) h1;
            }
            if (rate1 > 1.0f)
            {
                rate1 = 1.0f;
            }
            float dw1 = (float) w1 * rate1;
            float dh1 = (float) h1 * rate1;
            Matrix mat1 = new Matrix();
            mat1.postRotate(rotation1);
            mat1.postScale(rate1, rate1);

            target = allocateBitmap((int) dw1, (int) dh1);
            if (target != null)
            {
                Canvas bitmapCanvas = new Canvas(target);
                Bitmap bitmap1 = BitmapFactory.decodeFile(imageFileName, opt1);
                Bitmap bmp1 = Bitmap.createBitmap(bitmap1, 0, 0, bitmap1.getWidth(), bitmap1.getHeight(), mat1, true);
                bitmapCanvas.drawBitmap(bmp1, 0, 0, null);
                bitmap1.recycle();
                bmp1.recycle();
            }
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
        return (target);
    }

    /**
     *   画像の回転角度を求める
     *
     * @param imageFile イメージファイル
     * @return 回転角度
     */
    private int getRotation(String imageFile)
    {
        int rotation = 0;
        try
        {
            ExifInterface exifInterface = new ExifInterface(imageFile);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation)
            {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotation = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotation = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotation = 270;
                    break;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return (rotation);
    }
}

package jp.osdn.gokigen.aira01b.olycamerawrapper.takepicture;

import android.graphics.Color;
import android.graphics.PointF;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jp.co.olympus.camerakit.OLYCamera;
import jp.co.olympus.camerakit.OLYCameraAutoFocusResult;
import jp.osdn.gokigen.aira01b.IAirA01BInterfacesProvider;
import jp.osdn.gokigen.aira01b.liveview.IAutoFocusFrameDisplay;
import jp.osdn.gokigen.aira01b.liveview.IMessageDrawer;
import jp.osdn.gokigen.aira01b.liveview.IStatusViewDrawer;
import jp.osdn.gokigen.aira01b.olycamerawrapper.IOlyCameraProperty;

/**
 *   オートブラケッティング実行クラス
 *
 *
 * Created by MRSa on 2016/06/18.
 */
public class BracketingShotControl implements OLYCamera.TakePictureCallback
{
    private final String TAG = toString();

    private static final int BRACKETING_INTERVAL_MILLISECOND = 300; // 撮影待ち時間(ms)
    private static final int BRACKETING_PROPERTY_VALUE_SET_RETRY = 10;

    private static final int BRACKET_NONE = 0;       // 通常のショット
    private static final int BRACKET_EXPREV = 1;     // 露出補正
    private static final int BRACKET_APERTURE = 2;   // 絞り
    private static final int BRACKET_ISO = 3;         // ISO
    private static final int BRACKET_SHUTTER = 4;    // シャッター
    private static final int BRACKET_WB = 5;          // ホワイトバランス
    private static final int BRACKET_COLOR_TONE = 6; // カラートーン

    private final OLYCamera camera;
    private final IAirA01BInterfacesProvider provider;
    private final IAutoFocusFrameDisplay autoFocusFrame;
    private boolean isShootingWait = false;
    private boolean isBracketingAction = false;
    private int retryUpdateBracketingStatus = 0;
    private int waitSeconds = 0;  // 撮影待ち時間

    private int bracketCount = 0;
    private String targetPropertyName = null;
    private String originalProperty = null;
    private int  originalPropertyIndex = -1;
    private List<String> propertyValueList = null;

    /**
     *　 コンストラクタ
     *
     */
    public BracketingShotControl(OLYCamera camera, IAirA01BInterfacesProvider provider)
    {
        this.camera = camera;
        this.provider = provider;
        this.autoFocusFrame = provider.getAutoFocusFrameInterface();
    }

    /**
     *　 ブラケッティング対象のプロパティの現在設定値と、その選択肢を記憶する
     *
     * @param name ブラケッティング対象の名前
     * @return  ブラケッティングの現在設定値
     */
    private int prepareBracketProperty(String name)
    {
        try
        {
            targetPropertyName = name;
            if (name.length() > 0)
            {
                originalProperty = camera.getCameraPropertyValue(name);
                propertyValueList = camera.getCameraPropertyValueList(name);
                if (bracketCount < 0)
                {
                    bracketCount = propertyValueList.size();
                }
                return (propertyValueList.indexOf(originalProperty));
            }
            else
            {
                originalProperty = null;
                propertyValueList = null;
            }
        }
        catch (Exception e)
        {
            originalProperty = null;
            propertyValueList = null;
            e.printStackTrace();
            System.gc();
        }
        return (-1);
    }


    /**
     *   ブラケッティング対象のプロパティを特定する
     *
     * @param isBracketing プロパティ
     * @return true : 対象の特定完了 / false : 対象の特定失敗
     */
    private boolean decideBracketProperty(int isBracketing)
    {
        switch (isBracketing)
        {
            case BRACKET_EXPREV:
                // 露出ブラケット
                targetPropertyName = IOlyCameraProperty.EXPOSURE_COMPENSATION;
                break;

            case BRACKET_APERTURE:
                // 絞り値設定
                targetPropertyName = IOlyCameraProperty.APERTURE;
                break;

            case BRACKET_ISO:
                // ISO
                targetPropertyName = IOlyCameraProperty.ISO_SENSITIVITY;
                break;

            case BRACKET_SHUTTER:
                // シャッターブラケット
                targetPropertyName = IOlyCameraProperty.SHUTTER_SPEED;
                break;

            case BRACKET_WB:
                // ホワイトバランスブラケット
                targetPropertyName = IOlyCameraProperty.WB_MODE;
                bracketCount = -1;
                break;

            case BRACKET_COLOR_TONE:
                // ピクチャーモードブラケット
                targetPropertyName = IOlyCameraProperty.COLOR_TONE;
                bracketCount = -1;
                break;

            case BRACKET_NONE:
                // パラメータは変更しないパターン...
                targetPropertyName = "";
                break;

            default:
                // 何もしない
                return (false);
        }
        originalPropertyIndex = prepareBracketProperty(targetPropertyName);
        return (true);
    }


    /**
     *  写真撮影(ブラケッティング撮影)を開始する
     *    bracketingStyle : ブラケッティングスタイル
     *    bracketingCount : 撮影枚数
     *    durationSeconds : 撮影間隔（単位：秒）
     */
    public void startShootBracketing(int bracketingStyle, int bracketingCount, int durationSeconds)
    {
        if ((camera.isTakingPicture())||(camera.isRecordingVideo())||(isBracketingAction))
        {
            // スチル or ムービー撮影中、ブラケッティング撮影中なら、何もしない
            return;
        }

        // ブラケッティング撮影の準備
        bracketCount = bracketingCount;
        if (!decideBracketProperty(bracketingStyle))
        {
            // ブラケッティング指定ではないので、何もせずに終了する
            return;
        }

        // 撮影間隔 (単位：秒)
        waitSeconds = durationSeconds;

        // ブラケッティング撮影開始！ (別スレッドでブラケッティング撮影を開始する）
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(new Runnable()
        {
            @Override
            public void run()
            {
                isBracketingAction = true;
                updateMessage("INT");
                try
                {
                    startBracket();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                isBracketingAction = false;
                updateMessage("");
            }
        });
    }

    /**
     *   画面にメッセージを表示する
     *
     * @param msg  表示するメッセージ
     */
    private void updateMessage(String msg)
    {
        if (provider != null)
        {
            IStatusViewDrawer statusDrawer = provider.getStatusViewDrawer();
            if (statusDrawer != null)
            {
                statusDrawer.getMessageDrawer().setMessageToShow(IMessageDrawer.MessageArea.UPCENTER, Color.CYAN, IMessageDrawer.SIZE_LARGE, msg);
            }
        }
    }

    /**
     *   ブラケッティング撮影を開始する
     *   (これは別スレッドで処理する)
     *
     *      一番小さい選択肢（インデックス）から設定された撮影枚数分、
     *      徐々に選択肢をずらして撮影する。
     *
     */
    private void startBracket()
    {
        int startIndex = originalPropertyIndex - (bracketCount / 2);
        if (propertyValueList != null)
        {
            if ((startIndex + bracketCount) > propertyValueList.size())
            {
                startIndex = propertyValueList.size() - bracketCount;
            }
        }
        if (startIndex < 0)
        {
            startIndex = 0;
        }

        PointF afPoint = camera.getActualAutoFocusPoint();
        for (int index = 0; index < bracketCount; index++)
        {
            // 撮影条件を更新する
            updateBracketingStatus(index, startIndex);
            startIndex++;

            try
            {
                // AFポイントを設定する
                if (afPoint != null)
                {
                    camera.setAutoFocusPoint(afPoint);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            if (bracketCount == 1)
            {
                // 1枚しか撮影しない時は、撮影前にウェイトをかける
                waitSeconds(waitSeconds);
            }

            try
            {
                // 写真を撮影する
                camera.takePicture(new HashMap<String, Object>(), this);
                isShootingWait = true;
                while (isShootingWait)
                {
                    // ここで撮影状態が整うまで少し待つ
                    Thread.sleep(BRACKETING_INTERVAL_MILLISECOND);
                    updateShootingWaitStatus();
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            // 指定された時間待機、ただし、最後の撮影が終わったあとには待たないようにする。
            if ((index + 1) < bracketCount)
            {
                waitSeconds(waitSeconds);
            }
        }
        try
        {
            // 変更したプロパティ値を元の値に戻す...ちょっと待ってから
            Thread.sleep(BRACKETING_INTERVAL_MILLISECOND);
            if (originalProperty != null)
            {
                camera.setCameraPropertyValue(targetPropertyName, originalProperty);
            }

            // とにかくAF枠を消す。
            camera.clearAutoFocusPoint();
            autoFocusFrame.hideFocusFrame();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     *   指定された時間待機する
     *
     * @param seconds  待機秒数
     */
    private void waitSeconds(int seconds)
    {
        for (int count = seconds; count > 0; count--)
        {
            // 待ち時間（単位：秒）
            try
            {
                // BKT表示(撮影枚数表示と待ち時間)を変える
                updateMessage("WAIT " + count + "sec.");
                Thread.sleep(1000);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        updateMessage("");
    }

    /**
     *   ブラケッティング撮影の状態を更新する
     *
     * @param index  撮影が終了したカウント（０始まり）
     */
    private void updateBracketingStatus(int index, int currentIndex)
    {
        Log.v(TAG, "updateBracketingStatus() : " + index + "(" + currentIndex + ")");

        // カメラのプロパティ設定を変える
        try
        {
            if (propertyValueList != null)
            {
                Thread.sleep(BRACKETING_INTERVAL_MILLISECOND);
                camera.setCameraPropertyValue(targetPropertyName, propertyValueList.get(currentIndex));
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();

            // 頭に来たので、再度呼ぶ (リトライオーバーするまで)
            if (retryUpdateBracketingStatus < BRACKETING_PROPERTY_VALUE_SET_RETRY)
            {
                retryUpdateBracketingStatus++;
                updateBracketingStatus(index, currentIndex);
            }
        }
        retryUpdateBracketingStatus = 0;

        // 撮影枚数表示を変える
        updateMessage("INT " + (index + 1) + "/" + bracketCount);
    }

    /**
     *   カメラの状態を取得し、撮影可能か確認する。
     *   （trueならまだ撮影処理中、falseなら撮影可能）
     */
    private void updateShootingWaitStatus()
    {
        boolean isBusy = false;
        try
        {
            isBusy = ((camera.isTakingPicture())||(camera.isMediaBusy())||(camera.isRecordingVideo()));

            // ちょっと待ち時間をとりたい...
            String messageToShow = "getShootingBusyStatus() : " + String.valueOf(isBusy);
            Log.v(TAG, messageToShow);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        isShootingWait = isBusy;
    }

    /**
     *   OLYCamera.TakePictureCallback の実装
     *
     *
     */
    @Override
    public void onProgress(OLYCamera olyCamera, OLYCamera.TakingProgress takingProgress, OLYCameraAutoFocusResult olyCameraAutoFocusResult)
    {
        // 特に何もしないでおこう
    }

    /**
     *   OLYCamera.TakePictureCallback の実装
     *
     */
    @Override
    public void onCompleted()
    {
        // 撮影待ち状態の更新
        updateShootingWaitStatus();
    }

    /**
     *   OLYCamera.TakePictureCallback の実装
     *
     * @param e 例外情報
     */
    @Override
    public void onErrorOccurred(Exception e)
    {
         e.printStackTrace();

         // 撮影待ち状態の更新
         updateShootingWaitStatus();
    }
}

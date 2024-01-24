package jp.osdn.gokigen.aira01b.olycamerawrapper.takepicture;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;

import java.util.HashMap;

import jp.co.olympus.camerakit.OLYCamera;
import jp.co.olympus.camerakit.OLYCameraKitException;
import jp.osdn.gokigen.aira01b.IAirA01BInterfacesProvider;
import jp.osdn.gokigen.aira01b.R;
import jp.osdn.gokigen.aira01b.liveview.IMessageDrawer;
import jp.osdn.gokigen.aira01b.liveview.IStatusViewDrawer;

/**
 *   ビデオ撮影の開始・終了制御クラス。
 *
 */
public class MovieRecordingControl implements OLYCamera.CompletedCallback
{
    private final String TAG = toString();
    private final Context context;
    private final OLYCamera camera;
    private final  IAirA01BInterfacesProvider interfaceProvider;
    private boolean isRecordingStart = false;

    /**
     *   コンストラクタ
     *
     */
    public MovieRecordingControl(Context context, OLYCamera camera,  IAirA01BInterfacesProvider provider)
    {
        this.context = context;
        this.camera = camera;
        this.interfaceProvider = provider;
    }

    /**
     *   動画撮影の開始と終了
     *
     */
    public void movieControl()
    {
        try
        {
            Log.v(TAG, "MovieRecordingControl::movieControl()");
            if (camera.isTakingPicture())
            {
                // スチル撮影中の場合は、何もしない（モード異常なので）
                Log.v(TAG, "NOW TAKING PICTURE(STILL) : COMMAND IGNORED");
                return;
            }

            if (!camera.isRecordingVideo())
            {
                // ムービー撮影の開始指示
                camera.startRecordingVideo(new HashMap<String, Object>(), this);
                isRecordingStart = true;
            }
            else
            {
                // ムービー撮影の終了指示
                camera.stopRecordingVideo(this);
                isRecordingStart = false;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     *   処理完了
     *
     */
    @Override
    public void onCompleted()
    {
        try
        {
            Log.v(TAG, "MovieRecordingControl::onCompleted()");
            // 撮影終了をバイブレータで知らせる
            //statusDrawer.vibrate(IShowInformation.VIBRATE_PATTERN_SIMPLE_MIDDLE);
            IStatusViewDrawer statusDrawer = interfaceProvider.getStatusViewDrawer();
            if (statusDrawer != null)
            {
                if (isRecordingStart)
                {
                    statusDrawer.getMessageDrawer().setMessageToShow(IMessageDrawer.MessageArea.UPCENTER, Color.RED, IMessageDrawer.SIZE_LARGE, context.getString(R.string.video_recording));
                }
                else
                {
                    statusDrawer.getMessageDrawer().setMessageToShow(IMessageDrawer.MessageArea.UPCENTER, Color.WHITE, IMessageDrawer.SIZE_STD, "");
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     *   エラー発生
     *
     * @param e 例外情報
     */
    @Override
    public void onErrorOccurred(OLYCameraKitException e)
    {
        // 撮影失敗をバイブレータで知らせる
        //statusDrawer.vibrate(IShowInformation.VIBRATE_PATTERN_SIMPLE_SHORT);
        {
            //// 撮影失敗の表示をToastで行う
            //Toast.makeText(context, R.string.video_failure, Toast.LENGTH_SHORT).show();
            Log.v(TAG, "MovieControl::onErrorOccurred()");

            IStatusViewDrawer statusDrawer = interfaceProvider.getStatusViewDrawer();
            if (statusDrawer != null)
            {
                statusDrawer.getMessageDrawer().setMessageToShow(IMessageDrawer.MessageArea.UPCENTER, Color.WHITE, IMessageDrawer.SIZE_BIG, "");
            }
        }
        e.printStackTrace();
    }
}

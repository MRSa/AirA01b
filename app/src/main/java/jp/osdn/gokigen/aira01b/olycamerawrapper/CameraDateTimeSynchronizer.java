package jp.osdn.gokigen.aira01b.olycamerawrapper;

import android.content.Context;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import jp.co.olympus.camerakit.OLYCamera;
import jp.osdn.gokigen.aira01b.ConfirmationDialog;
import jp.osdn.gokigen.aira01b.R;
import jp.osdn.gokigen.aira01b.preference.ICameraPropertyAccessor;


public class CameraDateTimeSynchronizer implements ConfirmationDialog.Callback, ICameraDateTimeSynchronizer
{
    private final String TAG = toString();
    private final Context context;
    private final OLYCamera camera;
    private String preferenceKey = null;

    /**
     *   コンストラクタ
     *
     */
    public CameraDateTimeSynchronizer(Context context, OLYCamera camera)
    {
        this.context = context;
        this.camera = camera;
    }

    @Override
    public void synchronizeDatetime(boolean isConfirmation)
    {
        preferenceKey = ICameraPropertyAccessor.SYNCHRONIZE_DATETIME;

        if (isConfirmation)
        {
            // 確認ダイアログの生成と表示
            ConfirmationDialog dialog = ConfirmationDialog.newInstance(context);
            dialog.show(R.string.dialog_title_confirmation, R.string.confirm_synchronize_datetime, this);
        }
        else
        {
            // すぐに時刻の同期処理を行ってしまう
            confirm();
        }
    }

    @Override
    public void confirm()
    {
        Log.v(TAG, "CameraDateTimeSynchronizer::confirm()");
        if (preferenceKey.contains(ICameraPropertyAccessor.SYNCHRONIZE_DATETIME))
        {
            // カメラの時刻を端末の時刻に同期させる
            try
            {
                Date date = new Date();
                SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US);
                Log.v(TAG, "SET DATETIME : " + format.format(date));
                camera.changeTime(date);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }
}

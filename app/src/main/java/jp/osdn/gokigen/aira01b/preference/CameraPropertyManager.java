package jp.osdn.gokigen.aira01b.preference;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

/**
 *   CameraPropertyManager : カメラのプロパティを一元管理するクラス...
 *
 */
public class CameraPropertyManager implements ICameraPropertyAccessor
{
    //private final Context context;
    private final SharedPreferences preferences;

    /**
     *   コンストラクタ
     *
     */
    public CameraPropertyManager(Context context)
    {
        //this.context = context;
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    /**
     *   preferenceから LiveViewの画像サイズを取得する
     *
     * @return LiveViewの画像サイズ（テキスト）
     */
    @Override
    public String getLiveViewSize()
    {
        return (preferences.getString(ICameraPropertyAccessor.LIVE_VIEW_QUALITY, ICameraPropertyAccessor.LIVE_VIEW_QUALITY_DEFAULT_VALUE));
    }

    /**
     *   preferenceから全カメラパラメータを展開する
     *
     */
    @Override
    public void restoreCameraSettings(Callback callback)
    {

        if (callback != null)
        {
            callback.restored(true);
        }
    }

    /**
     *   preferenceへ全カメラパラメータを記憶する
     *
     */
    @Override
    public void storeCameraSettings(Callback callback)
    {

        if (callback != null)
        {
            callback.stored(true);
        }
    }
}

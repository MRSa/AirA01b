package jp.osdn.gokigen.aira01b.olycamerawrapper.ble;

import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import jp.osdn.gokigen.aira01b.ConfirmationDialog;
import jp.osdn.gokigen.aira01b.R;
import jp.osdn.gokigen.aira01b.preference.ICameraPropertyAccessor;

/**
 *   Olympus AIR の Bluetooth設定を記録する
 *
 *
 */
public class OlyCameraPowerOnSelector implements Preference.OnPreferenceClickListener, ConfirmationDialog.Callback
{
    private final String TAG = toString();
    private final AppCompatActivity context;
    //private String preferenceKey = null;

    /**
     *   コンストラクタ
     *
     */
    public OlyCameraPowerOnSelector(AppCompatActivity context)
    {
        this.context = context;
    }

    /**
     *   クラスの準備
     *
     */
    public void prepare()
    {
        // 何もしない
    }

    /**
     *
     *
     * @param preference クリックしたpreference
     * @return false : ハンドルしない / true : ハンドルした
     */
    @Override
    public boolean onPreferenceClick(Preference preference)
    {
        Log.v(TAG, "onPreferenceClick() : ");
        if (!preference.hasKey())
        {
            return (false);
        }

        String preferenceKey = preference.getKey();
        if (preferenceKey.contains(ICameraPropertyAccessor.OLYCAMERA_BLUETOOTH_SETTINGS))
        {
            try
            {
                // My Olympus Air登録用ダイアログを表示する
                OlyCameraEntryListDialog dialogFragment = OlyCameraEntryListDialog.newInstance(context.getString(R.string.pref_air_bt), context.getString(R.string.pref_summary_air_bt));
                dialogFragment.setRetainInstance(false);
                dialogFragment.setShowsDialog(true);
                dialogFragment.show(context.getSupportFragmentManager(), "dialog");
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            return (true);
        }
        return (false);
    }

    /**
     *
     *
     */
    @Override
    public void confirm()
    {
        Log.v(TAG, "confirm() ");
    }
}

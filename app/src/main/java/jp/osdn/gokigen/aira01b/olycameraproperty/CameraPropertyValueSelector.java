package jp.osdn.gokigen.aira01b.olycameraproperty;


import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.List;

import androidx.appcompat.app.AlertDialog;
import jp.osdn.gokigen.aira01b.R;
import jp.osdn.gokigen.aira01b.olycamerawrapper.IOlyCameraPropertyProvider;

public class CameraPropertyValueSelector implements ListView.OnItemClickListener, ListView.OnLongClickListener, DialogInterface.OnClickListener
{
    private final String TAG = toString();
    private final Context context;
    private final IOlyCameraPropertyProvider propertyInterface;
    private final ICametaPropertyUpdateNotify updater;

    private List<String> valueList = null;
    private CameraPropertyArrayItem item = null;

    /**
     *  選択されたアイテムの更新を行う
     *
     *
     */
    CameraPropertyValueSelector(Context context, IOlyCameraPropertyProvider propertyInterface, ICametaPropertyUpdateNotify updater)
    {
        this.context = context;
        this.propertyInterface = propertyInterface;
        this.updater = updater;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
    {
        Log.v(TAG, "onItemClick() : " + i);
        CameraPropertyArrayAdapter arrayAdapter = (CameraPropertyArrayAdapter) adapterView.getAdapter();
        item = arrayAdapter.getItem(i);
        //item = (CameraPropertyArrayItem) listView.getSelectedItem();
        if (item == null)
        {
            Log.v(TAG, "selectedItem is NULL.");
            return;
        }

        String propertyName = item.getPropertyName();
        String propertyValue = propertyInterface.getCameraPropertyValue(propertyName);
        Log.v(TAG, "TARGET PROPERTY : " + propertyName + " " + propertyValue);

        valueList = propertyInterface.getCameraPropertyValueList(propertyName);
        if ((valueList == null)||(valueList.size() == 0))
        {
            Log.v(TAG, "Value List is NULL : " + item.getPropertyName() + " " + propertyValue);
            return;
        }
        String[] items;
        if (item.getIconResource() == R.drawable.ic_block_black_18dp_1x)
        {
            // Read only, 値の変更はできない
            items = new String[1];
            items[0] =  propertyInterface.getCameraPropertyValueTitle(propertyValue);
        }
        else
        {
            // 設定可能な選択肢を一覧表示する
            items = new String[valueList.size()];
            for (int index = 0; index < valueList.size(); index++)
            {
                String propValue = valueList.get(index);
                if (propValue.equals(item.getInitialValue()))
                {
                    Log.v(TAG, "INIT: " + propValue);

                    // 初期値には、(*) マークを表示したい
                    items[index] = propertyInterface.getCameraPropertyValueTitle(propValue) + " (*)";
                }
                else
                {
                    items[index] = propertyInterface.getCameraPropertyValueTitle(propValue);
                }
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setSingleChoiceItems(items, valueList.indexOf(item.getPropertyValue()), this);
        builder.setCancelable(true);
        builder.show();
    }

    @Override
    public boolean onLongClick(View view)
    {
        return (false);
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int which)
    {
        dialogInterface.dismiss();
        String propertyValue = valueList.get(which);
        String rawValue = propertyInterface.getCameraPropertyValueTitle(propertyValue);

        // パラメータ設定 (アイテムを個別に設定する場合は有効に...）
        //propertyInterface.setCameraPropertyValue(propertyName, propertyValue);

        if (item.getIconResource() == R.drawable.ic_block_black_18dp_1x)
        {
            // Read Only パラメータは設定しない。
            return;
        }

        item.setPropertyValue(rawValue, propertyValue);
        String initialValue = item.getInitialValue();
        if (!initialValue.equals(propertyValue))
        {
            // 値を変更したマークにする
            item.setIconResource(R.drawable.ic_mode_edit_black_18dp_1x);
        }
        else
        {
            // デフォルト値なので、アイコンを初期アイコンに戻す
            item.setIconResource(R.drawable.ic_web_asset_black_18dp_1x);
        }
        if (updater != null)
        {
            updater.onCameraPropertyUpdate(which);
        }
    }
}

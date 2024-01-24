package jp.osdn.gokigen.aira01b.logcat;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import jp.osdn.gokigen.aira01b.ConfirmationDialog;
import jp.osdn.gokigen.aira01b.R;

class LogCatExporter implements AdapterView.OnItemLongClickListener
{
    private final String TAG = toString();
    private final Activity activity;

    LogCatExporter(@NonNull Activity context)
    {
        this.activity = context;

    }

    @Override
    public boolean onItemLongClick(final AdapterView<?> adapterView, View view, int i, long l)
    {
        Log.v(TAG, "onItemLongClick()" );

        ConfirmationDialog confirm = ConfirmationDialog.newInstance(activity);

        confirm.show(R.string.dialog_confirm_title_output_log, R.string.dialog_confirm_message_output_log, new ConfirmationDialog.Callback() {
            @Override
            public void confirm()
            {
                Log.v(TAG, "confirm()" );
                try {
                    StringBuilder buf = new StringBuilder();
                    ArrayAdapter<String> adapter = (ArrayAdapter<String>) adapterView.getAdapter();
                    for (int index = 0; index < adapter.getCount(); index++)
                    {
                        buf.append(adapter.getItem(index));
                        buf.append("\r\n");
                    }

                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_TITLE, "debug log for " + activity.getString(R.string.app_name));
                    intent.putExtra(Intent.EXTRA_TEXT, new String(buf));
                    activity.startActivity(intent);

                    // Toast.makeText(activity, adapter.getItem(adapter.getCount() - 1), Toast.LENGTH_SHORT).show();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });
        return (true);
    }
}

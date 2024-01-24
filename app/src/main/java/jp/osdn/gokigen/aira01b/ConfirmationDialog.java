package jp.osdn.gokigen.aira01b;

import android.content.Context;
import android.content.DialogInterface;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class ConfirmationDialog extends DialogFragment
{
    private Context context = null;

    public static ConfirmationDialog newInstance(Context context)
    {
        ConfirmationDialog instance = new ConfirmationDialog();
        instance.prepare(context);
        return (instance);
    }

    private void prepare(Context context)
    {
        this.context = context;
    }

    public void show(int titleResId, int messageResId, final Callback callback)
    {
        String title = "";
        String message = "";

        // タイトルとメッセージをのダイアログを表示する
        if (context != null)
        {
            title = context.getString(titleResId);
            message = context.getString(messageResId);
        }
        show(title, message, callback);
    }

    public void show(String title, String message, final Callback callback)
    {
        // 確認ダイアログの生成
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
        alertDialog.setTitle(title);
        alertDialog.setIcon(android.R.drawable.ic_dialog_alert);
        alertDialog.setMessage(message);
        alertDialog.setCancelable(true);

        // ボタンを設定する（実行ボタン）
        alertDialog.setPositiveButton(context.getString(R.string.dialog_positive_execute),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        callback.confirm();
                        dialog.dismiss();
                    }
                });

        // ボタンを設定する (キャンセルボタン）
        alertDialog.setNegativeButton(context.getString(R.string.dialog_negative_cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.cancel();
                    }
                });

        // 確認ダイアログを表示する
        alertDialog.show();
    }

    public void show(int iconResId, String title, String message)
    {
        // 表示イアログの生成
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
        alertDialog.setTitle(title);
        alertDialog.setIcon(iconResId);
        alertDialog.setMessage(message);
        alertDialog.setCancelable(true);

        // ボタンを設定する（実行ボタン）
        alertDialog.setPositiveButton(context.getString(R.string.dialog_positive_execute),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                    }
                });

        // 確認ダイアログを表示する
        alertDialog.show();
    }

    // コールバックインタフェース
    public interface Callback
    {
        void confirm();
    }
}

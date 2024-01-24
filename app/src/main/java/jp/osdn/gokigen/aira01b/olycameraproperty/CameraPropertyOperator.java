package jp.osdn.gokigen.aira01b.olycameraproperty;

import android.content.Context;
import android.util.Log;
import android.view.View;

import jp.osdn.gokigen.aira01b.ConfirmationDialog;
import jp.osdn.gokigen.aira01b.R;

public class CameraPropertyOperator implements View.OnClickListener
{
    private final String TAG = toString();

    private final Context context;
    private final CameraPropertyLoader loader;


    CameraPropertyOperator(Context context, CameraPropertyLoader loader)
    {
        this.context = context;
        this.loader = loader;
    }

    @Override
    public void onClick(View view)
    {
        int id = view.getId();
        Log.v(TAG, "onClick() : " + id);
        switch (id)
        {
            case R.id.propertySettings_restore:
                processRestoreCameraProperty();
                break;

            default:
                break;
        }
    }

    private void processRestoreCameraProperty()
    {
        try
        {
            final ConfirmationDialog dialog = ConfirmationDialog.newInstance(context);
            dialog.show(R.string.dialog_title_confirmation,
                    R.string.dialog_message_restore_camera_property,
                    new ConfirmationDialog.Callback() {
                        @Override
                        public void confirm()
                        {
                            loader.resetProperty();
                        }
                    });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}

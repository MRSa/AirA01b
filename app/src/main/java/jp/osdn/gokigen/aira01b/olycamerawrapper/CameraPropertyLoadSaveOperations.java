package jp.osdn.gokigen.aira01b.olycamerawrapper;

import android.app.Activity;
import android.app.ProgressDialog;
import android.util.Log;
import android.widget.Toast;

import jp.osdn.gokigen.aira01b.R;
import jp.osdn.gokigen.aira01b.liveview.ICameraStatusDisplay;
import jp.osdn.gokigen.aira01b.myolycameraprops.ICameraPropertyLoadSaveOperations;

public class CameraPropertyLoadSaveOperations implements ICameraPropertyLoadSaveOperations
{
    private final String TAG = toString();
    private final ILoadSaveCameraProperties loadSaveProperties;
    private final ICameraStatusDisplay cameraStatusDisplay;
    private final Activity activity;

    public CameraPropertyLoadSaveOperations(final Activity activity, ILoadSaveCameraProperties loadSaveProperties, ICameraStatusDisplay cameraStatusDisplay)
    {
        this.loadSaveProperties = loadSaveProperties;
        this.cameraStatusDisplay = cameraStatusDisplay;
        this.activity = activity;
    }

    @Override
    public void loadProperties(final String id, final String name)
    {
        //Log.v(TAG, "PROPERTY RESTORE ENTER : (" + id + ") " + name);

        //
        // BUSYダイアログを表示する
        //
        final ProgressDialog busyDialog = new ProgressDialog(activity);
        busyDialog.setMessage(activity.getString(R.string.dialog_start_load_property_message));
        busyDialog.setTitle(activity.getString(R.string.dialog_start_load_property_title));
        busyDialog.setIndeterminate(false);
        busyDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        busyDialog.show();

        try
        {
            Thread thread = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    final boolean toast = restoreCameraSettings(id, name);
                    busyDialog.dismiss();

                    activity.runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            cameraStatusDisplay.updateCameraStatus();

                            // Toast で展開したよのメッセージを表示
                            if (toast)
                            {
                                String restoredMessage = activity.getString(R.string.restored_my_props) + name;
                                Toast.makeText(activity, restoredMessage, Toast.LENGTH_SHORT).show();
                            }

                        }
                    });

                }
            });
            thread.start();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        //Log.v(TAG, "PROPERTY RESTORE EXIT : (" + id + ") " + name);
    }

    @Override
    public void saveProperties(final String id, final String name)
    {
        //
        // BUSYダイアログを表示する
        //
        final ProgressDialog busyDialog = new ProgressDialog(activity);
        busyDialog.setMessage(activity.getString(R.string.dialog_start_save_property_message));
        busyDialog.setTitle(activity.getString(R.string.dialog_start_save_property_title));
        busyDialog.setIndeterminate(false);
        busyDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        busyDialog.show();

        try
        {
            Thread thread = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    final boolean toast = storeCameraSettings(id, name);
                    busyDialog.dismiss();

                    activity.runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            cameraStatusDisplay.updateCameraStatus();

                            // Toast で保存したよのメッセージを表示
                            if (toast)
                            {
                                String storedMessage = activity.getString(R.string.saved_my_props) + name;
                                Toast.makeText(activity, storedMessage, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                }
            });
            thread.start();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        Log.v(TAG, "PROPERTY STORED : " + id + " " + name);
    }

    private boolean storeCameraSettings(String itemId, String restoredDataName)
    {
        boolean toast = false;
        //Log.v(TAG, "storeCameraSettings() : START");
        try
        {
            if (loadSaveProperties != null)
            {
                if (itemId.contentEquals("000"))
                {
                    Log.v(TAG, "AUTO SAVE DATA AREA...(NOT STORE PROPERTIES)");
                }
                else
                {
                    // データを保管する
                    loadSaveProperties.saveCameraSettings(itemId, restoredDataName);
                    Log.v(TAG, "STORED : (" + itemId + ") " + restoredDataName);
                    toast = true;
                }
            }
            else
            {
                Log.v(TAG, "STORE INTERFACE IS NULL...");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.v(TAG, "STORE FAILED...");
        }
        //Log.v(TAG, "storeCameraSettings() : END");
        return (toast);
    }

    private boolean restoreCameraSettings(String itemId, String restoredDataName)
    {
        boolean toast = false;
        //Log.v(TAG, "restoreCameraSettings() : START");
        try
        {
            if (loadSaveProperties != null)
            {
                if (itemId.contentEquals("000"))
                {
                    loadSaveProperties.loadCameraSettings("");
                    Log.v(TAG, "RESTORED AUTO SAVE DATA...");
                }
                else
                {
                    loadSaveProperties.loadCameraSettings(itemId);
                    Log.v(TAG, "RESTORED : (" + itemId + ") " + restoredDataName);
                }
                toast = true;
            }
            else
            {
                Log.v(TAG, "RESTORE INTERFACE IS NULL...");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.v(TAG, "RESTORE FAILED...");
        }
        //Log.v(TAG, "restoreCameraSettings() : END");
        return (toast);
    }
}

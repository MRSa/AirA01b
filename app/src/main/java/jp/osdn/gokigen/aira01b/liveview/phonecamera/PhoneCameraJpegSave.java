package jp.osdn.gokigen.aira01b.liveview.phonecamera;


import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.hardware.Camera;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import jp.osdn.gokigen.aira01b.R;

public class PhoneCameraJpegSave implements Camera.PictureCallback
{
    private final String TAG = toString();
    private final Context context;
    private final IPhoneCameraShutter finishedCallback;
    private Location currentLocation = null;

    PhoneCameraJpegSave(Context context, IPhoneCameraShutter callback)
    {
        this.context = context;
        this.finishedCallback = callback;
    }

    /**
     *   現在の位置情報を拾う
     */
    void setCurrentLocation(Location location)
    {
       currentLocation = location;
    }

    @Override
    public void onPictureTaken(byte[] bytes, Camera camera)
    {
        Log.v(TAG, "PhoneCameraJpegSave::onPictureTaken()");
        if (bytes == null)
        {
            Log.v(TAG, "PhoneCameraJpegSave::onPictureTaken() : Picture data is NULL.");
            finishedCallback.onSavedPicture(false);
            return;
        }

        try
        {
            Calendar calendar = Calendar.getInstance();
            String filename = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(calendar.getTime()) + ".jpg";
            final String directoryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath() + "/" + context.getString(R.string.app_name2) + "/";

            final File outputDir = new File(directoryPath);
            if (!outputDir.exists())
            {
                if (!outputDir.mkdirs())
                {
                    Log.v(TAG, "MKDIR FAIL. : " + directoryPath);
                }
            }

            ContentResolver resolver = context.getContentResolver();

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, filename);
            values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

            Uri extStorageUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            {
                String path = Environment.DIRECTORY_DCIM + File.separator + context.getString(R.string.app_name2);
                values.put(MediaStore.Images.Media.RELATIVE_PATH, path);
                values.put(MediaStore.Images.Media.IS_PENDING, true);
                extStorageUri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
            }
            else
            {
                values.put(MediaStore.Images.Media.DATA, outputDir.getAbsolutePath() + File.separator + filename);
                extStorageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            }

            Uri imageUri = resolver.insert(extStorageUri, values);
            if (imageUri != null)
            {
                OutputStream outputStream = resolver.openOutputStream(imageUri, "wa");
                if (outputStream != null)
                {
                    outputStream.write(bytes);
                    outputStream.flush();
                    outputStream.close();
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                {
                    values.put(MediaStore.Images.Media.IS_PENDING, false);
                    resolver.update(imageUri, values, null, null);
                }
            }
            else
            {
                Log.v(TAG, " cannot get imageUri...");
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            {
                if (currentLocation != null)
                {
                    // 位置情報を入れる
                    values.put(MediaStore.Images.Media.LATITUDE, currentLocation.getLatitude());
                    values.put(MediaStore.Images.Media.LONGITUDE, currentLocation.getLongitude());
                }
                final Uri pictureUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (pictureUri != null)
                {
                    Log.v(TAG, " SAVED : " + pictureUri.getPath());
                }
            }
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
        finishedCallback.onSavedPicture(true);
        currentLocation = null;
    }
}

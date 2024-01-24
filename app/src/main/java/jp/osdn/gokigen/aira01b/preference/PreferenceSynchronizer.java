package jp.osdn.gokigen.aira01b.preference;


import android.content.SharedPreferences;
import android.util.Log;

import jp.osdn.gokigen.aira01b.olycamerawrapper.CameraPropertyUtilities;
import jp.osdn.gokigen.aira01b.olycamerawrapper.IOlyCameraProperty;
import jp.osdn.gokigen.aira01b.olycamerawrapper.IOlyCameraPropertyProvider;

class PreferenceSynchronizer implements Runnable
{
    private final String TAG = toString();
    private final IOlyCameraPropertyProvider propertyInterface;
    private final SharedPreferences preference;
    private final IPropertySynchronizeCallback callback;

    PreferenceSynchronizer(IOlyCameraPropertyProvider propertyInterface, SharedPreferences preference, IPropertySynchronizeCallback callback)
    {
        this.propertyInterface = propertyInterface;
        this.preference = preference;
        this.callback = callback;
    }

    private String getPropertyValue(String key)
    {
        String propertyValue;
        try
        {
            String value = propertyInterface.getCameraPropertyValue(key);
            propertyValue = CameraPropertyUtilities.getPropertyValue(value);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            propertyValue = "";
        }
        Log.v(TAG, "getPropertyValue(" + key + ") : " + propertyValue);
        return (propertyValue);
    }

    @Override
    public void run()
    {
        Log.v(TAG, "run()");
        try
        {
            SharedPreferences.Editor editor = preference.edit();
            editor.putString(ICameraPropertyAccessor.TAKE_MODE, getPropertyValue(IOlyCameraProperty.TAKE_MODE));
            editor.putString(ICameraPropertyAccessor.COLOR_TONE, getPropertyValue(IOlyCameraProperty.COLOR_TONE));
            editor.putString(ICameraPropertyAccessor.AE_MODE, getPropertyValue(IOlyCameraProperty.AE_MODE));
            editor.putString(ICameraPropertyAccessor.WB_MODE, getPropertyValue(IOlyCameraProperty.WB_MODE));
            editor.putString(ICameraPropertyAccessor.EXPOSURE_COMPENSATION, getPropertyValue(IOlyCameraProperty.EXPOSURE_COMPENSATION));
            editor.putString(ICameraPropertyAccessor.SHUTTER_SPEED, getPropertyValue(IOlyCameraProperty.SHUTTER_SPEED));
            editor.putString(ICameraPropertyAccessor.APERTURE, getPropertyValue(IOlyCameraProperty.APERTURE));
            editor.putString(ICameraPropertyAccessor.ISO_SENSITIVITY, getPropertyValue(IOlyCameraProperty.ISO_SENSITIVITY));
            editor.putString(ICameraPropertyAccessor.SOUND_VOLUME_LEVEL, getPropertyValue(IOlyCameraProperty.SOUND_VOLUME_LEVEL));
            boolean value = getPropertyValue(IOlyCameraProperty.RAW).equals("ON");
            editor.putBoolean(ICameraPropertyAccessor.RAW, value);
            editor.apply();

            if (callback != null)
            {
                callback.synchronizedProperty();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    interface IPropertySynchronizeCallback
    {
        void synchronizedProperty();
    }
}

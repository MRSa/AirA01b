package jp.osdn.gokigen.aira01b.olycamerawrapper;

import android.util.Log;

import java.util.Map;

import jp.co.olympus.camerakit.OLYCamera;

/**
 *
 */
public class OlyCameraHardwareStatus implements ICameraHardwareStatus
{
    private final String TAG = toString();
    private final OLYCamera camera;

    /**
     *
     */
    OlyCameraHardwareStatus(OLYCamera camera)
    {
        this.camera = camera;
    }

    @Override
    public String getLensMountStatus()
    {
        String message;
        try
        {
            message = camera.getLensMountStatus();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            message = "[UNKNOWN]";
        }
        return (message);
    }

    @Override
    public String getMediaMountStatus()
    {
        String message;
        try
        {
            message = camera.getMediaMountStatus();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            message = "[UNKNOWN]";
        }
        return (message);
    }

    @Override
    public float getMinimumFocalLength()
    {
        float value;
        try
        {
            value = camera.getMinimumFocalLength();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            value = -1.0f;
        }
        return (value);
    }

    @Override
    public float getMaximumFocalLength()
    {
        float value;
        try
        {
            value = camera.getMaximumFocalLength();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            value = -1.0f;
        }
        return (value);
    }

    @Override
    public float getActualFocalLength()
    {
        float value;
        try
        {
            value = camera.getActualFocalLength();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            value = -1.0f;
        }
        return (value);
    }

    @Override
    public Map<String, Object> inquireHardwareInformation()
    {
        try
        {
            return (camera.inquireHardwareInformation());
        }
        catch (Exception e)
        {
            Log.v(TAG, "EXCEPTION : " + e.toString());
            e.printStackTrace();
        }
        return (null);
    }
}

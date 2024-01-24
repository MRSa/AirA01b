package jp.osdn.gokigen.aira01b.olycamerawrapper;

import android.util.Log;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jp.co.olympus.camerakit.OLYCamera;

/**
 *   カメラプロパティをやり取りするクラス (Wrapperクラス)
 */
class OlyCameraPropertyProxy implements IOlyCameraPropertyProvider
{
    private final String TAG = toString();
    private final OLYCamera camera;
    private final OlyCameraHardwareStatus hardwareStatusInterface;

    /**
     *   コンストラクタ
     *
     * @param camera OLYCameraクラス
     */
    OlyCameraPropertyProxy(OLYCamera camera)
    {
        this.camera = camera;
        this.hardwareStatusInterface = new OlyCameraHardwareStatus(camera);
    }

    /**
     *  フォーカス状態を知る（MF or AF）
     * @return true : MF / false : AF
     */
    boolean isManualFocus()
    {
        boolean isManualFocus = false;
        try
        {
            String value = camera.getCameraPropertyValue(IOlyCameraProperty.FOCUS_STILL);
            Log.v(TAG, "OlyCameraPropertyProxy::isManualFocus() " + value);
            isManualFocus = !(value.contains("AF"));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return (isManualFocus);
    }

    /**
     *  AE ロック状態を知る
     *
     * @return true : AE Lock / false : AE Unlock
     */
    boolean isExposureLocked()
    {
        boolean isExposureLocked =false;
        try
        {
            String value = camera.getCameraPropertyValue(IOlyCameraProperty.AE_LOCK_STATE);
            Log.v(TAG, "OlyCameraPropertyProxy::isExposureLocked() " + value);
            isExposureLocked = !(value.contains("UNLOCK"));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return (isExposureLocked);
    }

    @Override
    public Set<String> getCameraPropertyNames()
    {
        try
        {
            return (camera.getCameraPropertyNames());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return (null);
    }

    @Override
    public String getCameraPropertyValue(String name)
    {
        try
        {
            return (camera.getCameraPropertyValue(name));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return (null);
    }

    @Override
    public Map<String, String> getCameraPropertyValues(Set<String> names)
    {
        try
        {
            return (camera.getCameraPropertyValues(names));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return (null);
    }

    @Override
    public String getCameraPropertyTitle(String name)
    {
        try
        {
            return (camera.getCameraPropertyTitle(name));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return (null);
    }

    @Override
    public List<String> getCameraPropertyValueList(String name)
    {
        try
        {
            return (camera.getCameraPropertyValueList(name));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return (null);
    }

    @Override
    public String getCameraPropertyValueTitle(String propertyValue)
    {
        try
        {
            return (camera.getCameraPropertyValueTitle(propertyValue));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return (null);
    }

    @Override
    public void setCameraPropertyValue(String name, String value)
    {
        try
        {
            camera.setCameraPropertyValue(name, value);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void setCameraPropertyValues(Map<String, String> values)
    {
        try
        {
            camera.setCameraPropertyValues(values);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public boolean canSetCameraProperty(String name)
    {
        try
        {
            return (camera.canSetCameraProperty(name));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return (false);
    }

    @Override
    public boolean isConnected()
    {
        try
        {
            return (camera.isConnected());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return (false);
    }

    /**
     * 電動ズーム機能を持つレンズが装着されているか確認
     *
     * @return true ; 電動ズーム付き / false : 電動ズームなし
     */
    @Override
    public boolean isElectricZoomLens()
    {
        return ((camera != null)&&(camera.getLensMountStatus()).contains("electriczoom"));
    }

    /**
     *   ズームレンズホルダを応答する
     *
     */
    @Override
    public IZoomLensHolder getZoomLensHolder()
    {
        return (new ZoomLensHolder(camera));
    }

    @Override
    public ICameraHardwareStatus getHardwareStatus()
    {
        return (hardwareStatusInterface);
    }

}

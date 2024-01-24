package jp.osdn.gokigen.aira01b.olycamerawrapper;

import jp.co.olympus.camerakit.OLYCamera;

/**
 *   カメラプロパティに関する雑多な処理...
 *
 */
public class CameraPropertyUtilities
{
    /**
     *   toLiveViewSize() : スクリーンサイズの文字列から、OLYCamera.LiveViewSize型へ変換する
     *
     * @param quality スクリーンサイズ文字列
     * @return OLYCamera.LiveViewSize型
     */
    static OLYCamera.LiveViewSize toLiveViewSizeType(String quality)
    {
        if (quality == null)
        {
            return OLYCamera.LiveViewSize.QVGA;
        }
        if (quality.equalsIgnoreCase("QVGA"))
        {
            return OLYCamera.LiveViewSize.QVGA;
        }
        else if (quality.equalsIgnoreCase("VGA"))
        {
            return OLYCamera.LiveViewSize.VGA;
        } else if (quality.equalsIgnoreCase("SVGA"))
        {
            return OLYCamera.LiveViewSize.SVGA;
        } else if (quality.equalsIgnoreCase("XGA"))
        {
            return OLYCamera.LiveViewSize.XGA;
        } else if (quality.equalsIgnoreCase("QUAD_VGA"))
        {
            return OLYCamera.LiveViewSize.QUAD_VGA;
        }
        return OLYCamera.LiveViewSize.QVGA;
    }

    public static String getPropertyValue(String value)
    {
        if (value == null)
        {
            return ("");
        }
        String[] keyValue = OLYCamera.decodeCameraPropertyValue(value);
        return (keyValue[1]);
    }
 }

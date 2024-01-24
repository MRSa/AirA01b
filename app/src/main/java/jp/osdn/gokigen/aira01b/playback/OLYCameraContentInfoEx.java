package jp.osdn.gokigen.aira01b.playback;

import jp.co.olympus.camerakit.OLYCameraFileInfo;

public class OLYCameraContentInfoEx
{
    private final OLYCameraFileInfo fileInfo;
    private boolean hasRaw;
    private boolean checked = false;

    OLYCameraContentInfoEx(OLYCameraFileInfo fileInfo, boolean hasRaw)
    {
        this.fileInfo = fileInfo;
        this.hasRaw = hasRaw;
    }

    void setChecked(boolean isChecked)
    {
        checked = isChecked;
    }

    void toggleChecked()
    {
        checked = !checked;
    }

    public boolean isChecked()
    {
        return (checked);
    }

    void setHasRaw()
    {
        hasRaw = true;
    }

    public OLYCameraFileInfo getFileInfo()
    {
        return (fileInfo);
    }

    public boolean hasRaw()
    {
        return (hasRaw);
    }
}

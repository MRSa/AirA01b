package jp.osdn.gokigen.aira01b.olycamerawrapper;

import java.util.Map;

/**
 *
 *
 */
public interface ICameraHardwareStatus
{
    String getLensMountStatus();
    String getMediaMountStatus();

    float getMinimumFocalLength();
    float getMaximumFocalLength();
    float getActualFocalLength();

    Map<String, Object> inquireHardwareInformation();
}

package jp.osdn.gokigen.aira01b.olycamerawrapper;

public interface ILoadSaveCameraProperties
{
    void loadCameraSettings(final String id);
    void saveCameraSettings(final String id, final String name);
}

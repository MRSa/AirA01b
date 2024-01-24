package jp.osdn.gokigen.aira01b.olycamerawrapper;

/**
 *
 *
 */
public interface ICameraStatusReceiver
{
    void onStatusNotify(String message);
    void onCameraConnected();
    void onCameraDisconnected();
    void onCameraOccursException(String message, Exception e);
}

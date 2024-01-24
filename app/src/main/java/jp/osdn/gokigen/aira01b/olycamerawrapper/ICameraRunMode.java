package jp.osdn.gokigen.aira01b.olycamerawrapper;

public interface ICameraRunMode
{
    /** カメラの動作モード変更 **/
    void changeRunMode(boolean isRecording);
    boolean isRecordingMode();
}

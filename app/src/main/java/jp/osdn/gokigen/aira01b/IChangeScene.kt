package jp.osdn.gokigen.aira01b;

/**
 *
 */
public interface IChangeScene
{
    void changeSceneToCameraPropertyList();
    void changeSceneToConfiguration();
    void changeSceneToPlaybackCamera();
    void changeSceneToPlaybackPhone();
    void changeSceneToManipulateImage();
    void changeSceneToDebugInformation();
    void exitApplication();
}

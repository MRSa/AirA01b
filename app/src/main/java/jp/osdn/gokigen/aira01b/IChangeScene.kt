package jp.osdn.gokigen.aira01b

/**
 * 
 */
interface IChangeScene {
    fun changeSceneToCameraPropertyList()
    fun changeSceneToConfiguration()
    fun changeSceneToPlaybackCamera()
    fun changeSceneToPlaybackPhone()
    fun changeSceneToManipulateImage()
    fun changeSceneToDebugInformation()
    fun exitApplication()
}

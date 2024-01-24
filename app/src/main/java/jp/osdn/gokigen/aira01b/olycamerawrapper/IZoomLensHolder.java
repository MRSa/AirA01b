package jp.osdn.gokigen.aira01b.olycamerawrapper;

/**
 *  ズームレンズの状態
 *
 */

public interface IZoomLensHolder
{
    boolean canZoom();
    void updateStatus();
    float getMaximumFocalLength();
    float getMinimumFocalLength();
    float getCurrentFocalLength();
    void driveZoomLens(float targetLength);
    boolean isDrivingZoomLens();

}

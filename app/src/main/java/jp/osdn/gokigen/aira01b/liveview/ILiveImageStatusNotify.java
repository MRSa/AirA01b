package jp.osdn.gokigen.aira01b.liveview;

import android.location.Location;

/**
 *
 *
 */
interface ILiveImageStatusNotify
{
    void toggleFocusAssist();
    void toggleShowGridFrame();

    void captureLiveImage(Location location, boolean isShare);

    IMessageDrawer getMessageDrawer();
}

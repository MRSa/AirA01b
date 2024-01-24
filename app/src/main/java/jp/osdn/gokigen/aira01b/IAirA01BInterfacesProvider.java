package jp.osdn.gokigen.aira01b;

import jp.osdn.gokigen.aira01b.liveview.IStatusViewDrawer;
import jp.osdn.gokigen.aira01b.liveview.IAutoFocusFrameDisplay;
import jp.osdn.gokigen.aira01b.liveview.ICameraStatusDisplay;
import jp.osdn.gokigen.aira01b.olycamerawrapper.IOlyCameraPropertyProvider;
import jp.osdn.gokigen.aira01b.preference.ICameraPropertyAccessor;
import jp.osdn.gokigen.aira01b.olycamerawrapper.ICameraStatusReceiver;

public interface IAirA01BInterfacesProvider
{
    ICameraStatusDisplay getCameraStatusInterface();
    IAutoFocusFrameDisplay getAutoFocusFrameInterface();
    IStatusViewDrawer getStatusViewDrawer();
    IChangeScene getChangeSceneCoordinator();
    ICameraPropertyAccessor getPropertyAccessor();
    IOlyCameraPropertyProvider getPropertyProvider();
    ICameraStatusReceiver getStatusReceiver();
}

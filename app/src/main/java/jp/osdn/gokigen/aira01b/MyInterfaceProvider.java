package jp.osdn.gokigen.aira01b;

import android.content.Context;

import jp.osdn.gokigen.aira01b.liveview.IStatusViewDrawer;
import jp.osdn.gokigen.aira01b.liveview.IAutoFocusFrameDisplay;
import jp.osdn.gokigen.aira01b.liveview.ICameraStatusDisplay;
import jp.osdn.gokigen.aira01b.olycamerawrapper.IOlyCameraPropertyProvider;
import jp.osdn.gokigen.aira01b.preference.CameraPropertyManager;
import jp.osdn.gokigen.aira01b.preference.ICameraPropertyAccessor;
import jp.osdn.gokigen.aira01b.olycamerawrapper.ICameraStatusReceiver;

/**
 *   内部インタフェースを提供するクラス
 *
 */
public class MyInterfaceProvider implements IAirA01BInterfacesProvider
{
    private final CameraPropertyManager propertyManager;
    private final ICameraStatusReceiver statusReceiver;

    private IStatusViewDrawer statusViewDrawer = null;
    private ICameraStatusDisplay statusDisplay = null;
    private IAutoFocusFrameDisplay autoFocusFrameDisplay = null;
    private IOlyCameraPropertyProvider propertyProvider = null;
    private IChangeScene changeSceneCoordinator = null;


    /**
     *   コンストラクタ
     */
    MyInterfaceProvider(Context context, ICameraStatusReceiver statusReceiver, IChangeScene changeScene)
    {
        this.propertyManager = new CameraPropertyManager(context);
        this.statusReceiver = statusReceiver;
        this.changeSceneCoordinator = changeScene;
    }

    public void setStatusViewDrawer(IStatusViewDrawer statusViewDrawer)
    {
        this.statusViewDrawer = statusViewDrawer;
    }

    public void setStatusInterface(ICameraStatusDisplay statusDisplay)
    {
        this.statusDisplay = statusDisplay;
    }

    public void setAutoFocusFrameDisplay(IAutoFocusFrameDisplay afFrameDisplay)
    {
        this.autoFocusFrameDisplay = afFrameDisplay;
    }

    void setPropertyProvider(IOlyCameraPropertyProvider propertyProvider)
    {
        this.propertyProvider = propertyProvider;
    }

    @Override
    public ICameraStatusDisplay getCameraStatusInterface()
    {
        return (statusDisplay);
    }

    @Override
    public IStatusViewDrawer getStatusViewDrawer()
    {
        return (statusViewDrawer);
    }

    @Override
    public IChangeScene getChangeSceneCoordinator()
    {
        return (changeSceneCoordinator);
    }

    @Override
    public IAutoFocusFrameDisplay getAutoFocusFrameInterface()
    {
        return (autoFocusFrameDisplay);
    }

    @Override
    public ICameraPropertyAccessor getPropertyAccessor()
    {
        return (propertyManager);
    }

    @Override
    public IOlyCameraPropertyProvider getPropertyProvider()
    {
        return (propertyProvider);
    }

    @Override
    public ICameraStatusReceiver getStatusReceiver()
    {
        return (statusReceiver);
    }
}

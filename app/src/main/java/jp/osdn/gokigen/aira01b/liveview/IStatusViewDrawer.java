package  jp.osdn.gokigen.aira01b.liveview;

import androidx.fragment.app.FragmentActivity;

public interface IStatusViewDrawer
{
    void updateStatusView(String message);
    void updateFocusAssistStatus();
    void updateGridFrameStatus();

    void showFavoriteSettingDialog(FragmentActivity activity);

    void toggleTimerStatus();

    void toggleGpsTracking();
    void updateGpsTrackingStatus();

    void updateLiveViewMagnifyScale(final boolean isMaxLimit, final float scale);

    IMessageDrawer getMessageDrawer();
}

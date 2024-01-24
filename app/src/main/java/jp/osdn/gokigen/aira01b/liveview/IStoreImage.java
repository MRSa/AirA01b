package jp.osdn.gokigen.aira01b.liveview;

import android.app.Activity;
import android.graphics.Bitmap;
import android.location.Location;

public interface IStoreImage
{
    void doStore(final Bitmap target, final Location location, final boolean isShare);
    void setActivity(Activity activity);
}

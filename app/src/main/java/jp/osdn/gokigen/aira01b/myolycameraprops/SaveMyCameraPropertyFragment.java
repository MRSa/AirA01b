package jp.osdn.gokigen.aira01b.myolycameraprops;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.fragment.app.ListFragment;
import jp.osdn.gokigen.aira01b.R;
import jp.osdn.gokigen.aira01b.olycamerawrapper.LoadSaveCameraProperties;

public class SaveMyCameraPropertyFragment extends ListFragment
{
    private ILoadSaveMyCameraPropertyDialogDismiss dialogDismiss = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return (inflater.inflate(R.layout.list_camera_properties, container, false));
    }

    void setDismissInterface(ILoadSaveMyCameraPropertyDialogDismiss dismiss)
    {
        this.dialogDismiss = dismiss;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        List<MyCameraPropertySetItems> listItems = new ArrayList<>();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        for (int index = 1; index <= LoadSaveCameraProperties.MAX_STORE_PROPERTIES; index++)
        {
            String idHeader = String.format(Locale.ENGLISH, "%03d", index);
            String prefDate = preferences.getString(idHeader + LoadSaveCameraProperties.DATE_KEY, "");
            if (prefDate.length() <= 0)
            {
                listItems.add(new MyCameraPropertySetItems(0, idHeader, "", ""));
                break;
            }
            String prefTitle = preferences.getString(idHeader + LoadSaveCameraProperties.TITLE_KEY, "");
            listItems.add(new MyCameraPropertySetItems(0, idHeader, prefTitle, prefDate));
        }
        MyCameraPropertySetArrayAdapter adapter = new MyCameraPropertySetArrayAdapter(getActivity(),  R.layout.column_save, listItems, dialogDismiss);
        setListAdapter(adapter);
    }
}

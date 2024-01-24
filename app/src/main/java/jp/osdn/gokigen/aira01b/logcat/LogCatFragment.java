package jp.osdn.gokigen.aira01b.logcat;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.ListFragment;
import jp.osdn.gokigen.aira01b.R;

/**
 *
 */
public class LogCatFragment extends ListFragment
{
    private final String TAG = toString();
    private ArrayAdapter<String> adapter;
    private List<String> dataItems = new ArrayList<>();
    private LogCatUpdater updater = new LogCatUpdater();
    public static LogCatFragment newInstance()
    {
        LogCatFragment instance = new LogCatFragment();

        // パラメータはBundleにまとめておく
        Bundle arguments = new Bundle();
        //arguments.putString("title", title);
        //arguments.putString("message", message);
        instance.setArguments(arguments);

        //instance.prepare();
        return (instance);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.debug_view, menu);
/*
        String title = getString(R.string.app_name) + " " + getString(R.string.pref_degug_info);
        try {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            ActionBar bar = activity.getSupportActionBar();
            if (bar != null)
            {
                bar.setTitle(title);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
*/
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == R.id.action_refresh)
        {
            update();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     *   表示データの更新
     *
     */
    private void update()
    {
        dataItems.clear();
        Thread thread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                Log.v(TAG, "START LOGCAT");
                dataItems = updater.getLogCat("main", "time", "*:v", "gokigen", "");
                Log.v(TAG, "FINISH LOGCAT");
                try
                {
                    final FragmentActivity activity = getActivity();
                    if (activity != null)
                    {
                        activity.runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                try
                                {
                                    // 中身があったらクリアする
                                    if (adapter.getCount() > 0)
                                    {
                                        adapter.clear();
                                    }

                                    // リストの内容を更新する
                                    adapter.addAll(dataItems);

                                    // 最下部にカーソルを移したい
                                    ListView view = activity.findViewById(android.R.id.list);
                                    view.setSelection(dataItems.size());

                                    // 更新終了通知
                                    Snackbar.make(getActivity().findViewById(R.id.fragment1), getString(R.string.finish_refresh), Snackbar.LENGTH_SHORT).show();
                                    //Toast.makeText(getActivity(), getString(R.string.finish_refresh), Toast.LENGTH_SHORT).show();
                                }
                                catch (Exception ee)
                                {
                                    ee.printStackTrace();
                                }
                            }
                        });
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });
        try
        {
            // 本当は、ここでダイアログを出したい
            thread.start();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        Log.v(TAG, "onResume()");

        update();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        Log.v(TAG, "onPause()");
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "LogCatFragment::onCreate()");
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        Log.v(TAG, "LogCatFragment::onActivityCreated()");
        setHasOptionsMenu(true);

        Activity activity = getActivity();
        if (activity != null)
        {
            ListView view = getListView();
            if (view != null)
            {
                getListView().setOnItemLongClickListener(new LogCatExporter(activity));
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        adapter = new ArrayAdapter<>(inflater.getContext(), android.R.layout.simple_list_item_1, dataItems);
        setListAdapter(adapter);

        return (super.onCreateView(inflater, container, savedInstanceState));
    }
}

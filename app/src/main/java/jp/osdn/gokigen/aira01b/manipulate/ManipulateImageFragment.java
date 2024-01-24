package jp.osdn.gokigen.aira01b.manipulate;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import jp.osdn.gokigen.aira01b.R;

/**
 *   画像加工用フラグメント
 *
 *
 */
public class ManipulateImageFragment extends Fragment implements IManipulateImageHolder, IManipulateImageOperation.IManipulateImageCallback
{
    private final String TAG = toString();

    private ImageManipulator imageManipulator = null;
    private EffectImageProcessor imageProcessor = null;

    private String manipulateImage1 = null;
    private String manipulateImage2 = null;

    private boolean enableSaveMenu = false;

    int SELECT_SOURCE_IMAGE1_CODE = 210;
    int SELECT_SOURCE_IMAGE2_CODE = 220;

    /**
     *
     *
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate()");
        imageManipulator = new ImageManipulator(getActivity());
        imageProcessor = new EffectImageProcessor(getActivity(), this, imageManipulator);
    }

    /**
     *
     *
     */
    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);
        Log.v(TAG, "onAttach()");
    }

    /**
     *
     *
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        Log.v(TAG, "onCreateView()");

        View view = inflater.inflate(R.layout.fragment_manipulate_image_view, container, false);
        setHasOptionsMenu(true);

        // 左側画像
        final Button src1 = (Button) view.findViewById(R.id.selectSourceImage1Button);
        src1.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v)
                                    {
                                        Intent intent = new Intent(Intent.ACTION_PICK);
                                        intent.setType("image/*");
                                        startActivityForResult(intent, SELECT_SOURCE_IMAGE1_CODE);
                                    }

                                });

        // 右側画像
        final Button src2 = (Button) view.findViewById(R.id.selectSourceImage2Button);
        src2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, SELECT_SOURCE_IMAGE2_CODE);
            }

        });

/**
        // 画像に施す効果を選択
        final ImageButton effect = (ImageButton) view.findViewById(R.id.choiceEffectImageButton);
        effect.setOnClickListener(imageProcessor);

        // 画像の保存指示
        final Button save = (Button) view.findViewById(R.id.saveImageButton);
        save.setOnClickListener(imageProcessor);

        // 画像の共有指示
        final Button share = (Button) view.findViewById(R.id.shareImageButton);
        share.setOnClickListener(imageProcessor);
**/

        // プレビュー画像のタッチ
        final ImageView imgView = (ImageView) view.findViewById(R.id.targetImageView);
        imgView.setOnTouchListener(imageProcessor);

        return (view);
    }

    /**
     *
     *
     *
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.manipulate_view, menu);
        MenuItem saveMenuItem = menu.findItem(R.id.action_manipulate_save_image);
        saveMenuItem.setEnabled(enableSaveMenu);
        saveMenuItem.setVisible(enableSaveMenu);

        try {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            ActionBar bar = activity.getSupportActionBar();
            if (bar != null) {
                bar.setTitle(getString(R.string.pref_manipulate_image));
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     *   メニューが選択されたときの処理
     *
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        if (id ==  R.id.action_manipulate_image)
        {
            imageProcessor.selectEffectType(this);
        }
        else if (id == R.id.action_manipulate_save_image)
        {
            imageProcessor.selectedSaveImage();
        }
        else if (id == R.id.action_image_share)
        {
            imageProcessor.sharedSaveImage();
        }
        else
        {
            return (super.onOptionsItemSelected(item));
        }
        return (true);
    }

    /**
     *
     *
     */
    @Override
    public void onStart()
    {
        super.onStart();
        Log.v(TAG, "onStart()");
    }

    /**
     *
     *
     */
    @Override
    public void onResume()
    {
        super.onResume();
        Log.v(TAG, "onResume() Start");
        try {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            ActionBar bar = activity.getSupportActionBar();
            if (bar != null) {
                bar.setDisplayShowHomeEnabled(true);
                bar.show();
            }
            Log.v(TAG, "onResume() End");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     *
     *
     */
    @Override
    public void onPause()
    {
        super.onPause();
        Log.v(TAG, "onPause() Start");
        try
        {
            AppCompatActivity activity = (AppCompatActivity)getActivity();
            ActionBar bar = activity.getSupportActionBar();
            if (bar != null)
            {
                bar.hide();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        Log.v(TAG, "onPause() End");
    }

    /**
     *
     *
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        Log.v(TAG, "onActivityResult() : start");

        switch (resultCode)
        {
            case Activity.RESULT_OK:
                String filePath = "";
                String[] projection = {MediaStore.MediaColumns.DATA};
                try
                {
                    Cursor cursor = getActivity().getContentResolver().query(data.getData(), projection, null, null, null);
                    if (cursor != null)
                    {
                        if (cursor.getCount() > 0)
                        {
                            cursor.moveToNext();
                            filePath = cursor.getString(0);
                        }
                        cursor.close();
                    }
                    setActivityResultValue(requestCode, filePath);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                break;

            default:
                break;
        }
        Log.v(TAG, "onActivityResult() : end");
    }

    /**
     *   選択した画像を受け取る処理
     *
     * @param requestCode  種別
     * @param filePath     ファイル名
     */
    private void setActivityResultValue(int requestCode, String filePath)
    {
        if (requestCode == SELECT_SOURCE_IMAGE1_CODE)
        {
            manipulateImage1 = filePath;
            showImageView(filePath, R.id.sourceImage1Label, R.id.sourceImageView1);
        }
        else if (requestCode == SELECT_SOURCE_IMAGE2_CODE)
        {
            manipulateImage2 = filePath;
            showImageView(filePath, R.id.sourceImage2Label, R.id.sourceImageView2);
        }
    }

    /**
     *   画像を表示する
     *
     * @param filePath  画像ファイルのパス
     * @param labelId   画像ファイル名(文字)を表示する部品のID
     * @param imageId   画像を表示する部品のID
     */
    private void showImageView(String filePath, int labelId, int imageId)
    {
        // 画像とファイル名を表示する
        String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
        try
        {
            View view = getView();
            if (view != null)
            {
                ((TextView) view.findViewById(labelId)).setText(fileName);

                Log.v(TAG, "showImageView() :" + filePath);
                imageManipulator.setImage((ImageView) view.findViewById(imageId), filePath);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public String getSourceImage1()
    {
        return (manipulateImage1);
    }

    @Override
    public String getSourceImage2()
    {
        return (manipulateImage2);
    }

    @Override
    public ImageView getImageTargetImageView()
    {
        try
        {
            View view = getView();
            if (view != null)
            {
                return (ImageView) view.findViewById(R.id.targetImageView);
            }
        }

        catch (Exception e)
        {
            e.printStackTrace();
        }
        return (null);
    }

    /**
     *   画像加工の成否を伝達
     *
     * @param command  画像加工のコマンド
     * @param isSuccess trueなら、画像の加工が成功した（保存ボタンを有効にする）
     */
    @Override
    public void manipulateImageResult(int command, boolean isSuccess)
    {
        try {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            ActionBar bar = activity.getSupportActionBar();
            if (bar != null)
            {
                bar.setTitle(getString(R.string.pref_manipulate_image));
            }
            enableSaveMenu = isSuccess;
            activity.getFragmentManager().invalidateOptionsMenu();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}

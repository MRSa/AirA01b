package jp.osdn.gokigen.aira01b;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Settings;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import jp.co.olympus.camerakit.OLYCamera;
import jp.osdn.gokigen.aira01b.preference.ICameraPropertyAccessor;

/**
 *  ConnectingFragment
 *
 */
public class ConnectingFragment extends Fragment implements View.OnClickListener
{
    private final String TAG = this.toString();
    private TextView connectingTextView = null;
    private TextView connectInformationArea = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_connecting_view, container, false);
        String versionText = getString(R.string.sdk_version) + " " + OLYCamera.getVersion();
        TextView sdkVersionTextView = view.findViewById(R.id.sdkVersionTextView);
        if (sdkVersionTextView != null)
        {
            sdkVersionTextView.setText(versionText);
            sdkVersionTextView.setOnClickListener(this);
        }

        connectingTextView = view.findViewById(R.id.connectingStatusTextView);
        if (connectingTextView != null)
        {
            connectingTextView.setOnClickListener(this);
        }

        setHasOptionsMenu(true);

        connectInformationArea = view.findViewById(R.id.connectionInformationTextArea);
        if (connectInformationArea != null)
        {
            connectInformationArea.setOnClickListener(this);
        }

        // 起動画像のカスタマイズチェック
        ImageView splashImage = view.findViewById(R.id.splashImageView);
        if (splashImage != null)
        {
            //splashImage.setOnClickListener(this);  // 画像を押したときに処理するようにする
            SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(getContext());
            if (preference != null)
            {
                if (preference.getBoolean(ICameraPropertyAccessor.USE_CUSTOM_SPLASH, false))
                {
                    if (!setSplashImage(preference, splashImage))
                    {
                        // カスタムの画像表示に失敗した時はデフォルトロゴを出すようにする
                        splashImage.setImageResource(R.drawable.logo);
                    }
                }
            }
        }
        return (view);
    }

    /**
     *  メッセージを表示する
     *
     * @param message  表示するメッセージ
     */
    void setInformationText(String message)
    {
        if (connectingTextView != null)
        {
            connectingTextView.setText(message);
        }
    }

    /**
     *
     * @param message 表示するメッセージ
     */
    public void setConnectionText(String message)
    {
        if (connectInformationArea != null)
        {
            connectInformationArea.setText(message);
        }
    }

    /**
     *   （タイトルを表示する場合には）表示する
     *
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu,@NonNull MenuInflater inflater)
    {
        AppCompatActivity activity = (AppCompatActivity)getActivity();
        if (activity != null)
        {
            ActionBar bar = activity.getSupportActionBar();
            if (bar != null)
            {
                bar.setTitle(getString(R.string.app_name));
            }
        }
    }

    /**
     *   起動画像のカスタマイズ処理
     *
     * @param imageView 起動画像の表示エリア
     * @return true: 成功 / false: 失敗
     */
    private boolean setSplashImage(SharedPreferences preference, ImageView imageView)
    {
        boolean isCustomImage = false;
        try
        {
            String fileName = preference.getString(ICameraPropertyAccessor.SELECT_SPLASH_IMAGE, "");
            if (fileName != null)
            {
                if (fileName.length() > 0)
                {
                    File file = new File(fileName);
                    if (file.exists())
                    {
                        imageView.setImageURI(Uri.fromFile(file));
                        isCustomImage = true;
                    }
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return (isCustomImage);
    }

    /**
     *   隠し？機能...WiFi接続の進捗メッセージを押したとき...
     *
     */
    @Override
    public void onClick(View v)
    {
        try
        {
            // Wifi 設定画面を表示する
            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
        }
        catch (android.content.ActivityNotFoundException ex)
        {
            // Activity が存在しなかった...設定画面が起動できなかった
            Log.v(TAG, "android.content.ActivityNotFoundException...");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}

package com.stdio.videokiosk;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import com.warnyul.android.widget.FastVideoView;
import com.yausername.youtubedl_android.DownloadProgressCallback;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.mapper.VideoInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private boolean downloading = false;
    private Tools tools;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    View mVideoLayout;
    String[] ids = {"37-1SkifcnE", "sjIACXSI8wg", "Irv3KJR6B80", "9oHF4PcRnIY", "v77qRdZq57Q"};
    ArrayList<String> paths = new ArrayList<>();
    int position = 0;
    TextView tv, tvProgress;

    private DownloadProgressCallback callback = new DownloadProgressCallback() {
        @Override
        public void onProgressUpdate(float progress, long etaInSeconds) {
            runOnUiThread(() -> {
                        System.out.println(progress + "% (Required " + etaInSeconds + " seconds more)");
                        tvProgress.setText(position + 1 + " из " + ids.length + "\n" + progress + "% (Required " + etaInSeconds + " seconds more)");
                    }
            );
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        mVideoLayout = findViewById(R.id.video_layout);
        tv = findViewById(R.id.tv);
        tvProgress = findViewById(R.id.tvProgress);
        tools = new Tools(this);
        if (isNetworkConnected()) {
            startDownload();
        } else {
            tv.setText("Нет подключеия к интернету");
        }
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    //region start download and perform download related job
    private void startDownload() {
        //region if a download is already pending
        if (downloading) {
            Toast.makeText(MainActivity.this, "Cannot start download.A download is already in progress", Toast.LENGTH_LONG).show();
            return;
        }
        //endregion

        //region get permission
        if (!tools.isStoragePermissionGranted()) {
            Toast.makeText(MainActivity.this, "Grant storage permission and try again", Toast.LENGTH_LONG).show();
            return;
        }
        //endregion

        //region get url from editText
        String url = "https://www.youtube.com/watch?v=" + ids[position];

        YoutubeDLRequest request = new YoutubeDLRequest(url);
        File youtubeDLDir = getDownloadLocation();
        request.addOption("-o", youtubeDLDir.getAbsolutePath() + "/%(title)s.%(ext)s");

        downloading = true;
        try {
            YoutubeDL.getInstance().init(getApplication());
        } catch (YoutubeDLException e) {
            Log.e("Error", "Failed to initialize youtubedl-android", e);
        }
        Disposable disposable = Observable.fromCallable(() -> YoutubeDL.getInstance().execute(request, callback))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(youtubeDLResponse -> {
                    Toast.makeText(MainActivity.this, "Download successful", Toast.LENGTH_LONG).show();
                    downloading = false;
                    VideoInfo streamInfo = YoutubeDL.getInstance().getInfo(url);
                    position++;
                    paths.add(youtubeDLDir.getAbsolutePath() + "/" + streamInfo.getTitle() + ".mp4");
                    if (position < ids.length) {
                        startDownload();
                    } else {
                        tv.setVisibility(View.GONE);
                        tvProgress.setVisibility(View.GONE);
                        startPlaying();
                        position = 0;
                    }
                }, e -> {
                    if (BuildConfig.DEBUG) Log.e("Download Error", "Failed to download", e);

                    Toast.makeText(MainActivity.this, "Download failed", Toast.LENGTH_LONG).show();
                    downloading = false;
                });
        compositeDisposable.add(disposable);
    }
    //endregion

    private void startPlaying() {
        FastVideoView videoView = (FastVideoView)findViewById(R.id.video);
        videoView.setMediaController(new MediaController(this));
        videoView.setVideoPath(paths.get(position%ids.length));
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                position++;
                startPlaying();
            }
        });
        videoView.start();
    }

    @Override
    protected void onDestroy() {
        compositeDisposable.dispose();
        super.onDestroy();
    }

    @NonNull
    private File getDownloadLocation() {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File youtubeDLDir = new File(downloadsDir, "fast-youtube-downloader");
        if (!youtubeDLDir.exists()) youtubeDLDir.mkdir();
        return youtubeDLDir;
    }
}
package com.example.musicok.activity;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.musicok.R;
import com.example.musicok.player.MediaBrowserHelper;
import com.example.musicok.service.MusicService;
import com.example.musicok.util.MediaLibrary;
import com.example.musicok.util.TimeUtil;
import com.example.musicok.view.MediaSeekBar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private ImageView mIvPlay;
    private TextView mTvStartTime;
    private TextView mTvEndTime;
    private MediaSeekBar mSeekBar;
    private MediaBrowserHelper mMediaBrowserHelper;
    private boolean mIsPlaying;
    private TextView mTvSongName;
    private TextView mTvSonger;
    private LinearLayout mRootView;
    private List<MediaMetadataCompat> mPlaylist = new ArrayList<>();
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mIvPlay = findViewById(R.id.iv);
        mTvStartTime = findViewById(R.id.tv_startTime);
        mTvEndTime = findViewById(R.id.tv_endTime);
        mTvSongName = findViewById(R.id.tvSongName);
        mTvSonger = findViewById(R.id.tvSonger);
        mSeekBar = findViewById(R.id.seekbar);
        mRootView = findViewById(R.id.rootview);

        String mHost = "http://10.1.21.36:8080/";
//        mPlaylist.add(new MediaMetadataCompat.Builder()
//                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, "2019-12-135")
//                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, "2019-12-135")
//                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, mHost + "2019-12-135.mp3")
//                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, TimeUnit.MILLISECONDS.convert(2 * 60 * 60 + 17 * 60 + 36, TimeUnit.SECONDS))
//                .build());
//
//        mPlaylist.add(new MediaMetadataCompat.Builder()
//                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, "2019-12-116-1")
//                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, "2019-12-116-1")
//                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, mHost + "2019-12-116-1.mp3")
//                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, TimeUnit.MILLISECONDS.convert(28 * 60 + 25, TimeUnit.SECONDS))
//                .build());

//        mPlaylist.add(new MediaMetadataCompat.Builder()
//                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, "2019-12-129")
//                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, "2019-12-129")
//                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, "http://spcz.bjcourt.gov.cn/Uploads/2019-12-129.mp3")
//                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, TimeUnit.MILLISECONDS.convert(2 * 60 * 60 + 48 * 60 + 34, TimeUnit.SECONDS))
//                .build());

        mPlaylist.add(new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, "歌曲的名字1")
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, "作者的名字1")
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, mHost + "test1.mp3")
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, TimeUnit.MILLISECONDS.convert(160, TimeUnit.SECONDS))
                .build());

        mPlaylist.add(new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, "歌曲的名字2")
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, "作者的名字2")
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, mHost + "test2.mp3")
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, TimeUnit.MILLISECONDS.convert(102, TimeUnit.SECONDS))
                .build());

        mPlaylist.add(new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, "小苹果")
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, "筷子兄弟")
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, mHost + "test3.mp3")
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, TimeUnit.MILLISECONDS.convert(174, TimeUnit.SECONDS))
                .build());

        MediaLibrary.getInstance().setMedialList(mPlaylist);

        mMediaBrowserHelper = new MediaBrowserConnection(this);
        mMediaBrowserHelper.registerCallback(new MediaBrowserListener());
        mMediaBrowserHelper.onStart();

        //模拟音频地址
        String musicUrl = "";

        mIvPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsPlaying) {
                    mMediaBrowserHelper.getTransportControls().pause();
                } else {
                    mMediaBrowserHelper.getTransportControls().play();
                }
            }
        });

        mSeekBar.setOnSeekChangeListener(new MediaSeekBar.OnSeekChangeListener() {
            @Override
            public void onSeekChange(int progress, int max) {
                mTvStartTime.setText(TimeUtil.getTime(progress));
            }
        });

        mSeekBar.setOnMetadataChangedListener(new MediaSeekBar.OnMetadataChangedListener() {
            @Override
            public void onMetadataChanged() {
                mMediaBrowserHelper.getTransportControls().fastForward();
            }
        });

        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "3", Toast.LENGTH_SHORT).show();
                mMediaBrowserHelper.getTransportControls().skipToQueueItem(3);
            }
        });

        findViewById(R.id.btn_slow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMediaBrowserHelper.getTransportControls().setRating(RatingCompat.newPercentageRating(1f));
            }
        });

        findViewById(R.id.btn_fast).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMediaBrowserHelper.getTransportControls().setRating(RatingCompat.newPercentageRating(3f));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSeekBar.disconnectController();
        mMediaBrowserHelper.onStop();
    }

    private class MediaBrowserConnection extends MediaBrowserHelper {
        public MediaBrowserConnection(Context context) {
            super(context, MusicService.class);
        }

        @Override
        protected void onConnected(@NonNull MediaControllerCompat mediaControllerCompat) {
            mSeekBar.setMediaController(mediaControllerCompat);
        }

        @Override
        public void onLoadChildren(@NonNull String parentId, @NonNull List<MediaBrowserCompat.MediaItem> result) {
            MediaControllerCompat mediaController = getMediaController();
            for (MediaBrowserCompat.MediaItem mediaItem : result) {
                mediaController.addQueueItem(mediaItem.getDescription());
            }
            mediaController.getTransportControls().prepare();
        }
    }

    private class MediaBrowserListener extends MediaControllerCompat.Callback {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            mIsPlaying = state != null && state.getState() == PlaybackStateCompat.STATE_PLAYING;
            if (mIsPlaying) {
                mIvPlay.setBackgroundResource(R.drawable.ic_pause);
            } else {
                mIvPlay.setBackgroundResource(R.drawable.ic_play);
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            if (metadata == null) {
                return;
            }
            //修改歌名
            mTvSongName.setText(metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE));
            //修改演唱者
            mTvSonger.setText(metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE));
            //设置背景图 TODO Glide加载网络地址图片
//            mRootView.setBackgroundColor();

            mTvStartTime.setText(TimeUtil.getTime(0));
            mTvEndTime.setText(TimeUtil.getTime(metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)));
        }
    }
}

package com.example.musicok.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.View;
import android.widget.Button;

import com.example.musicok.R;
import com.example.musicok.player.MediaBrowserHelper;
import com.example.musicok.service.MusicService;
import com.example.musicok.util.TimeUtil;
import com.example.musicok.view.MediaSeekBar;

import java.util.List;

public class TestActivity extends AppCompatActivity {

    private MediaSeekBar mSeekBar;
    private Button btn;
    private MediaBrowserConnection mMediaBrowserHelper;
    private boolean mIsPlaying;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        mSeekBar = findViewById(R.id.seekbar);
        btn = findViewById(R.id.btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mIsPlaying){
                    mMediaBrowserHelper.getTransportControls().pause();
                }else{
                    mMediaBrowserHelper.getTransportControls().play();
                }
            }
        });

        mMediaBrowserHelper = new MediaBrowserConnection(this);
        mMediaBrowserHelper.registerCallback(new MediaBrowserListener());
    }

    @Override
    protected void onStart() {
        super.onStart();
        mMediaBrowserHelper.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
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
            for(MediaBrowserCompat.MediaItem mediaItem : result){
                mediaController.addQueueItem(mediaItem.getDescription());
            }
            mediaController.getTransportControls().prepare();
        }
    }

    private class MediaBrowserListener extends MediaControllerCompat.Callback {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            mIsPlaying = state!=null && state.getState() == PlaybackStateCompat.STATE_PLAYING;
            if(mIsPlaying){
                btn.setText("暂停");
            }else {
                btn.setText("播放");
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            if(metadata == null){
                return;
            }

//            //修改歌名
//            mTvSongName.setText(metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE));
//            //修改演唱者
//            mTvSonger.setText(metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE));
//            //设置背景图 TODO Glide加载网络地址图片
////            mRootView.setBackgroundColor();
//
//            mTvStartTime.setText(TimeUtil.getTime(0));
//            mTvEndTime.setText(TimeUtil.getTime(metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)));
        }
    }
}

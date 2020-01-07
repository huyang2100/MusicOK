package com.example.musicok.service;

import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.media.MediaBrowserServiceCompat;

import com.example.musicok.notification.MediaNotificationManager;
import com.example.musicok.player.MediaPlayerAdapter;
import com.example.musicok.player.PlaybackInfoListener;
import com.example.musicok.util.MediaLibrary;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 音乐播放服务
 */
public class MusicService extends MediaBrowserServiceCompat {
    private static final String TAG = "MusicService";
    private MediaSessionCompat mSession;
    private MediaSessionCallback mSessionCallback;
    private MediaNotificationManager mNotificationManager;
    private MediaPlayerAdapter mPlayerAdapter;
    private boolean mServiceInStartedState;
    private List<MediaBrowserCompat.MediaItem> mMediaItemList = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: ");

        //创建MediaSession（会话）
        mSession = new MediaSessionCompat(this, "MusicService");
        //创建MediaSessionCallback（会话回调）
        mSessionCallback = new MediaSessionCallback();
        //绑定MediaSessionCallback（会话 绑定 会话回调）
        mSession.setCallback(mSessionCallback);
        //设置MediaSession Flag（会话 Flag）
        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        //设置MediaSession Token（会话 Token）
        setSessionToken(mSession.getSessionToken());
        //创建通知管理器（通知）
        mNotificationManager = new MediaNotificationManager(this);
        //创建播放器Adapter（实际播放音乐）
        mPlayerAdapter = new MediaPlayerAdapter(this, new MediaPlayerListener());
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        //停止播放
        mPlayerAdapter.stop();
        //释放会话
        mSession.release();
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        return new BrowserRoot("root", null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        Log.d(TAG, "onLoadChildren: ");
        result.sendResult(mMediaItemList);
    }

    //按钮UI控制播放器Player
    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        private List<MediaMetadataCompat> mPlaylist = MediaLibrary.getInstance().getmMediaList();
        private int mQueueIndex = 0;
        private MediaMetadataCompat mPreparedMedia;

        @Override
        public void onAddQueueItem(MediaDescriptionCompat description) {

        }

        @Override
        public void onRemoveQueueItem(MediaDescriptionCompat description) {

        }

        @Override
        public void onSkipToQueueItem(long id) {
            super.onSkipToQueueItem(id);
            if (id < 0) {
                id = 0;
            }
            mQueueIndex = id >= mPlaylist.size() ? mPlaylist.size() - 1 : (int) id;
            mPreparedMedia = null;
            onPlay();
        }

        @Override
        public void onPrepare() {
            Log.d(TAG, "onPrepare: ");
            if (mPlaylist.isEmpty()) {
                // Nothing to play.
                return;
            }

            //TODO 通过binder获取需要播放音乐的地址
            mPreparedMedia = mPlaylist.get(mQueueIndex);
            mSession.setMetadata(mPreparedMedia);

            if (!mSession.isActive()) {
                mSession.setActive(true);
            }
        }

        @Override
        public void onPlay() {
            Log.d(TAG, "onPlay: ");
            if (mPlaylist.isEmpty()) {
                return;
            }

            if (mPreparedMedia == null) {
                onPrepare();
            }

            mPlayerAdapter.playFromUrl(mPreparedMedia);
        }

        @Override
        public void onPause() {
            Log.d(TAG, "onPause: ");
            mPlayerAdapter.pause();
        }

        @Override
        public void onSetRating(RatingCompat rating) {
            float speed = rating.getPercentRating();
            mPlayerAdapter.setSpeed(speed);
        }

        @Override
        public void onStop() {
            Log.d(TAG, "onStop: ");
            mPlayerAdapter.stop();
            mSession.setActive(false);
        }

        @Override
        public void onSkipToPrevious() {
            mQueueIndex = mQueueIndex > 0 ? mQueueIndex - 1 : mPlaylist.size() - 1;
            mPreparedMedia = null;
            onPlay();
        }

        @Override
        public void onSkipToNext() {
            mQueueIndex = (++mQueueIndex % mPlaylist.size());
            mPreparedMedia = null;
            onPlay();
        }

        @Override
        public void onSeekTo(long pos) {
            Log.d(TAG, "onSeekTo: ");
            mPlayerAdapter.seekTo(pos);
        }
    }

    public class MediaPlayerListener extends PlaybackInfoListener {
        private ServiceManager mServiceManager;

        public MediaPlayerListener() {
            mServiceManager = new ServiceManager();
        }

        @Override
        public void onPlaybackStateChange(PlaybackStateCompat state) {
            Log.d(TAG, "onPlaybackStateChange: ");
            //更改session状态
            mSession.setPlaybackState(state);

            switch (state.getState()) {
                case PlaybackStateCompat.STATE_PLAYING:
                    mServiceManager.moveServiceToStartedState(state);
                    break;
                case PlaybackStateCompat.STATE_PAUSED:
                    mServiceManager.updateNotificationForPause(state);
                    break;
                case PlaybackStateCompat.STATE_STOPPED:
                    mServiceManager.moveServiceOutOfStartedState(state);
                    break;
            }
        }


        private class ServiceManager {
            public void moveServiceToStartedState(PlaybackStateCompat state) {
                Notification notification = mNotificationManager.getNotification(mPlayerAdapter.getCurrentMedia(), state, getSessionToken());
                if (!mServiceInStartedState) {
                    ContextCompat.startForegroundService(MusicService.this, new Intent(MusicService.this, MusicService.class));
                    mServiceInStartedState = true;
                }

                startForeground(MediaNotificationManager.NOTIFICATION_ID, notification);
            }

            public void updateNotificationForPause(PlaybackStateCompat state) {
                stopForeground(false);
                Notification notification = mNotificationManager.getNotification(mPlayerAdapter.getCurrentMedia(), state, getSessionToken());
                mNotificationManager.getNotifiManager().notify(MediaNotificationManager.NOTIFICATION_ID, notification);
            }

            public void moveServiceOutOfStartedState(PlaybackStateCompat state) {
                stopForeground(true);
                stopSelf();
                mServiceInStartedState = false;
            }
        }
    }
}

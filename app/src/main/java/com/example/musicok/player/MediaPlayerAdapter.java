package com.example.musicok.player;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.SystemClock;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.Toast;

import com.example.musicok.service.MusicService;

import java.io.IOException;

public class MediaPlayerAdapter extends PlayerAdapter {
    private final Context mContext;
    private final PlaybackInfoListener mPlaybackInfoListener;
    private MediaPlayer mMediaPlayer;
    private String mUrl;
    private boolean mCurrentMediaPlayedCompletion;
    private static final String TAG = "MediaPlayerAdapter";
    private MediaMetadataCompat mCurrentMetadata;
    private long mSeekWhileNotPlaying = -1;
    private int mState;

    public MediaPlayerAdapter(Context context, PlaybackInfoListener playbackInfoListener) {
        super(context);
        mContext = context;
        mPlaybackInfoListener = playbackInfoListener;
    }

    private void initMediaPlayer() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    setNewState(PlaybackStateCompat.STATE_PAUSED);
                }
            });
        }
    }

    private void setNewState(@PlaybackStateCompat.State int state) {
        mState = state;

        if (mState == PlaybackStateCompat.STATE_STOPPED) {
            mCurrentMediaPlayedCompletion = true;
        }

        long reportPosition;
        if (mSeekWhileNotPlaying >= 0) {
            reportPosition = mSeekWhileNotPlaying;
            if (mState == PlaybackStateCompat.STATE_PLAYING) {
                mSeekWhileNotPlaying = -1;
            }
        } else {
            reportPosition = mMediaPlayer == null ? 0 : mMediaPlayer.getCurrentPosition();
        }

        PlaybackStateCompat.Builder builder = new PlaybackStateCompat.Builder();
        builder.setActions(getAvailableActions());
        builder.setState(mState, reportPosition, 1.0f, SystemClock.elapsedRealtime());
        mPlaybackInfoListener.onPlaybackStateChange(builder.build());
    }

    @PlaybackStateCompat.Actions
    private long getAvailableActions() {
        long actions = PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID | PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
        switch (mState) {
            case PlaybackStateCompat.STATE_STOPPED:
                actions |= PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE;
                break;
            case PlaybackStateCompat.STATE_PLAYING:
                actions |= PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SEEK_TO;
                break;
            case PlaybackStateCompat.STATE_PAUSED:
                actions |= PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_STOP;
                break;
            default:
                actions |= PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_PAUSE;
        }
        return actions;
    }

    @Override
    protected void onPlay() {
        if (mMediaPlayer != null && !mMediaPlayer.isPlaying()) {
            mMediaPlayer.start();
            setNewState(PlaybackStateCompat.STATE_PLAYING);
        }
    }

    @Override
    protected void onStop() {
        setNewState(PlaybackStateCompat.STATE_STOPPED);
        release();
    }

    @Override
    public void seekTo(long pos) {
        if (mMediaPlayer != null) {
            if (!mMediaPlayer.isPlaying()) {
                mSeekWhileNotPlaying = pos;
            }
            mMediaPlayer.seekTo((int) pos);
            //位置改变了，重新设置状态
            setNewState(mState);
        }
    }

    @Override
    protected void onPause() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            setNewState(PlaybackStateCompat.STATE_PAUSED);
        }
    }

    @Override
    protected boolean isPlaying() {
        return mMediaPlayer != null && mMediaPlayer.isPlaying();
    }

    @Override
    protected void setVolume(float volume) {
        if (mMediaPlayer != null) {
            mMediaPlayer.setVolume(volume, volume);
        }
    }

    public void playFromUrl(MediaMetadataCompat metadata) {
        mCurrentMetadata = metadata;
        String url = metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI);
        boolean isMediaChanged = mUrl == null || !mUrl.equals(url);
        if (mCurrentMediaPlayedCompletion) {
            isMediaChanged = true;
            mCurrentMediaPlayedCompletion = false;
        }

        if (!isMediaChanged) {
            if (!isPlaying()) {
                play();
            }
            return;
        } else {
            release();
        }

        mUrl = url;

        initMediaPlayer();

        try {
            mMediaPlayer.setDataSource(url);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "playFromUrl: " + e.toString());
        }

        mMediaPlayer.prepareAsync();

        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
                setNewState(PlaybackStateCompat.STATE_PLAYING);
            }
        });
    }

    private void release() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    public MediaMetadataCompat getCurrentMedia() {
        return mCurrentMetadata;
    }
}

package com.example.musicok.player;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
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
    private float mSpeed = 1.0f;
    private int mPercent;

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
        builder.setBufferedPosition(mPercent);
        builder.setState(mState, reportPosition, mSpeed, SystemClock.elapsedRealtime());
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
            mPercent = 0;
            release();
        }

        mUrl = url;

        initMediaPlayer();

        try {
            mMediaPlayer.setDataSource(url);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "playFromUrl: " + e.toString());
            Toast.makeText(mContext, e.toString(), Toast.LENGTH_SHORT).show();
        }

        mMediaPlayer.prepareAsync();

        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
                setNewState(PlaybackStateCompat.STATE_PLAYING);
            }
        });

        mMediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                return false;
            }
        });

        mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                switch (what) {
                    //File or network related operation errors
                    case MediaPlayer.MEDIA_ERROR_IO:
                        Toast.makeText(mContext, "网络错误，请检查网络！" + what, Toast.LENGTH_SHORT).show();
                        break;
                    //Bitstream is not conforming to the related coding standard or file spec
                    case MediaPlayer.MEDIA_ERROR_MALFORMED:
                        Toast.makeText(mContext, "码流不符合相关编码标准或文件规范！", Toast.LENGTH_SHORT).show();
                        break;
                    //Media server died. In this case, the application must release the MediaPlayer object and instantiate a new one
                    case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                        playFromUrl(mCurrentMetadata);
                        break;
                    //Some operation takes too long to complete, usually more than 3-5 seconds.
                    case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                        Toast.makeText(mContext, "超时！", Toast.LENGTH_SHORT).show();
                        break;
                    //Unspecified media player error
                    case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                        Toast.makeText(mContext, "未知错误！", Toast.LENGTH_SHORT).show();
                        break;
                    //Bitstream is conforming to the related coding standard or file spec, but the media framework does not support the feature
                    case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                        Toast.makeText(mContext, "不支持该编码！", Toast.LENGTH_SHORT).show();
                        break;
                    case -38:
                        Toast.makeText(mContext, "网络错误，请检查网络！ " + what, Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        Toast.makeText(mContext, "错误：" + what, Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        });

        mMediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                mPercent = percent;
                setNewState(mState);
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

    public void setSpeed(float speed) {
        if (mMediaPlayer != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                mSpeed = speed;
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.setPlaybackParams(mMediaPlayer.getPlaybackParams().setSpeed(speed));
                } else {
                    mMediaPlayer.setPlaybackParams(mMediaPlayer.getPlaybackParams().setSpeed(speed));
                    mMediaPlayer.pause();
                }
                setNewState(mState);
            }
        }
    }

    public void syncState() {
        setNewState(mState);
    }
}

package com.example.musicok.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.animation.LinearInterpolator;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatSeekBar;

import java.util.concurrent.TimeUnit;

public class MediaSeekBar extends AppCompatSeekBar {
    private static final String TAG = "MediaSeekBar";
    private boolean mIsTracking;
    private MediaControllerCompat mMediaController;
    private MediaControllerCallback mControllerCallback;
    private ValueAnimator mProgressAnimator;
    private OnSeekChangeListener mOnSeekChangeListener;
    private OnMetadataChangedListener mMetadataChangedListener;

    public MediaSeekBar(Context context) {
        super(context);
    }

    public MediaSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MediaSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    {
        super.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mOnSeekChangeListener != null) {
                    mOnSeekChangeListener.onSeekChange(progress, seekBar.getMax());
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mIsTracking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mIsTracking = false;
                if (mMediaController != null) {
                    mMediaController.getTransportControls().seekTo(seekBar.getProgress());
                }
            }
        });
    }

    public void setOnSeekChangeListener(OnSeekChangeListener onSeekChangeListener) {
        mOnSeekChangeListener = onSeekChangeListener;
    }

    public interface OnSeekChangeListener {
        void onSeekChange(int progress, int max);
    }

    @Override
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener l) {
        throw new UnsupportedOperationException("MediaSeekBar不能添加监听器！");
    }

    public void setMediaController(MediaControllerCompat mediaController) {
        if (mediaController != null) {
            //注册
            mControllerCallback = new MediaControllerCallback();
            mediaController.registerCallback(mControllerCallback);
        } else if (mMediaController != null) {
            //反注册
            mMediaController.unregisterCallback(mControllerCallback);
            mControllerCallback = null;
        }
        mMediaController = mediaController;
    }

    public void disconnectController() {
        if (mMediaController != null) {
            mMediaController.unregisterCallback(mControllerCallback);
            mControllerCallback = null;
            mMediaController = null;
        }
    }

    private class MediaControllerCallback extends MediaControllerCompat.Callback implements ValueAnimator.AnimatorUpdateListener {

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            if (mIsTracking) {
                animation.cancel();
                return;
            }

            int animatedValue = (int) animation.getAnimatedValue();
            setProgress(animatedValue);
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            //去除已存在的动画
            if (mProgressAnimator != null) {
                mProgressAnimator.cancel();
                mProgressAnimator = null;
            }

            int progress = state != null ? (int) state.getPosition() : 0;

            setProgress(progress);

            int bufferProgress = state != null ? (int) state.getBufferedPosition() : 0;
            setSecondaryProgress(bufferProgress * getMax() / 100);

            Log.d(TAG, String.format("onPlaybackStateChanged: state: %d, max: %d ,progress: %d", state.getState(), getMax(), progress));

            //如果媒体正在播放，则seekbar需要跟随拖动
            if (state != null && state.getState() == PlaybackStateCompat.STATE_PLAYING) {
                //剩余的播放时间
                int timeToEnd = (int) ((getMax() - progress) / state.getPlaybackSpeed());
                if (timeToEnd <= 0) {
                    return;
                }

                if (mProgressAnimator != null) {
                    mProgressAnimator.cancel();
                }

                mProgressAnimator = ValueAnimator.ofInt(progress, getMax()).setDuration(timeToEnd);
                mProgressAnimator.setInterpolator(new LinearInterpolator());
                mProgressAnimator.addUpdateListener(this);
                mProgressAnimator.start();
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            Log.d(TAG, "onPlaybackStateChanged: seek");
            int max = metadata != null ? (int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION) : 0;
            setMax(max);
            setProgress(0);
            setSecondaryProgress(0);

            if (mMetadataChangedListener != null) {
                mMetadataChangedListener.onMetadataChanged();
            }
        }
    }

    public interface OnMetadataChangedListener {
        void onMetadataChanged();
    }

    public void setOnMetadataChangedListener(OnMetadataChangedListener onMetadataChangedListener) {
        mMetadataChangedListener = onMetadataChangedListener;
    }
}

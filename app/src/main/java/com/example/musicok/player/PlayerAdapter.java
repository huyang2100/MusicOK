package com.example.musicok.player;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.support.v4.media.MediaMetadataCompat;

abstract class PlayerAdapter {
    private static final float MEDIA_VOLUME_DEFAULT = 1.0f;
    private static final float MEDIA_VOLUME_DUCK = 0.2f;
    private final Context mAppContext;
    private BroadcastReceiver mAudioNosiyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())){
                if(isPlaying()){
                    pause();
                }
            }
        }
    };
    private final AudioManager mAudioManager;
    private final AudioFocusHelper mAudioFocusHelper;
    private boolean mPlayOnAudioFocus;
    private boolean mAudioNosiyReceiverRegisted;

    public PlayerAdapter(Context context) {
        mAppContext = context.getApplicationContext();
        mAudioManager = (AudioManager) mAppContext.getSystemService(Context.AUDIO_SERVICE);
        mAudioFocusHelper = new AudioFocusHelper();

    }

    protected abstract void playFromUrl(MediaMetadataCompat metadata);

    protected abstract MediaMetadataCompat getCurrentMedia();

    protected abstract boolean isPlaying();

    protected void play() {
        if(mAudioFocusHelper.requestAudioFocus()){
            registerAudioNosiyReceiver();
            onPlay();
        }
    }

    private void registerAudioNosiyReceiver() {
        if(!mAudioNosiyReceiverRegisted){
            mAppContext.registerReceiver(mAudioNosiyReceiver,new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
            mAudioNosiyReceiverRegisted = true;
        }
    }

    private void unregisterAudioNosiyReceiver(){
        if(mAudioNosiyReceiverRegisted){
            mAppContext.unregisterReceiver(mAudioNosiyReceiver);
            mAudioNosiyReceiverRegisted = false;
        }
    }

    protected abstract void onPlay();

    public void pause() {
        if(!mPlayOnAudioFocus) mAudioFocusHelper.abandonAudioFocus();
        unregisterAudioNosiyReceiver();
        onPause();
    }

    protected abstract void onPause();

    public void stop() {
        mAudioFocusHelper.abandonAudioFocus();
        unregisterAudioNosiyReceiver();
        onStop();
    }

    protected abstract void onStop();

    protected abstract void seekTo(long pos);

    protected abstract void setVolume(float volume);

    private class AudioFocusHelper implements AudioManager.OnAudioFocusChangeListener{

        private boolean requestAudioFocus(){
            int result = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }

        private void abandonAudioFocus(){
            mAudioManager.abandonAudioFocus(this);
        }

        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange){
                case AudioManager.AUDIOFOCUS_GAIN:
                    if(mPlayOnAudioFocus && !isPlaying()){
                        play();
                    }else if(isPlaying()){
                        setVolume(MEDIA_VOLUME_DEFAULT);
                    }

                    mPlayOnAudioFocus = false;
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    setVolume(MEDIA_VOLUME_DUCK);
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    if(isPlaying()){
                        mPlayOnAudioFocus = true;
                        pause();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    mAudioManager.abandonAudioFocus(this);
                    mPlayOnAudioFocus = false;
                    stop();
                    break;

            }
        }
    }

}

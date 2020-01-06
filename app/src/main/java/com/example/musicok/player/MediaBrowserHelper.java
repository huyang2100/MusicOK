package com.example.musicok.player;

import android.content.ComponentName;
import android.content.Context;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.media.MediaBrowserServiceCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * 帮助类：管理MediaBrowser连接和断开连接
 */
public class MediaBrowserHelper {
    private final Context mContext;
    private Class<? extends MediaBrowserServiceCompat> mBrowserServiceClass;
    private MediaBrowserConnectionCallback mBrowserConnCallback;
    private MediaControllerCallback mControllerCallback;
    private MediaBrowserSubscriptionCallback mBrowserSubCallback;
    private MediaBrowserCompat mMediaBrowser;
    private MediaControllerCompat mMediaController;
    private static final String TAG = "MediaBrowserHelper";
    private List<MediaControllerCompat.Callback> mCallbackList = new ArrayList<>();

    public MediaBrowserHelper(Context context, Class<? extends MediaBrowserServiceCompat> serviceClass) {
        mContext = context;
        mBrowserServiceClass = serviceClass;
        mBrowserConnCallback = new MediaBrowserConnectionCallback();
        mControllerCallback = new MediaControllerCallback();
        mBrowserSubCallback = new MediaBrowserSubscriptionCallback();
    }

    public void onStart() {
        if (mMediaBrowser == null) {
            //新建一个客户端
            mMediaBrowser = new MediaBrowserCompat(mContext, new ComponentName(mContext, mBrowserServiceClass), mBrowserConnCallback, null);
            //连接客户端
            mMediaBrowser.connect();
        }
    }

    public void onStop() {
        if (mMediaController != null) {
            mMediaController.unregisterCallback(mControllerCallback);
            mMediaController = null;
        }

        if (mMediaBrowser != null && mMediaBrowser.isConnected()) {
            mMediaBrowser.disconnect();
            mMediaBrowser = null;
        }

        resetState();
    }

    private void resetState() {
        performOnAllCallbacks(new CallbackCommand() {
            @Override
            public void perform(MediaControllerCompat.Callback callback) {
                callback.onPlaybackStateChanged(null);
            }
        });

        Log.d(TAG, "resetState: ");
    }

    private void performOnAllCallbacks(@NonNull CallbackCommand command) {
        for (MediaControllerCompat.Callback callback : mCallbackList) {
            if (callback != null) {
                command.perform(callback);
            }
        }
    }

    protected void onConnected(@NonNull MediaControllerCompat mediaControllerCompat) {
        Log.d(TAG, "onConnected: ");
    }

    public void onLoadChildren(@NonNull String parentId, @NonNull List<MediaBrowserCompat.MediaItem> result) {
        Log.d(TAG, "onLoadChildren: ");
    }

    protected void onDisconnected() {
        Log.d(TAG, "onDisconnected: ");
    }

    protected MediaControllerCompat getMediaController() {
        if (mMediaController == null) {
            throw new IllegalStateException("MediaController为null!");
        }
        return mMediaController;
    }

    //客户端成功连接Service后的回调
    private class MediaBrowserConnectionCallback extends MediaBrowserCompat.ConnectionCallback {
        @Override
        public void onConnected() {
            Log.d(TAG, "onConnected: ");
            try {
                mMediaController = new MediaControllerCompat(mContext, mMediaBrowser.getSessionToken());
                mMediaController.registerCallback(mControllerCallback);

                mControllerCallback.onMetadataChanged(mMediaController.getMetadata());
                mControllerCallback.onPlaybackStateChanged(mMediaController.getPlaybackState());
                MediaBrowserHelper.this.onConnected(mMediaController);
            } catch (RemoteException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }

            mMediaBrowser.subscribe(mMediaBrowser.getRoot(), mBrowserSubCallback);
        }
    }

    private class MediaControllerCallback extends MediaControllerCompat.Callback {
        @Override
        public void onMetadataChanged(final MediaMetadataCompat metadata) {
            performOnAllCallbacks(new CallbackCommand() {
                @Override
                public void perform(MediaControllerCompat.Callback callback) {
                    callback.onMetadataChanged(metadata);
                }
            });
        }

        @Override
        public void onPlaybackStateChanged(final PlaybackStateCompat state) {
            performOnAllCallbacks(new CallbackCommand() {
                @Override
                public void perform(MediaControllerCompat.Callback callback) {
                    callback.onPlaybackStateChanged(state);
                }
            });
        }

        @Override
        public void onSessionDestroyed() {
            resetState();
            onPlaybackStateChanged(null);
            MediaBrowserHelper.this.onDisconnected();
        }
    }

    private class MediaBrowserSubscriptionCallback extends MediaBrowserCompat.SubscriptionCallback {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaBrowserCompat.MediaItem> children) {
            MediaBrowserHelper.this.onLoadChildren(parentId, children);
        }
    }

    interface CallbackCommand {
        void perform(MediaControllerCompat.Callback callback);
    }

    public MediaControllerCompat.TransportControls getTransportControls(){
        if(mMediaController == null){
            Log.d(TAG, "getTransportControls: ");
            throw new IllegalStateException("MediaController为Null！");
        }
        return mMediaController.getTransportControls();
    }

    public void registerCallback(MediaControllerCompat.Callback callback){
        if(callback!=null){
            mCallbackList.add(callback);

            if(mMediaController!=null){
                MediaMetadataCompat metadata = mMediaController.getMetadata();
                if(metadata!=null){
                    callback.onMetadataChanged(metadata);
                }

                PlaybackStateCompat playbackState = mMediaController.getPlaybackState();
                if(playbackState!=null){
                    callback.onPlaybackStateChanged(playbackState);
                }
            }
        }
    }
}

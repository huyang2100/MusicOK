package com.example.musicok.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.media.session.MediaButtonReceiver;

import com.example.musicok.R;
import com.example.musicok.activity.MainActivity;
import com.example.musicok.service.MusicService;

public class MediaNotificationManager {
    public static final int NOTIFICATION_ID = 412;
    private static final int REQUEST_CODE = 501;
    private static final String CHANNEL_ID = MediaNotificationManager.class.getPackage().getName() + ".channel";
    private final MusicService mMusicService;
    private final NotificationManager mNotifiManager;
    private final NotificationCompat.Action mPlayAction;
    private final NotificationCompat.Action mPauseAction;
    private final NotificationCompat.Action mPreAction;
    private final NotificationCompat.Action mNextAction;

    public MediaNotificationManager(MusicService musicService) {
        mMusicService = musicService;

        mNotifiManager = (NotificationManager) mMusicService.getSystemService(Context.NOTIFICATION_SERVICE);

        mPlayAction = new NotificationCompat.Action(R.drawable.ic_play_white_24dp, "播放", MediaButtonReceiver.buildMediaButtonPendingIntent(mMusicService, PlaybackStateCompat.ACTION_PLAY));
        mPauseAction = new NotificationCompat.Action(R.drawable.ic_pause_white_24dp, "暂停", MediaButtonReceiver.buildMediaButtonPendingIntent(mMusicService, PlaybackStateCompat.ACTION_PAUSE));
        mPreAction = new NotificationCompat.Action(R.drawable.ic_skip_previous_white_24dp, "上一个", MediaButtonReceiver.buildMediaButtonPendingIntent(mMusicService, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS));
        mNextAction = new NotificationCompat.Action(R.drawable.ic_skip_next_white_24dp, "下一个", MediaButtonReceiver.buildMediaButtonPendingIntent(mMusicService, PlaybackStateCompat.ACTION_SKIP_TO_NEXT));

        //取消先前的所有通知
        mNotifiManager.cancelAll();
    }

    public NotificationManager getNotifiManager() {
        return mNotifiManager;
    }

    public Notification getNotification(MediaMetadataCompat mediaMetadataCompat, PlaybackStateCompat state, MediaSessionCompat.Token token) {
        MediaDescriptionCompat description = mediaMetadataCompat.getDescription();
        NotificationCompat.Builder builder = buildNotification(state, token, description);
        return builder.build();
    }

    private NotificationCompat.Builder buildNotification(PlaybackStateCompat state, MediaSessionCompat.Token token, MediaDescriptionCompat description) {
        //AndroidO+需要创建Channel
        if (isAndroidOOrHeigher()) {
            createChannel();
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(mMusicService, CHANNEL_ID);
        builder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(token)
                .setShowActionsInCompactView(0, 1, 2)
                .setShowCancelButton(true)
                .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(mMusicService, PlaybackStateCompat.ACTION_STOP)))
                .setColor(ContextCompat.getColor(mMusicService, R.color.colorPrimary))
                .setSmallIcon(R.drawable.ic_stat_image_audiotrack)
                .setContentIntent(createContentIntent())//用户点击后的意图
                .setContentTitle(description.getTitle())//通常是歌曲的名称
                .setContentText(description.getSubtitle())//通常是歌曲的作者
                .setLargeIcon(BitmapFactory.decodeResource(mMusicService.getResources(),R.drawable.ic_pause))//TODO 设置通知的背景图片
                .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(mMusicService, PlaybackStateCompat.ACTION_STOP))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);//不在锁屏上显示该通知（自定义显示）


        if ((state.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) != 0) {
            builder.addAction(mPreAction);
        }

        boolean isPlaying = state.getState() == PlaybackStateCompat.STATE_PLAYING;
        builder.addAction(isPlaying ? mPauseAction : mPlayAction);

        if ((state.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_NEXT) != 0) {
            builder.addAction(mNextAction);
        }

        return builder;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createChannel() {
        if (mNotifiManager.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "music_channel", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("music channel desc");
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            mNotifiManager.createNotificationChannel(channel);
        }
    }

    private PendingIntent createContentIntent() {
        Intent intent = new Intent(mMusicService, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(mMusicService, REQUEST_CODE, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private boolean isAndroidOOrHeigher() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }
}

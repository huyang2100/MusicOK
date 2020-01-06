package com.example.musicok.util;

import android.support.v4.media.MediaMetadataCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MediaLibrary {
    private MediaLibrary(){}
    private List<MediaMetadataCompat> mPlaylist = new ArrayList<>();

    private static MediaLibrary mMediaLibrary = new MediaLibrary();

    public static MediaLibrary getInstance(){
        return mMediaLibrary;
    }

    public void setMedialList(List<MediaMetadataCompat> list){
        mPlaylist = list;
    }

    public List<MediaMetadataCompat> getmMediaList() {
        return mPlaylist;
    }
}

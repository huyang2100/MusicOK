package com.example.musicok.util;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class TimeUtil {
    public static String getTime(long second) {
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        formatter.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
        String hms = formatter.format(second);
        if(hms.startsWith("00:")){
            hms = hms.substring(3);
        }
        return hms;
    }
}

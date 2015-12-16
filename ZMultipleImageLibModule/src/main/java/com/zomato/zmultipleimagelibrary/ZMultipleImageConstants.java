package com.zomato.zmultipleimagelibrary;

/**
 * Created by cagkanciloglu on 12/15/15.
 */
public class ZMultipleImageConstants {
    public static final int REQUEST_CODE = 4000;

    public static final int FETCH_STARTED = 4001;
    public static final int FETCH_COMPLETED = 4002;
    public static final int PERMISSION_GRANTED = 4003;
    public static final int PERMISSION_DENIED = 4004;


    public static final int PERMISSION_REQUEST_READ_EXTERNAL_STORAGE = 34;

    public static final String INTENT_EXTRA_PHOTOALBUM = "photo_album";
    public static final String INTENT_EXTRA_PHOTOS = "photos";
    public static final String INTENT_EXTRA_SELECTED_PHOTOS = "selected_photos";
    public static final String INTENT_EXTRA_LIMIT = "photo_limit";
    public static final int DEFAULT_LIMIT = 10;

    //Maximum number of photo that can be selected at a time
    public static int limit;
}

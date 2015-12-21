package com.zomato.zmultipleimagelibrary.models;

import java.util.Comparator;

/**
 * Created by cagkanciloglu on 12/15/15.
 */
public class PhotoAlbum {
    public String name;
    public String cover;
    public int photoCount;

    public PhotoAlbum(String name, String cover) {
        this.name = name;
        this.cover = cover;
    }

    public PhotoAlbum(String name, String cover, int photoCount) {
        this.name = name;
        this.cover = cover;
        this.photoCount = photoCount;
    }

    public static class PhotoAlbumComparator implements Comparator<PhotoAlbum> {

        public int compare(PhotoAlbum obj1, PhotoAlbum obj2) {
            return obj1.name.compareTo(obj2.name);
        }

    }
}

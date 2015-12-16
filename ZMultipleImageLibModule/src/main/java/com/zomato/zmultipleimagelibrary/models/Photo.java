package com.zomato.zmultipleimagelibrary.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by cagkanciloglu on 12/15/15.
 */
public class Photo implements Parcelable {
    public long id;
    public String name;
    public String path;
    public boolean isSelected;

    public Photo(long id, String name, String path, boolean isSelected) {
        this.id = id;
        this.name = name;
        this.path = path;
        this.isSelected = isSelected;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(name);
        dest.writeString(path);
    }

    public static final Parcelable.Creator<Photo> CREATOR = new Parcelable.Creator<Photo>() {
        @Override
        public Photo createFromParcel(Parcel source) {
            return new Photo(source);
        }

        @Override
        public Photo[] newArray(int size) {
            return new Photo[size];
        }
    };

    private Photo(Parcel in) {
        id = in.readLong();
        name = in.readString();
        path = in.readString();
    }
}
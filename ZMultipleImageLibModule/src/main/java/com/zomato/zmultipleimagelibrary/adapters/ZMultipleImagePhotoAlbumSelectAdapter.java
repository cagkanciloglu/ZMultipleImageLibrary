package com.zomato.zmultipleimagelibrary.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.zomato.zmultipleimagelibrary.R;
import com.zomato.zmultipleimagelibrary.models.PhotoAlbum;

import java.util.ArrayList;

/**
 * Created by cagkanciloglu on 12/15/15.
 */
public class ZMultipleImagePhotoAlbumSelectAdapter extends ZMultipleImageGenericAdapter<PhotoAlbum> {
    public ZMultipleImagePhotoAlbumSelectAdapter(Context context, ArrayList<PhotoAlbum> albums) {
        super(context, albums);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.grid_view_item_album_select, null);

            viewHolder = new ViewHolder();
            viewHolder.imageView = (ImageView) convertView.findViewById(R.id.image_view_album_image);
            viewHolder.textView = (TextView) convertView.findViewById(R.id.text_view_album_name);
            viewHolder.textViewCount = (TextView) convertView.findViewById(R.id.text_view_album_photo_count);

            convertView.setTag(viewHolder);

        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.imageView.getLayoutParams().width = size;
        viewHolder.imageView.getLayoutParams().height = size;

        viewHolder.textView.setText(arrayList.get(position).name);
        viewHolder.textViewCount.setText(String.valueOf(arrayList.get(position).photoCount));
        Glide.with(context)
                .load(arrayList.get(position).cover)
                .placeholder(R.drawable.image_placeholder).centerCrop().into(viewHolder.imageView);

        return convertView;
    }

    private static class ViewHolder {
        public ImageView imageView;
        public TextView textView;
        public TextView textViewCount;
    }
}
package com.zomato.zmultipleimagelibrary.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.zomato.zmultipleimagelibrary.R;
import com.zomato.zmultipleimagelibrary.ZMultipleImageConstants;
import com.zomato.zmultipleimagelibrary.adapters.ZMultipleImagePhotoAlbumSelectAdapter;
import com.zomato.zmultipleimagelibrary.models.Photo;
import com.zomato.zmultipleimagelibrary.models.PhotoAlbum;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by cagkanciloglu on 12/15/15.
 */
public class ZMultipleImagePhotoAlbumSelectActivity extends AppCompatActivity {
    private final String TAG = ZMultipleImagePhotoAlbumSelectActivity.class.getName();

    private ArrayList<PhotoAlbum> albums;

    private TextView permissionHint;

    private ProgressBar progressBar;
    private GridView gridView;
    private ZMultipleImagePhotoAlbumSelectAdapter adapter;

    private ActionBar actionBar;

    private ContentObserver observer;
    private Handler handler;
    private Thread thread;

    private final String[] projection = new String[]{ MediaStore.Images.Media.BUCKET_DISPLAY_NAME, MediaStore.Images.Media.DATA };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_select);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);

            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(R.string.album_view);
        }

        Intent intent = getIntent();
        if (intent == null) {
            finish();
        }
        ZMultipleImageConstants.limit = intent.getIntExtra(ZMultipleImageConstants.INTENT_EXTRA_LIMIT, ZMultipleImageConstants.DEFAULT_LIMIT);

        permissionHint = (TextView) findViewById(R.id.text_view_permission_denied);
        permissionHint.setVisibility(View.INVISIBLE);

        progressBar = (ProgressBar) findViewById(R.id.progress_bar_album_select);
        gridView = (GridView) findViewById(R.id.grid_view_album_select);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), ZMultipleImageSelectActivity.class);
                intent.putExtra(ZMultipleImageConstants.INTENT_EXTRA_PHOTOALBUM, albums.get(position).name);
                intent.putExtra(ZMultipleImageConstants.INTENT_EXTRA_SELECTED_PHOTOS,getIntent().getParcelableArrayListExtra(ZMultipleImageConstants.INTENT_EXTRA_SELECTED_PHOTOS));
                startActivityForResult(intent, ZMultipleImageConstants.REQUEST_CODE);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case ZMultipleImageConstants.PERMISSION_GRANTED: {
                        permissionHint.setVisibility(View.INVISIBLE);

                        loadAlbums();

                        break;
                    }

                    case ZMultipleImageConstants.PERMISSION_DENIED: {
                        Toast.makeText(getApplicationContext(), getString(R.string.permission_denied), Toast.LENGTH_SHORT).show();

                        permissionHint.setVisibility(View.VISIBLE);

                        progressBar.setVisibility(View.INVISIBLE);
                        gridView.setVisibility(View.INVISIBLE);

                        break;
                    }

                    case ZMultipleImageConstants.FETCH_STARTED: {
                        progressBar.setVisibility(View.VISIBLE);
                        gridView.setVisibility(View.INVISIBLE);

                        break;
                    }

                    case ZMultipleImageConstants.FETCH_COMPLETED: {
                        if (adapter == null) {
                            adapter = new ZMultipleImagePhotoAlbumSelectAdapter(getApplicationContext(), albums);
                            gridView.setAdapter(adapter);

                            progressBar.setVisibility(View.INVISIBLE);
                            gridView.setVisibility(View.VISIBLE);
                            orientationBasedUI(getResources().getConfiguration().orientation);

                        } else {
                            adapter.notifyDataSetChanged();
                        }

                        break;
                    }

                    default: {
                        super.handleMessage(msg);
                    }
                }
            }
        };
        observer = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                loadAlbums();
            }
        };
        getContentResolver().registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false, observer);

        requestPermission();
    }

    private void requestPermission() {
        if (ContextCompat.checkSelfPermission(ZMultipleImagePhotoAlbumSelectActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(ZMultipleImagePhotoAlbumSelectActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, ZMultipleImageConstants.PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
            return;
        }

        Message message = handler.obtainMessage();
        message.what = ZMultipleImageConstants.PERMISSION_GRANTED;
        message.sendToTarget();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == ZMultipleImageConstants.PERMISSION_REQUEST_READ_EXTERNAL_STORAGE) {
            Message message = handler.obtainMessage();
            message.what = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED ? ZMultipleImageConstants.PERMISSION_GRANTED : ZMultipleImageConstants.PERMISSION_DENIED;
            message.sendToTarget();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        abortLoading();

        getContentResolver().unregisterContentObserver(observer);
        observer = null;

        handler.removeCallbacksAndMessages(null);
        handler = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(null);
        }
        albums = null;
        if (adapter != null) {
            adapter.releaseResources();
        }
        gridView.setOnItemClickListener(null);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        orientationBasedUI(newConfig.orientation);
    }

    private void orientationBasedUI(int orientation) {
        final WindowManager windowManager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        final DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);

        if (adapter != null) {
            int size = orientation == Configuration.ORIENTATION_PORTRAIT ? metrics.widthPixels / 2 : metrics.widthPixels / 4;
            adapter.setLayoutParams(size);
        }
        gridView.setNumColumns(orientation == Configuration.ORIENTATION_PORTRAIT ? 2 : 4);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ZMultipleImageConstants.REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            setResult(RESULT_OK, data);
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                onBackPressed();
                return true;
            }

            default: {
                return false;
            }
        }
    }

    private void loadAlbums() {
        abortLoading();

        AlbumLoaderRunnable runnable = new AlbumLoaderRunnable();
        thread = new Thread(runnable);
        thread.start();
    }


    private void abortLoading() {
        if (thread == null) {
            return;
        }

        if (thread.isAlive()) {
            thread.interrupt();
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private class AlbumLoaderRunnable implements Runnable {
        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

            Message message;
            if (adapter == null) {
                message = handler.obtainMessage();
                message.what = ZMultipleImageConstants.FETCH_STARTED;
                message.sendToTarget();
            }

            if (Thread.interrupted()) {
                return;
            }


            Cursor cursor = getApplicationContext().getContentResolver()
                    .query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection,
                            null, null, MediaStore.Images.Media.DATE_ADDED);

            ArrayList<PhotoAlbum> temp = new ArrayList<PhotoAlbum>(cursor.getCount());
            HashSet<String> albumSet = new HashSet<String>();
            int allPhotoCount = 0;
            File file;
            PhotoAlbum allPhotos = null;
            if (cursor.moveToLast()) {
                do {
                    if (Thread.interrupted()) {
                        return;
                    }

                    String album = cursor.getString(cursor.getColumnIndex(projection[0]));
                    String image = cursor.getString(cursor.getColumnIndex(projection[1]));


                    /*
                    It may happen that some image file paths are still present in cache,
                    though image file does not exist. These last as long as media
                    scanner is not run again. To avoid get such image file paths, check
                    if image file exists.
                     */
                    file = new File(image);
                    if (file.exists() && !albumSet.contains(album))
                    {
                        if (allPhotos == null)
                        {
                            allPhotos = new PhotoAlbum(getString(R.string.all_photos),image);//latest photo
                            temp.add(allPhotos);
                            albumSet.add(getString(R.string.all_photos));
                        }
                        Cursor cursor1 = getApplicationContext().getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null,
                                MediaStore.Images.Media.BUCKET_DISPLAY_NAME + " =?", new String[]{album}, null);

                        allPhotoCount += cursor1.getCount();

                        temp.add(new PhotoAlbum(album, image, cursor1.getCount()));
                        albumSet.add(album);
                    }
                } while (cursor.moveToPrevious());
            }
            cursor.close();

            if (albums == null) {
                albums = new ArrayList<PhotoAlbum>();
            }
            albums.clear();
            albums.addAll(temp);

            PhotoAlbum.PhotoAlbumComparator comparator = new PhotoAlbum.PhotoAlbumComparator();
            Collections.sort(albums, comparator);
            int i = albums.indexOf(allPhotos);
            Collections.swap(albums, i, 0);
            for (PhotoAlbum album:albums) {
                if (album.name.equalsIgnoreCase(getString(R.string.all_photos)))
                    album.photoCount = allPhotoCount;
            }

            message = handler.obtainMessage();
            message.what = ZMultipleImageConstants.FETCH_COMPLETED;
            message.sendToTarget();

            Thread.interrupted();
        }
    }
}

package com.zomato.zmultipleimagelibrary.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.*;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
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
import com.zomato.zmultipleimagelibrary.adapters.ZMultipleImageSelectAdapter;
import com.zomato.zmultipleimagelibrary.models.Photo;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by cagkanciloglu on 12/15/15.
 */
public class ZMultipleImageSelectActivity  extends AppCompatActivity {
    private ArrayList<Photo> images;
    private ArrayList<Photo> selectedPhotos;
    private String album;

    private TextView permissionHint;

    private ProgressBar progressBar;
    private GridView gridView;
    private ZMultipleImageSelectAdapter adapter;

    private ActionBar actionBar;

    private ActionMode actionMode;
    private int countSelected;

    private ContentObserver observer;
    private Handler handler;
    private Thread thread;

    private final String[] projection = new String[]{ MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATA };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_select);

        Intent intent = getIntent();
        if (intent == null) {
            finish();
        }
        album = intent.getStringExtra(ZMultipleImageConstants.INTENT_EXTRA_PHOTOALBUM);
        selectedPhotos = intent.getParcelableArrayListExtra(ZMultipleImageConstants.INTENT_EXTRA_SELECTED_PHOTOS);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);

            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(album);
        }

        permissionHint = (TextView) findViewById(R.id.text_view_permission_denied);
        permissionHint.setVisibility(View.INVISIBLE);

        progressBar = (ProgressBar) findViewById(R.id.progress_bar_image_select);
        gridView = (GridView) findViewById(R.id.grid_view_image_select);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (actionMode != null || getSelected().size() > 0)
                {
                    if (actionMode == null) {
                        actionMode = ZMultipleImageSelectActivity.this.startActionMode(callback);
                    }

                    toggleSelection(position);
                    actionMode.setTitle(countSelected + " " + getString(R.string.selected));
                    actionMode.invalidate();

                    if (countSelected == 0) {
                        actionMode.finish();
                    }
                }
                else
                {
                    toggleSelection(position);
                    sendIntent();
                }
            }
        });
        gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (actionMode == null) {
                    actionMode = ZMultipleImageSelectActivity.this.startActionMode(callback);
                }
                toggleSelection(position);
                actionMode.setTitle(countSelected + " " + getString(R.string.selected));
                actionMode.invalidate();

                if (countSelected == 0) {
                    actionMode.finish();
                }
                return true;
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

                        loadImages();

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
                        /*
                        If adapter is null, this implies that the loaded images will be shown
                        for the first time, hence send FETCH_COMPLETED message.
                        However, if adapter has been initialised, this thread was run either
                        due to the activity being restarted or content being changed.
                         */
                        if (adapter == null) {
                            adapter = new ZMultipleImageSelectAdapter(getApplicationContext(), images);
                            gridView.setAdapter(adapter);

                            progressBar.setVisibility(View.INVISIBLE);
                            gridView.setVisibility(View.VISIBLE);
                            orientationBasedUI(getResources().getConfiguration().orientation);

                        } else {
                            adapter.notifyDataSetChanged();
                            /*
                            Some selected images may have been deleted
                            hence update action mode title
                             */
                            if (actionMode != null) {
                                countSelected = msg.arg1;
                                actionMode.setTitle(countSelected + " " + getString(R.string.selected));
                            }
                        }
                        prepareActionModeForSelectedPhotos();
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
            public void onChange(boolean selfChange) {
                loadImages();
            }
        };
        getContentResolver().registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false, observer);

        requestPermission();
    }

    private void requestPermission() {
        if (ContextCompat.checkSelfPermission(ZMultipleImageSelectActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(ZMultipleImageSelectActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, ZMultipleImageConstants.PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
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
        images = null;
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
            int size = orientation == Configuration.ORIENTATION_PORTRAIT ? metrics.widthPixels / 3 : metrics.widthPixels / 5;
            adapter.setLayoutParams(size);
        }
        gridView.setNumColumns(orientation == Configuration.ORIENTATION_PORTRAIT ? 3 : 5);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_action_bar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.menu_item_add_multiple_image)
        {
            if (actionMode == null) {
                actionMode = ZMultipleImageSelectActivity.this.startActionMode(callback);
            }
            actionMode.setTitle(R.string.image_view);
            return true;
        }
        else if (id == android.R.id.home)
        {
            onBackPressed();
            return true;
        }
        return false;
    }

    private ActionMode.Callback callback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater menuInflater = mode.getMenuInflater();
            menuInflater.inflate(R.menu.menu_contextual_action_bar, menu);

            actionMode = mode;
            countSelected = 0;

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            if (countSelected > 0){
                MenuItem item = menu.findItem(R.id.menu_item_add_image);
                item.setVisible(true);
                return true;
            } else {
                MenuItem item = menu.findItem(R.id.menu_item_add_image);
                item.setVisible(false);
                return true;
            }
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {int i = item.getItemId();
            if (i == R.id.menu_item_add_image) {
                sendIntent();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            if (countSelected > 0) {
                deselectAll();
            }
            actionMode = null;
            onBackPressed();
        }
    };

    private void toggleSelection(int position) {
        if (!images.get(position).isSelected && countSelected >= ZMultipleImageConstants.limit) {
            Toast.makeText(getApplicationContext(), String.format(getString(R.string.limit_exceeded), ZMultipleImageConstants.limit), Toast.LENGTH_SHORT).show();
            return;
        }

        images.get(position).isSelected = !images.get(position).isSelected;
        if (images.get(position).isSelected) {
            countSelected++;
        } else {
            countSelected--;
        }
        adapter.notifyDataSetChanged();
    }

    private void deselectAll() {
        for (int i = 0, l = images.size(); i < l; i++) {
            images.get(i).isSelected = false;
        }
        countSelected = 0;
        adapter.notifyDataSetChanged();
    }

    private ArrayList<Photo> getSelected() {
        ArrayList<Photo> selectedImages = new ArrayList<Photo>();
        for (int i = 0, l = images.size(); i < l; i++) {
            if (images.get(i).isSelected) {
                selectedImages.add(images.get(i));
            }
        }
        return selectedImages;
    }

    private void sendIntent() {
        Intent intent = new Intent();
        intent.putParcelableArrayListExtra(ZMultipleImageConstants.INTENT_EXTRA_PHOTOS, getSelected());
        setResult(RESULT_OK, intent);
        finish();
    }

    private void loadImages() {
        abortLoading();

        ImageLoaderRunnable runnable = new ImageLoaderRunnable();
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

    private void prepareActionModeForSelectedPhotos()
    {
        if (actionMode == null) {
            actionMode = ZMultipleImageSelectActivity.this.startActionMode(callback);
        }
        actionMode.setTitle(album);
        actionMode.setSubtitle(R.string.image_view);

        if (selectedPhotos != null)
        {
            countSelected = 0;
            for (int position = 0; position < images.size(); position++) {
                if (images.get(position).isSelected)
                {
                    countSelected++;
                }
            }
            adapter.notifyDataSetChanged();
            actionMode.setTitle(countSelected + " " + getString(R.string.selected));
            actionMode.invalidate();

            if (countSelected == 0) {
                actionMode.finish();
            }
        }
    }

    private class ImageLoaderRunnable implements Runnable {
        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

            Message message;
            if (adapter == null) {
                message = handler.obtainMessage();
                /*
                If the adapter is null, this is first time this activity's view is
                being shown, hence send FETCH_STARTED message to show progress bar
                while images are loaded from phone
                 */
                message.what = ZMultipleImageConstants.FETCH_STARTED;
                message.sendToTarget();
            }

            if (Thread.interrupted()) {
                return;
            }

            File file;
            HashSet<Long> selectedImages = new HashSet<Long>();
            if (images != null) {
                Photo image;
                for (int i = 0, l = images.size(); i < l; i++) {
                    image = images.get(i);
                    file = new File(image.path);
                    if (file.exists() && image.isSelected) {
                        selectedImages.add(image.id);
                    }
                }
            }

            Cursor cursor;
            if (album.equalsIgnoreCase(getString(R.string.all_photos)))
            {
                cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection,
                        null, null, MediaStore.Images.Media.DATE_ADDED);
            }
            else
                cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection,
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME + " =?", new String[]{ album }, MediaStore.Images.Media.DATE_ADDED);

            /*
            In case this runnable is executed to onChange calling loadImages,
            using countSelected variable can result in a race condition. To avoid that,
            tempCountSelected keeps track of number of selected images. On handling
            FETCH_COMPLETED message, countSelected is assigned value of tempCountSelected.
             */
            int tempCountSelected = 0;
            ArrayList<Photo> temp = new ArrayList<Photo>(cursor.getCount());

            if (cursor.moveToLast()) {
                do {
                    if (Thread.interrupted()) {
                        return;
                    }

                    long id = cursor.getLong(cursor.getColumnIndex(projection[0]));
                    String name = cursor.getString(cursor.getColumnIndex(projection[1]));
                    String path = cursor.getString(cursor.getColumnIndex(projection[2]));
                    boolean isSelected = selectedImages.contains(id);
                    if (isSelected) {
                        tempCountSelected++;
                    }

                    file = new File(path);
                    if (file.exists()) {
                        temp.add(new Photo(id, name, path, isSelected));
                    }

                } while (cursor.moveToPrevious());
            }
            cursor.close();

            if (selectedPhotos != null)
            {
                for (Photo selectedPhoto: selectedPhotos) {
                    for (Photo photo: temp) {
                        if (photo.id == selectedPhoto.id && photo.path.equalsIgnoreCase(selectedPhoto.path)) {
                            photo.isSelected = true;
                        }
                    }
                }
            }

            if (images == null) {
                images = new ArrayList<Photo>();
            }
            images.clear();
            images.addAll(temp);

            message = handler.obtainMessage();
            message.what = ZMultipleImageConstants.FETCH_COMPLETED;
            message.arg1 = tempCountSelected;
            message.sendToTarget();

            Thread.interrupted();
        }
    }
}

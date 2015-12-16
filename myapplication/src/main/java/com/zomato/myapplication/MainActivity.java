package com.zomato.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;


import com.zomato.zmultipleimagelibrary.ZMultipleImageConstants;
import com.zomato.zmultipleimagelibrary.activities.ZMultipleImagePhotoAlbumSelectActivity;
import com.zomato.zmultipleimagelibrary.models.Photo;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private TextView textView;
    private ArrayList<Photo> selectedPhotos;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView) findViewById(R.id.text_view);

        Intent intent = new Intent(MainActivity.this, ZMultipleImagePhotoAlbumSelectActivity.class);
        intent.putExtra(ZMultipleImageConstants.INTENT_EXTRA_LIMIT, 5);
        startActivityForResult(intent, ZMultipleImageConstants.REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ZMultipleImageConstants.REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<Photo> images = data.getParcelableArrayListExtra(ZMultipleImageConstants.INTENT_EXTRA_PHOTOS);
            StringBuffer stringBuffer = new StringBuffer();
            for (int i = 0, l = images.size(); i < l; i++) {
                stringBuffer.append("id:" + images.get(i).id + " Path:" + images.get(i).path + "\n\n");
            }
            selectedPhotos = images;
            textView.setText(stringBuffer.toString());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            Intent intent = new Intent(MainActivity.this, ZMultipleImagePhotoAlbumSelectActivity.class);
            intent.putExtra(ZMultipleImageConstants.INTENT_EXTRA_LIMIT, 500);
            intent.putParcelableArrayListExtra(ZMultipleImageConstants.INTENT_EXTRA_SELECTED_PHOTOS, selectedPhotos);
            startActivityForResult(intent, ZMultipleImageConstants.REQUEST_CODE);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

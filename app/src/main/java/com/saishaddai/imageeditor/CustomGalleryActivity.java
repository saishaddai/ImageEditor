package com.saishaddai.imageeditor;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;

import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiscCache;
import com.nostra13.universalimageloader.cache.memory.impl.WeakMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;
import com.nostra13.universalimageloader.utils.StorageUtils;
import com.saishaddai.imageeditor.adapter.GalleryAdapter;


import java.io.File;
import java.util.ArrayList;
import java.util.Collections;


/**
 * This activity shows all the images in the device gallery to allow multiple choosing in older versions of Android
 * prior version 18.
 */
public class CustomGalleryActivity extends AppCompatActivity {

    private static final String TAG = CustomGalleryActivity.class.getSimpleName();
    GridView gridGallery;
    Handler handler;
    GalleryAdapter adapter;

    ImageView imgNoMedia;

    private ImageLoader imageLoader;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_gallery);
        initImageLoader();
        init();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.device_gallery, menu);
        return true;
    }

    private void initImageLoader() {
        String CACHE_DIR = Environment.getExternalStorageDirectory()
                .getAbsolutePath() + "/.temp_tmp";
        try {
            boolean dirsCreated = new File(CACHE_DIR).mkdirs();
            Log.v(TAG, "created structure for /.temp_tmp : " + dirsCreated);

            File cacheDir = StorageUtils.getOwnCacheDirectory(getBaseContext(),
                    CACHE_DIR);

            DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
                    .imageScaleType(ImageScaleType.EXACTLY)
                    .bitmapConfig(Bitmap.Config.RGB_565).build();
            ImageLoaderConfiguration.Builder builder = new ImageLoaderConfiguration.Builder(
                    getBaseContext())
                    .defaultDisplayImageOptions(defaultOptions)
                    .discCache(new UnlimitedDiscCache(cacheDir))
                    .memoryCache(new WeakMemoryCache());

            ImageLoaderConfiguration config = builder.build();
            imageLoader = ImageLoader.getInstance();
            imageLoader.init(config);

        } catch (Exception e) {
            Log.e(TAG, "error setting the images in app secure space: " + CACHE_DIR, e);
        }
    }

    private void init() {

        handler = new Handler();
        gridGallery = (GridView) findViewById(R.id.gridGallery);
        gridGallery.setFastScrollEnabled(true);
        adapter = new GalleryAdapter(getApplicationContext(), imageLoader);
        PauseOnScrollListener listener = new PauseOnScrollListener(imageLoader,
                true, true);
        gridGallery.setOnScrollListener(listener);

        gridGallery.setOnItemClickListener(mItemMulClickListener);
        adapter.setMultiplePick(true);

        gridGallery.setAdapter(adapter);
        imgNoMedia = (ImageView) findViewById(R.id.imgNoMedia);


        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        adapter.addAll(getGalleryPhotos());
                        checkImageStatus();
                    }
                });
                Looper.loop();
            }

        }.start();

    }

    // Setup action bar onclick events
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_done:
                ArrayList<CustomGallery> selected = adapter.getSelected();
                String[] imagePaths = new String[selected.size()];
                for (int i = 0; i < imagePaths.length; i++) {
                    imagePaths[i] = selected.get(i).sdcardPath;
                }
                Intent data = new Intent().putExtra("imagePaths", imagePaths);
                setResult(RESULT_OK, data);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkImageStatus() {
        if (adapter.isEmpty()) {
            imgNoMedia.setVisibility(View.VISIBLE);
        } else {
            imgNoMedia.setVisibility(View.GONE);
        }
    }

    AdapterView.OnItemClickListener mItemMulClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> l, View v, int position, long id) {
            adapter.changeSelection(v, position);

        }
    };

    private ArrayList<CustomGallery> getGalleryPhotos() {
        ArrayList<CustomGallery> galleryList = new ArrayList<>();

        try {
            final String[] columns = {MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media._ID};
            final String orderBy = MediaStore.Images.Media._ID;

            Cursor imageCursor = getContentResolver().query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns,
                    null, null, orderBy);

            if (imageCursor != null && imageCursor.getCount() > 0) {

                while (imageCursor.moveToNext()) {
                    CustomGallery item = new CustomGallery();

                    int dataColumnIndex = imageCursor
                            .getColumnIndex(MediaStore.Images.Media.DATA);

                    item.sdcardPath = imageCursor.getString(dataColumnIndex);

                    if(!item.sdcardPath.contains(BuildConfig.APPLICATION_ID))
                        galleryList.add(item);

                }
            }
            if (imageCursor != null) {
                imageCursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Check your permissions for read internal/external memory", e);
        }

        // show newest photo at beginning of the list
        Collections.reverse(galleryList);
        return galleryList;
    }

    public class CustomGallery {
        public String sdcardPath;
        public boolean isSelected = false;

    }

}
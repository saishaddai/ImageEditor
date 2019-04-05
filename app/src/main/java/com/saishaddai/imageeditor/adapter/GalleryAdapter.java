package com.saishaddai.imageeditor.adapter;

import java.util.ArrayList;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;
import com.saishaddai.imageeditor.CustomGalleryActivity;
import com.saishaddai.imageeditor.R;

public class GalleryAdapter extends BaseAdapter {

    private static final String TAG = GalleryAdapter.class.getSimpleName();
    private LayoutInflater inflater;
    private ArrayList<CustomGalleryActivity.CustomGallery> data = new ArrayList<>();
    ImageLoader imageLoader;

    private boolean isActionMultiplePick;

    public GalleryAdapter(Context context, ImageLoader imageLoader) {
        inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.imageLoader = imageLoader;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public CustomGalleryActivity.CustomGallery getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setMultiplePick(boolean isMultiplePick) {
        this.isActionMultiplePick = isMultiplePick;
    }

    public ArrayList<CustomGalleryActivity.CustomGallery> getSelected() {
        ArrayList<CustomGalleryActivity.CustomGallery> dataT = new ArrayList<>();

        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).isSelected) {
                dataT.add(data.get(i));
            }
        }

        return dataT;
    }

    public void addAll(ArrayList<CustomGalleryActivity.CustomGallery> files) {

        try {
            this.data.clear();
            this.data.addAll(files);

        } catch (Exception e) {
            Log.e(TAG, "error adding all the images", e);
        }

        notifyDataSetChanged();
    }

    public void changeSelection(View v, int position) {
        data.get(position).isSelected = !data.get(position).isSelected;
        ((ViewHolder) v.getTag()).imgQueueMultiSelected.setSelected(data
                .get(position).isSelected);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final ViewHolder holder;
        if (convertView == null) {

            convertView = inflater.inflate(R.layout.gallery_item, null);
            holder = new ViewHolder();
            holder.imgQueue = (ImageView) convertView
                    .findViewById(R.id.imgQueue);

            holder.imgQueueMultiSelected = (ImageView) convertView
                    .findViewById(R.id.imgQueueMultiSelected);

            if (isActionMultiplePick) {
                holder.imgQueueMultiSelected.setVisibility(View.VISIBLE);
            } else {
                holder.imgQueueMultiSelected.setVisibility(View.GONE);
            }

            convertView.setTag(holder);

        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.imgQueue.setTag(position);

        try {
            imageLoader.displayImage("file://" + data.get(position).sdcardPath,
                    holder.imgQueue, new SimpleImageLoadingListener() {
                        @Override
                        public void onLoadingStarted(String imageUri, View view) {
                            holder.imgQueue
                                    .setImageResource(R.drawable.no_media);
                            super.onLoadingStarted(imageUri, view);
                        }
                    });

            if (isActionMultiplePick) {
                holder.imgQueueMultiSelected
                        .setSelected(data.get(position).isSelected);

            }

        } catch (Exception e) {
            Log.e(TAG, "error getting the image from device", e);
        }

        return convertView;
    }

    public class ViewHolder {
        ImageView imgQueue;
        ImageView imgQueueMultiSelected;
    }

    public void clear() {
        data.clear();
        notifyDataSetChanged();
    }

}
package com.saishaddai.imageeditor.model;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;

public class Image {

    private static final String TAG = Image.class.getName();
    public String deviceUrl; // Audit transaction ID + '_' + Image#.jpg
    public String remoteUrl; // http://URL + /Composite ID/PRODUCT/Image#.jpg
    public String path; // Composite id /PRODUCT/image#.jpg
    transient String imageType; // PRODUCT or CONTAINER
    transient Bitmap imageBitmap;
    public boolean submitted;

    public Bitmap getImageBitmap() {
        return imageBitmap;
    }

    public void setImageBitmap(Bitmap imageBitmap) {
        this.imageBitmap = imageBitmap;
    }

    public Image() {
    }

    public String getDeviceUrl() {
        return deviceUrl;
    }

    public boolean getSubmitted() {
        return this.submitted;
    }

    public void setSubmitted(boolean submitted) {
        this.submitted = submitted;
    }

    public void setDeviceUrl(String deviceUrl) {
        this.deviceUrl = deviceUrl;
    }

    public String getPath() {
        return this.path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getRemoteUrl() {
        return remoteUrl;
    }

    public void setRemoteUrl(String remoteUrl) {
        this.remoteUrl = remoteUrl;
    }

    public void setImageType(String imageType) {
        this.imageType = imageType;
    }


    public Bitmap getImageFromDeviceUrl(int sampleSize, Context context) {
        if (deviceUrl == null)
            return null;
        try {
            File file;
            deviceUrl = deviceUrl.substring(deviceUrl.lastIndexOf("/") + 1);
            file = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), deviceUrl);
            FileInputStream streamIn = new FileInputStream(file);
            // ByteArrayOutputStream stream = new ByteArrayOutputStream();
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPurgeable = true;
            options.inInputShareable = true;
            options.inDither = true;
            options.inSampleSize = sampleSize;
            imageBitmap = BitmapFactory.decodeStream(streamIn, null, options);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "deviceUrl: " + deviceUrl, e);
        }
        return imageBitmap;
        // bmp.compress(Bitmap.CompressFormat.JPEG, 80, stream);
        // int size = bmp.getByteCount()/1024;
        // byteArray = stream.toByteArray();
    }

    public String getBase64EncodedImageFromDevice(Context context) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Bitmap bitmap = getImageFromDeviceUrl(1 /*4*/, context);
        if (bitmap != null) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            return Base64.encodeToString(byteArray, Base64.DEFAULT);
            // Log.v(TAG,encoded);
            // System.out.println("IMAGE IS --------------     " +encoded );
        } else
            return null;
    }

    public boolean saveImageToDeviceUrl(Context context) {
        File path = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File file = new File(path, deviceUrl + ".jpg");
        if (file.exists()) {
            file.delete();
            try {
                file.createNewFile();
            } catch (IOException e) {
                Log.e(TAG, "path: " + path + ", deviceUrl: " + deviceUrl, e);
            }
        } else {
            try {
                file.createNewFile();
            } catch (IOException e) {
                Log.e(TAG, "file: " + file, e);
            }
        }
        FileOutputStream fileOutputStream;
        try {
            fileOutputStream = new FileOutputStream(file.getPath());
            BufferedOutputStream bos = new BufferedOutputStream(fileOutputStream);
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "filePath: " + file.getPath(), e);
        }
        return true;
    }

    public void deleteImageFromDevice(Context context) {
        try {
            File path = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (path != null) {
                File file = new File(path, this.deviceUrl);
                file.delete();
            }
        } catch (Exception e) {
            Log.e(TAG, "deviceUrl: " + this.deviceUrl, e);
        }
    }
}


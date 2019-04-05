package com.saishaddai.imageeditor.utils;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

public class DataUtils {

    private static final String TAG = DataUtils.class.getName();

    public static List<Integer> parseArrayOfNumbers(String arrayString) {
        if (arrayString == null || arrayString.length() == 0) {
            return new ArrayList<>();
        }
        List<String> items = Arrays.asList(arrayString
                .replaceAll("\\[", "")
                .replaceAll("\\]", "")
                .split("\\s*,\\s*"));
        List<Integer> results = new ArrayList<>();
        for (String item : items) {
            try {
                if (!item.isEmpty())
                    results.add(Integer.valueOf(item));
            } catch (NumberFormatException e) {
                Log.e(TAG, "arrayString: " + arrayString + ", item: " + item, e);
            }
        }
        return results;
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public static List<String> parseArrayOfString(String arrayString) {
        if (arrayString == null || arrayString.length() == 0)
            return new ArrayList<>();
        String[] items = arrayString.substring(1, arrayString.length() - 1).split(",");
        List<String> results = new ArrayList<>();
        for (String item : items) {
            try {
                item = item.replace("\"", "");
                results.add(item);
            } catch (Exception e) {
                Log.e(TAG, "arrayString: " + arrayString + ", item: " + item, e);
            }
        }
        return results;
    }

    static String parseArrayOfStringAsString(String arrayString) {
        if (arrayString == null || arrayString.length() == 0)
            return "";
        String[] items = arrayString.substring(1, arrayString.length() - 1).split(",");
        StringBuilder results = new StringBuilder();
        for (String item : items) {
            try {
                item = item.replace("\"", "");
                if (!item.isEmpty()) {
                    results.append("'");
                    results.append(item.trim());
                    results.append("',");
                }
            } catch (Exception e) {
                Log.e(TAG, "arrayString: " + arrayString + ", item: " + item, e);
            }
        }
        String arrayAsStringQuery = results.toString();
        if (arrayAsStringQuery.isEmpty())
            return "";

        return arrayAsStringQuery.substring(0, arrayAsStringQuery.length() - 1);
    }

    public static String convertIntArrayToString(int[] array) {
        if (array == null || array.length == 0)
            return "[]";
        StringBuilder str = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            if (i > 0) str.append(",");
            str.append(array[i]);
        }
        str.append("]");
        return str.toString();
    }

    public static String convertArrayToString(String[] array) {
        StringBuilder sb = new StringBuilder();
        for (String n : array) {
            if (sb.length() > 0)
                sb.append(',');
            sb.append("'").append(n).append("'");
        }
        return sb.toString();
    }

    public static String convertArrayToStringNoFormat(String[] array) {
        StringBuilder builder = new StringBuilder();
        for (String n : array) {
            if (builder.length() > 0)
                builder.append(',');
            builder.append(n);
        }
        return builder.toString();
    }

    public static boolean getStringAsBoolean(String booleanString) {
        return booleanString != null && booleanString.equalsIgnoreCase("TRUE");
    }

    public static boolean isJSONObject(String test) {
        try {
            new JSONObject(test);
        } catch (JSONException ex) {
            return false;
        }
        return true;
    }

    public static boolean isJsonArray(String response) {
        if (response.charAt(0) == '<')
            return false;
        Gson gson = new Gson();
        try {
            JsonElement element = gson.fromJson(response, JsonElement.class);
            if (element.isJsonArray())
                return true;
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "response: " + response, e);
            return false;
        }
        return false;
    }

    public static boolean isEmptyJsonArray(String response) {
        if (response.charAt(0) == '<')
            return false;
        Gson gson = new Gson();
        try {
            JsonElement element = gson.fromJson(response, JsonElement.class);
            if (element.isJsonArray()) {
                JsonArray jArray = element.getAsJsonArray();
                if (jArray.size() == 0)
                    return true;
            }
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "response: " + response, e);
            return false;
        }
        return false;
    }

    public static String parseAuditMasterIDFromCompositeId(String compositeID) {
        int first = compositeID.indexOf('-');
        int second = compositeID.indexOf('-', first + 1);
        return compositeID.substring(first + 1, second);
    }

    public static String parseAuditGroupIDFromCompositeId(String compositeID) {
        int first = compositeID.indexOf('-');
        int second = compositeID.indexOf('-', first + 1);
        int third = compositeID.indexOf('-', second + 1);
        return compositeID.substring(second + 1, third);
    }

    public static double convertMetersToMiles(double meters) {
        if (meters != 0)
            return meters * 0.00062137119;
        else
            return 0;
    }

    public static String ellipsizeRatingName(String text) {
        if (text != null) {
            if (text.length() > 0) {
                int length = text.length();
                if (length > 20)
                    return text.substring(0, 20) + "…";
                else
                    return text;
            }
        }
        return text;
    }


    public static String ellipsizeByMaxCharacters(String text, int maxCharacters) {
        if (text != null && !text.isEmpty() && maxCharacters > 0) {
            if (text.length() > maxCharacters) {
                return text.substring(0, maxCharacters) + "…";
            } else {
                return text;
            }
        }
        return text;
    }

    //Not used anymore, please remove soon 12/15/2017
//    public static int getImageOrientation(String imagePath) {
//        int rotate = 0;
//        try {
//            File imageFile = new File(imagePath);
//            ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
//            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
//                    ExifInterface.ORIENTATION_NORMAL);
//            switch (orientation) {
//                case ExifInterface.ORIENTATION_ROTATE_270:
//                    rotate = 270;
//                    break;
//                case ExifInterface.ORIENTATION_ROTATE_180:
//                    rotate = 180;
//                    break;
//                case ExifInterface.ORIENTATION_ROTATE_90:
//                    rotate = 90;
//                    break;
//            }
//        } catch (IOException e) {
//            Log.e(TAG, "imagePath: " + imagePath, e);
//        }
//        return rotate;
//    }

    //Copy file used for adding images from outside sources into an inspection
    public static boolean copyFile(File sourceFile, File destFile) throws IOException {
        boolean success = false;
        if (!sourceFile.exists()) {
            return false;
        }
        FileChannel source = new FileInputStream(sourceFile).getChannel();
        FileChannel destination = new FileOutputStream(destFile).getChannel();
        if (source != null) {
            long bytesMoved = destination.transferFrom(source, 0, source.size());
            if (bytesMoved > 0) {
                success = true;
            }
        }
        if (source != null) {
            source.close();
        }
        destination.close();
        return success;
    }

    /**
     * Compares two version strings.
     * <p/>
     * Use this instead of String.compareTo() for a non-lexicographical comparison that works for version strings. e.g.
     * "1.10".compareTo("1.6").
     *
     * @param str1 a string of ordinal numbers separated by decimal points.
     * @param str2 a string of ordinal numbers separated by decimal points.
     * @return The result is a negative integer if str1 is _numerically_ less than str2. The result is a positive
     * integer if str1 is _numerically_ greater than str2. The result is zero if the strings are _numerically_
     * equal.
     * note It does not work if "1.10" is supposed to be equal to "1.10.0".
     */
    public static Integer versionCompare(String str1, String str2) {
        String[] values1 = str1.split("\\.");
        String[] values2 = str2.split("\\.");
        int i = 0;
        // set index to first non-equal ordinal or length of shortest version string
        while (i < values1.length && i < values2.length && values1[i].equals(values2[i])) {
            i++;
        }
        // compare first non-equal ordinal number
        if (i < values1.length && i < values2.length) {
            int diff = Integer.valueOf(values1[i]).compareTo(Integer.valueOf(values2[i]));
            return Integer.signum(diff);
        }
        // the strings are equal or one string is a substring of the other
        // e.g. "1.2.3" = "1.2.3" or "1.2.3" < "1.2.3.4"
        else {
            return Integer.signum(values1.length - values2.length);
        }
    }

    public static Bitmap RotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix,
                true);
    }

    public static Bitmap cropToSquare(Bitmap bitmap) {
        //get original width, height
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int newValueOfWidthOrHeight, newHeight, cropWidth, cropHeight;
        //set width equal to longest edge of image
        if (height > width) newValueOfWidthOrHeight = width;
        else newValueOfWidthOrHeight = height;

        //set height equal to width to make 1:1 aspect ratio
        if (height > width) newHeight = height - (height - width);
        else newHeight = height;

        //set point where to crop from original image equal to halfway down both sides
        cropWidth = (width - height) / 2;
        if (cropWidth < 0) cropWidth = 0;

        cropHeight = (height - width) / 2;
        if (cropHeight < 0) cropHeight = 0;

        //create new bitmap from crop values
        Bitmap cropImage = Bitmap.createBitmap(bitmap, cropWidth, cropHeight, newValueOfWidthOrHeight, newHeight);
        bitmap.recycle();
        return cropImage;
    }

    public static int calculateInSampleSize(
            int imageWidth, int imageHeight, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = imageWidth;
        final int width = imageHeight;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            // Calculate ratios of height and width to requested height and width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }

        return inSampleSize;
    }

    public static String parseSetAsStringListOfValues(Set<String> setOfStrings) {
        if (setOfStrings == null || setOfStrings.size() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String token : setOfStrings) {
            if (token != null) {
                sb.append(token);
                sb.append("\n");
            }
        }
        String result = sb.toString();
        return result.substring(0, result.length() - 1);


    }

    public static String parseListAsCommaSeparatedValues(List<String> stringList) {
        if (stringList == null || stringList.size() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String token : stringList) {
            if (token != null) {
                sb.append(token.replace("\"", ""));
                sb.append(", ");
            }
        }
        String result = sb.toString();
        return result.substring(0, result.length() - 2);

    }

    public static long getDaysAgoAsLong(int daysAgo) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR) - daysAgo);
        return calendar.getTimeInMillis();
    }

    //This method removes the decimal point and decimal values to allow the parsing as a integer
    public static String cleanStringToParseAsInt(String rawString) {
        String defaultString = "0";
        String resultString;
        if (rawString == null)
            return defaultString;

        if (rawString.contains("."))
            resultString = rawString.substring(0, rawString.indexOf("."));
        else
            resultString = rawString;

        return resultString.isEmpty() ? "0" : resultString;
    }


    public static String getTimeZoneHeader() {
        TimeZone tz = TimeZone.getDefault();
        Calendar cal = GregorianCalendar.getInstance(tz);
        int offsetInMillis = tz.getOffset(cal.getTimeInMillis());

        String offset = String.format(Locale.getDefault(), "%02d:%02d", Math.abs(offsetInMillis / 3600000), Math.abs((offsetInMillis / 60000) % 60));
        offset = (offsetInMillis >= 0 ? "+" : "-") + offset;

        return offset;
    }

    public static String millisecondsToSeconds(double milliseconds) {
        return String.format(Locale.getDefault(), "%.2f", milliseconds);
    }

    public static String[] serializeRatings(JsonElement content) {
        String contentString = new Gson().toJson(content);
        Gson gson = new Gson();
        JsonElement element = gson.fromJson(contentString, JsonElement.class);
        JsonObject jsonObj = element.getAsJsonObject();
        JsonArray jArray = jsonObj.getAsJsonArray("combo_items");
        String[] options = new String[jArray.size()];
        for (int i = 0; i < jArray.size(); i++) {
            JsonElement jsonElement = jArray.get(i);
            options[i] = jsonElement.getAsString();
        }
        return options;
    }
}


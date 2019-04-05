package com.saishaddai.imageeditor;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;


import com.saishaddai.imageeditor.utils.DataUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Stack;

/*
 *
 *  Annotation
 *  	Circle
 *  	Rectangle
 *  	Text
 *  	Paint
 *
 *
 */
public class ImageEditorActivity extends AppCompatActivity {
    private ProgressDialog asyncDialog;
    private String deviceUrl;
    private Bitmap imageBitmap;
    private int width, height;
    private int originalWidth, originalHeight;
    private RelativeLayout mainLayout;
    private final int INITIAL_COLOR = Color.RED;
    private final int SMALL_FONT_SIZE = 24;
    private final int MEDIUM_FONT_SIZE = 48;
    private final int LARGE_FONT_SIZE = 72;
    private ImageEditorView imageEditorView;
    private InputMethodManager imm;
    private final String TAG = this.getClass().getSimpleName();

    // holds current tool
    private AnnotationType selectedTool = AnnotationType.Paint;
    private int selectedColor = INITIAL_COLOR;
    private int selectedFontSize = MEDIUM_FONT_SIZE;
    // expand/collapse menus
    private ToolState toolState = ToolState.Select;
    private ColorState colorState = ColorState.Select;
    // each tool is a button
    private Button paintBrushButton, circleButton, textButton, arrowButton, rectangleButton, colorButton,
            colorButtonRed, colorButtonOrange, colorButtonYellow, colorButtonGreen,
            colorButtonBlue, colorButtonPurple, colorButtonWhite, colorButtonGray,
            colorButtonBlack;
    private ImageButton fontButton, fontButtonSmall, fontButtonMedium, fontButtonLarge;
    private EditText editTextWidget;
    private boolean saveRequired = false;
    private boolean saveInProgress = false;

    private enum AnnotationType {
        None, Paint, Text, Circle, Arrow, Rectangle
    }

    private enum ToolState {
        Select, Work
    }

    private enum ColorState {
        Select, Work
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_editor);

        if (getIntent().getStringExtra("path") != null) {
            deviceUrl = getIntent().getStringExtra("path");
            deviceUrl = deviceUrl.substring(deviceUrl.lastIndexOf("/") + 1);
        }
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        width = displaymetrics.widthPixels;
        int delta = 0;
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.image_editor_title);
            delta = getSupportActionBar().getHeight();
        }
        height = displaymetrics.heightPixels - delta;
        // load linear layout
        mainLayout = findViewById(R.id.editorLayout);
        imm = (InputMethodManager) ImageEditorActivity.this
                .getSystemService(Service.INPUT_METHOD_SERVICE);
        new LoadImageTask().execute();
    }

    private void confirmFinishActivity() {
        AlertDialog.Builder builder = new AlertDialog.Builder(ImageEditorActivity.this);
        builder.setTitle(R.string.image_editor_dialog_title);
        builder.setMessage(R.string.image_editor_dialog_message);
        builder.setPositiveButton(R.string.common_button_ok,
                new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                });
        builder.setNegativeButton(R.string.common_button_cancel,
                new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onBackPressed() {
        confirmFinishActivity();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.image_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_undo:
                if (imageEditorView != null) {
                    imageEditorView.requestUndo = true;
                }
                return true;
            case R.id.action_cancel:
                onBackPressed();
                return true;
            case R.id.action_save:
                saveEditor();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveEditor() {
        saveRequired = true;
        saveInProgress = true;
        new SaveImageTask().execute();
        // imageEditorView.saveToDeviceUrl();
    }

    private class LoadImageTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            asyncDialog = new ProgressDialog(ImageEditorActivity.this);
            asyncDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            asyncDialog.setMessage(getString(R.string.image_editor_load_image_loader));
            asyncDialog.setCanceledOnTouchOutside(false);
            asyncDialog.show();
        }

        private Bitmap getQualityImage(String path) {
            try {
                File file = new File(getApplicationContext().getExternalFilesDir(
                        Environment.DIRECTORY_PICTURES), path);
                FileInputStream streamIn = new FileInputStream(file);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPurgeable = true;
                options.inInputShareable = true;
                options.inDither = true;
                options.inSampleSize = 1;
                return BitmapFactory.decodeStream(streamIn, null, options);
            } catch (FileNotFoundException e) {
                Log.e(TAG, e.getMessage());
            }
            return null;
        }

        @Override
        protected Boolean doInBackground(Void... v) {


            imageBitmap = getQualityImage(deviceUrl);
            if (imageBitmap != null) {

                originalHeight = imageBitmap.getHeight();
                originalWidth = imageBitmap.getWidth();
                if (originalWidth > originalHeight) {
                    imageBitmap = DataUtils.RotateBitmap(imageBitmap, 90);
                    originalHeight = imageBitmap.getHeight();
                    originalWidth = imageBitmap.getWidth();
                }

            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {

            if (imageBitmap == null) {
                if (asyncDialog != null && asyncDialog.isShowing()) {
                    try {
                        asyncDialog.cancel();
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage());
                    }
                }
                finish();
            }
            imageEditorView = new ImageEditorView(ImageEditorActivity.this);
            mainLayout.addView(imageEditorView);
            setupViews();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (imageEditorView != null)
            imageEditorView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (imageEditorView != null)
            imageEditorView.pause();

        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (getCurrentFocus() != null && imm != null) {
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

    }

    // Load buttons for toolbar overlay
    private void setupViews() {
        editTextWidget = new EditText(ImageEditorActivity.this);
        editTextWidget.setBackground(getResources().getDrawable(R.drawable.bg_input));
        editTextWidget.setVisibility(View.GONE);
        editTextWidget.setImeOptions(EditorInfo.IME_ACTION_DONE);
        editTextWidget.setSingleLine(true);
        editTextWidget.setTextSize(selectedFontSize);
        editTextWidget.setHint(R.string.image_editor_text_widget_hint);
        editTextWidget.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    imageEditorView.applyTextToBitmap();
                }
                return false;
            }
        });
        mainLayout.addView(editTextWidget);
        RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(100, 100);
        paintBrushButton = new Button(ImageEditorActivity.this);
        paintBrushButton.setText("");
        paintBrushButton.setId(R.id.editorPaint);
        // paintBrushButton.setBackgroundColor(getResources().getColor(R.color.WhiteSmoke));
        paintBrushButton.setBackgroundResource(R.drawable.editor_pencil);
        paintBrushButton.getBackground().setAlpha(220);// 75% transparent
        paintBrushButton.setTextColor(getResources().getColor(R.color.white));
        paintBrushButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedTool = AnnotationType.Paint;
                toggleToolState();
                updateButtons();
            }
        });
        p.leftMargin = 10;
        mainLayout.addView(paintBrushButton, p);
        p = new RelativeLayout.LayoutParams(100, 100);
        textButton = new Button(ImageEditorActivity.this);
        textButton.setText("");
        textButton.setId(R.id.editorText);
        textButton.setTextColor(getResources().getColor(R.color.white));
        textButton.setBackgroundResource(R.drawable.editor_text);
        textButton.getBackground().setAlpha(220);// 75% transparent
        textButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedTool = AnnotationType.Text;
                toggleToolState();
                updateButtons();
                toggleFontSizeButtons();
            }
        });

        p.addRule(RelativeLayout.BELOW, paintBrushButton.getId());
        p.topMargin = 10;
        p.leftMargin = 10;
        mainLayout.addView(textButton, p);
        p = new RelativeLayout.LayoutParams(100, 100);
        rectangleButton = new Button(ImageEditorActivity.this);
        rectangleButton.setText("");
        rectangleButton.setId(R.id.editorRectangle);
        rectangleButton.setTextColor(getResources().getColor(R.color.white));
        rectangleButton.setBackgroundResource(R.drawable.editor_rectangle);
        rectangleButton.getBackground().setAlpha(220);// 75% transparent
        rectangleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedTool = AnnotationType.Rectangle;
                toggleToolState();
                updateButtons();
            }
        });
        p.topMargin = 10;
        p.leftMargin = 10;
        p.addRule(RelativeLayout.BELOW, textButton.getId());
        mainLayout.addView(rectangleButton, p);
        p = new RelativeLayout.LayoutParams(100, 100);
        circleButton = new Button(ImageEditorActivity.this);
        circleButton.setText("");
        circleButton.setId(R.id.editorCircle);
        circleButton.setTextColor(getResources().getColor(R.color.white));
        circleButton.setBackgroundResource(R.drawable.editor_circle);
        circleButton.getBackground().setAlpha(220);// 75% transparent
        circleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedTool = AnnotationType.Circle;
                toggleToolState();
                updateButtons();
            }
        });
        p.topMargin = 10;
        p.leftMargin = 10;
        p.addRule(RelativeLayout.BELOW, rectangleButton.getId());
        mainLayout.addView(circleButton, p);
        p = new RelativeLayout.LayoutParams(100, 100);
        arrowButton = new Button(ImageEditorActivity.this);
        arrowButton.setText("");
        arrowButton.setId(R.id.editorArrow);
        arrowButton.setTextColor(getResources().getColor(R.color.white));
        arrowButton.setBackgroundResource(R.drawable.editor_arrow);
        arrowButton.getBackground().setAlpha(220);// 75% transparent
        arrowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedTool = AnnotationType.Arrow;
                toggleToolState();
                updateButtons();
            }
        });
        p.addRule(RelativeLayout.BELOW, circleButton.getId());
        p.topMargin = 10;
        p.leftMargin = 10;
        mainLayout.addView(arrowButton, p);
        // color picker
        p = new RelativeLayout.LayoutParams(100, 100);
        colorButton = new Button(ImageEditorActivity.this);
        colorButton.setText("");
        colorButton.setId(R.id.editorColor);
        colorButton.setBackgroundColor(selectedColor);
        colorButton.getBackground().setAlpha(192);// 75% transparent
        colorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                colorButton.setBackgroundColor(selectedColor);
                toggleColorState();
                updateButtons();
            }
        });
        p.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        p.topMargin = 10;
        p.rightMargin = 10;
        mainLayout.addView(colorButton, p);
        // red
        p = new RelativeLayout.LayoutParams(75, 75);
        colorButtonRed = new Button(ImageEditorActivity.this);
        colorButtonRed.setText("");
        colorButtonRed.setId(R.id.editorColorRed);
        colorButtonRed.setBackgroundColor(Color.RED);
        colorButtonRed.getBackground().setAlpha(192);// 75% transparent
        colorButtonRed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedColor = Color.RED;
                imageEditorView.updateColor(selectedColor);
                updateButtons();
            }
        });
        p.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        p.addRule(RelativeLayout.BELOW, colorButton.getId());
        p.topMargin = 10;
        p.rightMargin = 10;
        mainLayout.addView(colorButtonRed, p);
        // orange
        p = new RelativeLayout.LayoutParams(75, 75);
        colorButtonOrange = new Button(ImageEditorActivity.this);
        colorButtonOrange.setText("");
        colorButtonOrange.setId(R.id.editorColorOrange);
        colorButtonOrange.setBackgroundColor(getResources().getColor(R.color.Orange));
        colorButtonOrange.getBackground().setAlpha(192);// 75% transparent
        colorButtonOrange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedColor = getResources().getColor(R.color.Orange);
                imageEditorView.updateColor(selectedColor);
                updateButtons();
            }
        });
        p.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        p.addRule(RelativeLayout.BELOW, colorButtonRed.getId());
        p.topMargin = 10;
        p.rightMargin = 10;
        mainLayout.addView(colorButtonOrange, p);
        // yellow
        p = new RelativeLayout.LayoutParams(75, 75);
        colorButtonYellow = new Button(ImageEditorActivity.this);
        colorButtonYellow.setText("");
        colorButtonYellow.setId(R.id.editorColorYellow);
        colorButtonYellow.setBackgroundColor(Color.YELLOW);
        colorButtonYellow.getBackground().setAlpha(192);// 75% transparent
        colorButtonYellow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedColor = Color.YELLOW;
                imageEditorView.updateColor(selectedColor);
                updateButtons();
            }
        });
        p.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        p.addRule(RelativeLayout.BELOW, colorButtonOrange.getId());
        p.topMargin = 10;
        p.rightMargin = 10;
        mainLayout.addView(colorButtonYellow, p);
        // green
        p = new RelativeLayout.LayoutParams(75, 75);
        colorButtonGreen = new Button(ImageEditorActivity.this);
        colorButtonGreen.setText("");
        colorButtonGreen.setId(R.id.editorColorGreen);
        colorButtonGreen.setBackgroundColor(Color.GREEN);
        colorButtonGreen.getBackground().setAlpha(192);// 75% transparent
        colorButtonGreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedColor = Color.GREEN;
                imageEditorView.updateColor(selectedColor);
                updateButtons();
            }
        });
        p.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        p.addRule(RelativeLayout.BELOW, colorButtonYellow.getId());
        p.topMargin = 10;
        p.rightMargin = 10;
        mainLayout.addView(colorButtonGreen, p);
        // blue
        p = new RelativeLayout.LayoutParams(75, 75);
        colorButtonBlue = new Button(ImageEditorActivity.this);
        colorButtonBlue.setText("");
        colorButtonBlue.setId(R.id.editorColorBlue);
        colorButtonBlue.setBackgroundColor(Color.BLUE);
        colorButtonBlue.getBackground().setAlpha(192);// 75% transparent
        colorButtonBlue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedColor = Color.BLUE;
                imageEditorView.updateColor(selectedColor);
                updateButtons();
            }
        });
        p.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        p.addRule(RelativeLayout.BELOW, colorButtonGreen.getId());
        p.topMargin = 10;
        p.rightMargin = 10;
        mainLayout.addView(colorButtonBlue, p);
        // purple
        p = new RelativeLayout.LayoutParams(75, 75);
        colorButtonPurple = new Button(ImageEditorActivity.this);
        colorButtonPurple.setText("");
        colorButtonPurple.setId(R.id.editorColorPurple);
        colorButtonPurple.setBackgroundColor(getResources().getColor(R.color.purple));
        colorButtonPurple.getBackground().setAlpha(192);// 75% transparent
        colorButtonPurple.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedColor = getResources().getColor(R.color.purple);
                imageEditorView.updateColor(selectedColor);
                updateButtons();
            }
        });
        p.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        p.addRule(RelativeLayout.BELOW, colorButtonBlue.getId());
        p.topMargin = 10;
        p.rightMargin = 10;
        mainLayout.addView(colorButtonPurple, p);
        // white
        p = new RelativeLayout.LayoutParams(75, 75);
        colorButtonWhite = new Button(ImageEditorActivity.this);
        colorButtonWhite.setText("");
        colorButtonWhite.setId(R.id.editorColorWhite);
        colorButtonWhite.setBackgroundColor(Color.WHITE);
        colorButtonWhite.getBackground().setAlpha(192);// 75% transparent
        colorButtonWhite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedColor = Color.WHITE;
                imageEditorView.updateColor(selectedColor);
                updateButtons();
            }
        });
        p.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        p.addRule(RelativeLayout.BELOW, colorButtonPurple.getId());
        p.topMargin = 10;
        p.rightMargin = 10;
        mainLayout.addView(colorButtonWhite, p);
        // gray
        p = new RelativeLayout.LayoutParams(75, 75);
        colorButtonGray = new Button(ImageEditorActivity.this);
        colorButtonGray.setText("");
        colorButtonGray.setId(R.id.editorColorGray);
        colorButtonGray.setBackgroundColor(getResources().getColor(R.color.gray));
        colorButtonGray.getBackground().setAlpha(192);// 75% transparent
        colorButtonGray.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedColor = getResources().getColor(R.color.gray);
                imageEditorView.updateColor(selectedColor);
                updateButtons();
            }
        });
        p.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        p.addRule(RelativeLayout.BELOW, colorButtonWhite.getId());
        p.topMargin = 10;
        p.rightMargin = 10;
        mainLayout.addView(colorButtonGray, p);
        // black
        p = new RelativeLayout.LayoutParams(75, 75);
        colorButtonBlack = new Button(ImageEditorActivity.this);
        colorButtonBlack.setText("");
        colorButtonBlack.setId(R.id.editorColorBlack);
        colorButtonBlack.setBackgroundColor(Color.BLACK);
        colorButtonBlack.getBackground().setAlpha(192);// 75% transparent
        colorButtonBlack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedColor = Color.BLACK;
                imageEditorView.updateColor(selectedColor);
                updateButtons();
            }
        });
        p.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        p.addRule(RelativeLayout.BELOW, colorButtonGray.getId());
        p.topMargin = 10;
        p.rightMargin = 10;
        mainLayout.addView(colorButtonBlack, p);

        //Font size buttons
        p = new RelativeLayout.LayoutParams(100, 100);
        fontButton = new ImageButton(ImageEditorActivity.this);
        fontButton.setLayoutParams(new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
        fontButton.setId(R.id.fontSize);
        fontButton.setImageDrawable(getResources().getDrawable(R.drawable.medium_font_alpha_selected));
        fontButton.setBackgroundColor(Color.WHITE);
        fontButton.getBackground().setAlpha(192);// 75% transparent
        fontButton.setClickable(false);
        fontButton.setVisibility(View.INVISIBLE);
        p.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        p.addRule(RelativeLayout.BELOW, textButton.getId());
        p.topMargin = 10;
        p.leftMargin = 10;
        mainLayout.addView(fontButton, p);

        //Small Font size button
        p = new RelativeLayout.LayoutParams(75, 75);
        fontButtonSmall = new ImageButton(ImageEditorActivity.this);
        fontButtonSmall.setLayoutParams(new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
        fontButtonSmall.setImageDrawable(getResources().getDrawable(R.drawable.small_font_alpha));
        fontButtonSmall.setId(R.id.fontSizeSmall);
        fontButtonSmall.setBackgroundColor(Color.WHITE);
        fontButtonSmall.getBackground().setAlpha(192);// 75% transparent
        fontButtonSmall.setVisibility(View.INVISIBLE);
        fontButtonSmall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedFontSize = SMALL_FONT_SIZE;
                Toast.makeText(getApplicationContext(),
                        R.string.image_editor_text_widget_small_font,
                        Toast.LENGTH_SHORT).show();
                fontButton.setImageDrawable(getResources().getDrawable(R.drawable.small_font_alpha_selected));
                editTextWidget.setTextSize(SMALL_FONT_SIZE);
            }
        });
        p.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        p.addRule(RelativeLayout.BELOW, fontButton.getId());
        p.topMargin = 10;
        p.leftMargin = 10;
        mainLayout.addView(fontButtonSmall, p);

        //Medium Font size button
        p = new RelativeLayout.LayoutParams(75, 75);
        fontButtonMedium = new ImageButton(ImageEditorActivity.this);
        fontButtonMedium.setLayoutParams(new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
        fontButtonMedium.setImageDrawable(getResources().getDrawable(R.drawable.medium_font_alpha));
        fontButtonMedium.setId(R.id.fontSizeMedium);
        fontButtonMedium.setBackgroundColor(getResources().getColor(R.color.white));
        fontButtonMedium.getBackground().setAlpha(192);// 75% transparent
        fontButtonMedium.setVisibility(View.INVISIBLE);
        fontButtonMedium.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedFontSize = MEDIUM_FONT_SIZE;
                Toast.makeText(getApplicationContext(),
                        R.string.image_editor_text_widget_medium_font,
                        Toast.LENGTH_SHORT).show();
                fontButton.setImageDrawable(getResources().getDrawable(R.drawable.medium_font_alpha_selected));
                editTextWidget.setTextSize(MEDIUM_FONT_SIZE);
            }
        });
        p.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        p.addRule(RelativeLayout.BELOW, fontButtonSmall.getId());
        p.topMargin = 10;
        p.leftMargin = 10;
        mainLayout.addView(fontButtonMedium, p);

        //Large Font size button
        p = new RelativeLayout.LayoutParams(75, 75);
        fontButtonLarge = new ImageButton(ImageEditorActivity.this);
        fontButtonLarge.setLayoutParams(new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
        fontButtonLarge.setImageDrawable(getResources().getDrawable(R.drawable.large_font_alpha));
        fontButtonLarge.setId(R.id.fontSizeLarge);
        fontButtonLarge.setBackgroundColor(Color.WHITE);
        fontButtonLarge.getBackground().setAlpha(192);// 75% transparent
        fontButtonLarge.setVisibility(View.INVISIBLE);
        fontButtonLarge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedFontSize = LARGE_FONT_SIZE;
                Toast.makeText(getApplicationContext(),
                        R.string.image_editor_text_widget_large_font,
                        Toast.LENGTH_SHORT).show();
                fontButton.setImageDrawable(getResources().getDrawable(R.drawable.large_font_alpha_selected));
                editTextWidget.setTextSize(LARGE_FONT_SIZE);
            }
        });
        p.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        p.addRule(RelativeLayout.BELOW, fontButtonMedium.getId());
        p.topMargin = 10;
        p.leftMargin = 10;
        mainLayout.addView(fontButtonLarge, p);

        if (asyncDialog != null && asyncDialog.isShowing()) {
            try {
                asyncDialog.dismiss();
                asyncDialog = null;
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    private void toggleFontSizeButtons() {
        if (selectedTool == AnnotationType.Text && toolState == ToolState.Work) {
            fontButton.setVisibility(View.VISIBLE);
            fontButtonSmall.setVisibility(View.VISIBLE);
            fontButtonMedium.setVisibility(View.VISIBLE);
            fontButtonLarge.setVisibility(View.VISIBLE);
        } else {
            fontButton.setVisibility(View.INVISIBLE);
            fontButtonSmall.setVisibility(View.INVISIBLE);
            fontButtonMedium.setVisibility(View.INVISIBLE);
            fontButtonLarge.setVisibility(View.INVISIBLE);
        }
    }

    // Refresh UI with ToolState and AnnotationType enum variable
    private void updateButtons() {
        paintBrushButton.setVisibility(View.GONE);
        circleButton.setVisibility(View.GONE);
        textButton.setVisibility(View.GONE);
        arrowButton.setVisibility(View.GONE);
        rectangleButton.setVisibility(View.GONE);
        colorButtonRed.setVisibility(View.GONE);
        colorButtonBlue.setVisibility(View.GONE);
        colorButtonYellow.setVisibility(View.GONE);
        colorButtonGreen.setVisibility(View.GONE);
        colorButtonWhite.setVisibility(View.GONE);
        colorButtonBlack.setVisibility(View.GONE);
        colorButtonGray.setVisibility(View.GONE);
        colorButtonPurple.setVisibility(View.GONE);
        colorButtonOrange.setVisibility(View.GONE);
        if (toolState == ToolState.Select) {
            paintBrushButton.setVisibility(View.VISIBLE);
            circleButton.setVisibility(View.VISIBLE);
            textButton.setVisibility(View.VISIBLE);
            arrowButton.setVisibility(View.VISIBLE);
            rectangleButton.setVisibility(View.VISIBLE);
        } else if (toolState == ToolState.Work) {
            switch (selectedTool) {
                case Paint:
                    paintBrushButton.setVisibility(View.VISIBLE);
                    paintBrushButton.requestFocus();
                    break;
                case Circle:
                    circleButton.setVisibility(View.VISIBLE);
                    circleButton.requestFocus();
                    break;
                case Arrow:
                    arrowButton.setVisibility(View.VISIBLE);
                    arrowButton.requestFocus();
                    break;
                case Text:
                    textButton.setVisibility(View.VISIBLE);
                    textButton.requestFocus();
                    break;
                case Rectangle:
                    rectangleButton.setVisibility(View.VISIBLE);
                    rectangleButton.requestFocus();
                    break;
                case None:
                    break;
            }
        }

        if (colorState == ColorState.Select) {
            colorButtonRed.setVisibility(View.VISIBLE);
            colorButtonBlue.setVisibility(View.VISIBLE);
            colorButtonYellow.setVisibility(View.VISIBLE);
            colorButtonGreen.setVisibility(View.VISIBLE);
            colorButtonWhite.setVisibility(View.VISIBLE);
            colorButtonBlack.setVisibility(View.VISIBLE);
            colorButtonGray.setVisibility(View.VISIBLE);
            colorButtonPurple.setVisibility(View.VISIBLE);
            colorButtonOrange.setVisibility(View.VISIBLE);
        }

        toggleFontSizeButtons();
    }

    //When tapping on selected color square, expand or hide palette
    private void toggleColorState() {
        if (colorState == ColorState.Work) {
            colorState = ColorState.Select;
        } else {
            colorState = ColorState.Work;
        }
    }

    //when tapping on tool, hide or show tool list and apply text if present
    private void toggleToolState() {
        if (toolState == ToolState.Work) {
            toolState = ToolState.Select;
            if (editTextWidget != null) {
                if (editTextWidget.getText().length() > 0) {
                    imageEditorView.applyTextToBitmap();
                } else {
                    imageEditorView.resetEditTextWidget();
                }
            }
        } else {
            toolState = ToolState.Work;
        }
    }

    public class ImageEditorView extends SurfaceView implements Runnable {
        //Single thread manages draw loop
        private Thread thread;
        private final SurfaceHolder holder;
        //On/Off flag for draw loop
        private boolean mFlag = false;
        private Path path = new Path();
        private PointF startPoint = new PointF();
        private PointF endPoint = new PointF();
        //Paint is used for drawing, dynamically modified to match color/stroke type
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textOutlinePaint = new Paint();
        //For tracking dirty (overlap) region when using paint tool
        private final RectF dirtyRect = new RectF();
        private float lastTouchX;
        private float lastTouchY;
        //Line width constant for drawing with pencil, circle, rectangle tool
        private static final float STROKE_WIDTH = 8f;
        private static final float HALF_STROKE_WIDTH = STROKE_WIDTH / 2;

        //For tracking shapes in progress (user has not lifted finger from screen yet)
        private ArrowPath arrowToAdd;
        private Rectangle rectangleToAdd;
        private Circle circleToAdd;
        private PaintBrush paintBrushToAdd;
        private Text textToAdd;

        //For saving
        private boolean shouldDrawImage = false;
        //Bitmap holding just annotations
        private Bitmap annotationsBitmap;
        //Stack contains all annotations for this session
        private Stack<Annotation> annotations = new Stack<>();
        //Sends an undo event to processing loop to avoid concurrent modification during draw
        private boolean requestUndo = false;

        //Annotation class holds all annotation types and details on how to draw.
        private class Annotation {
            float x, y, endX, endY;
            int color, fontSize;
            Path path;
            AnnotationType type;
            String text;
            RectF ovalRect;

            Annotation(Circle circle) {
                this.type = AnnotationType.Circle;
                this.ovalRect = circle.getRect();
                this.color = circle.color;
            }

            Annotation(Text textObj) {
                this.type = AnnotationType.Text;
                this.x = textObj.positionX;
                this.y = textObj.positionY;
                this.color = textObj.color;
                this.text = textObj.text;
                this.fontSize = textObj.fontSize;
            }

            Annotation(ArrowPath arrow) {
                this.type = AnnotationType.Arrow;
                this.x = arrow.start.x;
                this.y = arrow.start.y;
                this.path = arrow.path;
                this.color = arrow.color;
                if(arrow.end == null)
                    arrow.end = new PointF(0,0);
                this.endX = arrow.end.x;
                this.endY = arrow.end.y;
            }

            Annotation(PaintBrush brush) {
                this.type = AnnotationType.Paint;
                this.path = brush.path;
                this.color = brush.color;
            }

            Annotation(Rectangle rectangle) {
                this.type = AnnotationType.Rectangle;
                this.x = rectangle.x1;
                this.endX = rectangle.x2;
                this.y = rectangle.y1;
                this.endY = rectangle.y2;
                this.color = rectangle.color;
            }

            //switch will draw correct shape
            void draw(Canvas canvas) {
                switch (type) {
                    case Circle:
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setColor(color);
                        //canvas.drawCircle(x, y, radius, paint);
                        //TODO: rewrite to handle the canvas states correctly
                        if(canvas!=null && ovalRect != null)
                            canvas.drawOval(ovalRect, paint);
                        break;
                    case Arrow:
                        paint.setStyle(Paint.Style.FILL);
                        paint.setColor(color);
                        canvas.drawLine(x, y, endX, endY,
                                paint);
                        canvas.drawPath(path, paint);
                        break;
                    case Text:
                        paint.setColor(color);
                        paint.setStyle(Style.FILL);
                        paint.setAntiAlias(true);
                        paint.setTextAlign(Align.LEFT);
                        paint.setTypeface(Typeface.SANS_SERIF);
                        paint.setTextSize(fontSize);
                        textOutlinePaint.setTextSize(fontSize);
                        canvas.drawText(text, x, y, textOutlinePaint);
                        canvas.drawText(text, x, y, paint);
                        break;
                    case Paint:
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setColor(color);
                        canvas.drawPath(path, paint);
                        break;
                    case Rectangle:
                        paint.setStyle(Paint.Style.STROKE);

                        paint.setColor(color);
                        canvas.drawRect(x, y, endX, endY, paint);
                        break;
                    default:
                        break;
                }
            }
        }

        //Structures for shapes to use while drawing
        private class Circle {
            float x, y;
            float x2, y2;
            int color;
            boolean doneEditing = false;
            RectF rectF;

            Circle(float x, float y, int color) {
                this.x = x;
                this.y = y;
                this.color = color;
            }

            void setEnd(float x2, float y2) {
                this.x2 = x2;
                this.y2 = y2;
                rectF = new RectF(x, y, x2, y2);
            }

            RectF getRect() {
                return rectF;
            }
        }

        private class Rectangle {
            float x1, y1, x2, y2;
            int color;
            boolean doneEditing = false;

            Rectangle(float x, float y, int color) {
                this.x1 = x;
                this.y1 = y;
                this.color = color;
            }

            void setEndCoordinates(float x2, float y2) {
                this.x2 = x2;
                this.y2 = y2;
            }
        }

        private class Text {
            int positionX, positionY;
            int color;
            int fontSize;
            String text;

            Text(String text, int x, int y, int color, int fontSize) {
                this.text = text;
                this.positionX = x;
                this.positionY = y;
                this.color = color;
                this.fontSize = fontSize;
            }
        }

        private class ArrowPath {
            Path path;
            int color;
            PointF start, end;
            boolean doneEditing = false;

            ArrowPath(Path path, int color) {
                this.path = path;
                this.color = color;
            }
        }

        private class PaintBrush {
            Path path;
            int color;
            boolean doneEditing = false;

            PaintBrush(Path path, int color) {
                this.path = path;
                this.color = color;
            }
        }


        private Rect imageFullSizeRectangle;
        private Rect screenSizeRectangle;

        public ImageEditorView(Context context) {
            super(context);
            holder = getHolder();
            paint.setColor(selectedColor);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeWidth(STROKE_WIDTH);
            textOutlinePaint.setColor(Color.BLACK);
            textOutlinePaint.setTextAlign(Align.LEFT);
            textOutlinePaint.setStyle(Style.FILL);
            textOutlinePaint.setAntiAlias(true);
            textOutlinePaint.setTextAlign(Align.LEFT);
            textOutlinePaint.setStrokeWidth(STROKE_WIDTH * 2);
            textOutlinePaint.setTypeface(Typeface.SANS_SERIF);
            imageFullSizeRectangle = new Rect(0, 0, imageBitmap.getWidth(),
                    imageBitmap.getHeight());
            screenSizeRectangle = new Rect(0, 0, width, height);
            resume();
        }

        //Pop annotation stack for undo
        public void popAnnotation() {
            if (annotations != null && annotations.size() > 0)
                annotations.pop();
        }

        //On draw called every tick
        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            // Draw bitmap
            // canvas.drawBitmap(imageBitmap, 0, 0, paint);

            if (!saveRequired) {
                canvas.drawBitmap(imageBitmap, imageFullSizeRectangle, screenSizeRectangle,
                        paint);
            } else {
                if (shouldDrawImage) {
                    canvas.drawBitmap(imageBitmap, 0, 0, paint);
                    canvas.drawBitmap(annotationsBitmap, 0, 0, paint);
                    return;
                }
            }

            //Draw annotations (Most used)
            for (Annotation annotation : annotations) {
                annotation.draw(canvas);
            }


            //Draws annotations in progress (user hasn't lifted finger from screen)
            paint.setStyle(Paint.Style.STROKE);
            if (circleToAdd != null && circleToAdd.x2 > 0) {
                paint.setColor(circleToAdd.color);
                canvas.drawOval(circleToAdd.getRect(), paint);
            }
            // Draw paint
            paint.setStyle(Paint.Style.STROKE);
            if (paintBrushToAdd != null) {
                paint.setColor(selectedColor);
                canvas.drawPath(paintBrushToAdd.path, paint);
            }
            paint.setStyle(Paint.Style.FILL);
            if (arrowToAdd != null) {
                if (arrowToAdd.start != null && arrowToAdd.end != null) {
                    paint.setColor(arrowToAdd.color);
                    canvas.drawLine(arrowToAdd.start.x, arrowToAdd.start.y, arrowToAdd.end.x,
                            arrowToAdd.end.y, paint);
                }
            }
            paint.setStyle(Paint.Style.STROKE);
            if (rectangleToAdd != null && rectangleToAdd.x2 != 0) {
                paint.setColor(rectangleToAdd.color);
                canvas.drawRect(rectangleToAdd.x1, rectangleToAdd.y1, rectangleToAdd.x2, rectangleToAdd.y2, paint);
            }
        }

        public void updateColor(int color) {
            paint.setColor(selectedColor);
            colorButton.setBackgroundColor(color);
        }

        public void clear() {
            path.reset();
            invalidate();
            arrowToAdd = null;
            rectangleToAdd = null;
            circleToAdd = null;
            paintBrushToAdd = null;
            rectangleToAdd = null;
        }

        public void resume() {
            // Instantiating the thread
            thread = new Thread(this);
            // setting the mFlag to true for start repainting
            mFlag = true;
            // Start repaint the SurfaceView
            thread.start();
        }

        //Circle generation
        private boolean onCircleTouchEvent(MotionEvent event) {
            float eventX = event.getX();
            float eventY = event.getY();
            Point thisPosition = new Point();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    circleToAdd = new Circle(eventX, eventY, selectedColor);
                    lastTouchX = eventX;
                    lastTouchY = eventY;
                    startPoint = new PointF();
                    startPoint.set((int) eventX, (int) eventY);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    thisPosition.set((int) eventX, (int) eventY);
                    circleToAdd.setEnd(eventX, eventY);
                    return true;
                case MotionEvent.ACTION_UP:

                    circleToAdd.doneEditing = true;
                    break;
                default:
                    return false;
            }
            return true;
        }

        private void resetEditTextWidget() {
            editTextWidget.setVisibility(View.GONE);
            editTextWidget.setText("");
        }

        private void applyTextToBitmap() {
            if (editTextWidget != null) {
                String textValue = editTextWidget.getText().toString();
                if (textValue.length() > 0) {
                    RelativeLayout.LayoutParams lp = (android.widget.RelativeLayout.LayoutParams) editTextWidget
                            .getLayoutParams();
                    int x = lp.leftMargin;
                    int y = lp.topMargin;
                    float editTextOffsetX = 0;
                    float editTextOffsetY;
                    if (textValue.length() > 10) {
                        editTextWidget.measure(0, 0);
                        editTextOffsetY = editTextWidget.getMeasuredHeight() / 2;
                    } else {
                        editTextOffsetY = 20;
                        editTextOffsetX = 20;
                    }
                    textToAdd = new Text(textValue, (int) (x + editTextOffsetX),
                            (int) (y + editTextOffsetY), selectedColor, selectedFontSize * 3);
                    resetEditTextWidget();
                }
            }
        }

        //
        private boolean onTextTouchEvent(MotionEvent event) {
            float eventX = event.getX();
            float eventY = event.getY();
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) editTextWidget
                    .getLayoutParams();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    editTextWidget.setVisibility(View.VISIBLE);
                    lp.setMargins((int) Math.floor(eventX), (int) Math.floor(eventY), 0, 0);
                    editTextWidget.setLayoutParams(lp);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    editTextWidget.setVisibility(View.VISIBLE);
                    lp.setMargins((int) eventX, (int) eventY, 0, 0);
                    editTextWidget.setLayoutParams(lp);
                    return true;
                case MotionEvent.ACTION_UP:
                    editTextWidget.requestFocus();
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                    editTextWidget.requestFocus();
                    break;
                default:
                    return false;
            }
            return true;
        }

        private boolean onRectangleTouchEvent(MotionEvent event) {
            float eventX = event.getX();
            float eventY = event.getY();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    rectangleToAdd = new Rectangle(eventX, eventY, selectedColor);

                    break;
                case MotionEvent.ACTION_MOVE:
                    rectangleToAdd.setEndCoordinates(eventX, eventY);
                    break;
                case MotionEvent.ACTION_UP:
                    rectangleToAdd.doneEditing = true;
                    break;
                default:
                    return false;
            }
            return true;

        }

        private boolean onArrowTouchEvent(MotionEvent event) {
            float deltaX;
            float deltaY;
            float fraction;
            float point_x_1;
            float point_y_1;
            float point_x_2;
            float point_y_2;
            float point_x_3;
            float point_y_3;
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    path = new Path();
                    arrowToAdd = new ArrowPath(path, selectedColor);
                    // arrowToAdd.path.moveTo(x, y);
                    startPoint = new PointF(event.getX(), event.getY());
                    endPoint = new PointF();
                    arrowToAdd.start = startPoint;
                    break;
                case MotionEvent.ACTION_MOVE:
                    try {
                        endPoint.x = event.getX();
                        endPoint.y = event.getY();
                        arrowToAdd.end = endPoint;
                    } catch(NullPointerException e) {
                        Log.e(TAG, "arrowToAdd.end: " + arrowToAdd.end + ", endPoint: (" +
                                endPoint.x + ", " + endPoint.y + ")", e);
                        arrowToAdd.end = new PointF(0,0);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    // arrowToAdd.path.lineTo(mX, mY);
                    deltaX = endPoint.x - startPoint.x;
                    deltaY = endPoint.y - startPoint.y;
                    fraction = (float) 0.1;
                    point_x_1 = startPoint.x + ((1 - fraction) * deltaX + fraction * deltaY);
                    point_y_1 = startPoint.y + ((1 - fraction) * deltaY - fraction * deltaX);
                    point_x_2 = endPoint.x;
                    point_y_2 = endPoint.y;
                    point_x_3 = startPoint.x + ((1 - fraction) * deltaX - fraction * deltaY);
                    point_y_3 = startPoint.y + ((1 - fraction) * deltaY + fraction * deltaX);
                    arrowToAdd.path.moveTo(point_x_1, point_y_1);
                    arrowToAdd.path.lineTo(point_x_2, point_y_2);
                    arrowToAdd.path.lineTo(point_x_3, point_y_3);
                    arrowToAdd.path.lineTo(point_x_1, point_y_1);
                    arrowToAdd.path.lineTo(point_x_1, point_y_1);
                    arrowToAdd.doneEditing = true;
                    endPoint.x = event.getX();
                    endPoint.y = event.getY();
                    break;
                default:
                    return false;
            }
            return true;
        }

        private boolean onPaintTouchEvent(MotionEvent event) {
            float eventX = event.getX();
            float eventY = event.getY();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    path = new Path();
                    paintBrushToAdd = new PaintBrush(path, selectedColor);
                    paintBrushToAdd.path.moveTo(eventX, eventY);
                    lastTouchX = eventX;
                    lastTouchY = eventY;
                    // There is no end point yet, so don't waste cycles invalidating.
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (paintBrushToAdd != null && paintBrushToAdd.path != null) {
                        paintBrushToAdd.path.lineTo(eventX, eventY);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    // Start tracking the dirty region.
                    resetDirtyRect(eventX, eventY);
                    // When the hardware tracks events faster than they are delivered, the
                    // event will contain a history of those skipped points.
                    int historySize = event.getHistorySize();
                    for (int i = 0; i < historySize; i++) {
                        float historicalX = event.getHistoricalX(i);
                        float historicalY = event.getHistoricalY(i);
                        expandDirtyRect(historicalX, historicalY);
                        if (paintBrushToAdd != null && paintBrushToAdd.path != null) {
                            paintBrushToAdd.path.lineTo(historicalX, historicalY);
                        }
                    }
                    // After replaying history, connect the line to the touch point.
                    if (paintBrushToAdd != null && paintBrushToAdd.path != null) {
                        paintBrushToAdd.path.lineTo(eventX, eventY);
                        paintBrushToAdd.doneEditing = true;
                    }
                    break;
                default:
                    return false;
            }
            // Include half the stroke width to avoid clipping.
            invalidate((int) (dirtyRect.left - HALF_STROKE_WIDTH),
                    (int) (dirtyRect.top - HALF_STROKE_WIDTH),
                    (int) (dirtyRect.right + HALF_STROKE_WIDTH),
                    (int) (dirtyRect.bottom + HALF_STROKE_WIDTH));
            lastTouchX = eventX;
            lastTouchY = eventY;
            return true;
        }

        private void handleUndo() {
            if (requestUndo) {
                requestUndo = false;
                popAnnotation();

            }
        }

        private void addShapesToDraw() {
            if (circleToAdd != null) {
                if (circleToAdd.doneEditing) {
                    annotations.add(new Annotation(circleToAdd));
                    circleToAdd = null;
                }
            }
            if (arrowToAdd != null) {
                if (arrowToAdd.doneEditing) {
                    annotations.add(new Annotation(arrowToAdd));
                    arrowToAdd = null;
                }
            }
            if (paintBrushToAdd != null) {
                if (paintBrushToAdd.doneEditing) {
                    annotations.add(new Annotation(paintBrushToAdd));
                    paintBrushToAdd = null;
                }
            }
            if (textToAdd != null) {
                annotations.add(new Annotation(textToAdd));
                textToAdd = null;
            }
            if (rectangleToAdd != null) {
                if (rectangleToAdd.doneEditing) {
                    annotations.add(new Annotation(rectangleToAdd));
                    rectangleToAdd = null;
                }
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (toolState != ToolState.Work) {
                toolState = ToolState.Work;
                updateButtons();
            }
            switch (selectedTool) {
                case Paint:
                    return onPaintTouchEvent(event);
                case Circle:
                    return onCircleTouchEvent(event);
                case Text:
                    return onTextTouchEvent(event);
                case Arrow:
                    return onArrowTouchEvent(event);
                case Rectangle:
                    return onRectangleTouchEvent(event);
                default:
                    return false;
            }
        }

        public void pause() {
            mFlag = false;
        }

        @SuppressLint("WrongCall")
        @Override
        public void run() {
            Canvas canvas;
            while (mFlag) {
                canvas = null;
                if (!saveRequired) {
                    // Check whether the object holds a valid surface
                    if (!holder.getSurface().isValid())
                        continue;
                    // Start editing the surface
                    try {
                        canvas = holder.lockCanvas();
                        synchronized (holder) {
                            onDraw(canvas);
                        }
                    } finally {
                        if (canvas != null) {
                            holder.unlockCanvasAndPost(canvas);
                        }
                    }
                }
                // Draw text
                // Finish editing the canvas and show to the user
                addShapesToDraw();
                handleUndo();
                if (saveRequired) {
                    saveToDeviceUrl();
                    // new SaveImageTask(shouldFinishAfterSave).execute();
                    saveRequired = false;
                }
            }
        }

        /**
         * Resets the dirty region when the motion event occurs.
         */
        private void resetDirtyRect(float eventX, float eventY) {
            // The lastTouchX and lastTouchY were set when the ACTION_DOWN
            // motion event occurred.
            dirtyRect.left = Math.min(lastTouchX, eventX);
            dirtyRect.right = Math.max(lastTouchX, eventX);
            dirtyRect.top = Math.min(lastTouchY, eventY);
            dirtyRect.bottom = Math.max(lastTouchY, eventY);
        }

        /**
         * Called when replaying history to ensure the dirty region includes all points.
         */
        private void expandDirtyRect(float historicalX, float historicalY) {
            if (historicalX < dirtyRect.left) {
                dirtyRect.left = historicalX;
            } else if (historicalX > dirtyRect.right) {
                dirtyRect.right = historicalX;
            }
            if (historicalY < dirtyRect.top) {
                dirtyRect.top = historicalY;
            } else if (historicalY > dirtyRect.bottom) {
                dirtyRect.bottom = historicalY;
            }
        }

        @SuppressLint("WrongCall")
        public void saveToDeviceUrl() {
            annotationsBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas annotationsCanvas = new Canvas(annotationsBitmap);
            shouldDrawImage = false;
            onDraw(annotationsCanvas);
            // now, I need to do the scaling of the bitmap. This scaling needs to be the same amount that the original
            // image was downscaled as.
            annotationsBitmap = Bitmap.createScaledBitmap(annotationsBitmap, originalWidth,
                    originalHeight, true);
            final Bitmap bitmapToSave = Bitmap.createBitmap(originalWidth, originalHeight,
                    Bitmap.Config.ARGB_8888);
            Canvas canvasToSave = new Canvas(bitmapToSave);
            shouldDrawImage = true;
            onDraw(canvasToSave);
            File path = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            final File file = new File(path, deviceUrl);
            Thread thread = new Thread("New Thread") {
                public void run() {
                    try {
                        bitmapToSave.compress(Bitmap.CompressFormat.JPEG, 80, new FileOutputStream(file));
                    } catch (FileNotFoundException e) {
                        Log.e(TAG, e.getMessage());
                    }
                }
            };
            thread.start();

            saveInProgress = false;
        }
    }

    private class SaveImageTask extends AsyncTask<String, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            asyncDialog = new ProgressDialog(ImageEditorActivity.this);
            asyncDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            asyncDialog.setMessage(getString(R.string.image_editor_save_image_loader));
            asyncDialog.setCanceledOnTouchOutside(false);
            asyncDialog.show();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            while (saveInProgress) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (asyncDialog != null && asyncDialog.isShowing()) {
                try {
                    asyncDialog.cancel();
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }
            imageEditorView.clear();
            finish();
        }
    }


//    public static Bitmap Overlay(Bitmap bmp1, Bitmap bmp2) {
//        Bitmap bmOverlay = Bitmap.createBitmap(bmp1.getWidth(), bmp1.getHeight(), bmp1.getConfig());
//        Canvas canvas = new Canvas(bmOverlay);
//        canvas.drawBitmap(bmp1, new Matrix(), null);
//        canvas.drawBitmap(bmp2, 0, 0, null);
//        return bmOverlay;
//    }
}
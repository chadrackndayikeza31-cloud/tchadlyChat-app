package com.chadrack.tchadlychat;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Base64;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;

public class PhotoViewerActivity extends Activity {

    private ImageView imageView;
    private Matrix matrix;
    private float scale = 1f;
    private float lastX;
    private float lastY;
    private boolean dragging;
    private ScaleGestureDetector scaleDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        setContentView(R.layout.activity_photo_viewer);

        imageView = findViewById(R.id.imageViewer);
        matrix = new Matrix();

        String imageBase64 =
                getIntent().getStringExtra("imageBase64");

        if (imageBase64 == null || imageBase64.isEmpty()) {
            finish();
            return;
        }

        try {
            byte[] bytes =
                    Base64.decode(
                            imageBase64,
                            Base64.DEFAULT
                    );

            Bitmap bitmap =
                    BitmapFactory.decodeByteArray(
                            bytes,
                            0,
                            bytes.length
                    );

            if (bitmap == null) {
                finish();
                return;
            }

            imageView.setImageBitmap(bitmap);
            imageView.setScaleType(ImageView.ScaleType.MATRIX);

            scaleDetector =
                    new ScaleGestureDetector(
                            this,
                            new ScaleGestureDetector.SimpleOnScaleGestureListener() {

                                @Override
                                public boolean onScale(
                                        ScaleGestureDetector detector) {

                                    float factor =
                                            detector.getScaleFactor();

                                    float oldScale = scale;

                                    scale *= factor;

                                    if (scale < 1f) {
                                        scale = 1f;
                                    }

                                    if (scale > 6f) {
                                        scale = 6f;
                                    }

                                    float realFactor =
                                            scale / oldScale;

                                    matrix.postScale(
                                            realFactor,
                                            realFactor,
                                            detector.getFocusX(),
                                            detector.getFocusY()
                                    );

                                    imageView.setImageMatrix(matrix);

                                    return true;
                                }
                            }
                    );

            imageView.setOnTouchListener(
                    new View.OnTouchListener() {

                        @Override
                        public boolean onTouch(
                                View v,
                                MotionEvent event) {

                            scaleDetector.onTouchEvent(event);

                            switch (event.getActionMasked()) {

                                case MotionEvent.ACTION_DOWN:
                                    lastX = event.getX();
                                    lastY = event.getY();
                                    dragging = true;
                                    return true;

                                case MotionEvent.ACTION_MOVE:

                                    if (dragging &&
                                            !scaleDetector.isInProgress()) {

                                        float dx =
                                                event.getX() - lastX;

                                        float dy =
                                                event.getY() - lastY;

                                        matrix.postTranslate(
                                                dx,
                                                dy
                                        );

                                        imageView.setImageMatrix(
                                                matrix
                                        );

                                        lastX = event.getX();
                                        lastY = event.getY();
                                    }

                                    return true;

                                case MotionEvent.ACTION_UP:
                                case MotionEvent.ACTION_CANCEL:
                                    dragging = false;
                                    return true;
                            }

                            return true;
                        }
                    }
            );

        } catch (Exception e) {
            finish();
        }
    }
}
package com.example.watermarks;

import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.VideoView;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.ExecuteCallback;
import com.arthenica.mobileffmpeg.FFmpeg;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

public class ImageActivity extends AppCompatActivity {

    Button uploadBtn, downloadBtn;
    ImageView imgDisp;
    Bitmap newb;
    Uri selectedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        uploadBtn = findViewById(R.id.uploadImgBtn);
        downloadBtn = findViewById(R.id.downloadBtn);
        imgDisp = findViewById(R.id.imageDisp);

        uploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Launch the gallery so as the user can pick an image
                Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, 1);
            }
        });

        downloadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveImage(newb);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,  Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data !=null){
            // Get the image that the user selected
            selectedImage = data.getData();
            Log.d("TAG", "onActivityResult: "+selectedImage);

//            String type = data.getDataString();
//            Boolean isImage = type.contains("images");

            // Display it in the imageView
            try {
                // The bitmap we want to watermark
                Bitmap mainBitmap = MediaStore.Images.Media.getBitmap(ImageActivity.this.getContentResolver(), selectedImage);

                // The watermark itself
                Bitmap watermarkBitmap = BitmapFactory.decodeResource(ImageActivity.this.getResources(),
                        R.drawable.tiktok_logo);

                // Setting the Watermark
                newb = setWaterMark(mainBitmap, watermarkBitmap);
                // Display it in the imageView
                imgDisp.setImageBitmap(newb);

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private Bitmap setWaterMark(Bitmap src, Bitmap watermark) {
        int w = src.getWidth();
        int h = src.getHeight();
        int ww = w/4;
        int wh = h/6;
        // Create the new blank bitmap (By creating a canvas with mainBitmap)
        Bitmap result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(result);
        cv.drawBitmap(src, 0, 0, null);

        // Positioning the Watermark
        // Top-left
        int x = 0;
        int y = 0;

        // The actual watermarking
        cv.drawBitmap(getResizedBitmap(watermark, ww, wh), x, y, null);

        cv.save();
        cv.restore();
        return result;
    }

    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }

    private void saveImage (Bitmap bitmap) {
        OutputStream fos;
        try {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                ContentResolver resolver = getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "Image" + ".jpg");
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg");
                Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                fos = resolver.openOutputStream(Objects.requireNonNull(imageUri));
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                Objects.requireNonNull(fos);
            }
        } catch (Exception e) {

            Log.d("error", e.toString());
        }
    }



}
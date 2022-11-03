package com.example.watermarks;

import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;

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

import androidx.appcompat.app.AppCompatActivity;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.ExecuteCallback;
import com.arthenica.mobileffmpeg.FFmpeg;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

public class VideoActivity extends AppCompatActivity {

    Button uploadWatermarkBtn, uploadTrailingVidBtn, uploadVidBtn, downloadBtn;
    VideoView vidDisp;
    String trailerPath, watermarkPath, originalVideoPath;
    Uri selectedWatermark, selectedTrailVideo, selectedVideo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        uploadWatermarkBtn = findViewById(R.id.uploadWatermarkBtn);
        uploadTrailingVidBtn = findViewById(R.id.uploadTrailingVidBtn);
        uploadVidBtn = findViewById(R.id.uploadVidBtn);
        downloadBtn = findViewById(R.id.downloadBtn);
        vidDisp = findViewById(R.id.videoDisp);

        uploadWatermarkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Launch the gallery so as the user can pick a watermark
                Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, 1);
            }
        });

        uploadTrailingVidBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Launch the gallery so as the user can pick a trailing video
                Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, 2);


            }
        });

        uploadVidBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Launch the gallery so as the user can pick a video to watermark
                Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, 3);


            }
        });

        downloadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,  Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Watermark Request
        if (requestCode == 1 && resultCode == RESULT_OK && data !=null){
            // Get the image that the user selected
            selectedWatermark = data.getData();
            watermarkPath =  getPathFromURI(getApplicationContext(), selectedWatermark);
        }

        // Trailing Video Request
        else if (requestCode == 2 && resultCode == RESULT_OK && data !=null){
            // Get the video that the user selected
            selectedTrailVideo = data.getData();
            trailerPath =  getPathFromURI(getApplicationContext(), selectedTrailVideo);
        }

        // The Video to watermark Request
        else if (requestCode == 3 && resultCode == RESULT_OK && data !=null){
            // Get the video that the user selected
            selectedVideo = data.getData();

            ProgressDialog pd = new ProgressDialog(VideoActivity.this);
            pd.setMessage("Loading ....");
            pd.show();

            String fileName ="vid";
            String fileExt =".mp4";

            originalVideoPath = getPathFromURI (getApplicationContext(), selectedVideo);
            String dest = "/storage/emulated/0/Download/"+fileName+fileExt;
            String dest1 = "/storage/emulated/0/Download/"+fileName+"x"+fileExt;

            //Trial to merge 2 videos + watermark

            //Overlay Top right (with 25 pixels of padding)
            String watermark1CMD[]={
                    "-y",
                    "-i", originalVideoPath,
                    "-i", watermarkPath,
                    "-filter_complex",
                    "[1:v]scale=30:30 [ovrl], [0:v][ovrl]overlay=25:25:enable='between(t,0,3)'",
                    dest};

            //Overlay Bottom right (with 25 pixels of padding)
            String watermark2CMD[]={
                    "-y",
                    "-i", dest,
                    "-i", watermarkPath,
                    "-filter_complex",
                    "[1:v]scale=30:30 [ovrl], [0:v][ovrl]overlay=main_w-overlay_w-25:main_h-overlay_h-25:enable='gt(t,3)'",
                    dest1};

            String concatCMD[]={
                    "-y",
                    "-i", dest1,
                    "-i", trailerPath,
                    "-filter_complex",
                    "[0:v]scale=640:640, setsar=sar=10/11[v0]; " +
                        "[1:v]scale=640:640, setsar=sar=10/11[v1]; " +
                        "[v0][0:a] [v1][1:a] concat=n=2:v=1:a=1" ,
                    dest};

            FFmpeg.executeAsync(watermark1CMD, new ExecuteCallback() {
                @Override
                public void apply(long executionId, int returnCode) {
                    FFmpeg.executeAsync(watermark2CMD, new ExecuteCallback() {
                        @Override
                        public void apply(long executionId, int returnCode) {
                            FFmpeg.executeAsync(
                                    concatCMD, new ExecuteCallback() {
                                        @Override
                                        public void apply(final long executionId, final int returnCode) {
                                            if (returnCode == RETURN_CODE_SUCCESS) {

                                                if(pd.isShowing()){
                                                    pd.dismiss();
                                                }

                                                // after successful execution of ffmpeg command,
                                                // again set up the video Uri in VideoView
                                                vidDisp.setVideoURI(Uri.parse(dest));

                                                // play the result video in VideoView
                                                vidDisp.start();

                                            } else if (returnCode == RETURN_CODE_CANCEL) {
                                                Log.i(Config.TAG, "Async command execution cancelled by user.");
                                            } else {
                                                Log.i(Config.TAG, String.format("Async command execution failed with returnCode=%d.", returnCode));
                                            }
                                        }
                                    });
                        }
                    });
                }
            });

        }
    }

    private String getPathFromURI(Context context, Uri selectedVideo) {
        Cursor cursor =null;
        String [] proj ={MediaStore.Images.Media.DATA};
        cursor = context.getContentResolver().query(selectedVideo, proj, null,null,null);
        int colIdx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(colIdx);
    }


}
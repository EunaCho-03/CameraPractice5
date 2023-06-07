package com.example.camerapractice5;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.media.MediaActionSound;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ImageView;
import android.widget.Chronometer;
import android.widget.SeekBar;
import android.util.Log;
import android.widget.ToggleButton;
import androidx.annotation.RequiresApi;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.core.ZoomState;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.util.Consumer;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import android.content.Context;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MainActivity extends AppCompatActivity { // 하위버전 단말기에 실행 안되는 메소드를 지원하기 위해 AppCompatActivity를 extend함
    //버튼이나 필요한 API들 k선언하기
    Recording recording = null; // 실제 녹화를 실행함
    Chronometer chronometer;
    boolean running = false;
    MediaActionSound sound = new MediaActionSound();
    VideoCapture<Recorder> videoCapture = null;
    Button record, picture, flipCamera, flash;
    boolean flashOn = false;
    PreviewView previewView;
    ImageView imageView;
    ImageView focusSquare;
    Camera camera;
    ImageCapture imageCapture;
    ProcessCameraProvider processCameraProvider;
    int cameraFacing = CameraSelector.LENS_FACING_BACK;
    CameraManager cameraManager;
    String getCameraID;

    SeekBar zoombar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        record = findViewById(R.id.record);
        //record.setVisibility(View.INVISIBLE);
        picture = findViewById(R.id.picture);
        //picture.setVisibility(View.INVISIBLE);
        flipCamera = findViewById(R.id.flipCamera);
        //flipCamera.setVisibility(View.INVISIBLE);
        imageView = findViewById(R.id.imageView);
        focusSquare=findViewById(R.id.focusSquare); focusSquare.setVisibility(View.INVISIBLE);
        chronometer = findViewById(R.id.chronometer);
        chronometer.setFormat("%s");
        chronometer.setBackgroundColor(Color.RED);
        chronometer.setVisibility(View.INVISIBLE);
        zoombar=findViewById(R.id.zoombar);
        //zoombar.setBackgroundColor(Color.YELLOW);
        flash=findViewById(R.id.flash);


        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            getCameraID = cameraManager.getCameraIdList()[0];
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
/*
        if(camera.getCameraInfo().hasFlashUnit()){
            camera.getCameraControl().enableTorch(true);
        }
*/
        zoombar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                camera.getCameraControl().setLinearZoom((float) zoombar.getProgress()/seekBar.getMax());
                Log.e("TEST","seekBar_Max = " + seekBar.getMax());
                Log.e("TEST","progress = " + zoombar.getProgress());
                Log.e("TEST","Zoom_Ratio = " + zoombar.getProgress() / seekBar.getMax());
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        try {
            processCameraProvider = ProcessCameraProvider.getInstance(this).get();
        }
        catch (ExecutionException e) {
            e.printStackTrace();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            activityResultLauncher.launch(Manifest.permission.CAMERA);
        }else {
            bind();
        }

        previewView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        return true;
                    case MotionEvent.ACTION_UP:
                        MeteringPointFactory factory = previewView.getMeteringPointFactory();
                        MeteringPoint point = factory.createPoint(motionEvent.getX(), motionEvent.getY());
                        FocusMeteringAction action = new FocusMeteringAction.Builder(point).build();
                        CameraControl cameraControl = camera.getCameraControl();
                        cameraControl.startFocusAndMetering(action);
                        focusSquare.setX(motionEvent.getX());
                        focusSquare.setY(motionEvent.getY());
                        focusSquare.setVisibility(View.VISIBLE);
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                focusSquare.setVisibility(View.INVISIBLE);
                            }
                        }, 500);
                        return true;
                    default:
                        return false;
                }
            }
        });

        flash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (flashOn == false) {
                    try {
                        cameraManager.setTorchMode(getCameraID, true);
                        flashOn = true;
                        Toast.makeText(MainActivity.this, "Flashlight is turned ON", Toast.LENGTH_SHORT).show();
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        cameraManager.setTorchMode(getCameraID, false);
                        flashOn = false;
                        Toast.makeText(MainActivity.this, "Flashlight is turned OFF", Toast.LENGTH_SHORT).show();
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        });



        flipCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (cameraFacing == CameraSelector.LENS_FACING_BACK) {
                    cameraFacing = CameraSelector.LENS_FACING_FRONT;
                } else {
                    cameraFacing = CameraSelector.LENS_FACING_BACK;
                }
                processCameraProvider.unbindAll();
                bind();
            }
        });

        picture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    imageCapture.takePicture(ContextCompat.getMainExecutor(MainActivity.this),
                            new ImageCapture.OnImageCapturedCallback() {
                                @Override
                                public void onCaptureSuccess(@NonNull ImageProxy image) {
                                    @SuppressLint({"UnsafeExperimentalUsageError", "UnsafeOptInUsageError"})
                                    Image mediaImage = image.getImage();
                                    Bitmap bitmap = ImageUtil.mediaImageToBitmap(mediaImage);
                                    float rotationDegrees = image.getImageInfo().getRotationDegrees();
                                    Bitmap rotatedBitmap = ImageUtil.rotateBitmap(bitmap, rotationDegrees);
                                    imageView.setImageBitmap(rotatedBitmap);
                                    saveImage(rotatedBitmap);
                                }
                            }
                    );
            }


        });

        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //카메라, 오디오, 외부저장소 권한 체크
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    activityResultLauncher.launch(Manifest.permission.RECORD_AUDIO);
                    //} else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    //    activityResultLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                } else {
                    MainActivity.this.captureVideo(); // 모든 권한이 있다면 녹화하는 함수 호출
                }
            }
        });

    }

    private final ActivityResultLauncher<String> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            captureVideo();
        } else if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            bind();
        }
    });


    public void saveImage(Bitmap rotatedBitmap){
        Uri images;
        ContentResolver contentResolver = getContentResolver();
        images = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);

        String time = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault()).format(System.currentTimeMillis());
        ContentValues contentValues = new ContentValues(0);
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, time+ ".JPG");
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, "images/");
        contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Pictures/CameraX-Images");
        Uri uri = contentResolver.insert(images, contentValues);

        try{
            OutputStream outputStream = contentResolver.openOutputStream(Objects.requireNonNull(uri));
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG,100, outputStream);
            String msg = "촬영 완료: " + images;
            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void captureVideo() {
        Log.e("TEST","Capture Video Button Clicked");
        Recording recording1 = recording;
        if (recording1 != null) {
            Log.e("TEST","recording1 not null");
            recording1.stop();
            recording = null;
            return;
        }

        String time = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault()).format(System.currentTimeMillis());
        Log.e("TEST","Check time");
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, time);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
        contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video");

        MediaStoreOutputOptions options = new MediaStoreOutputOptions.Builder(getContentResolver(), MediaStore.Video.Media.EXTERNAL_CONTENT_URI).setContentValues(contentValues).build();
        Log.e("TEST","Media Store");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return;
        }


        recording = videoCapture.getOutput().prepareRecording(MainActivity.this, options).withAudioEnabled().start(ContextCompat.getMainExecutor(MainActivity.this), new Consumer<VideoRecordEvent>() {
            @Override
            public void accept(VideoRecordEvent videoRecordEvent) {
                Log.e("TEST","video accepted " + videoRecordEvent);
                //recording 계속 실행 (accept 함수로 인해 Finalize 될때까지 돌아감)
                if (videoRecordEvent instanceof VideoRecordEvent.Start) { // 녹화 시작
                    record.setEnabled(true); // record 시작
                    Log.e("TEST","On Progress");
                    //sound.play(MediaActionSound.START_VIDEO_RECORDING);

                    if(!running){ // 디폴트: false
                        chronometer.setVisibility(View.VISIBLE);
                        chronometer.setBase(SystemClock.elapsedRealtime()); // 현재시간과 마지막으로 클릭된 시간 차이 (한번 눌렀으니 0)
                        chronometer.start(); // 타이머 시작
                        running = true;
                        Log.e("TEST","Chronometer started");
                    }

                } else if (videoRecordEvent instanceof VideoRecordEvent.Finalize) { // 녹화 끝나서
                    if (!((VideoRecordEvent.Finalize) videoRecordEvent).hasError()) { // 에러가 없다면
                        //sound.play(MediaActionSound.STOP_VIDEO_RECORDING);
                        chronometer.setBase(SystemClock.elapsedRealtime());
                        chronometer.stop();
                        chronometer.setVisibility(View.INVISIBLE);
                        Log.e("TEST","Chronometer stopped");
                        running = false;
                        String msg = "녹화 완료: " + ((VideoRecordEvent.Finalize) videoRecordEvent).getOutputResults().getOutputUri(); // 메세지: 녹화분 정보
                        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show(); // 메세지와 함께 토스트 띄우기
                    } else {
                        recording.close(); // 녹화를 끝내고
                        recording = null; // recording에 있는 값들을 비움 처리
                        String msg = "Error: " + ((VideoRecordEvent.Finalize) videoRecordEvent).getError(); // 메세지: 어떤 에러인지
                        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show(); // 메세지와 함께 토스트 띄우기
                    }
                }
            }
        });
    }

    void bind(){
        //previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);
        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(cameraFacing)
                .build();
        Preview preview = new Preview.Builder()
                .build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        imageCapture = new ImageCapture.Builder()
                .build();
        Recorder recorder = new Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build();
        videoCapture = VideoCapture.withOutput(recorder);
        camera = processCameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, videoCapture);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
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
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ImageView;
import android.widget.Chronometer;
import android.widget.SeekBar;
import android.util.Log;

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
    ScaleGestureDetector mScaleGestureDetector;
    //private float mScaleFactor = 1.0f;

    //double first_interval_X = 0; // X 터치 간격

    //double first_interval_Y = 0; // Y 터치 간격
    double first_distance = 0;
    double initial_distance = 0;
    float initial_zoom = -1.0f;
    double first_X = 0;
    double first_Y = 0;
    double second_X = 0;
    double second_Y = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        record = findViewById(R.id.record);
        picture = findViewById(R.id.picture);
        flipCamera = findViewById(R.id.flipCamera);
        imageView = findViewById(R.id.imageView);
        focusSquare=findViewById(R.id.focusSquare); focusSquare.setVisibility(View.INVISIBLE);
        chronometer = findViewById(R.id.chronometer);
        chronometer.setFormat("%s");
        chronometer.setBackgroundColor(Color.RED);
        chronometer.setVisibility(View.INVISIBLE);
        zoombar=findViewById(R.id.zoombar);
        flash=findViewById(R.id.flash);
        mScaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());


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
                //Log.e("TEST","seekBar_Max = " + seekBar.getMax());
                //Log.e("TEST","progress = " + zoombar.getProgress());
                //Log.e("TEST","Zoom_Ratio = " + (float) zoombar.getProgress() / seekBar.getMax());
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

                //mScaleGestureDetector.onTouchEvent(motionEvent);
                switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {

                    case MotionEvent.ACTION_DOWN:
                        /*
                        first_X = motionEvent.getX();
                        first_Y = motionEvent.getY();
                        Log.e("TEST", "First X = " + first_X);
                        Log.e("TEST","First Y = " + first_Y);

                         */
/*
                        try {
                            double first_interval_X = (double) Math.abs(motionEvent.getX(0) - motionEvent.getX(1)); // 두 손가락 X좌표 차이 절대값
                            double first_interval_Y = (double) Math.abs(motionEvent.getY(0) - motionEvent.getY(1)); // 두 손가락 Y좌표 차이 절대값
                            double first_distance = Math.sqrt(Math.pow(first_interval_X, 2) + Math.pow(first_interval_Y, 2));
                            Log.e("TEST","ACTION_DOWN pass");
                            return true;
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                            Log.e("TEST", "Exception" + e.getMessage());
                        }
*/
                        return true;

                    case MotionEvent.ACTION_POINTER_DOWN:

                        initial_zoom = Objects.requireNonNull(camera.getCameraInfo().getZoomState().getValue()).getLinearZoom();

                        /*
                        second_X = motionEvent.getX();
                        second_Y = motionEvent.getY();
                        Log.e("TEST", "Second X = " + second_X);
                        Log.e("TEST", "Second Y = " + second_Y);

                         */
                        double touch_interval_X = (double) Math.abs(motionEvent.getX(0) - motionEvent.getX(1));
                        //Log.e("TEST","X 좌표 1 = " + motionEvent.getX(0));
                        //Log.e("TEST","X 좌표 2 = " + motionEvent.getX(1));
                        double touch_interval_Y = (double) Math.abs(motionEvent.getY(0) - motionEvent.getY(1));
                        initial_distance = Math.sqrt(Math.pow(touch_interval_X, 2) + Math.pow(touch_interval_Y, 2));
                        Log.e("TEST","Touch Distance = " + initial_distance);

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

                    case MotionEvent.ACTION_MOVE:


                        if(motionEvent.getPointerCount() == 2) {

                            try {

                                //double touch_interval_X = Math.abs(first_X - second_X);
                                //double touch_interval_Y = Math.abs(first_Y - second_Y);
                                //double first_distance = Math.sqrt(Math.pow(touch_interval_X, 2) + Math.pow(touch_interval_Y, 2));
                                //Log.e("TEST","First and Second Touch Distance = " + first_distance);

                                double now_interval_X = (double) Math.abs(motionEvent.getX(0) - motionEvent.getX(1)); // 두 손가락 X좌표 차이 절대값
                                //Log.e("TEST","X 좌표 1 = " + motionEvent.getX(0));
                                //Log.e("TEST","X 좌표 2 = " + motionEvent.getX(1));
                                double now_interval_Y = (double) Math.abs(motionEvent.getY(0) - motionEvent.getY(1)); // 두 손가락 Y좌표 차이 절대값
                                double now_distance = Math.sqrt(Math.pow(now_interval_X, 2) + Math.pow(now_interval_Y, 2));

                                //double zoom_scale = now_distance / touch_distance;
                                //Log.e("TEST","zoom scale = " + zoom_scale);

//                                if(now_distance > touch_distance){ // 움직였을때 처음 두 손가락의 위치보다 멀어진다면 (now distance increase / decrease 여부 판단)
//                                    //zoom in
//                                    //camera.getCameraControl().setLinearZoom((float) Math.abs((now_distance - touch_distance) / touch_distance));
//                                    camera.getCameraControl().setLinearZoom((float) Math.abs( 1 -(touch_distance / now_distance)));
//                                    Log.e("TEST","zoom in ratio = " + Math.abs( 1 -(touch_distance / now_distance)));
//                                }
//                                if(now_distance < touch_distance){ // 줄어든다면
//                                    //zoom out
//                                    camera.getCameraControl().setLinearZoom((float) Math.abs((now_distance / touch_distance)));
//                                    Log.e("TEST","zoom out ratio = " + Math.abs((now_distance / touch_distance)));
//                                }

                                float zoom_delta = (float) (now_distance / initial_distance); // 현재 줌인/줌아웃을 하기 위해 당긴 거리와 처음 화면에 댔을때 거리의 차이
                                float zoom_delta_trasposed = zoom_delta - 1.f; // 예) 줌아웃: 0.8 , 줌인: 1.2라면 같은 비율로 밀거나 당겨지기 위해 1을 빼 -0.2, 0.2를 만듬
                                final float zoom_ratio = 0.25f; // 너무 빨리 움직이므로 속도를 줄이기 위해
                                camera.getCameraControl().setLinearZoom(initial_zoom + (zoom_delta_trasposed * zoom_ratio)); // 원래 카메라의 줌값에 1을뺀 값(줌 비율)을 0.25만큼 곱한 값을 더함
                                //줌아웃이 될때는 느려지는데 고치는법?
                                Log.e("TEST","Current zoom = " + (initial_zoom + (zoom_delta_trasposed * zoom_ratio)));

//                              camera.getCameraControl().setLinearZoom((float) (initial_zoom * zoom_delta));

                                //Log.e("TEST","First Distance = " + first_distance);
                                //Log.e("TEST", "Now Distance = " + now_distance);

                                //camera.getCameraControl().setLinearZoom((float) (zoom_scale));
/*
                                if (first_distance < now_distance) {
                                    //Log.e("TEST", "Distance Difference = " + (now_distance - first_distance));
                                    camera.getCameraControl().setLinearZoom((float) (now_distance - first_distance) / 300);
                                    //camera.getCameraControl().setLinearZoom((float)(now_interval_X + now_interval_Y) / 950);
                                }
                                if (now_distance < first_distance) {
                                    camera.getCameraControl().setLinearZoom((float) (first_distance - now_distance) / 1000);
                                }
*/

                            } catch (IllegalArgumentException e) {
                                e.printStackTrace();
                            }
                            return true;

                        }

                            /*
                            Log.e("TEST","touch_X = " + motionEvent.getX());
                            Log.e("TEST","now_interval_X = " + now_interval_X);
                            Log.e("TEST","touch_Y = " + motionEvent.getY());
                            Log.e("TEST","now_interval_Y = " + now_interval_Y);
                            Log.e("TEST","zoom = " + (float)(now_interval_X + now_interval_Y) / 900);
                            if(touch_interval_X < now_interval_X && touch_interval_Y < now_interval_Y) { // 이전 값과 비교
                                //줌인
                                camera.getCameraControl().setLinearZoom((float)(now_interval_X + now_interval_Y) / 950);
                                //Log.e("TEST","zoom ratio = " + (float)(now_interval_X + now_interval_Y) / 900);
                                //camera.getCameraControl().setZoomRatio((float)(now_interval_X + now_interval_Y) / 900);
                             }
                            if(touch_interval_X > now_interval_X && touch_interval_Y > now_interval_Y) {
                                //줌아웃
                                camera.getCameraControl().setLinearZoom((float)(now_interval_X + now_interval_Y) / 900);
                                Log.e("TEST","now_interval_X = " + now_interval_X);
                                Log.e("TEST","now_interval_Y = " + now_interval_Y);
                                Log.e("TEST","now_sum = " + now_interval_X + now_interval_Y);
                            }
                            */

                    default:
                        return false;
                }
            }
        });

/*
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
*/
        flash.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if(flashOn == false) {
                    if (camera.getCameraInfo().hasFlashUnit()) {
                        camera.getCameraControl().enableTorch(true);
                    }
                    flashOn = true;
                } else{
                    camera.getCameraControl().enableTorch(false);
                    flashOn = false;
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
/*
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector){
            // ScaleGestureDetector에서 factor를 받아 변수로 선언한 factor에 넣고
            mScaleFactor *= scaleGestureDetector.getScaleFactor();

            // 최대 10배, 최소 10배 줌 한계 설정
            mScaleFactor = Math.max(0.5f, Math.min(mScaleFactor, 10.0f));

            // 프리뷰 스케일에 적용
            previewView.setScaleX(mScaleFactor);
            previewView.setScaleY(mScaleFactor);
            return true;
        }
    }

*/
    @Override
    public boolean onTouchEvent(MotionEvent event){
        mScaleGestureDetector.onTouchEvent(event);
        return true;
    }

    public class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector){
            float gesturefactor = detector.getScaleFactor();

            if(gesturefactor > 1){
                //zoom in
            }
            else{
                //zoom in
            }
            return true;
        }
    }


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
package com.example.camerapractice5;

import static androidx.camera.view.PreviewView.ImplementationMode.COMPATIBLE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.YuvImage;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaActionSound;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
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
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.DisplayOrientedMeteringPointFactory;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.core.resolutionselector.AspectRatioStrategy;
import androidx.camera.core.resolutionselector.ResolutionSelector;
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

import static android.Manifest.permission.CAMERA;

import com.bumptech.glide.Glide;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.opencv.android.OpenCVLoader;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import com.example.camerapractice5.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements ImageAnalysis.Analyzer{ // 하위버전 단말기에 실행 안되는 메소드를 지원하기 위해 AppCompatActivity를 extend함
    //ImageAnalysis.Analyzer = 인터페이스 상속(나중에 analyze 함수를 오버라이드 하기 위해서)

    private static final String TAG = "opencv";
    private Mat matInput;
    private Mat matResult;
    private CameraBridgeViewBase mOpenCvCameraView;

    //public native void ConvertRGBtoGray(long matAddrInput, long matAddrResult);
    public native void ConvertRGBtoGray_withoutCV(byte[] in, byte[] out, int w, int h);
    byte[]out = null;
    Bitmap outBitmap = null;

    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("camerapractice5");
    }


    //버튼이나 필요한 API들 k선언하기
    Recording recording = null; // 실제 녹화를 실행함
    Chronometer chronometer;
    boolean running = false;
    MediaActionSound sound = new MediaActionSound();
    VideoCapture<Recorder> videoCapture = null;
    Button record, picture, flipCamera, flash;
    boolean flashOn = false;
    static PreviewView previewView;
    //    ImageView previewView;
    ImageView grayView;
    ImageView imageView;
    ImageView focusSquare;
    Camera camera;
    Image image;
    ImageCapture imageCapture;
    ImageAnalysis imageAnalysis;
    ProcessCameraProvider processCameraProvider;
    int cameraFacing = CameraSelector.LENS_FACING_BACK;
    CameraManager cameraManager;
    String getCameraID;
    //Byte[] bytes = previewView;
    ImageReader imageReader;
    SeekBar zoombar;
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
    float initial_ratio;
    static Mat mat;

    byte[] buffer = new byte[10];
    Display display;
    Activity activity;
    int degrees = 0;
    int rotation = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (OpenCVLoader.initDebug()) { //OpenCv 잘 가지고 왔는지 확인
            Log.d("Loaded", "Success");
        } else {
            Log.d("Loaded", "error");
        }

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        grayView = findViewById(R.id.grayView);
        record = findViewById(R.id.record);
        record.setVisibility(View.VISIBLE);
        picture = findViewById(R.id.picture);
        flipCamera = findViewById(R.id.flipCamera);
        imageView = findViewById(R.id.imageView);
        focusSquare = findViewById(R.id.focusSquare);
        focusSquare.setVisibility(View.INVISIBLE);
        chronometer = findViewById(R.id.chronometer);
        chronometer.setFormat("%s");
        chronometer.setBackgroundColor(Color.RED);
        chronometer.setVisibility(View.INVISIBLE);
        zoombar = findViewById(R.id.zoombar);
        flash = findViewById(R.id.flash);

        zoombar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                camera.getCameraControl().setLinearZoom((float) zoombar.getProgress() / seekBar.getMax());
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
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            activityResultLauncher.launch(Manifest.permission.CAMERA);
        } else {
            Log.e("TEST", "Going to bind");
            bind();
        }

        // Get the WindowManager service
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        DisplayMetrics displayMetrics = new DisplayMetrics();

        Display display = windowManager.getDefaultDisplay();

        display.getMetrics(displayMetrics);


        int maxX = displayMetrics.widthPixels;
        //Log.e("TEST", "Max X = " + maxX);
        int maxY = displayMetrics.heightPixels;
        //Log.e("TEST", "Max Y = " + maxY);
        //Log.e("TEST", "Ratio = " + ((float) maxY / (float) maxX));


        previewView.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {

                    case MotionEvent.ACTION_DOWN:
                        return true;

                    case MotionEvent.ACTION_POINTER_DOWN:

                        initial_ratio = Objects.requireNonNull(camera.getCameraInfo().getZoomState().getValue()).getZoomRatio();
                        initial_zoom = Objects.requireNonNull(camera.getCameraInfo().getZoomState().getValue()).getLinearZoom();

                        double touch_interval_X = (double) Math.abs(motionEvent.getX(0) - motionEvent.getX(1));
                        double touch_interval_Y = (double) Math.abs(motionEvent.getY(0) - motionEvent.getY(1));
                        initial_distance = Math.sqrt(Math.pow(touch_interval_X, 2) + Math.pow(touch_interval_Y, 2));
                        return true;

                    case MotionEvent.ACTION_UP:
                        //가로모드일때는 둘 다 잘됨. 하지만 이미지뷰를 rotate시켜놔서 그런지 세로모드일때는 focusSquare은 잘 보여지지만 focus가 안맞음. 그래서 rotate 시켜놓으면 반대로 포커스는 맞는데 focusSquare 위치가 틀림
                        MeteringPointFactory factory = previewView.getMeteringPointFactory();
                        float motionEventX = motionEvent.getX();
                        float motionEventY = motionEvent.getY();
                        Log.e("TEST", "X = " + motionEventX + " Y = " + motionEventY);
                        focusSquare.setX(motionEventX - (focusSquare.getWidth() / 2));
                        focusSquare.setY(motionEventY - (focusSquare.getHeight() / 2));
                        focusSquare.setVisibility(View.VISIBLE);
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                focusSquare.setVisibility(View.INVISIBLE);
                            }
                        }, 500);

                        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            MeteringPoint point = factory.createPoint(motionEvent.getX(), motionEvent.getY());
                            FocusMeteringAction action = new FocusMeteringAction.Builder(point).build();
                            CameraControl cameraControl = camera.getCameraControl();
                            cameraControl.startFocusAndMetering(action);
                        } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                            float rotatedMeteringX = motionEventY;
                            float rotatedMeteringY = previewView.getHeight() - motionEventX;
                            MeteringPoint point = factory.createPoint((float) ((motionEventY) / 1.97), (float) (maxX - motionEventX / 1.97));
                            //MeteringPoint point = factory.createPoint(maxX - motionEventX,motionEventY);
                            Log.e("TEST", "RotatedX = " + (float) ((motionEventY) / 1.97) + " RotatedY = " + (float) (maxX - motionEventX / 1.97));
                            FocusMeteringAction action = new FocusMeteringAction.Builder(point).build();
                            CameraControl cameraControl = camera.getCameraControl();
                            cameraControl.startFocusAndMetering(action);
                        }

//                        float rotatedX = motionEvent.getX();
//                        float rotatedY = motionEvent.getY();
//                        Matrix matrix = new Matrix();
//                        matrix.postRotate(90);
//                        float[] points = {rotatedX, rotatedY};
//                        matrix.mapPoints(points);
//                        float rotatedMeteringX = points[0];
//                        float rotatedMeteringY = points[1];
//
//                        focusSquare.setX(motionEvent.getX() - (focusSquare.getWidth() / 2));
//                        focusSquare.setY(motionEvent.getY() - (focusSquare.getHeight() / 2));
//                        Log.e("TEST", "x = " + motionEvent.getX() + " y = " + motionEvent.getY());
//                        focusSquare.setVisibility(View.VISIBLE);
//                        new Handler().postDelayed(new Runnable() {
//                            @Override
//                            public void run() {
//                                focusSquare.setVisibility(View.INVISIBLE);
//                            }
//                        }, 500);
//
//                        MeteringPointFactory factory = previewView.getMeteringPointFactory();
//                        MeteringPoint point = factory.createPoint(motionEvent.getX(), motionEvent.getY());
//                        FocusMeteringAction action = new FocusMeteringAction.Builder(point).build();
//                        CameraControl cameraControl = camera.getCameraControl();
//                        cameraControl.startFocusAndMetering(action);


                        //float originalRotation = previewView.getRotation();
                        //previewView.setRotation(originalRotation + 90);

                        //previewView.setRotation(originalRotation);

                        return true;

                    case MotionEvent.ACTION_MOVE:


                        if (motionEvent.getPointerCount() == 2) {

                            try {

                                double now_interval_X = (double) Math.abs(motionEvent.getX(0) - motionEvent.getX(1)); // 두 손가락 X좌표 차이 절대값
                                double now_interval_Y = (double) Math.abs(motionEvent.getY(0) - motionEvent.getY(1)); // 두 손가락 Y좌표 차이 절대값
                                double now_distance = Math.sqrt(Math.pow(now_interval_X, 2) + Math.pow(now_interval_Y, 2));

                                float zoom_delta = (float) (now_distance / initial_distance); // 현재 줌인/줌아웃을 하기 위해 당긴 거리와 처음 화면에 댔을때 거리의 차이
                                float zoom_delta_trasposed = zoom_delta - 1.f; // 예) 줌아웃: 0.8 , 줌인: 1.2라면 같은 비율로 밀거나 당겨지기 위해 1을 빼 -0.2, 0.2를 만듬
                                final float zoom_ratio = 0.25f; // 너무 빨리 움직이므로 속도를 줄이기 위해
                                camera.getCameraControl().setLinearZoom(initial_zoom + (zoom_delta_trasposed * zoom_ratio)); // 원래 카메라의 줌값에 1을뺀 값(줌 비율)을 0.25만큼 곱한 값을 더함

                            } catch (IllegalArgumentException e) {
                                e.printStackTrace();
                            }
                            return true;

                        }

                    default:
                        return false;
                }
            }
        });

        flash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (flashOn == false) {
                    if (camera.getCameraInfo().hasFlashUnit()) {
                        camera.getCameraControl().enableTorch(true);
                    }
                    flashOn = true;
                } else {
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


    public void saveImage(Bitmap rotatedBitmap) {
        Uri images;
        ContentResolver contentResolver = getContentResolver();
        images = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);

        String time = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault()).format(System.currentTimeMillis());
        ContentValues contentValues = new ContentValues(0);
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, time + ".JPG");
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, "images/");
        contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Pictures/CameraX-Images");
        Uri uri = contentResolver.insert(images, contentValues);

        try {
            OutputStream outputStream = contentResolver.openOutputStream(Objects.requireNonNull(uri));
            mat = new Mat();
            Bitmap gray_bitmap = Bitmap.createBitmap(rotatedBitmap);

            Utils.bitmapToMat(gray_bitmap, mat);
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY);
            Utils.matToBitmap(mat, gray_bitmap);

            gray_bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            String msg = "촬영 완료: " + images;
            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void captureVideo() {
        Log.e("TEST", "Capture Video Button Clicked");
        Recording recording1 = recording;
        if (recording1 != null) {
            Log.e("TEST", "recording1 not null");
            recording1.stop();
            recording = null;
            return;
        }

        String time = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault()).format(System.currentTimeMillis());
        Log.e("TEST", "Check time");
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, time);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
        contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video");

        MediaStoreOutputOptions options = new MediaStoreOutputOptions.Builder(getContentResolver(), MediaStore.Video.Media.EXTERNAL_CONTENT_URI).setContentValues(contentValues).build();
        Log.e("TEST", "Media Store");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return;
        }


        recording = videoCapture.getOutput().prepareRecording(MainActivity.this, options).withAudioEnabled().start(ContextCompat.getMainExecutor(MainActivity.this), new Consumer<VideoRecordEvent>() {
            @Override
            public void accept(VideoRecordEvent videoRecordEvent) {
                Log.e("TEST", "video accepted " + videoRecordEvent);
                //recording 계속 실행 (accept 함수로 인해 Finalize 될때까지 돌아감)
                if (videoRecordEvent instanceof VideoRecordEvent.Start) { // 녹화 시작
                    record.setEnabled(true); // record 시작
                    Log.e("TEST", "On Progress");
                    //sound.play(MediaActionSound.START_VIDEO_RECORDING);

                    if (!running) { // 디폴트: false
                        chronometer.setVisibility(View.VISIBLE);
                        chronometer.setBase(SystemClock.elapsedRealtime()); // 현재시간과 마지막으로 클릭된 시간 차이 (한번 눌렀으니 0)
                        chronometer.start(); // 타이머 시작
                        running = true;
                        Log.e("TEST", "Chronometer started");
                    }

                } else if (videoRecordEvent instanceof VideoRecordEvent.Finalize) { // 녹화 끝나서
                    if (!((VideoRecordEvent.Finalize) videoRecordEvent).hasError()) { // 에러가 없다면
                        //sound.play(MediaActionSound.STOP_VIDEO_RECORDING);
                        chronometer.setBase(SystemClock.elapsedRealtime());
                        chronometer.stop();
                        chronometer.setVisibility(View.INVISIBLE);
                        Log.e("TEST", "Chronometer stopped");
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

    void bind() {
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

        ResolutionSelector.Builder selectorBuilder = new ResolutionSelector.Builder();
        selectorBuilder.setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY);
        imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setResolutionSelector(selectorBuilder.build())
                .build();
        imageAnalysis.setAnalyzer(getMainExecutor(), this);
        camera = processCameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, videoCapture, imageAnalysis); // 바인딩에 ImageAnalysis 포함시키기
    }

    @Override
    public void analyze(@NonNull ImageProxy image) {
        Bitmap bitmap = image.toBitmap();
        image.close();
        Bitmap gray = toGray(bitmap);
        rotation = image.getImageInfo().getRotationDegrees();
        //Log.e("TEST", "rotation "+rotation);
        Bitmap rotated = ImageUtil.rotateBitmap(gray, rotation);
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;
        float scaleX = (float) screenWidth / rotated.getWidth();
        float scaleY = (float) screenHeight / rotated.getHeight();
        Log.e("TEST","Screen height = " + screenHeight + "width = " + screenWidth);
        Log.e("TEST","rotated width = " + rotated.getWidth() + "height = " + rotated.getHeight());
        float scale = Math.min(scaleX, scaleY);
        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale);
        //Bitmap resizedBitmap = Bitmap.createBitmap(
                //rotated, 0, 0, rotated.getWidth(), rotated.getHeight(), matrix, true);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(rotated, screenWidth, screenHeight, true);
        Log.e("TEST","resized width = " + resizedBitmap.getWidth() + "height = " + resizedBitmap.getHeight());
        BitmapFactory.Options options = new BitmapFactory.Options();
        //options.inSampleSize = 4;
        //grayView.setScaleType(ImageView.ScaleType.FIT_XY);
        grayView.setImageBitmap(resizedBitmap);
        Log.e("TEST","grayPreivew height = " + grayView.getHeight() + "width = " + grayView.getWidth());
    }

    private Bitmap toGray(Bitmap bitmap){
//        ByteArrayOutputStream stream = new ByteArrayOutputStream();
//        bitmap.compress(Bitmap.CompressFormat.PNG,100,stream);
//        byte[]in = stream.toByteArray();
        //ByteArrayInputStream in = new ByteArrayInputStream(byteArray);
        byte[]in = bitmapToByteArray(bitmap);
        //Log.e("TEST","bitmap height = " + bitmap.getHeight());
        //Log.e("TEST","bitmap width = " + bitmap.getWidth());
        //Log.e("TEST","bitmap size = " + (bitmap.getHeight() * bitmap.getWidth()));

        //Log.e("TEST","byte[]in length = " + in.length);
//        int length = in.length;
//        for(int i = 0; i < length; i++){
//            out[i] = in[i];
//        }
        //out = [in.length/3];

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if(out == null)
        {
            //out = new byte[width * height;
            out = new byte[width * height * 4];
        }
        //Log.e("TEST","bitmap height = " + (bitmap.getHeight() + "* bitmap width = " + bitmap.getWidth()));
        if(outBitmap == null)
        {
            //outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8); // 문제점: 회색이 아닌 색깔있는 프리뷰에 회색을 덧씌운것처럼 나옴.
            //비트맵을 카피시켜보는 방법을 찾아볼것
            outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        }
        //Log.e("TEST","width = " + bitmap.getWidth() + "height = " + bitmap.getHeight());
        ConvertRGBtoGray_withoutCV(in, out, width, height);

//        int quarter = out.length / 4;
//        ByteBuffer buffer = ByteBuffer.wrap(out,0,quarter);
        ByteBuffer buffer = ByteBuffer.wrap(out);
        outBitmap.copyPixelsFromBuffer(buffer);
        //Bitmap CopiedBitmap = outBitmap.copy(outBitmap.getConfig(),true);
        //Log.e("TEST","width = " + previewView.getMeasuredWidth() + "height = " + previewView.getMeasuredHeight());
//        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
//        int screenWidth = displayMetrics.widthPixels;
//        int screenHeight = displayMetrics.heightPixels;
        //Bitmap resizedBitmap = Bitmap.createScaledBitmap(outBitmap, screenWidth, screenHeight, true);
        //Log.e("TEST","Resized bitmap height = " + resizedBitmap.getHeight() + "width = " + resizedBitmap.getWidth());
//        Log.e("TEST","Screen height = " + screenHeight + "width = " + screenWidth);
        return outBitmap; // 비트맵 리턴
        //return resizedBitmap;
    }
    public byte[] bitmapToByteArray(Bitmap bitmap) {
        //int bytes = bitmap.getRowBytes() * bitmap.getHeight();
        int bytes = bitmap.getByteCount();
        //Log.e("TEST","Bitmap byte count = " + bytes);
        //int bytes = bitmap.getHeight() * bitmap.getWidth();
        ByteBuffer buffer = ByteBuffer.allocate(bytes);
        //buffer.rewind();
        bitmap.copyPixelsToBuffer(buffer);
        return buffer.array();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
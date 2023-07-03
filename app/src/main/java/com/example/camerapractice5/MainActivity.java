package com.example.camerapractice5;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
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
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
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
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.core.resolutionselector.AspectRatioStrategy;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.PendingRecording;
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

import static com.example.camerapractice5.ImageUtil.rotateBitmap;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.android.OpenCVLoader;
//import org.bytedeco.javacv.FFmpegFrameRecorder;
//import org.bytedeco.javacv.AndroidFrameConverter;
//import org.bytedeco.javacv.Frame;
//import org.bytedeco.javacv.Frame;
//import org.bytedeco.javacv.FrameRecorder;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoWriter;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity implements ImageAnalysis.Analyzer{ // 하위버전 단말기에 실행 안되는 메소드를 지원하기 위해 AppCompatActivity를 extend함
    //ImageAnalysis.Analyzer = 인터페이스 상속(나중에 analyze 함수를 오버라이드 하기 위해서)

    private static final String TAG = "opencv";
    private Mat matInput;
    private Mat matResult;
    private CameraBridgeViewBase mOpenCvCameraView;

    //public native void ConvertRGBtoGray(long matAddrInput, long matAddrResult);
    public native void ConvertRGBtoGray_withoutCV(byte[] in, byte[] out, int w, int h);
    public native void drawHough(byte[]in, byte[]houghOut, int width, int height);
    public native void drawCanny(byte[]in, byte[]cannyOut, int width, int height);
    public native void detectFace(byte[]in, byte[]face, int width, int height, String filepath);
    byte[]out = null;
    byte[]houghOut = null;
    Bitmap outBitmap = null;
    Bitmap houghOutBitmap = null;
    byte[]cannyOut = null;
    Bitmap cannyOutBitmap = null;
    byte[] face = null;
    Bitmap faceBitmap = null;

    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("camerapractice5");
    }


    //버튼이나 필요한 API들 k선언하기
    Recording recording = null; // 실제 녹화를 실행함
    Chronometer chronometer;
    boolean running = false;
    MediaActionSound sound = new MediaActionSound(); // 여러 소리를 냄
    VideoCapture<Recorder> videoCapture = null; //카메라가 비디오프레임을 구성하게함
    Button record, picture, flipCamera, flash; // 만든 버튼들
    RadioButton colorMode, grayMode, houghMode, cannyMode, faceDetection;
    RadioGroup radioGroup;
    boolean flashOn = false;
    boolean isGrayMode = false;
    static PreviewView previewView; // 카메라에 비치는 화면의 역할
    //    ImageView previewView;
    ImageView overPreview;
    ImageView imageView; // 이미지를 화면에 띄우기 위해서
    ImageView focusSquare;
    Camera camera;
    Image image;
    ImageCapture imageCapture; // 사진을 캡쳐할 수 있도록 기본 컨트롤을 제공
    ImageAnalysis imageAnalysis;
    ProcessCameraProvider processCameraProvider; // 수명주기와 연결하여 기본적인 카메라 접근을 부여함(카메라가 핸드폰에 있는지, 카메라 정보등)
    VideoWriter videoWriter;
    int cameraFacing = CameraSelector.LENS_FACING_BACK; // 디폴트: 카메라 후면
    CameraManager cameraManager;
    String getCameraID;
    //Byte[] bytes = previewView;
    ImageReader imageReader;
    SeekBar zoombar;
    //private float mScaleFactor = 1.0f;

    //double first_interval_X = 0; // X 터치 간격

    //double first_interval_Y = 0; // Y 터치 간격
    double first_distance = 0;
    double initial_distance;
    int a;
    float initial_zoom;
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
    protected void onCreate(Bundle savedInstanceState) { // onCreate: 액티비티가 생성될때 호출되며 사용자 인터페이스(클래스가 구현해야할 행동을 지정함) 초기화에 사용
        super.onCreate(savedInstanceState); // super class 호출 (activity를 구현하는데 필요한 과정) savedInstanceState = 화면 구성이 변경될때 (가로모드, 세로모드 전환 / 언어/ 입력기기)
        setContentView(R.layout.activity_main); // layout에 있는 activity_main.xml로 화면 정의

        if (OpenCVLoader.initDebug()) { //OpenCv 잘 가지고 왔는지 확인
            Log.d("Loaded", "Success");
        } else {
            Log.d("Loaded", "error");
        }

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        previewView = findViewById(R.id.previewView); // findViewById = activity_main.xml에서 설정된 뷰를 가져오는 메소드
        overPreview = findViewById(R.id.overPreview);
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
        houghMode = findViewById(R.id.houghMode);
        grayMode = findViewById(R.id.grayMode);
        colorMode = findViewById(R.id.colorMode);
        cannyMode = findViewById(R.id.cannyMode);
        radioGroup = findViewById(R.id.radio_group);
        faceDetection = findViewById(R.id.faceDetection);

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

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch(checkedId){
                    case R.id.colorMode:
//                        overPreview.setVisibility(View.INVISIBLE);
//                        processCameraProvider.unbindAll();
//                        bindColor();
                    case  R.id.grayMode:

                    case R.id.houghMode:

                    case R.id.cannyMode:

                    case R.id.faceDetection:
                        overPreview.setVisibility(View.VISIBLE);
                        processCameraProvider.unbindAll();
                        bind();
                    break;
                    default:
                    break;
                }
            }
        });


        try {
            processCameraProvider = ProcessCameraProvider.getInstance(this).get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) { // 권한 체크
            activityResultLauncher.launch(Manifest.permission.CAMERA);
        } else {
            Log.e("TEST", "Going to bind");
            bind(); // 권한 부여 받았다면 카메라 연결
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
            public boolean onTouch(View view, MotionEvent motionEvent) { // onTouch는 boolean이여야함
                switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {

                    case MotionEvent.ACTION_DOWN:
                        return true;

                    case MotionEvent.ACTION_POINTER_DOWN: // 손가락으로 눌렀을때

                        initial_ratio = Objects.requireNonNull(camera.getCameraInfo().getZoomState().getValue()).getZoomRatio();
                        initial_zoom = Objects.requireNonNull(camera.getCameraInfo().getZoomState().getValue()).getLinearZoom();
                        Log.e("TEST","Initial ratio = " + initial_ratio);
                        //Log.e("TEST","Intial zoom1 = " + initial_zoom);

                        double touch_interval_X = (double) Math.abs(motionEvent.getX(0) - motionEvent.getX(1));
                        double touch_interval_Y = (double) Math.abs(motionEvent.getY(0) - motionEvent.getY(1));
                        initial_distance = Math.sqrt(Math.pow(touch_interval_X, 2) + Math.pow(touch_interval_Y, 2));
                        return true;

                    case MotionEvent.ACTION_UP: // 눌렀다 땠을때
                        //가로모드일때는 둘 다 잘됨. 하지만 이미지뷰를 rotate시켜놔서 그런지 세로모드일때는 focusSquare은 잘 보여지지만 focus가 안맞음. 그래서 rotate 시켜놓으면 반대로 포커스는 맞는데 focusSquare 위치가 틀림
                        MeteringPointFactory factory = previewView.getMeteringPointFactory();  // MeteringPoint를 만듣는 곳
                        float motionEventX = motionEvent.getX();
                        float motionEventY = motionEvent.getY();
                        //Log.e("TEST", "X = " + motionEventX + " Y = " + motionEventY);
                        focusSquare.setX(motionEventX - (focusSquare.getWidth() / 2));
                        focusSquare.setY(motionEventY - (focusSquare.getHeight() / 2));
                        focusSquare.setVisibility(View.VISIBLE);
                        //Log.e("TEST","Initial zoom = " + camera.getCameraInfo().getIntrinsicZoomRatio());
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                focusSquare.setVisibility(View.INVISIBLE);
                            }
                        }, 500);

                        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            MeteringPoint point = factory.createPoint(motionEvent.getX(), motionEvent.getY()); // MeterinPoint: 카메라의 지점. 그 지점을 x,y 좌표로 나타냄
                            FocusMeteringAction action = new FocusMeteringAction.Builder(point).build(); // 찍은 좌표에 포커스 맞추기
                            CameraControl cameraControl = camera.getCameraControl();
                            cameraControl.startFocusAndMetering(action);
                        } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                            float rotatedMeteringX = motionEventY;
                            float rotatedMeteringY = previewView.getHeight() - motionEventX;
                            MeteringPoint point = factory.createPoint((float) ((motionEventY) / 1.97), (float) (maxX - motionEventX / 1.97));
                            //MeteringPoint point = factory.createPoint(maxX - motionEventX,motionEventY);
                            //Log.e("TEST", "RotatedX = " + (float) ((motionEventY) / 1.97) + " RotatedY = " + (float) (maxX - motionEventX / 1.97));
                            FocusMeteringAction action = new FocusMeteringAction.Builder(point).build();
                            CameraControl cameraControl = camera.getCameraControl(); // CameraControl 기능: 확대/축소, 초점, 노출 보정
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

                                float change_zoom = (float) (now_distance / initial_distance);
                                camera.getCameraControl().setZoomRatio(initial_ratio*change_zoom);
                                Log.e("TEST","Initial ratio = " + initial_ratio);

//                                float zoom_delta = (float) (now_distance / initial_distance); // 현재 줌인/줌아웃을 하기 위해 당긴 거리와 처음 화면에 댔을때 거리의 차이
//                                float zoom_delta_trasposed = zoom_delta - 1.f; // 예) 줌아웃: 0.8 , 줌인: 1.2라면 같은 비율로 밀거나 당겨지기 위해 1을 빼 -0.2, 0.2를 만듬
//                                final float zoom_ratio = 0.25f; // 너무 빨리 움직이므로 속도를 줄이기 위해
//                                Log.e("TEST","Initial zoom2 = " + initial_zoom);
//                                camera.getCameraControl().setLinearZoom(initial_zoom + (zoom_delta_trasposed * zoom_ratio)); // 원래 카메라의 줌값에 1을뺀 값(줌 비율)을 0.25만큼 곱한 값을 더함
                            } catch (IllegalArgumentException e) {
                                e.printStackTrace();
                            } return true;
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
                if (cameraFacing == CameraSelector.LENS_FACING_BACK) { // 후면 카메라였다면 전면으로
                    cameraFacing = CameraSelector.LENS_FACING_FRONT;
                } else {
                    cameraFacing = CameraSelector.LENS_FACING_BACK; // 전면으였다면 후면으로
                }
                processCameraProvider.unbindAll(); //기존 생명주기에 있던 연결들을 끊고
                bind(); //변경된 방향으로 새로운 카메라 뷰 생성하기 (원하는 객체를 바인드)
            }
        });

        picture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //sound.play(MediaActionSound.SHUTTER_CLICK); // 셔터 사운드
                imageCapture.takePicture(ContextCompat.getMainExecutor(MainActivity.this), // 메인스레드에서 작동할 것이다
                        new ImageCapture.OnImageCapturedCallback() { // 이미지 캡쳐가 완료되면 콜백 (콜백:어떤 조건이 충족되면(이벤트가 발생하면) 이 코드 처리를 해라)
                            @Override
                            public void onCaptureSuccess(@NonNull ImageProxy image) { // close하는(끝내는) 콜백 (여기서 @NonNull ImageProxy image = 캡쳐된 이미지
                                @SuppressLint({"UnsafeExperimentalUsageError", "UnsafeOptInUsageError"}) // UnsafeExperimentalUsageError와 UnsafeOptInUsageError 검사 항목을 건너 뛰어라
                                Image mediaImage = image.getImage(); // mediaImage = 캡쳐된 이미지
                                Bitmap bitmap = ImageUtil.mediaImageToBitmap(mediaImage); //만들어둔 ImageUtil의 이미지를 비트맵으로 변환시키는 메소드를 씀

                                if(grayMode.isChecked()){
                                    Bitmap grayBitmap =toGray(bitmap);
                                    float rotationDegrees = image.getImageInfo().getRotationDegrees();
                                    Bitmap rotatedBitmap = rotateBitmap(grayBitmap, rotationDegrees);
                                    imageView.setImageBitmap(rotatedBitmap);
                                    saveImage(rotatedBitmap);
                                }else if(houghMode.isChecked()){
                                    Bitmap houghBitmap = toHough(bitmap);
//                                    //BitmapDrawable drawable = (BitmapDrawable)overPreview.getDrawable();
//                                    //Bitmap houghBit = drawable.getBitmap();
//                                    //float move = (bitmap.getWidth() - bitmap.getHeight()) / 2;
//                                    //Bitmap newImage = Bitmap.createBitmap(bitmap).copy(Bitmap.Config.ARGB_8888, true);
//                                    Bitmap combined = Bitmap.createBitmap(bitmap.getWidth(),bitmap.getHeight(),Bitmap.Config.ARGB_8888);
////                                    DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
////                                    int screenWidth = displayMetrics.widthPixels;
////                                    int screenHeight = displayMetrics.heightPixels;
////                                    Bitmap resizedBitmap = Bitmap.createScaledBitmap(houghBitmap, screenWidth, screenHeight, true);
//                                    Canvas canvas = new Canvas(combined);
//                                    canvas.drawBitmap(bitmap, 0,0,null);
//                                    //Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
//                                    Paint paint = new Paint();
//                                    paint.setAlpha(200);
//                                    //paint.setColor(Color.RED);
//                                    canvas.drawBitmap(houghBitmap, 0,0,paint);
//                                    bitmap.recycle();
//                                    houghBitmap.recycle();
                                    float rotationDegrees = image.getImageInfo().getRotationDegrees();
                                    Bitmap rotatedBitmap = rotateBitmap(houghBitmap, rotationDegrees);
                                    imageView.setImageBitmap(rotatedBitmap);
                                    saveImage(rotatedBitmap);
                                }else if(cannyMode.isChecked()){
                                    Bitmap cannyBitmap = toCanny(bitmap);
                                    float rotationDegrees = image.getImageInfo().getRotationDegrees();
                                    Bitmap rotatedBitmap = rotateBitmap(cannyBitmap, rotationDegrees);
                                    imageView.setImageBitmap(rotatedBitmap);
                                    saveImage(rotatedBitmap);
                                } else{
                                    float rotationDegrees = image.getImageInfo().getRotationDegrees(); // 회전시켜야할 각도
                                    Bitmap rotatedBitmap = rotateBitmap(bitmap, rotationDegrees);  // 그 각도만큼 회전시킴
                                    imageView.setImageBitmap(rotatedBitmap); // 이미지뷰에 비트맵을 로드해서 출력한다
                                    saveImage(rotatedBitmap); // 저장하는 함수 호출
                                }
                            }
                        }
                );
            }
        });

        record.setOnClickListener(new View.OnClickListener() {  //카메라, 오디오, 외부저장소 권한 체크
            @Override
            public void onClick(View view) {
                //카메라, 오디오, 외부저장소 권한 체크
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) { // 권한 확인
                    activityResultLauncher.launch(Manifest.permission.RECORD_AUDIO); // 권한을 부여 받지 못했다면 다시 요청
                    //} else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    //    activityResultLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                } else {
//                    if(grayButton.isChecked()){
//                        if (isGrayRecording) {
//                            isGrayRecording = false;
//                            if (videoWriter != null) {
//                                Log.e("TEST","VideoWriter not null");
//                                videoWriter.release();
//                                Log.e("TEST","Gray Video Recording ended");
//;                                videoWriter = null;
//
//                                chronometer.setBase(SystemClock.elapsedRealtime());
//                                chronometer.stop();
//                                chronometer.setVisibility(View.INVISIBLE);
//                                running = false;
//                            }
//                        } else {
//                            if (!running) {
//                                chronometer.setVisibility(View.VISIBLE);
//                                chronometer.setBase(SystemClock.elapsedRealtime());
//                                chronometer.start();
//                                running = true;
//                            }
//                            isGrayRecording = true;
//                            Log.e("TEST","VideoWriter null");
//                            MainActivity.this.captureGrayVideo();
//                        }
//                    }
//                    else{
                    MainActivity.this.captureVideo(); // 모든 권한이 있다면 녹화하는 함수 호출
                    //}
                }
            }
        });
    }

    private final ActivityResultLauncher<String> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
        //람다 호출 방식: (매개변수, ...) -> {실행문}
        //activityResultLauncher = 활동을 시작하고 다시 결과를 받는다 (여기서는 권한을 부여받았는지 확인하는 용도)
        //A 액티비티가 B액티비티 결과에 따라 실행하는 프로그램이 달라질때 여기서 결과를 확인할 수 있음
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            //권한을 이미 부여 받았다면 요청을 다시 하지 않는다 호출 결과: PERMISSION_GRANTED(권한 있음) 또는 PERMISSION_DENITED (권한 없음)
            captureVideo();
        } else if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            bind();
        }
    });

    public void saveImage(Bitmap rotatedBitmap) {
        Uri images; // Uri = 리소스(외부 앱, 이미지, 택스트 등)에 접근할 수 있는 식별자 역할 (주소)
        ContentResolver contentResolver = getContentResolver(); // 컨텐츠에 엑세스를 줌 (데이터를 읽는다). 기능: 생성, 검색, 업데이트 및 삭제
        images = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY); //ContentResolver를 통해 이미지를 넣어주고 해당 위치의 Uri를 받는다

        String time = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault()).format(System.currentTimeMillis()); // 기기의 지역을 판별하고 현재 시간을 저장
        ContentValues contentValues = new ContentValues(0); // ContentValues: 아래정보들의 이름과 값을 관리하기 위해 만들어진 객체
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, time + ".JPG"); // 파일 이름
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, "images/"); // MIME_TYPE = 데이터가 어떤 형식인지 ex.text / image / audio / video / application
        contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Pictures/CameraX-Images");
        Uri uri = contentResolver.insert(images, contentValues); // 주소 지정
        Bitmap bitmap = Bitmap.createBitmap(rotatedBitmap);
        try {
            OutputStream outputStream = contentResolver.openOutputStream(Objects.requireNonNull(uri)); // Outputstream = 파일에 데이터 쓰기, uri는 null이면 안됨
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream); // OutputStream은 비트맵 이미지를 저장하기 위한 객체를 받는다. outputStream이 가지고있는 uri 주소에 저장
            //비트맵의 용량이 너무 커서 저장할때는 압축을 시켰다가 화면에 띄울때 다시 100%로 보여준다는 뜻
            String msg = "촬영 완료: " + images;
            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
        } catch (Exception e) { // 예외 케이스
            e.printStackTrace();
        }
    }

    public void captureVideo() {
        Recording recording1 = recording; // recording1이라는 변수에 recording값을 넣음
        Log.e("TEST","Test if null");


        if (recording1 != null) { // 만약 지금 실행되고있는 녹화가 있다면
            recording1.stop(); // 멈추고
            recording = null; // recording값에 다시 null
            Log.e("TEST","Recording not null");
            return;
        }

        Log.e("TEST","Recording null");
        String time = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault()).format(System.currentTimeMillis());
        ContentValues contentValues = new ContentValues(); // ContentValues:이름과 값을 관리하는 객체
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, time);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
        contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video");
        //MediaStoreOutputOptions = 아웃풋(비디오)를 MediaStore에 저장하는 옵션 (여기는 외부 저장소에)
        MediaStoreOutputOptions options = new MediaStoreOutputOptions.Builder(getContentResolver(), MediaStore.Video.Media.EXTERNAL_CONTENT_URI).setContentValues(contentValues).build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Recorder recorder = videoCapture.getOutput();
        PendingRecording pendingRecording = recorder.prepareRecording(MainActivity.this, options)
                .withAudioEnabled();
        recording = pendingRecording.start(ContextCompat.getMainExecutor(MainActivity.this), new Consumer<VideoRecordEvent>() { //recording에 캡쳐된 비디오 담기
            @Override
            public void accept(VideoRecordEvent videoRecordEvent) {
                //Log.e("TEST", "video accepted " + videoRecordEvent);
                //recording 계속 실행 (accept 함수로 인해 Finalize 될때까지 돌아감)
                if (videoRecordEvent instanceof VideoRecordEvent.Start) {
                    record.setEnabled(true); // record 시작
                    //Log.e("TEST", "On Progress");
                    //sound.play(MediaActionSound.START_VIDEO_RECORDING);

                    //녹화 버튼을 두번째 눌렀다는것은 녹화를 멈추고 저장하고싶다는 뜻이니
                    if (!running) { // 디폴트: false
                        chronometer.setVisibility(View.VISIBLE);
                        chronometer.setBase(SystemClock.elapsedRealtime()); // 현재시간과 마지막으로 클릭된 시간 차이 (한번 눌렀으니 0)
                        chronometer.start(); // 타이머 시작
                        running = true;
                    }

                } else if (videoRecordEvent instanceof VideoRecordEvent.Finalize) { // 녹화 끝나서
                    if (!((VideoRecordEvent.Finalize) videoRecordEvent).hasError()) { // 에러가 없다면
                        Log.e("TEST","Video finalized");
                        //sound.play(MediaActionSound.STOP_VIDEO_RECORDING);
                        chronometer.setBase(SystemClock.elapsedRealtime());
                        chronometer.stop();
                        chronometer.setVisibility(View.INVISIBLE);
                        //Log.e("TEST", "Chronometer stopped");

//                        Bitmap videoBitmap = getVideoFrame(((VideoRecordEvent.Finalize) videoRecordEvent).getOutputResults().getOutputUri(), SystemClock.elapsedRealtime());
//                        Bitmap grayBitmap = toGray(videoBitmap);
//                        Uri videoUri = ((VideoRecordEvent.Finalize) videoRecordEvent).getOutputResults().getOutputUri();
//                        List<Bitmap> frameList = null;
//                        try {
//                            frameList = extractFramesFromVideo(videoUri);
//                        } catch (IOException e) {
//                            throw new RuntimeException(e);
//                        }
//                        List<Bitmap> grayFrameList = new ArrayList<>();
//                        for (Bitmap frame : frameList) {
//                            Bitmap grayFrame = toGray(frame);
//                            grayFrameList.add(grayFrame);
//                        }
//                        //그레이프레임리스트에 회색 비트맵은 들어가있는 상황. 이제 이 비트맵을 비디오프레임에 넣어보자
//                        Uri grayVideoUri = createGrayVideoFile();
//                        try {
//                            encodeFramesToVideo(grayFrameList, grayVideoUri);
//                        } catch (Exception e) {
//                            throw new RuntimeException(e);
//                        }
                        //cleanup();

                        running = false;
//                        if(grayButton.isChecked()) {
//                            Log.e("TEST","grayButton checked finalize");
//                            //String savedVideoPath = options.getContentValues().getAsString(MediaStore.MediaColumns.DATA);
//                            String s = MediaStore.Video.Media.EXTERNAL_CONTENT_URI.getPath();
//                            Log.e("TEST","Video Path = " + s);
//                            String savedVideoPath = MediaStore.Video.Media.EXTERNAL_CONTENT_URI.toString() + "/" + options.getContentValues().getAsInteger(MediaStore.MediaColumns._ID);
//                            Log.e("TEST","savedVideoPath = " + savedVideoPath);
//                            String outputVideoPath = savedVideoPath.replace(".mp4", "_gray.mp4");
//                            Uri savedVideoUri = getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);
//                            String savedVideoPath = savedVideoUri.toString();
//                            String outputVideoPath = savedVideoPath.replace(".mp4", "_gray.mp4");
//
//                            String[] projection = {MediaStore.MediaColumns.DATA};
//                            Cursor cursor = getContentResolver().query(savedVideoUri, projection, null, null, null);
//                            if (cursor != null && cursor.moveToFirst()) {
//                                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
//                                String savedVideoPath = cursor.getString(columnIndex);
//                                String outputVideoPath = savedVideoPath.replace(".mp4", "_gray.mp4");
//                                cursor.close();
//                                // Use the obtained savedVideoPath as inputVideoPath in convertToGrayscale() method
//                                try {
//                                    convertToGrayscale(fileAddress, outputVideoPath);
//                                } catch (IOException e) {
//                                    throw new RuntimeException(e);
//                                }
//                            }
//
//                            try {
//                                Log.e("TEST","Going to convertToGrayscale");
//                                convertToGrayscale(savedVideoPath, outputVideoPath);
//                            } catch (IOException e) {
//                                throw new RuntimeException(e);
//                            }
//                        }else{
//                            Log.e("TEST","savedVideoPath = " + MediaStore.Video.Media.EXTERNAL_CONTENT_URI.buildUpon()
//                                    .appendPath(time + ".mp4")
//                                    .build().toString());
//                        }

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

//    private void convertToGrayscale(String inputVideoPath, String outputGrayVideoPath) throws IOException {
//        Log.e("TEST","convertToGrayScale Arrived");
//        // Load the input video
//        try {
//            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
//            Log.e("TEST","MediaMetaDataRetreiver made");
//            retriever.setDataSource(inputVideoPath);
//            Log.e("TEST","inputVideoPath check");
//
//            // Get the properties of the input video
//            String frameWidthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
//            String frameHeightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
//            String frameRateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE);
//            Log.e("TEST","get properties");
//
//            double frameWidth = Double.parseDouble(frameWidthStr);
//            double frameHeight = Double.parseDouble(frameHeightStr);
//            double frameRate = Double.parseDouble(frameRateStr);
//
//            VideoWriter videoWriter = new VideoWriter(outputGrayVideoPath, VideoWriter.fourcc('X', 'V', 'I', 'D'), frameRate, new Size(frameWidth, frameHeight), true);
//            Log.e("TEST","Create VideoWriter");
//
//            for (int frameIndex = 0; ; frameIndex++) {
//                Bitmap frameBitmap = retriever.getFrameAtTime(frameIndex * 1000000L / (long) frameRate, MediaMetadataRetriever.OPTION_CLOSEST); // 각 프레임 받아오기
//                if (frameBitmap == null) {
//                    break;
//                }
//                // 받은 프레임 회색으로 바꾸기
//                Bitmap grayBitmap = toGray(frameBitmap);
//                // 회색 비트맵 -> byteArray
//                byte[] grayByteArray = bitmapToByteArray(grayBitmap);
//                // byteArray -> 프레임 -> 비디오
//                videoWriter.write(new MatOfByte(grayByteArray));
//                Log.e("TEST","VideoWriter pass");
//            }
//
//            videoWriter.release();
//            retriever.release();
//        }catch (RuntimeException e) {
//            e.printStackTrace();
//            Log.e("TEST","Exception catch");
//        }
//    }


//    private Mat toGray(Mat frame) {
//        Mat grayFrame = new Mat();
//        Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
//        return grayFrame;
//    }



//    private List<Bitmap> extractFramesFromVideo(Uri videoUri) throws IOException {
//        List<Bitmap> frameList = new ArrayList<>();
//
//        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
//        try {
//            retriever.setDataSource(this, videoUri);
//            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
//            long durationUs = Long.parseLong(durationStr) * 1000;
//            long frameIntervalUs = 1000000;
//            for (long timeUs = 0; timeUs < durationUs; timeUs += frameIntervalUs) {
//                Bitmap frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
//                if (frame != null) {
//                    frameList.add(frame);
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            retriever.release();
//        }
//        return frameList;
//    }

//    private Uri createGrayVideoFile() {
//        File outputDir = getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES);
//        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
//        String fileName = "gray_video_" + timeStamp + ".mp4";
//        File outputFile = new File(outputDir, fileName);
//
//        // Return the Uri representing the file location
//        return Uri.fromFile(outputFile);
//    }

//    private void encodeFramesToVideo(List<Bitmap>frameList, File outpurFile){
//        MediaCodec encoder = null;
//        MediaMuxer muxer = null;
//
//    }

//    private void encodeFramesToVideo(List<Bitmap> frameList, File outputFile) {
//        try {
//            MediaCodecInfo codecInfo = selectVideoCodec("video/avc");
//            int width = frameList.get(0).getWidth();
//            int height = frameList.get(0).getHeight();
//
//            MediaCodec mediaCodec = MediaCodec.createEncoderByType("video/avc");
//            MediaFormat format = MediaFormat.createVideoFormat("video/avc", width, height);
//            format.setInteger(MediaFormat.KEY_BIT_RATE, 2000000);
//            format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
//            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
//            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
//            format.setInteger(MediaFormat.KEY_CAPTURE_RATE, 30);
//
//            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
//            Surface surface = mediaCodec.createInputSurface();
//            mediaCodec.start();
//
//            MediaMuxer mediaMuxer = new MediaMuxer(outputFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
//            int trackIndex = mediaMuxer.addTrack(format);
//            mediaMuxer.start();
//
//            for (Bitmap frame : frameList) {
//                Bitmap rotatedFrame = rotateBitmap(frame, rotation);
//                ByteBuffer inputBuffer = getInputBuffer(mediaCodec);
//                byte[] frameData = getBitmapData(rotatedFrame);
//                inputBuffer.clear();
//                inputBuffer.put(frameData);
//                mediaCodec.queueInputBuffer(index, 0, frameData.length, presentationTimeUs, 0);
//                presentationTimeUs += 1000000 / 30;
//            }
//
//            // Signal end of input
//            mediaCodec.queueInputBuffer(index, 0, 0, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
//
//            // Drain output buffers
//            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
//            int outputBufferIndex;
//            ByteBuffer outputBuffer;
//
//            while (true) {
//                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US);
//                if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
//                    break;
//                } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
//                    MediaFormat newFormat = mediaCodec.getOutputFormat();
//                    trackIndex = mediaMuxer.addTrack(newFormat);
//                    mediaMuxer.start();
//                } else if (outputBufferIndex >= 0) {
//                    outputBuffer = getOutputBuffer(mediaCodec, outputBufferIndex);
//                    mediaMuxer.writeSampleData(trackIndex, outputBuffer, bufferInfo);
//                    mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
//                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
//                        break;
//                    }
//                }
//            }
//
//            mediaCodec.stop();
//            mediaCodec.release();
//            mediaMuxer.stop();
//            mediaMuxer.release();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }



//    private void encodeFramesToVideo(List<Bitmap> frameList, Uri outputFileUri) throws FrameRecorder.Exception {
//        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
//        int width = displayMetrics.widthPixels;
//        int height = displayMetrics.heightPixels;
//
//        FFmpegFrameRecorder videoEncoder = new FFmpegFrameRecorder(outputFileUri.getPath(), width, height);
//        videoEncoder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
//        videoEncoder.setFrameRate(30); // Set frame rate if desired
//        videoEncoder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P); // Set pixel format
//
//        try {
//            videoEncoder.start();
//
//            for (Bitmap frame : frameList) {
//                Frame videoFrame = convertBitmapToFrame(frame);
//                videoEncoder.record(videoFrame);
//            }
//
//            videoEncoder.stop();
//        } catch (FrameRecorder.Exception e) {
//            e.printStackTrace();
//        } finally {
//            videoEncoder.release();
//        }
//    }
//
//    private Frame convertBitmapToFrame(Bitmap bitmap) {
//        Frame frame = new Frame(bitmap.getWidth(), bitmap.getHeight(), Frame.DEPTH_UBYTE, 4);
//        ByteBuffer buffer = ByteBuffer.allocate(bitmap.getByteCount());
//        bitmap.copyPixelsToBuffer(buffer);
//        buffer.rewind();
//        frame.image[0] = buffer;
//        return frame;
//    }

//    public static Bitmap getVideoFrame(Uri uri, long frameTime){
//        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
//        try {
//            retriever.setDataSource(String.valueOf(uri));
//            return retriever.getFrameAtTime(frameTime, MediaMetadataRetriever.OPTION_CLOSEST);
//        } catch (IllegalArgumentException ex) {
//            ex.printStackTrace();
//        }
//        return null;
//    }

//    private void processFrame(Bitmap frame) {
//        Bitmap grayFrame = toGray(frame);
//        saveGrayscaleFrame(frame);
//    }

//    public void captureGrayVideo(){
//        Log.e("TEST","CaptureGrayVideo arrived");
//        processCameraProvider.unbindAll();
//        Log.e("TEST","Going to bindVideo");
//        bindVideo();
//    }

//    public void saveGrayVideo(Bitmap grayFrame, int rotation, double frameWidth, double frameHeight) throws IllegalAccessException, NoSuchFieldException {
//        if (videoWriter == null) {
//            Log.e("TEST", "Save Video");
//            double frameRate = 30;
//            Log.e("TEST", "Get frames");
//            String time = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault()).format(System.currentTimeMillis());
//            String fileName = time + ".mp4";
//            int fourcc = VideoWriter.fourcc('M', 'P', '4', 'V');
//            Log.e("TEST","Create videoWriter");
//
////            System.setProperty("java.library.path", "C:\\pathToFolderContainingDLL");
////            Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
////            fieldSysPath.setAccessible(true);
////            fieldSysPath.set(null, null);
//
//            //System.loadLibrary("opencv_ffmpeg343_64");
//
//            videoWriter = new VideoWriter(fileName, fourcc, frameRate, new Size(frameWidth, frameHeight), false);
//        }else {
//            Log.e("TEST","videoWriter not null");
//            Bitmap rotated = rotateBitmap(grayFrame, rotation);
//            Log.e("TEST", "Set videoWriter");
//            byte[] grayByteArray2 = bitmapToByteArray(rotated);
//            Log.e("TEST", "Gray bitmap to byteArray");
//            videoWriter.write(new MatOfByte(grayByteArray2));
//            Log.e("TEST", "videoWrite matOfByte");
//        }
//    }


    void bind() {
        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(cameraFacing).build(); // 카메라 방향 지정
        Preview preview = new Preview.Builder().build(); // 프리뷰 만들기
        preview.setSurfaceProvider(previewView.getSurfaceProvider()); //만든 프리뷰를 previewView에 연결하기
        Log.e("TEST","PreviewView size: width = " + previewView.getMeasuredWidth() + "height = " + previewView.getMeasuredHeight());
        imageCapture = new ImageCapture.Builder().build(); // 캡쳐하는 클래스 빌드
        Recorder recorder = new Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HIGHEST)).build(); // 새로운 퀄리티의 recorder 생성
        videoCapture = VideoCapture.withOutput(recorder); // recorder: VideoCapture과 결합된 VideoOutput의 구현. 동영상 및 오디오 캡쳐를 실행하는데 사용됨
        //videoCapture = 화면에 띄우는 역할
        //recorder = 화면에 띄워지는 비디오를 저장하는 역할
        ResolutionSelector.Builder selectorBuilder = new ResolutionSelector.Builder();
        selectorBuilder.setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY);
        imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setResolutionSelector(selectorBuilder.build())
                .build();
        imageAnalysis.setAnalyzer(getMainExecutor(), this);
//        if (grayButton.isChecked()) {
//            ResolutionSelector.Builder selectorBuilder = new ResolutionSelector.Builder();
//            selectorBuilder.setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY);
//            imageAnalysis = new ImageAnalysis.Builder()
//                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                    .setResolutionSelector(selectorBuilder.build())
//                    .build();
//            imageAnalysis.setAnalyzer(getMainExecutor(), new ImageAnalysis.Analyzer() {
//                @Override
//                public void analyze(@NonNull ImageProxy image) {
//                    Bitmap bitmap = image.toBitmap();
//                    image.close();
//                    Bitmap grayFrame = toGray(bitmap);
//                    processFrame(grayFrame);
//                }
//            });
        camera = processCameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, videoCapture, imageAnalysis);
        // 선택한 카메라와 사용사례를 카메라 수명주기(카메라를 여는 시점, 캡쳐 세션을 생성할 시점, 중지 및 종료 시점) 연결. 수명주기전환에 맞춰 카메라 상태가 적절히 변경될 수 있음
    }


//    void bindVideo() {
//        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(cameraFacing).build();
////        Preview preview = new Preview.Builder().build();
////        preview.setSurfaceProvider(previewView.getSurfaceProvider());
////        imageCapture = new ImageCapture.Builder().build();
//        Recorder recorder = new Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HIGHEST)).build();
//        videoCapture = VideoCapture.withOutput(recorder);
//
////            ResolutionSelector.Builder selectorBuilder = new ResolutionSelector.Builder();
////            selectorBuilder.setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY);
//            imageAnalysis = new ImageAnalysis.Builder()
//                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                    //.setResolutionSelector(selectorBuilder.build())
//                    .build();
//            imageAnalysis.setAnalyzer(getMainExecutor(), new ImageAnalysis.Analyzer() {
//                @Override
//                public void analyze(@NonNull ImageProxy image) {
//                    Log.e("TEST","Analyze Video");
//                    Bitmap bitmap = image.toBitmap();
//                    image.close();
//                    Log.e("TEST","Bitmap width = " + bitmap.getWidth() + "bitmap height = " + bitmap.getHeight());
//                    Bitmap grayFrame = toGray(bitmap);
//                    //processFrame(grayFrame);
//                    //saveGrayscaleFrame(grayFrame);
//                    double frameWidth = image.getWidth();
//                    double frameHeight = image.getHeight();
//                    rotation = image.getImageInfo().getRotationDegrees();
//                    if(isGrayRecording) {
//                        try {
//                            saveGrayVideo(grayFrame, rotation, frameWidth, frameHeight);
//                        } catch (IllegalAccessException e) {
//                            throw new RuntimeException(e);
//                        } catch (NoSuchFieldException e) {
//                            throw new RuntimeException(e);
//                        }
//                    }
//                    //byte[]grayByteArray2 = bitmapToByteArray(rotated);
//                    //videoWriter.write(new MatOfByte(grayByteArray2));
//                }
//            });
//            camera = processCameraProvider.bindToLifecycle(this, cameraSelector, videoCapture, imageAnalysis);
//    }


    @Override
    public void analyze(@NonNull ImageProxy image) {
        Bitmap bitmap = image.toBitmap();
        Log.e("TEST","Bitmap height = " + bitmap.getHeight() + " width = " + bitmap.getWidth() + " rotation = "+image.getImageInfo().getRotationDegrees());
        image.close();
        rotation = image.getImageInfo().getRotationDegrees();
        if(grayMode.isChecked()){
            Bitmap gray = toGray(bitmap);
            Bitmap rotated = rotateBitmap(gray, rotation);
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            int screenWidth = displayMetrics.widthPixels;
            int screenHeight = displayMetrics.heightPixels;
            Log.e("TEST","Gray screen size: width = " + overPreview.getWidth() + "height = " + overPreview.getHeight());
            float scaleX = (float) screenWidth / rotated.getWidth();
            float scaleY = (float) screenHeight / rotated.getHeight();
            float scale = Math.min(scaleX, scaleY);
            Matrix matrix = new Matrix();
            matrix.setScale(scale, scale);
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(rotated, screenWidth, screenHeight, true);
            Log.e("TEST","Resized bitmap size: width = " + resizedBitmap.getWidth() + "height = " + resizedBitmap.getHeight());
            BitmapFactory.Options options = new BitmapFactory.Options();
            overPreview.setImageBitmap(rotated);
        } else if(houghMode.isChecked()){
            Bitmap houghBitmap = toHough(bitmap);
            Bitmap rotated = rotateBitmap(houghBitmap, rotation);
            overPreview.setImageBitmap(rotated);
        }else if(cannyMode.isChecked()){
            Bitmap cannyBitmap = toCanny(bitmap);
            Bitmap rotated = rotateBitmap(cannyBitmap, rotation);
            overPreview.setImageBitmap(rotated);
        }else if(faceDetection.isChecked()){

            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                Bitmap faceBitmap = faceDetect(bitmap);
                Bitmap rotated = rotateBitmap(faceBitmap, rotation);
                overPreview.setImageBitmap(faceBitmap);
            }else if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
                Bitmap faceBitmap = faceDetect(bitmap);
                //Bitmap rotated = rotateBitmap(faceBitmap, rotation);
                overPreview.setImageBitmap(faceBitmap);
            }

//            Bitmap faceBitmap = faceDetect(bitmap);
//            rotation = image.getImageInfo().getRotationDegrees();
//            Bitmap rotated = rotateBitmap(faceBitmap, rotation);
//            overPreview.setImageBitmap(faceBitmap);
        }else{
            overPreview.setVisibility(View.INVISIBLE);
        }
    }

    private String copyXmlToPrivateStorage(String filepath) {
        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
        File cascadeFile = new File(cascadeDir, filepath);

        if (cascadeFile.exists()) {
            return cascadeFile.getAbsolutePath();
        }

        try {
            AssetManager assetManager = getAssets();
            InputStream inputStream = assetManager.open(filepath);
            FileOutputStream outputStream = new FileOutputStream(cascadeFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            inputStream.close();
            outputStream.close();
            return cascadeFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Bitmap faceDetect(Bitmap bitmap){
        Bitmap rotated = rotateBitmap(bitmap, rotation);
        byte[]in = bitmapToByteArray(rotated);
        int width = rotated.getWidth();
        int height = rotated.getHeight();
        Log.e("TEST", "Main width: " + width + "height: " + height);

        face = new byte[width * height * 4];
        faceBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        String cascadeFileName = "haarcascade_frontalface_default.xml";
        String copiedPath = copyXmlToPrivateStorage(cascadeFileName);

        detectFace(in, face, width, height, copiedPath);
        ByteBuffer buffer = ByteBuffer.wrap(face);
        faceBitmap.copyPixelsFromBuffer(buffer);
        return faceBitmap;
    }

    private Bitmap toCanny(Bitmap bitmap){
        byte[]in = bitmapToByteArray(bitmap);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        cannyOut = new byte[width * height * 4];
        cannyOutBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        drawCanny(in, cannyOut, width, height);
        ByteBuffer buffer = ByteBuffer.wrap(cannyOut);
        cannyOutBitmap.copyPixelsFromBuffer(buffer);
        return cannyOutBitmap;
    }

    private Bitmap toHough(Bitmap bitmap){
        byte[]in = bitmapToByteArray(bitmap);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        houghOut = new byte[width * height * 4];
        houghOutBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        drawHough(in, houghOut, width, height);
        //Log.e("TEST","Hough Bitmap size: width = " + width + "height = " + height);
        ByteBuffer buffer = ByteBuffer.wrap(houghOut);
        houghOutBitmap.copyPixelsFromBuffer(buffer);
        return houghOutBitmap;
    }

    private Bitmap toGray(Bitmap bitmap){
        byte[]in = bitmapToByteArray(bitmap); //비트맵을 바이트어레이로 변환(네이티브로 넘기기 위해)
        int width = bitmap.getWidth(); //비트맵 가로
        int height = bitmap.getHeight(); //세로 사이즈
        //Log.e("TEST","Gray Bitmap size: width = " + bitmap.getWidth() + "height = " + bitmap.getHeight());
        out = new byte[width * height * 4]; // 비트맵 사이즈에 r,g,b,a 들어가니 곱하기 4
        outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//        if(out == null)
//        {
//            //out = new byte[width * height;
//            out = new byte[width * height * 4];
//        }
//        //Log.e("TEST","bitmap height = " + (bitmap.getHeight() + "* bitmap width = " + bitmap.getWidth()));
//        if(outBitmap == null)
//        {
//            //outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8); // 문제점: 회색이 아닌 색깔있는 프리뷰에 회색을 덧씌운것처럼 나옴.
//            //비트맵을 카피시켜보는 방법을 찾아볼것
//            outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//        }
        //Log.e("TEST","width = " + bitmap.getWidth() + "height = " + bitmap.getHeight());
        ConvertRGBtoGray_withoutCV(in, out, width, height); // in 바이트어레이 넘겨서 out에 받는다
//        int quarter = out.length / 4;
//        ByteBuffer buffer = ByteBuffer.wrap(out,0,quarter);
        ByteBuffer buffer = ByteBuffer.wrap(out); // out 바이트 어레이를 감싸는 버퍼 만들고
        outBitmap.copyPixelsFromBuffer(buffer); // outBitmap에 복사
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

//    private final ImageAnalysis.Analyzer videoFrameAnalyzer = new ImageAnalysis.Analyzer() {
//        @Override
//        public void analyze(@NonNull ImageProxy image) {
//
//            // Get the properties of the input video
//            //String frameWidthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
//            //String frameHeightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
//            //String frameRateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE);
//            Log.e("TEST","get properties");
//
//            double frameWidth = image.getWidth();
//            double frameHeight = image.getHeight();
//            double frameRate = 30;
//            Log.e("TEST","Get frames");
//            String time = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault()).format(System.currentTimeMillis());
//            String fileName = time + ".mp4";
//            VideoWriter videoWriter = new VideoWriter(fileName, VideoWriter.fourcc('X', 'V', 'I', 'D'), frameRate, new Size(frameWidth, frameHeight), false);
//            Log.e("TEST","videoWriter");
//            Bitmap bitmap = image.toBitmap();
//            //Log.e("TEST","Bitmap height = " + bitmap.getHeight() + "width = " + bitmap.getWidth());
//            image.close();
//            Bitmap gray = toGray(bitmap);
//            rotation = image.getImageInfo().getRotationDegrees();
//            Bitmap rotated = rotateBitmap(gray, rotation);
//            byte[]grayByteArray2 = bitmapToByteArray(rotated);
//            videoWriter.write(new MatOfByte(grayByteArray2));
//        }
//    };

    @Override
    protected void onDestroy() { // 화면이 소멸했을때 실행:
        super.onDestroy();  // 액티비티 종료
    }
}
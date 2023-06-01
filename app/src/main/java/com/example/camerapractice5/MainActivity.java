package com.example.camerapractice5;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.media.Image;
import android.media.MediaActionSound;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Rational;
import android.util.Size;
import android.view.MotionEvent;
import android.view.View.OnTouchListener;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.FocusMeteringAction;
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
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.util.Consumer;

import com.example.camerapractice5.R;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.SystemClock;
import android.widget.Chronometer;

public class MainActivity extends AppCompatActivity { // 하위버전 단말기에 실행 안되는 메소드를 지원하기 위해 AppCompatActivity를 extend함
    //버튼이나 필요한 API들 선언하기
    Recording recording = null; // 실제 녹화를 실행함
    Chronometer chronometer;
    boolean running = false;
    MediaActionSound sound = new MediaActionSound(); // 여러 소리를 냄
    VideoCapture<Recorder> videoCapture = null; //카메라가 비디오프레임을 구성하게함
    Button record, picture, flipCamera; // 만든 버튼들
    TextView timerText;
    PreviewView previewView; // 카메라에 비치는 화면의 역할
    ImageView imageView; // 이미지를 화면에 띄우기 위해서

    Camera camera;
    ImageCapture imageCapture; // 사진을 캡쳐할 수 있도록 기본 컨트롤을 제공
    ProcessCameraProvider processCameraProvider; // 수명주기와 연결하여 기본적인 카메라 접근을 부여함(카메라가 핸드폰에 있는지, 카메라 정보등)
    int cameraFacing = CameraSelector.LENS_FACING_BACK; // 디폴트: 카메라 후면

    @Override
    protected void onCreate(Bundle savedInstanceState) { // onCreate: 액티비티가 생성될때 호출되며 사용자 인터페이스(클래스가 구현해야할 행동을 지정함) 초기화에 사용
        super.onCreate(savedInstanceState); // super class 호출 (activity를 구현하는데 필요한 과정) savedInstanceState = 화면 구성이 변경될때 (가로모드, 세로모드 전환 / 언어/ 입력기기)
        setContentView(R.layout.activity_main); // layout에 있는 activity_main.xml로 화면 정의

        previewView = findViewById(R.id.viewFinder); // findViewById = activity_main.xml에서 설정된 뷰를 가져오는 메소드
        record = findViewById(R.id.record);
        picture = findViewById(R.id.picture);
        flipCamera = findViewById(R.id.flipCamera);
        imageView = findViewById(R.id.imageView);
        chronometer = findViewById(R.id.chronometer);
        chronometer.setFormat("%s");
        chronometer.setBackgroundColor(Color.RED);

        try {
            processCameraProvider = ProcessCameraProvider.getInstance(this).get();
            // 전에 썼던 코드:
            /* processCameraProvider에 인스턴스 바로 넣기
            public void startCamera(int cameraFacing) {
        ListenableFuture<ProcessCameraProvider> future_processCameraProvider = ProcessCameraProvider.getInstance(MainActivity.this); //지금 액티비티의 ProcessCameraProvider을 회수함

        future_processCameraProvider.addListener(() -> { //startCamera 이벤트가 발생하면 리스너에게 이벤트를 알려주고 아래 기능을 구현한다. 지금 버튼에 호출함수가 아니기 때문에 AddListener로 연결시켜주는거임
            try {
                processCameraProvider = future_processCameraProvider.get(); //카메라의 생명주기를 액티비티와 같은 생명주기에 결합시킴
            } catch (ExecutionException | InterruptedException e) { // 이런 예외들이 발생한다면
                e.printStackTrace(); // 애러 메세지의 발생 근원지를 찾아서 단계별로 에러를 출력해라
            }
        }, ContextCompat.getMainExecutor(MainActivity.this)); // 카메라는 메인스레드에서 실행을 하기 때문에 MainExecutor에서 받아옴
    }
             */
        }
        catch (ExecutionException e) {
            e.printStackTrace();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) { // 권한 체크
            activityResultLauncher.launch(Manifest.permission.CAMERA);
        //} else if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
        //    activityResultLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }else {
            bind(); // 권한 부여 받았다면 카메라 연결
            //startCamera(cameraFacing); // 권한 부여받았다면 카메라 시작 함수 호출
            // 여기에 써야하는 이유는 startCamera에서 processCameraProvider를 정의하고 나중에 시작 버튼 클릭되어 바인딩할때 processCamera가 필요하기 때문
            //바로 위에 써도 안됨. 만들어지는데 시간이 걸리기 때문
        }

        /*
        previewView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    float x = event.getX();
                    float y = event.getY();
                    triggerFocusMetering(x, y);
                }
                return true;
            }
        });
         */
        previewView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) { // onTouch는 boolean이여야함
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN: // 손가락으로 눌렀을때
                        return true;
                    case MotionEvent.ACTION_UP: // 눌렀다 땠을때
                        MeteringPointFactory factory = previewView.getMeteringPointFactory(); // MeteringPoint를 만듣는 곳
                        MeteringPoint point = factory.createPoint(motionEvent.getX(), motionEvent.getY()); // MeterinPoint: 카메라의 지점. 그 지점을 x,y 좌표로 나타냄
                        FocusMeteringAction action = new FocusMeteringAction.Builder(point).build(); // 찍은 좌표에 포커스 맞추기
                        CameraControl cameraControl = camera.getCameraControl(); // CameraControl 기능: 확대/축소, 초점, 노출 보정
                        cameraControl.startFocusAndMetering(action);
                        return true;
                    default:
                        return false;
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
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) { // 권한 확인
                    activityResultLauncher.launch(Manifest.permission.CAMERA); // 권한을 부여 받지 못했다면 다시 요청
                } else {
                    sound.play(MediaActionSound.SHUTTER_CLICK); // 셔터 사운드
                    imageCapture.takePicture(ContextCompat.getMainExecutor(MainActivity.this), // 메인스레드에서 작동할 것이다
                            new ImageCapture.OnImageCapturedCallback() {  // 이미지 캡쳐가 완료되면 콜백 (콜백:어떤 조건이 충족되면(이벤트가 발생하면) 이 코드 처리를 해라)
                                @Override
                                public void onCaptureSuccess(@NonNull ImageProxy image) { // close하는(끝내는) 콜백 (여기서 @NonNull ImageProxy image = 캡쳐된 이미지
                                    @SuppressLint({"UnsafeExperimentalUsageError", "UnsafeOptInUsageError"}) // UnsafeExperimentalUsageError와 UnsafeOptInUsageError 검사 항목을 건너 뛰어라
                                    Image mediaImage = image.getImage(); // mediaImage = 캡쳐된 이미지
                                    Bitmap bitmap = ImageUtil.mediaImageToBitmap(mediaImage); //만들어둔 ImageUtil의 이미지를 비트맵으로 변환시키는 메소드를 씀
                                    float rotationDegrees = image.getImageInfo().getRotationDegrees(); // 회전시켜야할 각도
                                    Bitmap rotatedBitmap = ImageUtil.rotateBitmap(bitmap, rotationDegrees); // 그 각도만큼 회전시킴
                                    //Bitmap rotatedBitmap = ImageUtil.rotateBitmap(bitmap[0], image.getImageInfo().getRotationDegrees()); //그냥 mediaImage를 이미지뷰에 넣으면 회전된 각도로 나옴
                                    imageView.setImageBitmap(rotatedBitmap); // 이미지뷰에 비트맵을 로드해서 출력한다
                                    saveImage(rotatedBitmap); // 저장하는 함수 호출
                                }
                            }
                    );
                }

            }
        });

        record.setOnClickListener(view -> {
            //카메라, 오디오, 외부저장소 권한 체크
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) { // 권한 확인
                activityResultLauncher.launch(Manifest.permission.CAMERA); // 권한을 부여 받지 못했다면 다시 요청
            } else if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                activityResultLauncher.launch(Manifest.permission.RECORD_AUDIO);
            //} else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //    activityResultLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            } else {
                captureVideo(); // 모든 권한이 있다면 녹화하는 함수 호출
            }
        });

    }

    private final ActivityResultLauncher<String> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
        //람다 호출 방식: (매개변수, ...) -> {실행문}
        //activityResultLauncher = 활동을 시작하고 다시 결과를 받는다 (여기서는 권한을 부여받았는지 확인하는 용도)
        //A 액티비티가 B액티비티 결과에 따라 실행하는 프로그램이 달라질때 여기서 결과를 확인할 수 있음
        //registerForActivityResult = 다른 액티비티를 실행하는데 사용할 ActivityResultLauncher 반환
        if (result) { // result(boolean) = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            //MainActivity.this = 앱의 현재 상태 또는 흘러가는 맥락 / Manifst.permission.CAMERA = 필요한 권한 명칭
            //권한을 이미 부여 받았다면 요청을 다시 하지 않는다 호출 결과: PERMISSION_GRANTED(권한 있음) 또는 PERMISSION_DENITED (권한 없음)
            //startCamera(cameraFacing);
            bind();
        }
    });
/*
    private void triggerFocusMetering(float x, float y) {
        if (camera != null) {
            CameraControl cameraControl = camera.getCameraControl();
            DisplayMetrics metrics = new DisplayMetrics();
            previewView.getDisplay().getRealMetrics(metrics);

            float sensorX = x / previewView.getWidth();
            float sensorY = y / previewView.getHeight();

            int rotation = previewView.getDisplay().getRotation();
            if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
                float tmp = sensorX;
                sensorX = sensorY;
                sensorY = tmp;
            }

            Rational rational = new Rational(metrics.widthPixels, metrics.heightPixels);
            Size screenSize = new Size(metrics.widthPixels, metrics.heightPixels);
            Rational sensorAspectRatio = new Rational(previewView.getWidth(), previewView.getHeight());

            MeteringPointFactory factory = previewView.getMeteringPointFactory();
            MeteringPoint point = factory.createPoint(sensorX, sensorY, 1.0f);

            FocusMeteringAction action = new FocusMeteringAction.Builder(
                    point, FocusMeteringAction.FLAG_AF | FocusMeteringAction.FLAG_AE)
                    .build();

            cameraControl.startFocusAndMetering(action);
        }
    }
*/


    public void saveImage(Bitmap rotatedBitmap){
        Uri images; // Uri = 리소스(외부 앱, 이미지, 택스트 등)에 접근할 수 있는 식별자 역할 (주소)
        ContentResolver contentResolver = getContentResolver(); // 컨텐츠에 엑세스를 줌 (데이터를 읽는다). 기능: 생성, 검색, 업데이트 및 삭제
        images = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY); //ContentResolver를 통해 이미지를 넣어주고 해당 위치의 Uri를 받는다

        String time = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault()).format(System.currentTimeMillis()); // 기기의 지역을 판별하고 현재 시간을 저장
        ContentValues contentValues = new ContentValues(0); // ContentValues: 아래정보들의 이름과 값을 관리하기 위해 만들어진 객체
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, time+ ".JPG"); // 파일 이름
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, "images/"); // MIME_TYPE = 데이터가 어떤 형식인지 ex.text / image / audio / video / application
        contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Pictures/CameraX-Images");
        Uri uri = contentResolver.insert(images, contentValues); // 주소 지정

        try{
            OutputStream outputStream = contentResolver.openOutputStream(Objects.requireNonNull(uri)); // Outputstream = 파일에 데이터 쓰기, uri는 null이면 안됨
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG,100, outputStream); // OutputStream은 비트맵 이미지를 저장하기 위한 객체를 받는다. outputStream이 가지고있는 uri 주소에 저장
            //비트맵의 용량이 너무 커서 저장할때는 압축을 시켰다가 화면에 띄울때 다시 100%로 보여준다는 뜻
            Toast.makeText(getApplicationContext(), "갤러리에 저장 성공", Toast.LENGTH_LONG).show();

        }catch(Exception e){ // 예외 케이스
            e.printStackTrace();
        }
    }

    public void captureVideo() {
        Log.e("TEST","Capture Video Button Clicked");
        Recording recording1 = recording; // recording1이라는 변수에 recording값을 넣음


        //녹화 버튼을 두번째 눌렀다는것은 녹화를 멈추고 저장하고싶다는 뜻이니
        if (recording1 != null) { // 만약 지금 실행되고있는 녹화가 있다면
            Log.e("TEST","recording1 not null");
            recording1.stop(); // 멈추고
            recording = null; // recording값에 다시 null
        }

        String time = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault()).format(System.currentTimeMillis());
        Log.e("TEST","Check time");
        ContentValues contentValues = new ContentValues(); // ContentValues:이름과 값을 관리하는 객체
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, time);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
        contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video");

        //MediaStoreOutputOptions = 아웃풋(비디오)를 MediaStore에 저장하는 옵션 (여기는 외부 저장소에)
        MediaStoreOutputOptions options = new MediaStoreOutputOptions.Builder(getContentResolver(), MediaStore.Video.Media.EXTERNAL_CONTENT_URI).setContentValues(contentValues).build(); // contentValues = contentResolver가 처리하는 값
        Log.e("TEST","Media Store");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) { // 오디오 권한 체크하고 (아래 videoCapture이 권한채크를 요구함)
            return; // 오디오 권한을 부여받지 못했다면 돌아가기
        }

        recording = videoCapture.getOutput().prepareRecording(MainActivity.this, options).withAudioEnabled().start(ContextCompat.getMainExecutor(MainActivity.this), new Consumer<VideoRecordEvent>() {
            //recording에 캡쳐된 비디오 담기
            @Override
            public void accept(VideoRecordEvent videoRecordEvent) {
                Log.e("TEST","video accepted " + videoRecordEvent);
                //recording 계속 실행 (accept 함수로 인해 Finalize 될때까지 돌아감)
                if (videoRecordEvent instanceof VideoRecordEvent.Start) { // 녹화 시작
                    record.setEnabled(true); // record 시작
                    sound.play(MediaActionSound.START_VIDEO_RECORDING);
                    if(!running){ // 디폴트: false
                        chronometer.setBase(SystemClock.elapsedRealtime()); // 현재시간과 마지막으로 클릭된 시간 차이 (한번 눌렀으니 0)
                        chronometer.start(); // 타이머 시작
                        running = true;
                        Log.e("TEST","Chronometer started");
                    }
                } else if (videoRecordEvent instanceof VideoRecordEvent.Finalize) { // 녹화 끝나서
                    if (!((VideoRecordEvent.Finalize) videoRecordEvent).hasError()) { // 에러가 없다면
                        recording.close();
                        sound.play(MediaActionSound.STOP_VIDEO_RECORDING);
                        chronometer.setBase(SystemClock.elapsedRealtime());
                        chronometer.stop();
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
        previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER); //이미지의 가로, 세로 중 긴 쪽을 ImageView의 레이아웃에 맞춰출력함 (이미지 비율은 유지)
        CameraSelector cameraSelector = new CameraSelector.Builder() // 카메라 방향 지정
                .requireLensFacing(cameraFacing)
                .build();
        Preview preview = new Preview.Builder() // 프리뷰 만들기
                .build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider()); //만든 프리뷰를 previewView에 연결하기
        imageCapture = new ImageCapture.Builder() // 캡쳐하는 클래스 빌드
                .build();
        Recorder recorder = new Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST)) // 새로운 퀄리티의 recorder 생성
                .build();
        videoCapture = VideoCapture.withOutput(recorder); // recorder: VideoCapture과 결합된 VideoOutput의 구현. 동영상 및 오디오 캡쳐를 실행하는데 사용됨.
        //videoCapture = 화면에 띄우는 역할
        //recorder = 화면에 띄워지는 비디오를 저장하는 역할

        // 선택한 카메라와 사용사례를 카메라 수명주기(카메라를 여는 시점, 캡쳐 세션을 생성할 시점, 중지 및 종료 시점) 연결. 수명주기전환에 맞춰 카메라 상태가 적절히 변경될 수 있음
        processCameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, videoCapture);
    }

    @Override
    protected void onDestroy() { // 화면이 소멸했을때 실행:
        super.onDestroy(); // 액티비티 종료
    }
}
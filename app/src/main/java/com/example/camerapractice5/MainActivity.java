package com.example.camerapractice5;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.ImageView;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
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

import com.example.camerapractice5.R;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity {
    ExecutorService service;
    Recording recording = null;
    VideoCapture<Recorder> videoCapture = null;
    Button record, picture, flipCamera, start;
    PreviewView previewView;
    ImageView imageView;
    ImageCapture imageCapture;
    ProcessCameraProvider processCameraProvider;
    int cameraFacing = CameraSelector.LENS_FACING_BACK;
    private final ActivityResultLauncher<String> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera(cameraFacing);
        }
    });

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) { // 이 앱에서 카메라를 호출하고 그 결과를 다시 앱으로 가져옥ㄹ때
        super.onActivityResult(requestCode, resultCode, data);
        Log.e("TEST", "onActivityResult");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.viewFinder);
        record = findViewById(R.id.record);
        picture = findViewById(R.id.picture);
        start = findViewById(R.id.start);
        flipCamera = findViewById(R.id.flipCamera);
        imageView = findViewById(R.id.imageView);

        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, 1); // 권한 요청 코드 (1 = request code / single permission 하나)


        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { // 사용자가 클릭한 위젯이 view 매개변수 들어감
                //Log.e("TEST", "startButton onClick called");
                if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) { // 권한을 부여받았다면
                    //Log.e("TEST", "startButton onClick permission granted");
//                    Log.e("TEST", "startButton onclick START, calling startCamera()...");
                    Log.e("TEST", "startButton onclick startCamera finished, using processCameraProvider...");
                    processCameraProvider.unbindAll(); // 수명주기에 있는 액티비티 모두 카메라X에서 해제
                    bindPreview();
                    bindImageCapture();
                    bindVideoCapture();
                }
            }
        });

        record.setOnClickListener(view -> {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                activityResultLauncher.launch(Manifest.permission.CAMERA);
            } else if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                activityResultLauncher.launch(Manifest.permission.RECORD_AUDIO);
            } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                activityResultLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            } else {
                captureVideo();
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
                processCameraProvider.unbindAll(); // 아래 코드들로 변경된 방향으로 새로운 카메라 뷰 생성하기
                bindPreview();
                bindImageCapture();
            }
        });

        picture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("TEST","picture Button Clicked");
                imageCapture.takePicture(ContextCompat.getMainExecutor(MainActivity.this),
                        new ImageCapture.OnImageCapturedCallback() {  // 이미지 캡쳐가 완료되면 콜백 (콜백:어떤 조건이 충족되면(이벤트가 발생하면) 이 코드 처리를 해라)
                            @Override
                            public void onCaptureSuccess(@NonNull ImageProxy image) { // close하는(끝내는) 콜백 (여기서 @NonNull ImageProxy image = 캡쳐된 이미지
                                //Log.e("TEST", "takePicture onCaptureSuccess");
                                @SuppressLint({"UnsafeExperimentalUsageError", "UnsafeOptInUsageError"})
                                Image mediaImage = image.getImage();
                                Bitmap[] bitmap = {ImageUtil.mediaImageToBitmap(mediaImage)};
                                Bitmap rotatedBitmap = ImageUtil.rotateBitmap(bitmap[0], image.getImageInfo().getRotationDegrees());
                                //Bitmap rotatedBitmap = ImageUtil.mediaImageToBitmap(mediaImage); 그냥 mediaImage를 이미지뷰에 넣으면 회전된 각도로 나옴
                                imageView.setImageBitmap(rotatedBitmap); // 이미지뷰에 비트맵을 로드해서 출력한다
                                saveImage();
                                Log.e("TEST","saveImage called");
                            }
                        }
                );

            }
        });

        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            activityResultLauncher.launch(Manifest.permission.CAMERA);
        } else {
            Log.e("TEST","call startCamera");
            startCamera(cameraFacing);
        }
    }

    public void saveImage(){
        Uri images;
        ContentResolver contentResolver = getContentResolver();
        images = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);

        ContentValues contentValues = new ContentValues(0);
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, System.currentTimeMillis()+ ".JPG");
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, "images/");
        Uri uri = contentResolver.insert(images, contentValues);

        try{
            BitmapDrawable bitmapDrawable = (BitmapDrawable) imageView.getDrawable();
            Bitmap bitmap = bitmapDrawable.getBitmap();

            OutputStream outputStream = contentResolver.openOutputStream(Objects.requireNonNull(uri));
            bitmap.compress(Bitmap.CompressFormat.JPEG,100, outputStream);
            Objects.requireNonNull(outputStream);
            Toast.makeText(getApplicationContext(), "갤러리에 저장 성공", Toast.LENGTH_LONG).show();

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void captureVideo() {
        //capture.setImageResource(R.drawable.round_stop_circle_24);
        Recording recording1 = recording;
        if (recording1 != null) {
            recording1.stop();
            recording = null;
            return;
        }
        String name = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault()).format(System.currentTimeMillis());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
        contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video");

        MediaStoreOutputOptions options = new MediaStoreOutputOptions.Builder(getContentResolver(), MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                .setContentValues(contentValues).build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        recording = videoCapture.getOutput().prepareRecording(MainActivity.this, options).withAudioEnabled().start(ContextCompat.getMainExecutor(MainActivity.this), videoRecordEvent -> {
            if (videoRecordEvent instanceof VideoRecordEvent.Start) {
                record.setEnabled(true);
            } else if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
                if (!((VideoRecordEvent.Finalize) videoRecordEvent).hasError()) {
                    String msg = "Video capture succeeded: " + ((VideoRecordEvent.Finalize) videoRecordEvent).getOutputResults().getOutputUri();
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                } else {
                    recording.close();
                    recording = null;
                    String msg = "Error: " + ((VideoRecordEvent.Finalize) videoRecordEvent).getError();
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                }
                //capture.setImageResource(R.drawable.round_fiber_manual_record_24);
            }
        });
    }

    public void startCamera(int cameraFacing) {
        ListenableFuture<ProcessCameraProvider> future_processCameraProvider = ProcessCameraProvider.getInstance(MainActivity.this); //지금 액티비티의 ProcessCameraProvider을 회수함

        future_processCameraProvider.addListener(() -> {
            try {
                processCameraProvider = future_processCameraProvider.get(); //카메라의 생명주기를 액티비티와 같은 생명주기에 결합시킴
                Log.e("TEST", "startCamera future get complete");
                processCameraProvider.unbindAll();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(MainActivity.this));
    }


    void bindPreview() {
        previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER); //이미지의 가로, 세로 중 긴 쪽을 ImageView의 레이아웃에 맞춰출력함 (이미지 비율은 유지)

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(cameraFacing)
                .build();

        //cameraSelector = CameraSelector.LENS_FACING_BACK;
        Preview preview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3) //디폴트 표준 비율
                .build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        processCameraProvider.bindToLifecycle(this, cameraSelector, preview);
        //Log.e("TEST", "bindPreview SUCC");
    }

    void bindImageCapture() {
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(cameraFacing)
                .build();
        imageCapture = new ImageCapture.Builder()
                .build();

        processCameraProvider.bindToLifecycle(this, cameraSelector, imageCapture); // 카메라 생명주기 연결
        //Log.e("TEST", "bindImageCapture SUCC");
    }

    void bindVideoCapture(){
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(cameraFacing)
                .build();
        Recorder recorder = new Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST)) // 새로운 퀄리티의 recorder 생성
                .build();
        videoCapture = VideoCapture.withOutput(recorder);

        processCameraProvider.bindToLifecycle(this, cameraSelector, videoCapture);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        service.shutdown();
    }
}
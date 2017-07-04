package com.colorful.camera.view;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.colorful.camera.R;
import com.colorful.camera.assistant.constant.CameraConstant;
import com.colorful.camera.assistant.utils.Util;
import com.colorful.camera.ui.AutoFitTextureView;
import com.colorful.camera.ui.CircleButton;
import com.tbruyelle.rxpermissions.RxPermissions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.dxjia.ffmpeg.library.FFmpegNativeHelper;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import static com.colorful.camera.assistant.utils.Util.getMediaFileName;
import static com.colorful.camera.assistant.utils.Util.showToastMsg;

/**
 * Created by sg on 2017/5/24.
 */

public class MainActivity extends AppCompatActivity implements View.OnLongClickListener, View.OnTouchListener {


    @Bind(R.id.textureview)
    AutoFitTextureView mTextureView;
    @Bind(R.id.btn_select_picture)
    ImageView btnSelectPicture;
    @Bind(R.id.btn_flash)
    ImageView btnFlash;
    @Bind(R.id.btn_count_down)
    ImageView btnCountDown;
    @Bind(R.id.btn_take_photo)
    CircleButton btnTakePhoto;
    @Bind(R.id.btn_select_filter)
    ImageView btnSelectFilter;
    @Bind(R.id.btn_switch_front)
    ImageView btnSwitchFront;

    //TODO
    private Integer mSensorOrientation;
    private Size mVideoSize, mVideoPreviewSize, mPhotoSize, mPhotoPreviewSize;
    private MediaRecorder mMediaRecorder;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mPreviewSession;
    private CaptureRequest.Builder mPreviewBuilder;
    private Handler mBackgroundHandler;
    private HandlerThread mHandlerThread;
    private boolean mIsRecordingVideo;
    private ImageReader mImageReader;
    private String mCameraId = "0";//"1";
    private Timer timer;
    private TimerTask task;

    private String mVideoPath;
    private String mPhotoPath;

    private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;
    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();

    static {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    static {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        requestPermissions();
        btnTakePhoto.setOnLongClickListener(this);
        btnTakePhoto.setOnTouchListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceListener);
        }
    }

    private void startBackgroundThread() {
        mHandlerThread = new HandlerThread("CameraBackground");
        mHandlerThread.start();
        mBackgroundHandler = new Handler(mHandlerThread.getLooper());
    }

    private TextureView.SurfaceTextureListener mSurfaceListener
            = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    @SuppressLint("MissingPermission")
    private void openCamera(int width, int height) {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics charact = manager.getCameraCharacteristics(mCameraId);
            StreamConfigurationMap map = charact.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            mVideoSize = Util.chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
//            mVideoPreviewSize = Util.chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height, mVideoSize);
            mPhotoSize = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new Util.CompareSizesByArea());
            mPhotoPreviewSize = Util.chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height, mPhotoSize);

            Log.e("sg", "openCamera---videoSize---" + mVideoSize.getWidth() + "/" + mVideoSize.getHeight()
                    + "--photoSize--" + mPhotoSize.getWidth() + "/" + mPhotoSize.getHeight()
                    + "--photoPreview--" + mPhotoPreviewSize.getWidth() + "/" + mPhotoPreviewSize.getHeight());

            Util.configureTransform(this, mTextureView, mPhotoPreviewSize, width, height);
            mMediaRecorder = new MediaRecorder();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            manager.openCamera(mCameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    mCameraDevice = camera;
                    startPreview();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    camera.close();
                    mCameraDevice = null;
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    camera.close();
                    mCameraDevice = null;
                    finish();
                }
            }, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startPreview() {
        try {
            closePreviewSession();
            initCamera();

            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPhotoPreviewSize.getWidth(), mPhotoPreviewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            Surface previewSurface = new Surface(texture);
            Surface imgSurface = mImageReader.getSurface();

            mPreviewBuilder.addTarget(previewSurface);

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, imgSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    mPreviewSession = session;
                    updatePreview();
                    Util.showToastMsg(MainActivity.this, "camera preview start!");
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Util.showToastMsg(MainActivity.this, R.string.camera_connect_failed);
                }
            }, mBackgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private CaptureRequest.Builder mCaptureBuilder;

    private void takePhoto() {
        if (mCameraDevice == null) return;

        Log.e("sg", "拍照！");
        try {
            mCaptureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            mCaptureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            mCaptureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            mCaptureBuilder.addTarget(mImageReader.getSurface());

            //停止连续取景
            mPreviewSession.stopRepeating();
            //拍照，并回调CaptureCallback方法
            mPreviewSession.capture(mCaptureBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    updatePreview();
                    showToastMsg(MainActivity.this, "文件已保存");
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updatePreview() {
        try {
            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startRecord() {
        try {
            closePreviewSession();
            initRecordVideo();
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mVideoSize.getWidth(), mVideoSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);

            Surface previewSurface = new Surface(texture);
            Surface recorderSurface = mMediaRecorder.getSurface();

            mPreviewBuilder.addTarget(previewSurface);
            mPreviewBuilder.addTarget(recorderSurface);

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, recorderSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    Util.showToastMsg(MainActivity.this, "开始录像！");
                    btnTakePhoto.setMax(MAX_RECORD_TIME * TIME_INTERVAL);
                    btnTakePhoto.setProgress(0);
                    mPreviewSession = session;
                    updatePreview();
                    mIsRecordingVideo = true;
                    mMediaRecorder.start();
                    timerStart();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Util.showToastMsg(MainActivity.this, "录像连接失败");
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException | IOException e) {
            e.printStackTrace();
        }
    }

    private void stopRecord() {

        Log.e("sg", "录像结束");
        timerStop();
        stopBtnAnimation();

        try {
            Util.showToastMsg(this, "录像结束");
            btnTakePhoto.setProgress(0);
            mIsRecordingVideo = false;

            mPreviewSession.stopRepeating();
            mPreviewSession.abortCaptures();

            // Stop recording
            mMediaRecorder.stop();
            mMediaRecorder.reset();

            startPreview();

            //另存为gif
            Observable.just(0)
                    .observeOn(Schedulers.newThread())
                    .subscribe(new Action1<Integer>() {
                        @Override
                        public void call(Integer integer) {
                            FFmpegNativeHelper.runCommand("ffmpeg -i " + mVideoPath + " -vframes 50 -s 480*270 -y -f gif " + getSavePath(CameraConstant.TYPE_GIF));
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                        }
                    });


        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //TODO
    @OnClick({R.id.btn_select_picture, R.id.btn_flash, R.id.btn_count_down, R.id.btn_take_photo, R.id.btn_select_filter, R.id.btn_switch_front})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_select_picture:
                break;

            case R.id.btn_flash:
                break;

            case R.id.btn_count_down:
                break;

            case R.id.btn_select_filter:
                break;

            case R.id.btn_switch_front:

                break;

            case R.id.btn_take_photo:
                takePhoto();
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {

        startRecord();
        longClicked = true;
        return false;
    }

    boolean longClicked = false;

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (mIsRecordingVideo) {
                stopRecord();
            }
            if (longClicked) {
                longClicked = false;
                return true;
            }
        }

        return false;
    }


    private void initCamera() {
        mImageReader = ImageReader.newInstance(mPhotoSize.getWidth(), mPhotoSize.getHeight(), ImageFormat.JPEG, 10);
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {

            //当照片数据可用时激发此方法
            @Override
            public void onImageAvailable(ImageReader reader) {
                //获取照片存放的目标文件目录
                mPhotoPath = getSavePath(CameraConstant.TYPE_CAMERA);

                //获取捕获的照片数据
                Image image = reader.acquireNextImage();
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                //使用IO流將照片写入指定文件
                buffer.get(bytes);
                FileOutputStream outputStream = null;
                try {
                    outputStream = new FileOutputStream(mPhotoPath);
                    outputStream.write(bytes);

                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    image.close();
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }, mBackgroundHandler);
    }


    private void initRecordVideo() throws IOException {
        mVideoPath = getSavePath(CameraConstant.TYPE_VIDEO);

        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setVideoEncodingBitRate(10000000);
        mMediaRecorder.setVideoFrameRate(30);
        Log.e("sg", "video_size===" + mVideoSize.getWidth() + "/" + mVideoSize.getHeight());
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        mMediaRecorder.setOutputFile(mVideoPath);

//        int rotation = getWindowManager().getDefaultDisplay().getRotation();
//        switch (mSensorOrientation) {
//            case SENSOR_ORIENTATION_DEFAULT_DEGREES:
//                mMediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation));
//                break;
//            case SENSOR_ORIENTATION_INVERSE_DEGREES:
//                mMediaRecorder.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation));
//                break;
//        }
        mMediaRecorder.prepare();
    }

    private String getSavePath(int type) {
        String dir = Util.getCameraDir();
        String fileName = getMediaFileName(type);
        if (fileName == null) {
            return dir + System.currentTimeMillis();
        }
        return dir + File.separator + fileName;
    }

    private int count = 0;
    private int MAX_RECORD_TIME = 6;
    private int TIME_INTERVAL = 1000;

    private void timerStart() {
        timerStop();
        if (timer == null) {
            timer = new Timer();
        }

        startBtnAnimation();

        if (task == null) {
            task = new TimerTask() {
                @Override
                public void run() {
                    count++;

                    if (count > MAX_RECORD_TIME) {
                        count = 0;
                        Log.e("sg", "6S时间到");
                        Observable.just(0)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Action1<Integer>() {
                                    @Override
                                    public void call(Integer integer) {
                                        stopRecord();
                                    }
                                }, new Action1<Throwable>() {
                                    @Override
                                    public void call(Throwable throwable) {
                                    }
                                });
                    }
                }
            };
        }
        timer.schedule(task, 0, TIME_INTERVAL);
    }

    private void timerStop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (task != null) {
            task.cancel();
            task = null;
        }
        count = 0;
    }

    private Subscription mSubscription;

    private void startBtnAnimation() {
        Observable<Long> o = Observable.interval(100, TimeUnit.MILLISECONDS);
        o.observeOn(AndroidSchedulers.mainThread());
        mSubscription = o.subscribe(new Action1<Long>() {
            @Override
            public void call(Long aLong) {
                //这里为了让最后一个闭合，所以提前+1
                btnTakePhoto.setProgress(100 * (aLong + 1));
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {

            }
        });
    }

    private void stopBtnAnimation() {
        if (mSubscription != null && !mSubscription.isUnsubscribed()) {
            Log.e("sg", "停止任務器");
            mSubscription.unsubscribe();
        }
    }

    private void requestPermissions() {
        RxPermissions.getInstance(this).request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean result) {
                        if (result) {
                            Toast.makeText(MainActivity.this, "已授权", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "您没有授权该权限，请在设置中打开授权", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        closePreviewSession();
        stopThread();
        if (mCameraDevice != null) {
            mCameraDevice.close();
        }
    }

    private void closePreviewSession() {
        if (mPreviewSession != null) {
            try {
                mPreviewSession.stopRepeating();
                mPreviewSession.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mPreviewSession = null;
        }
    }

    private void stopThread() {
        if (mHandlerThread == null) {
            return;
        }

        mHandlerThread.quitSafely();
        try {
            mHandlerThread.join();
            mHandlerThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}

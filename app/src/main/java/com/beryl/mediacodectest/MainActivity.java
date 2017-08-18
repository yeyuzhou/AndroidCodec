package com.beryl.mediacodectest;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.beryl.codec.CodecInterface;
import com.beryl.codec.CodecUtils;
import com.beryl.codec.SurfaceViewDecode;
import com.beryl.codec.SurfaceViewEncode;
import com.beryl.jni.CodecJniBack;
import com.beryl.jni.CodecJniHelp;

/**
 * @author beryl
 * @date 20170818
 * @note 安卓手机硬编硬解H264，H265功能
 *       目前支持H264编码的最低版本为19，采用的mediacodec编解码
 *       H265支持的手机相对少一些，当前开发用的ZTE A2017 ，测试成功，编码出的视频录制后放HEVCAnalyzer可以播放
 *       华为P9，参数介绍里写支持H265，这里我测试不行，创建编码器失败。
 *       后续增加ffmpeg，添加滤镜，翻转等效果，当前demo只实现简单的编解码功能
 *       cpp代码仅提供了编码数据传到解码器的中转，以及NV21转YUV420时的数据转换（java效率太低）
 */
public class MainActivity extends Activity implements CodecInterface.onEncodeDataListerner,View.OnClickListener{

    private SurfaceViewEncode mEncodeSurface;
    private SurfaceViewDecode mDecodeSurface;

    private Button mPreviewBtn;
    private Button mRecordBtn;
    private Button mChangeCameraBtn;

    private int mEncFrameRate = 15;
    private int mEncBitrate = 512000;
    private int mEnciFrame_interval = 15;
    private int mEncwidth = 640;
    private int mEncheight = 480;

    private int mDecwidth = 640;
    private int mDecheight = 480;
    private int mDecframerate = 15;

    private CodecJniBack mJniBack = null;
    private RelativeLayout mParentView;
    // 添加屏幕不休眠功能，yt,20151215
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakelock = null;

    private int mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
    private boolean bIsPreview = true;
    /**此处修改CodecUtils.H265CODE即可切换H265编解码**/
    private int mVideoType = CodecUtils.H264CODE;
    private boolean bIsRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mJniBack = CodecJniBack.the();
        CodecJniHelp.Init(mJniBack);

        mEncodeSurface = new SurfaceViewEncode(this);
        mDecodeSurface = new SurfaceViewDecode(this,mVideoType ,mDecwidth, mDecheight, mDecframerate);
        mPreviewBtn = (Button) findViewById(R.id.preivew);
        mRecordBtn = (Button) findViewById(R.id.record);
        mChangeCameraBtn = (Button) findViewById(R.id.change_camera);
        mPreviewBtn.setOnClickListener(this);
        mRecordBtn.setOnClickListener(this);
        mChangeCameraBtn.setOnClickListener(this);
        mParentView = (RelativeLayout) findViewById(R.id.parenview);

        mEncodeSurface.setEncodeDatalistener(this);
        mJniBack.setRecvVideoListener(mDecodeSurface);

        mPowerManager = (PowerManager) getSystemService(POWER_SERVICE);
        mWakelock = mPowerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK,
                "my lock");
        mWakelock.acquire();
    }


    @Override
    public void onEncodeData(byte[] data, int length) {
        if(length > 0){
            CodecJniHelp.SetEncodeData(data, length,0);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.preivew: {
                mParentView.removeAllViews();
                bIsPreview = true;
                mEncodeSurface.setSurfaceParams(mVideoType, mCameraId, mEncwidth, mEncheight, mEncFrameRate, mEncBitrate, mEnciFrame_interval, bIsPreview);
                RelativeLayout.LayoutParams paramsEncode = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                mParentView.addView(mEncodeSurface, paramsEncode);

                if (bIsRecording){
                    bIsRecording = false;
                    mRecordBtn.setText(getString(R.string.start_record));
                }
                break;
            }
            case R.id.record: {
                bIsRecording = !bIsRecording;
                mParentView.removeAllViews();

                if (bIsRecording){

                    bIsPreview = false;
                    mEncodeSurface.setSurfaceParams(mVideoType, mCameraId, mEncwidth, mEncheight, mEncFrameRate, mEncBitrate, mEnciFrame_interval, bIsPreview);
                    RelativeLayout.LayoutParams paramsEncode = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                    mParentView.addView(mEncodeSurface, paramsEncode);

                    RelativeLayout.LayoutParams decodeParams = new RelativeLayout.LayoutParams(320, 240);
                    mParentView.addView(mDecodeSurface, decodeParams);
                    mRecordBtn.setText(getString(R.string.stop_record));
                }else {
                    mRecordBtn.setText(getString(R.string.start_record));
                }



                break;
            }
            case R.id.change_camera:{
                if (mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT){
                    mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
                }else {
                    mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
                }
                mEncodeSurface.changeCamera(mCameraId);
                break;
            }
            default: {
                break;
            }
        }
    }
}

package com.beryl.codec;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.MediaCodecInfo;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.beryl.codec.CodecUtils.H264CODE;


/**
 * Created by beryl on 2017/8/17.
 */

public class SurfaceViewEncode extends TextureView implements TextureView.SurfaceTextureListener,Camera.PreviewCallback ,EncodeHelp.EncodeDataInterface {

    private final String TAG = "EncodeSurfaceView";
    private static final int NUM_CAMERA_PREVIEW_BUFFERS = 5;
    private Camera mCamera;
    private int mWidth = 320;
    private int mHeight = 240;
    private Activity mContext;
    private EncodeHelp mEncodeHelp;
    private int mCameraId = 0;
    private boolean bForntCamera = false;
    private int mPrevFmt = CodecUtils.PREVIEW_PIX_FORMAT_YV12;
    private int mEncFmt;
    private int mPrevSize;
    private byte[] mByteBuffer;
    private boolean bIsPreview = true;
    private boolean bIsCameraOpen = false;
    private byte[]mVideoSpsPpsData = null;
    private byte[]mData = null;
    private long mPrevFrameCount;
    private long  mStartPreviewMsec;
    private int mVideoFrameRate = 15;
    private boolean mIsFrameAdd = true;//lost frame
    /**编码数据写入文本，需要看具体信息时可放开**/
//    private FileOutputStream fos = null;
    private CodecInterface.onEncodeDataListerner mOnEncodeListener;
    private int mVideoType = 0; // 0: h264 1: h265

    /**
     * If only want to start preview ,set the value of context and bePreview
     * @param context
     */
    public SurfaceViewEncode(Activity context ) {
        super(context);

        this.mContext = context;

       /* File file = new File("/sdcard/a.264");
        if (file.exists()) {
            file.delete();
        }
        try {
            file.createNewFile();
            fos = new FileOutputStream(file);
        } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }*/


    }

    /**
     *
     * @param videotype
     * @param cameraId
     * @param width
     * @param height
     * @param mEncFrameRate
     * @param mEncBitrate
     * @param iFrame_interval
     * @param bePreview
     */
    public void setSurfaceParams(int videotype ,int cameraId,int width ,int height, int mEncFrameRate,int mEncBitrate ,int iFrame_interval ,boolean bePreview){

        this.mVideoType = videotype;
        this.mWidth = width;
        this.mHeight = height;
        this.bIsPreview = bePreview;
        this.mCameraId = cameraId;
        if (!bePreview){
            setEncodeParams(mEncFrameRate ,mEncBitrate ,iFrame_interval);
        }
        setSurfaceTextureListener(this);


    }

    /**
     * set encode params
     * @param mEncFrameRate :framerate eg:15
     * @param mEncBitrate :bitrate eg:512000
     * @param iFrame_interval :i_frame_interval eg:15
     */
    public void setEncodeParams(int mEncFrameRate,int mEncBitrate ,int iFrame_interval ){

        System.out.println("setEncodeParams enter >>>");
        if (mVideoType == CodecUtils.H264CODE){
            mEncFmt = CodecUtils.getMediaColorFormat(CodecUtils.H264_MIME);
        }else if (mVideoType == CodecUtils.H265CODE){
            mEncFmt = CodecUtils.getMediaColorFormat(CodecUtils.H265_MIME);
        }
        //此处可做错误处理
//        if (mEncFmt == -1){
//            return;
//        }

        mData = new byte[1048576];

        System.out.println("setEncodeParams mEncFmt : " +mEncFmt);
        this.mVideoFrameRate = mEncFrameRate;
        if(mEncFmt == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar){
            mPrevFmt = CodecUtils.PREVIEW_PIX_FORMAT_YV12;

        }else if(mEncFmt == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar){
            //华为的颜色值暂未做处理，目前色彩转换效果不对
            mPrevFmt = CodecUtils.PREVIEW_PIX_FORMAT_YV12;
        }else if(mEncFmt == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar){

            mPrevFmt = CodecUtils.PREVIEW_PIX_FORMAT_NV21;
        }else{

            mPrevFmt = CodecUtils.PREVIEW_PIX_FORMAT_UNSUPPORT;
            return;
        }


        Log.i(TAG ,"setEncodeParams mPrevFmt :"+mPrevFmt  + "  mCameraId :" + mCameraId + " videoType :" + mVideoType);
        if(mCameraId == 0){
            bForntCamera = false;
            mEncodeHelp = new EncodeHelp(mVideoType ,mWidth, mHeight,mPrevFmt,mEncFmt, mEncFrameRate,mEncBitrate, iFrame_interval, bForntCamera);
        }else{
            bForntCamera= true;
            mEncodeHelp = new EncodeHelp(mVideoType ,mWidth, mHeight, mPrevFmt,mEncFmt,mEncFrameRate,mEncBitrate, iFrame_interval, bForntCamera);
        }
        if(mEncodeHelp != null) {
            mEncodeHelp.setOnDataListener(this);
            mEncodeHelp.requestKeyFrame();
        }

        System.out.println("setEncodeParams enter <<<");

    }
    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        camera.addCallbackBuffer(mByteBuffer);

        long duration = System.currentTimeMillis()
                - mStartPreviewMsec;
        double currFPS = mPrevFrameCount * 1000 / (double) duration;

        if (currFPS > mVideoFrameRate) {
             Log.d(TAG, String.format("Java: drop fronPreviewFrame isFrameAddame fps %.2f",
             currFPS));
            return;
        }
        mPrevFrameCount++;
        if(bytes.length != 0 && mEncodeHelp != null){

            //此处添加丢帧处理，如若不需要，直接调用encodeHelp.addFrame(bytes, bytes.length);即可
            if (mIsFrameAdd){
                mIsFrameAdd = false;
                mIsFrameAdd = mEncodeHelp.addFrame(bytes, bytes.length);
            }
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        Log.i(TAG," onSurfaceTextureAvailable");
        if (bIsPreview){
            startPreview();
        }else {
            startEncode();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {
        Log.i(TAG," onSurfaceTextureSizeChanged  " );

        if (bIsPreview){
            startPreview();
        }else {
            startEncode();
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        Log.i(TAG,"yttest onSurfaceTextureDestroyed");
        if (bIsPreview){
            stopPreview();
        }else {
            stopEncode();
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

    }

    @Override
    public void onData(byte[] data, int start, int byteCount, long timestamp) {
        System.out.println("onData data 长度："+data.length);
        int pos = 0;
        // key frame 编码器生成关键帧时只有 00 00 00 01 65 没有pps sps， 要加上

        if (mVideoType == H264CODE){
            if((data[start+4]&0x1f) == CodecUtils.IDR_264 && mVideoSpsPpsData != null){
                System.arraycopy(mVideoSpsPpsData, 0, mData, pos,mVideoSpsPpsData.length );
                pos += mVideoSpsPpsData.length;
            }
        }else if (mVideoType == CodecUtils.H265CODE){
            if((data[start+4]&0x1f) == CodecUtils.SEI && mVideoSpsPpsData != null){
                System.arraycopy(mVideoSpsPpsData, 0, mData, pos,mVideoSpsPpsData.length );
                pos += mVideoSpsPpsData.length;
            }
        }

        System.arraycopy(data, start, mData, pos, byteCount);
        pos += byteCount;

        if(mOnEncodeListener != null){

            /*if(fos != null){

                try {
                    fos.write(mData, 0, mData.length);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }*/
            mOnEncodeListener.onEncodeData(mData, pos);
        }
    }

    @Override
    public void onSpsPps(byte[] data, int start, int byteCount) {
        if(mVideoSpsPpsData == null){
            mVideoSpsPpsData = new byte[byteCount];
            System.arraycopy(data, start, mVideoSpsPpsData, 0, byteCount);

        }
    }


    private void startPreview(){
        stopPreview();
        mCamera= Camera.open(mCameraId);

        setCameraDisplayOrientation(mContext ,mCameraId ,mCamera);
        try {
            mCamera.setPreviewTexture(getSurfaceTexture());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            Log.d(TAG,"camera.setPreviewDisplay(surfaceHolder) IOException");
            e.printStackTrace();
        }
        mCamera.startPreview();
        bIsCameraOpen = true;
    }
    private boolean startEncode(){
        stopPreview();
        if(mEncodeHelp == null){
            Log.d(TAG, "startEncode failed. Please check encode params.");
            return false;
        }
        mCamera = Camera.open(mCameraId);
        setCameraDisplayOrientation(mContext, mCameraId, mCamera);
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> pictureSizes = parameters.getSupportedPictureSizes();
        boolean founded = false;
        int prev_size = pictureSizes.size();
        Log.d(TAG, "prev_size size is : "+prev_size);

        for(int i = 0; i < prev_size; i++){
            Camera.Size size = pictureSizes.get(i);
            Log.d(TAG, String.format("camera suport preview size is %d x %d", size.width ,size.height));
            if(!founded){
                if(size.width == mWidth && size.height == mHeight){
                    founded = true;
                    break;
                }
            }
        }

        if(founded){
            parameters.setPictureSize(mWidth, mHeight);
        }else{
            return false;
        }
        parameters.setPictureFormat(ImageFormat.JPEG);
        int prev_fmt = parameters.getPreviewFormat();
        mPrevSize = mWidth * mHeight * ImageFormat.getBitsPerPixel(prev_fmt)/8;
        try {
            mCamera.setPreviewTexture(getSurfaceTexture());

        } catch (IOException e) {
            // TODO Auto-generated catch block
            Log.d(TAG,"camera.setPreviewDisplay(surfaceHolder) IOException");
            e.printStackTrace();
        }
        parameters.setPreviewSize(mWidth, mHeight);
        if(mPrevFmt == CodecUtils.PREVIEW_PIX_FORMAT_NV21){

            parameters.setPreviewFormat(ImageFormat.NV21);
        }else if(mPrevFmt == CodecUtils.PREVIEW_PIX_FORMAT_YV12){

            parameters.setPreviewFormat(ImageFormat.YV12);
        }else{
            return false;
        }
        List<int[]> rangeList = parameters.getSupportedPreviewFpsRange();
        for (int i=0;i<rangeList.size();i++) {
            int[] range = rangeList.get(i);
            Log.i(TAG, "Java: range[] " + Arrays.toString(range));
        }

        int CurPreRange[];
        CurPreRange = new int[2];
        parameters.getPreviewFpsRange(CurPreRange);
        Log.i(TAG, "Java: Current Preview MinFps = " + CurPreRange[0]
                + "  MaxFps = " + CurPreRange[1]);
        int setFPS = mVideoFrameRate * 1000;
        if (setFPS >= CurPreRange[0] && setFPS <= CurPreRange[1]) {
//            parameters.setPreviewFpsRange(setFPS, CurPreRange[1]);
            Log.i(TAG, "Java: set fps " + setFPS);
        }
        mCamera.setParameters(parameters);
        mByteBuffer = new byte[mPrevSize];
        for(int i = 0; i < NUM_CAMERA_PREVIEW_BUFFERS ;i++ ){
            mCamera.addCallbackBuffer(mByteBuffer);
        }
        mCamera.setPreviewCallbackWithBuffer(this);
        mPrevFrameCount = 0;
        mStartPreviewMsec = System.currentTimeMillis();
        mCamera.startPreview();
        bIsCameraOpen = true;
        return true;
    }
    private void stopPreview(){
        if(mCamera != null){
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        bIsCameraOpen = false;
    }
    private void stopEncode(){
        if(mEncodeHelp != null){
            mEncodeHelp.colse();
        }
      /*  if(fos != null){
            try {
                fos.flush();
                fos.close();
                fos = null;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }*/
        bIsCameraOpen = false;
        stopPreview();
    }

    public void changeCamera(int cameraId){
        this.mCameraId = cameraId;
        if (bIsCameraOpen){
            if (bIsPreview){
                startPreview();
            }else {
                if (mEncodeHelp != null){
                    mEncodeHelp.setIsFrontCamera(bForntCamera);
                }
                startEncode();
            }
        }
    }

    /**
     * 设置编码数据回调监听
     * @param onencodelistener
     */
    public void setEncodeDatalistener(CodecInterface.onEncodeDataListerner onencodelistener){
        this.mOnEncodeListener = onencodelistener;
    }

    private void setCameraDisplayOrientation(Activity context, int cameraId, Camera camera) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = context.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else {
            // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

}

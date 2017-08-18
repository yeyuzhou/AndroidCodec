package com.beryl.codec;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;


/**
 * Created by beryl on 2017/8/17.
 */

public class SurfaceViewDecode extends TextureView implements TextureView.SurfaceTextureListener ,CodecInterface.onDecodeDataListerner {

    private final String TAG = "SurfaceViewDecode";
    private int mWidth = 320;
    private int mHeight = 240;
    private DecodeHelp mDecodeHelp;
    private int mFramerate = 15;
    private int mVideoType = CodecUtils.H264CODE;

    public SurfaceViewDecode(Context context  ,int videoType , int width, int height, int framerate) {
        super(context);
        init(videoType ,width , height ,framerate);
    }
    private void init(int videoType  ,int width, int height, int framerate){
        this.mVideoType = videoType;
        this.mFramerate = framerate;
        this.mWidth = width;
        this.mHeight = height;

        setSurfaceTextureListener(this);

    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        Log.i(TAG,"yttest onSurfaceTextureAvailable");
        mDecodeHelp = new DecodeHelp();
        if(mDecodeHelp.openDecode(mVideoType ,mWidth, mHeight, mFramerate, new Surface(surfaceTexture))){
        }else{
            Log.e(TAG, "ERROR surfaceCreated error. openDecode failed");
            mDecodeHelp = null;
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        if (mDecodeHelp != null){
            mDecodeHelp.stopDecode();
            mDecodeHelp = null;
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

    }

    @Override
    public void onDecodeData(byte[] data, int length ) {
        // TODO Auto-generated method stub
        System.out.println("onDecodeData length:"+length);
        if(mDecodeHelp != null && length > 4){

            mDecodeHelp.addFrame(data, 0, length);
        }
    }

    @Override
    public void onDecodeSecondData(byte[] data, int length) {
        // TODO Auto-generated method stub
        if(mDecodeHelp != null && length > 4){
            mDecodeHelp.addFrame(data, 0, length);
        }
    }

}

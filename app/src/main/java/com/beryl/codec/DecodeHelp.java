package com.beryl.codec;

import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class DecodeHelp {
	private final String TAG = "DecodeHelp";
	private int framerate;
	private MediaCodec mMediaCodecDecode;
	private long mFrames;
	private byte[] mInfo;
	private int mVideoType = 0;//0 : 264   1: 265
	private String MIME_TYPE =  CodecUtils.H264_MIME;

	public boolean openDecode(int videoType ,int width, int height, int framerate ,Surface surface){
		this.framerate = framerate;
		if(surface == null){
			Log.e(TAG, "ERROR openDecode error for the surface is null");
			return false;
		}
		this.mVideoType = videoType;
		if (mVideoType == CodecUtils.H264CODE){
			MIME_TYPE = CodecUtils.H264_MIME;
		}else if (mVideoType == CodecUtils.H265CODE){
			MIME_TYPE = CodecUtils.H265_MIME;
		}

		try {
			mMediaCodecDecode = MediaCodec.createDecoderByType(MIME_TYPE);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);

		mMediaCodecDecode.configure(format, surface, null, 0);
		mMediaCodecDecode.start();

		return true;
	}


	public void addFrame(byte[]data ,int start ,int length){
		if (mMediaCodecDecode == null) {
			System.out.println("mMediaCodecDecode is null");
		}
		else {
			try{
				ByteBuffer[] inputBuffers = mMediaCodecDecode.getInputBuffers();
				int inputBufferIndex = mMediaCodecDecode.dequeueInputBuffer(-1);
				if (inputBufferIndex >= 0) {
					long timeStamp = mFrames * 1000000 / framerate;
				    ByteBuffer bytebuffer = inputBuffers[inputBufferIndex];
					//high level support ,current min level is 19
//					ByteBuffer bytebuffer = mMediaCodecDecode.getInputBuffer(inputBufferIndex);
					bytebuffer.clear();
					bytebuffer.put(data, start, length);
					mMediaCodecDecode.queueInputBuffer(inputBufferIndex, 0/*offset*/, length, timeStamp, 0);

				}
				BufferInfo bufferInfo = new BufferInfo();
				int outputBufferIndex = mMediaCodecDecode.dequeueOutputBuffer(bufferInfo, 1000);
				do {
					if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
						//no output available yet
						break;
					} else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
						//mediaformat changed

					} else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
						//encodeOutputBuffers = mDecodeMediaCodec.getOutputBuffers();

					} else {

						mFrames++;
						mMediaCodecDecode.releaseOutputBuffer(outputBufferIndex, true);

						outputBufferIndex = mMediaCodecDecode.dequeueOutputBuffer(bufferInfo, 0);

					}

				} while (outputBufferIndex > 0);
			}catch (Exception e){
				e.printStackTrace();
			}
		}

	}

	public void stopDecode(){

		if(mMediaCodecDecode != null){
			mMediaCodecDecode.stop();
			mMediaCodecDecode.release();
			mMediaCodecDecode = null;
		}
	}

}

package com.beryl.codec;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Bundle;
import android.util.Log;

import com.beryl.jni.CodecJniHelp;

import java.io.IOException;
import java.nio.ByteBuffer;

import static com.beryl.codec.CodecUtils.H264CODE;
import static com.beryl.codec.CodecUtils.H264_MIME;
import static com.beryl.codec.CodecUtils.H265CODE;
import static com.beryl.codec.CodecUtils.H265_MIME;

public class EncodeHelp {
	private static final String TAG = "EncodeHelp";
	private MediaCodec mEncodeCodec;
	private int mWidth;
	private int mHeight;
	private int mEncFrameRate = 15;
	private byte[]yuv420;
	private int mEncFmt;
	private long mFrames;
	private int mEncBitrate = 512000;
	private boolean mIsKeyFrame = false;
	private EncodeDataInterface encodeDataListener;
	private byte[]mInfo;
	private int dataLength = 0;
	private int mPrevFmt = CodecUtils.PREVIEW_PIX_FORMAT_YV12;
	private boolean bForntCamera = false;
	private int mVideoType = 0;//0 : 264   1: 265

	public EncodeHelp(int videoType , int width , int height , int mPrevFmt , int mEncFmt, int mEncFrameRate, int mEncBitrate , int iFrame_interval , boolean front_camera){
		this.mVideoType = videoType;
		this.mWidth = width;
		this.mHeight = height;
		this.mPrevFmt = mPrevFmt;
		this.mEncFmt = mEncFmt;

		dataLength = width * height *3/2;
		yuv420 = new byte[dataLength];
		try {
			if (videoType == H264CODE){
				mEncodeCodec = MediaCodec.createEncoderByType(CodecUtils.H264_MIME);
			}else if (videoType == H265CODE){
				mEncodeCodec = MediaCodec.createEncoderByType(CodecUtils.H265_MIME);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		setEncoderOption("bitrate="+mEncBitrate);

		MediaFormat mediaformat = null;
		if (videoType == H264CODE){
			mediaformat = MediaFormat.createVideoFormat(H264_MIME, mWidth, mHeight);
		}else if (videoType == H265CODE){
			mediaformat = MediaFormat.createVideoFormat(H265_MIME, mWidth, mHeight);
		}


		mediaformat.setInteger(MediaFormat.KEY_BIT_RATE, this.mEncBitrate);
		mediaformat.setInteger(MediaFormat.KEY_FRAME_RATE, mEncFrameRate);
		mediaformat.setInteger(MediaFormat.KEY_COLOR_FORMAT, mEncFmt);
		mediaformat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrame_interval);

		mEncodeCodec.configure(mediaformat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
		mEncodeCodec.start();
//		mEncodeCodec.flush();

	}

	public void setEncoderOption(String option) {
		// TODO Auto-generated method stub
		int pos = option.indexOf("bitrate=");
		if (pos != -1) {
			mEncBitrate = Integer.valueOf(
					(String)option.subSequence(pos + "bitrate=".length(), option.length()));
			Log.i(TAG, "Java: set mEncBitrate to " + mEncBitrate);
		}
	}

	public boolean addFrame(byte[] data ,int length ){
		if (mEncodeCodec == null){
			return false;
		}
		/**
		 * 此处未做前置摄像头镜像处理，手机翻转处理也没做，由于用的for循环，效率太低，后续加上
		 * ffmpeg，再加翻转滤镜等效果
		 */

		if (mPrevFmt == CodecUtils.PREVIEW_PIX_FORMAT_YV12){

			//YYYYYYYY VVUU to YYYYYYYY UUVV
			CodecJniHelp.changeYV12toI420(data,yuv420,length,mWidth ,mHeight);

		}else if(mPrevFmt == CodecUtils.PREVIEW_PIX_FORMAT_NV21){
			//YV12 YYYYYYYY VUVU to YYYYYYYY UVUV
			CodecJniHelp.changeNV21toYUV420SemiPlanar(data,yuv420,length,mWidth ,mHeight ,bForntCamera);

		}else{
			return false;
		}


		/**
		 * 检索输入缓冲区集。这叫后start()
		 *返回。调用此方法后，任何字节缓冲区
		 *先前调用此方法之前返回的值必须为
		 *不再使用。
		 */
		ByteBuffer[]inputBuffers = mEncodeCodec.getInputBuffers();

		/**返回要输入有效数据的输入缓冲区的索引
		*或- 1，如果没有这样的缓冲区目前可用。
		这个方法将timeoutus = = 0立即返回，无限期的等待*/
		int inputbufferIndex = mEncodeCodec.dequeueInputBuffer(-1);
		if(inputbufferIndex >= 0){
			ByteBuffer inputBuffer = inputBuffers[inputbufferIndex];
			/**
			 * 清除此缓冲区。该位置设置为零，限制设置为
			 * 能力，标志被丢弃。
			 */
			inputBuffer.clear();
			long timestamps = mFrames * 1000000 /mEncFrameRate;
			if (mEncFmt == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar &&
					mPrevFmt == CodecUtils.PREVIEW_PIX_FORMAT_NV21) {
				inputBuffer.put(data);
				if(mIsKeyFrame){
					System.out.println("Encode mIsKeyFrame is true");
					Bundle params = new Bundle();
					params.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
					mEncodeCodec.setParameters(params);
				}
				mEncodeCodec.queueInputBuffer(inputbufferIndex, 0, data.length, timestamps, 0);
			}else{
				inputBuffer.put(yuv420);
				if(mIsKeyFrame){
					System.out.println("Encode mIsKeyFrame is true");
					Bundle params = new Bundle();
					params.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
					mEncodeCodec.setParameters(params);
				}
				/**
				 * 在指定索引范围内填充输入缓冲区后
				 * 提交给组件
				 */
				mEncodeCodec.queueInputBuffer(inputbufferIndex, 0, inputBuffers[inputbufferIndex].limit(), timestamps, 0);
			}
			mIsKeyFrame = false;
			mFrames++;

		}else{
			Log.w(TAG, "EncodeHelp encode data overflow");
		}
		/**
		 *
		 检索输出缓冲区集。
		 */
		ByteBuffer[] outputBuffers = mEncodeCodec.getOutputBuffers();
		MediaCodec.BufferInfo bufferinfo = new MediaCodec.BufferInfo();
		/**
		 * 返回已成功输出缓冲区的索引
		 */
		int outputbufferIndex = mEncodeCodec.dequeueOutputBuffer(bufferinfo, 0);
		while(outputbufferIndex >= 0){
			ByteBuffer outputBuffer = outputBuffers[outputbufferIndex];
			if((bufferinfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0){

				if(mInfo == null){
					mInfo = new byte[bufferinfo.size];
					outputBuffer.get(mInfo);
					Log.i(TAG, "Java: sps_pps saved  info size : " +mInfo.length );
					if(encodeDataListener != null){
						encodeDataListener.onSpsPps(mInfo, 0, mInfo.length);
					}

				}
			}else{
				byte[]outData = new byte[bufferinfo.size];
				outputBuffer.position(bufferinfo.offset);
				outputBuffer.limit(bufferinfo.offset+bufferinfo.size);
				outputBuffer.get(outData);

				if (mVideoType == H264CODE){
					if((outData[4]&0x1f) != CodecUtils.SPS && (outData[4]&0x1f) != CodecUtils.PPS){
						if(encodeDataListener != null){
							if ((outData[4]&0x1f) == CodecUtils.SEI){
								int offset = 0;
								for (int j = 0; j < bufferinfo.size - 4; j++){
									if ((outData[j]&0x1f) == 0 && (outData[j+1]&0x1f) == 0 && (outData[j+2]&0x1f) == 0 && (outData[j+3]&0x1f) == 1){
										if ((outData[j + 4]&0x1f) == CodecUtils.IDR_264 ){
											offset = j;
											break;
										}

									}
								}
								encodeDataListener.onData(outData, offset, bufferinfo.size - offset, bufferinfo.presentationTimeUs /1000 );
							}else {
								encodeDataListener.onData(outData, 0, bufferinfo.size, bufferinfo.presentationTimeUs /1000 );
							}

						}
					}
				}else if (mVideoType == H265CODE){


					if((outData[4]&0x1f) != CodecUtils.SPS && (outData[4]&0x1f) != CodecUtils.PPS){
						if(encodeDataListener != null){
							if ((outData[4]&0x1f) == CodecUtils.SEI){
								int offset = 0;
								for (int j = 0; j < bufferinfo.size - 4; j++){
									if ((outData[j]&0x1f) == 0 && (outData[j+1]&0x1f) == 0 && (outData[j+2]&0x1f) == 0 && (outData[j+3]&0x1f) == 1){
										if ((outData[j + 4]&0x1f) == CodecUtils.IDR_265){
											offset = j;
											break;
										}

									}
								}
								encodeDataListener.onData(outData, offset, bufferinfo.size - offset, bufferinfo.presentationTimeUs /1000 );
							}else {
								encodeDataListener.onData(outData, 0, bufferinfo.size, bufferinfo.presentationTimeUs /1000 );
							}

						}
					}
				}


			}
			mEncodeCodec.releaseOutputBuffer(outputbufferIndex, false);
			outputbufferIndex = mEncodeCodec.dequeueOutputBuffer(bufferinfo, 0);
		}


		return true;
	}

	public void colse(){

		if(mEncodeCodec != null){
			mEncodeCodec.stop();
			mEncodeCodec.release();
			mEncodeCodec = null;
		}
	}

	public void setIsFrontCamera(boolean isFrontCamera){
		this.bForntCamera = isFrontCamera;
	}

	public void setOnDataListener(EncodeDataInterface encodeDataListener){
		this.encodeDataListener = encodeDataListener;
	}


	public void requestKeyFrame(){
		mIsKeyFrame = true;
	}
	public interface EncodeDataInterface{
		void onData(byte[] data, int start, int byteCount, long timestamp);
		void onSpsPps(byte[] data, int start, int byteCount);
	}
}

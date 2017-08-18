package com.beryl.jni;


import com.beryl.codec.CodecInterface;

import java.util.ArrayList;
import java.util.List;


public class CodecJniBack implements JNIInterface {
	private CodecInterface.onDecodeDataListerner onRecvVideoDatalistener = null;
	private static CodecJniBack myAgent = null;

	private List<CodecInterface.onDecodeDataListerner> listernerList = new ArrayList<>();


	public static CodecJniBack the(){
		if(myAgent == null){
			synchronized (CodecJniBack.class){
				if (myAgent == null)
				myAgent = new CodecJniBack();
			}

		}
		return myAgent;
	}

	@Override
	public void onDecodeDataCallback(byte[] data ,int length ,int tag) {
		// TODO Auto-generated method stub
//		System.out.println("接收到回调了啦"+length);
//		Log.i(tag+"", "33"+"onDecodeDataCallback start time is :"+System.currentTimeMillis());
		if(onRecvVideoDatalistener != null){
			onRecvVideoDatalistener.onDecodeData(data, length);
		}

		int count = listernerList.size();
//		Log.i(tag+"", "33"+"onDecodeDataCallback count :"+ count);
		for (int i = 0; i < count; i++){
			listernerList.get(i).onDecodeData(data, length);
		}



	}
	public void setRecvVideoListener(CodecInterface.onDecodeDataListerner onRecvVideoDatalistener){
		this.onRecvVideoDatalistener = onRecvVideoDatalistener;
	}
	public void addRecvVideoListener(CodecInterface.onDecodeDataListerner onRecvVideoDatalistener){
		this.listernerList.add(onRecvVideoDatalistener);
	}

	public void removeAllVideoListener(){
		listernerList.clear();
	}



	@Override
	public void onDecodeSecondDataCallback(byte[] data ,int length) {
		// TODO Auto-generated method stub
		if(onRecvVideoDatalistener != null){
			onRecvVideoDatalistener.onDecodeSecondData(data, length);
		}
	}

}

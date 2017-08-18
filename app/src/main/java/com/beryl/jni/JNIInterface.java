package com.beryl.jni;

public interface JNIInterface {
	public  void onDecodeDataCallback(byte[] data, int length, int tag);
	public  void onDecodeSecondDataCallback(byte[] data, int length);
}

package com.beryl.codec;

public interface CodecInterface {
	public interface onDecodeDataListerner{
		void onDecodeData(byte[] data, int length);
		void onDecodeSecondData(byte[] data, int length);
	}
	public interface onEncodeDataListerner{
		void onEncodeData(byte[] data, int length);
	}
}

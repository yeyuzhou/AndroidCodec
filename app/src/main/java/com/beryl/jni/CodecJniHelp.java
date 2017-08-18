package com.beryl.jni;


public class CodecJniHelp{

	/**
	 * A native method that is implemented by the 'native-lib' native library,
	 * which is packaged with this application.
	 */

	public native static boolean Init(Object obj);
	public native static void SetEncodeData(byte[] data,int length ,int tag);
	public native static void changeNV21toYUV420SemiPlanar(byte[]input,byte [] output,int length,int w ,int h ,boolean isFrontCamera);


	// Used to load the 'native-lib' library on application startup.
	static {
		System.loadLibrary("native-lib");
	}

	public static void changeYV12toI420(byte[]input,byte [] output,int length,int width ,int height){
		System.arraycopy(input, 0, output, 0, width * height);
		System.arraycopy(input, width * height + width * height / 4,
				output, width * height,
				width * height / 4);
		System.arraycopy(input, width * height,
				output, width * height + width * height / 4,
				width * height / 4);
	}

}

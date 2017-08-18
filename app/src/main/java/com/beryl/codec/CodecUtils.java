package com.beryl.codec;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;

import java.util.Arrays;

/**
 * Created by Administrator on 2017/8/17.
 */

public class CodecUtils {

    public static final String H264_MIME = "video/avc";
    public static final String H265_MIME = "video/hevc";

    public final static int H264CODE = 0;
    public final static int H265CODE = 1;
    public final static int IDR_264 = 5;
    // Supplemental enhancement information (SEI) sei_rbsp( )
    public final static int SEI = 6;
    // Sequence parameter set seq_parameter_set_rbsp( )
    public final static int SPS = 7;
    // Picture parameter set pic_parameter_set_rbsp( )
    public final static int PPS = 8;

    public final static int IDR_265 = 26;
    public static final int PREVIEW_PIX_FORMAT_UNSUPPORT	= -1;
    public static final int PREVIEW_PIX_FORMAT_NV21	= 0;
    public static final int PREVIEW_PIX_FORMAT_YV12	= 1;


    public static int getMediaColorFormat(String minetype) {
        int numbers = MediaCodecList.getCodecCount();
        MediaCodecInfo codeinfo = null;
        for(int i = 0 ;i < numbers && codeinfo == null; i++){
            MediaCodecInfo info = MediaCodecList.getCodecInfoAt(i);
            if(!info.isEncoder()){
                continue;
            }
            boolean isFound = false;
            String[]types = info.getSupportedTypes();
            for(int j = 0; j < types.length && !isFound ;j++){
                if(types[j].equals(minetype)){
                    isFound = true;
                }
            }

            if(!isFound){
                continue;
            }
            codeinfo = info;
        }
        if (codeinfo == null){
            return -1;
        }

        int colorFormat = 0;
        MediaCodecInfo.CodecCapabilities capabilities = codeinfo.getCapabilitiesForType(minetype);
        System.out.println("length :"+capabilities.colorFormats.length +" == "+ Arrays.toString(capabilities.colorFormats));
        for(int i = 0; i<capabilities.colorFormats.length && colorFormat == 0 ; i++){
            int format = capabilities.colorFormats[i];
            switch (format) {
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
                    colorFormat = format;
                    break;
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
                    colorFormat = format;
                    break;
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
                    colorFormat = format;
                    break;
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
                    colorFormat = format;
                    break;
                case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
                    colorFormat = format;
                    break;

                default:
                    System.out.println("skipping unsupport format :"+format);
                    break;
            }
        }
        System.out.println("colorformat :"+colorFormat);
        return colorFormat;

    }
}

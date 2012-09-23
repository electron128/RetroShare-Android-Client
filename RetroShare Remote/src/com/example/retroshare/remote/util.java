package com.example.retroshare.remote;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.WriterException;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.ByteMatrix;
import com.google.zxing.qrcode.encoder.Encoder;
import com.google.zxing.qrcode.encoder.QRCode;

public class util {
	
	// stolen from the internet
	public static String byteArrayToHexString(byte[] b) {
			StringBuffer sb = new StringBuffer(b.length * 2);
			for (int i = 0; i < b.length; i++) {
				int v = b[i] & 0xff;
				if (v < 16) {
				sb.append('0');
			}
		sb.append(Integer.toHexString(v));
		}
		return sb.toString().toUpperCase();
	}
	
	
	
	static Bitmap encodeQrCode(String contents) throws WriterException {
		QRCode qrCode=new QRCode();
		Encoder.encode(contents, ErrorCorrectionLevel.L, qrCode);

		ByteMatrix result =qrCode.getMatrix();
		int width = result.getWidth();
		int height = result.getHeight();
		int[] pixels = new int[width * height];
		for (int y = 0; y < height; y++) {
			int offset = y * width;
			for (int x = 0; x < width; x++) {
				if(result.get(x, y)==1){
					pixels[offset + x] =  Color.BLACK;
				}else{
					pixels[offset + x] = Color.WHITE;
				}
			}
		}

		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
		bitmap=Bitmap.createScaledBitmap(bitmap, width*6, height*6, false);
		return bitmap;
		}
}

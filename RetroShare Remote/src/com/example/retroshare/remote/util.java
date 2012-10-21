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
		
		System.out.println("encoded qrCode: "+qrCode);

		ByteMatrix result =qrCode.getMatrix();
		int codewidth=result.getWidth();
		int codeheight=result.getHeight();
		// +2 for quiet zone
		int imagewidth = result.getWidth()+2;
		int imageheight = result.getHeight()+2;
		int[] pixels = new int[imagewidth * imageheight];
		
		// init pixels with white:
		for (int i = 0; i < pixels.length; i++) {
			pixels[i] = Color.WHITE;
		}
		
		// set black pixels
		// use 1pixel quiet zone, this is enough for most readers, see
		// http://qrworld.wordpress.com/2011/08/09/the-quiet-zone/
		for (int y = 0; y < codeheight; y++) {
			//             +1 line horizontal for quiet zone
			int offset = (y+1) * imagewidth;
			for (int x = 0; x < codewidth; x++) {
				if(result.get(x, y)==1){
					//                +1 pixel vertical for quiet zone
					pixels[offset + x +1] =  Color.BLACK;
				}
			}
		}

		Bitmap bitmap = Bitmap.createBitmap(imagewidth, imageheight, Bitmap.Config.ARGB_8888);
		bitmap.setPixels(pixels, 0, imagewidth, 0, 0, imagewidth, imageheight);
		bitmap=Bitmap.createScaledBitmap(bitmap, imagewidth*6, imageheight*6, false);
		return bitmap;
		}
}

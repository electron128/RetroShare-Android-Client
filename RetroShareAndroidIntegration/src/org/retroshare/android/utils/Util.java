package org.retroshare.android.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;


import com.google.zxing.WriterException;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.ByteMatrix;
import com.google.zxing.qrcode.encoder.Encoder;
import com.google.zxing.qrcode.encoder.QRCode;

import java.io.ByteArrayOutputStream;

public class Util
{
	public static final String URI_REG_EXP = "([a-z]+[0-9a-z\\.\\-\\+]*:\\/\\/)(\\S)+"; // Probably too permissive, but I never saw a false positive in many tests

	public static String TAG() { return "Util"; }
	
	// stolen from the internet
	public static String byteArrayToHexString(byte[] b)
	{
		StringBuffer sb = new StringBuffer(b.length * 2);
		for (int i = 0; i < b.length; i++)
		{
			int v = b[i] & 0xff;
			if (v < 16) sb.append('0');
			sb.append(Integer.toHexString(v));
		}
		return sb.toString().toUpperCase();
	}

	public static boolean hasContent(EditText et) { return (et.getText().toString().trim().length() > 0); }

	private static Long dCounter = 0L;
	public static void uDebug(Context c, String tag, String msg)
	{
		Log.d(tag, msg + " <n" + dCounter.toString() + ">" );
		Toast.makeText(c, tag + " " + msg  + " <n" + dCounter.toString() + ">", Toast.LENGTH_LONG).show();

		++dCounter;
	}


	public static Bitmap encodeQrCode(String contents) throws WriterException
	{
		QRCode qrCode = new QRCode();
		Encoder.encode(contents, ErrorCorrectionLevel.L, qrCode); // commented momentanuesly because raises ErrorCorrectionLevel cannot find symbol ( no time to understand what zxing related library is missing... )
		
		System.out.println("encoded qrCode: "+qrCode);

		ByteMatrix result = qrCode.getMatrix();
		int codewidth = result.getWidth();
		int codeheight = result.getHeight();
		// +2 for quiet zone
		int imagewidth = result.getWidth() + 2;
		int imageheight = result.getHeight() + 2;
		int[] pixels = new int[imagewidth * imageheight];
		
		// init pixels with white:
		for (int i = 0; i < pixels.length; i++) pixels[i] = Color.WHITE;
		
		// set black pixels
		// use 1pixel quiet zone, this is enough for most readers, see
		// http://qrworld.wordpress.com/2011/08/09/the-quiet-zone/
		for (int y = 0; y < codeheight; y++)
		{
			//             +1 line horizontal for quiet zone
			int offset = (y+1) * imagewidth;
			for (int x = 0; x < codewidth; x++)
			{
				if( result.get(x, y) == 1 )
				{
					//                +1 pixel vertical for quiet zone
					pixels[offset + x +1] =  Color.BLACK;
				}
			}
		}

		Bitmap bitmap = Bitmap.createBitmap(imagewidth, imageheight, Bitmap.Config.ARGB_8888);
		bitmap.setPixels(pixels, 0, imagewidth, 0, 0, imagewidth, imageheight);
		bitmap = Bitmap.createScaledBitmap(bitmap, imagewidth*6, imageheight*6, false);

		return bitmap;
	}

	public static String getCertFromUri(Uri uri)
	{
		if( uri.getScheme().equals("retroshare") && uri.getHost().equals("certificate") )
		{
			String cert="-----BEGIN PGP PUBLIC KEY BLOCK-----\n";
			cert+=uri.getQueryParameter("gpgbase64");
			cert+="\n=";
			cert+=uri.getQueryParameter("gpgchecksum");
			cert+="\n-----END PGP PUBLIC KEY BLOCK-----\n";
			cert+="--SSLID--"+uri.getQueryParameter("sslid")+";--LOCATION--"+uri.getQueryParameter("location")+";\n";
			// Note: at locipp and extipp, the ';' is included in the query string
			cert+="--LOCAL--"+uri.getQueryParameter("locipp")+"--EXT--"+uri.getQueryParameter("extipp");
			return cert;
		}
		else
		{
			Log.e( TAG(), "getCertFromUri( "+ uri.toString() +" ): wrong scheme or host");
			return null;
		}
	}

	public static String encodeTobase64(Bitmap image)
	{
		Bitmap immagex=image;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		immagex.compress(Bitmap.CompressFormat.JPEG, 100, baos);
		byte[] b = baos.toByteArray();
		String imageEncoded = Base64.encodeToString(b, Base64.DEFAULT);

		return imageEncoded;
	}
	public static Bitmap decodeBase64(String input)
	{
		byte[] decodedByte = Base64.decode(input, 0);
		return BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);
	}
	public static Bitmap downScaleIfBiggerThenMaxDimension(Bitmap input, int maxDimension)
	{
		Bitmap ret = input;
		int h = input.getHeight();
		int w = input.getWidth();
		if( h > maxDimension || w > maxDimension)
		{
			int scaleRatio;
			if( h > w ) scaleRatio = maxDimension/h;
			else scaleRatio = maxDimension/w;
			ret = Bitmap.createScaledBitmap(input, w*scaleRatio, h*scaleRatio, false);
		}
		return ret;
	}
}

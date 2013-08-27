/**
 * @license
 *
 * Copyright (c) 2013 Gioacchino Mazzurco <gio@eigenlab.org>.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.retroshare.android.utils;

import android.content.ContentResolver;
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
import java.io.FileNotFoundException;

public class Util
{
	public static final String URI_REG_EXP = "([a-z]+[0-9a-z\\.\\-\\+]*:\\/\\/)(\\S)+"; // Probably too permissive, but I never saw a false positive in many tests

	public static String TAG() { return "Util"; }

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
		Encoder.encode(contents, ErrorCorrectionLevel.L, qrCode);
		
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

	public static String encodeTobase64(Bitmap image, Bitmap.CompressFormat format, int quality)
	{
		if(image == null) throw new NullPointerException("encodeTobase64 called with null argument");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		image.compress(format, quality, baos);
		return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
	}
	public static Bitmap decodeBase64(String input)
	{
		byte[] decodedByte = Base64.decode(input, 0);
		return BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);
	}
	public static Bitmap loadFittingBitmap(ContentResolver contentResolver, Uri uri, int maxDimension) throws FileNotFoundException
	{
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeStream(contentResolver.openInputStream(uri), null, options);
		int h = options.outHeight;
		int w = options.outWidth;
		if( h > maxDimension || w > maxDimension)
		{
			if( h > w ) options.inSampleSize = Math.round((float) h / (float) maxDimension);
			else options.inSampleSize = Math.round((float) w / (float) maxDimension);
		}
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeStream(contentResolver.openInputStream(uri), null, options);
	}

	public static int opposeColor(int colorToInvert)
	{
		int RGBMAX = 255;
		float[] hsv = new float[3];
		float H;
		Color.RGBToHSV( Color.red(colorToInvert),  RGBMAX - Color.green( colorToInvert), Color.blue(colorToInvert), hsv );
		H = (float) (hsv[0] + 0.5);
		if (H > 1) H -= 1;
		return Color.HSVToColor(hsv);
	}
}

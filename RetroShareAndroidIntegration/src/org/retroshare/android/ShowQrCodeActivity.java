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

package org.retroshare.android;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import org.retroshare.android.utils.Util;


public class ShowQrCodeActivity extends Activity
{
	public String TAG() { return "ShowQrCodeActivity"; }

	public static final String PGP_ID_EXTRA = "pgpId";
	public static final String NAME_EXTRA = "name";

    @Override
    public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Intent i = getIntent();
		if(i.hasExtra(PGP_ID_EXTRA) && i.hasExtra(NAME_EXTRA))
		{
			setContentView(R.layout.activity_show_qrcode);

			String name = i.getStringExtra(NAME_EXTRA);
			String pgpId = i.getStringExtra(PGP_ID_EXTRA);

			((TextView) findViewById(R.id.textViewTitleQrCode)).setText(name + " (" + pgpId + ")");

			try
			{
				// using format like retroshare://person?name=Just%20Relay%20It&hash=AA3BFD5CEEE7EC17
				Uri.Builder uriBuilder = new Uri.Builder();
				String data = uriBuilder
						.scheme(getString(R.string.retroshare_uri_scheme))
						.authority(getString(R.string.person_uri_authority))
						.appendQueryParameter(getString(R.string.name_uri_query_param), name)
						.appendQueryParameter(getString(R.string.hash_uri_query_param), pgpId)
						.build()
						.toString();

				Bitmap bm = Util.encodeQrCode(data);

				ImageView mImageView = (ImageView) findViewById(R.id.imageViewQrCode);
				mImageView.setImageBitmap(bm);
			}
			catch (Exception e) {}

		}
		else Log.wtf(TAG(), "onCreate() how we get here without data required data in the intent?");
	}
}

// mode BYTE version 22
//private String data="retroshare://certificate?sslid=4bab7a4eb473263295f4d726ecfdd7ff&gpgid=8931AA29&gpgbase64=xsBNBFBRzEMBCACuNNXThmv7XkDXse0aMXF5GOSJx+xsOZpDoqhbDsWjeK6Ar+JrxomX4dh2LQuIORpRBfeRA3/yBF6mBVK3BLwQX3P3RzkRpUq6pGwj1HmD4F3Nr8uuNJvJL9wBcK5Hye7HWSIbDfvjQ1/+c0LHfgiW0wdajhRZPtQEmCDkncTJ3RNQjmL7J0NLbybxC8WSrDWwLlO9mOpIq2oRwl1wQnjwU9wivGWBH0epaCgzEcwygY0WVq5a+Xcfxb63jgW1ZHm0kbrYyPNOzaVFhmWmInmb4FxO6NUl8f8YpzakIg3vohskzJeYJCQWjapx6tQQr06YXOf4lseDSxtSVL4Q8lvjABEBAAHNJWRldnggKEdlbmVyYXRlZCBieSBSZXRyb1NoYXJlKSA8ZGV2eD7CwF8EEwECABMFAlBRzEMJECPWaviJMaopAhkBAABVjQgAqC+JJP1NVgnuY42SQv71GLcBGxgD18gbfP73vLQzIEI85tk3tPQ5hpI/xl5Pp0pMpHDXxRrcTXXeCo75DCG+sm5ArokNIt7zr/9ge/pePvDO0HvHMMJbyGxTagnC6lEu+m9SRSSboT2gSmxTU+Yr3fsr2XPTd0fJk5WzIHt1lvcxFrIuPGlDvTj0B6P96Trhn4PdfPDSx3C6sRkxuA9vvv9Gk9jTkEe5CyLyCmPeJOGXTsOxBu0rI3T+e2z73VEywgyIEqUa+CTmZaAWxc0GuOPhT4bTpdtAFI1r4cCKjAej0UAoMiGXS3O1qV2cW9EoqjbeyVSHzONBahrZUDDUIQ==&gpgchecksum=f+Ib&location=devxhome&name=devx&locipp=192.168.1.102:39382;&extipp=109.192.144.39:39382;";
//
//private String data="Hallo echo was geht";
//
// mode ALPHANUMERIC version 18
//private String data="RETROSHARE://C/0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
//
// mode ALPHANUMERIC
//private String data="RETROSHARE://C/0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
//private String data="HALLO264876";
//
//lnge eines schlssels:
//
//neues format: 894 zeichen
//
//base 64 decodiert:
//649 bytes
//
//macht 944 alfanumerische zeichen fr den qrcode
//
//http://en.wikipedia.org/wiki/QR_code#Encoding
//
//rs cert url: 993 zeichen (bytes)

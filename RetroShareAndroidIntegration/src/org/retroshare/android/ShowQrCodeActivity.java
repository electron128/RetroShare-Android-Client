package org.retroshare.android;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import java.net.URLEncoder;

import rsctrl.core.Core.Person;


public class ShowQrCodeActivity extends ProxiedActivityBase
{
	private static final String TAG = "ShowQrCodeActivity";

    @Override
    public void onCreateBeforeConnectionInit(Bundle savedInstanceState) { setContentView(R.layout.activity_show_qrcode); }

    @Override
    protected void onServiceConnected()
	{
		RsPeersService ps = getConnectedServer().mRsPeersService;

		Person me = ps.getOwnPerson();
		String name = me.getName();

		TextView tv = (TextView) findViewById(R.id.textViewTitleQrCode);
		tv.setText(name);

        try
		{
			// using format like retroshare://person?name=Just%20Relay%20It&hash=AA3BFD5CEEE7EC17
			String data = "retroshare://person?name=" + URLEncoder.encode(name, "UTF-8") + "&hash=" + me.getGpgId();

			Bitmap bm = util.encodeQrCode(data);

			ImageView mImageView = (ImageView) findViewById(R.id.imageViewQrCode);
			mImageView.setImageBitmap(bm);
        }
		catch (Exception e) {}
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

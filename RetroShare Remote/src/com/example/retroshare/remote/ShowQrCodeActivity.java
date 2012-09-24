package com.example.retroshare.remote;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;


public class ShowQrCodeActivity extends RsActivityBase {
	private static final String TAG="ShowQrCodeActivity";
	
	
	//private final static String data="retroshare://certificate?sslid=4bab7a4eb473263295f4d726ecfdd7ff&gpgid=8931AA29&gpgbase64=xsBNBFBRzEMBCACuNNXThmv7XkDXse0aMXF5GOSJx+xsOZpDoqhbDsWjeK6Ar+JrxomX4dh2LQuIORpRBfeRA3/yBF6mBVK3BLwQX3P3RzkRpUq6pGwj1HmD4F3Nr8uuNJvJL9wBcK5Hye7HWSIbDfvjQ1/+c0LHfgiW0wdajhRZPtQEmCDkncTJ3RNQjmL7J0NLbybxC8WSrDWwLlO9mOpIq2oRwl1wQnjwU9wivGWBH0epaCgzEcwygY0WVq5a+Xcfxb63jgW1ZHm0kbrYyPNOzaVFhmWmInmb4FxO6NUl8f8YpzakIg3vohskzJeYJCQWjapx6tQQr06YXOf4lseDSxtSVL4Q8lvjABEBAAHNJWRldnggKEdlbmVyYXRlZCBieSBSZXRyb1NoYXJlKSA8ZGV2eD7CwF8EEwECABMFAlBRzEMJECPWaviJMaopAhkBAABVjQgAqC+JJP1NVgnuY42SQv71GLcBGxgD18gbfP73vLQzIEI85tk3tPQ5hpI/xl5Pp0pMpHDXxRrcTXXeCo75DCG+sm5ArokNIt7zr/9ge/pePvDO0HvHMMJbyGxTagnC6lEu+m9SRSSboT2gSmxTU+Yr3fsr2XPTd0fJk5WzIHt1lvcxFrIuPGlDvTj0B6P96Trhn4PdfPDSx3C6sRkxuA9vvv9Gk9jTkEe5CyLyCmPeJOGXTsOxBu0rI3T+e2z73VEywgyIEqUa+CTmZaAWxc0GuOPhT4bTpdtAFI1r4cCKjAej0UAoMiGXS3O1qV2cW9EoqjbeyVSHzONBahrZUDDUIQ==&gpgchecksum=f+Ib&location=devxhome&name=devx&locipp=192.168.1.102:39382;&extipp=109.192.144.39:39382;";
	private String data="Hallo echo was geht";
	
	
	
	ImageView mImageView;
	TextView mTextView;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_qrcode);
        
        mImageView=(ImageView) findViewById(R.id.imageViewQrCode);
        mTextView=(TextView) findViewById(R.id.textViewTitle);
        
        mTextView.setText(getIntent().getStringExtra("Description"));
    	data=getIntent().getStringExtra("Data");
        
        try {
	        Bitmap bm = util.encodeQrCode(data);
	
	        if(bm != null) {
	        	mImageView.setImageBitmap(bm);
	        }
        } catch (WriterException e) { }
        
    }
    
    @Override
    protected void onServiceConnected(){
    	
    }
    
    @Override
    public void onResume(){
    	super.onResume();
    	
    }
    @Override
    public void onPause(){
    	super.onPause();
    	
    }
}

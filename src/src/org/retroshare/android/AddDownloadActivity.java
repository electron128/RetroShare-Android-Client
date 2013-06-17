package org.retroshare.android;

import org.retroshare.java.RsCtrlService.RsMessage;

import com.google.protobuf.InvalidProtocolBufferException;

import rsctrl.core.Core;
import rsctrl.core.Core.File;
import rsctrl.core.Core.Status.StatusCode;
import rsctrl.files.Files.RequestControlDownload.Action;
import rsctrl.files.Files.ResponseControlDownload;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class AddDownloadActivity extends ProxiedActivityBase
{
	private static final String TAG="AddDownloadActivity";
	
	TextView textViewName;
	TextView textViewSize;
	TextView textViewHash;
	TextView textViewResult;
	
	Button buttonDownload;
	
	Core.File mFile=null;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_download);
        
    	textViewName=(TextView)findViewById(R.id.textViewName);
    	textViewSize=(TextView)findViewById(R.id.textViewSize);
    	textViewHash=(TextView)findViewById(R.id.textViewHash);
    	textViewResult=(TextView)findViewById(R.id.textViewResult);
    	
    	buttonDownload=(Button)findViewById(R.id.buttonDownload);
    	
    	buttonDownload.setVisibility(View.GONE);
    	
    	/*
    	try {
			mFile=File.parseFrom(getIntent().getByteArrayExtra("File"));
		} catch (InvalidProtocolBufferException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
    	if(getIntent().hasExtra("File")){
    		try {
				mFile = File.parseFrom(getIntent().getByteArrayExtra("File"));
			} catch (InvalidProtocolBufferException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}else{
	    	Uri uri = getIntent().getData();
	    	mFile=File.newBuilder()
	    			.setName(uri.getQueryParameter("name"))
	    			.setHash(uri.getQueryParameter("hash"))
	    			.setSize(Long.parseLong(uri.getQueryParameter("size")))
	    			.build();
    	}
    }
    
    @Override
    protected void onServiceConnected()
	{
    	if(mFile!=null){
        	textViewName.setText(mFile.getName());
        	textViewSize.setText(Long.toString(mFile.getSize()));
        	textViewHash.setText(mFile.getHash());
    	}
    	if(getConnectedServer().isOnline()){
    		buttonDownload.setVisibility(View.VISIBLE);
    		textViewResult.setVisibility(View.GONE);
    	}else{
    		buttonDownload.setVisibility(View.GONE);
    		textViewResult.setVisibility(View.VISIBLE);
    		textViewResult.setText("you have to be connected to download a file");
    	}
    }
    
    
    public void onButtonDownloadClick(View v)
	{
    	buttonDownload.setVisibility(View.GONE);
    	textViewResult.setVisibility(View.VISIBLE);
    	textViewResult.setText("processing...");
    	if(mFile!=null){
        	getConnectedServer().filesService.sendRequestControlDownload(mFile, Action.ACTION_START,new RsMessageHandler(){

				@Override
				protected void rsHandleMsg(RsMessage msg) {
					try {
						ResponseControlDownload resp=ResponseControlDownload.parseFrom(msg.body);
						//textViewResult.setText(resp.getStatus().toString());
			    		if(resp.getStatus().getCode().equals(StatusCode.SUCCESS)){
			    			textViewResult.setText("ok");
			    		}else{
			    			textViewResult.setText("nok");
			    		}
					} catch (InvalidProtocolBufferException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
        		
        	});
    	}
    }
}

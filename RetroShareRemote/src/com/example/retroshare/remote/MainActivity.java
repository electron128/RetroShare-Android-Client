package com.example.retroshare.remote;

import java.text.DecimalFormat;

import rsctrl.core.Core;
import rsctrl.system.System.RequestSystemStatus;
import rsctrl.system.System.ResponseSystemStatus;

import com.example.retroshare.remote.RsCtrlService.RsCtrlServiceListener;
import com.example.retroshare.remote.RsCtrlService.RsMessage;
import com.google.protobuf.InvalidProtocolBufferException;

import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends RsActivityBase implements RsCtrlServiceListener {
	private static final String TAG="MainActivity";
	
	private static final int UPDATE_INTERVALL=1000;
	
	private TextView textViewConnectionState;
	
	private TextView textViewNetStatus;
	private TextView textViewNoPeers;
	private TextView textViewBandwidth;
	
	private Handler mHandler;
	
	private boolean isInForeground=false;
	
	private RsServerData mServerData;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_2);
        
        textViewConnectionState=(TextView) findViewById(R.id.textViewConnectionState);
        
    	textViewNetStatus=(TextView) findViewById(R.id.textViewNetStatus);
    	textViewNoPeers=(TextView) findViewById(R.id.textViewNoPeers);
    	textViewBandwidth=(TextView) findViewById(R.id.textViewBandwidth);
    	
    	mHandler=new Handler();
    	mHandler.postAtTime(new requestSystemStatusRunnable(), SystemClock.uptimeMillis()+UPDATE_INTERVALL);
        
    }
    
    @Override
    protected void onServiceConnected(){
    	mServerData=mRsService.mRsCtrlService.getServerData();
    	mRsService.mRsCtrlService.registerListener(this);
    	mServerData=mRsService.mRsCtrlService.getServerData();
    	updateViews();
    }
    
    @Override
    public void onResume(){
    	super.onResume();
    	if(mRsService!=null){
        	mServerData=mRsService.mRsCtrlService.getServerData();
        	updateViews();
    	}
    	isInForeground=true;
    }
    @Override
    public void onPause(){
    	super.onPause();
    	isInForeground=false;
    }
    
    private void updateViews(){
    	if(mBound){
    		if(mRsService.mRsCtrlService.isOnline()){
    			
    	    	TextView textViewServerKey=(TextView) findViewById(R.id.textViewServerKey);
    	    	try{
    	    		textViewServerKey.setText("Server Key:"+mServerData.getHostkeyFingerprint());//+mServerData.hostkey);
    	    	}catch(NullPointerException e){
    	    		textViewServerKey.setText("Server Key: Error in sd.hostkey.toString");
    	    		e.printStackTrace();
    	    	}
            	textViewConnectionState.setText("server: "+mServerData.hostname+":"+Integer.toString(mServerData.port));
    		}else{
    			// error printing is done in the login activity
    			finish();
    		}
    	}else{
    		Log.e(TAG,"Error: MainActivity.updateViews(): not bound");
    	}
    }
    
    public void deleteServerKey(View v){
    	mServerData.hostkey=null;
    	mRsService.mRsCtrlService.setServerData(mServerData);
    }

    public void showPeers(View v){
    	Intent intent = new Intent(this, PeersActivity.class);
    	startActivity(intent);
    }
    
    public void showChatLobbies(View v){
    	Intent intent = new Intent(this, ChatlobbyActivity.class);
    	startActivity(intent);
    }
    
    public void onShowQrCode(View v){
    	Intent intent = new Intent(this, ShowQrCodeActivity.class);
    	intent.putExtra("Description", "just a test");
    	intent.putExtra("Data", "just a test");
    	startActivity(intent);
    }
    
    public void showFilesActivity(View v){
    	Intent intent = new Intent(this, FilesActivity.class);
    	startActivity(intent);
    }
    
    public void showSearchActivity(View v){
    	Intent intent = new Intent(this, ListSearchesActivity.class);
    	startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

	@Override
	public void onConnectionStateChanged(RsCtrlService.ConnectionEvent ce) {
		//Log.v(TAG,"MainActivity.onConnectionStateChanged()");
		updateViews();
	}
	
	private void requestSystemStatus(){
    	if(mBound && mRsService.mRsCtrlService.isOnline()){
			RsMessage msg=new RsMessage();
			msg.msgId=(Core.ExtensionId.CORE_VALUE<<24)|(Core.PackageId.SYSTEM_VALUE<<8)|rsctrl.system.System.RequestMsgIds.MsgId_RequestSystemStatus_VALUE;
			msg.body=RequestSystemStatus.newBuilder().build().toByteArray();
			mRsService.mRsCtrlService.sendMsg(msg, new SystemStatusHandler());
    	}
	}
	
	private class requestSystemStatusRunnable implements Runnable{
		@Override
		public void run() {
			if(isInForeground){
				requestSystemStatus();
			}
			mHandler.postAtTime(new requestSystemStatusRunnable(), SystemClock.uptimeMillis()+UPDATE_INTERVALL);
		}
	}
	
	private class SystemStatusHandler extends RsMessageHandler{
		@Override
		protected void rsHandleMsg(RsMessage msg){
			ResponseSystemStatus resp;
			try {
				resp = ResponseSystemStatus.parseFrom(msg.body);
		    	textViewNetStatus.setText(getResources().getText(R.string.network_status)+":\n"+resp.getNetStatus().toString());
		    	textViewNoPeers.setText(getResources().getText(R.string.peers)+": "+Integer.toString(resp.getNoConnected())+"/"+Integer.toString(resp.getNoPeers()));
		    	DecimalFormat df = new DecimalFormat("#.##");
		    	textViewBandwidth.setText(getResources().getText(R.string.bandwidth_up_down)+":\n"+df.format(resp.getBwTotal().getUp())+"/"+df.format(resp.getBwTotal().getDown())+" (kB/s)");
		    	
		    	textViewNetStatus.setVisibility(View.VISIBLE);
		    	textViewNoPeers.setVisibility(View.VISIBLE);
		    	textViewBandwidth.setVisibility(View.VISIBLE);
			} catch (InvalidProtocolBufferException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}
}

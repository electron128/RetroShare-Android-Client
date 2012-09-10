package com.example.retroshare.remote;

import com.example.retroshare.remote.RsService.RsBinder;
import com.example.retroshare.remote.RsService.RsMessage;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class RsActivityBase extends Activity {
	protected RsService mRsService;
	protected boolean mBound=false;
	
	private static final String TAG="RsActivityBase";
	
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            RsBinder binder = (RsBinder) service;
            mRsService = binder.getService();
            mBound = true;
            Log.v(TAG, "onServiceConnected");
            RsActivityBase.this.onServiceConnected();
        }
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
            Log.v(TAG, "onServiceDisconnected");
        }
    };
    
    //should be overridden by child classes
    protected void onServiceConnected(){}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(this, RsService.class);
        if(bindService(intent, mConnection, Context.BIND_AUTO_CREATE)){
        	Log.v(TAG, "onCreate: bindService returned true");
        }
        else{
        	Log.e(TAG, "onCreate: bindService returned false");
        }

    }
    
    @Override
    public void onDestroy(){
    	super.onDestroy();
    	unbindService(mConnection);
    }
}

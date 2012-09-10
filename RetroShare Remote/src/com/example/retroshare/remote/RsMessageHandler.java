package com.example.retroshare.remote;

import android.os.Handler;

import com.example.retroshare.remote.RsCtrlService.RsMessage;

public class RsMessageHandler extends Handler implements Runnable{
	// must be implemented in child class
	// will be called by run()
	protected void rsHandleMsg(RsMessage msg){}
	
	private RsMessage mMsg;
	
	public void setMsg(RsMessage m){
		mMsg=m;
	}
	@Override
	public void run() {
		rsHandleMsg(mMsg);
	}
	
}

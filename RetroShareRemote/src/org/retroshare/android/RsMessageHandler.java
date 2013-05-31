package org.retroshare.android;

import android.os.Handler;

import org.retroshare.android.RsCtrlService.RsMessage;

public abstract class RsMessageHandler extends Handler implements Runnable{
	
	// must be implemented in child class
	// will be called by run()
	abstract protected void rsHandleMsg(RsMessage msg);
	
	private RsMessage mMsg;
	
	public void setMsg(RsMessage m){
		mMsg=m;
	}
	@Override
	public void run() {
		rsHandleMsg(mMsg);
	}
	
}

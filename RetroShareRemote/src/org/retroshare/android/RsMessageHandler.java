
package org.retroshare.android;


import android.os.Handler;

import org.retroshare.android.RsCtrlService.RsMessage;

/**
 *	This is an abstract class used as base for received messages handlers   
 */
public abstract class RsMessageHandler extends Handler implements Runnable
{
	
	/**
	 * Should be implemented in child class, it is a callback called by run() when a message to handle is received
	 * @param msg The message to handle
	 */
	protected void rsHandleMsg(RsMessage msg) {}
	
	private RsMessage mMsg;
	
	public void setMsg(RsMessage m) { mMsg=m; }
	
	@Override
	public void run() { rsHandleMsg(mMsg); }
}

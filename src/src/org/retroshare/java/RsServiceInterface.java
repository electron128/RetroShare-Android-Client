package org.retroshare.java;

import org.retroshare.java.RsCtrlService.RsMessage;


public interface RsServiceInterface
{
	public void handleMessage(RsMessage m);
}

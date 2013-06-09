package org.retroshare.android;

import org.retroshare.android.RsCtrlService.RsMessage;

public interface ServiceInterface
{
	public void handleMessage(RsMessage m);
}

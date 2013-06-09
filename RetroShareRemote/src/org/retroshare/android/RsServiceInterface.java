package org.retroshare.android;

import org.retroshare.android.RsCtrlService.RsMessage;

public interface RsServiceInterface
{
	public void handleMessage(RsMessage m);
}

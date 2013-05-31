package com.example.retroshare.remote;

import com.example.retroshare.remote.RsCtrlService.RsMessage;

public interface ServiceInterface {
	public void handleMessage(RsMessage m);
}

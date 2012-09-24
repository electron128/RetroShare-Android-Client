package com.example.retroshare.remote;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rsctrl.chat.Chat;
import rsctrl.chat.Chat.RequestChatLobbies;
import rsctrl.core.Core;
import rsctrl.core.Core.File;
import rsctrl.core.Core.Status.StatusCode;
import rsctrl.files.Files;
import rsctrl.files.Files.RequestControlDownload;
import rsctrl.files.Files.RequestControlDownload.Action;
import rsctrl.files.Files.RequestTransferList;
import rsctrl.files.Files.ResponseTransferList;
import rsctrl.peers.Peers.ResponsePeerList;

import com.example.retroshare.remote.RsCtrlService.RsMessage;
import com.google.protobuf.InvalidProtocolBufferException;

public class FilesService implements ServiceInterface{
	RsCtrlService mRsCtrlService;
	FilesService(RsCtrlService s){
		mRsCtrlService=s;
	}
	
	public static interface FilesServiceListener{
		public void update();
	}
	
	private Set<FilesServiceListener>mListeners=new HashSet<FilesServiceListener>();
	public void registerListener(FilesServiceListener l){
		mListeners.add(l);
	}
	public void unregisterListener(FilesServiceListener l){
		mListeners.remove(l);
	}
	private void _notifyListeners(){
		for(FilesServiceListener l:mListeners){
			l.update();
		}
	}
	
	List<Files.FileTransfer> transfersUp=new ArrayList<Files.FileTransfer>();
	List<Files.FileTransfer> transfersDown=new ArrayList<Files.FileTransfer>();
	
	public void updateTransfers(Files.Direction d){
		RequestTransferList.Builder reqb=RequestTransferList.newBuilder();
    	reqb.setDirection(d);
    	RsMessage msg=new RsMessage();
    	msg.msgId=(Core.ExtensionId.CORE_VALUE<<24)|(Core.PackageId.FILES_VALUE<<8)|Files.RequestMsgIds.MsgId_RequestTransferList_VALUE;
    	msg.body=reqb.build().toByteArray();
    	mRsCtrlService.sendMsg(msg,new ResponseTransferListHandler(d));
	}
	
	private class ResponseTransferListHandler extends RsMessageHandler{
		Files.Direction mDirection;
		
		ResponseTransferListHandler(Files.Direction d){
			super();
			mDirection=d;
		}

		@Override
		protected void rsHandleMsg(RsMessage msg) {
			try {
				ResponseTransferList resp=ResponseTransferList.parseFrom(msg.body);
				if(resp.getStatus().getCode().equals(StatusCode.SUCCESS)){
					List<Files.FileTransfer> files=ResponseTransferList.parseFrom(msg.body).getTransfersList();
					if(mDirection.equals(Files.Direction.DIRECTION_UPLOAD)){
						transfersUp=files;
					}else{
						transfersDown=files;
					}
					_notifyListeners();
				}
			} catch (InvalidProtocolBufferException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void handleMessage(RsMessage m) {
		// TODO Auto-generated method stub
		
	}
	public void sendRequestControlDownload(File file, Action action) {
		sendRequestControlDownload(file,action,null);
	}
	public void sendRequestControlDownload(File file, Action action,RsMessageHandler handler) {
    	RsMessage msg=new RsMessage();
    	msg.msgId=(Core.ExtensionId.CORE_VALUE<<24)|(Core.PackageId.FILES_VALUE<<8)|Files.RequestMsgIds.MsgId_RequestControlDownload_VALUE;
    	msg.body=RequestControlDownload.newBuilder().setFile(file).setAction(action).build().toByteArray();
    	mRsCtrlService.sendMsg(msg,handler);
	}
	
}

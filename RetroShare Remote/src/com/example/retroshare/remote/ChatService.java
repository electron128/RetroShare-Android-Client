package com.example.retroshare.remote;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rsctrl.chat.Chat;
import rsctrl.chat.Chat.ChatId;
import rsctrl.chat.Chat.ChatMessage;
import rsctrl.chat.Chat.RequestChatLobbies;
import rsctrl.chat.Chat.RequestSendMessage;
import rsctrl.chat.Chat.ResponseChatLobbies;
import rsctrl.core.Core;

import com.example.retroshare.remote.RsCtrlService.RsMessage;
import com.google.protobuf.InvalidProtocolBufferException;

public class ChatService implements ServiceInterface{
	RsCtrlService mRsCtrlService;
	ChatService(RsCtrlService s){
		mRsCtrlService=s;
	}
	
	public static interface ChatServiceListener{
		public void update();
	}
	
	private Set<ChatServiceListener>mListeners=new HashSet<ChatServiceListener>();
	public void registerListener(ChatServiceListener l){
		mListeners.add(l);
	}
	public void unregisterListener(ChatServiceListener l){
		mListeners.remove(l);
	}
	private void _notifyListeners(){
		for(ChatServiceListener l:mListeners){
			l.update();
		}
	}
	
	private List<Chat.ChatLobbyInfo> ChatLobbies=new ArrayList<Chat.ChatLobbyInfo>();
	private Map<ChatId,List<ChatMessage>> ChatHistory=new HashMap<ChatId,List<ChatMessage>>();
	
	public void updateChatLobbies(){
    	RequestChatLobbies.Builder reqb=RequestChatLobbies.newBuilder();
    	reqb.setLobbySet(RequestChatLobbies.LobbySet.LOBBYSET_ALL);
    	RsMessage msg=new RsMessage();
    	msg.msgId=(Core.ExtensionId.CORE_VALUE<<24)|(Core.PackageId.CHAT_VALUE<<8)|Chat.RequestMsgIds.MsgId_RequestChatLobbies_VALUE;
    	msg.body=reqb.build().toByteArray();
    	mRsCtrlService.sendMsg(msg);
	}
	
	public List<Chat.ChatLobbyInfo>
	getChatLobbies(){
		return ChatLobbies;
	}
	
	public List<ChatMessage> 
	getChatHistoryForChatId(ChatId id){
		if(ChatHistory.get(id)==null){
			return new ArrayList<ChatMessage>();
		}
		else{
			return ChatHistory.get(id);
		}
	}

	@Override
	public void handleMessage(RsMessage msg) {
		
		// response ChatLobbies
		if(msg.msgId==(RsCtrlService.RESPONSE|(Core.PackageId.CHAT_VALUE<<8)|Chat.ResponseMsgIds.MsgId_ResponseChatLobbies_VALUE)){
			System.err.println("received Chat.ResponseMsgIds.MsgId_ResponseChatLobbies_VALUE");
			try {
				ResponseChatLobbies resp=Chat.ResponseChatLobbies.parseFrom(msg.body);
				ChatLobbies=resp.getLobbiesList();
				_notifyListeners();
			} catch (InvalidProtocolBufferException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	

	
	
	public void sendChatMessage(ChatMessage m){
    	RsMessage msg=new RsMessage();
    	msg.msgId=(Core.ExtensionId.CORE_VALUE<<24)|(Core.PackageId.CHAT_VALUE<<8)|Chat.RequestMsgIds.MsgId_RequestSendMessage_VALUE;
    	msg.body=RequestSendMessage.newBuilder().setMsg(m).build().toByteArray();
    	mRsCtrlService.sendMsg(msg, null);
    	_addChatMessageToHistory(m);
	}
	
	private void _addChatMessageToHistory(ChatMessage m){
		if(ChatHistory.get(m.getId())==null){
			ChatHistory.put(m.getId(), new ArrayList<ChatMessage>());
		}
		ChatHistory.get(m.getId()).add(m);
		_notifyListeners();
	}
	
	
}

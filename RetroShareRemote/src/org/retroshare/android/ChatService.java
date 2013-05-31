package org.retroshare.android;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rsctrl.chat.Chat;
import rsctrl.chat.Chat.ChatId;
import rsctrl.chat.Chat.ChatLobbyInfo;
import rsctrl.chat.Chat.ChatMessage;
import rsctrl.chat.Chat.ChatType;
import rsctrl.chat.Chat.EventChatMessage;
import rsctrl.chat.Chat.RequestChatLobbies;
import rsctrl.chat.Chat.RequestJoinOrLeaveLobby;
import rsctrl.chat.Chat.RequestRegisterEvents;
import rsctrl.chat.Chat.RequestSendMessage;
import rsctrl.chat.Chat.ResponseChatLobbies;
import rsctrl.chat.Chat.ResponseMsgIds;
import rsctrl.core.Core;
import rsctrl.core.Core.Person;

import org.retroshare.android.RsCtrlService.RsMessage;
import com.google.protobuf.InvalidProtocolBufferException;

public class ChatService implements ServiceInterface, RsCtrlService.RsCtrlServiceListener{
	RsCtrlService mRsCtrlService;
	ChatService(RsCtrlService s){
		mRsCtrlService=s;
		mRsCtrlService.registerListener(this);
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
	private Map<ChatId,Boolean> ChatChanged=new HashMap<ChatId,Boolean>();
	private ChatId NotifyBlockedChat;
	
	private ChatMessage lastPrivateChatMessage;
	private ChatMessage lastChatlobbyMessage;
	
	public ChatMessage getLastPrivateChatMessage(){
		return lastPrivateChatMessage;
	}
	public ChatMessage getLastChatlobbyMessage(){
		return lastChatlobbyMessage;
	}
	
	
	public void setNotifyBlockedChat(ChatId id){
		NotifyBlockedChat=id;
		if(id!=null){
			if(lastPrivateChatMessage!=null){
				if(id.equals(lastPrivateChatMessage.getId())){
					lastPrivateChatMessage=null;
				}
			}
			if(lastChatlobbyMessage!=null){
				if(id.equals(lastChatlobbyMessage.getId())){
					lastChatlobbyMessage=null;
				}
			}
			clearChatChanged(id);
		}
	}
	public void clearChatChanged(ChatId ci){
		ChatChanged.put(ci, false);
		_notifyListeners();
	}
	public Map<ChatId,Boolean> getChatChanged(){
		return ChatChanged;
	}
	
	public void updateChatLobbies(){
    	RequestChatLobbies.Builder reqb=RequestChatLobbies.newBuilder();
    	reqb.setLobbySet(RequestChatLobbies.LobbySet.LOBBYSET_ALL);
    	RsMessage msg=new RsMessage();
    	msg.msgId=(Core.ExtensionId.CORE_VALUE<<24)|(Core.PackageId.CHAT_VALUE<<8)|Chat.RequestMsgIds.MsgId_RequestChatLobbies_VALUE;
    	msg.body=reqb.build().toByteArray();
    	requestChatLobbiesRequestId=mRsCtrlService.sendMsg(msg);
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
	
	
	
	
	int requestChatLobbiesRequestId=0;

	@Override
	public void handleMessage(RsMessage msg) {
		
		// check reqId, because JoinOrLeaveLobby answers with ResponseChatLobbies, but without ChatLobbyInfos
		if(msg.reqId==requestChatLobbiesRequestId){
			// response ChatLobbies
			if(msg.msgId==(RsCtrlService.RESPONSE|(Core.PackageId.CHAT_VALUE<<8)|Chat.ResponseMsgIds.MsgId_ResponseChatLobbies_VALUE)){
				System.err.println("received Chat.ResponseMsgIds.MsgId_ResponseChatLobbies_VALUE");
				try {
					ResponseChatLobbies resp=Chat.ResponseChatLobbies.parseFrom(msg.body);
					ChatLobbies=resp.getLobbiesList();
					
					System.err.println("ChatService::handleMessage: ResponseChatLobbies\n"+ChatLobbies);
					
					_notifyListeners();
				} catch (InvalidProtocolBufferException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		// response ChatMessage
		if(msg.msgId==(RsCtrlService.RESPONSE|(Core.PackageId.CHAT_VALUE<<8)|ResponseMsgIds.MsgId_EventChatMessage_VALUE)){
			System.err.println("received Chat.ResponseMsgIds.MsgId_EventChatMessage_VALUE");
			try {
				EventChatMessage resp=EventChatMessage.parseFrom(msg.body);
				
				ChatMessage m=resp.getMsg();
				// ad name information, and update lastChatMessage
		    	if(m.getId().getChatType()==ChatType.TYPE_LOBBY){
		    		// we have lobby
		    		// so names are included in the ChatMessage
		    		
		    		lastChatlobbyMessage=m;
		    	}else{
		    		// private chat, we have to add names
		    		Person p=mRsCtrlService.peersService.getPersonFromSslId(m.getId().getChatId());
			    	if(p!=null){
			    		m=ChatMessage.newBuilder().setId(m.getId()).setMsg(m.getMsg()).setPeerNickname(
				    				p.getName()
				    			).build();
			    	}
			    	lastPrivateChatMessage=m;
		    	}
		    	
				_addChatMessageToHistory(m);
				//_addChatMessageToHistory() will notify Listeners
				
				System.err.println(resp.getMsg());
				
			} catch (InvalidProtocolBufferException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
	}
	
	// called once in onConnected()
	public void registerForEventsAtServer(){
		RequestRegisterEvents.Builder reqb= RequestRegisterEvents.newBuilder();
		reqb.setAction(RequestRegisterEvents.RegisterAction.REGISTER);
		
    	RsMessage msg=new RsMessage();
    	msg.msgId=(Core.ExtensionId.CORE_VALUE<<24)|(Core.PackageId.CHAT_VALUE<<8)|Chat.RequestMsgIds.MsgId_RequestRegisterEvents_VALUE;
    	msg.body=reqb.build().toByteArray();
    	mRsCtrlService.sendMsg(msg);
	}
	
	

	public void joinChatLobby(ChatLobbyInfo li){
		
		RequestJoinOrLeaveLobby.Builder reqb= RequestJoinOrLeaveLobby.newBuilder();
		reqb.setLobbyId(li.getLobbyId());
		reqb.setAction(RequestJoinOrLeaveLobby.LobbyAction.JOIN_OR_ACCEPT);
		
    	RsMessage msg=new RsMessage();
    	msg.msgId=(Core.ExtensionId.CORE_VALUE<<24)|(Core.PackageId.CHAT_VALUE<<8)|Chat.RequestMsgIds.MsgId_RequestJoinOrLeaveLobby_VALUE;
    	msg.body=reqb.build().toByteArray();
    	mRsCtrlService.sendMsg(msg);
	}
	
	public void leaveChatLobby(ChatLobbyInfo li){
		RequestJoinOrLeaveLobby.Builder reqb= RequestJoinOrLeaveLobby.newBuilder();
		reqb.setLobbyId(li.getLobbyId());
		reqb.setAction(RequestJoinOrLeaveLobby.LobbyAction.LEAVE_OR_DENY);
		
    	RsMessage msg=new RsMessage();
    	msg.msgId=(Core.ExtensionId.CORE_VALUE<<24)|(Core.PackageId.CHAT_VALUE<<8)|Chat.RequestMsgIds.MsgId_RequestJoinOrLeaveLobby_VALUE;
    	msg.body=reqb.build().toByteArray();
    	mRsCtrlService.sendMsg(msg);
	}
	
	public void sendChatMessage(ChatMessage m){
		System.err.println("ChatService: Sending Message:\n"+m);
		
    	RsMessage msg=new RsMessage();
    	msg.msgId=(Core.ExtensionId.CORE_VALUE<<24)|(Core.PackageId.CHAT_VALUE<<8)|Chat.RequestMsgIds.MsgId_RequestSendMessage_VALUE;
    	msg.body=RequestSendMessage.newBuilder().setMsg(m).build().toByteArray();
    	mRsCtrlService.sendMsg(msg);
    	
		// ad name information
    	if(m.getId().getChatType().equals(ChatType.TYPE_LOBBY)){
    		System.err.println("ChatService::_sendChatMessage: ChatLobbies:\n"+ChatLobbies);
    		// we have lobby
    		for(ChatLobbyInfo i:ChatLobbies){
    			if(i.getLobbyId().equals(m.getId().getChatId())){
    				System.err.println("ChatService::_sendChatMessage: Lobby found");
	    			// no nick set
	    			if(i.getLobbyNickname().equals("")){
	    				m=ChatMessage.newBuilder().setId(m.getId()).setMsg(m.getMsg()).setPeerNickname(mRsCtrlService.peersService.getOwnPerson().getName()).build();
	    			}
	    			//nick set
	    			else{
	    				m=ChatMessage.newBuilder().setId(m.getId()).setMsg(m.getMsg()).setPeerNickname(i.getLobbyNickname()).build();
	    			}
	    			break;
    			}
    		}
    		
    	}else{
    		// private chat
	    	if(mRsCtrlService.peersService.getOwnPerson()!=null){
	    		m=ChatMessage.newBuilder().setId(m.getId()).setMsg(m.getMsg()).setPeerNickname(mRsCtrlService.peersService.getOwnPerson().getName()).build();
	    	}else{
	    		// no name available
	    	}
    	}
    	
    	_addChatMessageToHistory(m);
	}
	
	private void _addChatMessageToHistory(ChatMessage m){
		
		if(ChatHistory.get(m.getId())==null){
			ChatHistory.put(m.getId(), new ArrayList<ChatMessage>());
		}

		ChatHistory.get(m.getId()).add(m);
		if(! m.getId().equals(NotifyBlockedChat)){
			ChatChanged.put(m.getId(), true);
		}
		_notifyListeners();
	}
	
	@Override
	public void onConnectionStateChanged(RsCtrlService.ConnectionEvent ce) {
		if(ce==RsCtrlService.ConnectionEvent.CONNECTED){
			registerForEventsAtServer();
		}
	}
	
	
}

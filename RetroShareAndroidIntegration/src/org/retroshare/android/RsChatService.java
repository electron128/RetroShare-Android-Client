package org.retroshare.android;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
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
import org.retroshare.android.utils.WeakHashSet;

import com.google.protobuf.InvalidProtocolBufferException;

public class RsChatService implements RsServiceInterface, RsCtrlService.RsCtrlServiceListener
{
	public String TAG() { return "RsChatService"; }

	RsCtrlService mRsCtrlService;
	UiThreadHandlerInterface mUiThreadHandler;

	RsChatService(RsCtrlService s, UiThreadHandlerInterface u)
	{
		mRsCtrlService = s;
		mRsCtrlService.registerListener(this);
		mUiThreadHandler = u;
	}
	
	public static interface ChatServiceListener
	{
		public void update();
	}
	
	private Set<ChatServiceListener> mListeners = new WeakHashSet<ChatServiceListener>();
	public void registerListener(ChatServiceListener l) { mListeners.add(l); }
	public void unregisterListener(ChatServiceListener l){ mListeners.remove(l); }
	private void _notifyListeners() { if(mUiThreadHandler != null) mUiThreadHandler.postToUiThread( new Runnable() { @Override public void run() { for(ChatServiceListener l : mListeners) l.update(); }	} ); }
	
	private List<Chat.ChatLobbyInfo> mChatLobbies = new ArrayList<Chat.ChatLobbyInfo>();
	private Map<ChatId,List<ChatMessage>> mChatHistory = new HashMap<ChatId,List<ChatMessage>>();
	private Map<ChatId,Boolean> mChatChanged = new HashMap<ChatId,Boolean>();
	private Set<ChatId> notificationDisabledChats = new HashSet<ChatId>();
	
	private ChatMessage lastPrivateChatMessage;
	private ChatMessage lastChatlobbyMessage;
	
	public ChatMessage getLastPrivateChatMessage() { return lastPrivateChatMessage; }
	public ChatMessage getLastChatlobbyMessage() { return lastChatlobbyMessage; }
	
	
	public void disableNotificationForChat(ChatId id)
    {
		if( id != null )
		{
			Collection<ChatId> c = new HashSet<ChatId>();
			c.add(id);
			disableNotificationForChats(c);
		}
		else Log.wtf(TAG(), "disableNotificationForChat(null)");
	}
	public void disableNotificationForChats(Collection<ChatId> ids)
	{
		if(ids != null)
		{
			for ( ChatId chatId : ids )
			{
				if( chatId != null )
				{
					if( lastPrivateChatMessage != null && chatId.equals(lastPrivateChatMessage.getId()) ) lastPrivateChatMessage = null;
					if( lastChatlobbyMessage != null && chatId.equals(lastChatlobbyMessage.getId()) ) lastChatlobbyMessage = null;

					clearChatChanged(chatId);
				}
				else ids.remove(chatId);
			}

			notificationDisabledChats.addAll(ids);
		}
		else Log.wtf(TAG(), "disableNotificationForChats(null)" );
	}

	public void enableNotificationForChat(ChatId id)
	{
		if( id != null) notificationDisabledChats.remove(id);
		else Log.wtf(TAG(), "enableNotificationForChat(null)");
	}
	public void enableNotificationForChats(Collection<ChatId> ids)
	{
		if ( ids != null ) notificationDisabledChats.removeAll(ids);
		else Log.wtf(TAG(), "enableNotificationForChats(null)");
	}

	public void clearChatChanged(ChatId ci)
	{
		mChatChanged.put(ci, false);
		_notifyListeners();
	}
	public Map<ChatId,Boolean> getChatChanged() { return mChatChanged; }
	
	public void updateChatLobbies()
	{
    	RequestChatLobbies.Builder reqb = RequestChatLobbies.newBuilder();
    	reqb.setLobbySet(RequestChatLobbies.LobbySet.LOBBYSET_ALL);
    	RsMessage msg = new RsMessage();
    	msg.msgId = (Core.ExtensionId.CORE_VALUE<<24)|(Core.PackageId.CHAT_VALUE<<8)|Chat.RequestMsgIds.MsgId_RequestChatLobbies_VALUE;
    	msg.body = reqb.build().toByteArray();
    	requestChatLobbiesRequestId = mRsCtrlService.sendMsg(msg);
	}
	
	public List<Chat.ChatLobbyInfo> getChatLobbies() { return mChatLobbies; }
	
	public List<ChatMessage> getChatHistoryForChatId(ChatId id)
	{
		if( mChatHistory.get(id) == null ) return new ArrayList<ChatMessage>();
		return mChatHistory.get(id);
	}

	int requestChatLobbiesRequestId = 0;

	@Override
	public void handleMessage(RsMessage msg)
	{
		
		// check reqId, because JoinOrLeaveLobby answers with ResponseChatLobbies, but without ChatLobbyInfos
		if( msg.reqId == requestChatLobbiesRequestId )
		{
			// response mChatLobbies
			if( msg.msgId == ( RsCtrlService.RESPONSE | (Core.PackageId.CHAT_VALUE<<8) | Chat.ResponseMsgIds.MsgId_ResponseChatLobbies_VALUE ) )
			{
				System.err.println("received Chat.ResponseMsgIds.MsgId_ResponseChatLobbies_VALUE");
				try
				{
					ResponseChatLobbies resp = Chat.ResponseChatLobbies.parseFrom(msg.body);
					mChatLobbies = resp.getLobbiesList();
					
					System.err.println("RsChatService::handleMessage: ResponseChatLobbies\n"+ mChatLobbies);
					
					_notifyListeners();
				}
				catch (InvalidProtocolBufferException e) { e.printStackTrace(); }
			}
		}
		
		// response ChatMessage
		if( msg.msgId == (RsCtrlService.RESPONSE|(Core.PackageId.CHAT_VALUE<<8)|ResponseMsgIds.MsgId_EventChatMessage_VALUE) )
		{
			System.err.println("received Chat.ResponseMsgIds.MsgId_EventChatMessage_VALUE");
			try
			{
				EventChatMessage resp = EventChatMessage.parseFrom(msg.body);
				
				ChatMessage m = resp.getMsg();
				// add name information, and update lastChatMessage
		    	if(m.getId().getChatType() == ChatType.TYPE_LOBBY)
				{
		    		// we have lobby
		    		// so names are included in the ChatMessage
		    		
		    		lastChatlobbyMessage = m;
		    	}
				else
				{
		    		// private chat, we have to add names
					// We are adding receive time too because we need it for message ordering in set
		    		Person p = mRsCtrlService.mRsPeersService.getPersonBySslId(m.getId().getChatId());

					//                                                                                                                               TODO ask drBob to put long instead of int
			    	if( p != null ) m = ChatMessage.newBuilder().setId( m.getId() ).setMsg( m.getMsg() ).setPeerNickname( p.getName() ).setRecvTime(  (int)(System.currentTimeMillis()/1000L)  ).build();
			    	lastPrivateChatMessage = m;
		    	}
		    	
				_addChatMessageToHistory(m); // This will notify Listeners too
				
				System.err.println(resp.getMsg());
				
			}
			catch (InvalidProtocolBufferException e) { e.printStackTrace(); }
		}
	}
	
	// called once in onConnected()
	public void registerForEventsAtServer()
	{
		RequestRegisterEvents.Builder reqb = RequestRegisterEvents.newBuilder();
		reqb.setAction(RequestRegisterEvents.RegisterAction.REGISTER);
		
    	RsMessage msg = new RsMessage();
    	msg.msgId = (Core.ExtensionId.CORE_VALUE<<24)|(Core.PackageId.CHAT_VALUE<<8)|Chat.RequestMsgIds.MsgId_RequestRegisterEvents_VALUE;
    	msg.body = reqb.build().toByteArray();
    	mRsCtrlService.sendMsg(msg);
	}

	public void joinChatLobby(ChatLobbyInfo li)
	{
		
		RequestJoinOrLeaveLobby.Builder reqb = RequestJoinOrLeaveLobby.newBuilder();
		reqb.setLobbyId(li.getLobbyId());
		reqb.setAction(RequestJoinOrLeaveLobby.LobbyAction.JOIN_OR_ACCEPT);
		
    	RsMessage msg = new RsMessage();
    	msg.msgId = (Core.ExtensionId.CORE_VALUE<<24)|(Core.PackageId.CHAT_VALUE<<8)|Chat.RequestMsgIds.MsgId_RequestJoinOrLeaveLobby_VALUE;
    	msg.body = reqb.build().toByteArray();
    	mRsCtrlService.sendMsg(msg);
	}
	
	public void leaveChatLobby(ChatLobbyInfo li)
	{
		RequestJoinOrLeaveLobby.Builder reqb = RequestJoinOrLeaveLobby.newBuilder();
		reqb.setLobbyId(li.getLobbyId());
		reqb.setAction(RequestJoinOrLeaveLobby.LobbyAction.LEAVE_OR_DENY);
		
    	RsMessage msg = new RsMessage();
    	msg.msgId = (Core.ExtensionId.CORE_VALUE<<24)|(Core.PackageId.CHAT_VALUE<<8)|Chat.RequestMsgIds.MsgId_RequestJoinOrLeaveLobby_VALUE;
    	msg.body = reqb.build().toByteArray();
    	mRsCtrlService.sendMsg(msg);
	}
	
	public void sendChatMessage(ChatMessage m)
	{
		System.err.println("RsChatService: Sending Message:\n"+m);

		//mRsCtrlService.mRsPeersService
		
    	RsMessage msg = new RsMessage();
    	msg.msgId = (Core.ExtensionId.CORE_VALUE<<24)|(Core.PackageId.CHAT_VALUE<<8)|Chat.RequestMsgIds.MsgId_RequestSendMessage_VALUE;
    	msg.body = RequestSendMessage.newBuilder().setMsg(m).build().toByteArray();
    	mRsCtrlService.sendMsg(msg);
    	
		// ad name information
    	if(m.getId().getChatType().equals(ChatType.TYPE_LOBBY)) // chat lobby
		{
    		System.err.println("RsChatService::_sendChatMessage: ChatLobbies:\n"+ mChatLobbies);
    		// we have lobby
    		for(ChatLobbyInfo i: mChatLobbies)
			{
    			if(i.getLobbyId().equals(m.getId().getChatId()))
				{
    				System.err.println("RsChatService::_sendChatMessage: Lobby found");
	    			if(i.getLobbyNickname().equals("")) m = ChatMessage.newBuilder().setId( m.getId() ).setMsg( m.getMsg() ).setPeerNickname( mRsCtrlService.mRsPeersService.getOwnPerson().getName() ).build(); // no nick set
	    			else m = ChatMessage.newBuilder().setId(m.getId()).setMsg(m.getMsg()).setPeerNickname(i.getLobbyNickname()).build(); //nick set
	    			break;
    			}
    		}
    	}
		else // private chat
		{
	    	if(mRsCtrlService.mRsPeersService.getOwnPerson() != null) m = ChatMessage.newBuilder().setId( m.getId() ).setMsg( m.getMsg() ).setSendTime( m.getSendTime() ).setPeerNickname( mRsCtrlService.mRsPeersService.getOwnPerson().getName() ).build();
	    	else {} // no name available
    	}
    	
    	_addChatMessageToHistory(m);
	}
	
	private void _addChatMessageToHistory(ChatMessage m)
	{
		if( mChatHistory.get(m.getId()) == null ) mChatHistory.put(m.getId(), new ArrayList<ChatMessage>());
		mChatHistory.get(m.getId()).add(m);
		if( ! notificationDisabledChats.contains(m.getId()) ) mChatChanged.put(m.getId(), true);
		_notifyListeners();
	}
	
	@Override
	public void onConnectionStateChanged(RsCtrlService.ConnectionEvent ce) { if(ce.kind == RsCtrlService.ConnectionEventKind.CONNECTED) registerForEventsAtServer(); }
}

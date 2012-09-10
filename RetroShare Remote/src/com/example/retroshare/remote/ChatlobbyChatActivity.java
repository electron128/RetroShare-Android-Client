package com.example.retroshare.remote;

import java.util.HashMap;
import java.util.Map;

import rsctrl.chat.Chat;
import rsctrl.chat.Chat.ChatId;
import rsctrl.chat.Chat.ChatMessage;
import rsctrl.chat.Chat.ChatType;
import rsctrl.chat.Chat.EventChatMessage;
import rsctrl.chat.Chat.RequestJoinOrLeaveLobby;
import rsctrl.chat.Chat.RequestRegisterEvents;
import rsctrl.chat.Chat.RequestSendMessage;
import rsctrl.chat.Chat.ResponseMsgIds;
import rsctrl.core.Core;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.EditText;

import com.example.retroshare.remote.RsService.RsMessage;
import com.google.protobuf.InvalidProtocolBufferException;

public class ChatlobbyChatActivity extends RsActivityBase {
	private static final String TAG="ChatlobbyChatActivity";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.activity_chatlobbychat);
	}
	
	private ChatHandler mChatHandler=null;
	
	// set to true once, to prevent multiple registration
	// rs-nogui will send events twice if we register twice
	private static boolean haveRegisteredEventsOnServer=false;
	
	protected void onServiceConnected(){
		// Join Lobby
		{
			Intent i=getIntent();
			String lobbyId=i.getStringExtra("lobbyId");
			String lobbyName=i.getStringExtra("lobbyName");
			
			//TextView tv=(TextView) findViewById(R.id.textView1);
			//tv.setText(tv.getText()+"Lobby Id: "+lobbyId+"\nLobby Name:"+lobbyName);
			
			RequestJoinOrLeaveLobby.Builder reqb= RequestJoinOrLeaveLobby.newBuilder();
			reqb.setLobbyId(lobbyId);
			reqb.setAction(RequestJoinOrLeaveLobby.LobbyAction.JOIN_OR_ACCEPT);
			
	    	RsMessage msg=mRsService.new RsMessage();
	    	msg.msgId=(Core.ExtensionId.CORE_VALUE<<24)|(Core.PackageId.CHAT_VALUE<<8)|Chat.RequestMsgIds.MsgId_RequestJoinOrLeaveLobby_VALUE;
	    	msg.body=reqb.build().toByteArray();
	    	mRsService.sendMsg(msg, null);
		}
		
		// Register for Events
		{
			int RESPONSE=(0x01<<24);
			final int MsgId_EventChatMessage=(RESPONSE|(Core.PackageId.CHAT_VALUE<<8)|ResponseMsgIds.MsgId_EventChatMessage_VALUE);
			
			Intent i=getIntent();
			String lobbyId=i.getStringExtra("lobbyId");
			
			//at RsService
			
			//is now done in RsService::onCreate()
			//->so it happens only once, and even if ChatlobbyChatActivity is not started
			//only get the handler from RsService here
			mChatHandler=(ChatHandler)mRsService.getHandler(MsgId_EventChatMessage);
			//register at handler
			mChatHandler.addListener(lobbyId,this);
			//get data from handler and update views
			updateViews();
			
			/*
			mRsService.registerMsgHandler(MsgId_EventChatMessage, new RsMessageHandler(){
				private static final int RESPONSE=(0x01<<24);
				@Override protected void rsHandleMsg(RsMessage msg){
		    		if(msg.msgId==MsgId_EventChatMessage){
		    			System.out.println("received Chat.ResponseMsgIds.MsgId_EventChatMessage_VALUE");
		    			try {
		    				EventChatMessage resp=EventChatMessage.parseFrom(msg.body);
		    				String s=resp.getMsg().getPeerNickname()+": "+resp.getMsg().getMsg()+"</br>";
		    				
		    				
		    				_addChatMsg(resp.getMsg());


		    				
		    			} catch (InvalidProtocolBufferException e) {
		    				// TODO Auto-generated catch block
		    				e.printStackTrace();
		    			}
		    		}
				}
			});
			*/
			
			//at Server
			if(haveRegisteredEventsOnServer==false){
				haveRegisteredEventsOnServer=true;
				
				RequestRegisterEvents.Builder reqb= RequestRegisterEvents.newBuilder();
				reqb.setAction(RequestRegisterEvents.RegisterAction.REGISTER);
				
		    	RsMessage msg=mRsService.new RsMessage();
		    	msg.msgId=(Core.ExtensionId.CORE_VALUE<<24)|(Core.PackageId.CHAT_VALUE<<8)|Chat.RequestMsgIds.MsgId_RequestRegisterEvents_VALUE;
		    	msg.body=reqb.build().toByteArray();
		    	mRsService.sendMsg(msg, null);
			}
			
		}
		
		//_sendChatMsg("<img src='data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAHElEQVQI12P4//8/w38GIAXDIBKE0DHxgljNBAAO9TXL0Y4OHwAAAABJRU5ErkJggg==' alt='Red dot'>");
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		Intent i=getIntent();
		String lobbyId=i.getStringExtra("lobbyId");
		//remove from handler, so activity can get garbage collected
		mChatHandler.removeListener(lobbyId);
	}
	
	
	public static class ChatHandler extends RsMessageHandler{
		private static final String TAG="ChatHandler";
		
		private static final int RESPONSE=(0x01<<24);
		
		final int MsgId_EventChatMessage=(RESPONSE|(Core.PackageId.CHAT_VALUE<<8)|ResponseMsgIds.MsgId_EventChatMessage_VALUE);
		
		//this is called by RsService an runs in the ui thread
		@Override protected void rsHandleMsg(RsMessage msg){
    		if(msg.msgId==MsgId_EventChatMessage){
    			System.out.println("received Chat.ResponseMsgIds.MsgId_EventChatMessage_VALUE");
    			try {
    				EventChatMessage resp=EventChatMessage.parseFrom(msg.body);
    				addChatMsg(resp.getMsg());
    			} catch (InvalidProtocolBufferException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}
    		}
		}
		
		private Map<String,ChatlobbyChatActivity> mListeners=new HashMap<String,ChatlobbyChatActivity>();
		public void addListener(String id, ChatlobbyChatActivity l){
			mListeners.put(id, l);
		}
		public void removeListener(String id){
			mListeners.remove(id);
		}
		
		//ad meta to define encoding, needed to display צהü
		//public String ChatHistory="<meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\">";
		// chatId,ChatHistory
		public Map<String,String> ChatHistories=new HashMap<String,String>();
		
		public void addChatMsg(ChatMessage msg){
			String ChatId=msg.getId().getChatId();
			
			String ChatHistory=ChatHistories.get(ChatId);
			if(ChatHistory==null){ChatHistory="<meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\">";}
			ChatHistory+="<span style=\"color:dodgerblue;\">"+msg.getPeerNickname()+":</span> "+msg.getMsg()+"</br>";
			ChatHistories.put(ChatId, ChatHistory);
				
			ChatlobbyChatActivity l=mListeners.get(ChatId);
			if(l!=null){
				l.updateViews();
			}else{
				Log.v(TAG,"no handler for Chat "+ChatId);
			}
		}
	}
	
	public void updateViews(){
		Intent i=getIntent();
		String lobbyId=i.getStringExtra("lobbyId");
		String history=mChatHandler.ChatHistories.get(lobbyId);
		if(history==null){history=TAG+".updateViews(): mChatHandler.ChatHistories.get("+lobbyId+")returned null";}
		String base64 = android.util.Base64.encodeToString(history.getBytes(), android.util.Base64.DEFAULT);
		((WebView) findViewById(R.id.webView1)).loadData(base64, "text/html", "base64"); 
	}
	
	public void sendChatMsg(View v){
		EditText et=(EditText) findViewById(R.id.editText1);
		_sendChatMsg(et.getText().toString());
	}
	
	private void _sendChatMsg(String s){
		Intent i=getIntent();
		String lobbyId=i.getStringExtra("lobbyId");
		
		Log.v(TAG,"_sendChatMsg("+s+")");
		
		//TextView tv=(TextView) findViewById(R.id.textView1);
		//tv.setText(tv.getText()+"\nsself: "+s);
		ChatMessage cm=ChatMessage.newBuilder().setMsg(s).setPeerNickname("self").setId(ChatId.newBuilder()
							.setChatId(lobbyId)
							.setChatType(ChatType.TYPE_LOBBY)).build();
		mChatHandler.addChatMsg(cm);
		
		RequestSendMessage.Builder reqb= RequestSendMessage.newBuilder();
		reqb.setMsg(
				ChatMessage.newBuilder()
				.setId(ChatId.newBuilder()
							.setChatId(lobbyId)
							.setChatType(ChatType.TYPE_LOBBY))
				.setMsg(s)
		);
		
    	RsMessage msg=mRsService.new RsMessage();
    	msg.msgId=(Core.ExtensionId.CORE_VALUE<<24)|(Core.PackageId.CHAT_VALUE<<8)|Chat.RequestMsgIds.MsgId_RequestSendMessage_VALUE;
    	msg.body=reqb.build().toByteArray();
    	mRsService.sendMsg(msg, null);
	}
	

	
	private void _sayHi(){
/*************************************
		  \___/
		  /o o\       |_| '
		 '-----'      | | |
		||     ||
		||     ||
		||     ||
		 '-----'
		   | |
**************************************/   
		
		_sendChatMsg("<pre>  \\___/\n  /o o\\       |_| '\n '-----'      | | |\n||     ||\n||     ||\n||     ||\n '-----'\n   | |</pre>");
	}
}

package org.retroshare.android;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rsctrl.chat.Chat;
import rsctrl.chat.Chat.ChatId;
import rsctrl.chat.Chat.ChatLobbyInfo;
import rsctrl.chat.Chat.ChatMessage;
import rsctrl.chat.Chat.ChatType;
import rsctrl.chat.Chat.EventChatMessage;
import rsctrl.chat.Chat.RequestJoinOrLeaveLobby;
import rsctrl.chat.Chat.RequestRegisterEvents;
import rsctrl.chat.Chat.RequestSendMessage;
import rsctrl.chat.Chat.ResponseMsgIds;
import rsctrl.core.Core;
import rsctrl.core.Core.Person;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import org.retroshare.java.ChatService.ChatServiceListener;
import org.retroshare.java.RsCtrlService.RsMessage;
//import org.retroshare.android.RsService.RsMessage;
import com.google.protobuf.InvalidProtocolBufferException;

public class ChatActivity extends RsActivityBase implements ChatServiceListener{
	private static final String TAG="ChatlobbyChatActivity";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.activity_chatlobbychat);
	    
	    ((Button) findViewById(R.id.button1)).setVisibility(View.GONE);
	    
	    ((EditText) findViewById(R.id.editText1)).setOnKeyListener(new KeyListener());
	}
	
	private class KeyListener implements OnKeyListener{
		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event) {
			
			if((event.getAction()==KeyEvent.ACTION_DOWN)&(event.getKeyCode() == KeyEvent.KEYCODE_ENTER)){
				Log.v(TAG,"KeyListener.onKey() event.getKeyCode() == KeyEvent.KEYCODE_ENTER");
				sendChatMsg(null);
				return true;
			}else{
				return false;
			}
		}
		
	}
	
	//private ChatHandler mChatHandler=null;
	
	// set to true once, to prevent multiple registration
	// rs-nogui will send events twice if we register twice
	//private static boolean haveRegisteredEventsOnServer=false;
	
	private ChatId mChatId;
	private ChatLobbyInfo mChatLobbyInfo;
	
	protected void onServiceConnected(){
		
		// done in RsService now
		//mRsService.mRsCtrlService.chatService.registerForEventsAtServer();
		
		try {
			mChatId=ChatId.parseFrom(getIntent().getByteArrayExtra("ChatId"));
			if(getIntent().hasExtra("ChatLobbyInfo")){
				mChatLobbyInfo=ChatLobbyInfo.parseFrom(getIntent().getByteArrayExtra("ChatLobbyInfo"));
			}
		} catch (InvalidProtocolBufferException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(mChatLobbyInfo!=null){
			//chatlobby
			mRsService.mRsCtrlService.chatService.joinChatLobby(mChatLobbyInfo);
			TextView tv=(TextView) findViewById(R.id.textView1);
			tv.setText(mChatLobbyInfo.getLobbyName());
			
		    Button b=(Button) findViewById(R.id.buttonLeaveLobby);
		    b.setVisibility(View.VISIBLE);
		} else{
			//private chat
			TextView tv=(TextView) findViewById(R.id.textView1);
			Person p=mRsService.mRsCtrlService.peersService.getPersonFromSslId(mChatId.getChatId());
			String name="Error: no Person found";
			if(p!=null){
				name=p.getName();
			}
			tv.setText(name);
			
		    Button b=(Button) findViewById(R.id.buttonLeaveLobby);
		    b.setVisibility(View.GONE);
		}
		
		updateViews();
		
		mRsService.mRsCtrlService.chatService.setNotifyBlockedChat(mChatId);
		
		mRsService.mRsCtrlService.chatService.registerListener(this);
		
		Log.v(TAG,"onServiceConnected(): mChatId="+mChatId);
		
		/*
		// Join Lobby
		{
			Intent i=getIntent();
			String lobbyId=i.getStringExtra("lobbyId");
			String lobbyName=i.getStringExtra("lobbyName");
			
			TextView tv=(TextView) findViewById(R.id.textView1);
			tv.setText(lobbyName);
			
			RequestJoinOrLeaveLobby.Builder reqb= RequestJoinOrLeaveLobby.newBuilder();
			reqb.setLobbyId(lobbyId);
			reqb.setAction(RequestJoinOrLeaveLobby.LobbyAction.JOIN_OR_ACCEPT);
			
	    	RsMessage msg=new RsMessage();
	    	msg.msgId=(Core.ExtensionId.CORE_VALUE<<24)|(Core.PackageId.CHAT_VALUE<<8)|Chat.RequestMsgIds.MsgId_RequestJoinOrLeaveLobby_VALUE;
	    	msg.body=reqb.build().toByteArray();
	    	mRsService.mRsCtrlService.sendMsg(msg, null);
		}
		*/
		/*
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
			mChatHandler=(ChatHandler)mRsService.mRsCtrlService.getHandler(MsgId_EventChatMessage);
			//register at handler
			mChatHandler.addListener(lobbyId,this);
			//get data from handler and update views
			updateViews();
			*/
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
			/*
			//at Server
			if(haveRegisteredEventsOnServer==false){
				haveRegisteredEventsOnServer=true;
				
				RequestRegisterEvents.Builder reqb= RequestRegisterEvents.newBuilder();
				reqb.setAction(RequestRegisterEvents.RegisterAction.REGISTER);
				
		    	RsMessage msg=new RsMessage();
		    	msg.msgId=(Core.ExtensionId.CORE_VALUE<<24)|(Core.PackageId.CHAT_VALUE<<8)|Chat.RequestMsgIds.MsgId_RequestRegisterEvents_VALUE;
		    	msg.body=reqb.build().toByteArray();
		    	mRsService.mRsCtrlService.sendMsg(msg, null);
			}
			*/
		
	//	}
		
		//_sendChatMsg("<img src='data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAHElEQVQI12P4//8/w38GIAXDIBKE0DHxgljNBAAO9TXL0Y4OHwAAAABJRU5ErkJggg==' alt='Red dot'>");
	}
	
	@Override
	public void onPause(){
		super.onPause();
		if(mBound){
			mRsService.mRsCtrlService.chatService.setNotifyBlockedChat(null);
		}
	}
	
	@Override
	public void onResume(){
		super.onResume();
		if(mBound){
			mRsService.mRsCtrlService.chatService.setNotifyBlockedChat(mChatId);
		}
	}
	
		
		
	@Override
	public void onDestroy(){
		super.onDestroy();
		/*
		Intent i=getIntent();
		String lobbyId=i.getStringExtra("lobbyId");
		//remove from handler, so activity can get garbage collected
		mChatHandler.removeListener(lobbyId);
		*/
	}
	
	/*
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
		
		//ad meta to define encoding, needed to display ���
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
	*/
	
	public void updateViews(){
		//Intent i=getIntent();
		//String lobbyId=i.getStringExtra("lobbyId");
		//String history=mChatHandler.ChatHistories.get(lobbyId);
		List<ChatMessage> ChatHistory=mRsService.mRsCtrlService.chatService.getChatHistoryForChatId(mChatId);
		
		String historyString="";
		//ad meta to define encoding, needed to display ���
		historyString+="<meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\">";
		
		for(ChatMessage msg:ChatHistory){
			historyString+="<span style=\"color:dodgerblue;\">"+msg.getPeerNickname()+":</span> "+msg.getMsg()+"</br>";
		}
		
		//if(history==null){history=TAG+".updateViews(): mChatHandler.ChatHistories.get("+lobbyId+")returned null";}
		String base64 = android.util.Base64.encodeToString(historyString.getBytes(), android.util.Base64.DEFAULT);
		
		//ScrollView sv=(ScrollView) findViewById(R.id.scrollView1);
		//sv.
		((WebView) findViewById(R.id.webView1)).loadData(base64, "text/html", "base64"); 
	}
	
	public void sendChatMsg(View v){
		EditText et=(EditText) findViewById(R.id.editText1);
		//_sendChatMsg(et.getText().toString());
		
		ChatMessage msg=ChatMessage.newBuilder().setId(mChatId).setMsg((et.getText().toString())).build();
		et.setText("");
		
		//mRsService.mRsCtrlService.chatService.sendChatMessage(msg);
		
		if(msg.getMsg().equals("a")){
			sendAndroid(null);
		}else{
			mRsService.mRsCtrlService.chatService.sendChatMessage(msg);
		}
	}
	public void sendAndroid(View v){
		String android="<span><span style=\"font-family:\'\';font-size:8.25pt;font-weight:400;font-style:normal;\"><img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAIAAAD8GO2jAAAAA3NCSVQICAjb4U/gAAAACXBIWXMAAA7EAAAOxAGVKw4bAAACzklEQVRIibVWS08TURQ+dzqFKZROpg+balsKpE1jiUSQh0gwggsTYhpZqgtjMHFjTFTcuNOd8hPc4MLEBcaFEIORhYaoSSVIRYkgEKChQCktbaXv62JKAXs7w4z128y55/F9d86dMzOABTG59BTjbLFoMh2d9j0XZgDhcOj34tz6SH6ZziQy2WR+ObUymEzHhBkQxhgE4fU9Uyl1vzZe+8MTO6kgAFSUHTGxp6zaTlrB1Bl6hMtFBIKx2bGZe4Hod2LUxDZ3OR9XMWaZAqthz4i3L5WJCdQzSu3FhkFdpbNYAlUsEIn73ny7mcrEWmvu1ujPFyY0227XGi7EU8ER7414akuywPjcw0Q6DAD6qnoNYy1M0KtdrKoaAGIJ/+f5J8V4yC3ais2+8PQAiJz/3jYRfbXtfUWZgRAiFswHRg/PDgBZnF7cfEfWJnoD0enDs+dKIuSSvRYtBEZ9oU82XbeZO+MPe+KpkCQBlVJrZBsXAm99oY82XZeZ6zggsBz8MOy9DgAIUb2NL8fnHvnDXyQJmLn2Bkvf8NQuyckhQ1U95Fu0FpnkDYyz69uTkqjzWNveRxL5ytuEMxB9eRwGeZKic1Aq/C8BDBkBgRK0CARahHFGHicCRaGzlC1CiODMCVCIIF4SkO6AuBOp2OXICejVrnxIrz4uj9Ogri8kofmLVXu2yzmwsjVerTtnYpvlCVi0nd3OgeUcScsBAQBwGN0Oo5u3EcjoEgIAu9Ft3yXhQX6KBL6xxaBTk0toore1tl9BlYd25qMJ/2b0B+/UMGau0g4Aq2FPMh3hnce40zTFaCvsTbZbRCqR35afa6/GZvp5+4T5WnvdAwAYmujdiHh55+XWMQ1jEWAQGTTRw8A4K5wgZ5IlPQCyBKSMvYhAuZLbs2n2LwMhqpzW/JOAhetwGC9RiDaxTa6jV3hnS80djcpKK1RttfeZfTsg4g/+D5MwF/zpFwAAAABJRU5ErkJggg==\"/></span></span>";
		String desktop="<span><span style=\"font-family:\'\';font-size:8.25pt;font-weight:400;font-style:normal;\"><img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAOxAAADsQBlSsOGwAAAjdJREFUOI2lkztrVFEUhb9zzr2ZSTRhYojGBCEgNtrYaGEjWAgpBQtbK0Gwihb+Aa1s9E+klTSRoIUBUSsxJiIhYDAPY2Zy53Xn3nte22L+QbKbVa3Fx957KRHhNKNP5QbU3OKrBwv37r82GlJjSLzFFxWDPBBcgAiEQPSOaD3Rg4TIYbMjn1ffPk68yIXL4+n006mM54eTbHQMuqewuSM6hxYQCWAjqQLnhc2djIPf+0jem0kKJ367lbO8/4cfpPzqJoxVCqyGApS3GGdRUTjqVOztZdAtAAch+iQKrGWerdBg49w4tfkzGCeErI9tZoRWQdUvyLISshJsBVJC2QXnSHI9KnNna7y/M8X8R9j5WzCaD5BmRuy2sa0mtAdgBZyHvA+hHC6wNkoiu002v27zaWqWnbWf4FMKCTDoQ9kDW4KuwdgIlA5KC2Ubqg6UfUmoPMe7R7z8PgHNFrQLqI2AWPB2eKtRA5KACqAU+AjeISjR9J3cnq2z/PAm1xoptI7BVxAiRA1iwAlU1ZAmuGG4BNA1EtQI3XaXlW97bKwfQGqG5uDBBVApVAV4B0UOeTZU70AZEiZn9JetgoVHS3C+AWkdBgAJ1OtQH4FEDwkMEC34PmDBjOkE4M2zuyd64yeLL0huXKz3lj7sumiMkdRo0QqMRmtAa5AI1qGch+hQMUKw0TgXbl2/2lMr71brjelLVwSZACZQjIFKETGIKIZtFRQBwSmkALpK6U7e+beVACWwftI2/gfYDUdg4soRigAAAABJRU5ErkJggg==\"/></span></span>";
		ChatMessage msg=ChatMessage.newBuilder().setId(mChatId).setMsg(android).build();
		mRsService.mRsCtrlService.chatService.sendChatMessage(msg);
	}
	
	public void leaveLobby(View v){
		if(mChatLobbyInfo!=null){
			mRsService.mRsCtrlService.chatService.leaveChatLobby(mChatLobbyInfo);
			// quit this activity
			finish();
		}
	}
	/*
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
		
    	RsMessage msg=new RsMessage();
    	msg.msgId=(Core.ExtensionId.CORE_VALUE<<24)|(Core.PackageId.CHAT_VALUE<<8)|Chat.RequestMsgIds.MsgId_RequestSendMessage_VALUE;
    	msg.body=reqb.build().toByteArray();
    	mRsService.mRsCtrlService.sendMsg(msg, null);
	}
	*/

	
	//private void _sayHi(){
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
		
//		_sendChatMsg("<pre>  \\___/\n  /o o\\       |_| '\n '-----'      | | |\n||     ||\n||     ||\n||     ||\n '-----'\n   | |</pre>");
//	}
	
	// will be called by ChatService
	@Override
	public void update() {
		updateViews();
		
	}
}

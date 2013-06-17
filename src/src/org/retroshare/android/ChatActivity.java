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
import org.retroshare.java.RsCtrlService;
import org.retroshare.java.RsCtrlService.RsMessage;
//import org.retroshare.android.RsService.RsMessage;
import com.google.protobuf.InvalidProtocolBufferException;

public class ChatActivity extends ProxiedActivityBase implements ChatServiceListener
{
	private static final String TAG="ChatActivity";
	
	@Override
	public void onCreateBeforeConnectionInit(Bundle savedInstanceState)
	{
	    setContentView(R.layout.activity_chatlobbychat);
	    
	    findViewById(R.id.button1).setVisibility(View.GONE);
	    findViewById(R.id.editText1).setOnKeyListener(new KeyListener());
	}
	
	private class KeyListener implements OnKeyListener
	{
		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event)
		{
			if(( event.getAction() == KeyEvent.ACTION_DOWN ) & ( event.getKeyCode() == KeyEvent.KEYCODE_ENTER ))
			{
				Log.v(TAG,"KeyListener.onKey() event.getKeyCode() == KeyEvent.KEYCODE_ENTER");
				sendChatMsg(null);
				return true;
			}
			else return false;
		}
	}
	
	//private ChatHandler mChatHandler=null;
	
	// set to true once, to prevent multiple registration
	// rs-nogui will send events twice if we register twice
	//private static boolean haveRegisteredEventsOnServer=false;
	
	private ChatId mChatId;
	private ChatLobbyInfo mChatLobbyInfo;
	
	protected void onServiceConnected()
	{
		RsCtrlService server = getConnectedServer();

		try
		{
			mChatId = ChatId.parseFrom(getIntent().getByteArrayExtra("ChatId"));
			if(getIntent().hasExtra("ChatLobbyInfo")) mChatLobbyInfo = ChatLobbyInfo.parseFrom(getIntent().getByteArrayExtra("ChatLobbyInfo"));
		}
		catch (InvalidProtocolBufferException e) { e.printStackTrace();} // TODO Auto-generated catch block

		if( mChatLobbyInfo != null ) //chatlobby
		{
			server.chatService.joinChatLobby(mChatLobbyInfo); // TODO here it get nullpointer
			TextView tv = (TextView) findViewById(R.id.textView1);
			tv.setText(mChatLobbyInfo.getLobbyName());
			
		    Button b=(Button) findViewById(R.id.buttonLeaveLobby);
		    b.setVisibility(View.VISIBLE);
		}
		else //private chat
		{
			TextView tv = (TextView) findViewById(R.id.textView1);
			Person p = server.peersService.getPersonFromSslId(mChatId.getChatId());
			String name="Error: no Person found"; // TODO HARDCODED string
			if(p != null) name = p.getName();
			tv.setText(name);
			
		    Button b=(Button) findViewById(R.id.buttonLeaveLobby);
		    b.setVisibility(View.GONE);
		}
		
		updateViews();

		server.chatService.setNotifyBlockedChat(mChatId);
		server.chatService.registerListener(this);
		
		Log.v(TAG,"onServiceConnected(): mChatId="+mChatId);
	}

	@Override
	public void onPause()
	{
		super.onPause();
		if(mBound) getConnectedServer().chatService.setNotifyBlockedChat(null);
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		if(mBound) getConnectedServer().chatService.setNotifyBlockedChat(mChatId);
	}

	public void updateViews()
	{
		List<ChatMessage> ChatHistory = getConnectedServer().chatService.getChatHistoryForChatId(mChatId);
		
		String historyString = "";
		//ad meta to define encoding, needed to display
		historyString+="<meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\">"; //TODO HARDCODED string
		
		for(ChatMessage msg:ChatHistory) historyString+="<span style=\"color:dodgerblue;\">"+msg.getPeerNickname()+":</span> "+msg.getMsg()+"</br>";  //TODO HARDCODED string

		String base64 = android.util.Base64.encodeToString(historyString.getBytes(), android.util.Base64.DEFAULT);
		((WebView) findViewById(R.id.webView1)).loadData(base64, "text/html", "base64");
	}
	
	public void sendChatMsg(View v)
	{
		EditText et = (EditText) findViewById(R.id.editText1);
		ChatMessage msg = ChatMessage.newBuilder().setId(mChatId).setMsg((et.getText().toString())).build();
		et.setText("");

		if(msg.getMsg().equals("a")) sendAndroid(null);
		else getConnectedServer().chatService.sendChatMessage(msg);
	}

	public void sendAndroid(View v)
	{
		String android = "<span><span style=\"font-family:\'\';font-size:8.25pt;font-weight:400;font-style:normal;\"><img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAIAAAD8GO2jAAAAA3NCSVQICAjb4U/gAAAACXBIWXMAAA7EAAAOxAGVKw4bAAACzklEQVRIibVWS08TURQ+dzqFKZROpg+balsKpE1jiUSQh0gwggsTYhpZqgtjMHFjTFTcuNOd8hPc4MLEBcaFEIORhYaoSSVIRYkgEKChQCktbaXv62JKAXs7w4z128y55/F9d86dMzOABTG59BTjbLFoMh2d9j0XZgDhcOj34tz6SH6ZziQy2WR+ObUymEzHhBkQxhgE4fU9Uyl1vzZe+8MTO6kgAFSUHTGxp6zaTlrB1Bl6hMtFBIKx2bGZe4Hod2LUxDZ3OR9XMWaZAqthz4i3L5WJCdQzSu3FhkFdpbNYAlUsEIn73ny7mcrEWmvu1ujPFyY0227XGi7EU8ER7414akuywPjcw0Q6DAD6qnoNYy1M0KtdrKoaAGIJ/+f5J8V4yC3ais2+8PQAiJz/3jYRfbXtfUWZgRAiFswHRg/PDgBZnF7cfEfWJnoD0enDs+dKIuSSvRYtBEZ9oU82XbeZO+MPe+KpkCQBlVJrZBsXAm99oY82XZeZ6zggsBz8MOy9DgAIUb2NL8fnHvnDXyQJmLn2Bkvf8NQuyckhQ1U95Fu0FpnkDYyz69uTkqjzWNveRxL5ytuEMxB9eRwGeZKic1Aq/C8BDBkBgRK0CARahHFGHicCRaGzlC1CiODMCVCIIF4SkO6AuBOp2OXICejVrnxIrz4uj9Ogri8kofmLVXu2yzmwsjVerTtnYpvlCVi0nd3OgeUcScsBAQBwGN0Oo5u3EcjoEgIAu9Ft3yXhQX6KBL6xxaBTk0toore1tl9BlYd25qMJ/2b0B+/UMGau0g4Aq2FPMh3hnce40zTFaCvsTbZbRCqR35afa6/GZvp5+4T5WnvdAwAYmujdiHh55+XWMQ1jEWAQGTTRw8A4K5wgZ5IlPQCyBKSMvYhAuZLbs2n2LwMhqpzW/JOAhetwGC9RiDaxTa6jV3hnS80djcpKK1RttfeZfTsg4g/+D5MwF/zpFwAAAABJRU5ErkJggg==\"/></span></span>"; //TODO HARDCODED string
		String desktop = "<span><span style=\"font-family:\'\';font-size:8.25pt;font-weight:400;font-style:normal;\"><img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAOxAAADsQBlSsOGwAAAjdJREFUOI2lkztrVFEUhb9zzr2ZSTRhYojGBCEgNtrYaGEjWAgpBQtbK0Gwihb+Aa1s9E+klTSRoIUBUSsxJiIhYDAPY2Zy53Xn3nte22L+QbKbVa3Fx957KRHhNKNP5QbU3OKrBwv37r82GlJjSLzFFxWDPBBcgAiEQPSOaD3Rg4TIYbMjn1ffPk68yIXL4+n006mM54eTbHQMuqewuSM6hxYQCWAjqQLnhc2djIPf+0jem0kKJ367lbO8/4cfpPzqJoxVCqyGApS3GGdRUTjqVOztZdAtAAch+iQKrGWerdBg49w4tfkzGCeErI9tZoRWQdUvyLISshJsBVJC2QXnSHI9KnNna7y/M8X8R9j5WzCaD5BmRuy2sa0mtAdgBZyHvA+hHC6wNkoiu002v27zaWqWnbWf4FMKCTDoQ9kDW4KuwdgIlA5KC2Ubqg6UfUmoPMe7R7z8PgHNFrQLqI2AWPB2eKtRA5KACqAU+AjeISjR9J3cnq2z/PAm1xoptI7BVxAiRA1iwAlU1ZAmuGG4BNA1EtQI3XaXlW97bKwfQGqG5uDBBVApVAV4B0UOeTZU70AZEiZn9JetgoVHS3C+AWkdBgAJ1OtQH4FEDwkMEC34PmDBjOkE4M2zuyd64yeLL0huXKz3lj7sumiMkdRo0QqMRmtAa5AI1qGch+hQMUKw0TgXbl2/2lMr71brjelLVwSZACZQjIFKETGIKIZtFRQBwSmkALpK6U7e+beVACWwftI2/gfYDUdg4soRigAAAABJRU5ErkJggg==\"/></span></span>"; //TODO HARDCODED string
		ChatMessage msg = ChatMessage.newBuilder().setId(mChatId).setMsg(android).build();
		getConnectedServer().chatService.sendChatMessage(msg);
	}
	
	public void leaveLobby(View v)
	{
		if( mChatLobbyInfo != null )
		{
			getConnectedServer().chatService.leaveChatLobby(mChatLobbyInfo);
			finish();
		}
	}
	
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
	public void update()
	{
		updateViews();
	}
}

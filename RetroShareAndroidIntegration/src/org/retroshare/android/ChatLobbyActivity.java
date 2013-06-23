package org.retroshare.android;

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

import com.google.protobuf.InvalidProtocolBufferException;

import org.retroshare.android.RsChatService.ChatServiceListener;


import java.util.List;

import rsctrl.chat.Chat.ChatId;
import rsctrl.chat.Chat.ChatLobbyInfo;
import rsctrl.chat.Chat.ChatMessage;

public class ChatLobbyActivity extends ProxiedActivityBase implements ChatServiceListener
{
	@Override public String TAG() { return "ChatLobbyActivity"; }

	public final static String CHAT_LOBBY_ID_EXTRA = "org.retroshare.android.intent_extra_keys.ChatId";
	public final static String CHAT_LOBBY_INFO_EXTRA = "org.retroshare.android.intent_extra_keys.ChatLobbyInfo";
	
	@Override
	public void onCreateBeforeConnectionInit(Bundle savedInstanceState)
	{
	    setContentView(R.layout.activity_chatlobbychat);

	    findViewById(R.id.chatMessageEditText).setOnKeyListener(new KeyListener());
	}
	
	private class KeyListener implements OnKeyListener
	{
		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event)
		{
			if(( event.getAction() == KeyEvent.ACTION_DOWN ) & ( event.getKeyCode() == KeyEvent.KEYCODE_ENTER ))
			{
				sendChatMsg(null);
				return true;
			}

			return false;
		}
	}

	private ChatLobbyInfo chatLobbyInfo;
	private ChatId chatLobbyId;
	
	protected void onServiceConnected()
	{
		Log.d(TAG(), "onServiceConnected()");

		RsCtrlService server = getConnectedServer();

		try
		{
			chatLobbyInfo = ChatLobbyInfo.parseFrom(getIntent().getByteArrayExtra(CHAT_LOBBY_INFO_EXTRA));
			chatLobbyId = ChatId.parseFrom(getIntent().getByteArrayExtra(CHAT_LOBBY_ID_EXTRA));
		}
		catch (InvalidProtocolBufferException e) { e.printStackTrace();} // TODO Auto-generated catch block

		server.mRsChatService.joinChatLobby(chatLobbyInfo);
		TextView tv = (TextView) findViewById(R.id.chatHeaderTextView);
		tv.setText(chatLobbyInfo.getLobbyName());

		Button b=(Button) findViewById(R.id.buttonLeaveLobby);
		b.setVisibility(View.VISIBLE);

		server.mRsChatService.disableNotificationForChat(chatLobbyId);

		updateViews();
		server.mRsChatService.registerListener(this);
	}

	@Override
	public void onPause()
	{
		if(isBound()) getConnectedServer().mRsChatService.enableNotificationForChat(chatLobbyId);
		super.onPause();
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		if(isBound()) getConnectedServer().mRsChatService.disableNotificationForChat(chatLobbyId);
	}

	private boolean recentlySentMessage = false;
	public void updateViews()
	{
		Log.d(TAG(), "updateViews()");

		if(isBound())
		{
			List<ChatMessage> chatLobbyHistory = getConnectedServer().mRsChatService.getChatHistoryForChatId(chatLobbyId);

			String historyString = "";
			//ad meta to define encoding, needed to display
			historyString+="<meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\" />";

			for(ChatMessage msg : chatLobbyHistory) historyString+="<span style=\"color:dodgerblue;\">" + msg.getPeerNickname() + ":</span> " + msg.getMsg() + "<br/>";
			historyString += "<br/>";

			String base64 = android.util.Base64.encodeToString(historyString.getBytes(), android.util.Base64.DEFAULT);

			WebView messageView = (WebView) findViewById(R.id.chatWebView);
			messageView.loadData(base64, "text/html", "base64");

			if(recentlySentMessage)
			{
				ScrollView scrollView = (ScrollView) findViewById(R.id.chatScrollView);
				scrollView.scrollTo( 0, messageView.getBottom());
				recentlySentMessage = false;
			}
		}
		else Log.e(TAG(), "updateViews() Why am I not bound?");
	}
	
	public void sendChatMsg(View v)
	{
		if(isBound())
		{
			EditText et = (EditText) findViewById(R.id.chatMessageEditText);
			ChatMessage msg = ChatMessage.newBuilder().setId(chatLobbyId).setMsg((et.getText().toString())).build();
			et.setText("");

			if(msg.getMsg().equals("a")) sendAndroid(null); // Easter egg
			else getConnectedServer().mRsChatService.sendChatMessage(msg);

			recentlySentMessage = true;
		}
	}

	public void sendAndroid(View v)
	{
		String android = "<span><span style=\"font-family:\'\';font-size:8.25pt;font-weight:400;font-style:normal;\"><img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAIAAAD8GO2jAAAAA3NCSVQICAjb4U/gAAAACXBIWXMAAA7EAAAOxAGVKw4bAAACzklEQVRIibVWS08TURQ+dzqFKZROpg+balsKpE1jiUSQh0gwggsTYhpZqgtjMHFjTFTcuNOd8hPc4MLEBcaFEIORhYaoSSVIRYkgEKChQCktbaXv62JKAXs7w4z128y55/F9d86dMzOABTG59BTjbLFoMh2d9j0XZgDhcOj34tz6SH6ZziQy2WR+ObUymEzHhBkQxhgE4fU9Uyl1vzZe+8MTO6kgAFSUHTGxp6zaTlrB1Bl6hMtFBIKx2bGZe4Hod2LUxDZ3OR9XMWaZAqthz4i3L5WJCdQzSu3FhkFdpbNYAlUsEIn73ny7mcrEWmvu1ujPFyY0227XGi7EU8ER7414akuywPjcw0Q6DAD6qnoNYy1M0KtdrKoaAGIJ/+f5J8V4yC3ais2+8PQAiJz/3jYRfbXtfUWZgRAiFswHRg/PDgBZnF7cfEfWJnoD0enDs+dKIuSSvRYtBEZ9oU82XbeZO+MPe+KpkCQBlVJrZBsXAm99oY82XZeZ6zggsBz8MOy9DgAIUb2NL8fnHvnDXyQJmLn2Bkvf8NQuyckhQ1U95Fu0FpnkDYyz69uTkqjzWNveRxL5ytuEMxB9eRwGeZKic1Aq/C8BDBkBgRK0CARahHFGHicCRaGzlC1CiODMCVCIIF4SkO6AuBOp2OXICejVrnxIrz4uj9Ogri8kofmLVXu2yzmwsjVerTtnYpvlCVi0nd3OgeUcScsBAQBwGN0Oo5u3EcjoEgIAu9Ft3yXhQX6KBL6xxaBTk0toore1tl9BlYd25qMJ/2b0B+/UMGau0g4Aq2FPMh3hnce40zTFaCvsTbZbRCqR35afa6/GZvp5+4T5WnvdAwAYmujdiHh55+XWMQ1jEWAQGTTRw8A4K5wgZ5IlPQCyBKSMvYhAuZLbs2n2LwMhqpzW/JOAhetwGC9RiDaxTa6jV3hnS80djcpKK1RttfeZfTsg4g/+D5MwF/zpFwAAAABJRU5ErkJggg==\"/></span></span>";
		ChatMessage msg = ChatMessage.newBuilder().setId(chatLobbyId).setMsg(android).build();
		getConnectedServer().mRsChatService.sendChatMessage(msg);
	}
	
	public void leaveLobby(View v)
	{
		if( chatLobbyInfo != null )
		{
			getConnectedServer().mRsChatService.leaveChatLobby(chatLobbyInfo);
			finish();
		}
	}

	@Override
	public void update() // will be called by RsChatService
	{
		updateViews();
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

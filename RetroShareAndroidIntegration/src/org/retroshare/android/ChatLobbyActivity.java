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


import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

import rsctrl.chat.Chat.ChatId;
import rsctrl.chat.Chat.ChatLobbyInfo;
import rsctrl.chat.Chat.ChatMessage;

public class ChatLobbyActivity extends ProxiedActivityBase implements ChatServiceListener
{
	@Override public String TAG() { return "ChatLobbyActivity"; }

	public final static String CHAT_LOBBY_ID_EXTRA = "org.retroshare.android.intent_extra_keys.ChatId";
	public final static String CHAT_LOBBY_INFO_EXTRA = "org.retroshare.android.intent_extra_keys.ChatLobbyInfo";

	private static final String htmlFileName = "chatWebView.html";
	private static final String JAVASCRIPT_NAME = "javaInterface";

	@Override
	public void onCreateBeforeConnectionInit(Bundle savedInstanceState)
	{
	    setContentView(R.layout.activity_chatlobbychat);

	    findViewById(R.id.chatMessageEditText).setOnKeyListener(new KeyListener());

		InputStream htmlIn = getResources().openRawResource(R.raw.chatwebview);
		boolean writeFile = false;

		try
		{
			InputStream htmlOutRead = openFileInput(htmlFileName);

			for(int i = 0; i < 1024; i++) // We are supposing that if file changed his firsts 1024 bytes changed too
				if(htmlIn.read() != htmlOutRead.read())
				{
					writeFile = true;
					break;
				}

			htmlOutRead.close();
		}
		catch (Exception e) { writeFile = true; }

		if( writeFile )
		{
			try
			{
				htmlIn.reset();
				FileOutputStream htmlOut = openFileOutput(htmlFileName, MODE_PRIVATE);

				int read = htmlIn.available(); // We expect the html file is little so it can stay all in memory
				byte[] buff = new byte[read];
				htmlIn.read(buff, 0, read);
				htmlOut.write(buff, 0, read);
				htmlOut.close();
			}
			catch (Exception e) { e.printStackTrace(); }
		}

		try { htmlIn.close(); } catch (Exception e) { e.printStackTrace(); }

		File htmlFile = new File(getFilesDir(), htmlFileName);
		WebView messagesWebView = (WebView) findViewById(R.id.chatWebView);
		messagesWebView.getSettings().setJavaScriptEnabled(true);
//		messagesWebView.loadData(util.readTextFromResource(this, R.raw.chatwebview), "text/html", "utf-8"); // loading data and not url make javascript not working :(
		messagesWebView.loadUrl("file://" + htmlFile.getAbsolutePath());
		messagesWebView.addJavascriptInterface(new JavaScriptInterface(), JAVASCRIPT_NAME);

		try
		{
			chatLobbyInfo = ChatLobbyInfo.parseFrom(getIntent().getByteArrayExtra(CHAT_LOBBY_INFO_EXTRA));
			chatLobbyId = ChatId.parseFrom(getIntent().getByteArrayExtra(CHAT_LOBBY_ID_EXTRA));
		}
		catch (InvalidProtocolBufferException e) { e.printStackTrace();} // TODO Auto-generated catch block
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
			WebView messagesView = (WebView) findViewById(R.id.chatWebView);
			messagesView.loadUrl("javascript:updateMessages();");

			if(recentlySentMessage)
			{
				ScrollView scrollView = (ScrollView) findViewById(R.id.chatScrollView);
				scrollView.scrollTo( 0, messagesView.getBottom());
				recentlySentMessage = false;
			}
		}
		else { Log.e(TAG(), "updateViews() Why am I not bound?"); }
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

	private class JavaScriptInterface
	{
		public String TAG() { return "JavaScriptInterface"; }

		private int lastDisplayedMessageIndex = 0;
		private ChatMessage actMessage;

		public boolean goNextMessage()
		{
			Log.d(TAG(), "goNextMessage()");

			List<ChatMessage> chatLobbyHistory = getConnectedServer().mRsChatService.getChatHistoryForChatId(chatLobbyId);
			if(lastDisplayedMessageIndex < chatLobbyHistory.size())
			{
				actMessage = chatLobbyHistory.get(lastDisplayedMessageIndex);
				++lastDisplayedMessageIndex;
				return true;
			}
			return false;
		}

		public String getLastMessageNick() { return actMessage.getPeerNickname(); }
		public String getLastMessageBody() { return actMessage.getMsg(); }
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

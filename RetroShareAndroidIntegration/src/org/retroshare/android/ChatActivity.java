package org.retroshare.android;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import rsctrl.chat.Chat.ChatId;
import rsctrl.chat.Chat.ChatType;
import rsctrl.chat.Chat.ChatMessage;
import rsctrl.core.Core;
import rsctrl.core.Core.Person;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import org.retroshare.android.RsChatService.ChatServiceListener;
import org.retroshare.android.utils.util;


public class ChatActivity extends ProxiedActivityBase implements ChatServiceListener
{
	@Override public String TAG() { return "ChatActivity"; }

	public final static String PGP_ID_EXTRA = "org.retroshare.android.intent_extra_keys.PgpId";
	private String pgpId;

	TreeSet<_ChatMsg> messages = new TreeSet<_ChatMsg>();
	_ChatMsg lastShowedMessage;

	private Set<ChatId> privateChatIds = new HashSet<ChatId>();
	private Map<ChatId, Integer> lastLoadedMessageInex = new HashMap<ChatId, Integer>();

	private static final String htmlFileName = "chatWebView.html";
	private static final String JAVASCRIPT_NAME = "javaInterface";

	@Override
	public void onCreateBeforeConnectionInit(Bundle savedInstanceState)
	{
	    setContentView(R.layout.activity_chatlobbychat);

		findViewById(R.id.buttonLeaveLobby).setVisibility(View.GONE);
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

		pgpId = getIntent().getStringExtra(PGP_ID_EXTRA);
	}
	
	private class KeyListener implements OnKeyListener
	{
		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event)
		{
			if(( event.getAction() == KeyEvent.ACTION_DOWN ) & ( event.getKeyCode() == KeyEvent.KEYCODE_ENTER ))
			{
//				Log.d(TAG(), "KeyListener.onKey() event.getKeyCode() == KeyEvent.KEYCODE_ENTER");
				sendChatMsg(null);
				return true;
			}

			return false;
		}
	}

	protected void onServiceConnected()
	{
		Log.d(TAG(), "onServiceConnected()");

		RsCtrlService server = getConnectedServer();
		Person person = server.mRsPeersService.getPersonByPgpId(pgpId);
		if(person == null)
		{
			util.uDebug(this, TAG(), "onServiceConnected() how can it be that person for " + pgpId + " is null??");
			return;
		}

		privateChatIds.clear();
		lastLoadedMessageInex.clear();

		for ( Core.Location location : person.getLocationsList() )
		{
			ChatId newChatId = ChatId.newBuilder().setChatType(ChatType.TYPE_PRIVATE).setChatId(location.getSslId()).build();
			privateChatIds.add(newChatId);
			lastLoadedMessageInex.put(newChatId, Integer.valueOf(-1));
		}

		TextView tv = (TextView) findViewById(R.id.chatHeaderTextView);
		tv.setText(person.getName());

		server.mRsChatService.disableNotificationForChats(privateChatIds);

		updateViews();
		server.mRsChatService.registerListener(this);
	}

	@Override
	public void onPause()
	{
		if(isBound()) getConnectedServer().mRsChatService.enableNotificationForChats(privateChatIds);
		super.onPause();
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		if(isBound()) getConnectedServer().mRsChatService.disableNotificationForChats(privateChatIds);
	}

	private boolean recentlySentMessage = false;
	public void updateViews()
	{
		Log.d(TAG(), "updateViews()");

		if(isBound())
		{
			RsChatService cs = getConnectedServer().mRsChatService;
			for (ChatId lChat : privateChatIds)
			{
				List<ChatMessage> fullMsgList = cs.getChatHistoryForChatId(lChat);
				Integer loadMsgIndex = lastLoadedMessageInex.get(lChat) + 1 ;
				int fullListSize = fullMsgList.size();
				for (; loadMsgIndex < fullListSize; loadMsgIndex++)
				{
					messages.add(new _ChatMsg(fullMsgList.get(loadMsgIndex)));
					lastLoadedMessageInex.put(lChat, loadMsgIndex);
				}
			}

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
		Log.d(TAG(), "sendChatMsg(View v)");

		if(isBound())
		{
			EditText et = (EditText) findViewById(R.id.chatMessageEditText);
			String msgText = et.getText().toString();
			RsChatService chatService = getConnectedServer().mRsChatService;

			//                                                                     TODO ask drBob to put long instead of int
			ChatMessage.Builder msgBuilder = ChatMessage.newBuilder().setSendTime( (int)(System.currentTimeMillis()/1000L) );

			if( msgText.equals("a") ) // Easter egg
			{
				String android = "<span><span style=\"font-family:\'\';font-size:8.25pt;font-weight:400;font-style:normal;\"><img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAIAAAD8GO2jAAAAA3NCSVQICAjb4U/gAAAACXBIWXMAAA7EAAAOxAGVKw4bAAACzklEQVRIibVWS08TURQ+dzqFKZROpg+balsKpE1jiUSQh0gwggsTYhpZqgtjMHFjTFTcuNOd8hPc4MLEBcaFEIORhYaoSSVIRYkgEKChQCktbaXv62JKAXs7w4z128y55/F9d86dMzOABTG59BTjbLFoMh2d9j0XZgDhcOj34tz6SH6ZziQy2WR+ObUymEzHhBkQxhgE4fU9Uyl1vzZe+8MTO6kgAFSUHTGxp6zaTlrB1Bl6hMtFBIKx2bGZe4Hod2LUxDZ3OR9XMWaZAqthz4i3L5WJCdQzSu3FhkFdpbNYAlUsEIn73ny7mcrEWmvu1ujPFyY0227XGi7EU8ER7414akuywPjcw0Q6DAD6qnoNYy1M0KtdrKoaAGIJ/+f5J8V4yC3ais2+8PQAiJz/3jYRfbXtfUWZgRAiFswHRg/PDgBZnF7cfEfWJnoD0enDs+dKIuSSvRYtBEZ9oU82XbeZO+MPe+KpkCQBlVJrZBsXAm99oY82XZeZ6zggsBz8MOy9DgAIUb2NL8fnHvnDXyQJmLn2Bkvf8NQuyckhQ1U95Fu0FpnkDYyz69uTkqjzWNveRxL5ytuEMxB9eRwGeZKic1Aq/C8BDBkBgRK0CARahHFGHicCRaGzlC1CiODMCVCIIF4SkO6AuBOp2OXICejVrnxIrz4uj9Ogri8kofmLVXu2yzmwsjVerTtnYpvlCVi0nd3OgeUcScsBAQBwGN0Oo5u3EcjoEgIAu9Ft3yXhQX6KBL6xxaBTk0toore1tl9BlYd25qMJ/2b0B+/UMGau0g4Aq2FPMh3hnce40zTFaCvsTbZbRCqR35afa6/GZvp5+4T5WnvdAwAYmujdiHh55+XWMQ1jEWAQGTTRw8A4K5wgZ5IlPQCyBKSMvYhAuZLbs2n2LwMhqpzW/JOAhetwGC9RiDaxTa6jV3hnS80djcpKK1RttfeZfTsg4g/+D5MwF/zpFwAAAABJRU5ErkJggg==\"/></span></span>";
				msgBuilder.setMsg(android);
			}
			else msgBuilder.setMsg(msgText);

			for ( ChatId sChatId : privateChatIds )
			{
				msgBuilder.setId(sChatId);
				chatService.sendChatMessage(msgBuilder.build());
			}

			et.setText("");

			recentlySentMessage = true;
		}
		else { Log.e(TAG(), "sendChatMsg(View v) cannot send message without connection to rsProxy"); }
	}

	@Override
	public void update() // will be called by RsChatService
	{
		updateViews();
	}

	private class _ChatMsg implements Comparable
	{
		private ChatMessage msg;

		public _ChatMsg(ChatMessage msg) { this.msg = msg; }

		public String getNick() { return msg.getPeerNickname(); }
		public String getBody() { return msg.getMsg(); }
		public int getTime()
		{
			if (msg.getSendTime() == 0) return msg.getRecvTime();
			return msg.getSendTime();
		}

		@Override
		public int compareTo(Object o)
		{
			_ChatMsg m = (_ChatMsg) o;

			int thisTime = this.getTime();
			int otherTime = m.getTime();
			if( thisTime != otherTime ) return thisTime - otherTime;
			return this.getBody().compareTo(m.getBody());
		}
	}

	private class JavaScriptInterface
	{
		public String TAG() { return "ChatActivity.JavaScriptInterface"; }

		private _ChatMsg actMsg;

		public boolean goNextMessage()
		{
			Log.d(TAG(), "goNextMessage()");

			if (messages.isEmpty()) return false;

			if(lastShowedMessage == null)
			{
				actMsg = messages.first();
				lastShowedMessage = actMsg;
				return true;
			}

			TreeSet<_ChatMsg> tmpSet = new TreeSet<_ChatMsg>(messages.tailSet(lastShowedMessage));
			tmpSet.remove(lastShowedMessage);

			if( tmpSet.isEmpty() ) return false;

			actMsg = tmpSet.first();
			lastShowedMessage = actMsg;

//			Log.d(TAG(), "unreaded messages:");
//			while(!tmpSet.isEmpty())
//			{
//				_ChatMsg msg = tmpSet.first();
//				printMsg(msg);
//				tmpSet.remove(msg);
//			}

			return true;
		}

		public String getLastMessageNick()
		{
			return actMsg.getNick();
		}

		public String getLastMessageBody()
		{
			return actMsg.getBody();
		}

		private void printMsg(_ChatMsg msg) { Log.d(TAG(), String.valueOf(msg.getTime()) + " " + msg.getNick() + " " + msg.getBody()); }
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

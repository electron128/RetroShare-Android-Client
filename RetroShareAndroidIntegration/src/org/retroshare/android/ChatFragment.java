/**
 * @license
 *
 * Copyright (c) 2013 Gioacchino Mazzurco <gio@eigenlab.org>.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.retroshare.android;


import android.app.Activity;
import android.database.DataSetObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.util.Linkify;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.apache.commons.lang.StringEscapeUtils;
import org.retroshare.android.utils.HtmlBase64ImageGetter;
import org.retroshare.android.utils.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import rsctrl.chat.Chat;

/**
 * @author G10h4ck
 */
public class ChatFragment extends ProxiedFragmentBase implements View.OnKeyListener
{
	interface ChatFragmentContainer { Chat.ChatId getChatId(ChatFragment f); }
	private ChatFragmentContainer cfc;

	private ChatMsgAdapter adapter = new ChatMsgAdapter();
	private ListView chatMessageList;
	private LayoutInflater mInflater;

	private HtmlBase64ImageGetter chatImageGetter;

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View fv = inflater.inflate(R.layout.chat_fragment, container);
		chatMessageList = (ListView)fv.findViewById(R.id.chatMessageList);
		chatMessageList.setAdapter(adapter);
		fv.findViewById(R.id.chatFragmentMessageEditText).setOnKeyListener(this);
		return fv;
	}
	@Override public void onAttach(Activity a)
	{
		super.onAttach(a);

		try { cfc = (ChatFragmentContainer) a; }
		catch (ClassCastException e) { throw new ClassCastException(a.toString() + " must implement ChatFragmentContainer"); }

		chatImageGetter = new HtmlBase64ImageGetter(getResources());
		mInflater = a.getLayoutInflater();
	}
	@Override public void onResume()
	{
		super.onResume();
		if(isBound())
		{
			RsChatService rsc = getConnectedServer().mRsChatService;
			rsc.disableNotificationForChat(cfc.getChatId(this));
			rsc.registerListener(adapter);
		}
	}
	@Override public void onPause()
	{
		if(isBound())
		{
			RsChatService rsc = getConnectedServer().mRsChatService;
			rsc.unregisterListener(adapter);
			rsc.enableNotificationForChat(cfc.getChatId(this));
		}
		super.onPause();
	}
	@Override public void onServiceConnected() { if(isVisible()) getConnectedServer().mRsChatService.registerListener(adapter); }

	private class ChatMsgAdapter implements ListAdapter, RsChatService.ChatServiceListener
	{
		private List<_ChatMessage> messageList = new ArrayList<_ChatMessage>();
		private int lastShowedPosition = 0;
		private List<DataSetObserver> observerList = new ArrayList<DataSetObserver>();
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View view = convertView;
			if (view == null) view = mInflater.inflate(R.layout.chat_message_item, parent, false);

			TextView msgBodyView = (TextView) view.findViewById(R.id.chatMessageTextView);
			_ChatMessage msg = messageList.get(position);

			if(msg.isMine()) msgBodyView.setBackgroundResource(R.drawable.bubble_green);
			else msgBodyView.setBackgroundResource(R.drawable.bubble_yellow);

			msgBodyView.setText(msg.getBody());
			Linkify.addLinks(msgBodyView, Pattern.compile(Util.URI_REG_EXP), "");

			return view;
		}

		private class UpdateMessagesAsyncTask extends AsyncTask<Void, Void, List<_ChatMessage>>
		{
			@Override
			protected List<_ChatMessage> doInBackground(Void... voids)
			{
				List<_ChatMessage> fmsg = new ArrayList<_ChatMessage>();
				List<Chat.ChatMessage> msgs = new ArrayList<Chat.ChatMessage>(getConnectedServer().mRsChatService.getChatHistoryForChatId(cfc.getChatId(ChatFragment.this)));
				for ( Chat.ChatMessage msg : msgs ) fmsg.add(new _ChatMessage(msg));
				return fmsg;
			}

			@Override protected void onPostExecute(List<_ChatMessage> ml)
			{
				messageList = ml;
				notifyObservers();
				if(recentlySentMessage) lastShowedPosition = messageList.size()-1;
				chatMessageList.smoothScrollToPosition(lastShowedPosition);
			}
		}

		@Override public void update() { new UpdateMessagesAsyncTask().execute(null, null, null); }
		@Override public int getViewTypeCount() { return 1; }
		@Override public void registerDataSetObserver(DataSetObserver observer) { observerList.add(observer); }
		@Override public void unregisterDataSetObserver(DataSetObserver observer) { observerList.remove(observer); }
		@Override public boolean areAllItemsEnabled() { return true; }
		@Override public boolean isEnabled(int position) { return true; }
		@Override public boolean hasStableIds() { return false; }
		@Override public boolean isEmpty() { return messageList.isEmpty(); }
		@Override public int getCount() { return messageList.size(); }
		@Override public int getItemViewType(int position) { return 0; }
		@Override public long getItemId(int position) { return 0; }
		@Override public Object getItem(int position) { return messageList.get(position); }

		/**
		 * Notify data observer that fresh data are available
		 */
		public void notifyObservers() { for(DataSetObserver obs : observerList) { obs.onChanged(); }; }
	}

	private class _ChatMessage
	{
		private Chat.ChatMessage msg;
		private Spanned msgBody;
		private boolean isMine;
		private int time;

		/**
		 * This is executed in the AsyncTask thread so we can do work here
		 * @param msg
		 */
		public _ChatMessage(Chat.ChatMessage msg)
		{
			this.msg = msg;

			Spanned spn = Html.fromHtml(msg.getMsg(), chatImageGetter, null);
			msgBody = spn;

			isMine = getConnectedServer().mRsPeersService.getOwnPerson().getName().equals(msg.getPeerNickname());

			time = msg.getSendTime();
			if ( time == 0) time = msg.getRecvTime();
		}

		/**
		 * Those methods are called in the UI thread so should be faster as possible
		 */
		public String getNick() { return msg.getPeerNickname(); }
		public Spanned getBody() { return msgBody; }
		public boolean isMine() { return isMine; }
		public int getTime() { return time; }
	}

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

	private boolean recentlySentMessage = false;
	public void sendChatMsg(View v)
	{
		Log.d(TAG(), "sendChatMsg(View v)");

		if(isBound())
		{
			EditText et = (EditText) getActivity().findViewById(R.id.chatFragmentMessageEditText);
			String inputText = et.getText().toString();

			if(inputText.length() > 0)
			{
				String msgText = StringEscapeUtils.escapeHtml(inputText);

				RsChatService chatService = getConnectedServer().mRsChatService;

				//                                                                     TODO ask drBob to put long instead of int
				Chat.ChatMessage.Builder msgBuilder = Chat.ChatMessage.newBuilder().setSendTime( (int)(System.currentTimeMillis()/1000L) );

				if( msgText.equals("a") ) // Easter egg
				{
					String android = "<span><span style=\"font-family:\'\';font-size:8.25pt;font-weight:400;font-style:normal;\"><img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAIAAAD8GO2jAAAAA3NCSVQICAjb4U/gAAAACXBIWXMAAA7EAAAOxAGVKw4bAAACzklEQVRIibVWS08TURQ+dzqFKZROpg+balsKpE1jiUSQh0gwggsTYhpZqgtjMHFjTFTcuNOd8hPc4MLEBcaFEIORhYaoSSVIRYkgEKChQCktbaXv62JKAXs7w4z128y55/F9d86dMzOABTG59BTjbLFoMh2d9j0XZgDhcOj34tz6SH6ZziQy2WR+ObUymEzHhBkQxhgE4fU9Uyl1vzZe+8MTO6kgAFSUHTGxp6zaTlrB1Bl6hMtFBIKx2bGZe4Hod2LUxDZ3OR9XMWaZAqthz4i3L5WJCdQzSu3FhkFdpbNYAlUsEIn73ny7mcrEWmvu1ujPFyY0227XGi7EU8ER7414akuywPjcw0Q6DAD6qnoNYy1M0KtdrKoaAGIJ/+f5J8V4yC3ais2+8PQAiJz/3jYRfbXtfUWZgRAiFswHRg/PDgBZnF7cfEfWJnoD0enDs+dKIuSSvRYtBEZ9oU82XbeZO+MPe+KpkCQBlVJrZBsXAm99oY82XZeZ6zggsBz8MOy9DgAIUb2NL8fnHvnDXyQJmLn2Bkvf8NQuyckhQ1U95Fu0FpnkDYyz69uTkqjzWNveRxL5ytuEMxB9eRwGeZKic1Aq/C8BDBkBgRK0CARahHFGHicCRaGzlC1CiODMCVCIIF4SkO6AuBOp2OXICejVrnxIrz4uj9Ogri8kofmLVXu2yzmwsjVerTtnYpvlCVi0nd3OgeUcScsBAQBwGN0Oo5u3EcjoEgIAu9Ft3yXhQX6KBL6xxaBTk0toore1tl9BlYd25qMJ/2b0B+/UMGau0g4Aq2FPMh3hnce40zTFaCvsTbZbRCqR35afa6/GZvp5+4T5WnvdAwAYmujdiHh55+XWMQ1jEWAQGTTRw8A4K5wgZ5IlPQCyBKSMvYhAuZLbs2n2LwMhqpzW/JOAhetwGC9RiDaxTa6jV3hnS80djcpKK1RttfeZfTsg4g/+D5MwF/zpFwAAAABJRU5ErkJggg==\"/></span></span>";
					msgBuilder.setMsg(android);
				}
				else msgBuilder.setMsg(msgText);

				msgBuilder.setId(cfc.getChatId(this));
				chatService.sendChatMessage(msgBuilder.build());

				et.setText("");

				recentlySentMessage = true;
			}
		}
		else Log.e(TAG(), "sendChatMsg(View v) cannot send message without connection to rsProxy");
	}

	public ChatFragment() { super(); }
}

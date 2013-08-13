package org.retroshare.android;


import android.app.Activity;
import android.database.DataSetObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import rsctrl.chat.Chat;

/**
 * Created by G10h4ck on 8/12/13.
 * Will supersede ChatActivity
 */
public class ChatFragment extends ProxiedFragmentBase
{
	interface ChatFragmentContainer { Chat.ChatId getChatId(); }
	private ChatFragmentContainer cfc;

	private ChatMsgAdapter adapter = new ChatMsgAdapter();
	private LayoutInflater mInflater;

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View fv = inflater.inflate(R.layout.chat_fragment, container);
		((ListView)fv.findViewById(R.id.chatMessageList)).setAdapter(adapter);
		return fv;
	}
	@Override public void onAttach(Activity a)
	{
		super.onAttach(a);

		try { cfc = (ChatFragmentContainer) a; }
		catch (ClassCastException e) { throw new ClassCastException(a.toString() + " must implement ChatFragmentContainer"); }

		mInflater = a.getLayoutInflater();
	}
	@Override public void onResume()
	{
		super.onResume();
		if(isBound()) getConnectedServer().mRsChatService.registerListener(adapter);
	}
	@Override public void onPause()
	{
		if(isBound()) getConnectedServer().mRsChatService.unregisterListener(adapter);
		super.onPause();
	}

	private class ChatMsgAdapter implements ListAdapter, RsChatService.ChatServiceListener
	{
		private List<_ChatMessage> messageList = new ArrayList<_ChatMessage>();
		private List<DataSetObserver> observerList = new ArrayList<DataSetObserver>();

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View view = convertView;
			if (view == null) view = mInflater.inflate(R.layout.chat_message_item, parent, false);

			_ChatMessage msg = messageList.get(position);
			TextView msgBodyView = (TextView) view.findViewById(R.id.chatMessageTextView);
			msgBodyView.setText(msg.getBody());
			if(msg.isMine()) msgBodyView.setBackgroundResource(R.drawable.bubble_green);
			//else msgBodyView.setBackgroundResource(R.drawable.bubble_yellow);

			return view;
		}

		private class UpdateMessagesAsyncTask extends AsyncTask<Void, Void, List<_ChatMessage>>
		{
			@Override
			protected List<_ChatMessage> doInBackground(Void... voids)
			{
				ArrayList fmsg = new ArrayList<_ChatMessage>();
				for ( Chat.ChatMessage msg : getConnectedServer().mRsChatService.getChatHistoryForChatId(cfc.getChatId()) )
					fmsg.add(new _ChatMessage(msg));

				return fmsg;
			}

			@Override protected void onPostExecute(List<_ChatMessage> ml)
			{
				messageList = ml;
				notifyObservers();
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
		private boolean isMine;
		private int time;

		/**
		 * This is executed in the AsyncTask thread so we can do work here
		 * @param msg
		 */
		public _ChatMessage(Chat.ChatMessage msg)
		{
			this.msg = msg;

			isMine = getConnectedServer().mRsPeersService.getOwnPerson().getName().equals(msg.getPeerNickname());

			time = msg.getSendTime();
			if ( time == 0) time = msg.getRecvTime();
		}

		/**
		 * Those methods are called in the UI thread so should be faster as possible
		 */
		public String getNick() { return msg.getPeerNickname(); }
		public String getBody() { return msg.getMsg(); }
		public boolean isMine() { return isMine; }
		public int getTime() { return time; }
	}

	public ChatFragment() { super(); }
}

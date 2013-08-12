package org.retroshare.android;


import android.app.Activity;
import android.database.DataSetObserver;
import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;

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

	@Override
	public void onAttach(Activity a)
	{
		super.onAttach(a);

		try { cfc = (ChatFragmentContainer) a; }
		catch (ClassCastException e) { throw new ClassCastException(a.toString() + " must implement ChatFragmentContainer"); }
	}

	private class ChatMsgAdapter implements ListAdapter, RsChatService.ChatServiceListener
	{
		private List<_ChatMessage> messageList = new ArrayList<_ChatMessage>();
		private List<DataSetObserver> observerList = new ArrayList<DataSetObserver>();

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			return null;
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

		public _ChatMessage(Chat.ChatMessage msg) { this.msg = msg; }

		public String getNick() { return msg.getPeerNickname(); }
		public String getBody() { return msg.getMsg(); }
		public boolean isMine() { return getConnectedServer().mRsPeersService.getOwnPerson().getName().equals(msg.getPeerNickname()); }
		public int getTime()
		{
			if (msg.getSendTime() == 0) return msg.getRecvTime();
			return msg.getSendTime();
		}
	}
}

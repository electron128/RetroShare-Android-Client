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
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Html;
import android.text.Spanned;
import android.text.util.Linkify;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.apache.commons.lang.StringEscapeUtils;
import org.retroshare.android.utils.ColorHash;
import org.retroshare.android.utils.HtmlBase64ImageGetter;
import org.retroshare.android.utils.Util;

import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.retroshare.android.RsConversationService.ConversationId;
import org.retroshare.android.RsConversationService.ConversationMessage;
import org.retroshare.android.RsConversationService.ConversationEvent;
import org.retroshare.android.RsConversationService.ConversationEventKind;
import org.retroshare.android.RsConversationService.NewMessageConversationEvent;


public class ConversationFragment extends RsServiceClientFragmentBase implements View.OnKeyListener, ListView.OnScrollListener
{
	@Override public String TAG() { return "ConversationFragment"; }

	public static final String CONVERSATION_ID_EXTRA_KEY = "org.retroshare.android.extra_keys.conversationID";
	public static final int CONVERSATION_IMAGE_MAX_DIMENSION = 200;

	private ConversationId conversationId;

	private List<_ConversationMessage> messageList = new ArrayList<_ConversationMessage>();
	private final ConversationAdapter adapter = new ConversationAdapter();
	private ListView conversationMessageListView;
	private LayoutInflater mInflater;
	private View moreMessageUpIndicator;

	private HtmlBase64ImageGetter chatImageGetter;

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View fv = inflater.inflate(R.layout.conversation_fragment, container, false);

		/**
		 * Message List
		 */
		conversationMessageListView = (ListView)fv.findViewById(R.id.chatMessageList);
		conversationMessageListView.setAdapter(adapter);

		/**
		 * New message editor
		 */
		View inputTextView = fv.findViewById(R.id.chatFragmentMessageEditText);
		inputTextView.setOnKeyListener(this);
		inputTextView.setOnLongClickListener(new OnShowSendExtraLongClickListener());

		/**
		 * More items down indicator
		 */
		View moreMessageDownIndicator = fv.findViewById(R.id.moreChatMessageDownImageView);
		moreMessageDownIndicator.setVisibility(View.INVISIBLE);
		moreMessageDownIndicator.setOnClickListener(new GoDownButtonListener());
		conversationMessageListView.setScrollIndicators(null, moreMessageDownIndicator);

		/**
		 * More items up indicator ( Logic is different from down one )
		 */
		moreMessageUpIndicator = fv.findViewById(R.id.moreChatMessageUpImageView);
		moreMessageUpIndicator.setVisibility(View.INVISIBLE);
		moreMessageUpIndicator.setOnClickListener(new GoUpButtonListener());
		conversationMessageListView.setOnScrollListener(this);

		/**
		 * Send Extra menu
		 */
		sendExtraMenu = fv.findViewById(R.id.sendExtraLayout);
		sendExtraMenu.setVisibility(View.INVISIBLE);
		sendExtraMenu.findViewById(R.id.sendImageButton).setOnClickListener(new OnAddImageClickListener());
//		sendExtraMenu.findViewById(R.id.sendFileButton).setVisibility(View.GONE);
		sendExtraMenu.findViewById(R.id.showInfoImageButton).setOnClickListener(new ShowConversationInfoLongClickListener());

		return fv;
	}
	@Override public void onAttach(Activity a)
	{
		super.onAttach(a);

		final Bundle arguments = getArguments();
		if(arguments.containsKey(CONVERSATION_ID_EXTRA_KEY)) conversationId = arguments.getParcelable(CONVERSATION_ID_EXTRA_KEY);
		else throw new RuntimeException(TAG() + " need arguments contains valid value for " + ConversationFragment.CONVERSATION_ID_EXTRA_KEY);

		chatImageGetter = new HtmlBase64ImageGetter(getResources());
		mInflater = a.getLayoutInflater();
	}
	@Override public void onResume()
	{
		super.onResume();
		initializeConversation();
	}
	@Override public void onPause()
	{
		if(isBound()) getConnectedServer().mRsConversationService.enableNotificationForConversation(conversationId);
		super.onPause();
	}
	@Override public void onServiceConnected(ComponentName className, IBinder service)
	{
		super.onServiceConnected(className, service);
		initializeConversation();
	}
	@Override public void registerRsServicesListeners()
	{
		Log.d(TAG(), "registerRsServicesListeners()");
		super.registerRsServicesListeners();
		getConnectedServer().mRsConversationService.registerRsConversationServiceListener(adapter);
	}
	@Override public void unregisterRsServicesListeners()
	{
		Log.d(TAG(), "unregisterRsServicesListeners()");
		getConnectedServer().mRsConversationService.unregisterRsConversationServiceListener(adapter);
		super.unregisterRsServicesListeners();
	}

	private class ConversationAdapter implements ListAdapter, RsConversationService.RsConversationServiceListener
	{


		private class ReloadMessagesHistoryAsyncTask extends AsyncTask<Integer, Void, ReloadMessagesHistoryAsyncTask.MsgsBund>
		{
			@Override
			protected MsgsBund doInBackground(Integer... lastMineIntegers)
			{
				Log.d(TAG(), "doInBackground(" + String.valueOf(lastMineIntegers[0])  + ")");

				List<_ConversationMessage> fmsg = new ArrayList<_ConversationMessage>();
				int lastMineIndex = lastMineIntegers[0];

				if(isBound()) try
				{
					List<ConversationMessage> msgs = (List<ConversationMessage>) getConnectedServer().mRsConversationService.getConversationHistory(conversationId);
					for ( int i = 0; i < msgs.size(); ++i )
					{
						_ConversationMessage msg = new _ConversationMessage(msgs.get(i));
						fmsg.add(msg);
						if( (i > lastMineIndex) && msg.isMine() ) lastMineIndex = i;
					}
				} catch (RuntimeException e) {}

				return new MsgsBund(fmsg, lastMineIndex);
			}

			@Override protected void onPostExecute(MsgsBund mb)
			{
				messageList = mb.messagesList;
				lastMineMessageIndex = mb.lastMinePosition;
				int lastPosition = messageList.size()-1;
				conversationMessageListView.setSelection(lastPosition);
				adapter.notifyObservers();
				autoScrollTo(lastPosition, false);
			}

			public final class MsgsBund
			{
				public MsgsBund(List<_ConversationMessage> msgs, int lastMineIndex) { messagesList = msgs; lastMinePosition = lastMineIndex; };
				public final List<_ConversationMessage> messagesList;
				public final int lastMinePosition;
			}
		}

		/** Implements RsConversationServiceListener */
		private final Long handle = RsConversationService.RsConversationServiceListenerUniqueHandleFactory.getNewUniqueHandle();
		public Long getUniqueRsConversationServiceListenerHandle() { return handle; }
		@Override public void onConversationsEvent(ConversationEvent event)
		{
			Log.d(TAG(), "onConversationsEvent(...)");
			if(event.getEventKind().equals(ConversationEventKind.NEW_CONVERSATION_MESSAGE) && ((NewMessageConversationEvent)event).getConversationMessage().getConversationId().equals(conversationId)) new ReloadMessagesHistoryAsyncTask().execute(Integer.valueOf(lastMineMessageIndex));
		}

		/** Implements ListAdapter */
		public View getView(int position, View convertView, ViewGroup parent)
		{
			_ConversationMessage msg = messageList.get(position);

			View view = convertView;
			if (view == null) view = mInflater.inflate(R.layout.chat_message_item, parent, false);

			TextView msgBodyView = (TextView) view.findViewById(R.id.chatMessageBodyTextView);
			Drawable background = getResources().getDrawable(R.drawable.bubble_colorizable); // Doing this here can create problems?
			int backgroundColor;
			if(msg.isMine()) backgroundColor = 0xFFA8D324; // TODO: Make own color configurable
			else backgroundColor = ColorHash.getObjectColor(msg.getNick());
			background.setColorFilter(backgroundColor, PorterDuff.Mode.MULTIPLY);
			msgBodyView.setBackgroundDrawable(background);
			msgBodyView.setLinkTextColor(Util.opposeColor(backgroundColor));
			msgBodyView.setText(msg.getBody());
			Linkify.addLinks(msgBodyView, Pattern.compile(Util.URI_REG_EXP), ""); // This must be executed on UI thread

			TextView timeView = (TextView) view.findViewById(R.id.chatMessageTimeTextView);
			timeView.setText(msg.getTime());

			TextView nickView = (TextView) view.findViewById(R.id.chatMessageNickTextView);
			nickView.setText(msg.getNick());

			return view;
		}
		public int getViewTypeCount() { return 1; }
		public void registerDataSetObserver(DataSetObserver observer) { observerSet.add(observer); }
		public void unregisterDataSetObserver(DataSetObserver observer) { observerSet.remove(observer); }
		public boolean areAllItemsEnabled() { return true; }
		public boolean isEnabled(int position) { return true; }
		public boolean hasStableIds() { return false; }
		public boolean isEmpty() { return messageList.isEmpty(); }
		public int getCount() { return messageList.size(); }
		public int getItemViewType(int position) { return 0; }
		public long getItemId(int position) { return 0; }
		public Object getItem(int position) { return messageList.get(position); }

		private Set<DataSetObserver> observerSet = new HashSet<DataSetObserver>();

		/**
		 * Notify data observer that fresh data are available
		 */
		public void notifyObservers() { for(DataSetObserver obs : observerSet) obs.onChanged(); }
	}

	private class _ConversationMessage
	{
		/**
		 * This is executed in the AsyncTask thread so we can do work here
		 * @param msg
		 */
		public _ConversationMessage(RsConversationService.ConversationMessage msg)
		{
			Spanned spn = Html.fromHtml(msg.getMessageString(), chatImageGetter, null);
			msgBody = spn;
			nick = msg.getAuthorString();
			isMine = getConnectedServer().mRsPeersService.getOwnPerson().getName().equals(nick);

			if(msg.hasTime()) time = new SimpleDateFormat(msg.getDefaultTimeFormat()).format(new Date(msg.getTime()));
			else time = "";
		}

		/**
		 * Those methods are called in the UI thread so should be faster as possible
		 */
		public String getNick() { return nick; }
		public Spanned getBody() { return msgBody; }
		public boolean isMine() { return isMine; }
		public String getTime() { return time; }

		private final Spanned msgBody;
		private final boolean isMine;
		private final String time;
		private final String nick;
	}

	private void initializeConversation()
	{
		if(isUserVisible() && isBound())
		{
			RsConversationService rsConversationService = getConnectedServer().mRsConversationService;
			rsConversationService.cancelNotificationForConversation(conversationId);
			rsConversationService.disableNotificationForConversation(conversationId);
			lastMineMessageIndex = 0;
			adapter.new ReloadMessagesHistoryAsyncTask().execute(Integer.valueOf(lastMineMessageIndex));
		}
	}

	/**
	 * Message sending stuff
	 */

	private void sendChatMsg(View v)
	{
		if(isBound())
		{
			EditText et = (EditText) getActivity().findViewById(R.id.chatFragmentMessageEditText);
			String inputText = et.getText().toString();

			if(inputText.length() > 0)
			{
				String msgText;
				if( inputText.equals("a") ) msgText = "<span><span style=\"font-family:\'\';font-size:8.25pt;font-weight:400;font-style:normal;\"><img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAIAAAD8GO2jAAAAA3NCSVQICAjb4U/gAAAACXBIWXMAAA7EAAAOxAGVKw4bAAACzklEQVRIibVWS08TURQ+dzqFKZROpg+balsKpE1jiUSQh0gwggsTYhpZqgtjMHFjTFTcuNOd8hPc4MLEBcaFEIORhYaoSSVIRYkgEKChQCktbaXv62JKAXs7w4z128y55/F9d86dMzOABTG59BTjbLFoMh2d9j0XZgDhcOj34tz6SH6ZziQy2WR+ObUymEzHhBkQxhgE4fU9Uyl1vzZe+8MTO6kgAFSUHTGxp6zaTlrB1Bl6hMtFBIKx2bGZe4Hod2LUxDZ3OR9XMWaZAqthz4i3L5WJCdQzSu3FhkFdpbNYAlUsEIn73ny7mcrEWmvu1ujPFyY0227XGi7EU8ER7414akuywPjcw0Q6DAD6qnoNYy1M0KtdrKoaAGIJ/+f5J8V4yC3ais2+8PQAiJz/3jYRfbXtfUWZgRAiFswHRg/PDgBZnF7cfEfWJnoD0enDs+dKIuSSvRYtBEZ9oU82XbeZO+MPe+KpkCQBlVJrZBsXAm99oY82XZeZ6zggsBz8MOy9DgAIUb2NL8fnHvnDXyQJmLn2Bkvf8NQuyckhQ1U95Fu0FpnkDYyz69uTkqjzWNveRxL5ytuEMxB9eRwGeZKic1Aq/C8BDBkBgRK0CARahHFGHicCRaGzlC1CiODMCVCIIF4SkO6AuBOp2OXICejVrnxIrz4uj9Ogri8kofmLVXu2yzmwsjVerTtnYpvlCVi0nd3OgeUcScsBAQBwGN0Oo5u3EcjoEgIAu9Ft3yXhQX6KBL6xxaBTk0toore1tl9BlYd25qMJ/2b0B+/UMGau0g4Aq2FPMh3hnce40zTFaCvsTbZbRCqR35afa6/GZvp5+4T5WnvdAwAYmujdiHh55+XWMQ1jEWAQGTTRw8A4K5wgZ5IlPQCyBKSMvYhAuZLbs2n2LwMhqpzW/JOAhetwGC9RiDaxTa6jV3hnS80djcpKK1RttfeZfTsg4g/+D5MwF/zpFwAAAABJRU5ErkJggg==\"/></span></span>"; // Easter egg
				else msgText = StringEscapeUtils.escapeHtml(inputText);

				ConversationMessage msg = ConversationMessage.Factory.newConversationMessage(conversationId);
				msg.setMessageString(msgText);
				getConnectedServer().mRsConversationService.sendConversationMessage(msg);

				et.setText("");

				autoScrollTo(conversationMessageListView.getCount(), true);
			}
		}
		else Log.e(TAG(), "sendChatMsg(View v) cannot send message without connection to rsProxy");
	}
	@Override public boolean onKey(View v, int keyCode, KeyEvent event)
	{
		boolean enterPressed = ( event.getAction() == KeyEvent.ACTION_DOWN ) & ( event.getKeyCode() == KeyEvent.KEYCODE_ENTER );
		if(enterPressed) sendChatMsg(null);
		return enterPressed;
	}

	/**
	 * Autoscroll stuff
	 */
	private boolean humanScrolled = false;
	private int lastMineMessageIndex = 0;
	@Override public void onScrollStateChanged(AbsListView absListView, int i) {}
	@Override public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount)
	{
		if(lastMineMessageIndex < firstVisibleItem) moreMessageUpIndicator.setVisibility(View.VISIBLE);
		else moreMessageUpIndicator.setVisibility(View.INVISIBLE);
	}
	private void autoScrollTo(int position, boolean resetHumanScrolled)
	{
		if(resetHumanScrolled) humanScrolled = false;
		if(!humanScrolled)
		{
			conversationMessageListView.smoothScrollToPosition(position);
			conversationMessageListView.setSelection(position);
		}
	}
	private class GoDownButtonListener implements View.OnClickListener { public void onClick(View v) { autoScrollTo(conversationMessageListView.getCount()-1, true); } }
	private class GoUpButtonListener implements View.OnClickListener
	{
		public void onClick(View v)
		{
			autoScrollTo(lastMineMessageIndex, true);
			humanScrolled = true;
		}
	}

	/**
	 * ExtraMenu stuff
	 */
	private View sendExtraMenu;
	private final class OnShowSendExtraLongClickListener implements View.OnLongClickListener { @Override public boolean onLongClick(View view) { sendExtraMenu.setVisibility(View.VISIBLE); return true; } }
	private final static int SELECT_IMAGE_INTENT_REQUEST_CODE = 1;
	private final class OnAddImageClickListener implements View.OnClickListener
	{
		@Override public void onClick(View view)
		{
			sendExtraMenu.setVisibility(View.INVISIBLE);

			Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
			photoPickerIntent.setType("image/*");
			startActivityForResult(photoPickerIntent, SELECT_IMAGE_INTENT_REQUEST_CODE);
		}
	}
	@Override public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if(resultCode != Activity.RESULT_OK) return;

		switch (requestCode)
		{
			case SELECT_IMAGE_INTENT_REQUEST_CODE:
			{
				new SendImageAsyncTask().execute(data.getData());
				break;
			}
		}
	}
	private final class SendImageAsyncTask extends AsyncTask<Uri, Void, ConversationMessage>
	{
		ContentResolver mContentResolver;
		@Override public void onPreExecute() { mContentResolver = getActivity().getContentResolver(); }
		@Override protected ConversationMessage doInBackground(Uri... uris)
		{
			Bitmap bitmapImage;
			try { bitmapImage = Util.loadFittingBitmap(mContentResolver, uris[0], CONVERSATION_IMAGE_MAX_DIMENSION); }
			catch (FileNotFoundException e) { return null; }

			StringBuilder msgBodyBuilder = new StringBuilder(CONVERSATION_IMAGE_MAX_DIMENSION * CONVERSATION_IMAGE_MAX_DIMENSION);
			msgBodyBuilder.append("<img src=\"data:image/png;base64,");
			msgBodyBuilder.append(Util.encodeTobase64(bitmapImage, Bitmap.CompressFormat.PNG, 100)); bitmapImage.recycle(); // Mark the bitmap as garbage
			msgBodyBuilder.append("\"/>");

			ConversationMessage msg = ConversationMessage.Factory.newConversationMessage(conversationId);
			msg.setMessageString(msgBodyBuilder.toString());

			return msg;
		}
		@Override public void onPostExecute(ConversationMessage msg) { if(msg != null) getConnectedServer().mRsConversationService.sendConversationMessage(msg); }
	}
	private static final String CONFERSATION_INFO_DIALOG_FRAGMENT_TAG = "org.retroshare.android.ConversationInfoDialogFragment";
	private final class ShowConversationInfoLongClickListener implements View.OnClickListener
	{
		@Override public void onClick(View v)
		{
			RsConversationService.ConversationInfo info = getConnectedServer().mRsConversationService.getConversationInfo(conversationId);
			Bundle args = new Bundle(1);
			args.putParcelable(ConversationInfoDialogFragment.CONVERSATION_INFO_EXTRA, info);
			ConversationInfoDialogFragment df = new ConversationInfoDialogFragment(getActivity());
			df.setArguments(args);
			df.show(getFragmentManager(), CONFERSATION_INFO_DIALOG_FRAGMENT_TAG);
		}
	}


	public ConversationFragment() { super(); }
}

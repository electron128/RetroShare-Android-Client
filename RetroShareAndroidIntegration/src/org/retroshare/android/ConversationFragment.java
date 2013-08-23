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
import android.content.ContentResolver;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.net.Uri;
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


public class ConversationFragment extends ProxiedFragmentBase implements View.OnKeyListener
{
	@Override public String TAG() { return "ConversationFragment"; }

	public static final int CONVERSATION_IMAGE_MAX_DIMENSION = 200;

	interface ConversationFragmentContainer { ConversationId getConversationId(ConversationFragment f); }
	private ConversationFragmentContainer cfc;

	private RsConversationService mRsConversationService;

	private List<_ChatMessage> messageList = new ArrayList<_ChatMessage>();
	private final ConversationAdapter adapter = new ConversationAdapter();
	private ListView conversationMessageListView;
	private LayoutInflater mInflater;
	private int lastShowedPosition = 0;
	private int autoScrollSemaphore = 0;

	private HtmlBase64ImageGetter chatImageGetter;

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View fv = inflater.inflate(R.layout.conversation_fragment, container);

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
		 * More item down indicator
		 */
		View moreMessageDownIndicator = fv.findViewById(R.id.moreChatMessageDownImageView);
		moreMessageDownIndicator.setVisibility(View.INVISIBLE);
		moreMessageDownIndicator.setOnClickListener(new GoDownButtonListener());
		conversationMessageListView.setScrollIndicators(null, moreMessageDownIndicator);

		/**
		 * Send Extra menu
		 */
		sendExtraMenu = fv.findViewById(R.id.sendExtraLayout);
		sendExtraMenu.setVisibility(View.INVISIBLE);
		sendExtraMenu.findViewById(R.id.sendImageButton).setOnClickListener(new OnAddImageClickListener());
		sendExtraMenu.findViewById(R.id.sendFileButton).setVisibility(View.GONE);
		sendExtraMenu.findViewById(R.id.showInfoImageButton).setOnClickListener(new ShowLobbyInfoLongClickListener());

		return fv;
	}
	@Override public void onAttach(Activity a)
	{
		super.onAttach(a);

		try { cfc = (ConversationFragmentContainer) a; }
		catch (ClassCastException e) { throw new ClassCastException(a.toString() + " must implement ChatFragmentContainer"); }

		chatImageGetter = new HtmlBase64ImageGetter(getResources());
		mInflater = a.getLayoutInflater();

	}
	@Override public void onResume()
	{
		super.onResume();
		onVisibleAndBound();
	}
	@Override public void onPause()
	{
		if(isBound()) mRsConversationService.enableNotificationForConversation(cfc.getConversationId(this));
		super.onPause();
	}
	@Override public void onServiceConnected()
	{
		mRsConversationService = getConnectedServer().mRsConversationService;
		onVisibleAndBound();
		super.onServiceConnected();
	}
	@Override public void registerRsServicesListeners() { mRsConversationService.registerRsConversationServiceListener(adapter); }
	@Override public void unregisterRsServicesListeners() { mRsConversationService.unregisterRsConversationServiceListener(adapter); }

	private class ConversationAdapter implements ListAdapter, RsConversationService.RsConversationServiceListener
	{
		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			_ChatMessage msg = messageList.get(position);

			View view = convertView;
			if (view == null) view = mInflater.inflate(R.layout.chat_message_item, parent, false);

			TextView msgBodyView = (TextView) view.findViewById(R.id.chatMessageBodyTextView);
			if(msg.isMine()) msgBodyView.setBackgroundResource(R.drawable.bubble_green_spaced); // TODO: use a grey image + color filter instead of different resources, so we can use multicolor in group chat
			else msgBodyView.setBackgroundResource(R.drawable.bubble_yellow_spaced);
			msgBodyView.setText(msg.getBody());
			Linkify.addLinks(msgBodyView, Pattern.compile(Util.URI_REG_EXP), ""); // This must be executed on UI thread

			TextView timeView = (TextView) view.findViewById(R.id.chatMessageTimeTextView);
			timeView.setText(msg.getTime());

			TextView nickView = (TextView) view.findViewById(R.id.chatMessageNickTextView);
			nickView.setText(msg.getNick());

			return view;
		}

		private class UpdateMessagesAsyncTask extends AsyncTask<Void, Void, List<_ChatMessage>>
		{
			@Override
			protected List<_ChatMessage> doInBackground(Void... voids)
			{
				List<_ChatMessage> fmsg = new ArrayList<_ChatMessage>();
				ConversationId id = cfc.getConversationId(ConversationFragment.this);
				List<RsConversationService.ConversationMessage> msgs = getConnectedServer().mRsConversationService.getConversationHistory(id);
				for ( RsConversationService.ConversationMessage msg : msgs ) fmsg.add(new _ChatMessage(msg));
				return fmsg;
			}

			@Override protected void onPostExecute(List<_ChatMessage> ml)
			{
				messageList = ml;

				conversationMessageListView.setSelection(lastShowedPosition);

				if(autoScrollSemaphore > 0)
				{
					lastShowedPosition = messageList.size()-1;
					--autoScrollSemaphore;
				}

				notifyObservers();

				conversationMessageListView.smoothScrollToPosition(lastShowedPosition);
			}
		}

		@Override public void onConversationsUpdate() { new UpdateMessagesAsyncTask().execute(null, null, null); }
		@Override public int getViewTypeCount() { return 1; }
		@Override public void registerDataSetObserver(DataSetObserver observer) { observerSet.add(observer); }
		@Override public void unregisterDataSetObserver(DataSetObserver observer) { observerSet.remove(observer); }
		@Override public boolean areAllItemsEnabled() { return true; }
		@Override public boolean isEnabled(int position) { return true; }
		@Override public boolean hasStableIds() { return false; }
		@Override public boolean isEmpty() { return messageList.isEmpty(); }
		@Override public int getCount() { return messageList.size(); }
		@Override public int getItemViewType(int position) { return 0; }
		@Override public long getItemId(int position) { return 0; }
		@Override public Object getItem(int position) { return messageList.get(position); }

		private Set<DataSetObserver> observerSet = new HashSet<DataSetObserver>();

		/**
		 * Notify data observer that fresh data are available
		 */
		public void notifyObservers() { for(DataSetObserver obs : observerSet) obs.onChanged(); }
	}

	private class _ChatMessage
	{
		private Spanned msgBody;
		private boolean isMine;
		private String time;
		private String nick;

		/**
		 * This is executed in the AsyncTask thread so we can do work here
		 * @param msg
		 */
		public _ChatMessage(RsConversationService.ConversationMessage msg)
		{
			Spanned spn = Html.fromHtml(msg.getMessageString(), chatImageGetter, null);
			msgBody = spn;
			nick = msg.getAuthorString();
			isMine = getConnectedServer().mRsPeersService.getOwnPerson().getName().equals(nick);

			if(msg.hasTime()) time = new SimpleDateFormat(msg.getDefaultTimeFormat()).format(new Date(msg.getTime()));
		}

		/**
		 * Those methods are called in the UI thread so should be faster as possible
		 */
		public String getNick() { return nick; }
		public Spanned getBody() { return msgBody; }
		public boolean isMine() { return isMine; }
		public String getTime() { return time; }
	}

	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event)
	{
		boolean enterPressed = ( event.getAction() == KeyEvent.ACTION_DOWN ) & ( event.getKeyCode() == KeyEvent.KEYCODE_ENTER );
		if(enterPressed) sendChatMsg(null);
		return enterPressed;
	}

	private void onVisibleAndBound()
	{
		if(isUserVisible() && isBound())
		{
			ConversationId id = cfc.getConversationId(this);
			mRsConversationService.cancelNotificationForConversation(id);
			mRsConversationService.disableNotificationForConversation(id);
			adapter.onConversationsUpdate();
		}
	}

	private void sendChatMsg(View v)
	{
		Log.d(TAG(), "sendChatMsg(View v)");

		if(isBound())
		{
			EditText et = (EditText) getActivity().findViewById(R.id.chatFragmentMessageEditText);
			String inputText = et.getText().toString();

			if(inputText.length() > 0)
			{
				String msgText;
				if( inputText.equals("a") ) msgText = "<span><span style=\"font-family:\'\';font-size:8.25pt;font-weight:400;font-style:normal;\"><img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAIAAAD8GO2jAAAAA3NCSVQICAjb4U/gAAAACXBIWXMAAA7EAAAOxAGVKw4bAAACzklEQVRIibVWS08TURQ+dzqFKZROpg+balsKpE1jiUSQh0gwggsTYhpZqgtjMHFjTFTcuNOd8hPc4MLEBcaFEIORhYaoSSVIRYkgEKChQCktbaXv62JKAXs7w4z128y55/F9d86dMzOABTG59BTjbLFoMh2d9j0XZgDhcOj34tz6SH6ZziQy2WR+ObUymEzHhBkQxhgE4fU9Uyl1vzZe+8MTO6kgAFSUHTGxp6zaTlrB1Bl6hMtFBIKx2bGZe4Hod2LUxDZ3OR9XMWaZAqthz4i3L5WJCdQzSu3FhkFdpbNYAlUsEIn73ny7mcrEWmvu1ujPFyY0227XGi7EU8ER7414akuywPjcw0Q6DAD6qnoNYy1M0KtdrKoaAGIJ/+f5J8V4yC3ais2+8PQAiJz/3jYRfbXtfUWZgRAiFswHRg/PDgBZnF7cfEfWJnoD0enDs+dKIuSSvRYtBEZ9oU82XbeZO+MPe+KpkCQBlVJrZBsXAm99oY82XZeZ6zggsBz8MOy9DgAIUb2NL8fnHvnDXyQJmLn2Bkvf8NQuyckhQ1U95Fu0FpnkDYyz69uTkqjzWNveRxL5ytuEMxB9eRwGeZKic1Aq/C8BDBkBgRK0CARahHFGHicCRaGzlC1CiODMCVCIIF4SkO6AuBOp2OXICejVrnxIrz4uj9Ogri8kofmLVXu2yzmwsjVerTtnYpvlCVi0nd3OgeUcScsBAQBwGN0Oo5u3EcjoEgIAu9Ft3yXhQX6KBL6xxaBTk0toore1tl9BlYd25qMJ/2b0B+/UMGau0g4Aq2FPMh3hnce40zTFaCvsTbZbRCqR35afa6/GZvp5+4T5WnvdAwAYmujdiHh55+XWMQ1jEWAQGTTRw8A4K5wgZ5IlPQCyBKSMvYhAuZLbs2n2LwMhqpzW/JOAhetwGC9RiDaxTa6jV3hnS80djcpKK1RttfeZfTsg4g/+D5MwF/zpFwAAAABJRU5ErkJggg==\"/></span></span>"; // Easter egg
				else msgText = StringEscapeUtils.escapeHtml(inputText);

				ConversationId id = cfc.getConversationId(this);
				ConversationMessage msg = ConversationMessage.Factory.newConversationMessage(id);
				msg.setMessageString(msgText);
				mRsConversationService.sendConversationMessage(msg);

				et.setText("");

				fillAutoScrollSemaphore();
			}
		}
		else Log.e(TAG(), "sendChatMsg(View v) cannot send message without connection to rsProxy");
	}

	private class GoDownButtonListener implements View.OnClickListener
	{
		public void onClick(View v)
		{
			lastShowedPosition = conversationMessageListView.getCount()-1;
			conversationMessageListView.setSelection(lastShowedPosition);
			conversationMessageListView.smoothScrollToPosition(lastShowedPosition);
			fillAutoScrollSemaphore();
		}
	}

	private int fillAutoScrollSemaphore() { return (autoScrollSemaphore = (conversationMessageListView.getLastVisiblePosition() - conversationMessageListView.getFirstVisiblePosition())-1); }


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

			ConversationId id = cfc.getConversationId(ConversationFragment.this);
			ConversationMessage msg = ConversationMessage.Factory.newConversationMessage(id);
			msg.setMessageString(msgBodyBuilder.toString());

			return msg;
		}
		@Override public void onPostExecute(ConversationMessage msg) { if(msg != null) mRsConversationService.sendConversationMessage(msg); }
	}
	private final class ShowLobbyInfoLongClickListener implements View.OnClickListener
	{
		@Override public void onClick(View v)
		{
			RsConversationService.ConversationInfo info = mRsConversationService.getConversationInfo(cfc.getConversationId(ConversationFragment.this));
			Bundle args = new Bundle(1);
			args.putParcelable(ConversationInfoDialogFragment.CONVERSATION_INFO_EXTRA, info);
			ConversationInfoDialogFragment df = new ConversationInfoDialogFragment(getActivity());
			df.setArguments(args);
			df.show(getFragmentManager(), "Conversation Details");
		}
	}

	public ConversationFragment() { super(); }
}

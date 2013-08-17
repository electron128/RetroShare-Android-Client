/**
 * @license
 *
 * Copyright (c) 2013 Gioacchino Mazzurco <gio@eigenlab.org>
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


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.google.protobuf.InvalidProtocolBufferException;

import rsctrl.chat.Chat;
import rsctrl.core.Core;

import org.retroshare.android.RsCtrlService.RsMessage;
import org.retroshare.android.utils.WeakHashSet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rsctrl.chat.Chat.ChatMessage;

public class RsConversationService implements RsServiceInterface, RsCtrlService.RsCtrlServiceListener
{
	public RsConversationService(RsCtrlService rsCtrlService, HandlerThreadInterface handlerThreadInterface, Context context)
	{
		mRsCtrlService = rsCtrlService;
		mRsPeerService = rsCtrlService.mRsPeersService;
		mHandlerThreadInterface = handlerThreadInterface;
		mContext = context;
		mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
	}

	/**
	 * Conversation stuff
	 */
	public static enum ConversationKind { PGP_CHAT };
	public static interface ConversationId extends Serializable
	{
		public ConversationKind getConversationKind();
	}
	public static interface ConversationMessage
	{
		public ConversationId getConversationId();
	}
	private final Map<ConversationId, ArrayList<ConversationMessage>> conversationHistoryMap = new HashMap<ConversationId, ArrayList<ConversationMessage>>();
	private void appendConversationMessageToHistoryMap(ConversationMessage msg)
	{
		ConversationId id = msg.getConversationId();
		ArrayList conversationHistory = conversationHistoryMap.get(id);
		if(conversationHistory == null)
		{
			conversationHistory = new ArrayList<ConversationMessage>();
			conversationHistoryMap.put(id,conversationHistory);
		}
		conversationHistory.add(msg);
		notifyRsConversationServiceListeners();
	}
	public List<ConversationMessage> getConversationHistory(ConversationId id)
	{
		List<ConversationMessage> history = new ArrayList<ConversationMessage>();
		history.addAll(conversationHistoryMap.get(id));
		return history;
	}
	public void sendConversationMessage(ConversationMessage msg)
	{
		switch (msg.getConversationId().getConversationKind())
		{
			case PGP_CHAT:
				sendPgpChatMessage((PgpChatMessage)msg);
				break;
		}
	}

	/**
	 * RsConversationService Listeners Stuff
	 */
	public static interface RsConversationServiceListener
	{
		public void onConversationsUpdate(); // Add some data passing via some ConversationEvent or stuff like that to make room to more optimization ( for example we can pass just new messages instead of reload the entire list )
	}
	private final class RsConversationServiceUpdateListenerRunnable implements Runnable	{ @Override public void run() { for(RsConversationServiceListener listener : mRsConversationServiceListenersSet) listener.onConversationsUpdate(); } }
	private final Set<RsConversationServiceListener> mRsConversationServiceListenersSet = new WeakHashSet<RsConversationServiceListener>();
	public void registerRsConversationServiceListener(RsConversationServiceListener listener) { mRsConversationServiceListenersSet.add(listener); }
	public void unregisterRsConversationServiceListener(RsConversationServiceListener listener) { mRsConversationServiceListenersSet.remove(listener); }
	private void notifyRsConversationServiceListeners() { mHandlerThreadInterface.postToHandlerThread(new RsConversationServiceUpdateListenerRunnable()); }

	/**
	 * Notification Stuff
	 */
	private final NotificationManager mNotificationManager;
	private final Set<ConversationId> notificationDisabledConversation = new HashSet<ConversationId>();
	public void cancelNotificationForConversation(ConversationId id) { mNotificationManager.cancel(id.hashCode());}
	public void disableNotificationForConversation(ConversationId id) { notificationDisabledConversation.add(id); }
	public void enableNotificationForConversation(ConversationId id) { notificationDisabledConversation.remove(id); }
	private boolean notificationForConversationEnabled(ConversationId id) { return ! notificationDisabledConversation.contains(id); }
	private void notifyAndroidAboutConversation(ConversationId id, int iconId, String title, String message, Intent action, boolean autoCancel)
	{
		if(notificationForConversationEnabled(id))
		{
			Notification notification = new NotificationCompat.Builder(mContext)
					.setSmallIcon(iconId)
					.setContentTitle(title)
					.setContentText(message)
					.setContentIntent(PendingIntent.getActivity(mContext, 0, action, 0))
					.setAutoCancel(autoCancel)
					.build();
			mNotificationManager.notify(id.hashCode(), notification);
		}
	}

	@Override /** Implements RsCtrlServiceListener */
	public void onConnectionStateChanged(RsCtrlService.ConnectionEvent ce)
	{
		/** If connected to the rs-core register to Chat Events */
		if(ce.kind == RsCtrlService.ConnectionEventKind.CONNECTED)
		{
			Chat.RequestRegisterEvents.Builder reqb = Chat.RequestRegisterEvents.newBuilder();
			reqb.setAction(Chat.RequestRegisterEvents.RegisterAction.REGISTER);

			RsMessage msg = new RsMessage();
			msg.msgId = RsCtrlService.constructMsgId(Core.ExtensionId.CORE_VALUE, Core.PackageId.CHAT_VALUE, Chat.RequestMsgIds.MsgId_RequestRegisterEvents_VALUE, false);
			msg.body = reqb.build().toByteArray();
			mRsCtrlService.sendMsg(msg);
		}
	}

	@Override /** Implements RsServiceInterface */
	public void handleMessage(RsCtrlService.RsMessage msg)
	{
		if( msg.msgId == RsCtrlService.constructMsgId(Core.ExtensionId.CORE_VALUE, Core.PackageId.CHAT_VALUE, Chat.ResponseMsgIds.MsgId_EventChatMessage_VALUE, false))
		{
			Chat.EventChatMessage resp;
			try { resp = Chat.EventChatMessage.parseFrom(msg.body); }
			catch (InvalidProtocolBufferException e) { return; }

			Chat.ChatMessage cMsg = resp.getMsg();

			switch (cMsg.getId().getChatType())
			{
				case TYPE_PRIVATE:
					PgpChatId pgpId = new PgpChatId(mRsPeerService.getPersonBySslId(cMsg.getId().getChatId()).getGpgId());
					appendPgpChatMessageToHistory(new PgpChatMessage(pgpId, ChatMessage.newBuilder(cMsg).setRecvTime((int) (System.currentTimeMillis() / 1000L)).build()));
					break;
			}
		}
	}


	/**
	 * PGP_CHAT stuff
	 */
	public static final class PgpChatId implements ConversationId
	{
		public PgpChatId(String destPgpId) { mPgpId = destPgpId; }
		public String getDestPgpId() { return mPgpId; }

		@Override public ConversationKind getConversationKind(){ return ConversationKind.PGP_CHAT; }

		private final String mPgpId;
	}
	public static class PgpChatMessage implements ConversationMessage
	{
		public PgpChatMessage(PgpChatId id, String message)
		{
			mPgpChatId = id;
			chatMessage = ChatMessage.newBuilder().setMsg(message).buildPartial();
		}
		public PgpChatMessage(PgpChatId id, ChatMessage cMsg)
		{
			mPgpChatId = id;
			chatMessage = cMsg;
		}

		@Override public PgpChatId getConversationId() { return mPgpChatId; }
		public ChatMessage getData() { return chatMessage; }

		private final PgpChatId mPgpChatId;
		private final ChatMessage chatMessage;
	}
	private void appendPgpChatMessageToHistory(PgpChatMessage msg)
	{
		PgpChatId pId = msg.getConversationId();
		appendConversationMessageToHistoryMap(msg);
		if(notificationForConversationEnabled(pId))
		{
			Intent i = new Intent(mContext, ConversationFragmentActivity.class)
					.putExtra(ConversationFragmentActivity.SERVER_NAME_EXTRA, mRsCtrlService.getServerData().name)
					.putExtra(ConversationFragmentActivity.CONVERSATION_ID_EXTRA, pId);
			ChatMessage cMsg = msg.getData();
			notifyAndroidAboutConversation(msg.getConversationId(), R.drawable.chat_bubble, cMsg.getPeerNickname(), cMsg.getMsg(), i, false );
		}
	}
	private void sendPgpChatMessage(PgpChatMessage msg)
	{
		RsMessage rsMessage = new RsMessage();
		rsMessage.msgId = RsCtrlService.constructMsgId(Core.ExtensionId.CORE_VALUE,Core.PackageId.CHAT_VALUE, Chat.RequestMsgIds.MsgId_RequestSendMessage_VALUE, false);
		ChatMessage.Builder mb = ChatMessage.newBuilder(msg.getData())
				.setSendTime((int)(System.currentTimeMillis()/1000L))
				.setPeerNickname(mRsPeerService.getOwnPerson().getName());
		Chat.ChatId.Builder cId = Chat.ChatId.newBuilder().setChatType(Chat.ChatType.TYPE_PRIVATE);

		for(Core.Location l : mRsPeerService.getPersonByPgpId(msg.mPgpChatId.getDestPgpId()).getLocationsList())
		{
			cId.setChatId(l.getSslId());
			mb.setId(cId.build());
			rsMessage.body = mb.build().toByteArray();

			mRsCtrlService.sendMsg(rsMessage);
		}

		appendConversationMessageToHistoryMap(new PgpChatMessage(msg.getConversationId(), mb.build()));
	}

	private final HandlerThreadInterface mHandlerThreadInterface;
	private final RsCtrlService mRsCtrlService;
	private final RsPeersService mRsPeerService;
	private final Context mContext;
}

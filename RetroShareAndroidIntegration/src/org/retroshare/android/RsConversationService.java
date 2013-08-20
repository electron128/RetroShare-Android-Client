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
import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;

import rsctrl.chat.Chat;
import rsctrl.chat.Chat.RequestSendMessage;
import rsctrl.chat.Chat.ChatMessage;
import rsctrl.chat.Chat.ChatId;
import rsctrl.chat.Chat.ChatLobbyInfo;
import rsctrl.chat.Chat.ResponseChatLobbies;
import rsctrl.chat.Chat.RequestChatLobbies;
import rsctrl.core.Core;

import org.retroshare.android.RsCtrlService.RsMessage;
//import org.retroshare.android.utils.WeakHashSet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;


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
	public String TAG() { return "RsConversationService"; }

	/**
	 * Conversation stuff
	 */
	public static enum ConversationKind { PGP_CHAT, LOBBY_CHAT };
	public static interface ConversationId extends Serializable
	{
		public ConversationKind getConversationKind();

		public static class Factory
		{
			public static ConversationId getConversationId(Serializable s) throws ClassCastException
			{
				ConversationId cId = (ConversationId) s;

				switch (cId.getConversationKind())
				{
					case PGP_CHAT:
						return PgpChatId.Factory.getPgpChatId(s);
					case LOBBY_CHAT:
						return LobbyChatId.Factory.getLobbyChatId(s);
				}

				throw new ClassCastException("Unknown Conversation Kind");
			}
		}
	}
	public static interface ConversationMessage
	{
		public ConversationId getConversationId();
		public String getAuthorString();
		public void setAuthorString(String author);
		public boolean hasAuthorString();
		public String getMessageString();
		public void setMessageString(String message);
		public boolean hasMessageString();
		public String getDefaultTimeFormat();
		public long getTime();
		public void setTime(long time);
		public boolean hasTime();

		public static final class Factory
		{
			public static ConversationMessage newConversationMessage(ConversationId id)
			{
				switch (id.getConversationKind())
				{
					case PGP_CHAT:
						return new PgpChatMessage((PgpChatId)id);
					case LOBBY_CHAT:
						return new LobbyChatMessage((LobbyChatId)id);
					default :
						return null;
				}
			}
		}
	}
	private final Map<ConversationId, ArrayList<ConversationMessage>> conversationHistoryMap = new HashMap<ConversationId, ArrayList<ConversationMessage>>();
	private void appendConversationMessageToHistoryMap(ConversationMessage msg)
	{
		ConversationId id = msg.getConversationId();
		ArrayList conversationHistory = conversationHistoryMap.get(id);
		if(conversationHistory == null)
		{
			conversationHistory = new ArrayList<ConversationMessage>();
			conversationHistoryMap.put(id, conversationHistory);
		}
		conversationHistory.add(msg);
//		Log.wtf(TAG(), "conversationHistoryMap.size() = " + String.valueOf(conversationHistoryMap.size()) + " conversationHistory.size() = " + String.valueOf(conversationHistory.size()) );
		notifyRsConversationServiceListeners();
	}
	public List<ConversationMessage> getConversationHistory(ConversationId id)
	{
		List<ConversationMessage> ret = new ArrayList<ConversationMessage>();
		List<ConversationMessage> history = conversationHistoryMap.get(id);
		if(history != null) ret.addAll(history);
		return ret;
	}
	public void sendConversationMessage(ConversationMessage msg)
	{
		switch (msg.getConversationId().getConversationKind())
		{
			case PGP_CHAT:
				sendPgpChatMessage((PgpChatMessage)msg);
				break;
			case LOBBY_CHAT:
				sendLobbyChatMessage((LobbyChatMessage)msg);
				break;
		}
	}
	public void joinConversation(ConversationId id)
	{
		switch (id.getConversationKind())
		{
			case LOBBY_CHAT:
			{
				LobbyChatId lobbyChatId = (LobbyChatId) id;

				RsMessage msg = new RsMessage();
				msg.msgId = RsCtrlService.constructMsgId(Core.ExtensionId.CORE_VALUE, Core.PackageId.CHAT_VALUE, Chat.RequestMsgIds.MsgId_RequestJoinOrLeaveLobby_VALUE, false);

				msg.body = Chat.RequestJoinOrLeaveLobby.newBuilder()
						.setLobbyId(lobbyChatId.getChatId().getChatId())
						.setAction(Chat.RequestJoinOrLeaveLobby.LobbyAction.JOIN_OR_ACCEPT)
						.build()
						.toByteArray();

				mRsCtrlService.sendMsg(msg);
				break;
			}
		}
	}
	public void leaveConversation(ConversationId id)
	{
		switch (id.getConversationKind())
		{
			case LOBBY_CHAT:
			{
				LobbyChatId lobbyChatId = (LobbyChatId) id;

				RsMessage msg = new RsMessage();
				msg.msgId = RsCtrlService.constructMsgId(Core.ExtensionId.CORE_VALUE, Core.PackageId.CHAT_VALUE, Chat.RequestMsgIds.MsgId_RequestJoinOrLeaveLobby_VALUE, false);

				msg.body = Chat.RequestJoinOrLeaveLobby.newBuilder()
						.setLobbyId(lobbyChatId.getChatId().getChatId())
						.setAction(Chat.RequestJoinOrLeaveLobby.LobbyAction.LEAVE_OR_DENY)
						.build()
						.toByteArray();

				mRsCtrlService.sendMsg(msg);
				break;
			}
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
	private final Set<RsConversationServiceListener> mRsConversationServiceListenersSet = new HashSet<RsConversationServiceListener>();
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

			requestLobbiesListUpdate();
		}
	}

	@Override /** Implements RsServiceInterface */
	public void handleMessage(RsCtrlService.RsMessage msg)
	{
//		Log.wtf(TAG(), "Parsing message with id " + String.valueOf(msg.msgId));

		if( msg.msgId == RsCtrlService.constructMsgId(Core.ExtensionId.CORE_VALUE, Core.PackageId.CHAT_VALUE, Chat.ResponseMsgIds.MsgId_ResponseChatLobbies_VALUE, true))
		{
//			Log.wtf(TAG(), "Parsing lobbies message reponse 1");

			ResponseChatLobbies resp;
			try { resp = Chat.ResponseChatLobbies.parseFrom(msg.body); }
			catch (InvalidProtocolBufferException e) {return;}

//			Log.wtf(TAG(), "Parsing lobbies message reponse 2");

			if(resp.getLobbiesCount() > 0)
			{
//				Log.wtf(TAG(), "Parsing lobbies message reponse 3");
				lobbiesRepository.clear();
				for (ChatLobbyInfo lobby : resp.getLobbiesList()) lobbiesRepository.put(LobbyChatId.Factory.getLobbyChatId(lobby.getLobbyId()), lobby);
			}

//			Log.wtf(TAG(), "Parsing lobbies message reponse 4");
			notifyRsConversationServiceListeners();

			return;
		}

		if( msg.msgId == RsCtrlService.constructMsgId(Core.ExtensionId.CORE_VALUE, Core.PackageId.CHAT_VALUE, Chat.ResponseMsgIds.MsgId_EventChatMessage_VALUE, true))
		{
			Chat.EventChatMessage resp;
			try { resp = Chat.EventChatMessage.parseFrom(msg.body); }
			catch (InvalidProtocolBufferException e) { return; }

			ChatMessage cMsg = resp.getMsg();

			switch (cMsg.getId().getChatType())
			{
				case TYPE_PRIVATE:
				{
					String locationId = cMsg.getId().getChatId();
					Core.Person author = mRsPeerService.getPersonBySslId(locationId);
					String authorNick = author.getName();
					if(authorNick.equals(mRsPeerService.getOwnPerson().getName())) for(Core.Location location : author.getLocationsList()) if (location.getSslId().equals(locationId)) authorNick+=(" (" + location.getLocation() + ")"); // Append location name to nick if the author has the same nick of the user
					String sPgpId = author.getGpgId();
					PgpChatId pgpId = PgpChatId.Factory.getPgpChatId(sPgpId);
					ChatMessage dataStoreMsg = ChatMessage
							.newBuilder(cMsg)
							.setPeerNickname(authorNick)
							.setRecvTime((int) (System.currentTimeMillis() / 1000L))
							.build();
					appendPgpChatMessageToHistory(new PgpChatMessage(pgpId, dataStoreMsg));
					break;
				}
				case TYPE_LOBBY:
				{
					ChatMessage dataStoreMsg = ChatMessage
							.newBuilder(cMsg)
							.setRecvTime((int) (System.currentTimeMillis() / 1000L))
							.build();
					appendLobbyChatMessageToHistory(new LobbyChatMessage(LobbyChatId.Factory.getLobbyChatId(dataStoreMsg.getId().getChatId()), dataStoreMsg));
					break;
				}
			}
			return;
		}
	}

	/**
	 * PGP_CHAT stuff
	 */
	public static final class PgpChatId implements ConversationId
	{
		public String getDestPgpId() { return mPgpId; }
		@Override public ConversationKind getConversationKind(){ return ConversationKind.PGP_CHAT; }

		@Override public boolean equals(Object o)
		{
			if(o == null) return false;
			PgpChatId c1;
			try { c1 = (PgpChatId) o; }
			catch (ClassCastException e) { return false; }

			return mPgpId.equalsIgnoreCase(c1.getDestPgpId());
		}

		@Override public int hashCode() { return mPgpId.hashCode(); }

		public static final class Factory
		{
			public static PgpChatId getPgpChatId(String destinationPgpId)
			{
				PgpChatId ret = repository.get(destinationPgpId);
				if(ret == null)
				{
					ret = new PgpChatId(destinationPgpId);
					repository.put(destinationPgpId, ret);
				}
				return ret;
			}

			public static PgpChatId getPgpChatId(Serializable s) throws ClassCastException
			{
				PgpChatId param = (PgpChatId) s;
				String pgpId = param.getDestPgpId();
				PgpChatId ret = repository.get(pgpId);
				if(ret == null)
				{
					ret = param;
					repository.put(pgpId, ret);
				}
				return ret;
			}

			private static final Map<String, PgpChatId> repository = new WeakHashMap<String, PgpChatId>();
		}

		private PgpChatId(String destPgpId) { mPgpId = destPgpId; }
		private final String mPgpId;
	}
	public static class PgpChatMessage implements ConversationMessage
	{
		public PgpChatMessage(PgpChatId id)
		{
			mPgpChatId = id;
			mChatMessage = ChatMessage.newBuilder().buildPartial();
		}
		public PgpChatMessage(PgpChatId id, ChatMessage cMsg)
		{
			mPgpChatId = id;
			mChatMessage = cMsg;
		}

		@Override public PgpChatId getConversationId() { return mPgpChatId; }
		@Override public String getAuthorString() { return mChatMessage.getPeerNickname(); }
		@Override public void setAuthorString(String author) { mChatMessage = ChatMessage.newBuilder(mChatMessage).setPeerNickname(author).buildPartial(); }
		@Override public boolean hasAuthorString() { return mChatMessage.hasPeerNickname(); }
		@Override public String getMessageString() { return mChatMessage.getMsg(); }
		@Override public void setMessageString(String message) { mChatMessage = ChatMessage.newBuilder(mChatMessage).setMsg(message).buildPartial(); }
		@Override public boolean hasMessageString() { return mChatMessage.hasMsg(); }
		@Override public String getDefaultTimeFormat() { return "HH:mm:ss"; }
		@Override public boolean hasTime() { return (mChatMessage.hasSendTime() || mChatMessage.hasRecvTime()); }
		@Override public void setTime(long time) { mChatMessage = ChatMessage.newBuilder().setSendTime((int)(time/1000L)).buildPartial(); }
		@Override public long getTime()
		{
			if(mChatMessage.hasSendTime()) return (mChatMessage.getSendTime() * 1000L);
			return (mChatMessage.getRecvTime()*1000L);
		}

		ChatMessage getRawData() { return mChatMessage; }

		private final PgpChatId mPgpChatId;
		private ChatMessage mChatMessage;
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
			ChatMessage cMsg = msg.getRawData();
			notifyAndroidAboutConversation(msg.getConversationId(), R.drawable.chat_bubble, cMsg.getPeerNickname(), cMsg.getMsg(), i, false );
		}
	}
	private void sendPgpChatMessage(PgpChatMessage msg)
	{
		RsMessage rsMessage = new RsMessage();
		rsMessage.msgId = RsCtrlService.constructMsgId(Core.ExtensionId.CORE_VALUE,Core.PackageId.CHAT_VALUE, Chat.RequestMsgIds.MsgId_RequestSendMessage_VALUE, false);

		RequestSendMessage.Builder builderRequestSendMessage = RequestSendMessage.newBuilder();

		ChatMessage.Builder builderChatMessage = ChatMessage.newBuilder(msg.getRawData())
				.setSendTime((int) (System.currentTimeMillis() / 1000L))
				.setPeerNickname(mRsPeerService.getOwnPerson().getName());

		ChatId.Builder builderChatId = ChatId.newBuilder().setChatType(Chat.ChatType.TYPE_PRIVATE);

		for(Core.Location location : mRsPeerService.getPersonByPgpId(msg.mPgpChatId.getDestPgpId()).getLocationsList())
		{
			ChatId chatId = builderChatId.setChatId(location.getSslId()).build();
			ChatMessage chatMessage = builderChatMessage.setId(chatId).build();
			RequestSendMessage requestSendMessage = builderRequestSendMessage.setMsg(chatMessage).build();

			rsMessage.body = requestSendMessage.toByteArray();

			Log.wtf(TAG(), "Sending \"" + requestSendMessage.getMsg().getMsg() + "\" to " + location.getLocation() + " <" + requestSendMessage.getMsg().getId().getChatId() + ">" );
			mRsCtrlService.sendMsg(rsMessage, new ResponseSendMessageHandler());
		}

		appendConversationMessageToHistoryMap(new PgpChatMessage(msg.getConversationId(), builderChatMessage.build()));
	}

	/**
	 * LOBBY_CHAT stuff
	 */
	public static final class LobbyChatId implements ConversationId
	{
		@Override public ConversationKind getConversationKind() { return ConversationKind.LOBBY_CHAT; }
		@Override public boolean equals(Object o)
		{
			LobbyChatId c1;
			try { c1 = (LobbyChatId) o; }
			catch (ClassCastException e) { return false; }

			return ( mChatId.getChatId().equalsIgnoreCase(c1.getChatId().getChatId()) && (c1.getChatId().getChatType() == Chat.ChatType.TYPE_LOBBY));
		}
		@Override public int hashCode() { return mChatId.getChatId().hashCode(); }
		public ChatId getChatId(){ return mChatId; }

		public final static class Factory
		{
			public static LobbyChatId getLobbyChatId(String lobbyIdString)
			{
				LobbyChatId ret = repository.get(lobbyIdString);
				if(ret == null)
				{
					ret = new LobbyChatId(lobbyIdString);
					repository.put(lobbyIdString, ret);
				}
				return ret;
			}

			public static LobbyChatId getLobbyChatId(Serializable s) throws ClassCastException
			{
				LobbyChatId param = (LobbyChatId) s;
				String lobbyId = param.getChatId().getChatId();
				LobbyChatId ret = repository.get(lobbyId);
				if(ret == null)
				{
					ret = param;
					repository.put(lobbyId, ret);
				}
				return ret;
			}

			private static final Map<String, LobbyChatId> repository = new WeakHashMap<String, LobbyChatId>();
		}

		private LobbyChatId(String lobbyIdString) { mChatId = ChatId.newBuilder().setChatType(Chat.ChatType.TYPE_LOBBY).setChatId(lobbyIdString).build(); }
		private final ChatId mChatId;
	}
	public static final class LobbyChatMessage implements ConversationMessage
	{
		public LobbyChatMessage(LobbyChatId chatId) { mLobbyChatId = chatId; mChatMessage = ChatMessage.newBuilder().buildPartial(); }
		public LobbyChatMessage(LobbyChatId chatId, ChatMessage msg) { mLobbyChatId = chatId; mChatMessage = ChatMessage.newBuilder(msg).buildPartial(); }

		@Override public LobbyChatId getConversationId() { return mLobbyChatId; }
		@Override public String getAuthorString() { return mChatMessage.getPeerNickname(); }
		@Override public void setAuthorString(String author) { mChatMessage = ChatMessage.newBuilder(mChatMessage).setPeerNickname(author).buildPartial(); }
		@Override public boolean hasAuthorString() { return mChatMessage.hasPeerNickname(); }
		@Override public String getMessageString() { return mChatMessage.getMsg(); }
		@Override public void setMessageString(String message) { mChatMessage = ChatMessage.newBuilder(mChatMessage).setMsg(message).buildPartial(); }
		@Override public boolean hasMessageString() { return mChatMessage.hasMsg(); }
		@Override public String getDefaultTimeFormat() { return "HH:mm:ss"; }
		@Override public boolean hasTime() { return (mChatMessage.hasSendTime() || mChatMessage.hasRecvTime()); }
		@Override public void setTime(long time) { mChatMessage = ChatMessage.newBuilder().setSendTime((int)(time/1000L)).buildPartial(); }
		@Override public long getTime()
		{
			if(mChatMessage.hasSendTime()) return (mChatMessage.getSendTime() * 1000L);
			return (mChatMessage.getRecvTime()*1000L);
		}

		public ChatMessage getRawData() { return mChatMessage; };

		private final LobbyChatId mLobbyChatId;
		private ChatMessage mChatMessage;
	}
	public void requestLobbiesListUpdate()
	{
		RsMessage rsMessage = new RsMessage();
		rsMessage.msgId = RsCtrlService.constructMsgId(Core.ExtensionId.CORE_VALUE, Core.PackageId.CHAT_VALUE, Chat.RequestMsgIds.MsgId_RequestChatLobbies_VALUE, false);
		rsMessage.body = RequestChatLobbies.newBuilder()
				.setLobbySet(RequestChatLobbies.LobbySet.LOBBYSET_ALL)
				.build()
				.toByteArray();

		mRsCtrlService.sendMsg(rsMessage);
	}
	public List<ChatLobbyInfo> getLobbiesList() { return new ArrayList<ChatLobbyInfo>(lobbiesRepository.values()); }
	public ChatLobbyInfo getLobbyInfo(LobbyChatId id) { return lobbiesRepository.get(id); }
	private void appendLobbyChatMessageToHistory(LobbyChatMessage msg)
	{
		LobbyChatId pId = msg.getConversationId();
		appendConversationMessageToHistoryMap(msg);
		if(notificationForConversationEnabled(pId))
		{
			Intent i = new Intent(mContext, ConversationFragmentActivity.class)
					.putExtra(ConversationFragmentActivity.SERVER_NAME_EXTRA, mRsCtrlService.getServerData().name)
					.putExtra(ConversationFragmentActivity.CONVERSATION_ID_EXTRA, pId);
			ChatMessage cMsg = msg.getRawData();
			notifyAndroidAboutConversation(msg.getConversationId(), R.drawable.chat_bubble, cMsg.getPeerNickname(), cMsg.getMsg(), i, false );
		}
	}
	private void sendLobbyChatMessage(LobbyChatMessage msg)
	{
		RsMessage rsMessage = new RsMessage();
		rsMessage.msgId = RsCtrlService.constructMsgId(Core.ExtensionId.CORE_VALUE, Core.PackageId.CHAT_VALUE, Chat.RequestMsgIds.MsgId_RequestSendMessage_VALUE, false);

		ChatMessage chatMessage = ChatMessage.newBuilder(msg.getRawData())
				.setSendTime((int)(System.currentTimeMillis()/1000L))
				.setPeerNickname(mRsPeerService.getOwnPerson().getName())
				.setId(msg.getConversationId().getChatId())
				.build();

		RequestSendMessage requestSendMessage = RequestSendMessage.newBuilder()
				.setMsg(chatMessage)
				.build();

		rsMessage.body = requestSendMessage.toByteArray();

		mRsCtrlService.sendMsg(rsMessage, new ResponseSendMessageHandler());

		appendConversationMessageToHistoryMap(new LobbyChatMessage(msg.getConversationId(), chatMessage));
	}
	private final Map<LobbyChatId, Chat.ChatLobbyInfo> lobbiesRepository = new HashMap<LobbyChatId, ChatLobbyInfo>();

	/**
	 * Internal stuff
	 */
	private final HandlerThreadInterface mHandlerThreadInterface;
	private final RsCtrlService mRsCtrlService;
	private final RsPeersService mRsPeerService;
	private final Context mContext;

	private static final class ResponseSendMessageHandler extends RsMessageHandler
	{
		private static final String TAG = "ResponseSendMessageHandler";
		@Override public void rsHandleMsg(RsMessage msg)
		{
			Chat.ResponseSendMessage resp;
			try { resp = Chat.ResponseSendMessage.parseFrom(msg.body); }
			catch (InvalidProtocolBufferException e)
			{
				Log.e(TAG, "Received invalid ResponseSendMessage");
				return;
			}

			Log.wtf(TAG, "Message sending ended with status code : " + String.valueOf(resp.getStatus().getCode().getNumber()) + "  and message: " + resp.getStatus().getMsg());
		}
	}
}

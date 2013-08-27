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
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.NotificationCompat;
import android.text.Html;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;


public class RsConversationService implements RsServiceInterface, RsCtrlService.RsCtrlServiceListener
{
	public String TAG() { return "RsConversationService"; }
	public RsConversationService(RsCtrlService rsCtrlService, HandlerThreadInterface handlerThreadInterface, Context context)
	{
		mRsCtrlService = rsCtrlService;
		mRsCtrlService.registerListener(this);
		mRsPeerService = rsCtrlService.mRsPeersService;
		mHandlerThreadInterface = handlerThreadInterface;
		mContext = context;
		mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
	}

	/**
	 * Conversation stuff
	 */
	public static enum ConversationKind	{ PGP_CHAT, LOBBY_CHAT }
	public static abstract class ConversationId implements Parcelable
	{
		abstract public ConversationKind getConversationKind();
		@Override public int describeContents() { return getConversationKind().ordinal(); }
	}
	public static abstract class ConversationInfo implements Parcelable
	{
		@Override public int describeContents() { return getConversationId().getConversationKind().ordinal(); }
		abstract public ConversationId getConversationId();
		abstract public boolean hasTitle();
		abstract public CharSequence getTitle();
		abstract public boolean hasTopic();
		abstract public CharSequence getTopic();
		abstract public int getParticipantsCount();
		abstract public List<CharSequence> getParticipantsNick();
		abstract public boolean isPrivate();
	}
	public static enum ConversationMessageStatus { UNREAD, READ }
	public static abstract class ConversationMessage implements Comparable<ConversationMessage>
	{
		abstract public ConversationId getConversationId();
		abstract public String getAuthorString();
		abstract public void setAuthorString(String author);
		abstract public boolean hasAuthorString();
		abstract public String getMessageString();
		abstract public void setMessageString(String message);
		abstract public boolean hasMessageString();
		abstract public String getDefaultTimeFormat();
		abstract public long getTime();
		abstract public void setTime(long time);
		abstract public boolean hasTime();
		abstract public boolean hasStatus();
		abstract public ConversationMessageStatus getStatus();
		abstract public void setStatus(ConversationMessageStatus status);

		@Override public int compareTo(ConversationMessage msg) { return (int)(getTime() - msg.getTime()); }

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
	private final Map<ConversationId, Collection<ConversationMessage>> conversationHistoryMap = new HashMap<ConversationId, Collection<ConversationMessage>>();
	public List<ConversationMessage> getConversationHistory(ConversationId id)
	{
		List<ConversationMessage> ret = new ArrayList<ConversationMessage>();
		Collection<ConversationMessage> history = conversationHistoryMap.get(id);
		if(history != null) ret.addAll(history);
		return ret;
	}
	public ConversationInfo getConversationInfo(ConversationId id)
	{
		switch (id.getConversationKind())
		{
			case PGP_CHAT:
				return new PgpChatInfo((PgpChatId)id);
			case LOBBY_CHAT:
				return new LobbyChatInfo((LobbyChatId)id);
			default:
				throw new ClassCastException("Attempted getConversationInfo(id) for ConversationId with unknown ConversationKind");
		}
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
	public static enum ConversationEventKind { UNSPECIFIED, LOBBY_LIST_UPDATE, NEW_CONVERSATION_MESSAGE }
	public static interface ConversationEvent
	{
		public ConversationEventKind getEventKind();
	}
	public static final class NewMessageConversationEvent implements ConversationEvent
	{
		@Override public ConversationEventKind getEventKind() { return ConversationEventKind.NEW_CONVERSATION_MESSAGE; }
		public NewMessageConversationEvent(ConversationMessage msg) { conversationMessage = msg; }
		public ConversationMessage getConversationMessage() { return conversationMessage; }
		private final ConversationMessage conversationMessage;
	}
	public static final class LobbyListUpdateConversationEvent implements ConversationEvent
	{
		@Override public ConversationEventKind getEventKind() { return ConversationEventKind.LOBBY_LIST_UPDATE; }
	}
	public static interface RsConversationServiceListener
	{
		public void onConversationsEvent(ConversationEvent event);
	}
	private final class RsConversationServiceUpdateListenerRunnable implements Runnable
	{
		public RsConversationServiceUpdateListenerRunnable(ConversationEvent event) { conversationEvent = event; }
		@Override public void run() { for(RsConversationServiceListener listener : mRsConversationServiceListenersSet) listener.onConversationsEvent(conversationEvent); }
		private final ConversationEvent conversationEvent;
	}
	private final Set<RsConversationServiceListener> mRsConversationServiceListenersSet = new HashSet<RsConversationServiceListener>();
	public void registerRsConversationServiceListener(RsConversationServiceListener listener) { mRsConversationServiceListenersSet.add(listener); }
	public void unregisterRsConversationServiceListener(RsConversationServiceListener listener) { mRsConversationServiceListenersSet.remove(listener); }
	private void notifyRsConversationServiceListeners(ConversationEvent event) { mHandlerThreadInterface.postToHandlerThread(new RsConversationServiceUpdateListenerRunnable(event)); }

	/**
	 * Android notification Stuff
	 */
	private final NotificationManager mNotificationManager;
	private final Set<ConversationId> notificationDisabledConversation = new HashSet<ConversationId>();
	public void cancelNotificationForConversation(ConversationId id) { mNotificationManager.cancel(id.hashCode());}
	public void disableNotificationForConversation(ConversationId id) { notificationDisabledConversation.add(id); }
	public void enableNotificationForConversation(ConversationId id) { notificationDisabledConversation.remove(id); }
	private boolean notificationForConversationEnabled(ConversationId id) { return ! notificationDisabledConversation.contains(id); }
	private void notifyAndroidAboutConversation(ConversationId id, int iconId, CharSequence title, CharSequence message, Intent action, boolean autoCancel)
	{
		if(notificationForConversationEnabled(id))
		{
			action.putExtra(ProxiedFragmentActivityBase.SERVER_NAME_EXTRA, mRsCtrlService.getServerData().name);
			action.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			action.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			Notification notification = new NotificationCompat.Builder(mContext)
					.setSmallIcon(iconId)
					.setContentTitle(title)
					.setContentText(message)
					.setContentIntent(PendingIntent.getActivity(mContext, 0, action, PendingIntent.FLAG_UPDATE_CURRENT))
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
				notifyRsConversationServiceListeners(new LobbyListUpdateConversationEvent());
			}

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
					PgpChatMessage pgpChatMessage = new PgpChatMessage(pgpId, dataStoreMsg);
					appendPgpChatMessageToHistory(pgpChatMessage);
					notifyAndroidAboutPgpChatMsg(pgpChatMessage);
					break;
				}
				case TYPE_LOBBY:
				{
					ChatMessage dataStoreMsg = ChatMessage
							.newBuilder(cMsg)
							.setRecvTime((int) (System.currentTimeMillis() / 1000L))
							.build();
					LobbyChatMessage lobbyChatMessage = new LobbyChatMessage(LobbyChatId.Factory.getLobbyChatId(dataStoreMsg.getId().getChatId()), dataStoreMsg);
					appendLobbyChatMessageToHistory(lobbyChatMessage);
					notifyAndroidAboutLobbyChatMessage(lobbyChatMessage);
					break;
				}
			}
			return;
		}
	}

	/**
	 * PGP_CHAT stuff
	 */
	public static final class PgpChatId extends ConversationId
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
		@Override public void writeToParcel(Parcel parcel, int i) { parcel.writeString(mPgpId); }

		public static final Parcelable.Creator<PgpChatId> CREATOR = new Parcelable.Creator<PgpChatId>()
		{
			public PgpChatId createFromParcel(Parcel in) { return PgpChatId.Factory.getPgpChatId(in.readString()); }
			public PgpChatId[] newArray(int size) { return new PgpChatId[size]; }
		};

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
			private static final Map<String, PgpChatId> repository = new WeakHashMap<String, PgpChatId>();
		}

		private PgpChatId(String destPgpId) { mPgpId = destPgpId; }
		private final String mPgpId;
	}
	public final class PgpChatInfo extends ConversationInfo
	{
		public PgpChatInfo(PgpChatId id)
		{
			nicks.add(mRsPeerService.getOwnPerson().getName());
			nicks.add(mRsPeerService.getPersonByPgpId(id.getDestPgpId()).getName());
			pgpChatId = id;
		};

		@Override public ConversationId getConversationId() { return pgpChatId; }
		@Override public int getParticipantsCount() { return 2; }
		@Override public List<CharSequence> getParticipantsNick() { return new ArrayList<CharSequence>(nicks); }
		@Override public boolean hasTitle() { return false; }
		@Override public CharSequence getTitle() { return ""; }
		@Override public boolean hasTopic() { return false; }
		@Override public CharSequence getTopic() { return ""; }
		@Override public boolean isPrivate() { return true; }

		@Override public void writeToParcel(Parcel parcel, int i) { parcel.writeString(pgpChatId.getDestPgpId()); }

		public final PgpChatInfo.Creator<PgpChatInfo> CREATOR = new Parcelable.Creator<PgpChatInfo>()
		{
			public PgpChatInfo createFromParcel(Parcel in) { return new PgpChatInfo(PgpChatId.Factory.getPgpChatId(in.readString())); }
			public PgpChatInfo[] newArray(int size) { return new PgpChatInfo[size]; }
		};

		private final PgpChatId pgpChatId;
		private final List<String> nicks = new ArrayList<String>();
	}
	public static class PgpChatMessage extends ConversationMessage
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
		@Override public boolean hasStatus() { return true; }
		@Override public ConversationMessageStatus getStatus() { return mConversationMessageStatus; }
		@Override public void setStatus(ConversationMessageStatus status) { mConversationMessageStatus = status; }

		ChatMessage getRawData() { return mChatMessage; }

		private final PgpChatId mPgpChatId;
		private ChatMessage mChatMessage;
		private ConversationMessageStatus mConversationMessageStatus = ConversationMessageStatus.UNREAD;
	}
	private void appendPgpChatMessageToHistory(PgpChatMessage msg)
	{
		PgpChatId pId = msg.getConversationId();

		ArrayList conversationHistory = (ArrayList<ConversationMessage>) conversationHistoryMap.get(pId);
		if(conversationHistory == null)
		{
			conversationHistory = new ArrayList<PgpChatMessage>();
			conversationHistoryMap.put(pId, conversationHistory);
		}
		conversationHistory.add(msg);
		Collections.sort(conversationHistory);

		notifyRsConversationServiceListeners(new NewMessageConversationEvent(msg));
	}
	private void notifyAndroidAboutPgpChatMsg(PgpChatMessage msg)
	{
		PgpChatId pId = msg.getConversationId();
		if(notificationForConversationEnabled(pId))
		{
			Intent i = new Intent(mContext, ConversationFragmentActivity.class)
					.putExtra(ConversationFragmentActivity.SERVER_NAME_EXTRA, mRsCtrlService.getServerData().name)
					.putExtra(ConversationFragmentActivity.CONVERSATION_ID_EXTRA, pId);
			ChatMessage cMsg = msg.getRawData();
			notifyAndroidAboutConversation(msg.getConversationId(), R.drawable.chat_bubble, (cMsg.getPeerNickname() + " " + mContext.getString(R.string.new_private_chat_message)), Html.fromHtml(cMsg.getMsg()), i, true );
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

		/**
		 * WORK AROUND begin
		 * Doing various tests have demonstrated some strange cases
		 * Sending message to friend with multiple location and with a good part of them offline seems confusing the core causing messages some times get delivered and sometimes not, in a randomlike manner
		 * Sending messages only to online locations seems avoid the problem
		 * this snippet of code select only online location shouldn't be necessary so when the problem in the core get isolated and fixed we should remove this unnecessary part
		 */
		List<Core.Location> onlineLocations = new ArrayList<Core.Location>();
		for(Core.Location location : mRsPeerService.getPersonByPgpId(msg.mPgpChatId.getDestPgpId()).getLocationsList())
			if((location.getState() & Core.Location.StateFlags.CONNECTED_VALUE) == Core.Location.StateFlags.CONNECTED_VALUE)
				onlineLocations.add(location);
//		for(Core.Location location : mRsPeerService.getPersonByPgpId(msg.mPgpChatId.getDestPgpId()).getLocationsList())
		for(Core.Location location : onlineLocations) /** WORK AROUND end */
		{
			ChatId chatId = builderChatId.setChatId(location.getSslId()).build();
			ChatMessage chatMessage = builderChatMessage.setId(chatId).build();
			RequestSendMessage requestSendMessage = builderRequestSendMessage.setMsg(chatMessage).build();

			rsMessage.body = requestSendMessage.toByteArray();

//			Log.d(TAG(), "Sending \"" + requestSendMessage.getMsg().getMsg() + "\" to " + location.getLocation() + " <" + requestSendMessage.getMsg().getId().getChatId() + ">" );
			mRsCtrlService.sendMsg(rsMessage);
		}

		appendPgpChatMessageToHistory(new PgpChatMessage(msg.getConversationId(), builderChatMessage.build()));
	}

	/**
	 * LOBBY_CHAT stuff
	 */
	public static final class LobbyChatId extends ConversationId
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
		@Override public void writeToParcel(Parcel parcel, int i) { parcel.writeString(mChatId.getChatId()); }

		public ChatId getChatId(){ return mChatId; }

		public static final Parcelable.Creator<LobbyChatId> CREATOR = new Parcelable.Creator<LobbyChatId>()
		{
			public LobbyChatId createFromParcel(Parcel in) { return LobbyChatId.Factory.getLobbyChatId(in.readString()); }
			public LobbyChatId[] newArray(int size) { return new LobbyChatId[size]; }
		};

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
			private static final Map<String, LobbyChatId> repository = new WeakHashMap<String, LobbyChatId>();
		}

		private LobbyChatId(String lobbyIdString) { mChatId = ChatId.newBuilder().setChatType(Chat.ChatType.TYPE_LOBBY).setChatId(lobbyIdString).build(); }
		private final ChatId mChatId;
	}
	public final class LobbyChatInfo extends ConversationInfo
	{
		public LobbyChatInfo(LobbyChatId id)
		{
			lobbyChatId = id;
			chatLobbyInfo = getLobbyInfo(id);
		}

		@Override public ConversationId getConversationId() { return lobbyChatId; }
		@Override public boolean hasTitle() { return (chatLobbyInfo.hasLobbyName() && (chatLobbyInfo.getLobbyName().length() > 0)); }
		@Override public CharSequence getTitle() { return chatLobbyInfo.getLobbyName(); }
		@Override public boolean hasTopic() { return (chatLobbyInfo.hasLobbyTopic() && (chatLobbyInfo.getLobbyTopic().length() > 0)); }
		@Override public CharSequence getTopic() { return chatLobbyInfo.getLobbyTopic(); }
		@Override public int getParticipantsCount() { return chatLobbyInfo.getNicknamesCount(); } // chatLobbyInfo.getNoPeers()
		@Override public List<CharSequence> getParticipantsNick() { return new ArrayList<CharSequence>(chatLobbyInfo.getNicknamesList()); }
		@Override public boolean isPrivate() { return (chatLobbyInfo.hasPrivacyLevel() && chatLobbyInfo.getPrivacyLevel().equals(Chat.LobbyPrivacyLevel.PRIVACY_PRIVATE)); }

		@Override public void writeToParcel(Parcel parcel, int i) { parcel.writeString(lobbyChatId.getChatId().getChatId()); }

		public final LobbyChatInfo.Creator<LobbyChatInfo> CREATOR = new Parcelable.Creator<LobbyChatInfo>()
		{
			public LobbyChatInfo createFromParcel(Parcel in) { return new LobbyChatInfo(LobbyChatId.Factory.getLobbyChatId(in.readString())); }
			public LobbyChatInfo[] newArray(int size) { return new LobbyChatInfo[size]; }
		};

		private final LobbyChatId lobbyChatId;
		private final ChatLobbyInfo chatLobbyInfo;
	}
	public static final class LobbyChatMessage extends ConversationMessage
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
		@Override public boolean hasStatus() { return true; }
		@Override public ConversationMessageStatus getStatus() { return mConversationMessageStatus; }
		@Override public void setStatus(ConversationMessageStatus status) { mConversationMessageStatus = status; }

		public ChatMessage getRawData() { return mChatMessage; };

		private final LobbyChatId mLobbyChatId;
		private ChatMessage mChatMessage;
		private ConversationMessageStatus mConversationMessageStatus = ConversationMessageStatus.UNREAD;
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

		ArrayList conversationHistory = (ArrayList<ConversationMessage>) conversationHistoryMap.get(pId);
		if(conversationHistory == null)
		{
			conversationHistory = new ArrayList<LobbyChatMessage>();
			conversationHistoryMap.put(pId, conversationHistory);
		}
		conversationHistory.add(msg);
		Collections.sort(conversationHistory);

		notifyRsConversationServiceListeners(new NewMessageConversationEvent(msg));
	}
	private void notifyAndroidAboutLobbyChatMessage(LobbyChatMessage msg)
	{
		LobbyChatId pId = msg.getConversationId();
		if(notificationForConversationEnabled(pId))
		{
			Intent i = new Intent(mContext, ConversationFragmentActivity.class)
					.putExtra(ConversationFragmentActivity.SERVER_NAME_EXTRA, mRsCtrlService.getServerData().name)
					.putExtra(ConversationFragmentActivity.CONVERSATION_ID_EXTRA, pId);
			ChatMessage cMsg = msg.getRawData();
			notifyAndroidAboutConversation(msg.getConversationId(), R.drawable.chat_bubble, (getLobbyInfo(pId).getLobbyName() + " " + mContext.getString(R.string.new_lobby_chat_message) + " " + mContext.getString(R.string.from) + ": " + msg.getAuthorString() ), Html.fromHtml(cMsg.getMsg()), i, true );
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

		mRsCtrlService.sendMsg(rsMessage);

		appendLobbyChatMessageToHistory(new LobbyChatMessage(msg.getConversationId(), chatMessage));
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

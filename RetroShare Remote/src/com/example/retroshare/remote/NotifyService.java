package com.example.retroshare.remote;

import java.util.Map;

import rsctrl.chat.Chat.ChatId;
import rsctrl.chat.Chat.ChatMessage;
import rsctrl.chat.Chat.ChatType;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.retroshare.remote.ChatService.ChatServiceListener;

/**
 * The only service class which relies on Android
 * @author till
 *
 */
public class NotifyService implements ChatServiceListener{
	private final static String TAG="NotifyService";
	
	
	private ChatService mChatService;
	
	private Service mService;
	private NotificationManager mNotificationManager;
	private Context mContext;
	
	NotifyService(ChatService cs,Service s){
		mChatService=cs;
		mChatService.registerListener(this);
		
		mService=s;
		mNotificationManager = (NotificationManager) s.getSystemService(Context.NOTIFICATION_SERVICE);
		mContext=s.getApplicationContext();
		
		//createNewNotification(null,null,null);
	}
	Notification notification;
	
	@Override
	public void update() {
		Log.v(TAG,"update");
		
		boolean privateUnread=false;
		boolean lobbyUnread=false;
		
		for(Map.Entry<ChatId,Boolean> entry:mChatService.getChatChanged().entrySet()){
			if(entry.getValue()){
				if(entry.getKey().getChatType().equals(ChatType.TYPE_PRIVATE)){
					privateUnread=true;
				}
				if(entry.getKey().getChatType().equals(ChatType.TYPE_LOBBY)){
					lobbyUnread=true;
				}
			}
		}
		
		if(privateUnread){
			int icon=R.drawable.chat;
			CharSequence tickerText;
			CharSequence contentTitle = mService.getResources().getText(R.string.app_name);
			CharSequence contentText = mService.getResources().getText(R.string.new_private_chat_message);
			
			ChatMessage m=mChatService.getLastPrivateChatMessage();
			if(m!=null){
				tickerText=m.getPeerNickname()+": "+android.text.Html.fromHtml(m.getMsg());
			}
			else{
				tickerText=mService.getResources().getText(R.string.new_private_chat_message);
			}
			
			notification = new Notification(icon, tickerText, System.currentTimeMillis());
			
			Intent notificationIntent = new Intent(mService, PeersActivity.class);
			PendingIntent contentIntent = PendingIntent.getActivity(mService, 0, notificationIntent, 0);
			
			notification.setLatestEventInfo(mContext, contentTitle, contentText, contentIntent);
			
			mNotificationManager.notify(NotificationIds.CHAT_PRIVATE, notification);
		}else{
			mNotificationManager.cancel(NotificationIds.CHAT_PRIVATE);
		}
		
		if(lobbyUnread){
			int icon=R.drawable.chat;
			CharSequence tickerText;
			CharSequence contentTitle = mService.getResources().getText(R.string.app_name);
			CharSequence contentText = mService.getResources().getText(R.string.new_lobby_chat_message);
			
			ChatMessage m=mChatService.getLastChatlobbyMessage();
			if(m!=null){
				tickerText=m.getPeerNickname()+": "+android.text.Html.fromHtml(m.getMsg());
			}
			else{
				tickerText=mService.getResources().getText(R.string.new_private_chat_message);
			}
			
			notification = new Notification(icon, tickerText, System.currentTimeMillis());
			
			Intent notificationIntent = new Intent(mService, ChatlobbyActivity.class);
			PendingIntent contentIntent = PendingIntent.getActivity(mService, 0, notificationIntent, 0);
			
			notification.setLatestEventInfo(mContext, contentTitle, contentText, contentIntent);
			
			mNotificationManager.notify(NotificationIds.CHAT_LOBBY, notification);
		}else{
			mNotificationManager.cancel(NotificationIds.CHAT_LOBBY);
		}
	}
	
	private void createNewNotification(/*Intent intent, String title, String text*/){
		
		Log.v(TAG,"createNewNotification");
		
		int icon=R.drawable.chat;
		CharSequence tickerText = mService.getResources().getText(R.string.new_message);
		CharSequence contentTitle = mService.getResources().getText(R.string.app_name);
		CharSequence contentText = mService.getResources().getText(R.string.new_message);
		
		notification = new Notification(icon, tickerText, System.currentTimeMillis());
		
		Intent notificationIntent = new Intent(mService, PeersActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(mService, 0, notificationIntent, 0);
		
		notification.setLatestEventInfo(mContext, contentTitle, contentText, contentIntent);
		
		mNotificationManager.notify(NotificationIds.CHAT_PRIVATE, notification);
		
	}
	
}

package com.example.retroshare.remote;

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
		
		if(mChatService.getChatChanged().containsValue(true)){
			createNewNotification();
		}else{
			mNotificationManager.cancel(NotificationIds.CHAT_PRIVATE);
		}
		/*
		Intent notificationIntent = new Intent(mService, MainActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(mService, 0, notificationIntent, 0);
		notification.setLatestEventInfo(mContext, "changed", "this thing changed", contentIntent);
		mNotificationManager.notify(2, notification);
		*/
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

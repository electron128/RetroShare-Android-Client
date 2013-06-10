package org.retroshare.android;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.retroshare.java.RsCtrlService;
import org.retroshare.java.RsServerData;
import org.retroshare.java.RsCtrlService.ConnectionError;
import org.retroshare.java.RsCtrlService.ConnectionEvent;
import org.retroshare.java.RsCtrlService.RsCtrlServiceListener;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;


/**
 * This class represents RetroShare as an android service,
 * Notify RetroShare events interesting for the user ( such as connection status and chat messages ) to the android system
 * And stores on disk RetroShare servers connection data like hostname, port, user...
 */
public class RsService extends Service implements RsCtrlServiceListener
{
	private static final String TAG="RsService";
	
	//TODO it is reasonable to use android way of save config instead of use a "raw" file ?
	private static class Datapack implements Serializable
	{
		static final long serialVersionUID = 1L;
		// index: server name // TODO specify better what server name mean ( hostname or arbitrary server name or hostname + port... )
		Map<String,RsServerData> serverDataMap = new HashMap<String,RsServerData>();
	}
	private Datapack mDatapack;
	private NotifyService mNotifyService;
	
	@Override
	public void onCreate()
	{
		try
		{
			ObjectInputStream i = new ObjectInputStream(openFileInput("RsService" + Long.toString(Datapack.serialVersionUID)));
			mDatapack = (Datapack) i.readObject();
			
			Log.v(TAG, "read Datapack, Datapack.serverDataMap="+mDatapack.serverDataMap);
		}
		catch (Exception e) { e.printStackTrace(); }
		
		
		if( mDatapack == null ) { mDatapack = new Datapack(); }
		
		mRsCtrlService = new RsCtrlService(new UiThreadHandler());
		mRsCtrlService.registerListener(this);
		
		mNotifyService = new NotifyService( mRsCtrlService.chatService, this );
		
		// update own State Notification
		updateNotification();
	}
	
	@Override
	public void onDestroy()
	{
		Log.v(TAG, "onDestroy()");
		mNotifyService.cancelAll();
		mRsCtrlService.disconnect();
		mRsCtrlService.destroy();
		mRsCtrlService = null;
	}
	
	public void saveData()
	{
		try
		{
			RsServerData sd = mRsCtrlService.getServerData();
			mDatapack.serverDataMap.put( sd.name, sd );
			Log.v(TAG, "trying to save Datapack, Datapack.serverDataMapt="+mDatapack.serverDataMap);
			ObjectOutputStream o = new ObjectOutputStream(openFileOutput("RsService"+Long.toString(Datapack.serialVersionUID), 0));
			o.writeObject(mDatapack);
		} catch (Exception e) { e.printStackTrace(); } // TODO Auto-generated catch block
	}
	
	public Map<String,RsServerData> getServers() { return mDatapack.serverDataMap; }
	
	private final IBinder mBinder=new RsBinder();
	@Override
	public IBinder onBind(Intent arg0)
	{
		/*
		// tut
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
		
		int icon = R.drawable.ic_launcher;
		CharSequence tickerText = "Hello";
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);
		
		Context context = getApplicationContext();
		CharSequence contentTitle = "My notification";
		CharSequence contentText = "Hello World!";
		Intent notificationIntent = new Intent(this, RsService.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
		
		int HELLO_ID = 1;

		mNotificationManager.notify(HELLO_ID, notification);
		*/
		
		/*
		// tut auch, macht die benachrichtigung aber wegen startForeground() unl�schbar
		Notification notification = new Notification(R.drawable.ic_launcher, "blubber",System.currentTimeMillis());
		
		Intent notificationIntent = new Intent(this, RsService.class);
		
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		
		notification.setLatestEventInfo(this, "rsremote","rs remote wurde gestartet", pendingIntent);
		
		//first param has to be greater 0, dont know why
		startForeground(1, notification);
		*/
		
		return mBinder;
	}
	
	public class RsBinder extends Binder
	{
		RsService getService() { return RsService.this; }
	}
	
	// neu neu nicht lschen
	private static class UiThreadHandler extends Handler implements UiThreadHandlerInterface
	{
		@Override
		public void postToUiThread(Runnable r) { post(r); }
	}
	
	public RsCtrlService mRsCtrlService;

	@Override
	public void onConnectionStateChanged(RsCtrlService.ConnectionEvent ce)
	{
		saveData(); // FIXME to avoid unnecessary write in flash memory it isn't better to save config only if ce == ConnectionEvent.SERVER_DATA_CHANGED ?
		updateNotification();
	}
	
	private void updateNotification()
	{
		int icon;
		String tickerText;
		String contentTitle=(String) getResources().getText(R.string.app_name);
		String contentMessage;
		
		//=(String) getResources().getText(R.string.error)
		
		if(mRsCtrlService.isOnline())
		{
			icon = R.drawable.rstray3;
			tickerText=(String) getResources().getText(R.string.connected);
			//contentTitle=(String) getResources().getText(R.string.app_name);
			contentMessage=(String) getResources().getText(R.string.connected);
		}
		else
		{
			if(mRsCtrlService.getLastConnectionError() == ConnectionError.NONE)
			{
				icon=R.drawable.rstray0;				
				tickerText=(String) getResources().getText(R.string.not_connected);
				//contentTitle="RetroShare Remote";
				contentMessage=(String) getResources().getText(R.string.not_connected);
			}
			else
			{
				icon=R.drawable.rstray0_err2;
				tickerText=(String) getResources().getText(R.string.connection_error);
				//contentTitle="RetroShare Remote";
				contentMessage=(String) getResources().getText(R.string.connection_error);
			}
		}

		
		Notification notification = new Notification(icon, tickerText,System.currentTimeMillis());
		
		Intent notificationIntent = new Intent(this, MainActivity.class);
		
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		
		notification.setLatestEventInfo(this, contentTitle,contentMessage, pendingIntent);
		
		startForeground(NotificationIds.RS_SERVICE, notification);
	}

}

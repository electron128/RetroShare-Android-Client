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
 * And stores as an android file RetroShare servers connection data like hostname, port, user...
 */
public class RsService extends Service implements RsCtrlServiceListener
{
	private static final String TAG="RsService";

	private static final String DataPackBaseFileName = "RetroShareServers";

	private static class Datapack implements Serializable
	{
		static final long serialVersionUID = 1L;
		Map<String,RsServerData> serverDataMap = new HashMap<String,RsServerData>(); // index: server name ( the one called arbitrary name on the ui )
	}
	private Datapack mDatapack;
	
	private NotifyService mNotifyService;
	
	@Override
	public void onCreate()
	{
		try
		{
			ObjectInputStream i = new ObjectInputStream(openFileInput(DataPackBaseFileName + Long.toString(Datapack.serialVersionUID)));
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
			ObjectOutputStream o = new ObjectOutputStream(openFileOutput( DataPackBaseFileName + Long.toString(Datapack.serialVersionUID), 0));
			o.writeObject(mDatapack);
		} catch (Exception e) { e.printStackTrace(); } // TODO Auto-generated catch block
	}
	
	public Map<String,RsServerData> getServers() { return mDatapack.serverDataMap; }

	public class RsBinder extends Binder { RsService getService() { return RsService.this; } }
	private final IBinder mBinder = new RsBinder();
	@Override
	public IBinder onBind(Intent arg0) { return mBinder; }
	
	// new new don't delete
	private static class UiThreadHandler extends Handler implements UiThreadHandlerInterface
	{
		@Override
		public void postToUiThread(Runnable r) { post(r); }
	}
	
	public RsCtrlService mRsCtrlService;

	@Override
	public void onConnectionStateChanged(ConnectionEvent ce)
	{
		if( ce.kind == RsCtrlService.ConnectionEventKind.SERVER_DATA_CHANGED ) saveData();
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
			contentMessage=(String) getResources().getText(R.string.connected);
		}
		else
		{
			if(mRsCtrlService.getLastConnectionError() == ConnectionError.NONE)
			{
				icon=R.drawable.rstray0;				
				tickerText=(String) getResources().getText(R.string.not_connected);
				contentMessage=(String) getResources().getText(R.string.not_connected);
			}
			else
			{
				icon=R.drawable.rstray0_err2;
				tickerText=(String) getResources().getText(R.string.connection_error);
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

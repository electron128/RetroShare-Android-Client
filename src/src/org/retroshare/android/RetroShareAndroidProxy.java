package org.retroshare.android;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import org.retroshare.java.RsCtrlService;
import org.retroshare.java.RsCtrlService.ConnectionError;
import org.retroshare.java.RsCtrlService.ConnectionEvent;
import org.retroshare.java.RsCtrlService.RsCtrlServiceListener;
import org.retroshare.java.RsServerData;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


/**
 * This class is mean to supersede RsService when will become stable, is the gateway between RetroShare Android Components and RetroShare Java RPC multiple back-ends.
 * Notify RetroShare events interesting for the user ( such as connection status and chat messages ) to the android system
 * And stores as an android file RetroShare servers connection data like hostname, port, user...
 */
public class RetroShareAndroidProxy extends Service implements RsCtrlServiceListener
{
	private static final String TAG="RetroShareAndroidProxy";

	private static final String DataPackBaseFileName = "RetroShareServers";

	public static class RsBund
	{
		RsCtrlService server;
		NotifyService notifier;

		RsBund(RsCtrlService serv, NotifyService notif)
		{
			server = serv;
			notifier = notif;
		}

		public RsCtrlService getServer() { return server; }
		public NotifyService getNotifier() { return notifier;}
	};
	public Map<String, RsBund> serverBunds = new HashMap<String, RsBund>();

	private static class Datapack implements Serializable
	{
		static final long serialVersionUID = 1L;
		Map<String,RsServerData> serverDataMap = new HashMap<String,RsServerData>(); // index: server name ( the one called arbitrary name on the ui )
	}
	private Datapack mDatapack;

	@Override
	public void onCreate()
	{
		try
		{
			ObjectInputStream i = new ObjectInputStream(openFileInput( DataPackBaseFileName + Long.toString(Datapack.serialVersionUID)));
			mDatapack = (Datapack) i.readObject();
			
			Log.v(TAG, "read Datapack, Datapack.serverDataMap="+mDatapack.serverDataMap);
		}
		catch (Exception e) { e.printStackTrace(); }

		if( mDatapack == null ) { mDatapack = new Datapack(); }

		// update own State Notification
		updateNotification();
	}
	
	@Override
	public void onDestroy()
	{
		Log.v(TAG, "onDestroy()");

		for (RsBund server : serverBunds.values()) _deactivateServer(server);

		super.onDestroy();
	}
	
	public void saveData()
	{

		for ( RsBund bund : serverBunds.values() )
		{
			RsServerData sd = bund.server.getServerData();
			mDatapack.serverDataMap.put( sd.name, sd );
		}

		try
		{
			Log.v(TAG, "trying to save Datapack, Datapack.serverDataMapt=" + mDatapack.serverDataMap);
			ObjectOutputStream o = new ObjectOutputStream(openFileOutput( DataPackBaseFileName + Long.toString(Datapack.serialVersionUID), 0));
			o.writeObject(mDatapack);
		}
		catch (Exception e) { e.printStackTrace(); } // TODO Auto-generated catch block
	}

	public Map<String,RsServerData> getServers() { return mDatapack.serverDataMap; }

	public class RsProxyBinder extends Binder { RetroShareAndroidProxy getService() { return RetroShareAndroidProxy.this; } }
	private final IBinder mBinder = new RsProxyBinder();
	@Override
	public IBinder onBind(Intent arg0) { return mBinder; }
	
	// new new don't delete
	private static class UiThreadHandler extends Handler implements UiThreadHandlerInterface
	{
		@Override
		public void postToUiThread(Runnable r) { post(r); }
	}

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
		
		boolean isOnline = false, hasError = false;
		for( RsBund bund : serverBunds.values() )
		{
			if(bund.server.isOnline())
			{
				isOnline = true;
				break;
			}
			else if(bund.server.getLastConnectionError() != ConnectionError.NONE) hasError = true;
		}

		if(isOnline)
		{
			icon = R.drawable.rstray3;
			tickerText=(String) getResources().getText(R.string.connected);
			contentMessage=(String) getResources().getText(R.string.connected);
		}
		else
		{
			if(!hasError)
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

	/**
	 * Return the requested server and start it if not running but exists in DataPack
	 * @param serverName The server name you want to get
	 * @return The requested server if exists, null otherwise
	 */
	public RsCtrlService getServer(String serverName)
	{
		_activateServer(serverName);
		return serverBunds.get(serverName).server;
	}

	/**
	 * Activate given server if exists and is not already activated
	 * @param serverName Local RetroShare server name ( to not be confused with hostname )
	 */
	private void _activateServer(String serverName)
	{
		RsServerData serverData = mDatapack.serverDataMap.get(serverName);
		if ( serverData != null && serverBunds.get(serverName) == null )
		{
			RsCtrlService server = new RsCtrlService(new UiThreadHandler()); // TODO check if we just one UiThreadHandler for all servers is enough or if we need one for each server
			server.setServerData(serverData);
			server.registerListener(this);
			RsBund bund = new RsBund(server, new NotifyService(server.chatService, this));
			serverBunds.put(serverName, bund);
			server.connect();
		}
	}

	/**
	 * Deactivate given server if is active
	 * @param serverName Local RetroShare server name ( to not be confused with hostname )
	 */
	private void _deactivateServer(String serverName)
	{
		RsBund bund = serverBunds.get(serverName);
		if( bund != null ) _deactivateServer(bund);
	}

	/**
	 * Deactivate given server
	 * @param bund RsBund to deactivate
	 */
	private void _deactivateServer(RsBund bund)
	{
		bund.server.disconnect();
		bund.server.unregisterListener(this);
		bund.server.destroy();
		bund.notifier.cancelAll();
		serverBunds.remove(bund);
	}
}

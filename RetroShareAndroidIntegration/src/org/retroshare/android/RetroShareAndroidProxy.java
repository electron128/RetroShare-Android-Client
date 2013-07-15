/**
 * @file
 *
 * @section Copyright
 * Copyright (C) 2013 Gioacchino Mazzurco <gio@eigenlab.org>
 *
 * @section License
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 */

package org.retroshare.android;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import org.retroshare.android.RsCtrlService.ConnectionError;
import org.retroshare.android.RsCtrlService.ConnectionEvent;
import org.retroshare.android.RsCtrlService.RsCtrlServiceListener;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


/**
 * This class is the gateway between RetroShare Android Components and RetroShare Java RPC multiple back-ends.
 * It stores as an android file RetroShare servers connection data (like hostname, port, user) and andle multiple connection to multiple retroshare servers
 */
public class RetroShareAndroidProxy extends Service implements RsCtrlServiceListener
{
	private static final String TAG = "RetroShareAndroidProxy";

	private static final String DataPackBaseFileName = "RetroShareServers";

    public UiThreadHandler mUiThreadHandler;

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

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; } // We want this service to continue running until it is explicitly stopped, so return sticky.

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
			ObjectOutputStream o = new ObjectOutputStream(openFileOutput( DataPackBaseFileName + Long.toString(Datapack.serialVersionUID), MODE_PRIVATE));
			o.writeObject(mDatapack);
		}
		catch (Exception e) { e.printStackTrace(); }
	}

	public class RsProxyBinder extends Binder { RetroShareAndroidProxy getService() { return RetroShareAndroidProxy.this; } }
	private final IBinder mBinder = new RsProxyBinder();
	@Override
	public IBinder onBind(Intent arg0) { return mBinder; }
	
	// new new don't delete
	public static class UiThreadHandler extends Handler implements UiThreadHandlerInterface
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
	 * This method add a server to the internal server pack
	 * @param serverData the server data
	 */
	public void addServer(RsServerData serverData)
	{
		mDatapack.serverDataMap.put(serverData.name, serverData);
		saveData();
	}

	/**
	 * This method del a server from the internal server pack ( server is disconnected before deletion )
	 * @param serverName the server name
	 */
	public void delServer(String serverName)
	{
		deactivateServer(serverName);
		mDatapack.serverDataMap.remove(serverName);
		saveData();
	}

	/**
	 * This method provide access to saved servers
	 * @return Map containing the saved servers
	 */
	public Map<String,RsServerData> getSavedServers() { return mDatapack.serverDataMap; }

	/**
	 * This method provide access to active servers
	 * @return Map containing the active servers
	 */
	public Map<String, RsCtrlService> getActiveServers()
	{
		Map<String, RsCtrlService> servers = new HashMap<String, RsCtrlService>();
		for(RsBund bund : serverBunds.values()) servers.put(bund.server.getServerData().name, bund.server);
		return servers;
	};

	/**
	 * Return the requested server and start it if not running but exists in DataPack
	 * @param serverName The server name you want to get
	 * @return The requested server if exists, null otherwise
	 */
	public RsCtrlService activateServer(String serverName)
	{
//		Log.d(TAG, "activateServer(" + serverName + ")");
		_activateServer(serverName);
		RsBund bund = serverBunds.get(serverName);
		if(bund != null) return bund.server;
		return null;
	}

	/**
	 * Activate given server if exists and is not already activated
	 * @param serverName Local RetroShare server name ( to not be confused with hostname )
	 */
	private void _activateServer(String serverName)
	{
//		Log.d(TAG, "_activateServer(" + serverName + ")");

		RsServerData serverData = mDatapack.serverDataMap.get(serverName);

		if ( serverData != null )
		{
			RsBund bund = serverBunds.get(serverName);
			if( bund == null)
			{
				Log.d(TAG, "_activateServer(String serverName) activating server");
				RsCtrlService server = new RsCtrlService(mUiThreadHandler);
				server.setServerData(serverData);
				server.registerListener(this);
				bund = new RsBund(server, new NotifyService(server.mRsChatService, this, serverName));
				serverBunds.put(serverName, bund);
			}

			if( ! bund.server.isOnline() ) bund.server.connect();
		}
		else { throw new RuntimeException(TAG + " _activateServer(" + serverName + ") called with for non existent server "); }
	}

	/**
	 * Deactivate given server if is active
	 * @param serverName Local RetroShare server name ( to not be confused with hostname )
	 */
	public void deactivateServer(String serverName)
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
        String serverName = bund.getServer().getServerData().name;
		bund.server.disconnect();
		bund.server.unregisterListener(this);
		bund.server.destroy();
		bund.notifier.cancelAll();
		serverBunds.remove(serverName);
	}
}

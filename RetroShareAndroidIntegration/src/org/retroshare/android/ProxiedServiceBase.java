package org.retroshare.android;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

/**
 * @author G10h4ck
 * This class is aimed to be inherited by Services that needs to communicate with RsService
 * provide out of the box almost all needed stuff to communicate with RsService
 * so each service doesn't need to handle all this common stuff
 */
public abstract class ProxiedServiceBase extends Service implements ServiceConnection
{
	private static final String TAG="ProxiedServiceBase";

	protected RetroShareAndroidProxy rsProxy;
	protected boolean mBound = false;

	@Override
	public void onServiceConnected(ComponentName className, IBinder service)
	{
		RetroShareAndroidProxy.RsProxyBinder binder = (RetroShareAndroidProxy.RsProxyBinder) service;
		rsProxy = binder.getService();
		mBound = true;
		Log.v(TAG, "onServiceConnected");
	}

	@Override
	public void onServiceDisconnected(ComponentName arg0)
	{
		mBound = false;
		Log.v(TAG, "onServiceDisconnected");
	}

	private void _bindRsService()
	{
		Intent intent = new Intent(this, RetroShareAndroidProxy.class);
		bindService(intent, this, Context.BIND_AUTO_CREATE);
	}
	private void _unBindRsService() { unbindService(this); }

	/**
	 * This method should be overridden by child classes that want to do something between Services.onCreate and connection initialization it is guaranteed to be executed before onServiceConnected
	 */
	protected void onCreateBeforeConnectionInit()
	{}

	@Override
	public void onCreate()
	{
		super.onCreate();
		onCreateBeforeConnectionInit();
		_bindRsService();
	}

	@Override
	public void onDestroy()
	{
		_unBindRsService();
		super.onDestroy();
	}
}

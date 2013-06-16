package org.retroshare.android;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;


/**
 * @author G10h4ck
 * This class is aimed to be inherited by Activityes that needs to communicate with RsService
 * provide out of the box almost all needed stuff to communicate with RsService
 * so each activity doesn't need to handle all this common stuff
 */
public abstract class ProxiedActivityBase extends Activity implements ServiceConnection
{
    private static final String TAG="ProxiedActivityBase";

    protected RetroShareAndroidProxy rsProxy;
    protected boolean mBound = false;

	public static final String serverNameExtraName = "serverName";
	protected String serverName;

	/**
	 * This method should be overridden by child classes that want to do something between Activity.onCreate and connection initialization it is guaranteed to be executed before onServiceConnected
	 * It is suggested for inflating your activity layout, so you are sure that your widget are in the right place when onServiceConnected() is called
	 */
	protected void onCreateBeforeConnectionInit(Bundle savedInstanceState)
	{}

	/**
	 * This method should be overridden by child classes that want to do something when connection to RetroShareAndroidProxy is available.
	 */
	protected void onServiceConnected()
	{}

	@Override
	public void onServiceConnected(ComponentName className, IBinder service)
	{
		Log.d(TAG, "onServiceConnected(ComponentName className, IBinder service)");

		RetroShareAndroidProxy.RsProxyBinder binder = (RetroShareAndroidProxy.RsProxyBinder) service;
		rsProxy = binder.getService();
		mBound = true;
		onServiceConnected();
	}

	@Override
	public void onServiceDisconnected(ComponentName arg0)
	{
		Log.d(TAG, "onServiceDisconnected(ComponentName arg0)");
		mBound = false;
	}

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
		serverName = getIntent().getStringExtra(serverNameExtraName);
        onCreateBeforeConnectionInit(savedInstanceState);
        _bindRsService();
    }

    @Override
    public void onDestroy()
    {
        _unBindRsService();
        super.onDestroy();
    }

	private void _bindRsService()
	{
		if(mBound) return;

		Intent intent = new Intent(this, RetroShareAndroidProxy.class);
		bindService(intent, this, Context.BIND_AUTO_CREATE);
	}

	private void _unBindRsService() { if(mBound) unbindService(this); }
}
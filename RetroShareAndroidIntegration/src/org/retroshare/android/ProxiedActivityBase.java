package org.retroshare.android;

import android.app.Activity;
import android.content.ComponentName;
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
    public String TAG() { return "ProxiedActivityBase"; }

    protected RetroShareAndroidProxy rsProxy;

    private boolean mBound = false;
	public boolean isBound(){return mBound;};
	protected void setBound(boolean v)
	{
		util.uDebug(this, TAG(), "setBound(" + String.valueOf(v) + ")");
		mBound = v;
	}

	public static final String SERVER_NAME_EXTRA = "org.retroshare.android.intent_extra_keys.serverName";
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

	/**
	 * Get Actual server
	 * @return The actual server if bound, null otherwise
	 */
	protected RsCtrlService getConnectedServer()
	{
		Log.d(TAG(), "getConnectedServer() -> " + serverName );

		if(isBound()) return rsProxy.activateServer(serverName);

		Log.e(TAG(), "getConnectedServer() shouldn't be called before binding");
		return null;
	}

	@Override
	public void onServiceConnected(ComponentName className, IBinder service)
	{
		Log.d(TAG(), "onServiceConnected(ComponentName className, IBinder service)");

		RetroShareAndroidProxy.RsProxyBinder binder = (RetroShareAndroidProxy.RsProxyBinder) service;
		rsProxy = binder.getService();
		setBound(true);
        if(rsProxy.mUiThreadHandler == null) rsProxy.mUiThreadHandler = new RetroShareAndroidProxy.UiThreadHandler();
		onServiceConnected();
	}

	@Override
	public void onServiceDisconnected(ComponentName arg0)
	{
		Log.d(TAG(), "onServiceDisconnected(" + arg0.toShortString() + ")" );
		setBound(false);
	}

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
		serverName = getIntent().getStringExtra(SERVER_NAME_EXTRA);
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
		Log.d(TAG(), "_bindRsService()");

		if(isBound()) return;

		Intent intent = new Intent(this, RetroShareAndroidProxy.class);
		startService(intent);
		bindService(intent, this, 0);
	}

	private void _unBindRsService()
	{
		Log.d(TAG(), "_unBindRsService()");

		if(isBound())
		{
			unbindService(this);
			setBound(false);
		}
	}

	/**
	 * This method launch an activity putting the server name as intent extra data transparently
	 * @param cls The activity to launch like MainActivity.class
	 */
	public void startActivity(Class<?> cls) { startActivity(cls, new Intent()); };

	/**
	 * This method launch an activity adding the server name in the already forged intent extra data transparently
	 * @param cls The activity to launch like MainActivity.class
	 */
	public void startActivity(Class<?> cls, Intent i)
	{
		i.setClass(this, cls);
		startActivity(i);
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public void startActivity(Intent i)
	{
		i.putExtra(SERVER_NAME_EXTRA, serverName);
		super.startActivity(i);
	}
}
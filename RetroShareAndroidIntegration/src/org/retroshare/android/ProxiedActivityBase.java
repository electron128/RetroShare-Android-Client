/**
 * @license
 *
 * Copyright (c) 2013 Gioacchino Mazzurco <gio@eigenlab.org>.
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

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;


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
    protected ProgressBar rsProxyConnectionProgressBar;

    private boolean mBound = false;
	public boolean isBound(){return mBound;}
	protected void setBound(boolean v) { mBound = v; }

	public static final String SERVER_NAME_EXTRA = "org.retroshare.android.intent_extra_keys.serverName";
	protected String serverName;

	private boolean isInForeground = false;
	public boolean isForeground() { return isInForeground; }

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
	public RsCtrlService getConnectedServer()
	{
		if(isBound()) return rsProxy.activateServer(serverName);
		throw new RuntimeException(TAG() + " getConnectedServer() called before binding");
	}

	@Override
	public void onServiceConnected(ComponentName className, IBinder service)
	{
//		Log.d(TAG(), "onServiceConnected(ComponentName className, IBinder service)");

		RetroShareAndroidProxy.RsProxyBinder binder = (RetroShareAndroidProxy.RsProxyBinder) service;
		rsProxy = binder.getService();
		setBound(true);
        if(rsProxy.mUiThreadHandler == null) rsProxy.mUiThreadHandler = new RetroShareAndroidProxy.HandlerThread();
		onServiceConnected();
        rsProxyConnectionProgressBar.setVisibility(View.GONE);
	}

	@Override
	public void onServiceDisconnected(ComponentName arg0)
	{
//		Log.d(TAG(), "onServiceDisconnected(" + arg0.toShortString() + ")" );
		setBound(false);
	}

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        rsProxyConnectionProgressBar = new ProgressBar(this);
        rsProxyConnectionProgressBar.setIndeterminate(true);
        rsProxyConnectionProgressBar.setVisibility(View.VISIBLE);

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

	protected void _bindRsService()
	{
//		Log.d(TAG(), "_bindRsService()");

		if(isBound()) return;

		Intent intent = new Intent(this, RetroShareAndroidProxy.class);
		getApplicationContext().startService(intent);
		getApplicationContext().bindService(intent, this, 0);
	}

	protected void _unBindRsService()
	{
//		Log.d(TAG(), "_unBindRsService()");

		if(isBound())
		{
			getApplicationContext().unbindService(this);
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

	@Override
	public void onPause()
	{
		super.onPause();
		isInForeground = false;
	}

	@Override
	public void onResume()
	{
		super.onResume();
		isInForeground = true;
	}
}
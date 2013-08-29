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

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import org.retroshare.android.utils.WeakHashSet;

import java.util.Collection;


/**
 * This class is aimed to be inherited by FragmentActivityes that needs to communicate with RsService
 * provide out of the box almost all needed stuff to communicate with RsService
 * so each activity doesn't need to handle all this common stuff
 */
public abstract class ProxiedFragmentActivityBase extends FragmentActivity implements ServiceConnection, ProxiedInterface
{
    public String TAG() { return "ProxiedFragmentActivityBase"; }

	/**
	 * Activity Life Cicle stuff
	 */

	/**
	 * This method should be overridden by child classes that want to do something between Activity.onCreate and connection initialization it is guaranteed to be executed before onServiceConnected
	 * It is suggested for inflating your activity layout, so you are sure that your widget are in the right place when onServiceConnected() is called
	 */
	protected void onCreateBeforeConnectionInit(Bundle savedInstanceState) {}
    @Override public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

		serverName = getIntent().getStringExtra(SERVER_NAME_EXTRA);
        onCreateBeforeConnectionInit(savedInstanceState);
        _bindRsService();
    }
	@Override public void onDestroy()
	{
		_unBindRsService();
		super.onDestroy();
	}
	@Override public void onPause()
	{
		super.onPause();
		isInForeground = false;
	}
	@Override public void onResume()
	{
		super.onResume();
		isInForeground = true;
		if(!isBound()) showConnectingToServiceFragmentDialog();
	}
	private boolean isInForeground = false;
	public boolean isForeground() { return isInForeground; }

	/**
	 * ProxiedFragment Stuff
	 */
	protected Collection<ProxiedFragmentBase> proxiedFragCollection = new WeakHashSet<ProxiedFragmentBase>();
	@Override public void onAttachFragment(Fragment fragment)
	{
		super.onAttachFragment(fragment);
		try
		{
			ProxiedFragmentBase pf = (ProxiedFragmentBase) fragment;
			proxiedFragCollection.add(pf);
		}
		catch (ClassCastException e) {}
	}
	@Override protected void onNewIntent(Intent i)
	{
		super.onNewIntent(i);
		if(i.hasExtra(SERVER_NAME_EXTRA)) serverName = i.getStringExtra(SERVER_NAME_EXTRA);
		for(ProxiedFragmentBase pfb : proxiedFragCollection) pfb.onNewIntent(i);
	}

	/**
	 * "Connecting to Service" dialog stuff
	 */
	protected static final String CONNECTING_TO_SERVIE_FRAGMENT_DIALOG_TAG = "org.retroshare.android.ConnectingToServiceFragmentDialog";
	protected void showConnectingToServiceFragmentDialog()
	{
		ConnectingToServiceFragmentDialog cfd = new ConnectingToServiceFragmentDialog();
		cfd.show(getSupportFragmentManager(), CONNECTING_TO_SERVIE_FRAGMENT_DIALOG_TAG);
	}
	protected void hideConnectingToServiceFragmentDialog()
	{
		ConnectingToServiceFragmentDialog cfd = (ConnectingToServiceFragmentDialog) getSupportFragmentManager().findFragmentByTag(CONNECTING_TO_SERVIE_FRAGMENT_DIALOG_TAG);
		if( cfd == null) Log.e(TAG(), "hideConnectingToServiceFragmentDialog() called before showConnectingToServiceFragmentDialog() ?");
		else cfd.dismiss();
	}

	/**
	 * Commodity startActivity
	 */

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
	@Override public void startActivity(Intent i)
	{
		i.putExtra(SERVER_NAME_EXTRA, serverName);
		super.startActivity(i);
	}

	/**
	 * Service Connection stuff
	 */
	private RetroShareAndroidProxy rsProxy;
	public RetroShareAndroidProxy getRsProxy() { return rsProxy; } /** Implements ProxiedInterface */
	private boolean mBound = false;
	protected void setBound(boolean v) { mBound = v; }
	public boolean isBound() { return mBound; } /** Implements ProxiedInterface */
	public static final String SERVER_NAME_EXTRA = "org.retroshare.android.intent_extra_keys.serverName";
	protected String serverName;
	public void onServiceConnected(ComponentName className, IBinder service) /** Implements ServiceConnection */
	{
		RetroShareAndroidProxy.RsProxyBinder binder = (RetroShareAndroidProxy.RsProxyBinder) service;
		rsProxy = binder.getService();
		setBound(true);
		if(rsProxy.mUiThreadHandler == null) rsProxy.mUiThreadHandler = new RetroShareAndroidProxy.HandlerThread();
		onServiceConnected();
		for(ProxiedFragmentBase pf : proxiedFragCollection) pf.onServiceConnected();
		if(isForeground()) hideConnectingToServiceFragmentDialog();
	}
	public void onServiceDisconnected(ComponentName arg0) /** Implements ServiceConnection */ { setBound(false); }
	protected void _bindRsService()
	{
		if(!isBound())
		{
			Intent intent = new Intent(this, RetroShareAndroidProxy.class);
			startService(intent);
			bindService(intent, this, 0);
		}
	}
	protected void _unBindRsService()
	{
		if(isBound())
		{
			unbindService(this);
			setBound(false);
		}
	}
	/**
	 * This method should be overridden by child classes that want to do something when connection to RetroShareAndroidProxy is available.
	 */
	protected void onServiceConnected() {}
	public RsCtrlService getConnectedServer() /** Implements ProxiedInterface */
	{
		if(isBound()) return rsProxy.activateServer(serverName);
		throw new RuntimeException(TAG() + " getConnectedServer() called before binding");
	}

}
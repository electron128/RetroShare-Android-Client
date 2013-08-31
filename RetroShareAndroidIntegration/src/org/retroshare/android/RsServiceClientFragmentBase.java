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
import android.os.IBinder;

/**
 * This class is aimed to be inherited by Fragments that needs to communicate with RsCore
 * provide out of the box almost all needed stuff to communicate with RsCore
 * so each fragment doesn't need to handle all this common stuff
 */
public class RsServiceClientFragmentBase extends ProxiedFragmentBase implements RsClientInterface
{
	public String TAG() { return  "RsServiceClientFragmentBase"; }

	/**
	 * RsClientInterface stuff
	 */

	@Override public void onAttach(Activity a)
	{
		super.onAttach(a);
		if(getArguments() != null && getArguments().containsKey(SERVER_NAME_EXTRA_KEY)) serverName = getArguments().getString(SERVER_NAME_EXTRA_KEY);
		else throw new RuntimeException(TAG() + " requires arguments contain value for key " + SERVER_NAME_EXTRA_KEY);
	}
	protected String serverName;
	public RsCtrlService getConnectedServer() { return getRsProxy().activateServer(serverName); }


	/**
	 * Rs*ServiceListener stuff
	 */

	@Override public void onPause()
	{
		if(isBound()) unregisterRsServicesListeners();
		super.onPause();
	}
	@Override public void onResume()
	{
		super.onResume();
		if(isBound()) registerRsServicesListeners();
	}
	@Override public void onServiceConnected(ComponentName className, IBinder service)
	{
		super.onServiceConnected(className, service);
		registerRsServicesListeners();
	}
//	@Override public void onServiceDisconnected(ComponentName className)
//	{
//		unregisterRsServicesListeners();
//		super.onServiceDisconnected(className);
//	}

	/**
	 * Fragment that need to register Rs*Listener should do it inside this method
	 */
	public void registerRsServicesListeners() {}
	/**
	 * Fragment that need to unregister Rs*Listener should do it inside this method
	 */
	public void unregisterRsServicesListeners() {}
}

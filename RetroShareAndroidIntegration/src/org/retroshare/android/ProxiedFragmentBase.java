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
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.util.Log;


/**
 * This class is aimed to be inherited by Fragments that needs to communicate with RsService
 * provide out of the box almost all needed stuff to communicate with RsService
 * so each fragment doesn't need to handle all this common stuff
 */
public abstract class ProxiedFragmentBase extends Fragment implements ServiceConnection, ProxiedInterface
{
	public String TAG() { return "ProxiedFragmentBase"; }


	/**
	 * Life Cicle stuff
	 */

	private boolean notPaused = false;

	/**
	 * @return true if it is visible to the user, false otherwise
	 */
	public boolean isUserVisible() { return notPaused; }

	@Override public void onPause()
	{
		notPaused = false;
		super.onPause();
	}
	@Override public void onResume()
	{
		super.onResume();
		notPaused = true;
	}



	/**
	 * Service Connection stuff
	 */
	private RetroShareAndroidProxy rsProxy;
	public RetroShareAndroidProxy getRsProxy() { return rsProxy; } /** Implements ProxiedInterface */
	private boolean mBound = false;
	protected void setBound(boolean v) { mBound = v; }
	public boolean isBound() { return mBound; } /** Implements ProxiedInterface */
	public void onServiceConnected(ComponentName className, IBinder service) /** Implements ServiceConnection */
	{
		RetroShareAndroidProxy.RsProxyBinder binder = (RetroShareAndroidProxy.RsProxyBinder) service;
		rsProxy = binder.getService();
		setBound(true);
		if(rsProxy.mUiThreadHandler == null) rsProxy.mUiThreadHandler = new RetroShareAndroidProxy.HandlerThread();
	}
	public void onServiceDisconnected(ComponentName className) /** Implements ServiceConnection */
	{
		setBound(false);
	}
	@Override public void onAttach(Activity a)
	{
		super.onAttach(a);
		if(!isBound())
		{
			Intent intent = new Intent(getActivity(), RetroShareAndroidProxy.class);
			a.getApplicationContext().startService(intent);
			a.getApplicationContext().bindService(intent, this, 0);
		}
	}
	@Override public void onDetach()
	{
		if(isBound())
		{
			getActivity().getApplicationContext().unbindService(this);
			setBound(false);
		}
		super.onDetach();
	}
}

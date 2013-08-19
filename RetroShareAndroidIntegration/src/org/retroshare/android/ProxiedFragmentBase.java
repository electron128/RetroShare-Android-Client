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
import android.support.v4.app.Fragment;


/**
 * This class is aimed to be inherited by Fragments that needs to communicate with RsService
 * provide out of the box almost all needed stuff to communicate with RsService
 * so each fragment doesn't need to handle all this common stuff
 * WARNING: This kind of fragment should be used only inside ProxiedActivityBase otherwise some method can raise exception!
 */
public abstract class ProxiedFragmentBase extends Fragment implements ProxiedInterface
{
	public String TAG() { return "ProxiedFragmentBase"; }

	/**
	 * This method should be overridden by child classes that want to do something when connection to RetroShareAndroidProxy is available.
	 */
	protected void onServiceConnected()
	{
		if(isUserVisible()) registerRsServicesListeners();
	}

	/**
	 * @return true if it is visible to the user, false otherwise
	 */
	public boolean isUserVisible() { return (isVisible() && userVisible); }

	/**
	 * Fragment who need to register Rs*Listener should do it inside this method
	 */
	public void registerRsServicesListeners() {}

	/**
	 *
	 * Fragment who need to unregister Rs*Listener should do it inside this method
	 */
	public void unregisterRsServicesListeners() {}

	@Override public boolean isBound() { return pxIf.isBound(); }
	@Override public RetroShareAndroidProxy getRsProxy() { return pxIf.getRsProxy();}
	@Override public RsCtrlService getConnectedServer() { return pxIf.getConnectedServer(); }
	@Override public void onPause()
	{
		userVisible = false;
		if(isBound()) unregisterRsServicesListeners();
		super.onPause();
	}
	@Override public void onResume()
	{
		super.onResume();
		userVisible = true;
		if(isBound()) registerRsServicesListeners();
	}
	@Override public void onAttach(Activity a)
	{
		super.onAttach(a);

		try { pxIf = ((ProxiedInterface) getActivity()); }
		catch (ClassCastException e) { throw new ClassCastException(a.toString() + " must implement ProxiedInterface"); }
	}

	private ProxiedInterface pxIf;
	private boolean userVisible = false;
}

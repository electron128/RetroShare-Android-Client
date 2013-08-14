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
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

/**
 * @author G10h4ck
 * This class is aimed to be inherited by Fragments that needs to communicate with RsService
 * provide out of the box almost all needed stuff to communicate with RsService
 * so each fragment doesn't need to handle all this common stuff
 * WARNING: This kind of fragment should be used only inside ProxiedActivityBase otherwise some method can raise exception!
 */
public abstract class ProxiedFragmentBase extends Fragment implements ProxiedInterface
{
	public String TAG() { return "ProxiedFragmentBase"; }

	private ProxiedInterface pxIf;

	@Override
	public void onAttach(Activity a)
	{
		super.onAttach(a);

		try { pxIf = ((ProxiedInterface) getActivity()); }
		catch (ClassCastException e) { throw new ClassCastException(a.toString() + " must implement ProxiedInterface"); }
	}

	/**
	 * This method should be overridden by child classes that want to do something when connection to RetroShareAndroidProxy is available.
	 */
	protected void onServiceConnected()
	{}

	@Override public boolean isBound() { return pxIf.isBound(); }
	@Override public RetroShareAndroidProxy getRsProxy() { return pxIf.getRsProxy();}
	@Override public RsCtrlService getConnectedServer() { return pxIf.getConnectedServer(); }
}

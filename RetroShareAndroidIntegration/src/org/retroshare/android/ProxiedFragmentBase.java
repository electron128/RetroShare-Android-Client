package org.retroshare.android;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
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

	@Override
	public void onAttach(Activity a)
	{
		super.onAttach(a);

		try { ProxiedInterface pf = ((ProxiedInterface) getActivity()); }
		catch (ClassCastException e)
		{
			ClassCastException e1 = new ClassCastException("Trying to use " + TAG() + " inside a non ProxiedFragmentActivityBase" );
			throw e1;
		}
	}

	/**
	 * This method should be overridden by child classes that want to do something when connection to RetroShareAndroidProxy is available.
	 */
	protected void onServiceConnected()
	{}

	@Override public boolean isBound() { return ((ProxiedInterface) getActivity()).isBound(); }
	@Override public RetroShareAndroidProxy getRsProxy() { return ((ProxiedInterface) getActivity()).getRsProxy();}
	@Override public RsCtrlService getConnectedServer() { return ((ProxiedInterface) getActivity()).getConnectedServer(); }
}

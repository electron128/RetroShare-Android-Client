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

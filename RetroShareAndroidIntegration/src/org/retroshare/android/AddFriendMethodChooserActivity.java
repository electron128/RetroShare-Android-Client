package org.retroshare.android;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import rsctrl.core.Core;


public class AddFriendMethodChooserActivity extends ProxiedActivityBase
{
	@Override
	public void onCreateBeforeConnectionInit(Bundle savedInstanceState)
	{
		setContentView(R.layout.activity_addfriend);
	}

	public void onFromKnownPeersButtonPressed(View v)
	{
		Intent i = new Intent();
		i.putExtra(PeersActivity.SHOW_ALL_PEER_EXTRA, true);
		startActivity(PeersActivity.class, i);
	}

	public void onShowQrCodeButtonPressed(View v)
	{
		if(isBound())
		{
			Core.Person p = getConnectedServer().mRsPeersService.getOwnPerson();
			Intent i = new Intent();
			i.putExtra(ShowQrCodeActivity.PGP_ID_EXTRA, p.getGpgId());
			i.putExtra(ShowQrCodeActivity.NAME_EXTRA, p.getName());
			startActivity(ShowQrCodeActivity.class, i);
		}
	}
}

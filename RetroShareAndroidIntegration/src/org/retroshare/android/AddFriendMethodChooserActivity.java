package org.retroshare.android;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;


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

	public void onFromQrCodeButtonPressed(View v)
	{
		Toast.makeText(getApplicationContext(), R.string.not_implemented_yet, Toast.LENGTH_LONG).show();
	}
}

package org.retroshare.android;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;


public class ContactMethodChooserActivity extends Activity
{
	public String TAG() { return "ContactMethodChooserActivity"; }

	private String serverName;
	private String pgpId;


	@Override
    public void onCreate(Bundle savedInstanceState)
    {
		super.onCreate(savedInstanceState);

		Log.d(TAG(), "onCreate(Bundle savedInstanceState)");

		Uri uri = getIntent().getData();
        if ( uri != null )
        {
			String sp[] = uri.getPath().split("/");

			serverName = sp[1];
			pgpId = sp[2];

			Intent i = new Intent(this, ChatActivity.class);
			i.putExtra(ChatActivity.PGP_ID_EXTRA, pgpId);
			i.putExtra(ChatActivity.SERVER_NAME_EXTRA, serverName);
			Log.d(TAG(), "Launching ChatActivity for SERVER_NAME_EXTRA=" + serverName + ", PGP_ID_EXTRA=" + pgpId );
			startActivity(i);

			// Finishing right after launching the new activity no problem because we set nohistory in the manifest (http://developer.android.com/guide/topics/manifest/activity-element.html#nohist)
			finish();
        }
		else finish(); // How did we get here without data?
    }
}
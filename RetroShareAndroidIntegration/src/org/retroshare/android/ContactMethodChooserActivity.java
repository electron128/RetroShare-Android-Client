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
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import org.retroshare.android.RsConversationService.PgpChatId;


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

			Intent i = new Intent(this, ConversationFragmentActivity.class);
			i.putExtra(ConversationFragmentActivity.SERVER_NAME_EXTRA, serverName);
			i.putExtra(ConversationFragmentActivity.CONVERSATION_ID_EXTRA, PgpChatId.Factory.getPgpChatId(pgpId));
//			Log.d(TAG(), "Launching ConversationFragmentActivity with SERVER_NAME_EXTRA=" + serverName + ", for pgpid=" + pgpId );
			startActivity(i);

			// Finishing right after launching the new activity no problem because we set nohistory in the manifest (http://developer.android.com/guide/topics/manifest/activity-element.html#nohist)
			finish();
        }
		else finish(); // How did we get here without data?
    }
}
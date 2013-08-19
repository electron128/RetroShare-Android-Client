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

package org.retroshare.android.tests;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import org.retroshare.android.ConversationFragmentActivity;
import org.retroshare.android.LobbiesListFragment;
import org.retroshare.android.LobbiesListFragmentActivity;
import org.retroshare.android.R;
import org.retroshare.android.RsConversationService;


public class TestActivity extends Activity
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tester);
	}

	public void onButtonTestQrReadPressed(View v)
	{
		Intent i = new Intent();
		i.setAction(Intent.ACTION_VIEW);
		i.addCategory(Intent.CATEGORY_DEFAULT);
		i.addCategory(Intent.CATEGORY_BROWSABLE);

		Uri uri = new Uri.Builder()
				.scheme(getString(R.string.retroshare_uri_scheme))
				.authority(getString(R.string.person_uri_authority))
				.appendQueryParameter(getString(R.string.name_uri_query_param), "Just Relay It")
				.appendQueryParameter(getString(R.string.hash_uri_query_param), "AA3BFD5CEEE7EC17")
				.build();

		i.setData(uri);

		startActivity(i);
	}

	public void onButtonTestFragmentChatActivityPressed(View v)
	{
		Intent i = new Intent(this, ConversationFragmentActivity.class);
		i.putExtra(ConversationFragmentActivity.SERVER_NAME_EXTRA, "testServer");
		i.putExtra(ConversationFragmentActivity.CONVERSATION_ID_EXTRA, RsConversationService.PgpChatId.Factory.getPgpChatId("F444EB20C713A5C0"));
		startActivity(i);
	}

	public void onButtonTestHtmlBase64ImageGetterPressed(View v)
	{
		Intent i = new Intent(this, HtmlBase64ImageGetterTestActivity.class);
		startActivity(i);
	}

	public void onButtonTestLobbyChatPressed(View v)
	{
		Intent i = new Intent(this, LobbiesListFragmentActivity.class);
		i.putExtra(LobbiesListFragmentActivity.SERVER_NAME_EXTRA, "testServer");
		startActivity(i);
	}
}

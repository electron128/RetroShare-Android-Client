/**
 * @license
 *
 * Copyright (c) 2013 Gioacchino Mazzurco <gio@eigenlab.org>
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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import org.retroshare.android.RsConversationService.ConversationId;


public class ConversationFragmentActivity extends ProxiedFragmentActivityBase
{
	public static final String CONVERSATION_ID_EXTRA_KEY = ConversationFragment.CONVERSATION_ID_EXTRA_KEY;

	@Override public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.factivity_conversation);

		onNewIntent(getIntent());
	}

	@Override protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);

		final ConversationId conversationId;
		if(intent.hasExtra(CONVERSATION_ID_EXTRA_KEY)) conversationId = intent.getParcelableExtra(CONVERSATION_ID_EXTRA_KEY);
		else throw new RuntimeException(TAG() + " need firing intent contains valid value for " + CONVERSATION_ID_EXTRA_KEY);

		Bundle fragmentArgs = new Bundle(1);
		fragmentArgs.putString(ConversationFragment.SERVER_NAME_EXTRA_KEY, serverName);
		fragmentArgs.putParcelable(ConversationFragment.CONVERSATION_ID_EXTRA_KEY, conversationId);

		ConversationFragment conversationFragment = new ConversationFragment();
		conversationFragment.setArguments(fragmentArgs);

		FragmentManager fragmentManager = getSupportFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.replace(R.id.conversationFragmentContainer, conversationFragment);
		fragmentTransaction.commit();
	}
}

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

import android.os.Bundle;

import org.retroshare.android.RsConversationService.ConversationId;


public class ConversationFragmentActivity extends ProxiedFragmentActivityBase implements ConversationFragment.ConversationFragmentContainer
{
	public final static String CONVERSATION_ID_EXTRA = "conversationID";

	private ConversationId actualConversationId;

	@Override /** Implements ConversationFragmentContainer */
	public ConversationId getConversationId(ConversationFragment f) { return actualConversationId; }

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		actualConversationId = getIntent().getParcelableExtra(CONVERSATION_ID_EXTRA);

		setContentView(R.layout.factivity_conversation);
	}

	@Override
	public void onResume()
	{
		actualConversationId = getIntent().getParcelableExtra(CONVERSATION_ID_EXTRA); // this is the first not casually
		super.onResume();
	}
}

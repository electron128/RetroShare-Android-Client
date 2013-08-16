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

import rsctrl.chat.Chat;

/**
 * @author G10h4ck
 */
public class ConversationFragmentActivity extends ProxiedFragmentActivityBase implements ChatFragment.ChatFragmentContainer
{
	@Override
	public Chat.ChatId getChatId(ChatFragment f)
	{
//		return Chat.ChatId.newBuilder().setChatType(Chat.ChatType.TYPE_PRIVATE).setChatId("ab85062d7142e2f756ee97bfdae319ef").build(); // blackcat
//		return Chat.ChatId.newBuilder().setChatType(Chat.ChatType.TYPE_PRIVATE).setChatId("767fe0b937b86e0fa6ad33593eb3a196").build(); // cave laptop
		return Chat.ChatId.newBuilder().setChatType(Chat.ChatType.TYPE_PRIVATE).setChatId("296dd05f154d3e1ccebb5aeabe164be4").build(); // Mine laptop
//		return Chat.ChatId.newBuilder().setChatType(Chat.ChatType.TYPE_PRIVATE).setChatId("463382d3d37c7ed4fb17ac9db9242e8c").build(); // Just Relay It
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.factivity_conversation);
	}
}

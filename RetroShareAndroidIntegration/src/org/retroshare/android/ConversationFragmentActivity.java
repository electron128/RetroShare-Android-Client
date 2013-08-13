package org.retroshare.android;

import android.os.Bundle;

import rsctrl.chat.Chat;

/**
 * @author G10h4ck
 */
public class ConversationFragmentActivity extends ProxiedFragmentActivityBase implements ChatFragment.ChatFragmentContainer
{
	@Override
	public Chat.ChatId getChatId()
	{
		return Chat.ChatId.newBuilder().setChatType(Chat.ChatType.TYPE_PRIVATE).setChatId("767fe0b937b86e0fa6ad33593eb3a196").build(); // cave laptop
//		return Chat.ChatId.newBuilder().setChatType(Chat.ChatType.TYPE_PRIVATE).setChatId("296dd05f154d3e1ccebb5aeabe164be4").build(); // Mine laptop
//		return Chat.ChatId.newBuilder().setChatType(Chat.ChatType.TYPE_PRIVATE).setChatId("463382d3d37c7ed4fb17ac9db9242e8c").build(); // Just Relay It
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.factivity_conversation);
	}
}

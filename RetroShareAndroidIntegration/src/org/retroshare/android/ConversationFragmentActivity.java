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
		return Chat.ChatId.newBuilder().setChatType(Chat.ChatType.TYPE_PRIVATE).setChatId("463382d3d37c7ed4fb17ac9db9242e8c").build();
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.factivity_conversation);
	}
}

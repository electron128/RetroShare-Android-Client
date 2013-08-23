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

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class ConversationInfoDialogFragment extends DialogFragment
{
	public static final String CONVERSATION_INFO_EXTRA = "org.retroshare.android.ConversationInfoDialogFragment.CONVERSATION_INFO_EXTRA";

	private final Context mContext;
	public ConversationInfoDialogFragment(Context context) { mContext = context; }

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		super.onCreateView(inflater, container, savedInstanceState);

		getDialog().setTitle("Conversation Details");

		RsConversationService.ConversationInfo conversationInfo = getArguments().getParcelable(CONVERSATION_INFO_EXTRA);

		View a = inflater.inflate(R.layout.conversation_info_fragment_layout, container);

		TextView privacyTextView = (TextView)a.findViewById(R.id.conversation_info_privacy);
		if(conversationInfo.isPrivate()) privacyTextView.setText(R.string._private);
		else privacyTextView.setText(R.string._public);

		if(conversationInfo.hasTitle())
		{
			a.findViewById(R.id.conversation_info_title_layout).setVisibility(View.VISIBLE);
			((TextView) a.findViewById(R.id.conversation_info_title)).setText(conversationInfo.getTitle());
		}
		else a.findViewById(R.id.conversation_info_title_layout).setVisibility(View.GONE);

		if(conversationInfo.hasTopic())
		{
			a.findViewById(R.id.conversation_info_topic_layout).setVisibility(View.VISIBLE);
			((TextView) a.findViewById(R.id.conversation_info_topic)).setText(conversationInfo.getTopic());
		}
		else a.findViewById(R.id.conversation_info_topic_layout).setVisibility(View.GONE);

		if(conversationInfo.getParticipantsCount() > 0)
		{
			a.findViewById(R.id.conversation_info_participants_layout).setVisibility(View.VISIBLE);
			ArrayAdapter adapter = new ArrayAdapter(mContext, R.layout.participants_list_item, R.id.participant_name_text_view);
			((ListView) a.findViewById(R.id.conversation_info_participants)).setAdapter(adapter);
			for(CharSequence nick : conversationInfo.getParticipantsNick()) adapter.add(nick);
		}
		else a.findViewById(R.id.conversation_info_participants_layout).setVisibility(View.GONE);

		return a;
	}
}

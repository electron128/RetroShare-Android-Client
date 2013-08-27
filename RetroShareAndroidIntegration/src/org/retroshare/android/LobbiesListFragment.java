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
import android.database.DataSetObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rsctrl.chat.Chat;

public class LobbiesListFragment extends ProxiedFragmentBase
{
	@Override public String TAG() { return "LobbiesListFragment"; }

	private ListView lobbiesListView;
	private LayoutInflater mInflater;

	private LobbiesListAdapter lobbiesListAdapter;
	private List<Chat.ChatLobbyInfo> lobbiesList = new ArrayList<Chat.ChatLobbyInfo>();

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		lobbiesListAdapter = new LobbiesListAdapter();

		View fv = inflater.inflate(R.layout.lobbies_list_fragment, container);
		lobbiesListView = (ListView) fv.findViewById(R.id.lobbiesList);
		lobbiesListView.setAdapter(lobbiesListAdapter);
		lobbiesListView.setOnItemClickListener(lobbiesListAdapter);
		registerForContextMenu(lobbiesListView);

		return fv;
	}
	@Override public void onAttach(Activity a)
	{
		super.onAttach(a);
		mInflater = a.getLayoutInflater();
	}
	@Override public void onServiceConnected()
	{
		if(mHandler == null) mHandler = new Handler();
		mHandler.post(new RequestLobbiesListUpdateRunnable());
		super.onServiceConnected();
	}
	@Override public void registerRsServicesListeners() { getConnectedServer().mRsConversationService.registerRsConversationServiceListener(lobbiesListAdapter); }
	@Override public void unregisterRsServicesListeners() { getConnectedServer().mRsConversationService.unregisterRsConversationServiceListener(lobbiesListAdapter); }
	@Override public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		getActivity().getMenuInflater().inflate(R.menu.lobbies_list_context_menu, menu);
		int position = ((AdapterView.AdapterContextMenuInfo)menuInfo).position;
		Chat.ChatLobbyInfo info = lobbiesList.get(position);
		boolean joined = info.getLobbyState().equals(Chat.ChatLobbyInfo.LobbyState.LOBBYSTATE_JOINED);
		menu.findItem(R.id.join_lobby_menu_item).setVisible(!joined).setOnMenuItemClickListener(new JoinLobbyMenuItemClickListener(position));
		menu.findItem(R.id.leave_lobby_menu_item).setVisible(joined).setOnMenuItemClickListener(new LeaveLobbyMenuItemClickListener(position));
		menu.findItem(R.id.lobby_show_details_menu_item).setOnMenuItemClickListener(new ShowLobbyInfoMenuItemClickListener(position));
	}

	private class LobbiesListAdapter implements ListAdapter, RsConversationService.RsConversationServiceListener, AdapterView.OnItemClickListener
	{
		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			Chat.ChatLobbyInfo lobbyInfo = lobbiesList.get(position);

			View lv = convertView;
			if( lv == null ) lv = mInflater.inflate(R.layout.lobby_list_item, parent, false);

			TextView nameTextView =  (TextView) lv.findViewById(R.id.lobbyNameTextView);
			nameTextView.setText(lobbyInfo.getLobbyName());
			if(lobbyInfo.getLobbyState().equals(Chat.ChatLobbyInfo.LobbyState.LOBBYSTATE_JOINED))
				nameTextView.setTextColor(getResources().getColor(android.R.color.primary_text_dark));
			else
				nameTextView.setTextColor(getResources().getColor(android.R.color.secondary_text_dark));

			TextView peerCountTextView = (TextView) lv.findViewById(R.id.lobbyParticipantsNumberTextView);
			peerCountTextView.setText("(" + String.valueOf(lobbyInfo.getNoPeers()) + ")");

			return lv;
		}

		@Override public void onConversationsEvent(RsConversationService.ConversationEvent event) { if(event.getEventKind().equals(RsConversationService.ConversationEventKind.LOBBY_LIST_UPDATE)) new UpdateLobbiesListAsyncTask().execute(null, null, null); }
		@Override public int getViewTypeCount() { return 1; }
		@Override public void registerDataSetObserver(DataSetObserver observer) { observerSet.add(observer); }
		@Override public void unregisterDataSetObserver(DataSetObserver observer) { observerSet.remove(observer); }
		@Override public boolean areAllItemsEnabled() { return true; }
		@Override public boolean isEnabled(int position) { return true; }
		@Override public boolean hasStableIds() { return false; }
		@Override public boolean isEmpty() { return lobbiesList.isEmpty(); }
		@Override public int getCount() { return lobbiesList.size(); }
		@Override public int getItemViewType(int position) { return 0; }
		@Override public long getItemId(int position) { return 0; }
		@Override public Object getItem(int position) { return lobbiesList.get(position); }

		/**
		 * Notify data observer that fresh data are available
		 */
		public void notifyObservers() { for(DataSetObserver obs : observerSet) obs.onChanged(); }

		@Override
		public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
		{
			Chat.ChatLobbyInfo lobbyInfo = lobbiesList.get(i);
			RsConversationService.LobbyChatId id = RsConversationService.LobbyChatId.Factory.getLobbyChatId(lobbyInfo.getLobbyId());
			if(!lobbyInfo.getLobbyState().equals(Chat.ChatLobbyInfo.LobbyState.LOBBYSTATE_JOINED)) getConnectedServer().mRsConversationService.joinConversation(id);
			Intent intent = new Intent();
			intent.putExtra(ConversationFragmentActivity.CONVERSATION_ID_EXTRA, id);
			((ProxiedFragmentActivityBase)getActivity()).startActivity(ConversationFragmentActivity.class, intent);
		}

		private final class UpdateLobbiesListAsyncTask extends AsyncTask<Void, Void, List<Chat.ChatLobbyInfo>>
		{
			@Override protected List<Chat.ChatLobbyInfo> doInBackground(Void... voids)
			{
				List<Chat.ChatLobbyInfo> ret = new ArrayList<Chat.ChatLobbyInfo>();
				if(isBound()) try
				{
					ret = getConnectedServer().mRsConversationService.getLobbiesList();
					Collections.sort(ret, new AlphabeticalLobbiesComparator());
				} catch (RuntimeException e) {}
				return ret;
			}
			@Override protected void onPostExecute(List<Chat.ChatLobbyInfo> ml)
			{
				if(ml.size() > 0)
				{
					lobbiesList = ml;
					notifyObservers();
				}
			}
		}

		private final Set<DataSetObserver> observerSet = new HashSet<DataSetObserver>();
	}

	private Handler mHandler;
	private static final int UPDATE_INTERVAL = 2000;
	private class RequestLobbiesListUpdateRunnable implements Runnable
	{
		@Override
		public void run()
		{
			if(isUserVisible() && isBound()) getConnectedServer().mRsConversationService.requestLobbiesListUpdate();
			mHandler.postAtTime(new RequestLobbiesListUpdateRunnable(), SystemClock.uptimeMillis() + UPDATE_INTERVAL);
		}
	}
	private static final class AlphabeticalLobbiesComparator implements Comparator<Chat.ChatLobbyInfo> { @Override public int compare(Chat.ChatLobbyInfo c1, Chat.ChatLobbyInfo c2) { return c1.getLobbyName().compareToIgnoreCase(c2.getLobbyName()); } }
	private final class JoinLobbyMenuItemClickListener implements MenuItem.OnMenuItemClickListener
	{
		private final int position;
		public JoinLobbyMenuItemClickListener(int position) { this.position = position; }
		@Override public boolean onMenuItemClick(MenuItem menuItem)
		{
			getConnectedServer().mRsConversationService.joinConversation(RsConversationService.LobbyChatId.Factory.getLobbyChatId(lobbiesList.get(position).getLobbyId()));
			new RequestLobbiesListUpdateRunnable().run();
			return true;
		}
	}
	private final class LeaveLobbyMenuItemClickListener implements MenuItem.OnMenuItemClickListener
	{
		private final int position;
		public LeaveLobbyMenuItemClickListener(int position) { this.position = position; }
		@Override public boolean onMenuItemClick(MenuItem menuItem)
		{
			getConnectedServer().mRsConversationService.leaveConversation(RsConversationService.LobbyChatId.Factory.getLobbyChatId(lobbiesList.get(position).getLobbyId()));
			new RequestLobbiesListUpdateRunnable().run();
			return true;
		}
	}
	private final class ShowLobbyInfoMenuItemClickListener implements MenuItem.OnMenuItemClickListener
	{
		private final int position;
		public ShowLobbyInfoMenuItemClickListener(int position) { this.position = position; }
		@Override public boolean onMenuItemClick(MenuItem menuItem)
		{
			RsConversationService.LobbyChatInfo info = getConnectedServer().mRsConversationService.new LobbyChatInfo(RsConversationService.LobbyChatId.Factory.getLobbyChatId(lobbiesList.get(position).getLobbyId()));
			Bundle args = new Bundle(1);
			args.putParcelable(ConversationInfoDialogFragment.CONVERSATION_INFO_EXTRA, info);
			ConversationInfoDialogFragment df = new ConversationInfoDialogFragment(getActivity());
			df.setArguments(args);
			df.show(getFragmentManager(), "Lobby Details");
			return true;
		}
	}
}

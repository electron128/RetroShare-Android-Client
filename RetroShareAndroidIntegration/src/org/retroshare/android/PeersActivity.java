package org.retroshare.android;

import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.retroshare.android.RsChatService.ChatServiceListener;
import org.retroshare.android.RsPeersService.PeersServiceListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rsctrl.chat.Chat.ChatId;
import rsctrl.chat.Chat.ChatType;
import rsctrl.core.Core.Location;
import rsctrl.core.Core.Person;

public class PeersActivity extends ProxiedActivityBase
{
	public String TAG() { return "PeersActivity"; }

	private boolean showAllPeers = false;
	public static final String SHOW_ALL_PEER_EXTRA = "showAllPeers";

	private boolean showAddFriendButton = false;
	public static final String SHOW_ADD_FRIEND_BUTTON = "shoAddFriendButton";

	private PeersListAdapterListener adapter;
	
    @Override
    public void onCreateBeforeConnectionInit(Bundle savedInstanceState)
	{
		showAllPeers = getIntent().getBooleanExtra(SHOW_ALL_PEER_EXTRA, false);
		showAddFriendButton = getIntent().getBooleanExtra(SHOW_ADD_FRIEND_BUTTON, false);

		setContentView(R.layout.activity_peers);

		ListView lv = (ListView) findViewById(R.id.peersList);
		Button btn = (Button) findViewById(R.id.addFriendsButton);

		if(showAddFriendButton) btn.setVisibility(View.VISIBLE);
		else btn.setVisibility(View.GONE);

        adapter = new PeersListAdapterListener(this);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(adapter);
        lv.setOnItemLongClickListener(adapter);
    }
    
    @Override
    protected void onServiceConnected()
	{
		_registerListeners();
		getConnectedServer().mRsPeersService.updateFriendsList();
	}
    
    @Override
    public void onResume()
	{
    	super.onResume();
		_registerListeners();
		if(isBound()) getConnectedServer().mRsPeersService.updateFriendsList();
    }

	@Override
	public void onPause()
	{
		_unregisterListeners();
		super.onPause();
	}
    
    private class PeersListAdapterListener implements ListAdapter, OnItemClickListener, OnItemLongClickListener, PeersServiceListener, ChatServiceListener
	{
		private final static String TAG = "PeersListAdapterListener";

		private List<Person> personList = new ArrayList<Person>();
		private List<DataSetObserver> observerList = new ArrayList<DataSetObserver>();

    	private LayoutInflater mInflater;
    	
    	public PeersListAdapterListener(Context context) { mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE); }
    	
    	@Override
    	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
		{
			//Log.v("ChatLobbyListAdapterListener","Clicked on Item No:"+Integer.toString(position));
			Person p = personList.get(position);

			if ( p.getRelation().equals(Person.Relationship.FRIEND) || p.getRelation().equals(Person.Relationship.YOURSELF) )
			{
				String sslId = p.getLocations(0).getSslId();
				Intent i = new Intent(PeersActivity.this, ChatActivity.class);
				i.putExtra(ChatActivity.CHAT_ID_EXTRA, ChatId.newBuilder().setChatType(ChatType.TYPE_PRIVATE).setChatId(sslId).build().toByteArray());
				startActivity(ChatActivity.class, i);
			}
    	}
    	
		@Override
		public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position, long id)
		{
			Person p = personList.get(position);
    		Intent i = new Intent( PeersActivity.this, PeerDetailsActivity.class );
    		i.putExtra(PeerDetailsActivity.PGP_ID_EXTRA, p.getGpgId()); // TODO HARDCODED string
    		startActivity(i);
			return true;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
	        Person p = personList.get(position);
	        
	        View view = mInflater.inflate(R.layout.activity_peers_person_item, parent, false);
	        
	        ImageView imageViewMessage   = (ImageView) view.findViewById(R.id.newMessageImageView);
	        ImageView imageViewUserState = (ImageView) view.findViewById(R.id.imageViewUserState);
	        TextView textView1 = (TextView) view.findViewById(R.id.PeerNameTextView);

			boolean isOnline = false;
			boolean hasMessage = false;
			for ( Location l : p.getLocationsList())
			{
				isOnline |= (l.getState() & Location.StateFlags.CONNECTED_VALUE) == Location.StateFlags.CONNECTED_VALUE;

				ChatId chatId = ChatId.newBuilder().setChatType(ChatType.TYPE_PRIVATE).setChatId(l.getSslId()).build();
				RsCtrlService server = getConnectedServer();
				if( server != null ) // TODO clean this porcata
				{
					RsChatService chatService = server.mRsChatService;
					if ( chatService !=  null)
					{
						if ( chatService.getChatChanged().get(chatId) != null ) hasMessage = true ;
					}
					else Log.e(TAG(), "Il chatService e' nullo");
				}
			}
			if(isOnline)
			{
				imageViewUserState.setImageResource(R.drawable.ic_contact_color);
				textView1.setTextColor(Color.BLUE);
			}
			else
			{
				imageViewUserState.setImageResource(R.drawable.ic_contact_picture);
				textView1.setTextColor(Color.GRAY);
			}
			if(hasMessage) imageViewMessage.setVisibility(View.VISIBLE);
			else imageViewMessage.setVisibility(View.GONE);

	        textView1.setText(p.getName());

	        return view;
		}

		@Override public int getCount() { return personList.size(); }
		@Override public Object getItem(int position) { return personList.get(position); }
		@Override public long getItemId(int position) { return 0; }
		@Override public int getItemViewType(int position) { return 0; }
		@Override public int getViewTypeCount() { return 1; }
		@Override public boolean hasStableIds() { return false; }
		@Override public boolean isEmpty() { return personList.isEmpty(); }
		@Override public void registerDataSetObserver(DataSetObserver observer) { observerList.add(observer); }
		@Override public void unregisterDataSetObserver(DataSetObserver observer) { observerList.remove(observer); }
		@Override public boolean areAllItemsEnabled() {return true;}
		@Override public boolean isEnabled(int position) {return true;}
		@Override public void update() // called by RsChatService and RsPeersService
		{
			Log.d(TAG, "update()");

			RsPeersService peersService = getConnectedServer().mRsPeersService;
			//peersService.updateFriendsList(); // Enabling this will cause a loop of request without timer, TODO refresh peers periodically ( like MainActivity do )

			personList.clear();
			if(showAllPeers) personList.addAll(peersService.getPersons());
			else
			{
				List<Person.Relationship> r = new ArrayList<Person.Relationship>();
				r.add(Person.Relationship.YOURSELF);
				r.add(Person.Relationship.FRIEND);
				personList.addAll(peersService.getPersonsByRelationship(r));
			}

			// TODO sort person list

			for(DataSetObserver obs : observerList) { obs.onChanged(); }
		}
    }

	private void _registerListeners()
	{
		Log.d(TAG(), "_registerListeners()");

		if( ! isBound() ) return;

		RsCtrlService server = getConnectedServer();
		server.mRsPeersService.registerListener(adapter);
		server.mRsChatService.registerListener(adapter);
	}

	private void _unregisterListeners()
	{
		Log.d(TAG(), "_unregisterListeners()");

		if( ! isBound() ) return;

		RsCtrlService server = getConnectedServer();
		server.mRsPeersService.unregisterListener(adapter);
		server.mRsChatService.unregisterListener(adapter);
	}

	public void onAddFriendsButtonPressed(View v) { startActivity(AddFriendMethodChooserActivity.class); }
}

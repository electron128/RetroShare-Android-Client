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
	private static final String TAG="PeersActivity";
	
	private PeersListAdapterListener adapter;
	
    @Override
    public void onCreateBeforeConnectionInit(Bundle savedInstanceState)
	{
        adapter = new PeersListAdapterListener(this);
        ListView lv = new ListView(this);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(adapter);
        lv.setOnItemLongClickListener(adapter);
        setContentView(lv);
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

    	private List<Location> locationList = new ArrayList<Location>();
    	private Map<Location,Person> mapLocationToPerson = new HashMap<Location,Person>();
    	private List<DataSetObserver> observerList = new ArrayList<DataSetObserver>();

    	private LayoutInflater mInflater;
    	
    	public PeersListAdapterListener(Context context) { mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE); }
    	
    	@Override
    	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
		{
    		//Log.v("ChatLobbyListAdapterListener","Clicked on Item No:"+Integer.toString(position));
    		Location loc = locationList.get(position);
			String sslId = loc.getSslId();

    		Intent i = new Intent(PeersActivity.this, ChatActivity.class);
    		i.putExtra(ChatActivity.CHAT_ID_EXTRA, ChatId.newBuilder().setChatType(ChatType.TYPE_PRIVATE).setChatId(sslId).build().toByteArray());
			startActivity(ChatActivity.class, i);
    	}
    	
		@Override
		public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position, long id)
		{
			Location loc = locationList.get(position);
			Person p = mapLocationToPerson.get(loc);
    		Intent i = new Intent(PeersActivity.this,PeerDetailsActivity.class);
    		i.putExtra("GpgId", p.getGpgId()); // TODO HARDCODED string
    		i.putExtra("SslId", loc.getSslId()); // TODO HARDCODED string
    		startActivity(i);
			return true;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			Location l = locationList.get(position);
	        Person p = mapLocationToPerson.get(l);
	        
	        View view = mInflater.inflate(R.layout.activity_peers_person_item, parent, false);
	        
	        ImageView imageViewMessage   = (ImageView) view.findViewById(R.id.newMessageImageView);
	        ImageView imageViewUserState = (ImageView) view.findViewById(R.id.imageViewUserState);
	        TextView textView1 = (TextView) view.findViewById(R.id.PeerNameTextView);
	        
	        ChatId chatId = ChatId.newBuilder().setChatType(ChatType.TYPE_PRIVATE).setChatId(l.getSslId()).build();
	        Boolean haveNewMessage = getConnectedServer().mRsChatService.getChatChanged().get(chatId);

	        if( haveNewMessage != null && haveNewMessage.equals(Boolean.TRUE) ) imageViewMessage.setVisibility(View.VISIBLE);
			else imageViewMessage.setVisibility(View.GONE);

	        if( (l.getState() & Location.StateFlags.CONNECTED_VALUE) == Location.StateFlags.CONNECTED_VALUE)
			{
	        	imageViewUserState.setImageResource(R.drawable.ic_contact_color);
	        	textView1.setTextColor(Color.BLUE);
	        }
			else
			{
	        	imageViewUserState.setImageResource(R.drawable.ic_contact_picture);
	        	textView1.setTextColor(Color.GRAY);
	        }
	        
	        textView1.setText(p.getName()+" ("+l.getLocation()+") "/*+Integer.toBinaryString(l.getState())*/);

	        return view;
		}

		@Override public int getCount() { return locationList.size(); }
		@Override public Object getItem(int position) { return locationList.get(position); }
		@Override public long getItemId(int position) { return 0; }
		@Override public int getItemViewType(int position) { return 0; }
		@Override public int getViewTypeCount() { return 1; }
		@Override public boolean hasStableIds() { return false; }
		@Override public boolean isEmpty() { return locationList.isEmpty(); }
		@Override public void registerDataSetObserver(DataSetObserver observer) { observerList.add(observer); }
		@Override public void unregisterDataSetObserver(DataSetObserver observer) { observerList.remove(observer); }
		@Override public boolean areAllItemsEnabled() {return true;}
		@Override public boolean isEnabled(int position) {return true;}
		@Override public void update() // called by RsChatService and RsPeersService
		{
			Log.d(TAG, "update()");

			RsPeersService peersService = getConnectedServer().mRsPeersService;
			peersService.updateFriendsList();

			List<Person> personList = peersService.getPeersList();
			locationList.clear();
			mapLocationToPerson.clear();
			for(Person p : personList)
				for(Location l : p.getLocationsList())
				{
					locationList.add(l);
					mapLocationToPerson.put(l, p);
				}

			for(DataSetObserver obs : observerList) { obs.onChanged(); }
		}
    }

	private void _registerListeners()
	{
		Log.d(TAG, "_registerListeners()");

		if( ! isBound() ) return;

		Log.d(TAG, "_registerListeners() bound");

		RsCtrlService server = getConnectedServer();
		server.mRsPeersService.registerListener(adapter);
		server.mRsChatService.registerListener(adapter);
	}

	private void _unregisterListeners()
	{
		Log.d(TAG, "_unregisterListeners()");

		if( ! isBound() ) return;

		RsCtrlService server = getConnectedServer();
		server.mRsPeersService.unregisterListener(adapter);
		server.mRsChatService.unregisterListener(adapter);
	}
}

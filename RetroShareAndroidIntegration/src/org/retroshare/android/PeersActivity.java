package org.retroshare.android;

import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
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

import org.retroshare.android.RsPeersService.PeersServiceListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import rsctrl.core.Core.Location;
import rsctrl.core.Core.Person;
import rsctrl.peers.Peers;

public class PeersActivity extends ProxiedActivityBase
{
	public String TAG() { return "PeersActivity"; }

	private static final int UPDATE_INTERVAL = 2000;
	Handler mHandler;

	private boolean showAllPeers = false;
	public static final String SHOW_ALL_PEER_EXTRA = "showAllPeers";

	private boolean showAddFriendButton = false;
	public static final String SHOW_ADD_FRIEND_BUTTON_EXTRA = "showAddFriendButton";

	private Peers.RequestPeers.SetOption updateSet = Peers.RequestPeers.SetOption.FRIENDS;
	private Peers.RequestPeers.InfoOption updateInfo = Peers.RequestPeers.InfoOption.ALLINFO;

	private Bitmap offLineImage;
	private Bitmap onLineImage;

	private PeersListAdapterListener adapter;
	
    @Override
    public void onCreateBeforeConnectionInit(Bundle savedInstanceState)
	{
		showAllPeers = getIntent().getBooleanExtra(SHOW_ALL_PEER_EXTRA, false);
		showAddFriendButton = getIntent().getBooleanExtra(SHOW_ADD_FRIEND_BUTTON_EXTRA, false);

		setContentView(R.layout.activity_peers);

		ListView lv = (ListView) findViewById(R.id.peersList);
		Button btn = (Button) findViewById(R.id.addFriendsButton);

		offLineImage = BitmapFactory.decodeResource(getResources(), R.drawable.ic_contact_picture);
		onLineImage  = BitmapFactory.decodeResource(getResources(), R.drawable.ic_contact_color);

		if(showAllPeers) updateSet = Peers.RequestPeers.SetOption.ALL;
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
		if(mHandler == null) mHandler = new Handler();
		mHandler.post(new RequestPeersListUpdateRunnable());
	}
    
    @Override
    public void onResume()
	{
    	super.onResume();
		_registerListeners();
		if(isBound()) getConnectedServer().mRsPeersService.requestPersonsUpdate(updateSet, updateInfo);
    }

	@Override
	public void onPause()
	{
		_unregisterListeners();
		super.onPause();
	}
    
    private class PeersListAdapterListener implements ListAdapter, OnItemClickListener, OnItemLongClickListener, PeersServiceListener
	{
		public String TAG() { return "PeersListAdapterListener"; }

		private List<_Person> personList = new ArrayList<_Person>();
		private List<DataSetObserver> observerList = new ArrayList<DataSetObserver>();

    	private LayoutInflater mInflater;

		private boolean firstUpdate = true;
    	
    	public PeersListAdapterListener(Context context) { mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE); }
    	
    	@Override
    	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
		{
			//Log.v("ChatLobbyListAdapterListener","Clicked on Item No:"+Integer.toString(position));
			_Person p = personList.get(position);

			if ( p.getRelation().equals(Person.Relationship.FRIEND) || p.getRelation().equals(Person.Relationship.YOURSELF) )
			{
				Intent i = new Intent(PeersActivity.this, ConversationFragmentActivity.class);
				i.putExtra(ConversationFragmentActivity.CONVERSATION_ID_EXTRA, RsConversationService.PgpChatId.Factory.getPgpChatId(p.getGpgId()));
				startActivity(ConversationFragmentActivity.class, i);
			}
    	}
    	
		@Override
		public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position, long id)
		{
			_Person p = personList.get(position);
    		Intent i = new Intent( PeersActivity.this, PeerDetailsActivity.class );
    		i.putExtra(PeerDetailsActivity.PGP_ID_EXTRA, p.getGpgId());
    		startActivity(i);
			return true;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) // We should reduce as much as possible the work done on this method
		{
	        _Person p = personList.get(position);
	        
	        View view = convertView;
			if (view == null) view = mInflater.inflate(R.layout.activity_peers_person_item, parent, false);
	        
	        ImageView imageViewMessage   = (ImageView) view.findViewById(R.id.newMessageImageView);
	        ImageView imageViewUserState = (ImageView) view.findViewById(R.id.imageViewUserState);
	        TextView nameTextView = (TextView) view.findViewById(R.id.PeerNameTextView);

			nameTextView.setText(p.getName());
			if(p.isOnline()) nameTextView.setTextColor(Color.BLUE);
			else { nameTextView.setTextColor(Color.GRAY); }

			if(p.hasNewMessage()) imageViewMessage.setVisibility(View.VISIBLE);
			else { imageViewMessage.setVisibility(View.GONE); }

			imageViewUserState.setImageBitmap(p.getImage());

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
			if(firstUpdate)
			{
				personList = getUpdatedData();
				notifyObservers();
				firstUpdate = false;
			}
			else { new UpdateListDataAsyncTask().execute(null, null, null); }
		}

		/**
		 * Update data used to build the ListView
		 */
		public List<_Person> getUpdatedData()
		{
			RsPeersService peersService = getConnectedServer().mRsPeersService;
			List<_Person> pL = new ArrayList<_Person>();

			if(showAllPeers)
			{
				for( Person p : peersService.getPersons()) pL.add(new _Person(p));
				Collections.sort(pL, new _PersonByNameComparator());
			}
			else
			{
				List<Person.Relationship> r = new ArrayList<Person.Relationship>();
				r.add(Person.Relationship.YOURSELF);
				r.add(Person.Relationship.FRIEND);
				for ( Person p : peersService.getPersonsByRelationship(r) ) pL.add(new _Person(p));
				Collections.sort(pL, new _PersonByStatusAndNameComparator() );
			}

			return pL;
		}

		/**
		 * Notify data observer that fresh data are available
		 */
		public void notifyObservers() { for(DataSetObserver obs : observerList) { obs.onChanged(); }; }

		/**
		 * This class is a local representation of a Person to have all information we need available with few method call
		 */
		private final class _Person
		{
			private boolean isOnline = false;
			private boolean hasNewMessage = false;
			private Person person;
			private Bitmap image;

			_Person(Person p)
			{
				person = p;

				for ( Location l : p.getLocationsList()) isOnline |= (l.getState() & Location.StateFlags.CONNECTED_VALUE) == Location.StateFlags.CONNECTED_VALUE;

				if(isOnline) image = onLineImage;
				else { image = offLineImage;}
			}

			public boolean isOnline() { return isOnline; }
			public boolean hasNewMessage() { return hasNewMessage; }
			public String getName() { return person.getName(); }
			public Person.Relationship getRelation() { return person.getRelation(); }
			public String getGpgId() { return person.getGpgId(); }
			public Bitmap getImage() { return image; }
		}

		private class _PersonByStatusAndNameComparator implements Comparator<_Person>
		{
			@Override
			public int compare( _Person p1, _Person p2)
			{
				boolean p1o = p1.isOnline();
				boolean p2o = p2.isOnline();

				if( p1o ^ p2o )
				{
					if ( p1o ) return -1;
					else return 1;
				}
				else return p1.getName().toLowerCase().compareTo(p2.getName().toLowerCase());
			}
		}

		private class _PersonByNameComparator implements Comparator<_Person> { @Override public int compare( _Person p1, _Person p2){ return p1.getName().toLowerCase().compareTo(p2.getName().toLowerCase()); } }

        private class UpdateListDataAsyncTask extends AsyncTask<Void, Void, List<_Person>>
        {
            @Override
            protected List<_Person> doInBackground(Void... voids) { return getUpdatedData(); }

            @Override protected void onPostExecute(List<_Person> pl)
			{
				personList = pl;
				notifyObservers();
			}
        }
    }

	private void _registerListeners()
	{
		Log.d(TAG(), "_registerListeners()");

		if( ! isBound() ) return;

		RsCtrlService server = getConnectedServer();
		server.mRsPeersService.registerListener(adapter);
	}

	private void _unregisterListeners()
	{
		Log.d(TAG(), "_unregisterListeners()");

		if( ! isBound() ) return;

		RsCtrlService server = getConnectedServer();
		server.mRsPeersService.unregisterListener(adapter);
	}

	public void onAddFriendsButtonPressed(View v) { startActivity(AddFriendMethodChooserActivity.class); }

	private class RequestPeersListUpdateRunnable implements Runnable
	{
		@Override
		public void run()
		{
			if(isForeground())
			{
				RsPeersService ps = getConnectedServer().mRsPeersService;
				ps.requestPersonsUpdate(Peers.RequestPeers.SetOption.OWNID, updateInfo);
				ps.requestPersonsUpdate(updateSet, updateInfo);
			}
			int updateInterval = UPDATE_INTERVAL;
			if(showAllPeers) updateInterval *= 5;
			mHandler.postAtTime(new RequestPeersListUpdateRunnable(), SystemClock.uptimeMillis()+ updateInterval);
		}
	}
}

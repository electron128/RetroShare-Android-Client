package org.retroshare.android;

import java.util.ArrayList;
import java.util.List;

import org.retroshare.android.RsSearchService.SearchServiceListener;

import rsctrl.search.Search.SearchHit;

import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

public class ShowSearchResultsActivity extends ProxiedActivityBase
{
	private static final String TAG="ShowSearchResultsActivity";
	
	private static final int UPDATE_INTERVAL =1500;
	
	private ListView listView;
	
	private SearchResultAdapterListener adapter;
	
	Handler mHandler;
	
	int mId;
	
    @Override
    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        
        mId=getIntent().getIntExtra("SearchId", 0);
        
        listView=new ListView(this);

        adapter=new SearchResultAdapterListener(this);
        
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(adapter);
        listView.setOnItemLongClickListener(adapter);
        
        setContentView(listView);
        
    	mHandler=new Handler();
    	
    	//mHandler.postAtTime(new updateRunnable(), SystemClock.uptimeMillis()+UPDATE_INTERVAL);
    }
    
    @Override
    protected void onServiceConnected()
	{
		RsCtrlService server = getConnectedServer();
        server.mRsSearchService.registerListener(adapter);
        
        // TODO remove this
		server.mRsSearchService.updateSearchResults(mId);
    }
    
    boolean isInForeground = false;
    
    @Override
    public void onResume()
	{
    	super.onResume();
    	isInForeground = true;
    }

    @Override
    public void onPause()
	{
    	super.onPause();
    	isInForeground = false;
    }

    @Override
    public void onDestroy()
	{
		getConnectedServer().mRsSearchService.unregisterListener(adapter);
    	super.onDestroy();
    }
    
	private class updateRunnable implements Runnable
	{
		@Override
		public void run()
		{
			RsCtrlService server = getConnectedServer();
			if( isInForeground && isBound() && server.isOnline()) server.mRsSearchService.updateSearchResults(mId);
			mHandler.postAtTime(new updateRunnable(), SystemClock.uptimeMillis()+ UPDATE_INTERVAL);
		}
	}
    
    private class SearchResultAdapterListener implements ListAdapter, OnItemClickListener, OnItemLongClickListener, SearchServiceListener{
    	
    	private List<SearchHit> list=new ArrayList<SearchHit>();
    	
    	private List<DataSetObserver> observerList=new ArrayList<DataSetObserver>();
    	
    	private LayoutInflater mInflater;
    	
    	public SearchResultAdapterListener(Context context) { mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE); }
    	
    	public void setData(List<SearchHit> l)
		{
    		list.clear();
    		list = l;
    		for(DataSetObserver obs:observerList) obs.onChanged();
    	}
    	
    	@Override
    	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
		{
	   		Intent i=new Intent(ShowSearchResultsActivity.this,AddDownloadActivity.class);
    		i.putExtra("File", list.get(position).getFile().toByteArray());
    		startActivity(i);
    	}
    	
		@Override
		public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position, long id)
		{
			/*Location loc=locationList.get(position);
			Person p=mapLocationToPerson.get(loc);
    		Intent i=new Intent(PeersActivity.this,PeerDetailsActivity.class);
    		i.putExtra("GpgId", p.getGpgId());
    		i.putExtra("SslId", loc.getSslId());
    		startActivity(i);*/
			return true;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			TextView tv = new TextView(parent.getContext());
			SearchHit sh = list.get(position);
			tv.setText(sh.getFile().getName()+" ("+Integer.toString(sh.getNoHits())+") "+Long.toString(sh.getFile().getSize()));
	        return tv;
		}

		@Override
		public void update()
		{
			RsCtrlService server = getConnectedServer();
			Log.v(TAG, "update: "+Integer.toString( server.mRsSearchService.getSearchResults(mId).size())+" items");
			setData(server.mRsSearchService.getSearchResults(mId));
		}

		@Override public int getCount() { return list.size(); }
		@Override public Object getItem(int position) { return list.get(position); }
		@Override public long getItemId(int position) { return 0; } // TODO Auto-generated method stub
		@Override public int getItemViewType(int position) { return 0; }
		@Override public int getViewTypeCount() { return 1; }
		@Override public boolean hasStableIds() { return false; } // TODO Auto-generated method stub
		@Override public boolean isEmpty() { return list.isEmpty(); }
		@Override public void registerDataSetObserver(DataSetObserver observer) { observerList.add(observer); }
		@Override public void unregisterDataSetObserver(DataSetObserver observer) { observerList.remove(observer); }
		@Override public boolean areAllItemsEnabled() {return true;}
		@Override public boolean isEnabled(int position) {return true;}
    }
}

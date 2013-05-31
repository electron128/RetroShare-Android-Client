package org.retroshare.android;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.retroshare.android.SearchService.SearchServiceListener;

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

public class ShowSearchResultsActivity extends RsActivityBase {
	private static final String TAG="ShowSearchResultsActivity";
	
	private static final int UPDATE_INTERVALL=1500;
	
	private ListView listView;
	
	private SearchResultAdapterListener adapter;
	
	Handler mHandler;
	
	int mId;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mId=getIntent().getIntExtra("SearchId", 0);
        
        listView=new ListView(this);

        adapter=new SearchResultAdapterListener(this);
        
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(adapter);
        listView.setOnItemLongClickListener(adapter);
        
        setContentView(listView);
        
    	mHandler=new Handler();
    	
    	//mHandler.postAtTime(new updateRunnable(), SystemClock.uptimeMillis()+UPDATE_INTERVALL);
    }
    
    @Override
    protected void onServiceConnected(){
        mRsService.mRsCtrlService.searchService.registerListener(adapter);
        
        // remove this
        mRsService.mRsCtrlService.searchService.updateSearchResults(mId);
    }
    
    boolean isInForeground=false;
    
    @Override
    public void onResume(){
    	super.onResume();
    	isInForeground=true;
    }
    @Override
    public void onPause(){
    	super.onPause();
    	isInForeground=false;
    }
    @Override
    public void onDestroy(){
    	super.onDestroy();
    	mRsService.mRsCtrlService.searchService.unregisterListener(adapter);
    }
    
	private class updateRunnable implements Runnable{
		@Override
		public void run() {
			if(isInForeground && mBound && mRsService.mRsCtrlService.isOnline()){
				mRsService.mRsCtrlService.searchService.updateSearchResults(mId);
			}
			mHandler.postAtTime(new updateRunnable(), SystemClock.uptimeMillis()+UPDATE_INTERVALL);
		}
	}
    
    private class SearchResultAdapterListener implements ListAdapter, OnItemClickListener, OnItemLongClickListener, SearchServiceListener{
    	
    	private List<SearchHit> list=new ArrayList<SearchHit>();
    	
    	private List<DataSetObserver> observerList=new ArrayList<DataSetObserver>();
    	
    	private LayoutInflater mInflater;
    	
    	public SearchResultAdapterListener(Context context) {
    		 mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	}
    	
    	public void setData(List<SearchHit> l){
    		list.clear();
    		list=l;
    		for(DataSetObserver obs:observerList){
    			obs.onChanged();
    		}
    	}
    	
    	@Override
    	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
	   		Intent i=new Intent(ShowSearchResultsActivity.this,AddDownloadActivity.class);
    		i.putExtra("File", list.get(position).getFile().toByteArray());
    		startActivity(i);
    	}
    	
		@Override
		public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position, long id) {
			/*Location loc=locationList.get(position);
			Person p=mapLocationToPerson.get(loc);
    		Intent i=new Intent(PeersActivity.this,PeerDetailsActivity.class);
    		i.putExtra("GpgId", p.getGpgId());
    		i.putExtra("SslId", loc.getSslId());
    		startActivity(i);*/
			return true;
		}

		@Override
		public int getCount() {
			return list.size();
		}

		@Override
		public Object getItem(int position) {
			return list.get(position);
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getItemViewType(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView tv=new TextView(parent.getContext());
			SearchHit sh=list.get(position);
			tv.setText(sh.getFile().getName()+" ("+Integer.toString(sh.getNoHits())+") "+Long.toString(sh.getFile().getSize()));
	        return tv;
		}

		@Override
		public int getViewTypeCount() {
			return 1;
		}

		@Override
		public boolean hasStableIds() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean isEmpty() {
			return list.isEmpty();
		}

		@Override
		public void registerDataSetObserver(DataSetObserver observer) {
			observerList.add(observer);
		}

		@Override
		public void unregisterDataSetObserver(DataSetObserver observer) {
			observerList.remove(observer);
		}

		@Override public boolean areAllItemsEnabled() {return true;}
		@Override public boolean isEnabled(int position) {return true;}

		@Override
		public void update() {
			Log.v(TAG, "update: "+Integer.toString(mRsService.mRsCtrlService.searchService.getSearchResults(mId).size())+" items");
			setData(mRsService.mRsCtrlService.searchService.getSearchResults(mId));
		}
    	
    }
}

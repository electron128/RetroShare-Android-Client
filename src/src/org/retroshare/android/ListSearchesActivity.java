package org.retroshare.android;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.retroshare.android.RsSearchService.SearchResponseHandler;

import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

public class ListSearchesActivity extends ProxiedActivityBase
{
	
	private static final String TAG="ListSearchesActivity";
	
	private EditText editText;
	private ListView listView;
	
	private SearchListAdapterListener adapter;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_listsearches);
        
        editText=(EditText) findViewById(R.id.editText);
        listView=(ListView) findViewById(R.id.listView);
        
        editText.setOnKeyListener(new KeyListener());
        
        adapter=new SearchListAdapterListener(this);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(adapter);
        listView.setOnItemLongClickListener(adapter);
    }
    
	private class KeyListener implements OnKeyListener
	{
		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event) {
			
			if((event.getAction()==KeyEvent.ACTION_DOWN)&(event.getKeyCode() == KeyEvent.KEYCODE_ENTER))
			{
				Log.v(TAG,"KeyListener.onKey() event.getKeyCode() == KeyEvent.KEYCODE_ENTER");
				getConnectedServer().mRsSearchService.sendRequestBasicSearch(editText.getText().toString(), new ResponseHandler());
				return true;
			}
			else return false;
		}
		
	}
	
	// needed, because we know dont know the serach id before we received the result
	private class ResponseHandler implements SearchResponseHandler
	{
		@Override
		public void onSearchResponseReceived(int id)
		{
	   		Intent i=new Intent(ListSearchesActivity.this,ShowSearchResultsActivity.class);
    		i.putExtra("SearchId", id);
    		Log.v(TAG, "ResponseHandler: starting ShowSearchResultsActivity with Id "+Integer.toString(id));
    		startActivity(i);
		}
		
	}
    
    @Override
    protected void onServiceConnected() { adapter.setData(getConnectedServer().mRsSearchService.getSearches()); }
    
    @Override
    public void onResume()
	{
		super.onResume();
    	if(mBound) adapter.setData(getConnectedServer().mRsSearchService.getSearches());
    }

    private class SearchListAdapterListener implements ListAdapter, OnItemClickListener, OnItemLongClickListener
	{
    	
    	private Map<Integer,Integer> searchIdFromIndex=new HashMap<Integer,Integer>();
    	private List<String> list=new ArrayList<String>();
    	
    	private List<DataSetObserver> observerList=new ArrayList<DataSetObserver>();
    	
    	private LayoutInflater mInflater;
    	
    	public SearchListAdapterListener(Context context) {
    		 mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	}
    	
    	public void setData(Map<Integer,String> s){
    		searchIdFromIndex.clear();
    		list.clear();
    		int i=0;
    		for(Map.Entry<Integer, String> e:s.entrySet()){
    			list.add(e.getValue());
    			searchIdFromIndex.put(i, e.getKey());
    		}
    		for(DataSetObserver obs:observerList){
    			obs.onChanged();
    		}
    	}
    	
    	@Override
    	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
	   		Intent i=new Intent(ListSearchesActivity.this,ShowSearchResultsActivity.class);
    		i.putExtra("SearchId", searchIdFromIndex.get(position));
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
			tv.setText(list.get(position));
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
    	
    }
}

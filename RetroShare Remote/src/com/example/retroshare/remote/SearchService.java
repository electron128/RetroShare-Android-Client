package com.example.retroshare.remote;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rsctrl.core.Core;
import rsctrl.search.Search;
import rsctrl.search.Search.RequestBasicSearch;
import rsctrl.search.Search.RequestSearchResults;
import rsctrl.search.Search.ResponseSearchIds;
import rsctrl.search.Search.ResponseSearchResults;
import rsctrl.search.Search.SearchHit;
import rsctrl.search.Search.SearchSet;

import android.annotation.SuppressLint;
import com.example.retroshare.remote.RsCtrlService.RsMessage;
import com.google.protobuf.InvalidProtocolBufferException;

public class SearchService implements ServiceInterface {
	
	RsCtrlService mRsCtrlService;
	
	SearchService(RsCtrlService s){
		mRsCtrlService=s;
	}
	
	public static interface SearchServiceListener{
		public void update();
	}
	
	private Set<SearchServiceListener>mListeners=new HashSet<SearchServiceListener>();
	public void registerListener(SearchServiceListener l){
		mListeners.add(l);
	}
	public void unregisterListener(SearchServiceListener l){
		mListeners.remove(l);
	}
	private void _notifyListeners(){
		for(SearchServiceListener l:mListeners){
			l.update();
		}
	}

	@Override
	public void handleMessage(RsMessage m) {
		// TODO Auto-generated method stub

	}
	
	
	public interface SearchResponseHandler{
		public void onSearchResponseReceived(int id);
	}
	
	// stores search terms
	@SuppressLint("UseSparseArrays")
	Map<Integer,String> searchTermsList=new HashMap<Integer,String>();
	
	public Map<Integer,String> getSearches(){
		return searchTermsList;
	}
	
	public void sendRequestBasicSearch(String term, SearchResponseHandler handler){
		RsMessage msg=new RsMessage();
		msg.msgId=(Core.ExtensionId.CORE_VALUE<<24)|(Core.PackageId.SEARCH_VALUE<<8)|Search.RequestMsgIds.MsgId_RequestBasicSearch_VALUE;
		msg.body=RequestBasicSearch.newBuilder().addTerms(term).build().toByteArray();
		mRsCtrlService.sendMsg(msg, new RequestBasicSearchHandler(term,handler));
	}
	
	private class RequestBasicSearchHandler extends RsMessageHandler{
		SearchResponseHandler handler;
		String term;
		RequestBasicSearchHandler(String t, SearchResponseHandler h){
			super();
			term=t;
			handler=h;
		}

		@Override
		protected void rsHandleMsg(RsMessage msg) {
			try {
				ResponseSearchIds resp=ResponseSearchIds.parseFrom(msg.body);
				searchTermsList.put(resp.getSearchId(0), term);
				handler.onSearchResponseReceived(resp.getSearchId(0));
				_notifyListeners();
			} catch (InvalidProtocolBufferException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	
	/*
	 * RequestListSearches is useless, because we dont save the terms
	 * maybe useful, to delete searches with unknown terms
	 * 
	private class RequestListSearchesHandler extends RsMessageHandler{

		@Override
		protected void rsHandleMsg(RsMessage msg) {
			try {
				ResponseSearchIds resp=ResponseSearchIds.parseFrom(msg.body);
				
			} catch (InvalidProtocolBufferException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	*/
	
	public void updateSearchResults(int id){
		RsMessage msg=new RsMessage();
		msg.msgId=msg.msgId=(Core.ExtensionId.CORE_VALUE<<24)|(Core.PackageId.SEARCH_VALUE<<8)|Search.RequestMsgIds.MsgId_RequestSearchResults_VALUE;
		msg.body=RequestSearchResults.newBuilder()/*.addSearchIds(id)*/.build().toByteArray();
		mRsCtrlService.sendMsg(msg,new RequestSearchResultsHandler(id));
	}
	
	public List<SearchHit> getSearchResults(int id){
		if(searchHitsMap.get(id)!=null){
			/*if(searchHitsMap.get(id).size()>100){
				return searchHitsMap.get(id).subList(0, 100);
			}else{*/
				return searchHitsMap.get(id);
			//}
		}else{
			return new ArrayList<SearchHit>();
		}
	}
	
	@SuppressLint("UseSparseArrays")
	private Map<Integer,List<SearchHit>> searchHitsMap=new HashMap<Integer,List<SearchHit>>();
	
	private class RequestSearchResultsHandler extends RsMessageHandler{
		int id;
		RequestSearchResultsHandler(int i){
			super();
			id=i;
		}

		@Override
		protected void rsHandleMsg(RsMessage msg) {
			try {
				ResponseSearchResults resp=ResponseSearchResults.parseFrom(msg.body);
				//System.out.println("RequestSearchResultsHandler:\n"+resp);
				//searchHitsMap.put(resp.getSearchesList().get(0).getSearchId(), resp.getSearchesList().get(0).getHitsList());
				
				for(SearchSet ss:resp.getSearchesList()){
					Map<String,SearchHit> searchHitsByHash=new HashMap<String,SearchHit>();
					for(SearchHit sh:ss.getHitsList()){
						searchHitsByHash.put(sh.getFile().getHash(), sh);
						/*
						if(searchHitsByHash.size()>100){
							break;
						}*/
					}
					searchHitsMap.put(ss.getSearchId(), new ArrayList<SearchHit>(searchHitsByHash.values()));
				}
				
				/*
				 * uses to much cpu
				 * 
				for(SearchSet ss:resp.getSearchesList()){
					List<SearchHit> l=searchHitsMap.get(ss.getSearchId());
					if(l==null){
						l=new ArrayList<SearchHit>();
					}
					for(SearchHit sh:ss.getHitsList()){
						// if l==0, the loop body would not be executed
						if(l.size()==0){
							l.add(sh);
						}else{
							boolean found=false;
							for(SearchHit e:l){
								// dont know if searchhits are equal, so test hash
								if(e.getFile().getHash().equals(sh.getFile().getHash())){
									found=true;
									// inc noHits here
								}
							}
							if(!found){
								l.add(sh);
							}
						}
					}
					searchHitsMap.put(ss.getSearchId(), l);
				}*/
				
				
				_notifyListeners();
			} catch (InvalidProtocolBufferException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}

}

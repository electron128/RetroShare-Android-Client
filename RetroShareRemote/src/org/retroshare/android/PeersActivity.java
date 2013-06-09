package org.retroshare.android;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rsctrl.chat.Chat;
import rsctrl.chat.Chat.ChatId;
import rsctrl.chat.Chat.ChatLobbyInfo;
import rsctrl.chat.Chat.ChatType;
import rsctrl.core.Core;
import rsctrl.core.Core.Location;
import rsctrl.core.Core.Person;
import rsctrl.peers.Peers;
import rsctrl.peers.Peers.RequestPeers;
import rsctrl.peers.Peers.ResponsePeerList;

import org.retroshare.java.ChatService.ChatServiceListener;
import org.retroshare.java.PeersService.PeersServiceListener;

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
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class PeersActivity extends RsActivityBase {
	private static final String TAG="PeersActivity";
	
	private PeersListAdapterListener adapter;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_peers);
        
        adapter=new PeersListAdapterListener(this);
        ListView lv=new ListView(this);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(adapter);
        lv.setOnItemLongClickListener(adapter);
        setContentView(lv);
    }
    
    @Override
    protected void onServiceConnected(){
        mRsService.mRsCtrlService.peersService.registerListener(adapter);
        mRsService.mRsCtrlService.chatService.registerListener(adapter);
        mRsService.mRsCtrlService.peersService.updatePeersList();
    	
    	
    	//showPeers();
    }
    
    @Override
    public void onResume(){
    	super.onResume();
    	if(mBound){
    		mRsService.mRsCtrlService.peersService.updatePeersList();
    		adapter.update();
    	}
    }
    
    //Button Handler
    public void showPeers(View view){
    	//showPeers();}
    }
    
    private class PeersListAdapterListener implements ListAdapter, OnItemClickListener, OnItemLongClickListener,PeersServiceListener,ChatServiceListener{
    	
    	private List<Person> personList=new ArrayList<Person>();
    	private List<Location> locationList=new ArrayList<Location>();
    	private Map<Location,Person> mapLocationToPerson=new HashMap<Location,Person>();
    	private List<DataSetObserver> observerList=new ArrayList<DataSetObserver>();
    	
    	private LayoutInflater mInflater;
    	
    	public PeersListAdapterListener(Context context) {
    		 mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	}
    	
    	public void setData(List<Person> pl){
    		personList=pl;
    		locationList.clear();
    		mapLocationToPerson.clear();
    		for(Person p:personList){
    			for(Location l:p.getLocationsList()){
    				locationList.add(l);
    				mapLocationToPerson.put(l, p);
    			}
    		}
    		for(DataSetObserver obs:observerList){
    			obs.onChanged();
    		}
    	}
    	
    	@Override
    	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    		//Log.v("ChatLobbyListAdapterListener","Clicked on Item No:"+Integer.toString(position));
    		Location loc=locationList.get(position);
    		
    		Intent i=new Intent(PeersActivity.this,ChatActivity.class);
    		i.putExtra("ChatId", ChatId.newBuilder().setChatType(ChatType.TYPE_PRIVATE).setChatId(loc.getSslId()).build().toByteArray());
    		// keine lobby info
    		//i.putExtra("ChatLobbyInfo", lobbyInfo.toByteArray());
    		startActivity(i);
    		
    	}
    	
		@Override
		public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position, long id) {
			Location loc=locationList.get(position);
			Person p=mapLocationToPerson.get(loc);
    		Intent i=new Intent(PeersActivity.this,PeerDetailsActivity.class);
    		i.putExtra("GpgId", p.getGpgId());
    		i.putExtra("SslId", loc.getSslId());
    		startActivity(i);
			return true;
		}

		@Override
		public int getCount() {
			return locationList.size();
		}

		@Override
		public Object getItem(int position) {
			return locationList.get(position);
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
			Location l=locationList.get(position);
	        Person p=mapLocationToPerson.get(l);
	        
	        View view = mInflater.inflate(R.layout.activity_peers_person_item, parent, false);
	        
	        ImageView imageViewMessage=(ImageView) view.findViewById(R.id.imageViewMessage);
	        ImageView imageViewUserState=(ImageView) view.findViewById(R.id.imageViewUserState);
	        TextView textView1 = (TextView) view.findViewById(R.id.textView1);
	        
	        ChatId chatId=ChatId.newBuilder().setChatType(ChatType.TYPE_PRIVATE).setChatId(l.getSslId()).build();
	        Boolean haveNewMesage = mRsService.mRsCtrlService.chatService.getChatChanged().get(chatId);
	        imageViewMessage.setVisibility(View.GONE);
	        if(haveNewMesage!=null){
	        	if(haveNewMesage.equals(Boolean.TRUE)){
	        		imageViewMessage.setVisibility(View.VISIBLE);
	        	}
	        }
	        
	        if((l.getState()&Location.StateFlags.CONNECTED_VALUE)==Location.StateFlags.CONNECTED_VALUE){
	        	imageViewUserState.setImageResource(R.drawable.identity);
	        	textView1.setTextColor(Color.BLUE);
	        }else{
	        	imageViewUserState.setImageResource(R.drawable.identity_grey);
	        	textView1.setTextColor(Color.GRAY);
	        }
	        
	        textView1.setText(p.getName()+" ("+l.getLocation()+") "/*+Integer.toBinaryString(l.getState())*/);
	        //textView2.setText("Thema:"+LobbyList.get(position).getLobbyTopic()+"\rLobbyId:"+LobbyList.get(position).getLobbyId());
	        return view;
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
			return locationList.isEmpty();
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
		
		// called by ChatService
		@Override
		public void update() {
			setData(mRsService.mRsCtrlService.peersService.getPeersList());
		}
    	
    }
    
    /*
    public void showPeers(){
        if(mBound){
        	
    		RequestPeers.Builder reqb= RequestPeers.newBuilder();
    		reqb.setSet(RequestPeers.SetOption.FRIENDS);
    		reqb.setInfo(RequestPeers.InfoOption.ALLINFO);
    		RequestPeers req=reqb.build();
    		byte[] b;
    		b=req.toByteArray();
    		//mjrs.sendRpc((Core.ExtensionId.CORE_VALUE<<24)|(Core.PackageId.PEERS_VALUE<<8)|Peers.RequestMsgIds.MsgId_RequestPeers_VALUE, b);
        	RsMessage msg= new RsMessage();
        	msg.msgId=(Core.ExtensionId.CORE_VALUE<<24)|(Core.PackageId.PEERS_VALUE<<8)|Peers.RequestMsgIds.MsgId_RequestPeers_VALUE;
        	msg.body=b;
        	mRsService.mRsCtrlService.sendMsg(msg, new PeersHandler());
        	//EditText text=(EditText) findViewById(R.id.editText1);
        	//text.setText("Bound");
        }
        else{
        	EditText text=(EditText) findViewById(R.id.editText1);
        	text.setText("Error: not bound");
        }
    }
 
    
    private static final int RESPONSE=(0x01<<24);
    
    private class PeersHandler extends RsMessageHandler{
    	@Override
    	protected void rsHandleMsg(RsMessage msg){
    		Log.v(TAG,"PeersHandler:rsHandleMessage");
    		EditText text=(EditText) findViewById(R.id.editText1);
    		if(msg.msgId==(RESPONSE|(Core.PackageId.PEERS_VALUE<<8)|Peers.ResponseMsgIds.MsgId_ResponsePeerList_VALUE)){
    			System.out.println("received Peers.ResponseMsgIds.MsgId_ResponsePeerList_VALUE");
    			try {
    				ResponsePeerList peers = ResponsePeerList.parseFrom(msg.body);
    				for(Core.Person person:peers.getPeersList()){
    					text.setText(text.getText()+person.getName()+"\n");
    				}
    				
    			} catch (InvalidProtocolBufferException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}
    		}
    	}
    }
    */
    
    
    
    
    
    
    
    
}

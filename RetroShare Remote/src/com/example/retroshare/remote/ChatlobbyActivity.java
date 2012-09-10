package com.example.retroshare.remote;

import java.util.ArrayList;
import java.util.List;

import rsctrl.chat.Chat;
import rsctrl.chat.Chat.ChatLobbyInfo;
import rsctrl.chat.Chat.RequestChatLobbies;
import rsctrl.chat.Chat.ResponseChatLobbies;
import rsctrl.core.Core;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.retroshare.remote.RsService.RsMessage;
import com.google.protobuf.InvalidProtocolBufferException;

public class ChatlobbyActivity extends RsActivityBase {
	private static final String TAG="ChatLobbyActivity";
	
	private TextView mText;
	private ChatLobbyListAdapterListener mclla;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        /*
        mText=new TextView(this);
        mText.setHeight(100);
        
        LinearLayout layout=new LinearLayout(this);
        //layout.addView(mText);
        Button button=new Button(this);
        //button.setWidth(-1);
        button.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.WRAP_CONTENT));
        button.setOnClickListener(new OnClickListener(){@Override public void onClick(View v){getChatLobbies();}});
        layout.addView(button);
        Button b2=new Button(this);
        b2.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.WRAP_CONTENT));
        layout.addView(b2);
        
        //TabHost th=new TabHost();
        //th.addTab(tabSpec)
        setContentView(layout);*/
        
        mclla=new ChatLobbyListAdapterListener(this);
        ListView lv=new ListView(this);
        lv.setAdapter(mclla);
        lv.setOnItemClickListener(mclla);
        setContentView(lv);
    }
    
    @Override
    protected void onServiceConnected(){
    	getChatLobbies();
    }
    
    private void getChatLobbies(){
    	RequestChatLobbies.Builder reqb=RequestChatLobbies.newBuilder();
    	reqb.setLobbySet(RequestChatLobbies.LobbySet.LOBBYSET_ALL);
    	RsMessage msg=mRsService.new RsMessage();
    	msg.msgId=(Core.ExtensionId.CORE_VALUE<<24)|(Core.PackageId.CHAT_VALUE<<8)|Chat.RequestMsgIds.MsgId_RequestChatLobbies_VALUE;
    	msg.body=reqb.build().toByteArray();
    	mRsService.sendMsg(msg, new ChatHandler());
    }
    
    private static final int RESPONSE=(0x01<<24);
    
    private class ChatHandler extends RsMessageHandler{
    	@Override
    	protected void rsHandleMsg(RsMessage msg){
    		Log.v(TAG,"ChatHandler:rsHandleMessage");
    		
    		if(msg.msgId==(RESPONSE|(Core.PackageId.CHAT_VALUE<<8)|Chat.ResponseMsgIds.MsgId_ResponseChatLobbies_VALUE)){
    			System.out.println("received Chat.ResponseMsgIds.MsgId_ResponseChatLobbies_VALUE");
    			//mText.setText(mText.getText()+"received Chat.ResponseMsgIds.MsgId_ResponseChatLobbies_VALUE"+"\n");
    			try {
    				ResponseChatLobbies resp=Chat.ResponseChatLobbies.parseFrom(msg.body);
    				/*for(Chat.ChatLobbyInfo li:resp.getLobbiesList()){
    					mText.setText(mText.getText()+li.getLobbyName()+"\n");
    				}*/
    				mclla.setData(resp.getLobbiesList());
    				
    			} catch (InvalidProtocolBufferException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}
    		}
    	}
    }
    
    private class ChatLobbyListAdapterListener implements ListAdapter , OnItemClickListener{
    	
    	private List<Chat.ChatLobbyInfo> LobbyList=new ArrayList<Chat.ChatLobbyInfo>();
    	private List<DataSetObserver> ObserverList=new ArrayList<DataSetObserver>();
    	
    	private LayoutInflater mInflater;
    	
    	public ChatLobbyListAdapterListener(Context context) {
    		 mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	}
    	
    	public void setData(List<Chat.ChatLobbyInfo> l){
    		LobbyList=l;
    		for(DataSetObserver obs:ObserverList){
    			obs.onChanged();
    		}
    	}
    	
    	@Override
    	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    		//Log.v("ChatLobbyListAdapterListener","Clicked on Item No:"+Integer.toString(position));
    		ChatLobbyInfo lobbyInfo=LobbyList.get(position);
    		Intent i=new Intent(ChatlobbyActivity.this,ChatlobbyChatActivity.class);
    		i.putExtra("lobbyId", lobbyInfo.getLobbyId());
    		i.putExtra("lobbyName", lobbyInfo.getLobbyName());
    		startActivity(i);
    		
    	}

		@Override
		public int getCount() {
			return LobbyList.size();
		}

		@Override
		public Object getItem(int position) {
			return LobbyList.get(position);
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
	        View view = mInflater.inflate(R.layout.activity_chalobby_lobby_item, parent, false);
	        TextView textView1 = (TextView) view.findViewById(R.id.textView1);
	        TextView textView2 = (TextView) view.findViewById(R.id.textView2);
	        textView1.setText(LobbyList.get(position).getLobbyName()+" ("+Integer.toString(LobbyList.get(position).getNoPeers())+")");
	        textView2.setText("Thema:"+LobbyList.get(position).getLobbyTopic()+"\rLobbyId:"+LobbyList.get(position).getLobbyId());
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
			return LobbyList.isEmpty();
		}

		@Override
		public void registerDataSetObserver(DataSetObserver observer) {
			ObserverList.add(observer);
		}

		@Override
		public void unregisterDataSetObserver(DataSetObserver observer) {
			ObserverList.remove(observer);
		}

		@Override public boolean areAllItemsEnabled() {return true;}
		@Override public boolean isEnabled(int position) {return true;}
    	
    }
}

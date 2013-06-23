package org.retroshare.android;

import java.util.ArrayList;
import java.util.List;

import rsctrl.chat.Chat;
import rsctrl.chat.Chat.ChatId;
import rsctrl.chat.Chat.ChatLobbyInfo;
import rsctrl.chat.Chat.ChatLobbyInfo.LobbyState;
import rsctrl.chat.Chat.ChatType;

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
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.retroshare.android.RsChatService.ChatServiceListener;
//import org.retroshare.android.RsService.RsMessage;


public class ChatLobbiesActivity extends ProxiedActivityBase
{
	public String TAG() { return "ChatLobbyActivity"; }

	private ChatLobbyListAdapterListener mclla;
	
    @Override
    public void onCreateBeforeConnectionInit(Bundle savedInstanceState)
	{
        mclla = new ChatLobbyListAdapterListener(this);
        ListView lv = new ListView(this);
        lv.setAdapter(mclla);
        lv.setOnItemClickListener(mclla);
        setContentView(lv);
    }
    
    @Override
    protected void onServiceConnected()
	{
		RsCtrlService server = getConnectedServer();
		_registerListeners();
    }
    
    @Override
    public void onResume()
	{
    	super.onResume();
		_registerListeners();

    }

	public void onPause()
	{
		_unregisterListeners();
		super.onPause();
	}

    private class ChatLobbyListAdapterListener implements ListAdapter, OnItemClickListener, ChatServiceListener
	{
    	
    	private List<Chat.ChatLobbyInfo> LobbyList = new ArrayList<Chat.ChatLobbyInfo>();
    	private List<DataSetObserver> ObserverList = new ArrayList<DataSetObserver>();
    	
    	private LayoutInflater mInflater;
    	
    	public ChatLobbyListAdapterListener(Context context) { mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE); }
    	
    	public void setData(List<Chat.ChatLobbyInfo> l)
		{
    		LobbyList = l;
    		for(DataSetObserver obs:ObserverList) obs.onChanged();
    	}
    	
    	@Override
    	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
		{
    		//Log.v("ChatLobbyListAdapterListener","Clicked on Item No:"+Integer.toString(position));
    		ChatLobbyInfo lobbyInfo = LobbyList.get(position);
    		
    		Intent i = new Intent(ChatLobbiesActivity.this, ChatLobbyActivity.class);
    		i.putExtra(ChatLobbyActivity.CHAT_LOBBY_ID_EXTRA, ChatId.newBuilder().setChatType(ChatType.TYPE_LOBBY).setChatId(lobbyInfo.getLobbyId()).build().toByteArray());
    		i.putExtra(ChatLobbyActivity.CHAT_LOBBY_INFO_EXTRA, lobbyInfo.toByteArray());
    		startActivity(i);
    	}

		@Override
		public int getCount() { return LobbyList.size(); }

		@Override
		public Object getItem(int position) { return LobbyList.get(position); }

		@Override
		public long getItemId(int position) { return 0; } // TODO Auto-generated method stub

		@Override
		public int getItemViewType(int position) { return 0; }

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
	        View view = mInflater.inflate(R.layout.activity_chalobby_lobby_item, parent, false);
	        
	        TextView textView1 = (TextView) view.findViewById(R.id.lobbyNameTextView);
	        TextView textView2 = (TextView) view.findViewById(R.id.lobbyTopicTextView);
	        ImageView imageViewMessage = (ImageView) view.findViewById(R.id.imageViewLobbyUnreadMessage);
	        
	        ChatId chatId = ChatId.newBuilder().setChatType(ChatType.TYPE_LOBBY).setChatId(LobbyList.get(position).getLobbyId()).build();
	        Boolean haveNewMesage = getConnectedServer().mRsChatService.getChatChanged().get(chatId);
	        imageViewMessage.setVisibility(View.GONE);
	        if( haveNewMesage != null && haveNewMesage.equals(Boolean.TRUE) ) imageViewMessage.setVisibility(View.VISIBLE);
	        
	        if(LobbyList.get(position).getLobbyState().equals(LobbyState.LOBBYSTATE_JOINED))
			{
	        	textView1.setTextColor(Color.BLUE);
	        	textView2.setTextColor(Color.BLUE);
	        }
			else
			{
	        	textView1.setTextColor(Color.GRAY);
	        	textView2.setTextColor(Color.GRAY);
	        }

	        textView1.setText( LobbyList.get(position).getLobbyName() + " (" + Integer.toString(LobbyList.get(position).getNoPeers()) + ")" );
	        textView2.setText( getResources().getText(R.string.topic) + ": " + LobbyList.get(position).getLobbyTopic() );
	        return view;
		}

		@Override public int getViewTypeCount() { return 1; }
		@Override public boolean hasStableIds() { return false; } // TODO Auto-generated method stub
		@Override public boolean isEmpty() { return LobbyList.isEmpty(); }
		@Override public void registerDataSetObserver(DataSetObserver observer) { ObserverList.add(observer); }
		@Override public void unregisterDataSetObserver(DataSetObserver observer) { ObserverList.remove(observer); }
		@Override public boolean areAllItemsEnabled() {return true;}
		@Override public boolean isEnabled(int position) {return true;}
		@Override public void update() { setData(getConnectedServer().mRsChatService.getChatLobbies()); } // called by RsChatService // TODO here it sometimes raise NullPointerException
    }

	private void _registerListeners()
	{
		Log.d(TAG(), "_registerListeners()");

		if( ! isBound() ) return;

		RsCtrlService server = getConnectedServer();
		server.mRsChatService.registerListener(mclla);
		server.mRsChatService.updateChatLobbies();
	}

	private void _unregisterListeners()
	{
		Log.d(TAG(), "_unregisterListeners()");

		if( ! isBound() ) return;

		RsCtrlService server = getConnectedServer();
		server.mRsChatService.unregisterListener(mclla);
	}
}

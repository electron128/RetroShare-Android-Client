package com.example.retroshare.remote;

import rsctrl.core.Core;
import rsctrl.peers.Peers;
import rsctrl.peers.Peers.RequestPeers;
import rsctrl.peers.Peers.ResponsePeerList;

import com.example.retroshare.remote.RsCtrlService.RsMessage;
//import com.example.retroshare.remote.RsService.RsMessage;
import com.google.protobuf.InvalidProtocolBufferException;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

public class PeersActivity extends RsActivityBase {
	private static final String TAG="PeersActivity";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peers);
    }
    
    @Override
    protected void onServiceConnected(){
    	showPeers();
    }
    
    //Button Handler
    public void showPeers(View view){showPeers();}
    
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
    
    
    
    
    
    
    
    
    
}

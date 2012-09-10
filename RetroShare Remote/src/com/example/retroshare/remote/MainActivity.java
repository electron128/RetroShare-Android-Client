package com.example.retroshare.remote;

//import JRS;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import rsctrl.core.Core;
import rsctrl.peers.Peers;
import rsctrl.peers.Peers.RequestPeers;
import rsctrl.peers.Peers.ResponsePeerList;

import com.google.protobuf.InvalidProtocolBufferException;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends RsActivityBase {
	private static final String TAG="MainActivity";
	
	ByteArrayOutputStream output=new ByteArrayOutputStream();
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        
    	RsServerData[] d=new RsServerData[2];
    	d[0]=new RsServerData();
    	d[0].hostname="192.168.16.2";
    	d[0].port=7022;
    	d[0].user="user";
    	d[0].password="ubuntu123";
    	RsServerData[] x=new RsServerData[2];
    	x=d.clone();
    	Log.v(TAG,"d[0]\""+d[0].user+":"+d[0].password+"@"+d[0].hostname+":"+Integer.toString(d[0].port)+"\"");
    	Log.v(TAG,"x[0]\""+d[0].user+":"+x[0].password+"@"+x[0].hostname+":"+Integer.toString(x[0].port)+"\"");
    	
        
    	EditText text=(EditText) findViewById(R.id.editTextHostname);
    	text.setText("192.168.16.2");
    	
    	text=(EditText) findViewById(R.id.editTextPort);
    	text.setText("7022");
    	
    	text=(EditText) findViewById(R.id.editTextUser);
    	text.setText("user");
    	
    	text=(EditText) findViewById(R.id.editTextPassword);
    	text.setText("ubuntu123");
        
        
       // PrintStream printStream =new PrintStream(output);
        //System.setOut(printStream);
        
        //System.out.println("Hallo Welt");
        
        
		/*
		final int RESPONSE=(0x01<<24);
		
		
		//System.out.println(Integer.toString(0x000000ff, 16));
		
		System.out.println("init jrs");
		JRS mjrs=new JRS("192.168.16.2", 7022, null, "user", "ubuntu123");
		System.out.println("jrs ok");
		//System.out.println("jrs.get:");
		//mjrs.get();
		
		RequestPeers.Builder reqb= RequestPeers.newBuilder();
		reqb.setSet(RequestPeers.SetOption.FRIENDS);
		reqb.setInfo(RequestPeers.InfoOption.ALLINFO);
		RequestPeers req=reqb.build();
		byte[] b;
		b=req.toByteArray();
		mjrs.sendRpc((Core.ExtensionId.CORE_VALUE<<24)|(Core.PackageId.PEERS_VALUE<<8)|Peers.RequestMsgIds.MsgId_RequestPeers_VALUE, b);
		
		// 13 7f 00 01 
		// 00 00 00 01
		// 00 00 00 01
		// 00 00 00 04 
		// 08 04 10 04
		// 137f000100000001000000010000000408041004
		//mjrs.recvToHex();
		
		
		int msgId;
		while((msgId=mjrs.recvRpcs())==0){
		}
		if(msgId==(RESPONSE|(Core.PackageId.PEERS_VALUE<<8)|Peers.ResponseMsgIds.MsgId_ResponsePeerList_VALUE)){
			System.out.println("received Peers.ResponseMsgIds.MsgId_ResponsePeerList_VALUE");
			try {
				ResponsePeerList peers = ResponsePeerList.parseFrom(mjrs.getNextRpc());
				for(Core.Person person:peers.getPeersList()){
					System.out.println(person.getName());
				}
				
			} catch (InvalidProtocolBufferException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}*/
        
        //EditText text=(EditText) findViewById(R.id.editText1);
        //text.setText(output.toString());
        
    }
    
    public void connect(View v){
    	Log.v(TAG, "connect");
        if(mBound){
        	//mRsService.startThread();
        	
        	RsServerData d=new RsServerData();
        	//d.hostname="192.168.16.2";
        	EditText text=(EditText) findViewById(R.id.editTextHostname);
        	d.hostname=text.getText().toString();
        	
        	//d.port=7022;
        	text=(EditText) findViewById(R.id.editTextPort);
        	d.port=Integer.parseInt(text.getText().toString());
        	
        	//d.user="user";
        	text=(EditText) findViewById(R.id.editTextUser);
        	d.user=text.getText().toString();
        	
        	//d.password="ubuntu123";
        	text=(EditText) findViewById(R.id.editTextPassword);
        	d.password=text.getText().toString();
        	
        	Log.v(TAG,"connecting to Server: \""+d.user+":"+d.password+"@"+d.hostname+":"+Integer.toString(d.port)+"\"");
        	
        	mRsService.mRsCtrlService.setServerData(d);
        	mRsService.mRsCtrlService.connect();
            //EditText text2=(EditText) findViewById(R.id.editText1);
            //text2.setText(output.toString());
        }
        else{
        	EditText text=(EditText) findViewById(R.id.editText1);
        	text.setText("Error: not bound");
        }
    }
    public void showPeers(View v){
    	Intent intent = new Intent(this, PeersActivity.class);
    	startActivity(intent);
    }
    
    public void showChatLobbies(View v){
    	Intent intent = new Intent(this, ChatlobbyActivity.class);
    	startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
}

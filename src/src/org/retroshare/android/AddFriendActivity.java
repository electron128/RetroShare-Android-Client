package org.retroshare.android;

import rsctrl.core.Core;
import rsctrl.peers.Peers;
import rsctrl.peers.Peers.RequestAddPeer;
import rsctrl.peers.Peers.RequestAddPeer.AddCmd;
import rsctrl.peers.Peers.ResponseAddPeer;

import org.retroshare.java.RsCtrlService.RsMessage;

import com.google.protobuf.InvalidProtocolBufferException;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class AddFriendActivity extends ProxiedActivityBase
{
	
	TextView tv;
	Button button;
	
    @Override
    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_addfriend);
        tv=(TextView) findViewById(R.id.textView2);
        button=(Button) findViewById(R.id.buttonAddFriend);
        tv.setText("waiting for ResponseAddPeer...\n");
        button.setVisibility(View.GONE);
    }
    
    @Override
    public void onServiceConnected()
	{
    	Uri uri = getIntent().getData();
        String cert=getCertFromUri(uri);
        
    	RequestAddPeer.Builder reqb=RequestAddPeer.newBuilder();
    	reqb.setGpgId("");
    	reqb.setCmd(AddCmd.EXAMINE);
    	reqb.setCert(cert);
    	RsMessage msg=new RsMessage();
    	msg.msgId=(Core.ExtensionId.CORE_VALUE<<24)|(Core.PackageId.PEERS_VALUE<<8)|Peers.RequestMsgIds.MsgId_RequestAddPeer_VALUE;
    	msg.body=reqb.build().toByteArray();
    	getConnectedServer().sendMsg(msg,new HandleRequestAddPeer());
    }
    
    private class HandleRequestAddPeer extends RsMessageHandler
	{
    	@Override
    	public void rsHandleMsg(RsMessage msg)
		{
    		// TODO: add check for msgid
    		try
			{
				ResponseAddPeer resp=ResponseAddPeer.parseFrom(msg.body);
				tv.setText(resp.toString());
			}
			catch (InvalidProtocolBufferException e) { e.printStackTrace(); } // TODO Auto-generated catch block
    	}
    }
    
    public static String getCertFromUri(Uri uri)
	{
    	if(uri.getScheme().equals("retroshare")&&uri.getHost().equals("certificate")){
    		String cert="-----BEGIN PGP PUBLIC KEY BLOCK-----\n";
    		cert+=uri.getQueryParameter("gpgbase64");
    		cert+="\n=";
    		cert+=uri.getQueryParameter("gpgchecksum");
    		cert+="\n-----END PGP PUBLIC KEY BLOCK-----\n";
    		cert+="--SSLID--"+uri.getQueryParameter("sslid")+";--LOCATION--"+uri.getQueryParameter("location")+";\n";
    		// Note: at locipp and extipp, the ';' is included in the query string
    		cert+="--LOCAL--"+uri.getQueryParameter("locipp")+"--EXT--"+uri.getQueryParameter("extipp");
    		
    		//System.err.println(cert);
    		
    		return cert;
    	}else{
    		System.err.println("getCertFromUri(): wrong scheme or host");
    		System.err.println(uri);
    		return null;
    	}
    	
    	
    }
}

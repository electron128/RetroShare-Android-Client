package org.retroshare.android;

import android.os.Looper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rsctrl.core.Core;
import rsctrl.core.Core.Location;
import rsctrl.core.Core.Person;
import rsctrl.peers.Peers;
import rsctrl.peers.Peers.RequestPeers;
import rsctrl.peers.Peers.ResponsePeerList;

import org.retroshare.android.RsCtrlService.RsMessage;

import com.google.protobuf.InvalidProtocolBufferException;

public class RsPeersService implements RsServiceInterface
{
	RsCtrlService mRsCtrlService;
	UiThreadHandlerInterface mUiThreadHandler;

    OwnIdReceivedHandler mOwnIdReceivedHandler;
    Thread mOwnIdReceivedHandlerThread;

	RsPeersService(RsCtrlService s, UiThreadHandlerInterface u)
	{
		mRsCtrlService = s;
		mUiThreadHandler = u;

        mOwnIdReceivedHandlerThread = new Thread(new Runnable() { @Override public void run() { Looper.prepare(); mOwnIdReceivedHandler = new OwnIdReceivedHandler(); Looper.loop(); } });
        mOwnIdReceivedHandlerThread.start();
	}

    private class OwnIdReceivedHandler extends RsMessageHandler
    {
        @Override
        protected void rsHandleMsg(RsMessage msg)
        {
            try { ownPerson = ResponsePeerList.parseFrom(msg.body).getPeersList().get(0); } catch (InvalidProtocolBufferException e) { e.printStackTrace(); } // TODO Auto-generated catch block
        }
    }

	public static interface PeersServiceListener
	{
		public void update();
	}
	
	private Set<PeersServiceListener>mListeners=new HashSet<PeersServiceListener>();
	public void registerListener(PeersServiceListener l) { mListeners.add(l); }
	public void unregisterListener(PeersServiceListener l) { mListeners.remove(l); }
	private void _notifyListeners() { if(mUiThreadHandler != null) mUiThreadHandler.postToUiThread(new Runnable() { @Override public void run(){ for(PeersServiceListener l:mListeners){ l.update(); }; }}); }

	private List<Person> Persons=new ArrayList<Person>();
	public List<Person> getPeersList(){ return Persons; }
	public Person getPersonFromSslId(String sslId)
	{
		for(Person p:Persons){for(Location l:p.getLocationsList()){ if(l.getSslId().equals(sslId)){ return p; } }}
		return null;
	}
	
	private Person ownPerson;
	public Person getOwnPerson()
	{
		if(ownPerson == null)
		{
			RequestPeers.Builder reqb = RequestPeers.newBuilder();
			reqb.setSet(RequestPeers.SetOption.OWNID);
			reqb.setInfo(RequestPeers.InfoOption.ALLINFO);
			RequestPeers req = reqb.build();
			byte[] b;
			b = req.toByteArray();
	    	RsMessage msg = new RsMessage();
	    	//          TODO Code like that is repeated a lot in the sources there is a way to avoid ( maybe creating a function ) to avoid this ?
	    	msg.msgId = (Core.ExtensionId.CORE_VALUE<<24)|(Core.PackageId.PEERS_VALUE<<8)|Peers.RequestMsgIds.MsgId_RequestPeers_VALUE;
	    	msg.body=b;
	    	mRsCtrlService.sendMsg(msg, mOwnIdReceivedHandler);
		}
		return ownPerson;
	}

	public void updatePeersList()
	{
		RequestPeers.Builder reqb = RequestPeers.newBuilder();
		reqb.setSet(RequestPeers.SetOption.FRIENDS);
		reqb.setInfo(RequestPeers.InfoOption.ALLINFO);
		RequestPeers req=reqb.build();
		byte[] b;
		b=req.toByteArray();
    	RsMessage msg= new RsMessage();
    	//         TODO Code like that is repeated a lot in the sources there is a way to avoid ( maybe creating a function ) to avoid this ?
    	msg.msgId=(Core.ExtensionId.CORE_VALUE<<24)|(Core.PackageId.PEERS_VALUE<<8)|Peers.RequestMsgIds.MsgId_RequestPeers_VALUE;
    	msg.body=b;
    	mRsCtrlService.sendMsg(msg);
	}

	@Override
	public void handleMessage(RsMessage msg)
	{
   		System.err.println("PeersHandler:rsHandleMessage");
   		
   		//            TODO Code like that is repeated a lot in the sources there is a way to avoid ( maybe creating a function ) to avoid this ?
		if(msg.msgId==(RsCtrlService.RESPONSE|(Core.PackageId.PEERS_VALUE<<8)|Peers.ResponseMsgIds.MsgId_ResponsePeerList_VALUE))
		{
			System.err.println("received Peers.ResponseMsgIds.MsgId_ResponsePeerList_VALUE");
			
			try
			{
				Persons = ResponsePeerList.parseFrom(msg.body).getPeersList();
				System.err.println(Persons);
				_notifyListeners();
			}
			catch (InvalidProtocolBufferException e) { e.printStackTrace();	}
		}
	}
}

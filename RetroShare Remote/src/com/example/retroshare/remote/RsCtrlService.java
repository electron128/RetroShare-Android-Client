package com.example.retroshare.remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.lag.jaramiko.AuthenticationFailedException;
import net.lag.jaramiko.BadSignatureException;
import net.lag.jaramiko.Channel;
import net.lag.jaramiko.ClientTransport;
/**
 * Platform independent RsCtrlService
 * @author till
 *
 */

public class RsCtrlService implements Runnable{
	private static final boolean DEBUG=true;
	
	public static final int MAGIC_CODE = 0x137f0001;
	public static final int RESPONSE=(0x01<<24);
	
	public static class RsMessage{
		public int msgId;
		public int reqId;
		public byte[] body;
	}
	
	// TODO: callbacks
	public interface RsCtrlServiceListener{
		public void onConnectionStateChanged();
	}
	
	private Set<RsCtrlServiceListener> mListeners=new HashSet<RsCtrlServiceListener>();
	public void registerListener(RsCtrlServiceListener l){
		mListeners.add(l);
	}
	public void unregisterListener(RsCtrlServiceListener l){
		mListeners.remove(l);
	}
	private void notifyListeners(){
		for(RsCtrlServiceListener l:mListeners){
			l.onConnectionStateChanged();
		}
	}
	private void postNotifyListenersToUiThread(){
		mUiThreadHandler.postToUiThread(new Runnable(){
			@Override
			public void run(){
				notifyListeners();
			}
		});
	}
	
	public enum ConnectState{
		ONLINE,OFFLINE
	}
	
	public enum ConnectAction{
		CONNECT,DISCONNECT,NONE
	}
	
	public enum ConnectionError{
		NONE,
		
		// Exception descritption copied from javadoc
		
		/*
		 * Thrown to indicate that the IP address of a host could not be determined.
		 */
		UnknownHostException,

		/*
		 * Signals that an error occurred while attempting to connect a socket
		 *  to a remote address and port. Typically, the remote host cannot be 
		 *  reached because of an intervening firewall, or if an intermediate 
		 *  router is down.
		 */
		NoRouteToHostException,
		
		/*
		 * Signals that an error occurred while attempting to connect a socket 
		 * to a remote address and port. Typically, the connection was refused 
		 * remotely (e.g., no process is listening on the remote address/port).
		 */
		ConnectException,
		
		// wrong hostkey
		BadSignatureException,
		
		//wrong username or password
		AuthenticationFailedException,
		
		SEND_ERROR,
		RECEIVE_ERROR,
		
		UNKNOWN,
	}
	public ConnectionError getLastConnectionError(){
		return mLastConnectionError;
	}
	public String getLasConnectionErrorString(){
		return mLastConnectionErrorString;
	}
	
	/*************************************/
	// dont know if i have to worry with enums and threads
	private volatile ConnectState mConnectState=ConnectState.OFFLINE;
	private volatile ConnectAction mConnectAction=ConnectAction.NONE;
	private volatile ConnectionError mLastConnectionError=ConnectionError.NONE;
	private volatile String mLastConnectionErrorString="";
	
	// accessed from worker and ui thread, so we have to use synchronized()
	
	private UiThreadHandlerInterface mUiThreadHandler;
	private Thread mThread;
	volatile static boolean runThread=false;
	
	private RsServerData mServerData=new RsServerData();
	
	/*************************************/
	// accessed from worker thread only
	private Socket mSocket;
	private ClientTransport mTransport;
	private Channel mChannel;
	private InputStream mInputStream;
	private OutputStream mOutputStream;
	/*************************************/
	
	private Set<ServiceInterface> Services=new HashSet<ServiceInterface>();
	public ChatService chatService;
	public PeersService peersService;
	public FilesService filesService;
	public SearchService searchService;
	
	RsCtrlService(UiThreadHandlerInterface h){
		mUiThreadHandler=h;
		mThread=new Thread(this);
		runThread=true;
		mThread.start();
		
		chatService=new ChatService(this);
		Services.add(chatService);
		
		peersService=new PeersService(this);
		Services.add(peersService);
		
		filesService=new FilesService(this);
		Services.add(filesService);
		
		searchService=new SearchService(this);
		Services.add(searchService);
		
		// preload own Name, needed for Chat
		peersService.getOwnPerson();
		// preload peers list, needed for chat notification
		peersService.updatePeersList();
		// preload chatobby list, because first requset returns just an empty list
		chatService.updateChatLobbies();
	}
	
	// **************************
	// todo: clone the server data, so outside thread 
	// can't change our serverdata which is used in our workerthread
	public void setServerData(RsServerData d){
		synchronized(mServerData){
			mServerData=d.clone();
		}
		notifyListeners();
	}
	public RsServerData getServerData(){
		synchronized(mServerData){
			return mServerData.clone();
		}
	}
	// **************************
	
	public void connect(){
		if(DEBUG){System.err.println("RsCtrlService: connect()");}
		
		synchronized(mConnectAction){
			mConnectAction=ConnectAction.CONNECT;
		}
	}
	
	public boolean isOnline(){
		synchronized(mConnectState){
			return (mConnectState==ConnectState.ONLINE);
		}
	}
	
	// use with synchronized()
	private ArrayList<RsMessage> outMsgList=new ArrayList<RsMessage>();
	private int msgCounter=0;
	
	//holds pairs of <reqId,Handler>
	private HashMap<Integer,RsMessageHandler> msgHandlers= new HashMap<Integer,RsMessageHandler>();
	//holds pairs of <msgId,handler>
	private HashMap<Integer,RsMessageHandler> msgHandlersById= new HashMap<Integer,RsMessageHandler>();
	
	public int sendMsg(RsMessage msg){
		return sendMsg(msg,null);
	}
	
	public int sendMsg(RsMessage msg, RsMessageHandler h){
		int reqId=0;
		synchronized(outMsgList){
			msgCounter++;
			msg.reqId=msgCounter;
			outMsgList.add(msg);
			reqId=msg.reqId;
		}
		synchronized(msgHandlers){
			msgHandlers.put(reqId, h);
		}
		return reqId;
	}
	
	public void registerMsgHandler(int msgId,RsMessageHandler h){
		synchronized(msgHandlersById){
			msgHandlersById.put(msgId, h);
		}
	}
	
	public RsMessageHandler getHandler(int msgId){
		return msgHandlersById.get(msgId);
	}
	
	/**
	 * 
	 */
	@Override
	public void run() {
		while(runThread){
			
			try {
				Thread.sleep(50);
				//Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				System.err.print(e);
			}
			
			boolean connect=false;
			boolean isonline=false;
			
			// check if we have to connect
			synchronized(mConnectAction){
				connect=(mConnectAction==ConnectAction.CONNECT);
			}
			
			synchronized(mConnectState){
				isonline=(mConnectState==ConnectState.ONLINE);
			}
			
			if(connect){
				_connect();
			}
			if(isonline){
				// handle outgoing
				{
					RsMessage msg=null;
					synchronized(outMsgList){
						if(!outMsgList.isEmpty()){
							msg=outMsgList.remove(0);
						}
					}
					if(msg != null){
						_sendMsg(msg);
					}
				}
				// handle incoming
				{
					int msgType=_recvMsg();
					if(msgType != -1){
						final RsMessage msg=new RsMessage();
						msg.msgId=curMsgId;
						msg.reqId=curReqId;
						msg.body=curBody;
						RsMessageHandler h=null;
						synchronized(msgHandlers){
							h=msgHandlers.remove(msg.reqId);
						}
						if(h!=null){
							if(DEBUG){System.err.println("RsCtrlService: run(): received Msg with reqId Handler, will now post Msg to UI Thread");}
							h.setMsg(msg);
							// post to ui thread
							h.post(h);
						}
						else{
							synchronized(msgHandlersById){
								h=msgHandlersById.get(msg.msgId);
							}
							if(h!=null){
								if(DEBUG){System.err.println("RsCtrlService: run(): received Msg with msgId Handler, will now post Msg to UI Thread");}
								h.setMsg(msg);
								// post to ui thread
								h.post(h);
							}
							else{
								if(DEBUG){System.err.println("RsCtrlService: run(): Error: msgHandler not found");}
							}
						}
						// tell every service about the message
						mUiThreadHandler.postToUiThread(new Runnable(){
							@Override public void run(){
								for(ServiceInterface service:Services){
									service.handleMessage(msg);
								}
							}
						});
					}
				}
				
			}
		}
		
	}

	
	private void _connect(){
		try {
			synchronized(mServerData){
				if(DEBUG){System.err.println("RsCtrlService: _connect() ...");}
				
				boolean newHostKey=false;
				if(mServerData.hostkey==null){
					newHostKey=true;
				}
				
				mSocket=new Socket();
				mSocket.connect(new InetSocketAddress(mServerData.hostname,mServerData.port), 2000);
				// try to find when crai in jaramiko is constructed
				// error here
				//System.err.println("RsCtrlService._connect: mServerData.hostkey="+mServerData.hostkey);
				mTransport=new ClientTransport(mSocket);
				// no more error here
				System.err.println("RsCtrlService._connect: mServerData.hostkey="+mServerData.hostkey);
				mTransport.start(mServerData.hostkey, 2000);
				if(newHostKey){
					mServerData.hostkey=mTransport.getRemoteServerKey();
				}
				mTransport.authPassword(mServerData.user, mServerData.password, 2000);
				mChannel=mTransport.openSession(2000);
				mChannel.invokeShell(2000);
				mInputStream=mChannel.getInputStream();
				mOutputStream=mChannel.getOutputStream();
				
				synchronized(mConnectState){mConnectState=ConnectState.ONLINE;}
				synchronized(mConnectAction){mConnectAction=ConnectAction.NONE;}
				
				postNotifyListenersToUiThread();
				
				/*mUiThreadHandler.postToUiThread(new Runnable(){
					@Override
					public void run(){
						notifyListeners();
					}
				});*/
				
				if(DEBUG){System.err.println("RsCtrlService: _connect(): success");}
			}
		} 
		
		// catch zeugs tut ned
		//noch zu händeln:
		// WLAN aus: Java.net.SocketException: Network unreachable
		// wlan an aber falsche ip: Java.net.SocketTimeoutException: Transport endpoint is not connected
		// rs-nogui hat kein cleanup gemacht: net.lag.jaramiko.SSHException: Timeout
		
		catch (UnknownHostException e){
			mLastConnectionError=ConnectionError.UnknownHostException;
			synchronized(mConnectState){mConnectState=ConnectState.OFFLINE;}
			synchronized(mConnectAction){mConnectAction=ConnectAction.NONE;}
			postNotifyListenersToUiThread();
		} catch (NoRouteToHostException e){
			mLastConnectionError=ConnectionError.NoRouteToHostException;
			synchronized(mConnectState){mConnectState=ConnectState.OFFLINE;}
			synchronized(mConnectAction){mConnectAction=ConnectAction.NONE;}
			postNotifyListenersToUiThread();
		} catch (ConnectException e){
			mLastConnectionError=ConnectionError.ConnectException;
			synchronized(mConnectState){mConnectState=ConnectState.OFFLINE;}
			synchronized(mConnectAction){mConnectAction=ConnectAction.NONE;}
			postNotifyListenersToUiThread();
		} catch (BadSignatureException e){
			mLastConnectionError=ConnectionError.BadSignatureException;
			synchronized(mConnectState){mConnectState=ConnectState.OFFLINE;}
			synchronized(mConnectAction){mConnectAction=ConnectAction.NONE;}
			postNotifyListenersToUiThread();
		
		// tut
		} catch (AuthenticationFailedException e){
			mLastConnectionError=ConnectionError.AuthenticationFailedException;	
			synchronized(mConnectState){mConnectState=ConnectState.OFFLINE;}
			synchronized(mConnectAction){mConnectAction=ConnectAction.NONE;}
			postNotifyListenersToUiThread();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			if(DEBUG){System.err.println(e);}
			
			mLastConnectionError=ConnectionError.UNKNOWN;
			mLastConnectionErrorString=e.toString();
			
			synchronized(mConnectState){mConnectState=ConnectState.OFFLINE;}
			synchronized(mConnectAction){mConnectAction=ConnectAction.NONE;}
			postNotifyListenersToUiThread();
		}
	}
	
	private void _sendMsg(RsMessage msg) {
		//allocate memory
		// 16 byte header + body
		ByteBuffer bb=ByteBuffer.allocate(16+msg.body.length);
		bb.putInt(MAGIC_CODE);
		bb.putInt(msg.msgId);
		bb.putInt(msg.reqId);
		bb.putInt(msg.body.length);
		bb.put(msg.body);
		try {
			if(DEBUG){System.err.println("RsCtrlService: _sendMsg() ...");}
			mOutputStream.write(bb.array());
			
			//System.out.println("sendRpc:");
			//System.out.println(util.byteArrayToHexString(bb.array()));
				
			if(DEBUG){System.err.println("RsCtrlService: _sendMsg(): success");}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			if(DEBUG){System.err.println(e);}
			mLastConnectionError=ConnectionError.SEND_ERROR;
			synchronized(mConnectState){mConnectState=ConnectState.OFFLINE;}
			synchronized(mConnectAction){mConnectAction=ConnectAction.NONE;}
			postNotifyListenersToUiThread();
		}
	}
	
	// allocate 4 Bytes for Magic Code
	private ByteBuffer inbuf=ByteBuffer.allocate(4);
	
	private enum InputState{
		BEGIN,HAVE_MAGIC_CODE,HAVE_MSG_ID,HAVE_REQ_ID,HAVE_BODY_SIZE
	}
	private InputState inputState=InputState.BEGIN;
	private int curMsgId;
	private int curReqId;
	private int curBodySize;
	private byte[] curBody;
	
	// blockiert nicht
	// aber: nicblockierend in einem eigenen thread ist schwachsinn,
	// deswegen auf blockierend umstellen, oder den thread weglassen und nichtblockierend pollen
	// problem: rausfinden wie das timeout für die blockierung in jaramiko ist
	// nachteil: blockierend blockiert auch alle anderen aktionen im thread
	//
	// neue erkenntnis:
	// http://lag.net/jaramiko/docs/net/lag/jaramiko/Channel.html#setTimeout(int)
	//
	// update: blockiert jetzt, jetzt weil ich auf bulk read umgestellt habe
	//
	private int _recvMsg(){
		try {
			switch(inputState){
				case BEGIN:
					while(mInputStream.available()>0 && inbuf.position()<4)
					{
						inbuf.put((byte) mInputStream.read());
					}
					if(inbuf.position()==4)
					{
						inbuf.rewind();
						if(inbuf.getInt()==MAGIC_CODE){
							inbuf=ByteBuffer.allocate(4);
							inputState=InputState.HAVE_MAGIC_CODE;
							System.out.println("received MAGIC_CODE");
						}
						else{
							System.out.println("Error: no MAGIC_CODE");
						}
					}
					break;
				case HAVE_MAGIC_CODE :
					while(mInputStream.available()>0 && inbuf.position()<4)
					{
						inbuf.put((byte) mInputStream.read());
					}
					if(inbuf.position()==4)
					{
						inbuf.rewind();
						curMsgId=inbuf.getInt();
						inbuf=ByteBuffer.allocate(4);
						inputState=InputState.HAVE_MSG_ID;
						System.out.print("received MSG_ID: ");
						System.out.print(curMsgId);
						System.out.println();
					}
					break;
				case HAVE_MSG_ID:
					while(mInputStream.available()>0 && inbuf.position()<4)
					{
						inbuf.put((byte) mInputStream.read());
					}
					if(inbuf.position()==4)
					{
						inbuf.rewind();
						curReqId=inbuf.getInt();
						inbuf=ByteBuffer.allocate(4);
						inputState=InputState.HAVE_REQ_ID;
						System.out.print("received REQ_ID: ");
						System.out.print(curReqId);
						System.out.println();
					}
					break;
				case HAVE_REQ_ID:
					while(mInputStream.available()>0 && inbuf.position()<4)
					{
						inbuf.put((byte) mInputStream.read());
					}
					if(inbuf.position()==4)
					{
						inbuf.rewind();
						curBodySize=inbuf.getInt();
						inbuf=ByteBuffer.allocate(curBodySize);
						inputState=InputState.HAVE_BODY_SIZE;
						System.out.print("received BODY_SIZE: ");
						System.out.print(curBodySize);
						System.out.println();
					}
					break;
				case HAVE_BODY_SIZE:
					// not blocking and fast
					int nobytestoread=mInputStream.available();
					if(nobytestoread>(curBodySize-inbuf.position())){
						nobytestoread=curBodySize-inbuf.position();
					}
					byte[] newbytes=new byte[nobytestoread];
					int nobytesread=mInputStream.read(newbytes,0,nobytestoread);
					inbuf.put(newbytes, 0, nobytesread);
					
					// maybe faster, but blocking
					/*
					byte[] bytes=new byte[curBodySize];
					int nobytesread=0;
					while(nobytesread<curBodySize){
						nobytesread=+mInputStream.read(bytes,nobytesread,curBodySize-nobytesread);
					}
					inbuf.put(bytes);
					*/
					/*
					 * to slow for megabytes
					 *
					while(mInputStream.available()>0 && inbuf.position()<curBodySize)
					{
						inbuf.put((byte) mInputStream.read());
					}*/
					
					if(inbuf.position()==curBodySize)
					{
						inbuf.rewind();
						curBody=inbuf.array();
						inbuf=ByteBuffer.allocate(4);
						inputState=InputState.BEGIN;
						if(curBodySize<1000){
							System.out.println("received complete Body:\n"+util.byteArrayToHexString(curBody));
						}else{
							System.out.println("received complete Body: bigger than 1000Bytes");
						}
						return curMsgId;
					}
					break;
				default:
					break;
				
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			if(DEBUG){System.err.println(e);}
			mLastConnectionError=ConnectionError.RECEIVE_ERROR;
			synchronized(mConnectState){mConnectState=ConnectState.OFFLINE;}
			synchronized(mConnectAction){mConnectAction=ConnectAction.NONE;}
			postNotifyListenersToUiThread();
		}
		return -1;
	}
	
}

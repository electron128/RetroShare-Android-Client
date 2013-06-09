package org.retroshare.android;

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

public class RsCtrlService implements Runnable
{
	private static final boolean DEBUG=true;
	
	public static final int MAGIC_CODE = 0x137f0001;
	public static final int RESPONSE=(0x01<<24);
	
	public static class RsMessage
	{
		public int msgId;
		public int reqId;
		public byte[] body;
	}
	
	public enum ConnectionEvent { SERVER_DATA_CHANGED, ERROR_WHILE_CONNECTING, CONNECTED,ERROR_DISCONNECTED }

	/**
	 * Stuff that need to receive info about connection with RetroShare core
	 * should implement this interface ( see LoginActivity for example ).
	 * The fact that we have a public inner interface make perfectly sense because
	 * we want that only code that can access RSCtrlService can be a Listener
	 * see http://stackoverflow.com/a/209158 for more details
	 */
	public interface RsCtrlServiceListener
	{
		/**
		 * Callback invoked on all registered listeners each time something happens
		 * @param ce a ConnectionEvent containing information about what happened 
		 */
		public void onConnectionStateChanged(ConnectionEvent ce);
	}
	
	private Set<RsCtrlServiceListener> mListeners = new HashSet<RsCtrlServiceListener>();
	public void registerListener(RsCtrlServiceListener l) { mListeners.add(l); }
	public void unregisterListener(RsCtrlServiceListener l){ mListeners.remove(l); }
	private void notifyListenersOnConnectionStateChanged(ConnectionEvent ce)
	{
		for(RsCtrlServiceListener l:mListeners)
		{
			l.onConnectionStateChanged(ce);
		}
	}

	private void postNotifyListenersToUiThreadOnConnectionStateChanged(final ConnectionEvent ce)
	{
		mUiThreadHandler.postToUiThread(new Runnable() { @Override public void run(){ notifyListenersOnConnectionStateChanged(ce); }});
	}
	
	public enum ConnectState{ ONLINE, OFFLINE }
	
	public enum ConnectAction{ CONNECT, DISCONNECT, NONE }
	
	/**
	 * Connection errors enum
	 */
	public enum ConnectionError
	{
		/**
		 * No error
		 */
		NONE,
		
		/**
		 * The IP address of the host could not be determined.
		 */
		UnknownHostException,

		/**
		 * Signals that an error occurred while attempting to connect a socket
		 * to a remote address and port. Typically, the remote host cannot be 
		 * reached because of an intervening firewall, or if an intermediate 
		 * router is down.
		 */
		NoRouteToHostException,
		
		/**
		 * Signals that an error occurred while attempting to connect a socket 
		 * to a remote address and port. Typically, the connection was refused 
		 * remotely (e.g., no process is listening on the remote address/port).
		 */
		ConnectException,
		
		/**
		 * Hostkey mismatch, hostkey changed since first connection this can mean that someone is trying to do a Man In The Middle attack
		 */
		BadSignatureException,
		
		/**
		 * Wrong user name or password
		 */
		AuthenticationFailedException,
		
		/**
		 * IO Level exception caught sending data
		 */
		SEND_ERROR,
		
		/**
		 * IO Level exception caught receiving data
		 */
		RECEIVE_ERROR,
		
		/**
		 * Something is not working but we don't know what/why
		 */
		UNKNOWN,
	}
	
	public ConnectionError getLastConnectionError(){ return mLastConnectionError; } /** @return Last connection error */
	public String getLasConnectionErrorString(){ return mLastConnectionErrorString;	} /** @return Last connection error string */
	
	/*************************************/
	// dont know if i have to worry with enums and threads
	private volatile ConnectState mConnectState=ConnectState.OFFLINE;
	private volatile ConnectAction mConnectAction=ConnectAction.NONE;
	private volatile ConnectionError mLastConnectionError=ConnectionError.NONE;
	private volatile String mLastConnectionErrorString="";
	
	// accessed from worker and ui thread, so we have to use synchronized()
	
	private UiThreadHandlerInterface mUiThreadHandler;
	private Thread mThread;
	volatile static boolean runThread = false;
	
	private RsServerData mServerData = new RsServerData();
	
	/*************************************/
	// accessed from worker thread only
	private Socket mSocket;
	private ClientTransport mTransport;
	private Channel mChannel;
	private InputStream mInputStream;
	private OutputStream mOutputStream;
	/*************************************/
	
	private Set<ServiceInterface> Services  =new HashSet<ServiceInterface>();
	public ChatService chatService;
	public PeersService peersService;
	public FilesService filesService;
	public SearchService searchService;
	
	/**
	 * Initialize the RetroShare Control Service
	 * @param h reference to ui thread handler to post notification to GUI when we have something ready
	 */
	RsCtrlService(UiThreadHandlerInterface h)
	{
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
		// preload chatobby list, because first request returns just an empty list
		chatService.updateChatLobbies();
	}
	
	public void destroy(){ runThread=false;	}
	

	/**
	 * Set data of the server we will connect to
	 * @param d RsServerData containing the data relative to the server you want to connect to ( host, port, user... )
	 */
	public void setServerData(RsServerData d)
	{
		// The passed RsServerData d is cloned, so the caller ( outside thread ) can't change our serverdata which is used in our workerthread
		synchronized(mServerData){ mServerData=d.clone(); }
		notifyListenersOnConnectionStateChanged(ConnectionEvent.SERVER_DATA_CHANGED);
	}

	/**
	 * Get Data relative to our RetroShare Server
	 * @return RsServerData containing data relative to actual RetroShare server ( host, port, user... )
	 */
	public RsServerData getServerData()
	{
		synchronized(mServerData){ return mServerData.clone(); }
	}

	/**
	 * Connect to the server previously set with setServerData setServerData(RsServerData d)
	 */
	public void connect()
	{
		if(DEBUG){System.err.println("RsCtrlService: connect()");}
		
		synchronized(mConnectAction){ mConnectAction = ConnectAction.CONNECT; }
	}
	
	/**
	 * Disconnect from the actual connected server
	 */
	public void disconnect(){
		if(DEBUG){System.err.println("RsCtrlService: disconnect()");}
		
		synchronized(mConnectAction){ mConnectAction=ConnectAction.DISCONNECT; }
	}
	
	/**
	 * @return True if we are connecter to RetroShare server, false otherwise
	 */
	public boolean isOnline()
	{
		synchronized(mConnectState){ return (mConnectState == ConnectState.ONLINE); }
	}
	
	// use with synchronized()
	private ArrayList<RsMessage> outMsgList = new ArrayList<RsMessage>();
	private int msgCounter=0;
	
	//holds pairs of <reqId,Handler>
	private HashMap<Integer,RsMessageHandler> msgHandlers= new HashMap<Integer,RsMessageHandler>();
	//holds pairs of <msgId,handler>
	private HashMap<Integer,RsMessageHandler> msgHandlersById= new HashMap<Integer,RsMessageHandler>();
	
	/**
	 * Send a message associating an handler for the eventual reply
	 * The RsMessageHandler rsHandleMsg method will be called when a response message ( a received message with same reqId ) is eventually received with the received message as parameter
	 * @param msg The RsMessage to send
	 * @param h Handler to handle the eventual response message
	 * @return the request id associated with the message 
	 */
	public int sendMsg(RsMessage msg, RsMessageHandler h)
	{
		int reqId=0;
		synchronized(outMsgList)
		{
			msgCounter++;
			msg.reqId = msgCounter;
			outMsgList.add(msg);
			reqId = msg.reqId;
		}
		synchronized(msgHandlers)
		{
			msgHandlers.put(reqId, h);
		}
		return reqId;
	}
	public int sendMsg(RsMessage msg){ return sendMsg(msg,null); }
	
	public void registerMsgHandler(int msgId,RsMessageHandler h)
	{
		synchronized(msgHandlersById){ msgHandlersById.put(msgId, h); }
	}
	
	public RsMessageHandler getHandler(int msgId) { return msgHandlersById.get(msgId); }
	
	@Override
	public void run()
	{
		while(runThread)
		{
			// TODO It isn't better to make time between update configurable ?
			try{ Thread.sleep(50); } catch (InterruptedException e) { System.err.print(e); }
			
			boolean connect, disconnect, isonline;
			synchronized(mConnectAction){ connect    = (mConnectAction == ConnectAction.CONNECT); }
			synchronized(mConnectAction){ disconnect = (mConnectAction == ConnectAction.DISCONNECT); }
			synchronized(mConnectState) { isonline   = (mConnectState  == ConnectState.ONLINE); }
			
			if(connect){ _connect(); }
			
			if(isonline)
			{
				// Send first outgoing message
				{
					RsMessage msg = null;
					synchronized(outMsgList) { if( !outMsgList.isEmpty() ) msg = outMsgList.remove(0); }
					if(msg != null) _sendMsg(msg);
				}
				
				// Receive one incoming message
				{
				
					int msgType = _recvMsg();
					if(msgType != -1)
					{
						final RsMessage msg = new RsMessage();
						msg.msgId = curMsgId;
						msg.reqId = curReqId;
						msg.body  = curBody;
						RsMessageHandler h = null;
						synchronized(msgHandlers) { h = msgHandlers.remove(msg.reqId); }
						if( h != null )
						{
							if(DEBUG){System.err.println("RsCtrlService: run(): received Msg with reqId Handler, will now post Msg to UI Thread");}
							h.setMsg(msg);
							h.post(h); // post to ui thread
						}
						else
						{
							synchronized(msgHandlersById) { h = msgHandlersById.get(msg.msgId); }
							if( h != null )
							{
								if(DEBUG){System.err.println("RsCtrlService: run(): received Msg with msgId Handler, will now post Msg to UI Thread");}
								h.setMsg(msg);
								h.post(h); // post to ui thread
							}
							else { if(DEBUG){System.err.println("RsCtrlService: run(): Error: msgHandler not found");}}
						}
						
						// TODO it seams to me that we write code similar to this more than 1 time, check if it is generalizable and find a solution to avoid rewriting same code
						// tell every service about the message
						mUiThreadHandler.postToUiThread(new Runnable() { @Override public void run() {for(ServiceInterface service:Services) { service.handleMessage(msg); }}});
					}
				}
			}
			
			if(disconnect) { _disconnect(); }
		}
	}

	private void _connect()
	{
		try {
			synchronized(mServerData)
			{
				if(DEBUG){System.err.println("RsCtrlService: _connect() ...");}
				
				boolean newHostKey=false;
				if( mServerData.hostkey == null ){ newHostKey = true; }
				
				mSocket = new Socket();
				//TODO Avoid hardcoding timeout, moreover same value is used multiple times in the code
				mSocket.connect(new InetSocketAddress(mServerData.hostname, mServerData.port), 2000);
				// TODO try to find when crai in jaramiko is constructed
				// error here
				//System.err.println("RsCtrlService._connect: mServerData.hostkey="+mServerData.hostkey);
				mTransport = new ClientTransport(mSocket);
				// no more error here
				System.err.println("RsCtrlService._connect: mServerData.hostkey="+mServerData.hostkey);
				mTransport.start(mServerData.hostkey, 2000);
				if(newHostKey){ mServerData.hostkey = mTransport.getRemoteServerKey(); }
				mTransport.authPassword(mServerData.user, mServerData.password, 2000);
				mChannel = mTransport.openSession(2000);
				mChannel.invokeShell(2000);
				mInputStream = mChannel.getInputStream();
				mOutputStream = mChannel.getOutputStream();
				
				synchronized(mConnectState){  mConnectState = ConnectState.ONLINE; }
				synchronized(mConnectAction){ mConnectAction = ConnectAction.NONE; }

				postNotifyListenersToUiThreadOnConnectionStateChanged(ConnectionEvent.CONNECTED);

				if(DEBUG){System.err.println("RsCtrlService: _connect(): success");}
			}
		} 
		
		// catch zeugs tut ned
		//noch zu hndeln:
		// WLAN aus: Java.net.SocketException: Network unreachable
		// wlan an aber falsche ip: Java.net.SocketTimeoutException: Transport endpoint is not connected
		// rs-nogui hat kein cleanup gemacht: net.lag.jaramiko.SSHException: Timeout
		// falscher hostkey: net.lag.jaramiko.SShException: Bad host key from server
		
		catch (UnknownHostException e)
		{
			mLastConnectionError=ConnectionError.UnknownHostException;
			synchronized(mConnectState){mConnectState=ConnectState.OFFLINE;}
			synchronized(mConnectAction){mConnectAction=ConnectAction.NONE;}
			postNotifyListenersToUiThreadOnConnectionStateChanged(ConnectionEvent.ERROR_WHILE_CONNECTING);
		}
		catch (NoRouteToHostException e)
		{
			mLastConnectionError=ConnectionError.NoRouteToHostException;
			synchronized(mConnectState){mConnectState=ConnectState.OFFLINE;}
			synchronized(mConnectAction){mConnectAction=ConnectAction.NONE;}
			postNotifyListenersToUiThreadOnConnectionStateChanged(ConnectionEvent.ERROR_WHILE_CONNECTING);
		}
		catch (ConnectException e)
		{
			mLastConnectionError=ConnectionError.ConnectException;
			synchronized(mConnectState){mConnectState=ConnectState.OFFLINE;}
			synchronized(mConnectAction){mConnectAction=ConnectAction.NONE;}
			postNotifyListenersToUiThreadOnConnectionStateChanged(ConnectionEvent.ERROR_WHILE_CONNECTING);
		}
		catch (BadSignatureException e)
		{
			mLastConnectionError=ConnectionError.BadSignatureException;
			synchronized(mConnectState){mConnectState=ConnectState.OFFLINE;}
			synchronized(mConnectAction){mConnectAction=ConnectAction.NONE;}
			postNotifyListenersToUiThreadOnConnectionStateChanged(ConnectionEvent.ERROR_WHILE_CONNECTING);
		
		// tut
		}
		catch (AuthenticationFailedException e)
		{
			mLastConnectionErrorString="wrong password or username";
			mLastConnectionError=ConnectionError.AuthenticationFailedException;	
			synchronized(mConnectState){mConnectState=ConnectState.OFFLINE;}
			synchronized(mConnectAction){mConnectAction=ConnectAction.NONE;}
			postNotifyListenersToUiThreadOnConnectionStateChanged(ConnectionEvent.ERROR_WHILE_CONNECTING);
			
		}
		catch (IOException e)
		{
			if(DEBUG){System.err.println(e);}
			
			mLastConnectionError=ConnectionError.UNKNOWN;
			mLastConnectionErrorString=e.toString();
			
			synchronized(mConnectState){mConnectState=ConnectState.OFFLINE;}
			synchronized(mConnectAction){mConnectAction=ConnectAction.NONE;}
			postNotifyListenersToUiThreadOnConnectionStateChanged(ConnectionEvent.ERROR_WHILE_CONNECTING);
		}
	}
	
	private void _disconnect()
	{
		if(DEBUG){System.err.println("RsCtrlService: _disconnect() ...");}
		
		synchronized(mConnectState){mConnectState=ConnectState.OFFLINE;}
		synchronized(mConnectAction){mConnectAction=ConnectAction.NONE;}
		
		mLastConnectionError=ConnectionError.NONE;
		
		mInputStream=null;
		mOutputStream=null;
		
		if(mChannel != null){ mChannel.close(); }
		mChannel = null;
		
		if(mTransport != null){ mTransport.close(); }
		mTransport=null;
		
		try { mSocket.close(); } catch (IOException e) { e.printStackTrace(); }
		mSocket=null;
		
		/*
		mSocket;
		mTransport;
		mChannel;
		mInputStream;
		mOutputStream;
		*/
		
	}
	
	private void _sendMsg(RsMessage msg)
	{
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
			postNotifyListenersToUiThreadOnConnectionStateChanged(ConnectionEvent.ERROR_DISCONNECTED);
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
	// problem: rausfinden wie das timeout f�r die blockierung in jaramiko ist
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
			postNotifyListenersToUiThreadOnConnectionStateChanged(ConnectionEvent.ERROR_DISCONNECTED);
		}
		return -1;
	}
	
}

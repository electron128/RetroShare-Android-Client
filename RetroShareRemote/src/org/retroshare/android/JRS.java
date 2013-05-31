package org.retroshare.android;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import net.lag.jaramiko.Channel;
import net.lag.jaramiko.ClientTransport;
import net.lag.jaramiko.PKey;


public class JRS {
	private final int MAGIC_CODE=0x137f0001;
	private Socket socket;
	private ClientTransport transport;
	private Channel channel;
	private InputStream inputStream;
	private OutputStream outputStream;
	private int msgcounter=0;
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
	
	public JRS(String hostname, int port, PKey hostkey, String username, String password){
		try {
			socket=new Socket();
			socket.connect(new InetSocketAddress(hostname,port), 1000);
			transport=new ClientTransport(socket);
			transport.start(hostkey, 1000);
			transport.authPassword(username, password, 1000);
			channel=transport.openSession(1000);
			channel.invokeShell(1000);
			inputStream=channel.getInputStream();
			outputStream=channel.getOutputStream();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	};
	public void sendRpc(int msgId,byte[] data){
		ByteBuffer bb=ByteBuffer.allocate(16+data.length);
		bb.order(ByteOrder.BIG_ENDIAN);
		bb.putInt(MAGIC_CODE);
		bb.putInt(msgId);
		msgcounter+=1;
		bb.putInt(msgcounter);
		bb.putInt(data.length);
		bb.put(data);
		try {
			outputStream.write(bb.array());
			System.out.println("sendRpc:");
			System.out.println(byteArrayToHexString(bb.array()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public int recvRpcs(){
		try {
			switch(inputState){
				case BEGIN:
					while(inputStream.available()>0 && inbuf.position()<4)
					{
						inbuf.put((byte) inputStream.read());
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
					while(inputStream.available()>0 && inbuf.position()<4)
					{
						inbuf.put((byte) inputStream.read());
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
					while(inputStream.available()>0 && inbuf.position()<4)
					{
						inbuf.put((byte) inputStream.read());
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
					while(inputStream.available()>0 && inbuf.position()<4)
					{
						inbuf.put((byte) inputStream.read());
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
					while(inputStream.available()>0 && inbuf.position()<curBodySize)
					{
						inbuf.put((byte) inputStream.read());
					}
					if(inbuf.position()==curBodySize)
					{
						inbuf.rewind();
						curBody=inbuf.array();
						inbuf=ByteBuffer.allocate(4);
						inputState=InputState.BEGIN;
						System.out.println("received complete Body:");
						System.out.print(byteArrayToHexString(curBody));
						System.out.println();
						return curMsgId;
					}
					break;
				default:
					break;
				
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}
	public byte[] getNextRpc(){
		return curBody;
	}
	
	public void recvToHex(){
		while(true){
			try {
				int i;
				i = inputStream.read();
				String s=Integer.toString(i,16);
				if(s.length()==1){
					System.out.print("0");
				}
				System.out.print(s);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	public boolean readMagic(){
		
		
		//ByteBuffer bbm=ByteBuffer.allocate(4);
		
		byte[] b=new byte[4];
		try {
			inputStream.read(b);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(b[0]!=0x13){
			return false;
		}
		if(b[1]!=0x7f){
			return false;
		}
		return true;
	}
	public void send(byte[] b){
		try {
			outputStream.write(b);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public boolean readMessage(){
		return false;
	}
	public void get(){
		//byte[] b=new byte[10];
		try {
			while(true){
				int i=inputStream.read();
				System.out.print((char)i);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//System.out.println(b);
	}
	
	  public static String byteArrayToHexString(byte[] b) {
		    StringBuffer sb = new StringBuffer(b.length * 2);
		    for (int i = 0; i < b.length; i++) {
		      int v = b[i] & 0xff;
		      if (v < 16) {
		        sb.append('0');
	      }
	      sb.append(Integer.toHexString(v));
	    }
	    return sb.toString().toUpperCase();
	  }

}

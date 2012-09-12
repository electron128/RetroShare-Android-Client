package com.example.retroshare.remote;

import java.io.IOException;
import java.io.NotActiveException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import net.lag.jaramiko.PKey;
import net.lag.jaramiko.SSHException;

// TODO: test serialization and clone()
// http://java.sun.com/developer/technicalArticles/Programming/serialization/
public class RsServerData implements Serializable, Cloneable{
	private static final boolean DEBUG=false;
	private static final long serialVersionUID = 0;
	
	public String user;
	public String password;
	public String hostname;
	public int port;
	public transient PKey hostkey;
	
	@Override
	public String toString(){
		//if(hostkey!=null){
			try{
				return "\""+user+":"+password+"@"+hostname+":"+Integer.toString(port)+" key="+hostkey+"\"";
			} catch(NullPointerException e){
				//System.err.println("NullPointerException in RsServerData.toString()");
				return "\""+user+":"+password+"@"+hostname+":"+Integer.toString(port)+" key=Error in RsServerData.toString() \"";
			}/*
		}else{
			return "\""+user+":"+password+"@"+hostname+":"+Integer.toString(port)+" key=RsServerData::toString: hostkey=null\"";
		}*/
	}
	
	private void writeObject(ObjectOutputStream out){
		if(DEBUG){System.err.println("RsServerData::writeObject: "+this);}
		try {
			out.defaultWriteObject();
			if(hostkey!=null){
				out.writeBoolean(true);
				out.writeObject(hostkey.toByteArray());
			}
			else{
				out.writeBoolean(false);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
	}
	
	private void readObject(ObjectInputStream in) throws NotActiveException, IOException, ClassNotFoundException{
		if(DEBUG){System.err.println("RsServerData::readObject begin: ");}
		in.defaultReadObject();
		if(in.readBoolean()){
			if(DEBUG){System.err.println("RsServerData::readObject: PKey!=null");}
			byte[] b=(byte[]) in.readObject();
			hostkey= PKey.createFromData(b);
		}
		else{
			if(DEBUG){System.err.println("RsServerData::readObject: PKey==null");}
			hostkey=null;
		}
		if(DEBUG){System.err.println("RsServerData::readObject end: "+this);}
	}
	
	protected RsServerData clone(){
		RsServerData d=new RsServerData();
		d.user=user;
		d.password=password;
		d.hostname=hostname;
		d.port=port;
		if(hostkey!=null){
			try {
				d.hostkey=PKey.createFromData(hostkey.toByteArray());
			} catch (SSHException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else{
			d.hostkey=null;
		}
		return d;
	}
}

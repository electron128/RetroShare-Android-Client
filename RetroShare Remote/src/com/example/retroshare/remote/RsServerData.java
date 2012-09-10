package com.example.retroshare.remote;

import java.io.IOException;
import java.io.NotActiveException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import net.lag.jaramiko.PKey;

// todo: test test serialise hostkey
// todo: implement cloneable
// http://java.sun.com/developer/technicalArticles/Programming/serialization/
public class RsServerData implements Serializable {
	private static final long serialVersionUID = 0;
	
	public String user;
	public String password;
	public String hostname;
	public int port;
	public transient PKey hostkey;
	
	@Override
	public String toString(){
		return "\""+user+":"+password+"@"+hostname+":"+Integer.toString(port)+hostkey+"\"";
		
	}
	
	private void writeObject(ObjectOutputStream out) throws IOException{
		out.defaultWriteObject();
		if(hostkey!=null){
			out.writeBoolean(true);
			out.writeObject(hostkey.toByteArray());
		}
		else{
			out.writeBoolean(false);
		}
		
	}
	
	private void readObject(ObjectInputStream in) throws NotActiveException, IOException, ClassNotFoundException{
		in.defaultReadObject();
		if(in.readBoolean()){
			byte[] b=(byte[]) in.readObject();
			hostkey= PKey.createFromData(b);
		}
		else{
			hostkey=null;
		}
	}
}

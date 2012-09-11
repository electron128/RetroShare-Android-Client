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
	private static final long serialVersionUID = 0;
	
	public String user;
	public String password;
	public String hostname;
	public int port;
	public transient PKey hostkey;
	
	@Override
	public String toString(){
		return "\""+user+":"+password+"@"+hostname+":"+Integer.toString(port)+" key="+hostkey+"\"";
		
	}
	
	private void writeObject(ObjectOutputStream out){
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
		in.defaultReadObject();
		if(in.readBoolean()){
			byte[] b=(byte[]) in.readObject();
			hostkey= PKey.createFromData(b);
		}
		else{
			hostkey=null;
		}
	}
	
	protected RsServerData clone(){
		RsServerData d=new RsServerData();
		d.user=user;
		d.password=password;
		d.hostname=hostname;
		d.port=port;
		try {
			d.hostkey=PKey.createFromData(hostkey.toByteArray());
		} catch (SSHException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return d;
	}
}

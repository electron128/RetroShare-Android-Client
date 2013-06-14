package org.retroshare.android;

public class bitdht {
	public native static String getIp(String nodeId);
	
	   static {
	       System.loadLibrary("bitdht");
	   }
}

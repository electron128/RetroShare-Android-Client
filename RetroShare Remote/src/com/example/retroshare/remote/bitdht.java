package com.example.retroshare.remote;

public class bitdht {
	public native static String getIp(String nodeId);
	
	   static {
	       System.loadLibrary("bitdht");
	   }
}

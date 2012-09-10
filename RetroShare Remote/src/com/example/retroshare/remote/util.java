package com.example.retroshare.remote;

public class util {
	
	// stolen from the internet
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

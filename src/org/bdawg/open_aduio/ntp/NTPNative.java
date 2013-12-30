package org.bdawg.open_aduio.ntp;

public class NTPNative {
	public native long getOffset();
	
	static {
        System.loadLibrary("ntp");
    }        
	
	public void print () {
	    long off = getOffset();
	    System.out.println(off);
	 }
}

package org.bdawg.open_audio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketAddress;

public abstract class Utils {
	
	private static String eMac = null;
	private static InetAddress eLocalAddress = null;
	
	public static String getMacAddresses() throws IOException {
		if (eMac != null){
			return eMac;
		}
		Socket socket = new Socket();
		SocketAddress endpoint = new InetSocketAddress("oa.bdawg.org", 80);
		socket.connect(endpoint);
		InetAddress localAddress = socket.getLocalAddress();
		eLocalAddress = localAddress;
		socket.close();
		NetworkInterface ni = NetworkInterface.getByInetAddress(localAddress);
		byte[] mac = ni.getHardwareAddress();
		StringBuilder s = new StringBuilder();
		if (mac != null) {
			for (int j = 0; j < mac.length; j++) {
				String part = String.format("%02X%s", mac[j],
						(j < mac.length - (1)) ? "" : "");
				s.append(part);
			}
			eMac = s.toString().toLowerCase();
			return eMac;
		} else {
			throw new IOException("Unable to find mac address");
		}
	}
	
	public static InetAddress getLocalInetAddress() throws IOException{
		if (eLocalAddress != null){
			return eLocalAddress;
		}
		Socket socket = new Socket();
		SocketAddress endpoint = new InetSocketAddress("oa.bdawg.org", 80);
		socket.connect(endpoint);
		InetAddress localAddress = socket.getLocalAddress();
		eLocalAddress = localAddress;
		socket.close();
		return eLocalAddress;
	}
}

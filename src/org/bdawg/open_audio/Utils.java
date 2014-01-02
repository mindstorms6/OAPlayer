package org.bdawg.open_audio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bdawg.open_audio.OpenAudioProtos.KVType;

public abstract class Utils {
	
	
	public final class OAConstants{

	    public static final String NOT_OWNED_STRING = "NOT_OWNED";
	    public static final String TRUE_STRING = "true";
	    
	    public static final String DL_TYPE_SIMPLE_URL = "GET_URL";
	    public static final String DL_TYPE_TORRENT = "TORRENT";
	    public static final String DL_TYPE_FS = "FS";

	    public static final String META_SIMPLE_URL_LOCATION_KEY = "URI";
	    public static final String META_SIMPLE_URL_CAN_PLAY_DIRECT_KEY = "CAN_PLAY_DIRECT";



	    public static final String BASE_TOPIC = "/home/breland/pi_audio/";

	    public static final String WS_HOST = "http://oa.bdawg.org/";
	    

	}
	
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
	
	public static Map<String,String> kvListToMap(List<KVType> allKVs){
		Map<String,String> tr = new HashMap<String,String>();
		for (KVType kv : allKVs){
			if (!tr.containsKey(kv.getKey())){
				tr.put(kv.getKey(), kv.getValue());
			}
		}
		return tr;
	}
	
	public static List<KVType> mapToKVList(Map<String, String> in){
		List<KVType> tr = new ArrayList<KVType>();
		for (Entry<String,String> entry : in.entrySet()){
			tr.add(KVType.newBuilder().setKey(entry.getKey()).setValue(entry.getValue()).build());
		}
		return tr;
	}
}

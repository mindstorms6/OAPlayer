package org.bdawg.open_audio;

import java.io.IOException;
import java.util.Properties;

public class PropertyManager {

	private static Properties storedProps;
	private static boolean inited = false;
	private static String DEFAULT_IF_EMPTY = "j39sjgljzj3lqql35fs";
	
	public enum PropertyKey{
		MQTT_HOST,
		MQTT_PORT,
		MQTT_BASE_TOPIC,
		MQTT_SCHEME,
		NTP_HOST,
		WS_HOST
	}
	
	private PropertyManager() throws IOException{
	}
	
	private synchronized static void checkInit(){
		if (!inited){
			storedProps = new Properties();
			try {
				storedProps.load(PropertyManager.class.getResourceAsStream("default.properties"));
			} catch (IOException e) {
				//Default props doesn't exist, empty props
			} catch (NullPointerException nex){
				
			} finally {
				inited = true;
			}
		}
	}
	
	public static int getIntForKey(PropertyKey key, int defaultValue){
		checkInit();
		String out = storedProps.getProperty(key.toString(), DEFAULT_IF_EMPTY);
		if (out.equals(DEFAULT_IF_EMPTY)){
			return defaultValue;
		} else {
			try {
				return Integer.parseInt(out);
			} catch (NumberFormatException nfx){
				return defaultValue;
			}
		}
	}
	
	public static String getStringForKey(PropertyKey key, String defaultValue){
		checkInit();
		String out = storedProps.getProperty(key.toString(), DEFAULT_IF_EMPTY);
		if (out.equals(DEFAULT_IF_EMPTY)){
			return defaultValue;
		} else {
			return out;
		}
	}
	
	public static boolean getBooleanForKey(PropertyKey key, boolean defaultValue){
		checkInit();
		String out = storedProps.getProperty(key.toString(), DEFAULT_IF_EMPTY);
		if (out.equals(DEFAULT_IF_EMPTY)){
			return defaultValue;
		} else {
			try {
				return Boolean.parseBoolean(out);
			} catch (NumberFormatException nfx){
				return defaultValue;
			}
		}
	}
}

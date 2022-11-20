package project.cs249.src.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Utils {
    public static byte[] hexToBytes(String str_hex){
       byte[] bytes_ret=new byte[str_hex.length()/2];
       for (int i = 0; i < bytes_ret.length; i++) {
            int index = i * 2;
        
            // Using parseInt() method of Integer class
            int val = Integer.parseInt(str_hex.substring(index, index + 2), 16);
            bytes_ret[i] = (byte)val;
        }
        return bytes_ret;
    }

    public static String bytesToHex(byte[] bytes_in){
        StringBuilder sb_hex=new StringBuilder();
        for(byte b:bytes_in){
            String hex_b=String.format("%02x",b);
            sb_hex.append(hex_b);
        }
        return sb_hex.toString();
    }

    public static String dateTimeToHex(){
        long millis = Instant.now().toEpochMilli();
        String hexMillis = Long.toHexString(millis);
        return hexMillis;
    }

    public static LocalDateTime hexToDateTime(String str_id){
        long convertedMillis=Long.decode("0x"+str_id);
        Instant instant=Instant.ofEpochMilli(convertedMillis);
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    public static InetAddress getHostInetAddress() throws UnknownHostException {
		InetAddress inetAddr = InetAddress.getLocalHost();
		return inetAddr;
	}

	public static String getHostInetName() throws UnknownHostException {
		String inetName = InetAddress.getLocalHost().getHostName();
		return inetName;
	}

    public static String mapToString(Map<String, ?> map) {
        StringBuilder mapAsString = new StringBuilder("{");
        for (String key : map.keySet()) {
            mapAsString.append(key + "=" + map.get(key) + ", ");
        }
        mapAsString.delete(mapAsString.length()-2, mapAsString.length()).append("}");
        return mapAsString.toString();
    }

    public static Map<String, String> stringToMap(String value){
        value = value.substring(1, value.length()-1);
        String[] keyValuePairs = value.split(",");
        Map<String,String> map = new HashMap<>();

        for(String pair : keyValuePairs)
        {
            String[] entry = pair.split("=");
            map.put(entry[0].trim(), entry[1].trim());
        }
        return map;
    }

    /**
     * random generates a port between min - max for dev purpose 
     * @return a string of port
     */
    public static String getRandomPort(int minPort, int maxPort){
        Random random=new Random();
        return String.valueOf(random.nextInt(maxPort-minPort+1)+minPort);
    }

    public static boolean isInRange(int num, int start, int end, boolean rightIncluded){
        if(rightIncluded==false){
            if(start<end) return num>=start && num<=end;
            else return num>=start || num<end;
        }
        else{
            if(start<end) return num>=start && num<end;
            else return num>=start || num<end;
        }

    }
}

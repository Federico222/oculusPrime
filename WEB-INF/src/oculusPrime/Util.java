package oculusPrime;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import oculusPrime.State.values;

public class Util {
	
	public final static String sep = System.getProperty("file.separator");

	public static final int PRECISION = 3;	
	public static final long ONE_DAY = 86400000;
	public static final long ONE_MINUTE = 60000;
	public static final long TWO_MINUTES = 120000;
	public static final long FIVE_MINUTES = 300000;
	public static final long TEN_MINUTES = 600000;
	public static final long ONE_HOUR = 3600000;
		
	public static final long MAX_lOG_MBYTES = 70;  
	
	static final int MAX_HISTORY = 50;
	static Vector<String> history = new Vector<String>(MAX_HISTORY);
	
	static String jettyPID = getJettyPID();
	static String redPID = getRed5PID();
	private static boolean shuttingdown = false;
	private static Process archiveProc = null; 
	private static boolean busyArchiving;

	public static void delay(long delay) {
		try {
			Thread.sleep(delay);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	public static void delay(int delay) {
		try {
			Thread.sleep(delay);
		} catch (Exception e) {
			printError(e);
		}
	}

	public static String getTime() {
        Date date = new Date();
		return date.toString();
	}

	public static String getDateStamp() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-M-dd_HH-mm-ss");
		Calendar cal = Calendar.getInstance();
		return dateFormat.format(cal.getTime());
	}

	/**
	 * Returns the specified double, formatted as a string, to n decimal places,
	 * as specified by precision.
	 * <p/>
	 * ie: formatFloat(1.1666, 1) -> 1.2 ie: formatFloat(3.1666, 2) -> 3.17 ie:
	 * formatFloat(3.1666, 3) -> 3.167
	 */
	public static String formatFloat(double number, int precision) {

		String text = Double.toString(number);
		if (precision >= text.length()) {
			return text;
		}

		int start = text.indexOf(".") + 1;
		if (start == 0)
			return text;

		if (precision == 0) {
			return text.substring(0, start - 1);
		}

		if (start <= 0) {
			return text;
		} else if ((start + precision) <= text.length()) {
			return text.substring(0, (start + precision));
		} else {
			return text;
		}
	}

	public static String formatFloat(String text, int precision) {
		int start = text.indexOf(".") + 1;
		if (start == 0) return text;

		if (precision == 0) return text.substring(0, start - 1);
	
		if (start <= 0) {
			return text;
		} else if ((start + precision) <= text.length()) {
			return text.substring(0, (start + precision));
		} else {
			return text;
		}
	}
	
	/**
	 * Returns the specified double, formatted as a string, to n decimal places,
	 * as specified by precision.
	 * <p/>
	 * ie: formatFloat(1.1666, 1) -> 1.2 ie: formatFloat(3.1666, 2) -> 3.17 ie:
	 * formatFloat(3.1666, 3) -> 3.167
	 */
	public static String formatFloat(double number) {

		String text = Double.toString(number);
		if (PRECISION >= text.length()) {
			return text;
		}

		int start = text.indexOf(".") + 1;
		if (start == 0)
			return text;

		if (start <= 0) {
			return text;
		} else if ((start + PRECISION) <= text.length()) {
			return text.substring(0, (start + PRECISION));
		} else {
			return text;
		}
	}

	public static boolean copyfile(String srFile, String dtFile) {
		try {
			
			File f1 = new File(srFile);
			File f2 = new File(dtFile);
			InputStream in = new FileInputStream(f1);

			// Append
			OutputStream out = new FileOutputStream(f2, true);

			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			in.close();
			out.close();

		} catch (Exception e) {
			System.out.println(e.getMessage());
			return false;
		}

		// file copied
		return true;
	}
	
	/**
	 * Run the given text string as a command on the host computer. 
	 * 
	 * @param args is the command to run, like: "restart
	 * 
	 */
	public static void systemCallBlocking(final String args) {
		try {	
			
			long start = System.currentTimeMillis();
			Process proc = Runtime.getRuntime().exec(args);
			BufferedReader procReader = new BufferedReader(
					new InputStreamReader(proc.getInputStream()));

			String line = null;
			System.out.println(proc.hashCode() + "OCULUS: exec():  " + args);
			while ((line = procReader.readLine()) != null)
				System.out.println(proc.hashCode() + " systemCallBlocking() : " + line);
			
			proc.waitFor(); // required for linux else throws process hasn't terminated error
			System.out.println("OCULUS: process exit value = " + proc.exitValue());
			System.out.println("OCULUS: blocking run time = " + (System.currentTimeMillis()-start) + " ms");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}	

	/**
	 * Run the given text string as a command on the windows host computer. 
	 * 
	 * @param str is the command to run, like: "restart
	 *  
	 */
	public static void systemCall(final String str){
		try {
			Runtime.getRuntime().exec(str); 
		} catch (Exception e) {
			printError(e);
		}
	}

//	/** @return a list of ip's for this local network */ 
//	public static String getLocalAddress() {
//		String address = "";
//		try {
//			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
//			if (interfaces != null)
//				while (interfaces.hasMoreElements()) {
//					NetworkInterface ni = (NetworkInterface) interfaces.nextElement();
//					if (!ni.isVirtual())
//						if (!ni.isLoopback())
//							if (ni.isUp()) {
//								Enumeration<InetAddress> addrs = ni.getInetAddresses();
//								while (addrs.hasMoreElements()) {
//									InetAddress a = (InetAddress) addrs.nextElement();
//									address += a.getHostAddress() + " ";
//								}
//							}
//				}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//		String[] addrs = address.split(" ");
//		for(int i = 0 ; i < addrs.length ; i++){
//			if(!addrs[i].contains(":"))
//				return addrs[i];
//		}
//		
//		return null;
//	}

	public static void setSystemVolume(int percent, Application app){
//		Util.systemCall("amixer set Master "+percent+"%"); // doesn't work in xubuntu 14.04 fresh install
		Util.systemCall("pactl -- set-sink-volume 0 "+percent+"%"); 		// pactl -- set-sink-volume 0 80%
		Settings.getReference().writeSettings(GUISettings.volume.name(), percent);
	}

	public static void saveUrl(String filename, String urlString) throws MalformedURLException, IOException {
        BufferedInputStream in = null;
        FileOutputStream fout = null;
        try{
                in = new BufferedInputStream(new URL(urlString).openStream());
                fout = new FileOutputStream(filename);
                byte data[] = new byte[1024];
                int count;
                while ((count = in.read(data, 0, 1024)) != -1)
                	fout.write(data, 0, count);	
                
        } finally {    
        	if (in != null) in.close();
            if (fout != null) fout.close();
        }
    }
	
	public static String tail(int lines){
		int i = 0;
		StringBuffer str = new StringBuffer();
	 	if(history.size() > lines) i = history.size() - lines;
		for(; i < history.size() ; i++) str.append(history.get(i) + "\n<br />"); 
		return str.toString();
	}
	
	public static String tailFormated(int lines){
		int i = 0;
		final long now = System.currentTimeMillis();
		StringBuffer str = new StringBuffer();
	 	if(history.size() > lines) i = history.size() - lines;
		for(; i < history.size() ; i++) {
			String line = history.get(i).substring(history.get(i).indexOf(",")+1).trim();
			String stamp = history.get(i).substring(0, history.get(i).indexOf(","));
			line = line.replaceFirst("\\$[0-9]", "");
			line = line.replaceFirst("^oculusprime.", "");
			line = line.replaceFirst("^oculusPrime.", "");
			line = line.replaceFirst("^Application.", "");
			line = line.replaceFirst("^static, ", "");		
			double delta = (double)(now - Long.parseLong(stamp)) / (double) 1000;
			String unit = " sec ";
			String d = formatFloat(delta, 0);
			if(delta > 60) { delta = delta / 60; unit = " min "; d =  formatFloat(delta, 1); }
			str.append("\n<tr><td colspan=\"11\">" + d + "<td>" + unit + "<td>&nbsp;&nbsp;" + line + "</tr> \n"); 
		}
		return str.toString();
	}
	
	public static void log(String method, Exception e, Object c) {
		log(method + ": " + e.getLocalizedMessage(), c);
	}
	
	public static void log(String str, Object c) {
    	if(str == null) return;
		String filter = "static";
		if(c!=null) filter = c.getClass().getName();
		if(history.size() > MAX_HISTORY) history.remove(0);
		history.add(System.currentTimeMillis() + ", " + filter + ", " +str);
		System.out.println("OCULUS: " + getTime() + ", " + filter + ", " + str);
	}
	
    public static void debug(String str, Object c) {
    	if(str == null) return;
    	String filter = "static";
    	if(c!=null) filter = c.getClass().getName();
		if(Settings.getReference().getBoolean(ManualSettings.debugenabled)) {
			System.out.println("DEBUG: " + getTime() + ", " + filter +  ", " +str);
			history.add(System.currentTimeMillis() + ", " +str);
		}
	}
    
    public static void debug(String str) {
    	if(str == null) return;
    	if(Settings.getReference().getBoolean(ManualSettings.debugenabled)){
    		System.out.println("DEBUG: " + getTime() + ", " +str);
    		history.add(System.currentTimeMillis() + ", " +str);
    	}
    }
    
	public static String memory() {
    	String str = "";
		str += "memory : " + ((double)Runtime.getRuntime().freeMemory()
			/ (double)Runtime.getRuntime().totalMemory())*100 + "% free<br>";
		
		str += "memory total : "+Runtime.getRuntime().totalMemory()+"<br>";    
	    str += "memory free : "+Runtime.getRuntime().freeMemory()+"<br>";
		return str;
    }
	
	public static Document loadXMLFromString(String xml){
		try {
	    
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder;
		
			builder = factory.newDocumentBuilder();
			InputSource is = new InputSource(new StringReader(xml));
			return builder.parse(is);
		
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	// replaces standard e.printStackTrace();
	public static String XMLtoString(Document doc) {
		String output = null;
		try {
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			StringWriter writer = new StringWriter();
			transformer.transform(new DOMSource(doc), new StreamResult(writer));
			output = writer.getBuffer().toString().replaceAll("\n|\r", "");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return output;
	}

	public static void printError(Exception e) {
		System.err.println("error "+getTime()+ ":");
		e.printStackTrace();
	}
	
	public static boolean validIP (String ip) {
	    try {
	    	
	        if (ip == null || ip.isEmpty()) return false;
	        String[] parts = ip.split( "\\." );
	        if ( parts.length != 4 ) return false;
	        for ( String s : parts ) {
	            int i = Integer.parseInt( s );
	            if ( (i < 0) || (i > 255) )
	            	return false;
	        }
	        
	        if(ip.endsWith(".")) return false;
	        
	        return true;
	    } catch (NumberFormatException nfe) {
	        return false;
	    }
	}

	public static long[] readProcStat() {
		try {

			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("/proc/stat")));
			String line = reader.readLine();
			reader.close();
			String[] values = line.split("\\s+");
			long total = Long.valueOf(values[1])+Long.valueOf(values[2])+Long.valueOf(values[3])+Long.valueOf(values[4]);
			long idle = Long.valueOf(values[4]);
			return new long[] { total, idle};

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static int getCPU(){
		long[] procStat = readProcStat();
		long totproc1st = procStat[0];
		long totidle1st = procStat[1];
		Util.delay(100);
		procStat = readProcStat();
		long totproc2nd = procStat[0];
		long totidle2nd = procStat[1];
		int percent = (int) ((double) ((totproc2nd-totproc1st) - (totidle2nd - totidle1st))/ (double) (totproc2nd-totproc1st) * 100);
		return percent;
	}

	// top -bn 2 -d 0.1 | grep '^%Cpu' | tail -n 1 | awk '{print $2+$4+$6}'
	// http://askubuntu.com/questions/274349/getting-cpu-usage-realtime
	/*
	public static String getCPUTop(){
		try {

			String[] cmd = { "/bin/sh", "-c", "top -bn 2 -d 5 | grep '^%Cpu' | tail -n 1 | awk \'{print $2+$4+$6}\'" };
			Process proc = Runtime.getRuntime().exec(cmd);
			BufferedReader procReader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			return procReader.readLine();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}	*/
	
	
	public static boolean testHTTP(){
		
		final String ext = State.getReference().get(values.externaladdress); 
		final String http = State.getReference().get(State.values.httpport);
		final String url = "http://"+ext+":"+ http +"/oculusPrime";
		
		if(ext == null || http == null) return false;
	
		try {
			
			log("testPortForwarding(): "+url, "testHTTP()");
			URLConnection connection = (URLConnection) new URL(url).openConnection();
			BufferedReader procReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			log("testPortForwarding(): "+procReader.readLine(), "testHTTP()");

		} catch (Exception e) {
			 log("testPortForwarding(): failed: " + url, "testHTTP()");
			return false;
		}
		
		return true;
	}
	
	public static boolean testTelnetRouter(){			
		try {

			// "127.0.0.1"; //
			final String port = Settings.getReference().readSetting(GUISettings.telnetport);
			final String ext =State.getReference().get(values.externaladdress);
			log("...telnet test: " +ext +" "+ port, null);
			Process proc = Runtime.getRuntime().exec("telnet " + ext + " " + port);
			BufferedReader procReader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			
			String line = procReader.readLine();
			if(line.toLowerCase().contains("trying")){
				line = procReader.readLine();
				if(line.toLowerCase().contains("connected")){
					log("telnet test pass...", null);
					return true;
				}
			}
		} catch (Exception e) {
			log("telnet test fail..."+e.getLocalizedMessage(), null);
			return false;
		}
		log("telnet test fail...", null);
		return false;
	}
	
	//TODO: 
	public static boolean testRTMP(){	
		try {

			final String ext = "127.0.0.1"; //State.getReference().get(values.externaladdress); //	
			final String rtmp = Settings.getReference().readRed5Setting("rtmp.port");

			log("testRTMP(): http = " +ext, null);
			
			Process proc = Runtime.getRuntime().exec("telnet " + ext + " " + rtmp);
			BufferedReader procReader = new BufferedReader(new InputStreamReader(proc.getInputStream()));

			String line = procReader.readLine();
			log("testRTMP(): " + line, null);
			line = procReader.readLine();
			log("testRTMP():" + line, null);
			log("testRTMP(): process exit value = " + proc.exitValue(), null);
			
			if(line == null) return false;
			else if(line.contains("Connected")) return true;
			
		} catch (Exception e) {
			return false;
		}
		
		return true;
	}
	
	public static String getJavaStatus(){
		
		if(redPID==null) return "jetty not running";
		
		String line = null;
		try {
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("/proc/"+ redPID +"/stat")));
			line = reader.readLine();
			reader.close();
			log("getJavaStatus:" + line, null);
					
		} catch (Exception e) {
			printError(e);
		}
		
		return line;
	}

	public static String getRed5PID(){	
		
		if(redPID!=null) return redPID;
		
		String[] cmd = { "/bin/sh", "-c", "ps -fC java" };
		
		Process proc = null;
		try { 
			proc = Runtime.getRuntime().exec(cmd);
			proc.waitFor();
		} catch (Exception e) {
			Util.log("getRed5PID(): "+ e.getMessage(), null);
			return null;
		}  
		
		String line = null;
		String[] tokens = null;
		BufferedReader procReader = new BufferedReader(new InputStreamReader(proc.getInputStream()));					
		
		try {
			while ((line = procReader.readLine()) != null){
				if(line.contains("red5")) {
					tokens = line.split(" ");
					for(int i = 1 ; i < tokens.length ; i++) {
						if(tokens[i].trim().length() > 0) {
							if(redPID==null) redPID = tokens[i].trim();							
						}
					}
				}	
			}
		} catch (IOException e) {
			Util.log("getRed5PID(): ", e.getMessage());
		}

		return redPID;
	}	
	
	public static String pingWIFI(final String addr){
		
		if(addr==null) return null;
					
		String[] cmd = new String[]{"ping", "-c1", "-W1", addr};
		
		long start = System.currentTimeMillis();
		
		Process proc = null;
		try { 
			proc = Runtime.getRuntime().exec(cmd);
			proc.waitFor();
		} catch (Exception e) {
			Util.log("pingWIFI(): "+ e.getMessage(), null);
			return null;
		}  
		
		String line = null;
		String time = null;
		BufferedReader procReader = new BufferedReader(new InputStreamReader(proc.getInputStream()));					
		
		try {
			while ((line = procReader.readLine()) != null){
				if(line.contains("time=")) {
					time = line.substring(line.indexOf("time=")+5, line.indexOf(" ms"));
					break;
				}	
			}
		} catch (IOException e) {
			Util.log("pingWIFI(): ", e.getMessage());
		}

		if(proc.exitValue() != 0 ) Util.debug("pingWIFI(): exit code: " + proc.exitValue(), null);
		if(time == null) Util.log("pingWIFI(): null result for address: " + addr, null);
		if((System.currentTimeMillis()-start) > 1100)
			Util.debug("pingWIFI(): ping timed out, took over a second: " + (System.currentTimeMillis()-start));
		
		return time;	
	}

	public static void updateLocalIPAddress(){	
		
		State state = State.getReference();
		String wdev = lookupWIFIDevice();
		
		try {			
			String[] cmd = new String[]{"/bin/sh", "-c", "ifconfig"};
			Process proc = Runtime.getRuntime().exec(cmd);
			proc.waitFor();
			
			String line = null;
			BufferedReader procReader = new BufferedReader(new InputStreamReader(proc.getInputStream()));					
			while ((line = procReader.readLine()) != null) {	
				if(line.contains(wdev)) {
					line = procReader.readLine();
					String addr = line.substring(line.indexOf(":")+1); 
					addr = addr.substring(0, addr.indexOf(" ")).trim();
									
					if(validIP(addr)) State.getReference().set(values.localaddress, addr);
					else Util.log("updateLocalIPAddress(): bad address ["+ addr + "]", null);
				}
			}
		} catch (Exception e) {
			Util.log("updateLocalIPAddress(): failed to lookup wifi device", null);
			state.delete(values.localaddress);
			updateEthernetAddress();
		}
	}
	
	public static void updateEthernetAddress(){	
		
		State state = State.getReference();

		try {			
			String[] cmd = new String[]{"/bin/sh", "-c", "ifconfig"};
			Process proc = Runtime.getRuntime().exec(cmd);
			proc.waitFor();
			
			String line = null;
			BufferedReader procReader = new BufferedReader(new InputStreamReader(proc.getInputStream()));					
			while ((line = procReader.readLine()) != null) {	
				if(line.contains("eth")) {
					line = procReader.readLine();
					String addr = line.substring(line.indexOf(":")+1); 
					addr = addr.substring(0, addr.indexOf(" ")).trim();
									
					if(validIP(addr)) State.getReference().set(values.localaddress, addr);
					else Util.log("updateLocalIPAddress(): bad address ["+ addr + "]", null);
				}
			}
		} catch (Exception e) {
			state.set(values.localaddress, "127.0.0.1");
		}
		
		if(!state.exists(values.localaddress)) state.set(values.localaddress, "127.0.0.1");
	}
	
	private static String lookupWIFIDevice(){
		
		String wdev = null;
		
		try { // this fails if no wifi is enabled 
			Process proc = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", "nmcli dev"});
			String line = null;
			BufferedReader procReader = new BufferedReader(new InputStreamReader(proc.getInputStream()));					
			while ((line = procReader.readLine()) != null) {
				if( ! line.startsWith("DEVICE") && line.contains("wireless")){
					String[] list = line.split(" ");
					wdev = list[0];
				}
			}
		} catch (Exception e) {
			Util.log("lookupDevice():  no wifi is enabled  ", null);
		}
		
		return wdev;
	}


	public static void updateExternalIPAddress(){
		new Thread(new Runnable() { public void run() {

			State state = State.getReference();

			// changed: updated only called on ssid change from non null
//			if(state.exists(values.externaladdress)) {
//				Util.log("updateExternalIPAddress(): called but already have an ext addr, try ping..", null);
//				if(Util.pingWIFI(state.get(values.externaladdress)) != null) {
//					Util.log("updateExternalIPAddress(): ping sucsessful, reject..", null);
//					return;
//				}
//			}

			try {

				URLConnection connection = (URLConnection) new URL("http://www.xaxxon.com/xaxxon/checkhost").openConnection();
				BufferedInputStream in = new BufferedInputStream(connection.getInputStream());

				int i;
				String address = "";
				while ((i = in.read()) != -1) address += (char)i;
				in.close();

				if(Util.validIP(address)) state.set(values.externaladdress, address);
				else state.delete(values.externaladdress);

			} catch (Exception e) {
				Util.log("updateExternalIPAddress():"+ e.getMessage(), null);
				state.delete(values.externaladdress);
			}
		} }).start();
	}

	public static String getJettyPID(){	
		
	//	if(jettyPID!=null) return jettyPID;
		
		String[] cmd = { "/bin/sh", "-c", "ps -fC java" };
		
		Process proc = null;
		try { 
			proc = Runtime.getRuntime().exec(cmd);
			proc.waitFor();
		} catch (Exception e) {
			Util.log("getJettyPID(): "+ e.getMessage(), null);
			return null;
		}  
		
		String line = null;
		String[] tokens = null;
		BufferedReader procReader = new BufferedReader(new InputStreamReader(proc.getInputStream()));					
		
		try {
			while ((line = procReader.readLine()) != null){
				if(line.contains("start.jar")) {
					tokens = line.split(" ");
					if(tokens[0].equals("root"))
					for(int i = 1 ; i < tokens.length ; i++) {
						if(tokens[i].trim().length() > 0) {
							if(jettyPID==null) jettyPID = tokens[i].trim();							
						}
					}
				}	
			}
		} catch (IOException e) {
			Util.log("getJettyPID(): ", e.getMessage());
		}

		return jettyPID;
	}	
	
	public static void setJettyTelnetPort() {
		
		if(jettyPID == null) return;
		
		new Thread(new Runnable() { public void run() {
			Settings settings = Settings.getReference();
			String url = "http://127.0.0.1/?action=telnet&port=" + settings.readSetting(GUISettings.telnetport);
			try {
				
				URLConnection connection = (URLConnection) new URL(url).openConnection();
				BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
				
				Util.log("url: " + url, this);
				int i;
				String line = null;
				while ((i = in.read()) != -1) line += (char)i;
				in.close();
				
				// debug("setJettyTelnetPort(): "+line, this);
				
			} catch (Exception e) {}
		} }).start();
	}
	
	public static void updateJetty() {
		
		if(jettyPID == null) return;
		
		new Thread(new Runnable() { public void run() {
			try {
				String url = "http://127.0.0.1/?action=push";
				URLConnection connection = (URLConnection) new URL(url).openConnection();
				BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
				
				Util.log("url: " + url, this);
				int i;
				String line = null;
				while ((i = in.read()) != -1) line += (char)i;
				in.close();
				
				// debug("updateJetty(): " + line, this);
				
			} catch (Exception e) {}
		} }).start();
	}

	public static String getJettyStatus() {
	
		if(jettyPID == null) return "no PID";
		
		try {
			
			String url = "http://127.0.0.1/?action=status";
			URLConnection connection = (URLConnection) new URL(url).openConnection();
			BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
			
			int i; String reply = "";
			while ((i = in.read()) != -1) reply += (char)i;
			in.close();
			return reply;
				
		} catch (Exception e) {
			return new Date().toString() + " DISABLED";
		}
	}

	public static void deleteLogFiles(){
	 	File[] files = new File(Settings.logfolder).listFiles();
	    for (int i = 0; i < files.length; i++){
	       if (files[i].isFile()) files[i].delete();
	   }
	}
	
	public static long getLogMBytes(){	
		long size = 0;
	    File[] files = new File(Settings.logfolder).listFiles();
	    for (int i = 0; i < files.length; i++){
	        if (files[i].isFile())
	        	size += files[i].length();
	    }
		    
		return (size / (1000*1000)); 
	}
	
	public static long getFrameMBytes(){	
		long size = 0;
	    File[] files = new File(Settings.framefolder).listFiles();
	    for (int i = 0; i < files.length; i++){
	        if (files[i].isFile())
	        	size += files[i].length();
	    }
		   
	    // log("getFrameMBytes: " + files.length + " files, mbytes " + (size / (1000*1000)), null);
		return (size / (1000*1000)); 
	}

	public static int countFrameGrabs(){
		File path = new File(Settings.framefolder);
		if (path.exists()) return path.listFiles().length;
		else return 0;
	}
	
	// TODO: hhh
	public static void truncStaleFrames(){
		File[] files  = new File(Settings.framefolder).listFiles();
		sortFiles(files); // prune back by a 3rd 
        for (int i = (files.length/4); i < files.length; i++){
			if (files[i].isFile()){
				log("truncFrames(): " + files[i].getName() + " *deleted*", null);
				files[i].delete();
	        }
		} 
        
        // do it again? 
        /**/
        if(getFrameMBytes() > MAX_lOG_MBYTES){
        	files  = new File(Settings.framefolder).listFiles();
        	log("truncFrames(): files: " + files.length + " size: " + getFrameMBytes(), null);
    /*
        	sortFiles(files);
            for (int i = (files.length/3); i < files.length; i++){
    			if (files[i].isFile()){
    				log("truncFrames(): " + files[i].getName() + " *deleted*", null);
    				files[i].delete();
    	        }
    		} 
    		*/
        	
        }
        
	}

	private static void sortFiles(File[] files) {
		Arrays.sort(files, new Comparator<File>(){
			public int compare( File f1, File f2){
                long result = f2.lastModified() - f1.lastModified();
                if( result > 0 ){
                    return 1;
                } else if( result < 0 ){
                    return -1;
                } else {
                    return 0;
                }
            }
        });	
	}
	
	public static boolean archiveLogs(){
		
		if(busyArchiving) return false;
	
		final long start = System.currentTimeMillis();
		final String path = "./archive" + sep + System.currentTimeMillis() + "_logs.tar.bz2";
		final String[] cmd = new String[]{"/bin/sh", "-c", "tar -cvjf " + path + " log"};
		new File(Settings.redhome + sep + "archive").mkdir(); 
		
		log("archiveLogs(): create archive file: " + path, null);
		
		new Thread(new Runnable() { public void run() {
			try {
				if((System.currentTimeMillis() - start) > ONE_MINUTE) {
					log("archiveLogs(): timout, deleting " + path, null);
					busyArchiving = false;
//					archiveProc.destroyForcibly();
					archiveProc.destroy();
					new File(path).delete();
				}
			} catch (Exception e) {}
		} }).start();
		
		try {			
			busyArchiving = true;
			archiveProc = Runtime.getRuntime().exec(cmd);
			String line = null;
			BufferedReader procReader = new BufferedReader(new InputStreamReader(archiveProc.getInputStream()));					
			while ((line = procReader.readLine()) != null && busyArchiving)  	
				Util.log("manageLogs(): tar: " + line, null);
			
			archiveProc.waitFor();
		} catch (Exception e) {
			Util.log("manageLogs(): fail: " + e.getCause() + " " + e.getMessage(), null);
			return false;
		}
		
		busyArchiving = false;
		log("archiveLogs(): tar operation took: " + ((System.currentTimeMillis() - start)/1000) + " seconds", null);
		if(new File(path).exists()) log("archiveLogs(): zip file size " +new File(path).length()/(1000*1000) + " mytes", null);
		return new File(path).exists();
	}

	public static synchronized void manageLogs(){

		// only call once 
		if(shuttingdown) return;
		shuttingdown = true;
		
		if(getLogMBytes() > MAX_lOG_MBYTES){
			log("...manageLogs() .... too big, archivve logs", null);
			if(archiveLogs()){
				log("manageLogs() .... call to delete log files after zipping", null);
				deleteLogFiles();
			}
		}
		
		if(getFrameMBytes() > MAX_lOG_MBYTES){
			log(".....manageLogs() .... too big, delete frames", null);
			truncStaleFrames();
		}	
		
		log("...manageLogs() ............. exit....", null);
		
	}
}

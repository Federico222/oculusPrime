package oculusPrime;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.Vector;

public class State {
	
	public enum values{ 
		
		motionenabled, moving, movingforward, motorport, cameratilt, motorspeed,   // motors

		dockgrabbusy, docking, dockstatus, autodocking, dockfound, dockmetrics, // dock 

		floodlightlevel, spotlightbrightness, strobeflashon, fwdfloodlevel, // lights

		driver, logintime, pendinguserconnected, telnetusers, // users
		
		streamactivityenabled, videosoundmode, stream, driverstream, volume, //audio video
		framegrabbusy, controlsinverted, lightlevel, streamactivitythreshold, streamactivity,
		motiondetect,

		wallpower, batterylife, powerport, batteryinfo, battvolts,  // power
		powererror, forceundock, // power problems

		javastartup, linuxboot, httpport, lastusercommand, // system

		distanceangle, direction, odometry, distanceanglettl, stopbetweenmoves, odometrybroadcast, // odometry
		odomturndpms, odomturnpwm, odomupdated, odomlinearmpms, odomlinearpwm,
		
		rosmapinfo, rosamcl, rosglobalpath, rosscan,  // navigation
		roscurrentgoal, rosmapupdated, rosmapwaypoints, navsystemstatus,
		rossetgoal, rosgoalstatus, rosgoalcancel, navigationroute, rosinitialpose,
		navigationrouteid, nextroutetime,
		
		localaddress, externaladdress, // network things 
		signalspeed, ssid, gateway, ethernetaddress, cpu, 
		
	}

	/** not to be broadcast over telnet channel when updated, to reduce chatter */
	public enum nonTelnetBroadcast { batterylife, sysvolts, batteryinfo, rosscan, rosmapwaypoints, rosglobalpath,
		odomturnpwm, odomlinearpwm, framegrabbusy, lastusercommand}
	
	/** @return true if given command is in the sub-set */
	public static boolean isNonTelnetBroadCast(final String str) {
		try { 
			nonTelnetBroadcast.valueOf(str); 
		} catch (Exception e) {return false;}
		
		return true; 
	}
	
	public static final int ERROR = -1;
	
	/** notify these on change events */
	public Vector<Observer> observers = new Vector<Observer>();
	
	/** reference to this singleton class */
	private static State singleton = new State();

	/** properties object to hold configuration */
	private HashMap<String, String> props = new HashMap<String, String>(); 
	
	public static State getReference() {
		return singleton;
	}

	/** private constructor for this singleton class */
	private State() {
		props.put(values.javastartup.name(), String.valueOf(System.currentTimeMillis()));	
		props.put(values.telnetusers.name(), "0");
		getLinuxUptime();
	}

	public void getLinuxUptime(){
		new Thread(new Runnable() {
			@Override
			public void run() {	
				try {
					
					Process proc = Runtime.getRuntime().exec(new String[]{"uptime", "-s"});
					BufferedReader procReader = new BufferedReader(new InputStreamReader(proc.getInputStream()));									
					String line = procReader.readLine();
					Date date = new SimpleDateFormat("yyyy-MM-dd h:m:s", Locale.ENGLISH).parse(line);
					set(values.linuxboot, date.getTime());
					
				} catch (Exception e) {
					Util.debug("getLinuxUptime(): "+ e.getLocalizedMessage());
				}										
			}
		}).start();
	}
	
	public void addObserver(Observer obs){
		observers.add(obs);
	}
	
	/** test for string equality. any nulls will return false */ 
	public boolean equals(final String a, final String b){
		String aa = get(a);
		if(aa==null) return false; 
		if(b==null) return false; 
		if(aa.equals("")) return false;
		if(b.equals("")) return false;
		
		return aa.equalsIgnoreCase(b);
	}
	
	public boolean equals(State.values value, String b) {
		return equals(value.name(), b);
	}
	
	@Override
	public String toString(){	
		String str = "";
		Set<String> keys = props.keySet();
		for(Iterator<String> i = keys.iterator(); i.hasNext(); ){
			String key = i.next();
			str += (key + " " + props.get(key) + "<br>"); // "\n\r");
		}
		return str;
	} 
	
	/**
	public String toTable(){	
		StringBuffer str = new StringBuffer("<table>");
		Set<String> keys = props.keySet();
		for(Iterator<String> i = keys.iterator(); i.hasNext(); ){
			String key = i.next();
			String value = props.get(key); 
			str.append("<tr><td>" + key + "<td>" + value + "</tr>");
		}
		str.append("</table>\n");
		return str.toString();
	}
	*/
	
	
	public String toHTML(){ 
		StringBuffer str = new StringBuffer("<table cellspacing=\"5\">");
		for (values key : values.values()) { 
			if(props.containsKey(key.name())) str.append("<tr><td> " + key.name() + "<td>" + props.get(key.name()));
			else str.append("<tr><td> " + key.name() + "<td><b>NULL</b>");
		}	
		
		str.append("</table>\n");
		return str.toString();
	}
	
	
	
	/**
	 * block until timeout or until member == target
	 * 
	 * @param member state key
	 * @param target block until timeout or until member == target
	 * @param timeout is the ms to wait before giving up 
	 * @return true if the member was set to the target in less than the given timeout 
	 */
	public boolean block(final values member, final String target, int timeout){
		
		long start = System.currentTimeMillis();
		String current = null;
		while(true){
			
			// keep checking 
			current = get(member); 
			if(current!=null){
				if(target.equals(current)) return true;
				if(target.startsWith(current)) return true;
			}
				
			// TODO: FIX with a call back?? 
			Util.delay(1); // no higher, used by motion, odometry
			if (System.currentTimeMillis()-start > timeout){ 
				Util.debug("block() timeout: " + member.name(), this);
				return false;
			}
		}
	} 
	
	/** Put a name/value pair into the configuration */
	public synchronized void set(final String key, final String value) {
		
		if(key==null) {
			Util.log("set() null key!", this);
			return;
		}
		if(value==null) {
			Util.log("set() use delete() instead", this);
			Util.log("set() null valu for key: " + key, this);
			return;
		}
		
		try {
			props.put(key.trim(), value.trim());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		for(int i = 0 ; i < observers.size() ; i++) observers.get(i).updated(key.trim());	
	}

	/** Put a name/value pair into the config */
	public void set(final String key, final long value) {
		set(key, Long.toString(value));
	}
	
	public String get(values key){
		return get(key.name());
	}
	
	/** */
	public synchronized String get(final String key) {

		String ans = null;
		try {

			ans = props.get(key.trim());

		} catch (Exception e) {
			System.err.println(e.getStackTrace());
			return null;
		}

		return ans;
	}


	/** */
	public boolean getBoolean(ManualSettings setting) {
		return getBoolean(setting);
	}

	
	/** true returns true, anything else returns false */
	public boolean getBoolean(String key) {
		
		boolean value = false;
		
		try {

			value = Boolean.parseBoolean(get(key));

		} catch (Exception e) {
			return false;
		}

		return value;
	}

	
	/** */
	public int getInteger(final String key) {

		String ans = null;
		int value = ERROR;

		try {

			ans = get(key);
			value = Integer.parseInt(ans);

		} catch (Exception e) {
			return ERROR;
		}

		return value;
	}
	
	
	/** */
	public long getLong(final String key) {

		String ans = null;
		long value = ERROR;

		try {

			ans = get(key);
			value = Long.parseLong(ans);

		} catch (Exception e) {
			return ERROR;
		}

		return value;
	}
	
	/** @return the ms since last app start */
	public long getUpTime(){
		return System.currentTimeMillis() - getLong(values.javastartup);
	}
	
	/** @return the ms since last user log in */
	public long getLoginSince(){
		return System.currentTimeMillis() - getLong(values.logintime);
	}

	/** */
	public synchronized void set(String key, boolean b) {
		if(b) set(key, "true");
		else set(key, "false");
	}
	
	public synchronized boolean exists(values key) {
		return props.containsKey(key.toString().trim());
	}
	
	public synchronized boolean exists(String key) {
		return props.containsKey(key.trim());
	}
	
	public synchronized void delete(String key) {
		
		if( ! props.containsKey(key)) return;
		
		props.remove(key);
		for(int i = 0 ; i < observers.size() ; i++)
			observers.get(i).updated(key);	
	}

	public void set(values key, values value) {
		set(key.name(), value.name());
	}

	public void delete(values key) {
		if(exists(key)) delete(key.name());
	}

	public int getInteger(values key) {
		return getInteger(key.name());
	}
	
	public long getLong(values key){
		return getLong(key.name());
	}
	
	public boolean getBoolean(values key){
		return getBoolean(key.name());
	}
	
	public void set(values key, long data){
		set(key.name(), data);
	}
	
	public void set(values key, String value){
		set(key.name(), value);
	}
	
	public void set(values key, boolean value){
		set(key.name(), value);
	}

	public void put(values value, String str) {
		set(value.name(), str);
	}

	public void put(values value, int b) {
		put(value, String.valueOf(b));
	}

	public void put(values value, boolean b) {
		put(value, String.valueOf(b));
	}

	public void delete(PlayerCommands cmd) {
		delete(cmd.name());
	}

	public void put(values value, long b) {
		put(value, String.valueOf(b));
	}
	
	public void put(values value, double b) {
		put(value, String.valueOf(b));
	}
	
	public void put(values key, values update) {
		put(key, update.name());
	}
	
	public double getDouble(String key) {
		double value = ERROR;
		
		if(get(key) == null) return value;
		
		try {
			value = Double.valueOf(get(key));
		} catch (NumberFormatException e) {
			Util.log("getDouble(): " + e.getMessage(), this);
		}
		
		return value;
	}

	public double getDouble(values key) {
		return getDouble(key.name());
	}

}
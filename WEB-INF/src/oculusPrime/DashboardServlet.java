package oculusPrime;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import oculusPrime.State.values;
import oculusPrime.commport.PowerLogger;

public class DashboardServlet extends HttpServlet implements Observer {
	
	static final long serialVersionUID = 1L;	
	static final String HTTP_REFRESH_DELAY_SECONDS = "5";
	static double VERSION = new Updater().getCurrentVersion();
	static Settings settings = null; ;
	static BanList ban = null;
	static State state = null;
	static Vector<String> history;
	
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		history = new Vector<String>();
		settings = Settings.getReference();
		state = State.getReference();
		ban = BanList.getRefrence();
		state.addObserver(this);
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	
		if( ! ban.knownAddress(request.getRemoteAddr())){
			Util.log("unknown address: danger: "+request.getRemoteAddr(), this);
			response.sendRedirect("/oculusPrime");   
			return;
		}
	
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
	
		String view = null;	
		String delay = null;	
		
		try {
			view = request.getParameter("view");
			delay = request.getParameter("delay");
		} catch (Exception e) {
			Util.debug("doGet(): " + e.getLocalizedMessage(), this);
		}
			
		if(delay == null) delay = HTTP_REFRESH_DELAY_SECONDS;
		
		out.println("<html><head><meta http-equiv=\"refresh\" content=\""+ delay + "\"></head><body> \n");

		if(view != null){
			if(view.equalsIgnoreCase("ban")){
				out.println(ban + "<br />\n");
				out.println(ban.tail(30) + "\n");
				out.println("\n</body></html> \n");
				out.close();
			}
			
			if(view.equalsIgnoreCase("state")){
				out.println(state.toHTML() + "\n");
				out.println("\n</body></html> \n");
				out.close();
			}
			
			if(view.equalsIgnoreCase("sysout")){
				out.println(new File(Settings.stdout).getAbsolutePath() + "<br />\n");
				out.println(Util.tail(40) + "\n");
				out.println("\n</body></html> \n");
				out.close();
			}
			
			if(view.equalsIgnoreCase("power")){	
				out.println(new File(PowerLogger.powerlog).getAbsolutePath() + "<br />\n");
				out.println(PowerLogger.tail(40) + "\n");
				out.println("\n</body></html> \n");
				out.close();
			}
			
			if(view.equalsIgnoreCase("ros")){
				out.println(rosDashboard() + "\n");
				out.println("\n</body></html> \n");
				out.close();
			}
			
			if(view.equalsIgnoreCase("log")){
				out.println("\nsystem output: <hr>\n");
				out.println(Util.tail(20) + "\n");
				out.println("\n<br />power log: <hr>\n");
				out.println("\n" + PowerLogger.tail(5) + "\n");
				out.println("\n<br />" +  ban + "<hr>\n");
				out.println("\n" + ban.tail(7) + "\n");
				out.println("\n</body></html> \n");
				out.close();
			}
		}
		
		out.println(toDashboard(request.getServerName()+":"+request.getServerPort() + "/oculusPrime/dashboard") + "\n");
		out.println("\n</body></html> \n");
		out.close();	
	}
	
	public String toTableHTML(){
		StringBuffer str = new StringBuffer("<table cellspacing=\"10\" border=\"2\"> \n");
		
		str.append("<tr>" 
				+ "<td><b>distanceangle</b><td>" + state.get(values.distanceangle)
				+ "<td><b>direction</b><td>" + state.get(values.direction)
				+ "<td><b>odometry</b><td>" + state.get(values.odometry) 
				+ "</tr> \n");
		
		str.append("<tr>" 
				+ "<td><b>distanceanglettl</b><td>" + state.get(values.distanceanglettl) 
				+ "<td><b>stopbetweenmoves</b><td>" + state.get(values.stopbetweenmoves) 
				+ "<td><b>odometrybroadcast</b><td>" + state.get(values.odometrybroadcast) 
				+ "<td><b>odomturndpms</b><td>" + state.get(values.odomturndpms) 
				+ "</tr> \n");
		
		str.append("<tr>" 
				+ "<td><b>odomturnpwm</b><td>" + state.get(values.odomturnpwm) 
				+ "<td><b>odomupdated</b><td>" + state.get(values.odomupdated) 
				+ "<td><b>odomlinearmpms</b><td>" + state.get(values.odomlinearmpms) 
				+ "<td><b>odomlinearpwm</b><td>" + state.get(values.odomlinearpwm) 
				+ "</tr> \n");
		
		str.append("<tr>"
				+ "<td><b>rosmapinfo</b><td colspan=\"7\">" + state.get(values.rosmapinfo) 
			// 	+ "<td><b>rosamcl</b><td>" + state.get(values.rosamcl) 
			//	+ "<td><b>rosglobalpath</b><td>" + state.state.get(values.rosglobalpath) 
				+ "</tr> \n");
			
		str.append("<tr><td><b>roscurrentgoal</b><td>" + state.get(values.roscurrentgoal) 
				+ "<td><b>rosmapupdated</b><td>" + state.get(values.rosmapupdated) 
			//	+ "<td><b>rosmapwaypoints</b><td>" + state.get(values.rosmapwaypoints) 
				+ "<td><b>navsystemstatus</b><td>" + state.get(values.navsystemstatus)
				+ "</tr> \n");
		
		str.append("<tr>" 
				+ "<td><b>rossetgoal</b><td>" + state.get(values.rossetgoal) 
				+ "<td><b>rosgoalstatus</b><td>" + state.get(values.rosgoalstatus)
				+ "<td><b>rosgoalcancel</b><td>" + state.get(values.rosgoalcancel) 
				+ "<td><b>navigationroute</b><td>" + state.get(values.navigationroute)
				+ "</tr> \n");
		
		str.append("<tr>" 
				+ "<td><b>rosinitialpose</b><td>" + state.get(values.rosinitialpose) 
				+ "<td><b>navigationrouteid</b><td>" + state.get(values.navigationrouteid) 
				+ "</tr> \n");
		
		str.append("<tr><td><b>rosmapwaypoints</b><td colspan=\"7\">" + state.get(values.rosmapwaypoints) );
		
		str.append("<tr><td><b>rosglobalpath</b><td colspan=\"10\">" + state.get(values.rosglobalpath) + "</tr> \n");
				
		str.append("\n</table>\n");
		return str.toString();
	}
	
	public String rosDashboard(){	
		StringBuffer str = new StringBuffer("<table cellspacing=\"10\" border=\"2\"> \n");
		
		str.append("<tr>" 
				+ "<td><b>distanceangle</b><td>" + state.get(values.distanceangle)
				+ "<td><b>direction</b><td>" + state.get(values.direction)
				+ "<td><b>odometry</b><td>" + state.get(values.odometry) 
				+ "</tr> \n");
		
		str.append("<tr>" 
				+ "<td><b>distanceanglettl</b><td>" + state.get(values.distanceanglettl) 
				+ "<td><b>stopbetweenmoves</b><td>" + state.get(values.stopbetweenmoves) 
				+ "<td><b>odometrybroadcast</b><td>" + state.get(values.odometrybroadcast) 
				+ "<td><b>odomturndpms</b><td>" + state.get(values.odomturndpms) 
				+ "</tr> \n");
		
		str.append("<tr>" 
				+ "<td><b>odomturnpwm</b><td>" + state.get(values.odomturnpwm) 
				+ "<td><b>odomupdated</b><td>" + state.get(values.odomupdated) 
				+ "<td><b>odomlinearmpms</b><td>" + state.get(values.odomlinearmpms) 
				+ "<td><b>odomlinearpwm</b><td>" + state.get(values.odomlinearpwm) 
				+ "</tr> \n");
		
		str.append("<tr>"
				+ "<td><b>rosmapinfo</b><td colspan=\"7\">" + state.get(values.rosmapinfo) 
			// 	+ "<td><b>rosamcl</b><td>" + state.get(values.rosamcl) 
			//	+ "<td><b>rosglobalpath</b><td>" + state.get(values.rosglobalpath) 
				+ "</tr> \n");
			
		str.append("<tr><td><b>roscurrentgoal</b><td>" + state.get(values.roscurrentgoal) 
				+ "<td><b>rosmapupdated</b><td>" + state.get(values.rosmapupdated) 
			//	+ "<td><b>rosmapwaypoints</b><td>" + state.get(values.rosmapwaypoints) 
				+ "<td><b>navsystemstatus</b><td>" + state.get(values.navsystemstatus)
				+ "</tr> \n");
		
		str.append("<tr>" 
				+ "<td><b>rossetgoal</b><td>" + state.get(values.rossetgoal) 
				+ "<td><b>rosgoalstatus</b><td>" + state.get(values.rosgoalstatus)
				+ "<td><b>rosgoalcancel</b><td>" + state.get(values.rosgoalcancel) 
				+ "<td><b>navigationroute</b><td>" + state.get(values.navigationroute)
				+ "</tr> \n");
		
		str.append("<tr>" 
				+ "<td><b>rosinitialpose</b><td>" + state.get(values.rosinitialpose) 
				+ "<td><b>navigationrouteid</b><td>" + state.get(values.navigationrouteid) 
				+ "</tr> \n");
		
		str.append("<tr><td><b>rosmapwaypoints</b><td colspan=\"7\">" + state.get(values.rosmapwaypoints) );
		str.append("<tr><td><b>rosglobalpath</b><td colspan=\"10\">" + state.get(values.rosglobalpath) + "</tr> \n");
		str.append("\n</table>\n");
		return str.toString();
	}
	
	public String toDashboard(final String url){
		
		StringBuffer str = new StringBuffer("<table cellspacing=\"10\" border=\"1\"> \n");
		
		String ssid = "disconnected";
		if(state.exists(values.ssid)) ssid = state.get(values.ssid);
		
		String eth = "disconnected";
		if(state.exists(values.ethernetaddress)) eth = state.get(values.ethernetaddress);
			
		str.append("<tr><td><b>version</b><td>" + VERSION 
				+ "<td><b>ssid</b><td>" + ssid
				+ "<td><b>telnet</b><td>" + state.get(values.telnetusers) 
				+ "<td><b>cpu</b><td>" + state.get(values.cpu) 
				+ "% </tr> \n");

		str.append("<tr><td><b>gate</b><td>" + state.get(values.gateway) + "&nbsp;&nbsp;&nbsp;&nbsp;"
				+ "<td><b>lan</b><td>" + state.get(values.localaddress) + "&nbsp;&nbsp;&nbsp;&nbsp;"
				+ "<td><b>wan</b><td>" + state.get(values.externaladdress) + "&nbsp;&nbsp;&nbsp;&nbsp;"
				+ "<td><b>eth</b><td>" + eth + "&nbsp;&nbsp;&nbsp;&nbsp;"
				+ "</tr> \n");
		
		str.append("<tr><td><b>motor</b><td>" + state.get(values.motorport) 
				+ "<td><b>linux</b><td>" + (((System.currentTimeMillis() - state.getLong(values.linuxboot)) / 1000) / 60)+ " mins"
				+ "<td><b>motion</b><td>" + state.get(values.motionenabled) + "<td><b>moving</b><td>" + state.get(values.moving)
				+ "</tr> \n");
				
		str.append("<tr><td><b>power</b><td>" + state.get(values.powerport)
				+ "<td><b>java</b><td>" + (state.getUpTime()/1000)/60  + " mins"
				+ "<td><b>life</b><td>" + state.get(values.batterylife) 
				+ "<td><b>volts</b><td>" + state.get(values.battvolts) 
				+ "</tr> \n");
		
		str.append("\n<tr><td colspan=\"11\">" + Util.tailShort(10) + "</tr> \n");
		str.append("\n<tr><td colspan=\"11\">" + getHTML() + "</tr> \n");	
		str.append("\n</table>\n");
		return str.toString();
	}
	
	private String getHTML(){
		String reply = "";
		for(int i = 0 ; i < history.size() ; i++) reply += history.get(i) + " <br />\n";
		return reply;
	}

	@Override
	public void updated(String key) {
		if(history.size() > 7) history.remove(0);
		if(state.exists(key)) history.add(Util.getDateStamp() + " " +key + " = " + state.get(key));
		else history.add(Util.getDateStamp() + " " + key + " was deleted");
	}
}

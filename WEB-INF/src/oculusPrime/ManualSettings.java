package oculusPrime;

import java.util.Properties;

public enum ManualSettings {
	
	motorport, powerport, developer, debugenabled, telnetport, wheeldiameter,
	gyrocomp, alertsenabled, odomturnpwm, odomlinearpwm,
	soundthreshold, motionthreshold, networkmonitor, checkaddresses, defaultuuid, ignoreuuids;
	
	/** get basic settings */
	public static Properties createDeaults(){
		Properties config = new Properties();
		config.setProperty(ignoreuuids.name(), "none");
		config.setProperty(defaultuuid.name(), "none");
		config.setProperty(developer.name(), Settings.FALSE);
		config.setProperty(debugenabled.name(), Settings.FALSE);
		config.setProperty(checkaddresses.name(), Settings.TRUE);	
		config.setProperty(motorport.name(), Settings.ENABLED);
		config.setProperty(powerport.name(), Settings.ENABLED);
		config.setProperty(telnetport.name(), Settings.DISABLED);
		config.setProperty(wheeldiameter.name(), "106");
		config.setProperty(gyrocomp.name() , "1.095");
		config.setProperty(alertsenabled.name() , Settings.TRUE);
		config.setProperty(soundthreshold.name(), "10");
		config.setProperty(motionthreshold.name(), "10");
		config.setProperty(motionthreshold.name(), "0.003");
		config.setProperty(networkmonitor.name(), Settings.FALSE);
		config.setProperty(odomlinearpwm.name(), "150");
		config.setProperty(odomturnpwm.name(), "150");
		return config;
	}
	
	public static String getDefault(ManualSettings setting){
		Properties defaults = createDeaults();
		return defaults.getProperty(setting.name());
	}
	
	public static boolean isDefault(ManualSettings manual){
		Settings settings = Settings.getReference();
		if(settings.readSetting(manual).equals(getDefault(manual))) return true;
		
		return false;
	}
}

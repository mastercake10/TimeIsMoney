package de.Linus122.TimeIsMoney;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import org.bukkit.plugin.Plugin;

import com.google.gson.Gson;

/*
 * SpaceIOMetrics main class by Linus122
 * version: 0.03
 * 
 */
public class Metrics {
	private Plugin pl;
	private final Gson gson = new Gson();
	
	private String URL = "https://spaceio.de/update/%s";
	private final String VERSION = "0.03";
	private int REFRESH_INTERVAL = 600000;
	
	public Metrics(Plugin pl){
		this.pl = pl;
		
		// check if Metrics are disabled (checks if file "disablemetrics" is added to the plugins's folder
		try {
			Files.list(pl.getDataFolder().getParentFile().toPath()).filter(Files::isRegularFile).forEach(v -> {
				if(v.getFileName().toString().equalsIgnoreCase("disablemetrics")){
					return;
				}
			});
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		URL = String.format(URL, pl.getName());
		
		// fetching refresh interval first
		pl.getServer().getScheduler().runTaskLaterAsynchronously(pl, () -> {
			String dataJson = collectData();
			try{
				REFRESH_INTERVAL = sendData(dataJson);
			}catch(Exception e){}
		}, 20L * 5);
		
		// executing repeating task, our main metrics updater
		pl.getServer().getScheduler().runTaskTimerAsynchronously(pl, () -> {
			String dataJson = collectData();
			try{
				sendData(dataJson);
			}catch(Exception e){}
			
		}, 20L * (REFRESH_INTERVAL / 1000), 20L * (REFRESH_INTERVAL / 1000));
	}
	private String collectData() {
		Data data = new Data();
		
		// collect plugin list
		for(Plugin plug : pl.getServer().getPluginManager().getPlugins()) data.plugs.put(plug.getName(), plug.getDescription().getVersion());
		
		// fetch online players
		data.onlinePlayers = pl.getServer().getOnlinePlayers().size();
		
		// server version
		data.serverVersion = getVersion();
		
		// plugin version
		data.pluginVersion = pl.getDescription().getVersion();
		
		// plugin author
		data.pluginAuthors = pl.getDescription().getAuthors();
		
		// core count
		data.coreCnt = Runtime.getRuntime().availableProcessors();
		
		// java version
		data.javaRuntime = System.getProperty("java.runtime.version");
		
		// online mode
		data.onlineMode = pl.getServer().getOnlineMode();

		// software information
		data.osName = System.getProperty("os.name");
		data.osArch = System.getProperty("os.arch");
		data.osVersion = System.getProperty("os.version");
		
		data.diskSize = new File("/").getTotalSpace();
		
		if(data.osName.equals("Linux")){
			data.linuxDistro = getDistro();
		}
		
		return gson.toJson(data);
	}
	private int sendData(String dataJson) throws Exception{
		java.net.URL obj = new java.net.URL(URL);
		HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", "Java/Bukkit");
		con.setRequestProperty("Metrics-Version", this.VERSION);

		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(dataJson);
		wr.flush();
		wr.close();
		
		return Integer.parseInt(con.getHeaderField("interval-millis"));
	}
	private String getVersion(){
        String packageName = pl.getServer().getClass().getPackage().getName();
        return  packageName.substring(packageName.lastIndexOf('.') + 1);
	}
	// method source: http://www.jcgonzalez.com/linux-get-distro-from-java-examples
	private String getDistro(){
		 //lists all the files ending with -release in the etc folder
        File dir = new File("/etc/");
        File fileList[] = new File[0];
        if(dir.exists()){
            fileList =  dir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String filename) {
                    return filename.endsWith("-release");
                }
            });
        }
        //looks for the version file (not all linux distros)
        File fileVersion = new File("/proc/version");
        if(fileVersion.exists()){
            fileList = Arrays.copyOf(fileList,fileList.length+1);
            fileList[fileList.length-1] = fileVersion;
        }       
        //prints first version-related file
        for (File f : fileList) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(f));
                String strLine = null;
                while ((strLine = br.readLine()) != null) {
                    return strLine;
                }
                br.close();
            } catch (Exception e) {}
        }
		return "unknown";    
	}
}
class Data {
	HashMap<String, String> plugs = new HashMap<String, String>();
	int onlinePlayers;
	String pluginVersion;
	public List<String> pluginAuthors;
	String serverVersion;
	
	long diskSize;
	int coreCnt;
	String javaRuntime;
	
	boolean onlineMode;
	
	String osName;
	String osArch;
	String osVersion;
	String linuxDistro;
}

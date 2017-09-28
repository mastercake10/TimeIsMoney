package de.Linus122.TimeIsMoney;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;

import javax.net.ssl.HttpsURLConnection;

import org.bukkit.plugin.Plugin;

import com.google.gson.Gson;

public class Metrics {
	private Plugin pl;
	private final Gson gson = new Gson();
	
	private String URL = "https://spaceio.de/update/%s";
	
	public Metrics(Plugin pl){
		this.pl = pl;
		URL = String.format(URL, pl.getName());
		pl.getServer().getScheduler().runTaskTimerAsynchronously(pl, () -> {
			String dataJson = collectData();
			try{
				sendData(dataJson);
			}catch(Exception e){
				// skip
				e.printStackTrace();
			}
		}, 20L * 5, 20L * 60 * 10);
	}
	private String collectData() {
		Data data = new Data();

		// collect plugin list
		for(Plugin plug : pl.getServer().getPluginManager().getPlugins()) data.plugs.put(plug.getName(), plug.getDescription().getVersion());
		
		// fetch online players
		data.onlinePlayers = pl.getServer().getOnlinePlayers().size();
		
		// server version
		data.serverVersion = getVersion();
		
		// core count
		data.coreCnt = Runtime.getRuntime().availableProcessors();
		
		// java version
		data.javaRuntime = System.getProperty("java.runtime.version");
		
		// online mode
		data.onlineMode = pl.getServer().getOnlineMode();

		return gson.toJson(data);
	}
	private void sendData(String dataJson) throws Exception{
		java.net.URL obj = new java.net.URL(URL);
		HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", "Java/Bukkit");

		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(dataJson);
		wr.flush();
		wr.close();
		con.getResponseCode();
	}
	private String getVersion(){
        String packageName = pl.getServer().getClass().getPackage().getName();
        return  packageName.substring(packageName.lastIndexOf('.') + 1);
	}
	
}
class Data {
	HashMap<String, String> plugs = new HashMap<String, String>();
	int onlinePlayers;
	String serverVersion;
	int coreCnt;
	String javaRuntime;
	boolean onlineMode;
}

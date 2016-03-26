package webapi;

import java.io.BufferedReader;
<<<<<<< HEAD
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;


public class VersionChecker {
	public static String url = "https://www.spigotmc.org/resources/time-is-money.12409/";
	public static String content = "";
	
	public static int getVersion(){
		int version2 = 10;
		try{
	        String s = content.split("<div class=\"section\" id=\"versionInfo\">")[1].split("</div>")[0];
	        String version = s.split("<h3>Version ")[1].split("</h3>")[0].replace(".", "");
	        version2 = Integer.valueOf(version);
		}catch(Exception e){
			
		}
        return version2;
	}
	public static String getNewVersionFileUrl(){
		String s = content.split("<label class=\"downloadButton \">")[1].split("</label>")[0];
		String link = s.split("<a href=\"")[1].split("\" class=\"inner\">")[0];
		String link2 = "https://www.spigotmc.org/" + link;
		return link2;
	}
	
	public static void init() throws MalformedURLException, IOException {
		HttpsURLConnection con = (HttpsURLConnection) new URL(url).openConnection();
		con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
		content = get_content(con);
	}
	public static void download(String url2, File location) throws IOException{
		HttpsURLConnection con = (HttpsURLConnection) new URL(url2).openConnection();
		con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
		InputStream in = con.getInputStream();
		FileOutputStream fos = new FileOutputStream(location);
		byte[] buf = new byte[512];
		while (true) {
		    int len = in.read(buf);
		    if (len == -1) {
		        break;
		    }
		    fos.write(buf, 0, len);
		}
		in.close();
		fos.flush();
		fos.close();
	}
    public static String get_content(HttpsURLConnection con){
		String content = "";
		if(con!=null){
				
		try {
					
		   BufferedReader br = 
			new BufferedReader(
				new InputStreamReader(con.getInputStream()));
					
		   String input;
		   while ((input = br.readLine()) != null){
		      content += input;
		   }
		   br.close();
					
		} catch (IOException e) {
		   e.printStackTrace();
		}
	    
			
	  }
		return content;
    }
=======
import java.io.InputStreamReader;
import java.net.URL;

import de.Linus122.TimeIsMoney.Main;

public class VersionChecker {
	public static int getVersion(){
        URL oracle;
		try {
			oracle = new URL("http://176.57.142.247/api/version.html");
	        BufferedReader in = new BufferedReader(
	        new InputStreamReader(oracle.openStream()));
	
	        String inputLine;
	        while ((inputLine = in.readLine()) != null)
	            return Integer.parseInt(inputLine);
	        in.close();
		} catch (Exception e) {
			return 9999;
		}
		return 9999;
	}
	public static void register(){
		URL url;
		try {
			url = new URL("http://176.57.142.247/api/post.php?info=" + "v-" + Main.version + "-");
			url.openConnection().getInputStream();
		} catch (Exception e) {

		}
	}
	public static void unregister(){
		
	}
>>>>>>> branch 'master' of https://github.com/mastercake10/TimeIsMoney.git
}

package webapi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;

import de.Linus122.TimeIsMoney.Main;


public class VersionChecker {
	public static String url_check_version = "http://avendria.de/tim/checkversion.php?version=";
	public static String url_download = "http://avendria.de/tim/download.php";
	
	public static int getVersion(){

		try {
			URLConnection con = new URL(url_check_version + Main.pl_version).openConnection();
			con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");

			return Integer.parseInt(get_content(con));
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;

	}
	public static void download(File location) throws IOException{
		URLConnection con =  new URL(url_download).openConnection();
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
    public static String get_content(URLConnection con){
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
}

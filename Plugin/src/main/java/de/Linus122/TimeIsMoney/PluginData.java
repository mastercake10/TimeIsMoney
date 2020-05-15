package de.Linus122.TimeIsMoney;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

public class PluginData {
	/**
	 * The payouts for the day.
	 */
	private static HashMap<String, Double> payedMoney = new HashMap<>();
	/**
	 * Day since last refresh
	 */
	private static int lastRefreshDay = 0;
	

	private final static String filePath = "plugins/TimeIsMoney/data/";
	private final static File dataFile = new File(filePath + "payed_today.data");
	
	/**
	 * Loads data from file if {@link #dataFile} exists.
	 */
	public static void loadData() {
		if(!dataFile.exists()) return;
		try {
			FileInputStream fis = new FileInputStream(dataFile);
			ObjectInputStream ois = new ObjectInputStream(fis);
			Object payedMoneyObj = ois.readObject();
			payedMoney = (HashMap<String, Double>) ((HashMap<String, Double>) payedMoneyObj).clone();
			lastRefreshDay = ois.readInt();
			ois.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	/**
	 * Saves the data on disc to file {@link #dataFile}
	 */
	public static void saveData() {
		(new File(filePath)).mkdirs();
		
		try {
			FileOutputStream fos = new FileOutputStream(dataFile);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(payedMoney);
			oos.writeInt(lastRefreshDay);
			oos.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * @return the lastRefreshDay
	 */
	public static int getLastRefreshDay() {
		return lastRefreshDay;
	}
	
	/**
	 * @param lastRefreshDay the lastRefreshDay to set
	 */
	public static void setLastRefreshDay(int lastRefreshDay) {
		PluginData.lastRefreshDay = lastRefreshDay;
	}
	
	/**
	 * @return the payedMoney
	 */
	public static HashMap<String, Double> getPayedMoney() {
		return payedMoney;
	}

}

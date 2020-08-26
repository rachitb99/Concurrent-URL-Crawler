package entity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;

public class IUT{
	
	public static void add(String url) {
		String hc = String.valueOf(Math.abs(url.hashCode()));
		try {
			BufferedWriter fw = new BufferedWriter(new FileWriter("./IUT/" + hc.substring(0, 3), true));
			fw.write(url + "\n");
			fw.flush();
			fw.close();
		} catch (Exception e) {
		}
	}
	
	@SuppressWarnings("resource")
	public static boolean contains(String url) {
		String hc = String.valueOf(Math.abs(url.hashCode()));
	    BufferedReader reader;
	    String line; 
		try {
			reader = new BufferedReader(new FileReader("./IUT/" + hc.substring(0, 3)));
			while ((line = reader.readLine()) != null) {	
				if(line.equals(url)) {
					return true;
				}
			}
		} catch (Exception e1) {
			return false;
		}
		return false;
	}
}
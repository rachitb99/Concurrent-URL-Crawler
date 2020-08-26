package driver;

import java.io.BufferedReader;
import java.io.File;

import entity.*;
import threads.*;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;

import java.util.*;

public class WebCrawlerDriver {

	// number of buffers
	private static final int NO_OF_BUFFERS = 3;

	// maximum capacity of the BUL
	private static final int MAX_CAPACITY = 1000; 

	// number of crawling threads
	private static final int NO_OF_CRAWLERS = 6;

	// number of building threads
	private static final int NO_OF_BUILDERS = 3;
	
	private static Thread[] crawlers;
	private static Thread[] builders;
	public static long TTL;
	public static String inputfile;
	public static String outputfile;
	public static int storedPageNum;
	
	private static ArrayList<Stack<UrlTuple>> stackList;
	
	
	/**
	 * This method checks if the arguments provided to the program are valid, and parses them.
	 */
	private static boolean checkArguments(String[] args) {
		
		boolean[] beenChecker = new boolean[4];
		
		for(int i = 0; i < args.length; i++) {
			
			String argument = args[i];
			
			if (argument.equals("-time")) {
			    if (i+1 < args.length) {
			    	beenChecker[i/2] = true;
			    	
			    	String timeString = args[++i];
			    	
			    	char lastchar = timeString.charAt(timeString.length()-1);
			    	int time = Integer.parseInt(timeString.substring(0, timeString.length() - 1));
			    	
			    	if(lastchar == 'h') {
				    	TTL = System.nanoTime() + TimeUnit.HOURS.toNanos(time);
			    	}else {
			        	TTL = System.nanoTime() + TimeUnit.MINUTES.toNanos(time);
			    	}
			    	continue;
			    }
			    else {
			    	System.err.println("-time requires a number");	
			    	return false;
			    }
			}
			
			if (argument.equals("-input")) {
			    if (i+1 < args.length) {
			    	beenChecker[i/2] = true;
			        inputfile = args[++i];
			        continue;
			    }
			    else {
			    	System.err.println("-inputrequires a filename");
			    	return false;
			    }
			}
			
			if (argument.equals("-output")) {
			    if (i+1 < args.length) {
			    	beenChecker[i/2] = true;
			        outputfile = args[++i];
					continue;
			    }
			    else {
			    	System.err.println("-output requires a filename");		
			    	return false;
			    }
			}
			
			if (argument.equals("-storedPageNum")) {
			    if (i+1 < args.length) {
			    	beenChecker[i/2] = true;
			    	storedPageNum = Integer.parseInt(args[++i]);
			        continue;
			    }
			    else {
			    	System.err.println("-storedPageNum requires a number");	
			    	return false;
			    }
			}
			return false;
		}
		
		for(int i = 0; i < beenChecker.length; i++) {
			if(!beenChecker[i])
				return false;
		}
		
		return true;
	}
	
	private static void deletePrevFiles()  throws Exception {
		FileUtils.deleteDirectory(new File("./htmls"));
		FileUtils.deleteDirectory(new File("./IUT"));
		try {
			FileUtils.forceDelete(new File("./" + outputfile));
		}catch(Exception e) {}
	}
	
	private static void initializeSeeds(ArrayList<ArrayList<String>> seeds) {
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(inputfile));
			String line = reader.readLine();
			int line_no = 0;
			while (line != null) {
				ArrayList<String> list = seeds.get(line_no % NO_OF_CRAWLERS);
				line = reader.readLine();
				list.add(line);
				line_no++;
				line = reader.readLine();
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public static void refillStack(int threadId) {
		Stack<UrlTuple> emptyStack = stackList.get(threadId);
		for(int i = 0; i < stackList.size(); i++) {
			if(i != threadId) {
				Stack<UrlTuple> stack = stackList.get(i);
				if (stack.size() > 5) {
					emptyStack.add(stack.pop());
				}
			}
		}
	}
	
	
	
	public static void main(String[] args) throws Exception {
		
		//Check if arguments are correct
		if(!checkArguments(args)) {
			System.err.println("Usage: java cawler.jar -time 24h -input seed.txt -output res.txt -storedPageNum 1000");	
			return;
		}
		
		//Delete the files made by an eventual previous run
		deletePrevFiles();
		
		//Preparing for the output
		new File("./htmls").mkdirs();
		new File("./IUT").mkdirs();

		//Initializing the buffers.
		ArrayList<ArrayList<UrlTuple>> buffers = new ArrayList<>();
		for (int i = 0; i < NO_OF_BUFFERS; i++) {
			buffers.add(new ArrayList<UrlTuple>());
		}

		// Split the doc of url seeds
		ArrayList<ArrayList<String>> seeds = new ArrayList<>();
		for (int i = 0; i < NO_OF_CRAWLERS; i++) {
			seeds.add(new ArrayList<String>());
		}
		
		initializeSeeds(seeds);
		
		stackList = new ArrayList<>();

		// create adn start the crawlers
		crawlers = new Thread[NO_OF_CRAWLERS];
		for (int i = 0; i < NO_OF_CRAWLERS; i++) {
			ArrayList<UrlTuple> buffer = buffers.get(i / 2);
			Stack<UrlTuple> taskStack = new Stack<>();
			stackList.add(taskStack);
			ArrayList<String> intialURLs = seeds.get(i);
			for (int j = 0; j < intialURLs.size(); j++) {
				taskStack.add(new UrlTuple("root", intialURLs.get(j)));
			}
			
			crawlers[i] = new Thread(
					new WebCrawler(buffer, taskStack, MAX_CAPACITY, i));
			crawlers[i].start();
		}

		// create and start the builders
		builders = new Thread[NO_OF_BUILDERS];
		IndexBuilder[] builderClasses = new IndexBuilder[NO_OF_BUILDERS];
		for (int i = 0; i < NO_OF_BUILDERS; i++) {
			ArrayList<UrlTuple> buffer = buffers.get(i);
			builderClasses[i] = new IndexBuilder(buffer, MAX_CAPACITY, outputfile);
			builders[i] = new Thread(builderClasses[i]);
			builders[i].start();
		}
		
		//Waiting for the time to run out. 
		while(TTL+1000 > System.nanoTime()){}
		
		//If any threads are waiting, interrupt them so they finish
		try {
			for(int i = 0; i < crawlers.length; i++) {
				crawlers[i].interrupt();
			}
			for(int i = 0; i < builders.length; i++) {
				builders[i].interrupt();
			}
		} catch (Exception e) {}
		
		
		// Print the content of the index to the console
		System.out.println("Total number of URLs scraped: " + IndexBuilder.totalScraped.toString());
		System.out.println("Total number of URLs found: " + WebCrawler.urlsFound.toString());
	}
}
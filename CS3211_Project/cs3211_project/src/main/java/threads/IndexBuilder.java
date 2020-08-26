package threads;

import entity.*;
import driver.WebCrawlerDriver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.*;

/**
 * This class defines the index building threads which consume elements from an
 * associated buffer (which is shared with other IBTs) and puts them into the
 * IUT.
 * 
 * @since 2020-03-18
 */
public class IndexBuilder implements Runnable {
	
	public boolean waiting;
	
	// BUL shared between two crawling threads and one index building thread
	private final List<UrlTuple> URL_BUFFER;
	
	// Capacity of the BUL
	private final int MAX_CAPACITY;
	
	//Sets the document ID for the htmls documents being saved, but also counts the amount of files crawled
	public static Integer htmlDocId = 0;
	
	//Total amount of scraped urls
	public static Integer totalScraped = 0;
	public static Integer cc = 0;
	
	//Writer to write the result file
	private String resultfile;

	public IndexBuilder(List<UrlTuple> sharedQueue, int max_capacity, String resultfile) {
		this.URL_BUFFER = sharedQueue;
		this.MAX_CAPACITY = max_capacity;
		this.resultfile = resultfile;
		this.waiting = false;
	}
	
	/**
	 * This method starts the Index building thread. It's task is to consume objects
	 * from the buffer and insert them into the URLIndexTree
	 */
	@Override
	public void run(){
		while (WebCrawlerDriver.TTL > System.nanoTime()) {
			try {
				consume();
			} catch (Exception ex) {
			}
		}
		System.out.println("Thread finished!!!!!: " + Thread.currentThread().getName());
		//DB.commit();
	}
	
	/**
	 * This method writes the results to files.
	 */
	public void writeURL(UrlTuple ut){
		try {	
			BufferedWriter reswriter = new BufferedWriter(new FileWriter(resultfile, true));
			if(ut.dead()) {
				reswriter.write( ut.getURL() + " ---> " + ut.getParent() + " : *dead-url* \n");
			} else if(htmlDocId > 1000) {
				reswriter.write(ut.getURL() + " ---> " + ut.getParent() + " : *ignored* \n");
			} else {
				String fileloc = "./htmls/" + htmlDocId.toString() + ".html";
				BufferedWriter htmlw = new BufferedWriter(new FileWriter(fileloc));
				htmlw.write(ut.getHTML());
				reswriter.write( ut.getURL() + " ---> " + ut.getParent() + " : " + new File(fileloc).getAbsolutePath() + "\n");
				htmlw.close();
				htmlDocId++;
			}
			reswriter.close();
		} catch (Exception e) {cc++;}
		totalScraped++;

	}

	/**
	 * This method tries to consume a pair from the associated buffer (by consume we
	 * mean pop it off the stack and place it in the URLIndexTree). The method is
	 * synchronized in order to avoid multi thread access to the shared buffer
	 */
	private void consume() throws InterruptedException {
		synchronized (URL_BUFFER) {		
			
			//Wait for BUL to be full
			while (URL_BUFFER.size() != MAX_CAPACITY && WebCrawlerDriver.TTL > System.nanoTime()) {
				System.out.println("Queue is empty " + Thread.currentThread().getName() + " is waiting , size: "
						+ URL_BUFFER.size());
				try{
					URL_BUFFER.wait();
				} catch(Exception e) {}
			}
			
			//copy and clear buffer
			ArrayList<UrlTuple> copy = new ArrayList<>();
			copy.addAll(URL_BUFFER);
			URL_BUFFER.clear();
			
			//Write the urls to files
			for(UrlTuple ut : copy){
				boolean contains = checkDuplicates(ut);
				writeOrDiscard(ut, contains);
			}

			URL_BUFFER.notifyAll();
		}
	}
	
	private boolean checkDuplicates(UrlTuple ut) {
		System.out.println(Thread.currentThread().getName() + " is reading from the IUT");
		this.waiting = false;
		return IUT.contains(ut.getURL());
	}
	
	private void writeOrDiscard(UrlTuple ut, boolean contains) {
		writeURL(ut);
		IUT.add(ut.getURL());
		if(!contains)
			System.out.println(Thread.currentThread().getName() +" wrote "+ ut.getURL() + " to the IUT");
	}
	
	
}
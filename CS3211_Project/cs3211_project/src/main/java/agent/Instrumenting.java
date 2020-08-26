package agent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import driver.WebCrawlerDriver;
import threads.IndexBuilder;

/**
 * 
 * @author niklas
 * This class contains helper methods that can be used to instrument the
 * execution of our concurrent program. 
 */
public class Instrumenting {
	
	// list of thread names used to orchestrate the locks associated with each thread
	public static List<String> threadNames = Arrays.asList(
			new String[]{"Thread-6-Read", "Thread-7-Read", "Thread-8-Read",
						 "Thread-6-Write", "Thread-7-Write", "Thread-8-Write"});
	
	static List<String> trace; 
	
	private static String sharedVar1 = "";
	private static String sharedVar2 = "";
	private static Object obj = new Object();
	
	public static void mySleep(int time_to_sleep) {
		try {
			System.out.println("[Agent] " + Thread.currentThread().getName() + " is starting to sleep");
			Thread.sleep(time_to_sleep);
			System.out.println("[Agent] " + Thread.currentThread().getName() + " is done sleeping");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * This method is used in the lock-implementaiton of the agent. The code is inserted 
	 * before writing to the IUT, thus causig the very first write-operation of the program
	 * to generate a data race. 
	 */
	public static void myLock() {
		synchronized (Instrumenting.obj) {
			if (sharedVar1.isEmpty()) {
				sharedVar1 = Thread.currentThread().getName();
				try{
					System.out.println( "[Agent] Locking " + Thread.currentThread().getName() );
					obj.wait();
					System.out.println( "[Agent] Releasing " + Thread.currentThread().getName() );
				} catch(InterruptedException e) {}
			// Lock the second thread entering critical section
			}else if(sharedVar2.isEmpty()) {
				sharedVar2 = Thread.currentThread().getName();
				try{
					System.out.println( "[Agent] Locking " + Thread.currentThread().getName() );
					obj.wait();
					System.out.println( "[Agent] Releasing " + Thread.currentThread().getName() );
				} catch(InterruptedException e) {}
			}
			// The third thread entering critical section is allowed to pass through here
			obj.notifyAll();
			
		}
	}
	
	/**
	 * This method is used in the orchestrating version of our agent.
	 * It causes the threads to wait until they are allowed to continue. One they are, they will 
	 * check the IUT for duplicates
	 */
	public static void checkDuplicates(IndexBuilder indexBuilder) {
			synchronized(threadNames.get(threadNames.indexOf(Thread.currentThread().getName() + "-Read"))){
				try {
					indexBuilder.waiting = true;
					threadNames.get(threadNames.indexOf(Thread.currentThread().getName() + "-Read")).wait();
				} catch (InterruptedException e1) {}
			}
	}
	/**
	 * This method is used in the orchestrating version of our agent.
	 * It causes the threads to wait until they are allowed to continue. One they are, they will 
	 * write their values into the IUT. 
	 */
	public static void writeOrDiscard(boolean contains, IndexBuilder indexBuilder) {
		synchronized(agent.Instrumenting.threadNames.get(agent.Instrumenting.threadNames.indexOf(Thread.currentThread().getName() + "-Write"))){
			if (contains) {
				System.out.println("[Agent] The tree already contains the value in " +  Thread.currentThread().getName() );
				return;
			}else {
				try {
					indexBuilder.waiting = true;
					agent.Instrumenting.threadNames.get(agent.Instrumenting.threadNames.indexOf(Thread.currentThread().getName() + "-Write")).wait();
				} catch (InterruptedException e1) {}
				//System.out.println("[Agent]" + Thread.currentThread().getName() + " is writing to the IUT");
				indexBuilder.waiting = false;
			}
		}

	}
	
	/**
	 * This method is used by the orchestrating implementation of our agent. It manages the index 
	 * building threads and tell them when they are allowed to move forward. it bases these 
	 * desicions on the trace that is provided as a parameter. 
	 * @param builderClasses
	 */
	public static void orchestrate(IndexBuilder[] builderClasses) {
		boolean allWaiting = true;
		for(IndexBuilder builder : builderClasses) {
			if (!builder.waiting){allWaiting = false;}
		}
		if (allWaiting && !trace.isEmpty()){
			try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();} // this sleep makes the prints pretty
			System.out.println("[Agent] Next element is " + threadNames.get(threadNames.indexOf(trace.get(0))));
			try {Thread.sleep(2000);} catch (InterruptedException e) {e.printStackTrace();}
			synchronized(threadNames.get(threadNames.indexOf(trace.get(0)))) {
				threadNames.get(threadNames.indexOf(trace.get(0))).notify();
				trace.remove(0);
			}
		}
	}
}

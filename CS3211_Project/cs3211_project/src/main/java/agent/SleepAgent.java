package agent;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.LinkedList;

public class SleepAgent {

	
	public static void premain(String agentArgs, Instrumentation inst) {
		// first number of input string represens version that we are to use
		int version = agentArgs.charAt(0) - '0';
		agentArgs = agentArgs.substring(1);
		switch(version) {
		  case 0:
		    // Implementation of sleep method
			System.out.println("[Agent] Sleep implementation");
			transformClass("threads.IndexBuilder", inst, 0);
		    break;
		  case 1:
			// Implementation of lock method
			System.out.println("[Agent] Lock implementation");
			transformClass("threads.IndexBuilder", inst, 1);
		    break;
		  case 2:
			// Implementation of orchestrating method
				Instrumenting.trace = formatInputString(agentArgs);//new LinkedList<String>(Arrays.asList(new String[]{"Thread-7-Read", "Thread-8-Read", "Thread-7-Write","Thread-8-Write", "Thread-8-Write","Thread-7-Read", "Thread-8-Read", "Thread-7-Write","Thread-8-Write", "Thread-8-Write"})); // TODO get as input
				System.out.println("[Agent] SleepAgent.java");
				String[] classNames = {"threads.IndexBuilder", "driver.WebCrawlerDriver"};
				for (String className : classNames) {
					transformClass(className, inst, 2); 
				}
			    break;
		  default:
		    return;
		}
	}
	
	private static LinkedList<String> formatInputString(String traceString) {
		LinkedList<String> list = new LinkedList<String>(Arrays.asList(new String[]{})); 
		
		// remove diamonds in beginning and end
		String[] trace = traceString.substring(1, traceString.length() - 1).split("->");
		
		// Iterate through all elements and append the relevant ones to the list
		for (String event : trace) {
			event = event.trim();
			if (event.substring(0, event.length() - 1).equals("checkDuplicate.") ) {
				list.add("Thread-" + event.substring(event.length()-1, event.length()) + "-Read");
			}else if (event.substring(0, event.length() - 1).equals("writeOrDiscard.") ) {
				list.add("Thread-" + event.substring(event.length()-1, event.length()) + "-Write");
			}
		}
		return list;
	}

	private static void transformClass(String className, Instrumentation instrumentation, int implementation) {
		Class<?> targetCls = null;
		ClassLoader targetClassLoader = null;
		// see if we can get the class using forName
		try {
			targetCls = Class.forName(className);
			targetClassLoader = targetCls.getClassLoader();
			transform(targetCls, targetClassLoader, instrumentation, implementation);
			return;
		} catch (Exception ex) {
			System.out.println("Class [{}] not found with Class.forName");
		}
		// otherwise iterate all loaded classes and find what we want
		for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
			if (clazz.getName().equals(className)) {
				targetCls = clazz;
				targetClassLoader = targetCls.getClassLoader();
				transform(targetCls, targetClassLoader, instrumentation, implementation); 
				return;
			}
		}
		throw new RuntimeException("Failed to find class [" + className + "]");
	}

	private static void transform(Class<?> clazz, ClassLoader classLoader, Instrumentation instrumentation, int implementation) {
		ClassFileTransformer dt;
		switch(implementation) {
		  case 0:
			  dt = new SleepTransformer(clazz.getName(), classLoader);
			  break;
		  case 1:
			  dt = new LockTransformer(clazz.getName(), classLoader);
			  break;
		  case 2:
			  dt = new ClassTransformer(clazz.getName(), classLoader);
			  break;
		  default:
			return;
		}
		instrumentation.addTransformer(dt, true);

		try {
			instrumentation.retransformClasses(clazz);
		} catch (Exception ex) {
			throw new RuntimeException("Transform failed for: [" + clazz.getName() + "]", ex);
		}
		
	}
}
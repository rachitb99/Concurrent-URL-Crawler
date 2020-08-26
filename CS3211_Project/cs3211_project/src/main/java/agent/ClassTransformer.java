package agent;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

import driver.WebCrawlerDriver;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

/**
 * 
 * This class is initialized in order to transform a given class. One new instance of this
 * class is created for every class we wish to transform. 
 */
public class ClassTransformer implements ClassFileTransformer{
	private ClassLoader targetClassLoader;
	private String targetClassName;

	
	public ClassTransformer(String className, ClassLoader classLoader) {
		this.targetClassLoader = classLoader;
		this.targetClassName = className;
	}
	
	/**
	 * This method is the one that is in charge of overriding our existing methods. 
	 * It sits between our compiled .class-files and the JVM
	 * The class is passed as a byte[]. We modify this array in the method and return 
	 * the modified byte[]. This will then be used by JVM
	 * 
	 * */
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, 
    						ProtectionDomain protectionDomain, byte[] classfileBuffer) {
    	byte[] byteCode = classfileBuffer;

        String finalTargetClassName = this.targetClassName.replaceAll("\\.", "/"); 
        if (!className.equals(finalTargetClassName)) {
            return byteCode;
        }
 
        if (className.equals(finalTargetClassName)&& loader.equals(targetClassLoader)) {
            System.out.println("[Agent] Found the class " + finalTargetClassName );
            if (className.equals("threads/IndexBuilder")) {
            	byteCode = transformIndexBuilder(byteCode);
            }else if (className.equals("driver/WebCrawlerDriver")) {
            	byteCode = transformWebCrawlerDriver(byteCode);
            }
            
        }
        return byteCode;
        
    }
    
    /**
     * Specific transformations for the IndexBuilder class
     */
    private byte[] transformIndexBuilder(byte[] byteCode) {
    	System.out.println("Altering IndexBuilder");
    	
    	
    	try {
            ClassPool cp = ClassPool.getDefault();
            CtClass cc = cp.get(targetClassName);

            // changing checkDuplicates method
            CtMethod m1 = cc.getDeclaredMethod("checkDuplicates");
            StringBuilder startBlock1 = new StringBuilder();
            startBlock1.append("agent.Instrumenting.checkDuplicates(this);");
            m1.insertBefore(startBlock1.toString());
            
         // changing writeOrDiscard method
            CtMethod m2 = cc.getDeclaredMethod("writeOrDiscard");
            StringBuilder startBlock2 = new StringBuilder();
            startBlock2.append("agent.Instrumenting.writeOrDiscard(contains, this);");
            m2.insertBefore(startBlock2.toString());
            
            byteCode = cc.toBytecode();
            cc.detach();
        } catch (NotFoundException | CannotCompileException | IOException e) {e.printStackTrace();}

    	return byteCode;
    	
    }
    
    /**
     * Specific transformations for the WebCrawlerDriver class
     */
    private byte[] transformWebCrawlerDriver(byte[] byteCode) {
    	System.out.println("Altering WebCrawlerDriver");
    	// Transform main method
    	try {
            ClassPool cp = ClassPool.getDefault();
            CtClass cc = cp.get(targetClassName);
            
            CtMethod m = cc.getDeclaredMethod("main");
            StringBuilder startBlock = new StringBuilder();
            startBlock.append("agent.Instrumenting.orchestrate(builderClasses);");
            //startBlock.append("agent.Instrumenting.myLock();");
            String str = startBlock.toString();
            m.insertAt(222, str);
            
            byteCode = cc.toBytecode();
            cc.detach();
        } catch (NotFoundException | CannotCompileException | IOException e) {
            e.printStackTrace();
        	System.out.println("Exception");
        }
    	return byteCode;
    }
}
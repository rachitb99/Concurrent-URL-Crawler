
package agent;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;


public class SleepTransformer implements ClassFileTransformer{
	
	private ClassLoader targetClassLoader;
	private String targetClassName;
	private String methodToBeAltered = "writeOrDiscard";
	
	public SleepTransformer(String className, ClassLoader classLoader) {
		this.targetClassLoader = classLoader;
		this.targetClassName = className;
		
	}
	
	/**
	 * This method is the one that is in charge of overriding our existing methods. 
	 * It sits between our compiled .class-files and the JVM
	 * The class is passed as a byte[]. We modify this array in the method and return 
	 * the modified byte[]. This will then be used by JVM
	 * 
	 * Note that you need to change the startBlock.append - statement later in this method depending on 
	 * if you want to do the sleep method / the lock method
	 * 
	 * */
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, 
    						ProtectionDomain protectionDomain, byte[] classfileBuffer) {
    	
        byte[] byteCode = classfileBuffer;
        String finalTargetClassName = this.targetClassName
          .replaceAll("\\.", "/"); 
        if (!className.equals(finalTargetClassName)) {
            return byteCode;
        }
 
        if (className.equals(finalTargetClassName) 
              && loader.equals(targetClassLoader)) {
  
            System.out.println("[Agent] Found the class " + finalTargetClassName + " and the method " + methodToBeAltered);
            
            try {
                ClassPool cp = ClassPool.getDefault();
                CtClass cc = cp.get(targetClassName);
                
                CtMethod m = cc.getDeclaredMethod(methodToBeAltered);
                StringBuilder startBlock = new StringBuilder();
                startBlock.append("agent.Instrumenting.mySleep(5000);");
                String str = startBlock.toString();
                m.insertBefore(str);
                
                byteCode = cc.toBytecode();
                cc.detach();
            } catch (NotFoundException | CannotCompileException | IOException e) {
                System.out.println("Exception");
            }
            
        }
        return byteCode;
    }
}
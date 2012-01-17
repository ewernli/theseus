package ch.unibe.iam.scg.rewriter;

import ch.unibe.iam.scg.ContextClassLoader;
import javassist.CannotCompileException;
import javassist.ClassMap;
import javassist.NotFoundException;

public class RewriterClassMap extends ClassMap {
	
	final String versionSuffix;
	final ContextClassLoader loader;
	
	public RewriterClassMap( String suffix, ContextClassLoader l){
		this.versionSuffix = suffix;
		this.loader = l;
	}
	
	public Object get(Object jvmClassName) {
		String name = toJavaName((String)jvmClassName);
		//System.out.println(jvmClassName);
		if (needsRewrite(name) && !name.endsWith(versionSuffix))
		{
			try {
				loader.findCtClass( name + versionSuffix );
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (CannotCompileException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			return toJvmName( name + versionSuffix );
		}
		else
		{
			return toJvmName( name );
		}
	}
	
	private boolean needsRewrite( String className ) {
		return className.startsWith("ch.unibe.iam.scg.test");
	}
}
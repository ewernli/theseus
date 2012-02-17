package ch.unibe.iam.scg.bootstrap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.TimeZone;

import javassist.ClassPool;

import ch.unibe.iam.scg.ContextClassLoader;
import ch.unibe.iam.scg.rewriter.helper.ArrayInterceptor;

public class RunJetty {
	//ClassPool cp = ClassPool.getDefault();
	
	public static void main(String[] args) throws Exception {
		new RunJetty().testServer(args);
	}
	
	public static ContextClassLoader newContextClassLoader( String suffix )
	{
		return  new ContextClassLoader(suffix);
	}
	
	
	public void testServer(String[] args) throws Exception
	{
		ClassLoader loader1;
		String className;
		
		if( args[0].equals("0")) {
			loader1 = this.getClass().getClassLoader();
			className = "org.mortbay.jetty.Main";
		}
		else
		{
			String suffix = args[0];
			loader1 = new ContextClassLoader("$$" + suffix);
			className = "ch.unibe.iam.scg.test.core.org.mortbay.jetty.Main$$" + suffix;
		}
		Thread.currentThread().setContextClassLoader(loader1);
		Class clazz = loader1.loadClass( className );
		Object server = clazz.newInstance();
		String[] params = new String[]{ args[1], args[2] };
		ArrayInterceptor.registerArray(params);
		invoke1( server, "main", String[].class, params ); 
		loader1 = null;
		System.gc();
	}
	
	private Object invoke( Object receiver, String method ) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException
	{
		Class clazz = receiver.getClass();
		Method meth = clazz.getMethod( method, new Class[0] );
		return meth.invoke( receiver, new Object[0] );
	}
	
	private Object invoke1( Object receiver, String method, Class c1, Object p1 ) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException
	{
		Class clazz = receiver.getClass();
		Method meth = clazz.getMethod( method, new Class[ ] { c1 } );
		return meth.invoke( receiver, new Object[]{ p1 } );
	}
}

package ch.unibe.iam.scg.bootstrap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javassist.ClassPool;

import ch.unibe.iam.scg.ContextClassLoader;

public class RunWebServer {
	ClassPool cp = ClassPool.getDefault();
	
	public static void main(String[] args) throws Exception {
		new RunWebServer().testServer();
	}
	
	public void testServer() throws Exception
	{
		//ch.unibe.iam.scg.test.jibble.WebServerMain.main(new String[]{"/Users/ewernli/Downloads/" , "9000" } );
		
		ContextClassLoader loader = new ContextClassLoader("$$1");
		Class clazz = loader.loadClass( "ch.unibe.iam.scg.test.jibble.WebServer$$1" );
		Object server = clazz.newInstance();
		invoke( server, "activate");
	}
	
	private Object invoke( Object receiver, String method ) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException
	{
		Class clazz = receiver.getClass();
		Method meth = clazz.getMethod( method, new Class[0] );
		return meth.invoke( receiver, new Object[0] );
	}
}

package ch.unibe.iam.scg;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Context extends ContextClassLoader {

	public Context(String suffix) {
		super(suffix);
	}
	
	public Object invoke( Object receiver, String name, Class[] parameterTypes, Object[] args) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException
	{
		Object finalReceiver = receiver;
		
		if( receiver instanceof ContextAware )
		{
			ContextAware contextReceiver = (ContextAware) receiver;
			if( ! contextReceiver.getClass().getClassLoader().equals(this))
			{
				finalReceiver = ((ContextAware)receiver).migrateToNext(this);
			}
		}
		
		Method m = finalReceiver.getClass().getMethod(name, parameterTypes);
		return m.invoke(finalReceiver, args);
	}
	
	public void execute( Runnable run ) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException
	{
		Runnable finalReceiver = run;
		
		if( run instanceof ContextAware )
		{
			ContextAware contextReceiver = (ContextAware) run;
			if( ! contextReceiver.getClass().getClassLoader().equals(this))
			{
				finalReceiver =  (Runnable) ((ContextAware)run).migrateToNext(this);
			}
		}
		
		finalReceiver.run();
	}
	
	public Context newContext( String className ) throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException
	{
		Class contextClazz = Class.forName( className );
		Constructor cst = contextClazz.getConstructor(String.class);
		ContextClassLoader context = (ContextClassLoader) cst.newInstance( "$$2" );
		this.setNext(context);
		return (Context) context;
	}
}

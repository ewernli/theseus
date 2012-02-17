package ch.unibe.iam.scg;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class Context extends ContextClassLoader {

	private static List rootSet = new ArrayList();
	
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
		
		rootSet.add(finalReceiver);
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
		
		rootSet.add(finalReceiver);
		finalReceiver.run();
	}
	
	public Context newContext( String className ) throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException
	{
		Class contextClazz = Class.forName( className );
		Constructor cst = contextClazz.getConstructor(String.class);
		String currentSuffix = this.suffix().substring(2);
		String newSuffix = "$$" + ( Integer.valueOf(currentSuffix) + 1 );
		ContextClassLoader context = (ContextClassLoader) cst.newInstance( newSuffix );
		this.setNext(context);
		return (Context) context;
	}
	
	@Override
	protected void finalize() throws Throwable {
		
		System.out.println("*****Finalized");
		super.finalize();
	}
	
	private int handleCount = 0;
	
	public ContextHandle getHandle()
	{
		handleCount++;
		return new ContextHandle(this);
	}
	
	public void disposeHandle()
	{
		handleCount--;
		if( handleCount==0)
		{
			this.forceRelease();
		}
	}
	
	private void forceRelease()
	{
		System.out.println("Force release");
		for( Object toRelease : rootSet )
		{
			// force release
		}
	}
}

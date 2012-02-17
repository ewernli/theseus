package ch.unibe.iam.scg;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ch.unibe.iam.scg.util.IdentitySet;

public class Context extends ContextClassLoader {

	// The root set is the set of "contextual" object we want to consider to force
	// the migration. It should contains the global objects (i.e. classes), and
	// also the objects that were used to "close over" in invoke/execute. 
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
		Method m = finalReceiver.getClass().getMethod(name, parameterTypes==null?new Class[0]:parameterTypes);
		return m.invoke(finalReceiver, args==null?new Object[0]:args);
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
	
	public Context newSuccessor( String className ) throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException
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
		try {
			System.out.println("Force release");
			Set alreadyProcessed = new IdentitySet();
			
			for( Object toRelease : rootSet )
			{
				if( toRelease instanceof ContextAware ) {
					
						forceSync( (ContextAware) toRelease, alreadyProcessed);
					
				}
			}
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	
	private void forceSync( ContextAware succ, Set alreadyProcessed ) throws IllegalAccessException
	{
		if( alreadyProcessed.contains(succ) )
		{
			return;
		}
		else
		{
			alreadyProcessed.add(succ);
		}
		
		Object prev = succ.getContextInfo().prev;
		Field[] fields = succ.getClass().getFields();
		
		for( Field nextF : fields )
		{
			Field prevF = nextF; // wrong
			Object prevValue = prevF.get(prev);
			Object nextValue = prevValue;
			if( prevValue instanceof ContextAware )
			{
				nextValue = this.migrateToNextIfNecessary(prevValue);
			}
			nextF.set(succ, nextValue);
		}
		
		// forced sync
		succ.getContextInfo().dirty = 0x000000;
		
		// recurse
		for( Field nextF : fields )
		{
			Object nextValue = nextF.get(prev);
			if( nextValue instanceof ContextAware ) {
				forceSync( (ContextAware) nextValue, alreadyProcessed);
			}
		}
	}
}

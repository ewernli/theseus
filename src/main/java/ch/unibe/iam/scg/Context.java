package ch.unibe.iam.scg;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ch.unibe.iam.scg.rewriter.ClassRewriter;
import ch.unibe.iam.scg.util.IdentitySet;

public class Context extends ContextClassLoader {

	// The root set is the set of "contextual" object we want to consider to force
	// the migration. It should contains the global objects (i.e. classes), and
	// also the objects that were used to "close over" in invoke/execute. 
	// @TODO should not be static
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
		
		System.out.println("Finalized context "+this.suffix());
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
				//@TODO handle other types, e.g. array
			}
			
			rootSet.clear();
			
			// remove itself from the ancestor. Should not be necessary
			this.next.prev.clear();
			this.next = null;
			
		} catch (Exception e) {
			throw new RuntimeException( "Could not force release", e);
		}
	}
	
	private void forceSync( ContextAware succ, Set alreadyProcessed ) throws IllegalAccessException, SecurityException, NoSuchFieldException
	{
		// this is the OLD context
		
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
			if( nextF.getName().equals( ClassRewriter.CONTEXT_INFO )) 
			{ 
				continue;
			}
			
			// doesn't always work
			Field prevF = prev.getClass().getField(nextF.getName());
			Object prevValue = prevF.get(prev);
			Object nextValue = prevValue;
			if( prevValue instanceof ContextAware )
			{
				nextValue = this.next.migrateToNextIfNecessary(prevValue);
			}
			// sync primitive types
			nextF.set(succ, nextValue);
			
			// release old ref
			prevF.set(prev,null);
		}
		
		// make succ local again
		succ.getContextInfo().dirty = 0x000000;
		succ.getContextInfo().prev = null;
		succ.getContextInfo().next = null; // not necessary
		succ.getContextInfo().global = false;
		
		// recurse
		for( Field nextF : fields )
		{
			Object nextValue = nextF.get(succ);
			if( nextValue instanceof ContextAware ) {
				forceSync( (ContextAware) nextValue, alreadyProcessed);
			}
			//@TODO handle other types, e.g. array
		}
	}
}

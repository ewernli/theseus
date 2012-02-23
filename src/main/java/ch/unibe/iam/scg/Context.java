package ch.unibe.iam.scg;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ch.unibe.iam.scg.rewriter.ClassRewriter;
import ch.unibe.iam.scg.rewriter.helper.ArrayInterceptor;
import ch.unibe.iam.scg.util.IdentitySet;

public class Context extends ContextClassLoader {

	// The root set is the set of "contextual" object we want to consider to force
	// the migration. It should contains the global objects (i.e. classes), and
	// also the objects that were used to "close over" in invoke/execute. 
	// @TODO should not be static?
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
				rootSet.add(finalReceiver);
			}
		}
		
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
				rootSet.add(finalReceiver);
			}
		}
		
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
			Set alreadyProcessed = new IdentitySet();
			
			System.out.println("Force release "+this.suffix());
			long t = System.currentTimeMillis();
			
			for( Object toRelease : rootSet )
			{
				if( toRelease instanceof ContextAware ) {
					forceSyncObject( (ContextAware) toRelease, alreadyProcessed);
				} else if ( toRelease instanceof Object[] ) {
					forceSyncArray( (Object[]) toRelease, alreadyProcessed);
				}
			}
			
			t = System.currentTimeMillis()-t;
			System.out.println(alreadyProcessed.size() + " object released in "+t+" ms");
			
			rootSet.clear();
			
			// remove itself from the ancestor. Should not be necessary
			// given it's a weak reference
			this.next.prev.clear();
			this.next = null;
			
		} catch (Exception e) {
			throw new RuntimeException( "Could not force release", e);
		}
	}
	
	private void forceSyncObject( ContextAware succ, Set alreadyProcessed ) throws IllegalAccessException, SecurityException, NoSuchFieldException
	{
		System.out.println("Force sync of "+succ.getClass().getName());
		// this is the OLD context
		
		if( alreadyProcessed.contains(succ) )
		{
			return;
		}
		else
		{
			alreadyProcessed.add(succ);
		}
		
		Field[] nextFields = nonFinalfieldsOfClassesOnly( succ.getClass() );
		
		// should normally not happen
		//if( ! succ.getContextInfo().global ) 
		{
		
			Object prev = succ.getContextInfo().prev;
			Field[] prevFields = nonFinalfieldsOfClassesOnly( prev.getClass() );
		
			for( int i=0;i<nextFields.length;i++ )
			{
				Field nextF = nextFields[i];
				if( nextF.getName().equals( ClassRewriter.CONTEXT_INFO )) 
				{ 
					continue;
				}
				// @TODO skip private fields that would cause an error. Such fields occur
				// if an updatable classe exntesd a non-updatable one, e.g. ShutdownThread
				// @TODO static fields are also excluded
				if( Modifier.isPrivate(nextF.getModifiers()) || 
					Modifier.isStatic(nextF.getModifiers()))
				{
					continue;
				}
				
				// doesn't always work
				Field prevF = prevFields[i];
				Object prevValue = prevF.get(prev);				
				Object nextValue = prevValue; // primitive types and array
				if( prevValue instanceof ContextAware )
				{
					nextValue = this.next.migrateToNextIfNecessary(prevValue);
					prevF.set(prev,null);
				}
				else if ( prevValue instanceof Object[]  )
				{		
					nextValue = this.next.migrateToNextIfNecessary(prevValue);
					prevF.set(prev,null); 
				} 
				
				// force new value
				nextF.set(succ, nextValue);
			}
			
			// make succ local again
			succ.getContextInfo().dirty = 0x000000;
			succ.getContextInfo().prev = null;
			succ.getContextInfo().next = null; // not necessary
			succ.getContextInfo().global = false;
		}
		
		// recurse
		for( Field nextF : nextFields )
		{
			// @TODO skip private fields that would cause an error. Such fields occur
			// if an updatable classe exntesd a non-updatable one, e.g. ShutdownThread
			// @TODO static fields are also excluded
			if( Modifier.isPrivate(nextF.getModifiers()) || 
				Modifier.isStatic(nextF.getModifiers()))
			{
				continue;
			}
			
			Object nextValue = nextF.get(succ);
			if( nextValue instanceof ContextAware ) {
				forceSyncObject( (ContextAware) nextValue, alreadyProcessed);
			}
			else if ( nextValue instanceof Object[] )
			{
				forceSyncArray( (Object[])nextValue, alreadyProcessed);
			}
		}
	}
	
	private void forceSyncArray( Object[] succ, Set alreadyProcessed ) throws IllegalAccessException, SecurityException, NoSuchFieldException
	{
		System.out.println("Force sync of "+succ.getClass().getName());
		// this is the OLD context
		
		if( alreadyProcessed.contains(succ) )
		{
			return;
		}
		else
		{
			alreadyProcessed.add(succ);
		}
		
		ContextInfo succInfo = ArrayInterceptor.contextInfoOfArray(succ);
		Object[] prev = (Object[]) succInfo.prev;
		
		for( int i=0; i<prev.length;i++ )
		{
			Object prevValue = prev[i];
			Object nextValue = prevValue;
			if( prevValue instanceof ContextAware )
			{
				nextValue = this.next.migrateToNextIfNecessary(prevValue);
			}
			else if ( prevValue instanceof Object[]  )
			{		
				nextValue = this.next.migrateToNextIfNecessary(prevValue);
			} 
			
			succ[i] = nextValue;
			// release old ref
			prev[i] = null;
		}
		
		// make succ local again
		succInfo.dirty = 0x000000;
		succInfo.prev = null;
		succInfo.next = null; // not necessary
		succInfo.global = false;
		
		// recurse
		for( int i=0 ; i<succ.length;i++ )
		{
			Object nextValue = succ[i];
			if( nextValue instanceof ContextAware ) {
				forceSyncObject( (ContextAware) nextValue, alreadyProcessed);
			}
			else if ( nextValue instanceof Object[] )
			{
				forceSyncArray( (Object[])nextValue, alreadyProcessed);
			}
		}
	}
}

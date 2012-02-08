package ch.unibe.iam.scg;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.RuntimeErrorException;

import javassist.CannotCompileException;
import javassist.ClassMap;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.AccessFlag;
import ch.unibe.iam.scg.rewriter.ClassRewriter;
import ch.unibe.iam.scg.rewriter.GenerateAccessorsRewriter;
import ch.unibe.iam.scg.rewriter.InterceptAccessorsRewriter;
import ch.unibe.iam.scg.rewriter.MapDependenciesRewriter;
import ch.unibe.iam.scg.rewriter.helper.ArrayInterceptor;

public class ContextClassLoader extends InstrumentingClassLoader {
	
	private ContextClassLoader next;
	
	public void setNext( ContextClassLoader n )
	{
		next = n;
	}
	
	public ContextClassLoader newNext() {
		//String newSuffix = "$$" + String.valueOf( Integer.valueOf( this.suffix().substring(2) ).intValue() + 1 );
		return next;
	}
	
	public ContextClassLoader( String suffix ) {
		super( suffix );
	}
	
	public void synchronizeRead(ContextAware obj, int selectedFieldPosition ) {
		///System.out.println("Synchronize read "+field);
		// Sync if dirty & clean flag, then proceeds with read
		this.synchronize(obj, selectedFieldPosition);
	} 
	
	public void synchronizeWrite(ContextAware obj, int selectedFieldPosition ) throws RuntimeException {
		///System.out.println("Synchronize write "+field);
		//  proceeds with write, then invalidate
		// @TODO atm, sync is not performed, it's not needed for one-to-one, but maybe for multi-fields mapping?
		this.invalidateOther(obj, selectedFieldPosition);
	}
	
	public void invalidateOther(ContextAware obj, int selectedFieldPosition ) throws RuntimeException {
		try
		{
			ContextInfo info = obj.getContextInfo();
			
			if( info == null ) {
				throw new NullPointerException();
			}
			
			if( info.global )
			{
				long mask = (1L<<selectedFieldPosition); 
				
				if( info.next == null && info.prev != null )
				{
					ContextAware ctxObj = (ContextAware) info.prev;
					ContextInfo prevInfo = ctxObj.getContextInfo();
					info.dirty = info.dirty & ~mask; // clear current flag 
					prevInfo.dirty = prevInfo.dirty | mask; // set the flag for the other
				}
				else if ( info.prev == null && info.next != null)
				{
					ContextAware ctxObj = (ContextAware) info.next;
					ContextInfo nextInfo = ctxObj.getContextInfo();
					info.dirty = info.dirty & ~mask; // clear current flag 
					nextInfo.dirty = nextInfo.dirty | mask; // set the flag for the other
				}
				else
				{
					throw new RuntimeException( "Context information ares erroneous");
				}		
			}
			else 
			{
				/// System.out.println("Object is not global");
			}
		}
		catch( Exception e )
		{
			throw new RuntimeException( "Could not synchronize field "+selectedFieldPosition, e);
		}
	}
	
	public void synchronize(ContextAware obj, int selectedFieldPosition ) throws RuntimeException {
		try
		{
			ContextInfo info = obj.getContextInfo();
			
			if( info == null ) {
				throw new NullPointerException();
			}
			
			if( info.global )
			{
				long mask = (1L<<selectedFieldPosition); 
				
				if( (info.dirty & mask ) > 0 ) {
					
					if( info.next == null && info.prev != null )
					{
						synchronizeFromPrev(obj,info,selectedFieldPosition);
					}
					else if ( info.prev == null && info.next != null)
					{
						synchronizeFromSucc(obj,info,selectedFieldPosition);
					}
					else
					{
						throw new RuntimeException( "Context information ares erroneous");
					}
					
					// Clear the flag
					info.dirty = info.dirty & ~mask;
				}
				else 
				{
					// System.out.println("Object is global but up-to-date");
				}
			}
			else 
			{
				/// System.out.println("Object is not global");
			}
		}
		catch( Exception e )
		{
			throw new RuntimeException( "Could not synchronize field "+selectedFieldPosition, e);
		}
	}
	
	public void synchronizeFromSucc(Object obj, ContextInfo info, int selectedFieldPosition) throws Exception {
		
		// synchronize with reflection
		// System.out.println("Synchronize from ancestor");
		
		Object next = info.next;
		Field[] nextFields = orderedFields( nonFinalfieldsOfClassesOnly( next.getClass() ) );
		if( selectedFieldPosition > nextFields.length) {
			throw new IndexOutOfBoundsException();
		}
		Field nextField = nextFields[ selectedFieldPosition];
	
		if( nextField.getName().equals( ClassRewriter.CONTEXT_INFO )) 
		{ 
			return ;
		}
		
		Field prevField = orderedFields( nonFinalfieldsOfClassesOnly( obj.getClass() ) )[ selectedFieldPosition ];
		Object nextValue = nextField.get(next);
		
		//System.out.println("Prev type:"+ ( prevValue==null?"null":prevValue.getClass().toString()));
		
		if( nextValue == null ) {
			prevField.set(obj, null );
		}
		else if( nextValue instanceof ContextAware ) {
			ContextAware nextAware = (ContextAware) nextValue;
			
			if( ! nextAware.getContextInfo().global			) 
				// this is an old migrated instance, it should be not global and prev=null, next=null
				// but this is not the case because we don't have forced garbage collection
			{
				throw new RuntimeException("Not implemented yet");
			}
			else {
				prevField.set(obj, nextAware.getContextInfo().prev );
			}			
			
			// @TODO children can be array -- test for all types
		} else if ( nextValue instanceof int[] ) {
			int[] nextArray = (int[]) nextValue;
			
			if( ! ArrayInterceptor.contextInfoOfArray(nextValue).global )
			{
				prevField.set(obj, migrateIntArrayToPrev(nextArray));
				assert( ArrayInterceptor.contextInfoOfArray(nextValue).global == true);
			}
			else {
				prevField.set(obj, ArrayInterceptor.contextInfoOfArray(nextValue).prev );
			}	
			
			
		} else if ( nextValue instanceof Object[] ) {
			Object[] nextArray = (Object[]) nextValue;
			
			if( ! ArrayInterceptor.contextInfoOfArray(nextValue).global )
			{
				prevField.set(obj, migrateObjArrayToPrev(nextArray));
				assert( ArrayInterceptor.contextInfoOfArray(nextValue).global == true);
			}
			else {
				prevField.set(obj, ArrayInterceptor.contextInfoOfArray(nextValue).next );
			}	
			
			// @TODO this include null -- is that correct?
		} else if( nextValue.getClass().isArray() ){
			throw new RuntimeException("Unsupported type:" + nextValue.getClass().toString() );
		} else if( nextValue.getClass() == Class.class ){
			throw new RuntimeException( "Not implemented yet");
		}
		else
		{
			// primitive
			prevField.set(obj, nextValue);
		}
		
	}
	
	
	public void synchronizeFromPrev(Object obj, ContextInfo info, int selectedFieldPosition) throws Exception {
		// synchronize with reflection
		// System.out.println("Synchronize from ancestor");
		
		Object prev = info.prev;
		Field[] prevFields = orderedFields( nonFinalfieldsOfClassesOnly( prev.getClass() ) );
		if( selectedFieldPosition > prevFields.length) {
			throw new IndexOutOfBoundsException();
		}
		Field prevField = prevFields[ selectedFieldPosition];
	
		if( prevField.getName().equals( ClassRewriter.CONTEXT_INFO )) 
		{ 
			return ;
		}
		
		Field nextField = orderedFields( nonFinalfieldsOfClassesOnly( obj.getClass() ) )[ selectedFieldPosition ];
		Object prevValue = prevField.get(prev);
		
		//System.out.println("Prev type:"+ ( prevValue==null?"null":prevValue.getClass().toString()));
		
		if( prevValue == null ) {
			nextField.set(obj, null );
		}
		else if( prevValue instanceof ContextAware ) {
			ContextAware prevAware = (ContextAware) prevValue;
			
			ContextAware nextAware = (ContextAware) migrateToNextIfNecessary(prevAware, this);
			nextField.set(obj, nextAware);
			
//			if( ! prevAware.getContextInfo().global ||
//					( prevAware.getContextInfo().global && prevAware.getContextInfo().next == null)) 
//				// this is an old migrated instance, it should be not global and prev=null, next=null
//				// but this is not the case because we don't have forced garbage collection
//			{
//				nextField.set(obj, prevAware.migrateToNext(this));
//				assert( prevAware.getContextInfo().global == true);
//			}
//			else {
//				nextField.set(obj, prevAware.getContextInfo().next );
//			}			
//			
			// @TODO children can be array -- test for all types
		} else if ( prevValue instanceof int[] ) {
			int[] prevArray = (int[]) prevValue;
			
			if( ! ArrayInterceptor.contextInfoOfArray(prevValue).global )
			{
				nextField.set(obj, migrateIntArrayToNext(prevArray));
				assert( ArrayInterceptor.contextInfoOfArray(prevValue).global == true);
			}
			else {
				nextField.set(obj, ArrayInterceptor.contextInfoOfArray(prevValue).next );
			}	
			
			
		} else if ( prevValue instanceof Object[] ) {
			Object[] prevArray = (Object[]) prevValue;
			
			if( ! ArrayInterceptor.contextInfoOfArray(prevValue).global )
			{
				nextField.set(obj, migrateObjArrayToNext(prevArray));
				assert( ArrayInterceptor.contextInfoOfArray(prevValue).global == true);
			}
			else {
				nextField.set(obj, ArrayInterceptor.contextInfoOfArray(prevValue).next );
			}	
			
			// @TODO this include null -- is that correct?
		} else if( prevValue.getClass().isArray() ){
			throw new RuntimeException("Unsupported type:" + prevValue.getClass().toString() );
		} else if( prevValue.getClass() == Class.class ){
			Class newClass = this.resolve( ((Class)prevValue).getName());
			nextField.set(obj, newClass);
		}
		else
		{
			// primitive
			nextField.set(obj, prevValue);
		}
		
	}
	
	// OK to be static, there are built-in
	public static void synchronizeArrayRead(Object obj, int pos ) {
		ContextInfo info = ArrayInterceptor.contextInfoOfArray(obj);

		//@TODO array might be returned by core, uninstrumented framework class, which
		//we assume to not be global
		if( info == null || ! info.global )
			return;
		
		if( info.dirty != 0 )
		{
			if( info.next == null && info.prev != null )
			{
				if( obj instanceof int[] )
				{
					int[] a = (int[]) obj;
					//@TODO will not work for Object[]	-- need to migrate if necessary
					System.arraycopy( info.prev, 0, a, 0, a.length);
					info.dirty = 0x0;
				}
				//@TODO thread aren't nice for us, should be handled one way of the other
				else if( obj instanceof  Object[] )
				{
					Object[] oldObjs = (Object[]) info.prev;
					Object[] objs = (Object[]) obj;	
					for( int i=0; i<oldObjs.length; i++ )
					{
						// hack we need to take the most specific loaded,
						// otherwise we might get null if the type of the array was loaded by 
						// java.lang classloader
						if( oldObjs[i] != null ) {
							ContextClassLoader loader = (ContextClassLoader) oldObjs[i].getClass().getClassLoader();
							objs[i] = migrateToNextIfNecessary( oldObjs[i] , loader.next );
						}
					}
				}
				else 
				{
					throw new RuntimeException("Not implemented yet");
				}
			}
			else if ( info.prev == null && info.next != null)
			{
				throw new RuntimeException( "Not implemented yet" );
			}
			else
			{
				throw new RuntimeException( "Context information ares erroneous");
			}		
			
		}
	}
	
	public static  Object migrateToNextIfNecessary( Object prev, ContextClassLoader nextLoader )
	{
		if( prev.getClass().getName().contains("org.mortbay.thread.BoundedThreadPool$PoolThread"))
		{
			int k=0;
			k++;
		}
		
		if( prev instanceof ContextAware )
		{
			ContextAware prevAware = (ContextAware) prev;
			if( ! prevAware.getContextInfo().global ||
					( prevAware.getContextInfo().global && prevAware.getContextInfo().next == null)) 
				// this is an old migrated instance, it should be not global and prev=null, next=null
				// but this is not the case because we don't have forced garbage collection
			{
				return prevAware.migrateToNext(nextLoader);
			}
			else {
				return prevAware.getContextInfo().next;
			}
		}
		else
		{
			return prev;
		}
	}
	
	public static  Object migrateToPrevIfNecessary( Object next, ContextClassLoader nextLoader )
	{
		if( next instanceof ContextAware )
		{
			ContextAware nextAware = (ContextAware) next;
			if( ! nextAware.getContextInfo().global ) 
				// this is an old migrated instance, it should be not global and prev=null, next=null
				// but this is not the case because we don't have forced garbage collection
			{
				throw new RuntimeException("Not implemented yet");
				//return nextAware.migrateToPrev(nextLoader);
			}
			else {
				return nextAware.getContextInfo().prev;
			}
		}
		else
		{
			return next;
		}
	}
	
	public static void synchronizeArrayWrite(Object obj, int pos ) {
		ContextInfo info = ArrayInterceptor.contextInfoOfArray(obj);
		
		//@TODO array might be returned by core, uninstrumented framework class, which
		//we assume to not be global
		if( info == null || ! info.global )
			return;
		
		if( info.next == null && info.prev != null )
		{
			ContextInfo prevInfo = ArrayInterceptor.contextInfoOfArray(info.prev);
			
			if( obj instanceof int[] )
			{
				int[] a = (int[]) obj;
				System.arraycopy( info.prev, 0, a, 0, a.length);  // make sure the whole array is up-to-date
			}
			//@TODO thread aren't nice for us, should be handled one way of the other
			else if( obj instanceof  Object[] )
			{
				Object[] oldObjs = (Object[]) info.prev;
				Object[] objs = (Object[]) obj;	
				for( int i=0; i<oldObjs.length; i++ )
				{
					// hack we need to take the most specific loaded,
					// otherwise we might get null if the type of the array was loaded by 
					// java.lang classloader
					if( oldObjs[i] != null ) {
						ContextClassLoader loader = (ContextClassLoader) oldObjs[i].getClass().getClassLoader();
						objs[i] = migrateToNextIfNecessary( oldObjs[i] , loader.next );
					}
				}
			}
			else
			{
				//@TODO will not work for Object[]					
				throw new RuntimeException("Not implemented yet");
			}
			
			info.dirty = 0x0; // clear current flag 
			prevInfo.dirty = 0x1; // set the flag for the other
		}
		else if ( info.prev == null && info.next != null)
		{
			ContextInfo nextInfo = ArrayInterceptor.contextInfoOfArray(info.next);
			
			if( obj instanceof int[] )
			{
				int[] a = (int[]) obj;
				System.arraycopy( info.next, 0, a, 0, a.length);  // make sure the whole array is up-to-date
			}
			//@TODO thread aren't nice for us, should be handled one way of the other
			else if( obj instanceof  Object[] )
			{
				Object[] newObjs = (Object[]) info.next;
				Object[] objs = (Object[]) obj;
				for( int i=0; i<newObjs.length; i++ )
				{
					// hack we need to take the most specific loaded,
					// otherwise we might get null if the type of the array was loaded by 
					// java.lang classloader
					if( newObjs[i] != null ) {
						ContextClassLoader loader = (ContextClassLoader) newObjs[i].getClass().getClassLoader();
						objs[i] = migrateToPrevIfNecessary( newObjs[i] , loader );
					}
				}
			}
			else
			{
				//@TODO will not work for Object[]					
				throw new RuntimeException("Not implemented yet");
			}
			
			info.dirty = 0x0; // clear current flag 
			nextInfo.dirty = 0x1; // set the flag for the other
		}
		else
		{
			throw new RuntimeException( "Context information ares erroneous");
		}		
			
	}
	
	public int[] migrateIntArrayToNext( int[] array ) {
		//@TODO should be new int[ array.length ]
		int[] array2 = array.clone();  
		ArrayInterceptor.registerArray(array2);
		ContextInfo info1 = ArrayInterceptor.contextInfoOfArray(array);
		ContextInfo info2 = ArrayInterceptor.contextInfoOfArray(array2);
		info1.global = true;
		info2.global = true;
		info1.next = array2;
		info2.prev = array;
		info2.dirty = 0xFFFFFFFF;
		info1.dirty = 0x0000;
		return array2;
	}
	
	public Object[] migrateObjArrayToNext( Object[] array ) {
		String oldTypeName = array.getClass().getComponentType().getName();
		Class newTypeClass = this.resolve(oldTypeName);
		Object array2 = Array.newInstance(newTypeClass, array.length);
		ArrayInterceptor.registerArray(array2);
		ContextInfo info1 = ArrayInterceptor.contextInfoOfArray(array);
		ContextInfo info2 = ArrayInterceptor.contextInfoOfArray(array2);
		info1.global = true;
		info2.global = true;
		info1.next = array2;
		info2.prev = array;
		info2.dirty = 0xFFFFFFFF;
		info1.dirty = 0x0000;
		return (Object[]) array2;
	}
	
	public int[] migrateIntArrayToPrev( int[] array ) {
		//@TODO should be new int[ array.length ]
		int[] array0 = array.clone();  
		ArrayInterceptor.registerArray(array0);
		ContextInfo info1 = ArrayInterceptor.contextInfoOfArray(array);
		ContextInfo info0 = ArrayInterceptor.contextInfoOfArray(array0);
		info1.global = true;
		info0.global = true;
		info0.next = array;
		info1.prev = array0;
		info0.dirty = 0xFFFFFFFF;
		info1.dirty = 0x0000;
		return array0;
	}
	
	public Object[] migrateObjArrayToPrev( Object[] array ) {
		throw new RuntimeException("Not implemented yet");
//		String newTypeName = array.getClass().getComponentType().getName();
//		Class oldTypeClass = this.resolve(newTypeName);
//		Object array2 = Array.newInstance(newTypeClass, array.length);
//		ArrayInterceptor.registerArray(array2);
//		ContextInfo info1 = ArrayInterceptor.contextInfoOfArray(array);
//		ContextInfo info2 = ArrayInterceptor.contextInfoOfArray(array2);
//		info1.global = true;
//		info0.global = true;
//		info0.next = array;
//		info1.prev = array0;
//		info0.dirty = 0xFFFFFFFF;
//		info1.dirty = 0x0000;
//		return (Object[]) array0;
	}
	
	@Override
	protected void finalize() throws Throwable {
		
		System.out.println("*****Finalized");
		super.finalize();
	}
	

	private int depthOf( Class c )
	{
		
		if( c.getName().equals( "java.lang.Object") )
			return 0;
		else
		{
			if( c.getSuperclass() == null )
			{
				throw new NullPointerException();
			}
		
			return 1 + depthOf( c.getSuperclass() );
		}
	}
	
	private Field[] orderedFields( Field[] fields )
	{
		Arrays.sort( fields, new Comparator<Field>() {
	
			public int compare(Field o1, Field o2) {
				int d1 = depthOf( o1.getDeclaringClass());
				int d2 = depthOf( o2.getDeclaringClass());
				if( d1 < d2 )
					return -1 ;
				else if ( d1 == d2 )
					return o1.getName().compareTo(o2.getName());
				else 
					return 1;	
			}
			
		});
		return fields;
	}
	
	private Field[] nonFinalfieldsOfClassesOnly( Class c ) 
	{
		Class current = c;
		List<Field> fields = new ArrayList<Field>();
		while( ! current.getName().equals( "java.lang.Object") )
		{
			for( Field f : current.getDeclaredFields() ) {
				if(( f.getModifiers() & AccessFlag.FINAL ) == 0 )
					fields.add(f);
			}
			current = current.getSuperclass();
		}
		return ( Field[] ) fields.toArray( new Field[0]);
	}
}

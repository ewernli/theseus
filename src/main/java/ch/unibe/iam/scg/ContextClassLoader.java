package ch.unibe.iam.scg;

import java.io.IOException;
import java.lang.ref.WeakReference;
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
	
	protected ContextClassLoader next;
	protected WeakReference<ContextClassLoader> prev;
	
	//@TODO it's not a true prev-next relationship
	public void setNext( ContextClassLoader n )
	{
		next = n;
		prev = new WeakReference<ContextClassLoader>(this);
		next.prev = new WeakReference<ContextClassLoader>(this);
		next.next = next;
	}

	private ContextClassLoader getNext()
	{
		return next;
		
	}
	
	protected ContextClassLoader getPrev() 
	{
		return prev.get();
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
						// The next class loader know the mapping, not the old one
						this.next.synchronizeFromSucc(obj,info,selectedFieldPosition);
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
		
		Object prevValue = this.migrateToPrevIfNecessary(nextValue);
		try
		{
			prevField.set(obj, prevValue);
		}
		catch(Exception e)
		{
			throw new RuntimeException("Ooops ", e);
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
		
		Object nextValue = this.migrateToNextIfNecessary(prevValue);
		try
		{
			nextField.set(obj, nextValue);
		}
		catch(Exception e)
		{
			throw new RuntimeException("Ooops convertion failed",e);
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
					System.arraycopy( info.prev, 0, a, 0, a.length);	
				}
				else if( obj instanceof byte[] )
				{
					byte[] a = (byte[]) obj;
					System.arraycopy( info.prev, 0, a, 0, a.length);	
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
							ClassLoader loader = oldObjs[i].getClass().getClassLoader();							
							if( loader instanceof ContextClassLoader )
								objs[i] = ((ContextClassLoader)loader).next.migrateToNextIfNecessary( oldObjs[i] );
							else // null or system class loader
								objs[i] = oldObjs[i];
								
						}
					}
				}
				else 
				{
					throw new RuntimeException("Not implemented yet");
				}
				info.dirty = 0x0;
			}
			else if ( info.prev == null && info.next != null)
			{
				if( obj instanceof int[] )
				{
					int[] a = (int[]) obj;
					//@TODO will not work for Object[]	-- need to migrate if necessary
					System.arraycopy( info.next, 0, a, 0, a.length);			
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
							
							ClassLoader loader = newObjs[i].getClass().getClassLoader();							
							if( loader instanceof ContextClassLoader )
								objs[i] = ((ContextClassLoader)loader).migrateToPrevIfNecessary( newObjs[i] );
							else // null or system class loader
								objs[i] = newObjs[i];
						}
					}
				}
				else 
				{
					throw new RuntimeException("Not implemented yet");
				}
				info.dirty = 0x0;
			}
			else
			{
				throw new RuntimeException( "Context information ares erroneous");
			}		
			
		}
	}
	
	public  Object migrateToNextIfNecessary( Object prev )
	{
		// this is the NEXT loader
		if( prev == null ) {
			return null;
		}
		
		// migration is exclusive
		synchronized( prev ) {
			if( prev instanceof ContextAware )
			{
				ContextAware prevAware = (ContextAware) prev;
				if( ! prevAware.getContextInfo().global ||
						( prevAware.getContextInfo().global && prevAware.getContextInfo().next == null)) 
					// this is an old migrated instance, it should be not global and prev=null, next=null
					// but this is not the case because we don't have forced garbage collection
				{
					return prevAware.migrateToNext( ((ContextClassLoader)this));
				}
				else {
					return prevAware.getContextInfo().next;
				}
			}
			else if ( prev instanceof int[] ) {
				int[] prevArray = (int[]) prev;
				
				if( ! ArrayInterceptor.contextInfoOfArray(prev).global )
				{
					return migrateIntArrayToNext(prevArray);
				}
				else {
					return ArrayInterceptor.contextInfoOfArray(prev).next;
				}	
				
			} else if ( prev instanceof byte[] ) {
				byte[] prevArray = (byte[]) prev;
				
				if( ! ArrayInterceptor.contextInfoOfArray(prev).global )
				{
					return migrateByteArrayToNext(prevArray);
				}
				else {
					return ArrayInterceptor.contextInfoOfArray(prev).next ;
				}	
				
				
			} else if ( prev instanceof Object[] ) {
				Object[] prevArray = (Object[]) prev;
				
				if( ! ArrayInterceptor.contextInfoOfArray(prev).global )
				{
					return migrateObjArrayToNext(prevArray);
				}
				else {
					return ArrayInterceptor.contextInfoOfArray(prev).next ;
				}	
			} else if( prev.getClass().isArray() ){
				throw new RuntimeException("Unsupported type:" + prev.getClass().toString() );
			} else if( prev.getClass() == Class.class ){
				return this.resolve( ((Class)prev).getName());
			}
			else
			{
				return prev;
			}
		}
	}
	
	public  Object migrateToPrevIfNecessary( Object next )
	{
		// this reference the NEXT loader
		
		if( next == null ) {
			return null;
		}
		else if( next instanceof ContextAware )
		{
			ContextAware nextAware = (ContextAware) next;
			if( ! nextAware.getContextInfo().global ) 
				// this is an old migrated instance, it should be not global and prev=null, next=null
				// but this is not the case because we don't have forced garbage collection
			{
				//@TODO ugly, the should be a method migrateToPrev()
				ContextAware prevAware = nextAware.migrateToNext(((ContextClassLoader)this).getPrev());
				ContextInfo prevInfo = prevAware.getContextInfo();
				ContextInfo nextInfo = nextAware.getContextInfo();
				prevInfo.next = next;
				prevInfo.prev = null;
				nextInfo.prev = prevAware;
				nextInfo.next = null;
				return prevAware;
			}
			else {
				return nextAware.getContextInfo().prev;
			}
		} 
		else  if ( next instanceof int[] ) {
			int[] nextArray = (int[]) next;
			
			if( ! ArrayInterceptor.contextInfoOfArray(next).global )
			{
				return migrateIntArrayToPrev(nextArray);
			}
			else {
				return ArrayInterceptor.contextInfoOfArray(next).prev;
			}	
			
		} else if ( next instanceof byte[] ) {
			byte[] nextArray = (byte[]) next;
			
			if( ! ArrayInterceptor.contextInfoOfArray(next).global )
			{
				return migrateByteArrayToPrev(nextArray);
			}
			else {
				return ArrayInterceptor.contextInfoOfArray(next).prev;
			}	
			
			
		} else if ( next instanceof Object[] ) {
			Object[] nextArray = (Object[]) next;
			
			if( ! ArrayInterceptor.contextInfoOfArray(next).global )
			{
				return migrateObjArrayToPrev(nextArray);
			}
			else {
				return ArrayInterceptor.contextInfoOfArray(next).next;
			}	
			// @TODO this include null -- is that correct?
		} else if( next.getClass().isArray() ){
			throw new RuntimeException("Unsupported type:" + next.getClass().toString() );
		} else if( next.getClass() == Class.class ){
			throw new RuntimeException( "Not implemented yet");
		} else
		{
			return next;
		}
	}
	
	public static void synchronizeArrayWrite(Object obj, int pos ) {
		
		// make sure the whole array is up-to-date
		synchronizeArrayRead(obj, pos);
		
		ContextInfo info = ArrayInterceptor.contextInfoOfArray(obj);
		
		//@TODO array might be returned by core, uninstrumented framework class, which
		//we assume to not be global
		if( info == null || ! info.global )
			return;
		
		// Now invalidate the other flag
		if( info.next == null && info.prev != null )
		{
			ContextInfo prevInfo = ArrayInterceptor.contextInfoOfArray(info.prev);			
			info.dirty = 0x0; // clear current flag 
			prevInfo.dirty = 0x1; // set the flag for the other
		}
		else if ( info.prev == null && info.next != null)
		{
			ContextInfo nextInfo = ArrayInterceptor.contextInfoOfArray(info.next);		
			info.dirty = 0x0; // clear current flag 
			nextInfo.dirty = 0x1; // set the flag for the other
		}
		else
		{
			throw new RuntimeException( "Context information ares erroneous");
		}		
			
	}
	
	public byte[] migrateByteArrayToNext( byte[] array ) {
		//@TODO should be new int[ array.length ]
		// But givent that we do not have per cell dirty flag,
		// we can clone and flag as clean
		byte[] array2 = array.clone();  
		ArrayInterceptor.registerArray(array2);
		ContextInfo info1 = ArrayInterceptor.contextInfoOfArray(array);
		ContextInfo info2 = ArrayInterceptor.contextInfoOfArray(array2);
		info1.global = true;
		info2.global = true;
		info1.next = array2;
		info2.prev = array;
		info2.dirty = 0x0000;
		info1.dirty = 0x0000;
		return array2;
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
		info2.dirty = 0x0000;
		info1.dirty = 0x0000;
		return array2;
	}
	
	public Object[] migrateObjArrayToNext( Object[] array ) {
		// this is the next class loader
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

	public byte[] migrateByteArrayToPrev( byte[] array ) {
		//@TODO should be new int[ array.length ]
		byte[] array0 = array.clone();  
		ArrayInterceptor.registerArray(array0);
		ContextInfo info1 = ArrayInterceptor.contextInfoOfArray(array);
		ContextInfo info0 = ArrayInterceptor.contextInfoOfArray(array0);
		info1.global = true;
		info0.global = true;
		info0.next = array;
		info1.prev = array0;
		info0.dirty = 0x0000;
		info1.dirty = 0x0000;
		return array0;
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
		info0.dirty = 0x0000;
		info1.dirty = 0x0000;
		return array0;
	}
	
	public Object[] migrateObjArrayToPrev( Object[] array ) {
		String newTypeName = array.getClass().getComponentType().getName();
		Class oldTypeClass = this.getPrev().resolve(newTypeName);
		Object array0 = Array.newInstance(oldTypeClass, array.length);
		ArrayInterceptor.registerArray(array0);
		ContextInfo info1 = ArrayInterceptor.contextInfoOfArray(array);
		ContextInfo info0 = ArrayInterceptor.contextInfoOfArray(array0);
		info1.global = true;
		info0.global = true;
		info0.next = array;
		info1.prev = array0;
		info0.dirty = 0xFFFFFFFF;
		info1.dirty = 0x0000;
		return (Object[]) array0;
	}
		

	public static int depthOf( Class c )
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
	
	public static Field[] orderedFields( Field[] fields )
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
	
	public static Field[] nonFinalfieldsOfClassesOnly( Class c ) 
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

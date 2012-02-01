package ch.unibe.iam.scg;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

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
		this.synchronize(obj, selectedFieldPosition);
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
					info.dirty = info.dirty ^ mask;
				}
				else 
				{
					System.out.println("Object is global but up-to-date");
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
		System.out.println("Synchronize from successor");
		
		Object next = info.next;
		Field prevField = obj.getClass().getFields()[ selectedFieldPosition ];
		Field nextField = next.getClass().getFields()[ selectedFieldPosition ];
		Object nextValue = nextField.get(next);
		prevField.set(obj, nextValue);
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
	
	public void synchronizeFromPrev(Object obj, ContextInfo info, int selectedFieldPosition) throws Exception {
		// synchronize with reflection
		System.out.println("Synchronize from ancestor");
		
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
		
		System.out.println("Prev type:"+ ( prevValue==null?"null":prevValue.getClass().toString()));
		
		if( prevValue == null ) {
			nextField.set(obj, null );
		}
		else if( prevValue instanceof ContextAware ) {
			ContextAware prevAware = (ContextAware) prevValue;
			
			if( ! prevAware.getContextInfo().global ||
					( prevAware.getContextInfo().global && prevAware.getContextInfo().next == null)) 
				// this is an old migrated instance, it should be not global and prev=null, next=null
				// but this is not the case because we don't have forced garbage collection
			{
				nextField.set(obj, prevAware.migrateToNext(this));
				assert( prevAware.getContextInfo().global == true);
			}
			else {
				nextField.set(obj, prevAware.getContextInfo().next );
			}			
			
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
		}
		else
		{
			// primitive
			nextField.set(obj, prevValue);
		}
		
	}
	
	// OK to be static, there are built-in
	public static void synchronizeArrayRead(Object obj, int pos ) {
		///System.out.println("Synchronize read[] "+pos);
		//this.synchronize(obj, field);
	}
	
	public static void synchronizeArrayWrite(Object obj, int pos ) {
		///System.out.println("Synchronize write[] "+pos);
		//this.synchronize(obj, field);
	}
	
	public void synchronizeArray( Object obj, int pos ) {
		
	}
	
	public int[] migrateIntArrayToNext( int[] array ) {
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
		Object[] array2 = array.clone();
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
	
	@Override
	protected void finalize() throws Throwable {
		// TODO Auto-generated method stub
		super.finalize();
	}
	
	public void perform( Runnable action ) throws Exception {
		// @TODO ugly type machinery, but that should work after the compilation
		Runnable a2 = (Runnable) ((ContextAware) action).migrateToNext(this);
		a2.run();
	}
	
}

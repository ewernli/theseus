package ch.unibe.iam.scg;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javassist.CannotCompileException;
import javassist.ClassMap;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.NotFoundException;
import ch.unibe.iam.scg.rewriter.ArrayInterceptor;
import ch.unibe.iam.scg.rewriter.ClassRewriter;
import ch.unibe.iam.scg.rewriter.GenerateAccessorsRewriter;
import ch.unibe.iam.scg.rewriter.InterceptAccessorsRewriter;
import ch.unibe.iam.scg.rewriter.RenameDependenciesRewriter;

public class ContextClassLoader extends javassist.Loader{

	private final String versionSuffix;
	private List<CtClass> toRewire = new ArrayList<CtClass>();
	
	public ContextClassLoader( String suffix )
	{
		versionSuffix = suffix;
	}

	class ContextClassMap extends ClassMap {
		   public Object get(Object jvmClassName) {
		     String name = (String)jvmClassName;
		     if (needsRewrite(name))
		         return name + versionSuffix;
		     else
		         return name;
		   }
		 }
	
	private void setClassName( CtClass clazz, String className ) throws CannotCompileException, NotFoundException
	{
		String oldName = clazz.getName();
		CtConstructor[] ctrs = clazz.getConstructors();
		clazz.setName( className );
	/*	for( CtConstructor ctr : ctrs ){
			
			CtConstructor nctr = javassist.CtNewConstructor.copy( ctr, clazz, null ) ; //new ContextClassMap() );
			clazz.removeConstructor(ctr);
			clazz.addConstructor(nctr);
			System.out.println("Constructor "+ ctr.getName() + "/"+ nctr.getName() );
		}
		*/
	}
	
	public CtClass findCtClass( String className ) throws ClassNotFoundException, CannotCompileException, NotFoundException
	{
		ClassPool cp = ClassPool.getDefault();
		CtClass clazz = null;
		try
		{
			clazz = cp.get(className);
			System.out.println("Found CtFind "+className);
		}
		catch( NotFoundException e )
		{
			System.out.println("Did not CtFind "+className);	
			String unversionedName = className.substring(0, className.length() - versionSuffix.length());
			clazz = cp.get(unversionedName);
			// @TODO fix hack
			int oldSize = toRewire.size();
			toRewire.add(clazz);
			
			setClassName( clazz, className );
			String superClassName = clazz.getSuperclass().getName();
			CtClass superClazz = null;
			if( needsRewrite(superClassName))
			{
				 superClazz = findCtClass( superClassName + versionSuffix );
				 //superClazz.toClass();
			}
			else
			{
				 superClazz = cp.get(superClassName);
			}
			clazz.setSuperclass( superClazz );
			// @TODO document that superclass must haven been instrumented 
	    	new GenerateAccessorsRewriter().rewrite(clazz);
	    	new RenameDependenciesRewriter( this, this.versionSuffix ).rewrite(clazz);		
			
	    	// @TODO fix super ugly hack
	    	if( oldSize == 0 ) {
	    		for( CtClass clazz2 : toRewire ) {
	    			new InterceptAccessorsRewriter().rewrite(clazz2);
	    			try {
	    				clazz2.writeFile();
	    				clazz2.defrost();
	    			}catch (IOException e2) {
	    				// TODO Auto-generated catch block
	    				e2.printStackTrace();
	    			} catch (NotFoundException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}
	    		}		
	    	}
		};
		
		//clazz.toClass();
		return clazz;
	}
	
	@Override
	protected Class<?> findClass(String className) throws ClassNotFoundException {
		
		System.out.println("Find "+className);
		
		if( needsRewrite(className) == false)
		{
			// We delegate to the parent, not super. 
			// Super means the loaded class would be bound in our class loader. 
			// Delegation implies it is bound by the parent.
			// We can have several sibling contextual loader, that can shared base types, e.g. ContextInfo, or 
			// the type ContextualClassLoader itself. 
			return this.getParent().loadClass(className);
		}
		
		try {
			CtClass clazz = findCtClass(className);
			byte[] b = clazz.toBytecode();
			return defineClass(className, b, 0, b.length);
		} catch (CannotCompileException e) {
			e.printStackTrace();
			throw new ClassNotFoundException();
		} catch (NotFoundException e) {
			e.printStackTrace();
			throw new ClassNotFoundException();
		} catch (IOException e) {
			e.printStackTrace();
			throw new ClassNotFoundException();
		}
	}

	private String rewriteName( String className ) {
		return className + versionSuffix;
	}
	
	private boolean needsRewrite( String className ) {
		return className.startsWith("ch.unibe.iam.scg.test");
	}
	

	// @TODO fix name should be the original
	public Class resolve(String oldInstrumentedName)
	{
		try {
			String originalName = oldInstrumentedName.substring(0, oldInstrumentedName.length() - versionSuffix.length() );
			return this.loadClass(originalName+this.versionSuffix);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException( "Could not find class", e );
		}
	}
	
	public void synchronizeRead(ContextAware obj, String field ) {
		System.out.println("Synchronize read "+field);
		try {
			// Sync if dirty & clean flag, then proceeds with read
			this.synchronize(obj, field);
		} catch (SecurityException e) {
			throw new RuntimeException( "Cound not synchronize",e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException( "Cound not synchronize",e);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException( "Cound not synchronize",e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException( "Cound not synchronize",e);
		}
	} 
	
	public void synchronizeWrite(ContextAware obj, String field ) {
		System.out.println("Synchronize write "+field);
		try {
			//  proceeds with write, then invalidate
			// @TODO atm, sync is not performed, it's not needed for one-to-one, but maybe for multi-fields mapping?
			this.synchronize(obj, field);
		} catch (SecurityException e) {
			throw new RuntimeException( "Cound not synchronize",e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException( "Cound not synchronize",e);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException( "Cound not synchronize",e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException( "Cound not synchronize",e);
		}
	}
	
	
	public void synchronize(ContextAware obj, String selectedField ) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		ContextInfo info = obj.getContextInfo();
		if( info.global  && info.dirty )
		{
			if( info.next == null && info.prev != null )
			{
				// synchronize with reflection
				System.out.println("Synchronize from ancestor");
				
				Object prev = info.prev;
				for( Field field : prev.getClass().getFields() ) {
			
					if( field.getName().equals( ClassRewriter.CONTEXT_INFO )) { continue; }
					
					Field prevField = prev.getClass().getField(field.getName());
					Field nextField = obj.getClass().getField(field.getName());
					Object prevValue = prevField.get(prev);
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
						
						// @TODO this include null -- is that correct?
					} else if( prevValue.getClass().isArray() ){
						throw new RuntimeException("Unsupported type");
					}
					else
					{
						// primitive
						nextField.set(obj, prevValue);
					}
				}
			}
			else if ( info.prev == null && info.next != null)
			{
				// synchronize with reflection
				System.out.println("Synchronize from successor");
				
				Object next = info.next;
				Field prevField = obj.getClass().getField(selectedField);
				Field nextField = next.getClass().getField(selectedField);
				Object nextValue = nextField.get(next);
				prevField.set(obj, nextValue);
			}
			else
			{
				throw new RuntimeException( "Ojbect context is erroneous");
			}
			
			// @TODO Clean the flag
			info.dirty = false;
		}
		else if( info.global && !info.dirty )
		{
			System.out.println("Object is global but up-to-date");
		}
		else if( ! info.global )
		{
			System.out.println("Object is not global");
		}
	}
	

	
	// OK to be static, there are built-in
	public static void synchronizeArrayRead(Object obj, int pos ) {
		System.out.println("Synchronize read[] "+pos);
		//this.synchronize(obj, field);
	}
	
	public static void synchronizeArrayWrite(Object obj, int pos ) {
		System.out.println("Synchronize write[] "+pos);
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
		info2.dirty = true;
		info1.dirty = false;
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

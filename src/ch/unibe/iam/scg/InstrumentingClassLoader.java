package ch.unibe.iam.scg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javassist.CannotCompileException;
import javassist.ClassMap;
import javassist.ClassPool;
import javassist.CodeConverter;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.NotFoundException;
import ch.unibe.iam.scg.rewriter.GenerateAccessorsRewriter;
import ch.unibe.iam.scg.rewriter.InterceptAccessorsRewriter;
import ch.unibe.iam.scg.rewriter.MapDependenciesRewriter;
import ch.unibe.iam.scg.rewriter.helper.ArrayInterceptor;

public class InstrumentingClassLoader  extends javassist.Loader {
	private final String versionSuffix;
	private List<CtClass> toRewire = new ArrayList<CtClass>();
	
	public InstrumentingClassLoader( String suffix )
	{
		versionSuffix = suffix;
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
			String unversionedName = unrewriteName( className );
			try {
				clazz = cp.get(unversionedName);
			}
			catch( NotFoundException ex )
			{
				throw ex;
			}
			// @TODO fix hack
			int oldSize = toRewire.size();
			toRewire.add(clazz);
			
			setClassName( clazz, className );
			String superClassName = clazz.getSuperclass().getName();
			CtClass superClazz = null;
			if( needsRewrite(superClassName))
			{
				 superClazz = findCtClass( rewriteName(superClassName) );
				 //superClazz.toClass();
				 clazz.setSuperclass( superClazz );
			}
			/*else
			{
				 superClazz = cp.get(superClassName);
			}
			clazz.setSuperclass( superClazz );*/
			// @TODO document that superclass must haven been instrumented 
	    	new GenerateAccessorsRewriter().rewrite(clazz);
	    	new MapDependenciesRewriter( this, this.versionSuffix ).rewrite(clazz);		
			
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
	    		};
	    		
	    		for( CtClass clazz2 : toRewire ) {
	    			 try {
	    				 CodeConverter conv = new CodeConverter();
	    				 CtClass indirectionClass = ClassPool.getDefault().get(ArrayInterceptor.class.getName());
	    				 conv.replaceArrayAccess( indirectionClass, new CodeConverter.DefaultArrayAccessReplacementMethodNames());
	    				 clazz2.instrument(conv);
	    			} catch (NotFoundException e2) {
	    				throw new CannotCompileException(e2);
	    			} catch (CannotCompileException e2) {
	    				throw e2;
	    			}
	    		};
	    		
	    		toRewire.clear();
	    	}
		};
		
		//clazz.toClass();
		return clazz;
	}
	
	
	
	@Override
	protected synchronized Class<?> loadClass(String name, boolean resolve)
	throws ClassNotFoundException
     {
		System.out.println("Load "+name );
		return super.loadClass(name, resolve);
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
			
			// The class we define must match the name that was provided,
			// any renaming must happen before!
			
			CtClass clazz = findCtClass(className);
			byte[] b = clazz.toBytecode();
			return defineClass(className, b, 0, b.length);
			
		} catch (Throwable e) {
			e.printStackTrace();
			throw new ClassNotFoundException("Could not define" + className, e);
		} 
	}

	private String rewriteName( String className ) {
		return Mapper.rewriteName(className, versionSuffix );
	}
	
	private String unrewriteName( String newName ) {
		return Mapper.unrewriteName(newName, versionSuffix);
	}
	
	private boolean needsRewrite( String className ) {
		return Mapper.needsRewrite(className);
	}
	
	// @TODO fix name should be the original
	public Class resolve(String oldInstrumentedName)
	{
		try {
			String originalName = unrewriteName( oldInstrumentedName );
			return this.loadClass(originalName+this.versionSuffix);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException( "Could not find class", e );
		}
	}
}

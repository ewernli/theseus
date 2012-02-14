package ch.unibe.iam.scg;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javassist.CannotCompileException;
import javassist.ClassMap;
import javassist.ClassPool;
import javassist.CodeConverter;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.NotFoundException;
import ch.unibe.iam.scg.rewriter.AddContextAwarenessRewriter;
import ch.unibe.iam.scg.rewriter.GenerateAccessorsRewriter;
import ch.unibe.iam.scg.rewriter.InterceptAccessorsRewriter;
import ch.unibe.iam.scg.rewriter.MapDependenciesRewriter;
import ch.unibe.iam.scg.rewriter.helper.ArrayInterceptor;

public class InstrumentingClassLoader  extends javassist.Loader {
	private final String versionSuffix;
	private List<CtClass> toRewire = new ArrayList<CtClass>();
	private ProtectionDomain domain;
	
	//private ClassPool cp = new ClassPool( ClassPool.getDefault() ); 
	
	public String suffix()
	{
		return versionSuffix;
	}
	
	public InstrumentingClassLoader( String suffix )
	{
		//cp.appendSystemPath();
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
	
	public synchronized CtClass findCtClass( String className ) throws ClassNotFoundException, CannotCompileException, NotFoundException
	{
		ClassPool cp = ClassPool.getDefault();
		CtClass clazz = null;
		try
		{
			clazz = cp.get(className);
			//System.out.println("Found CtFind "+className);
		}
		catch( NotFoundException e )
		{
			//System.out.println("Did not CtFind "+className);	
			String unversionedName = unrewriteName( className );
			try {
				clazz = cp.get(unversionedName);
			}
			catch( NotFoundException ex )
			{
				//throw new ClassNotFoundException("Could not load class "+className, e);
				return null;
			}
			// @TODO fix hack
			int oldSize = toRewire.size();
			toRewire.add(clazz);
			
			setClassName( clazz, className );
			String superClassName = clazz.getSuperclass().getName();
			CtClass superClazz = null;
			
			if( clazz.getName().contains("Arrays$ArrayList") ||
					clazz.getName().contains("AbstractCollection") ||
					clazz.getName().contains("AbstractList")) {
				int k=0;
				k++;
			}
			
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
	    	new MapDependenciesRewriter( this, this.versionSuffix ).rewrite(clazz);		
			
	    	// @TODO fix super ugly hack
	    	if( oldSize == 0 ) {
	    		
	    		for( CtClass clazz2 : toRewire ) {
	    			new AddContextAwarenessRewriter().rewrite(clazz2);
	    		}
	    		
	    		for( CtClass clazz2 : toRewire ) {
	    			new GenerateAccessorsRewriter().rewrite(clazz2);
	    		}
	    		
	    		for( CtClass clazz2 : toRewire ) {
	    			
	    			new InterceptAccessorsRewriter().rewrite(clazz2);
	    		};
	    		
	    		for( CtClass clazz2 : toRewire ) {
	    			 try {
	    				 if( clazz2.getName().contains("HashMap")) {
	    					 int k=0; k++;
	    				 }
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
	    		
	    		for( CtClass clazz2 : toRewire ) {			
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
		if( name.equals("com.sun.org.apache.xerces.internal.parsers.XIncludeAwareParserConfiguration"))
		{
			int k = 0;
			k++;
			System.out.println("Find "+name);
		}
		
		//System.out.println("Load "+name );
		
		if( needsRewrite(name) ) { //@TODO check if already loaded
			return findClass(name);
		}
		else
		{
			return super.loadClass(name, resolve);
		}
	}

	private ProtectionDomain getDomain() throws MalformedURLException
	{
		if( domain == null ) {
			PermissionCollection perms = new Permissions();
			perms.add(new AllPermission() );
			CodeSource cs = new CodeSource( new URL("file://Theseus") , new Certificate[0] );
			domain = new ProtectionDomain(cs,perms );
		}
		return domain;
	}
	private static HashMap loadedClasses = new HashMap();
	
	@Override
	protected Class<?> findClass(String className) throws ClassNotFoundException {
		
		//System.out.println("Find "+className);
		
		if( needsRewrite(className) == false)
		{
			// We delegate to the parent, not super. 
			// Super means the loaded class would be bound in our class loader. 
			// Delegation implies it is bound by the parent.
			// We can have several sibling contextual loader, that can shared base types, e.g. ContextInfo, or 
			// the type ContextualClassLoader itself. 
			return this.getParent().loadClass( className );
		}
		
		try {
			
			// The class we define must match the name that was provided,
			// any renaming must happen before!
			String newName = Mapper.normalize(className, versionSuffix);
			
//			if( (loadedClasses.size() % 100) == 0 ) {
//				System.out.println("Loading... ("+newName+")");
//			}
			
			if( loadedClasses.containsKey(newName))
			{
				return (Class) loadedClasses.get(newName);
			}
			else
			{
				CtClass clazz = findCtClass( newName );
				if( clazz == null ) return null;
			
				byte[] b = clazz.toBytecode();
				clazz.defrost(); 
				Class c = defineClass(newName, b, 0, b.length, getDomain() );
				clazz.prune();
				loadedClasses.put(newName, c);
				return c;
			}
			
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
	
	// @TODO fix name should be the original, not the instrumented
	public Class resolve(String oldInstrumentedName)
	{
		try {
			//@TODO fix hardcoded suffix 
			if( oldInstrumentedName.contains("$$") || oldInstrumentedName.contains("XX")) {
				String originalName = unrewriteName( oldInstrumentedName );
				return this.loadClass(rewriteName(originalName));
			}
			else
			{
				return this.loadClass(oldInstrumentedName);
			}
		} catch (ClassNotFoundException e) {
			throw new RuntimeException( "Could not find class", e );
		}
	}
}

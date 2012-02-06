package ch.unibe.iam.scg.rewriter;

import java.lang.reflect.Field;
import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import ch.unibe.iam.scg.ContextInfo;
import ch.unibe.iam.scg.rewriter.helper.ReflectionHelper;


import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtNewConstructor;
import javassist.CtField.Initializer;
import javassist.CtMember;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.MethodInfo;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.NewExpr;

public class GenerateAccessorsRewriter implements ClassRewriter {

	private void ensureConstructor(CtClass ctClass) throws CannotCompileException, NotFoundException
	{
		CtClass contextAwareInterface = ClassPool.getDefault().get("ch.unibe.iam.scg.ContextAware");
		CtClass contextInfoType = ClassPool.getDefault().get("ch.unibe.iam.scg.ContextInfo");
		
		if( ! Arrays.asList(ctClass.getInterfaces()).contains(contextAwareInterface) )
		{
			boolean found = false;
			for( CtConstructor cst : ctClass.getConstructors() )
			{
				if( cst.getParameterTypes().length == 1 && 
					cst.getParameterTypes()[0] == contextInfoType )
					found = true;
			}
			
			if( ! found ) {
				ensureConstructor(ctClass.getSuperclass());
				
				String constructor = "public  "+ctClass.getSimpleName()+"(ch.unibe.iam.scg.ContextInfo info) { " +
				"super( info );" +
				"}";
				CtConstructor constructorMethod = CtNewConstructor.make(constructor, ctClass);
				constructorMethod.setModifiers( Modifier.setPublic( constructorMethod.getModifiers()));
				ctClass.addConstructor(constructorMethod);
			}
		}
	}
	
	public void rewrite(CtClass ctClass) throws CannotCompileException {
		try {
			
			if( ctClass.isInterface() ) return;
			
			// System.out.println( "-> Write accessors for "+ ctClass.getName() );
	
			// Generate constructor, if not already existing
			ensureConstructor( ctClass );
		
			// Generate getter/setter
			for (CtField ctField : ctClass.getDeclaredFields()) {			
				boolean isStatic = (ctField.getModifiers() &  AccessFlag.STATIC) == AccessFlag.STATIC;
				
				if( ! isStatic ) {
					generateInstanceAccessor( ctClass, ctField );
				}
				else
				{
					generateClassAccessor( ctClass, ctField );
				}
			}
		} catch (Exception e) {
			throw new CannotCompileException("Error in GenerateAccessors", e);
		}

		// System.out.println( "<- Wrote accessors for "+ ctClass.getName() );
	}

	private int depthOf( CtClass c ) throws NotFoundException
	{
		if( c.getName().equals( "java.lang.Object") )
			return 0;
		else
			return 1 + depthOf( c.getSuperclass() );
	}
	
	private CtField[] nonFinalfieldsOfClassesOnly( CtClass c ) throws NotFoundException 
	{
		CtClass current = c;
		List<CtField> fields = new ArrayList<CtField>();
		while( ! current.getName().equals( "java.lang.Object") )
		{
			for( CtField f : current.getDeclaredFields() ) {
				if(( f.getModifiers() & AccessFlag.FINAL ) == 0 )
					fields.add(f);
			}
			current = current.getSuperclass();
		}
		return ( CtField[] ) fields.toArray( new CtField[0]);
	}
	
	public void generateInstanceAccessor( CtClass ctClass, CtField ctField ) throws Exception
	{
		String fieldName = ctField.getName();
    	String propertyName = 
			fieldName.substring(0, 1).toUpperCase()
			+ fieldName.substring(1);
    	String getter = ClassRewriter.GETTER_PREFIX + propertyName;
    	String setter = ClassRewriter.SETTER_PREFIX + propertyName;

    	try {
			ctClass.getDeclaredMethod(getter);	
			throw new CannotCompileException("Getter "+getter+" already exists");
		} catch (NotFoundException noGetter) {
			// Fine, it should not exist
		}
		
		
		CtField[] orderedFields =  nonFinalfieldsOfClassesOnly ( ctClass ); 
		Arrays.sort( orderedFields, new Comparator<CtField>() {

			public int compare(CtField o1, CtField o2) {			
				try {
					int d1 = depthOf( o1.getDeclaringClass());
					int d2 = depthOf( o2.getDeclaringClass());
					
					if( d1 < d2 )
						return -1 ;
					else if ( d1 == d2 )
						return o1.getName().compareTo(o2.getName());
					else 
						return 1;	
				} catch (NotFoundException e) {
					throw new RuntimeException("Error when sorting", e);
				}
			}
			
		});
		
		if( ctField.getName().contains("_server") ) 
		{
			int k=0;
			k++;
		}
		
		int index = Arrays.asList(orderedFields).indexOf(ctField);
		String getterCode = "public  " + ctField.getType().getName() + " "
				+ getter
				+ "() { " 
				+ " if( this.contextInfo.global )"
				+ " ((ch.unibe.iam.scg.ContextClassLoader) this.getClass().getClassLoader()).synchronizeRead( this, "+ index +" ); "
				+ " return this." + ctField.getName() + ";" 
				+ " }";
		CtMethod getMethod = CtMethod.make(getterCode, ctClass);
		//getMethod.setModifiers( getMethod.getModifiers() | AccessFlag.FINAL ) ;
		ctClass.addMethod(getMethod);

		try {
			ctClass.getDeclaredMethod(setter);
			throw new CannotCompileException("Getter "+setter+" already exists");
		} catch (NotFoundException noSetter) {
			// Fine, it should not exist
		}
					
		String setterCode = "public  void " 
				+ setter
				+ "(" + ctField.getType().getName() + " value) "
				+ "{ " 
				+ "this." + ctField.getName() + " = value; " 
				+ " if( this.contextInfo.global )"
				+ " ((ch.unibe.iam.scg.ContextClassLoader) this.getClass().getClassLoader()).synchronizeWrite( this, "+ index +" ); "
				+ " }";
		CtMethod setMethod = CtMethod.make( setterCode, ctClass );
		//setMethod.setModifiers( setMethod.getModifiers() | AccessFlag.FINAL ) ;
		ctClass.addMethod(setMethod);
//		
//		//Make field public
//		ctField.setModifiers( Modifier.setPublic( ctField.getModifiers()));
	}
	
	public void generateClassAccessor( CtClass ctClass, CtField ctField ) throws Exception
	{
		String fieldName = ctField.getName();
    	String propertyName = 
			fieldName.substring(0, 1).toUpperCase()
			+ fieldName.substring(1);
    	String getter = ClassRewriter.GETTER_PREFIX + propertyName;
    	String setter = ClassRewriter.SETTER_PREFIX + propertyName;

    	try {
			ctClass.getDeclaredMethod(getter);	
			throw new CannotCompileException("Getter "+getter+" already exists");
		} catch (NotFoundException noGetter) {
			// Fine, it should not exist
		}
		
		// @TODO synchronize static field
		String getterCode = "public static " + ctField.getType().getName() + " "
				+ getter
				+ "() { " 
				//+ " ((ch.unibe.iam.scg.ContextClassLoader) this.getClass().getClassLoader()).synchronizeRead( this, \""+ ctField.getName() +"\" ); "
				+ " return " + ctField.getName() + ";" 
				+ " }";
		CtMethod getMethod = CtMethod.make(getterCode, ctClass);
		getMethod.setModifiers( AccessFlag.STATIC );
		getMethod.setModifiers( Modifier.setPublic( getMethod.getModifiers()));					
		ctClass.addMethod(getMethod);

		try {
			ctClass.getDeclaredMethod(setter);
			throw new CannotCompileException("Getter "+setter+" already exists");
		} catch (NotFoundException noSetter) {
			// Fine, it should not exist
		}
				
		// @TODO synchronize static field
		String setterCode = "public static void " 
				+ setter
				+ "(" + ctField.getType().getName() + " val) "
				+ "{ " 
				//+ " ((ch.unibe.iam.scg.ContextClassLoader) this.getClass().getClassLoader()).synchronizeWrite( this, \""+ ctField.getName() +"\" ); "
				+ "" + ctField.getName() + " = val; " 
				+ " }";
		CtMethod setMethod = CtMethod.make( setterCode, ctClass );
		setMethod.setModifiers( AccessFlag.STATIC );
		setMethod.setModifiers( Modifier.setPublic( setMethod.getModifiers()));
		ctClass.addMethod(setMethod);
		
		//Make field public
		ctField.setModifiers( Modifier.setPublic( ctField.getModifiers()));
	}
}

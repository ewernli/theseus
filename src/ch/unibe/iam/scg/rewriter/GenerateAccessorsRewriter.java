package ch.unibe.iam.scg.rewriter;

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

	
	public void rewrite(CtClass ctClass) throws CannotCompileException {
		
		if( ctClass.isInterface() ) return;
		
		System.out.println( "-> Write accessors for "+ ctClass.getName() );
		
		// Make the class public
		ctClass.setModifiers( Modifier.setPublic(ctClass.getModifiers()));
		
		// Make sure the constructor exists for all instrumented classes
		String constructor = "public  "+ctClass.getSimpleName()+"(ch.unibe.iam.scg.ContextInfo info) { " +
		"this." + ClassRewriter.CONTEXT_INFO + " = info;" +
		"}";
		CtConstructor constructorMethod = CtNewConstructor.make(constructor, ctClass);
		constructorMethod.setModifiers( Modifier.setPublic( constructorMethod.getModifiers()));
		ctClass.addConstructor(constructorMethod);

		// Generate getter/setter
		for (CtField ctField : ctClass.getDeclaredFields()) {
			try {
				
				boolean isStatic = (ctField.getModifiers() &  AccessFlag.STATIC) == AccessFlag.STATIC;
				
				if( ! isStatic ) {
					generateInstanceAccessor( ctClass, ctField );
				}
				else
				{
					generateClassAccessor( ctClass, ctField );
				}
				
			} catch (Exception e) {
				throw new CannotCompileException("Error in PropertiesEnhancer", e);
			}
		}

		System.out.println( "<- Wrote accessors for "+ ctClass.getName() );
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
		
		
		CtField[] orderedFields =  ctClass.getFields(); 
		Arrays.sort( orderedFields, new Comparator<CtField>() {

			public int compare(CtField o1, CtField o2) {
				return o1.getName().compareTo(o2.getName());
			}
			
		});
		
		if( ctClass.getName().contains("AbstractConnector")) 
		{
			int k=0;
			k++;
		}
		
		int index = Arrays.asList(orderedFields).indexOf(ctField);
		String getterCode = "public " + ctField.getType().getName() + " "
				+ getter
				+ "() { " 
				+ " ((ch.unibe.iam.scg.ContextClassLoader) this.getClass().getClassLoader()).synchronizeRead( this, "+ index +" ); "
				+ " return this." + ctField.getName() + ";" 
				+ " }";
		CtMethod getMethod = CtMethod.make(getterCode, ctClass);
		ctClass.addMethod(getMethod);

		try {
			ctClass.getDeclaredMethod(setter);
			throw new CannotCompileException("Getter "+setter+" already exists");
		} catch (NotFoundException noSetter) {
			// Fine, it should not exist
		}
					
		String setterCode = "public void " 
				+ setter
				+ "(" + ctField.getType().getName() + " value) "
				+ "{ " 
				+ "this." + ctField.getName() + " = value; " 
				+ " ((ch.unibe.iam.scg.ContextClassLoader) this.getClass().getClassLoader()).synchronizeWrite( this, "+ index +" ); "
				+ " }";
		CtMethod setMethod = CtMethod.make( setterCode, ctClass );
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

package ch.unibe.iam.scg.rewriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ch.unibe.iam.scg.Mapper;
import ch.unibe.iam.scg.rewriter.helper.ReflectionHelper;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.CtField.Initializer;

public class AddContextAwarenessRewriter implements ClassRewriter {

	
	private List<CtClass> allSuperinterfaces( CtClass ctClass ) throws NotFoundException{
		CtClass currentClass = ctClass.getSuperclass();
		List<CtClass> intfs = new ArrayList<CtClass>();
		while( currentClass != null ) {
			intfs.addAll( Arrays.asList( currentClass.getInterfaces()  )); 
			currentClass = currentClass.getSuperclass();
		}
		return intfs;
	}
	
	public void rewrite(CtClass ctClass) throws CannotCompileException {
		
		if( ctClass.isInterface() ) return;
		
		//System.out.println( "-> Write awareness for "+ ctClass.getName() );
		
		try {
			
			
			// Make the class public
			ctClass.setModifiers( Modifier.setPublic(ctClass.getModifiers()));
			
			// Add interface if necessary, and appropriate constructor
			CtClass superclass = ctClass.getSuperclass();
			//@TODO checking based on name is a bit hackish
			if( ! Mapper.needsRewrite(superclass.getName())) {	
				
				generateContextGlueCode( ctClass );			
				
				// Make sure the constructor exists for all instrumented classes
				String constructor = "public  "+ctClass.getSimpleName()+"(ch.unibe.iam.scg.ContextInfo info) { " +
				"this." + ClassRewriter.CONTEXT_INFO + " = info;" +
				"}";
				CtConstructor constructorMethod = CtNewConstructor.make(constructor, ctClass);
				constructorMethod.setModifiers( Modifier.setPublic( constructorMethod.getModifiers()));
				ctClass.addConstructor(constructorMethod);
			}
			
			for( CtField ctField : ctClass.getDeclaredFields() ) {
				//Make field public
				ctField.setModifiers( Modifier.setPublic( ctField.getModifiers()));
			}
			
			
		} catch (Exception e) {
			throw new CannotCompileException( "Cound not contexutalize class", e);
		}
		
		// System.out.println( "<- Write awareness for "+ ctClass.getName() );
	}


	public void generateContextGlueCode( CtClass ctClass) throws Exception
	{
		CtClass contextAwareInterface = ClassPool.getDefault().get("ch.unibe.iam.scg.ContextAware");
		ctClass.addInterface( contextAwareInterface );
		
		CtField contextInfoField = CtField.make( "public ch.unibe.iam.scg.ContextInfo " + ClassRewriter.CONTEXT_INFO+ ";", ctClass );
		Initializer intializer = Initializer.byExpr("new ch.unibe.iam.scg.ContextInfo();");
		ctClass.addField(contextInfoField, intializer);
		
		String code = "public ch.unibe.iam.scg.ContextInfo getContextInfo() { " +
				"return this." + ClassRewriter.CONTEXT_INFO+ "; " +
				"}";
		CtMethod getMethod = CtMethod.make(code, ctClass);
		ctClass.addMethod(getMethod);
		
		String mig = "public ch.unibe.iam.scg.ContextAware migrateToNext( ch.unibe.iam.scg.ContextClassLoader nextLoader) { " +
				//"System.out.println(\"Migrate intance of "+ctClass.getName()+ "\");" +
				"ch.unibe.iam.scg.ContextAware clone = "+ReflectionHelper.class.getName()+".buildNewInstance( this, nextLoader );" +
				"clone.getContextInfo().prev = this; " +
				"this.getContextInfo().next = clone;" +
				"clone.getContextInfo().next = null; " +
				"this.getContextInfo().prev = null;" +
				"clone.getContextInfo().global = true;" +
				"clone.getContextInfo().dirty = 0xFFFFFFFF;" +
				"this.getContextInfo().global = true;" +
				"this.getContextInfo().dirty = 0x0000;" +
				"return clone; }";
		CtMethod migMethod = CtMethod.make(mig, ctClass);
		ctClass.addMethod(migMethod);
		
		String globalize = "public void globalize() { " +
				"this." + ClassRewriter.CONTEXT_INFO + ".global = true;" + 
				"}";
		CtMethod globalizeMethod = CtMethod.make(globalize, ctClass);
		ctClass.addMethod(globalizeMethod);
	}
	
}

package ch.unibe.iam.scg.rewriter;

import java.rmi.UnexpectedException;
import java.util.Arrays;
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
		
		CtClass contextAwareInterface;
		try {
			contextAwareInterface = ClassPool.getDefault().get("ch.unibe.iam.scg.ContextAware");
			List<CtClass> intfs = Arrays.asList( ctClass.getSuperclass().getInterfaces() ); 
			if( ! intfs.contains(contextAwareInterface)) {
				
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
						"System.out.println(\"Migreate intance of "+ctClass.getName()+ "\");" +
						"ch.unibe.iam.scg.ContextAware clone = "+ReflectionHelper.class.getName()+".buildNewInstance( this, nextLoader );" +
						"clone.getContextInfo().prev = this; " +
						"this.getContextInfo().next = clone;" +
						"clone.getContextInfo().next = null; " +
						"this.getContextInfo().prev = null;" +
						"clone.getContextInfo().global = true;" +
						"clone.getContextInfo().dirty = true;" +
						"this.getContextInfo().global = true;" +
						"this.getContextInfo().dirty = false;" +
						"return clone; }";
				CtMethod migMethod = CtMethod.make(mig, ctClass);
				ctClass.addMethod(migMethod);
				
				String globalize = "public void globalize() { " +
						"this." + ClassRewriter.CONTEXT_INFO + ".global = true;" + 
						"}";
				CtMethod globalizeMethod = CtMethod.make(globalize, ctClass);
				ctClass.addMethod(globalizeMethod);
		
			}
		} catch (NotFoundException e) {
			throw new RuntimeException( "Cound not contexutalize class", e);
		}
		
		
		// Make sure the constructor exists for all instrumented classes
		String constructor = "public  "+ctClass.getSimpleName()+"(ch.unibe.iam.scg.ContextInfo info) { " +
		"this." + ClassRewriter.CONTEXT_INFO + " = info;" +
		"}";
		CtConstructor constructorMethod = CtNewConstructor.make(constructor, ctClass);
		ctClass.addConstructor(constructorMethod);

		// Generate getter/setter
		for (CtField ctField : ctClass.getDeclaredFields()) {
			try {
				
				boolean isStatic = (ctField.getModifiers() &  AccessFlag.STATIC) == AccessFlag.STATIC;
				
				if( ! isStatic ) {
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
					
					String getterCode = "public " + ctField.getType().getName() + " "
							+ getter
							+ "() { " 
							+ " ((ch.unibe.iam.scg.ContextClassLoader) this.getClass().getClassLoader()).synchronizeRead( this, \""+ ctField.getName() +"\" ); "
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
							+ " ((ch.unibe.iam.scg.ContextClassLoader) this.getClass().getClassLoader()).synchronizeWrite( this, \""+ ctField.getName() +"\" ); "
							+ " }";
					CtMethod setMethod = CtMethod.make( setterCode, ctClass );
					ctClass.addMethod(setMethod);
					
					//Make field public
					ctField.setModifiers( Modifier.setPublic( ctField.getModifiers()));
				}
				else
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
					ctClass.addMethod(setMethod);
					
					//Make field public
					ctField.setModifiers( Modifier.setPublic( ctField.getModifiers()));
				}
				
			} catch (Exception e) {
				throw new CannotCompileException("Error in PropertiesEnhancer", e);
			}
		}

		System.out.println( "<- Wrote accessors for "+ ctClass.getName() );
	}

}

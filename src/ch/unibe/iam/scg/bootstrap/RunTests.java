package ch.unibe.iam.scg.bootstrap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.rmi.UnexpectedException;

import ch.unibe.iam.scg.ContextAware;
import ch.unibe.iam.scg.ContextClassLoader;
import ch.unibe.iam.scg.rewriter.GenerateAccessorsRewriter;
import ch.unibe.iam.scg.rewriter.InterceptAccessorsRewriter;
import ch.unibe.iam.scg.rewriter.RenameDependenciesRewriter;
import ch.unibe.iam.scg.test.DynamicScope;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.NewExpr;
import javassist.tools.web.Webserver;

public class RunTests {

	ClassPool cp = ClassPool.getDefault();
	
	public static void main(String[] args) throws Exception {
		new RunTests().test();
	}
	
	public void test() throws Exception
	{
//		testSubclass();
//		testStatic();
//		testIsolation();
//		testCycle();
		testArray();
		testMigNext();
		testMigArray();
//		testDynamicScope();
	}
	
	public void testSubclass() throws Exception
	{
		ClassLoader loader = new ContextClassLoader("$$1");
		Class clazz = loader.loadClass("ch.unibe.iam.scg.test.SubNode$$1");
		Object node = clazz.newInstance();
		invoke( node,  "deepen" );
		System.out.println( node.toString() );
		
		Class clazz2 = loader.loadClass("ch.unibe.iam.scg.test.AnotherSubNode$$1");
		Object node2 = clazz2.newInstance();
		System.out.println( node2.toString() );
	
	}
	
	public void testStatic() throws Exception
	{
		ClassLoader loader = new ContextClassLoader("$$1");
		Class clazz = loader.loadClass("ch.unibe.iam.scg.test.StaticSubNode$$1");
		Object node = clazz.newInstance();
		invoke( node,  "deepen" );
		System.out.println( node.toString() );
	}
	
	public void testIsolation() throws Exception
	{
		
		 Class clazz = new ContextClassLoader("$$1").loadClass("ch.unibe.iam.scg.test.SubNode$$1");
		 Object node = clazz.newInstance();
		 invoke( node,  "deepen" );
		 System.out.println( node.toString() );
		
		 Class clazz2 = new ContextClassLoader("$$2").loadClass("ch.unibe.iam.scg.test.SubNode$$2");
		 Object node2 = clazz2.newInstance();
		 invoke( node2,  "deepen" );
		 System.out.println( node2.toString() );
	}
	
	public void testCycle() throws Exception
	{
		Class builderClazz = new ContextClassLoader("Xx3").loadClass("ch.unibe.iam.scg.test.CycleBuilderXx3");
		Object builder = builderClazz.newInstance();
		Object cycle = invoke( builder, "buildLeftCycle");
		System.out.println( cycle.toString() );
	}
    
	public void testArray() throws Exception
	{
		ClassLoader loader = new ContextClassLoader("$$1");
		Class clazz = loader.loadClass("ch.unibe.iam.scg.test.Holder$$1");
		Object node = clazz.newInstance();
		Object res = invoke( node,  "sum" );
		System.out.println( res );
	}
	
	public void testMigNext() throws Exception
	{
		ContextClassLoader loaderPrev = new ContextClassLoader("$$1");
		ContextClassLoader loaderNext = new ContextClassLoader("$$2");
		
		Class clazz = loaderPrev.loadClass("ch.unibe.iam.scg.test.SubNode$$1");
		ContextAware nodePrev = (ContextAware) clazz.newInstance();
		ContextAware nodeNext = nodePrev.migrateToNext( loaderNext );
		 System.out.println( nodePrev == nodeNext );
		 System.out.println( nodePrev.toString() );
		 System.out.println( nodeNext.toString() );
		 invoke(nodePrev, "deepen");
		 System.out.println( nodePrev.toString() );
		 System.out.println( nodeNext.toString() );
		 
		 
	}
	
	public void testMigArray() throws Exception
	{
		ContextClassLoader loaderPrev = new ContextClassLoader("$$1");
		ContextClassLoader loaderNext = new ContextClassLoader("$$2");
		
		Class clazz = loaderPrev.loadClass("ch.unibe.iam.scg.test.Holder$$1");
		ContextAware nodePrev = (ContextAware) clazz.newInstance();
		ContextAware nodeNext = nodePrev.migrateToNext( loaderNext );
		 System.out.println( nodePrev == nodeNext );
		 System.out.println( invoke( nodePrev, "sum" ) );
		 System.out.println( invoke( nodeNext, "sum" ) );
	}
	
	public void testFrameworkClasses() throws Exception
	{
		
	}
	
	
	
	public void testDynamicScope() throws Exception
	{
		ContextClassLoader loader = new ContextClassLoader("$$1");
		Class clazz = loader.loadClass("ch.unibe.iam.scg.test.DynamicScope$$1");
		invoke( clazz.newInstance(), "test" );
	}
	
	private Object invoke( Object receiver, String method ) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException
	{
		Class clazz = receiver.getClass();
		Method meth = clazz.getMethod( method, new Class[0] );
		return meth.invoke( receiver, new Object[0] );
	}
}

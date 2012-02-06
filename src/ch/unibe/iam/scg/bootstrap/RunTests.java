package ch.unibe.iam.scg.bootstrap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.rmi.UnexpectedException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;

import ch.unibe.iam.scg.ContextAware;
import ch.unibe.iam.scg.ContextClassLoader;
import ch.unibe.iam.scg.rewriter.GenerateAccessorsRewriter;
import ch.unibe.iam.scg.rewriter.InterceptAccessorsRewriter;
import ch.unibe.iam.scg.rewriter.MapDependenciesRewriter;
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
import junit.framework.TestCase;

public class RunTests extends TestCase {

	ClassPool cp = ClassPool.getDefault();
	
	public void testSuite() throws Exception
	{
		testSubclass();
		testStatic();
		testIsolation();
		testCycle();
		
		testArray();
		testMigNext();
		testMigArray();

		testDynamicScope();
		testFrameworkClasses();
		testArrayList();
		testClassLiteral();
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
	
	public void testConstructorSubclass() throws Exception
	{
		ClassLoader loader = new ContextClassLoader("$$1");
		
		Class clazz = loader.loadClass("ch.unibe.iam.scg.test.SubWriteConstructor$$1");
		Object node = clazz.newInstance();
		System.out.println( node.toString() );
		
		Class clazz2 = loader.loadClass("ch.unibe.iam.scg.test.SubSubWriteConstructor$$1");
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
		ContextClassLoader loaderPrev = new ContextClassLoader("XX1");
		loaderPrev.doDelegation = false;
		Class clazz = loaderPrev.loadClass("ch.unibe.iam.scg.test.core.java.util.HashMapXX1");
		Object map = clazz.newInstance();
		System.out.println( "Yeah:"+ map.getClass().toString());
		invoke2( map, "put", Object.class, Object.class, 1, 42 );
		Object value = invoke1( map, "get" , Object.class,  1 );
		assert( value == Integer.valueOf(42) );
	}
	
	
	public void testArrayList() throws Exception
	{
	//	ArrayList<Object> l;
	//	l = new ArrayList<Object>();
		
		ContextClassLoader loaderPrev = new ContextClassLoader("XX1");
		loaderPrev.doDelegation = false;
		Class clazz = loaderPrev.loadClass("ch.unibe.iam.scg.test.core.java.util.ArrayListXX1");
		Object map = clazz.newInstance();
		System.out.println( "Yeah:"+ map.getClass().toString());
		invoke1( map, "add", Object.class, 42 );
		Object value = invoke1( map, "get" , int.class,  0 );
		assert( value == Integer.valueOf(42) );
	}
	
	public void testClassLiteral() throws Exception
	{
		ContextClassLoader loaderPrev = new ContextClassLoader("XX1");
		Class clazz = loaderPrev.loadClass("ch.unibe.iam.scg.test.ClassLiteralXX1");
		Object obj = clazz.newInstance();
		Object selfType = invoke( obj, "newSelfType");
		System.out.println( selfType.getClass().toString() );
	}
	
	public void testBenchmark() throws Exception
	{
		long t1, t2;
		Object map;
		HashMap m;
		Method put ;
		Method get ;
		
		ContextClassLoader loaderPrev = new ContextClassLoader("XX1");
		loaderPrev.doDelegation = false;
		Class clazz = loaderPrev.loadClass("ch.unibe.iam.scg.test.core.java.util.HashMapXX1");
		
		System.out.println( "----" );
		
		for( int k=0; k<3; k++) {
			map = clazz.newInstance();
			put = clazz.getMethod("put", Object.class, Object.class );
			get = clazz.getMethod("get", Object.class );
			t1 = System.currentTimeMillis();
			for( int i=0; i < 100000; i++ ) {
				put.invoke(map, 1, 42 );
				Object value = get.invoke(map, 1);
				//invoke2( map, "put", Object.class, Object.class, 1, 42 );
				//Object value = invoke1( map, "get" , Object.class,  1 );
				assert( value == Integer.valueOf(42) );
			}
			t1 = System.currentTimeMillis() - t1;
			
			map = new HashMap();
			put = HashMap.class.getMethod("put", Object.class, Object.class );
			get = HashMap.class.getMethod("get", Object.class );
			t2 = System.currentTimeMillis();
			for( int i=0; i < 100000; i++ ) {
				put.invoke(map, 1, 42 );
				Object value = get.invoke(map, 1);
				//invoke2( map, "put", Object.class, Object.class, 1, 42 );
				//Object value = invoke1( map, "get" , Object.class,  1 );
				assert( value == Integer.valueOf(42) );
			}
			t2 = System.currentTimeMillis() - t2;

			System.out.println( "Instrumented: "+t1+", normal: "+t2);
		}
	}
	
	public void testInterface() throws Exception
	{
		ContextClassLoader loader = new ContextClassLoader("$$1");
		Class clazz = loader.loadClass("ch.unibe.iam.scg.test.NodeService$$1");
		Object node = invoke( clazz.newInstance(), "serve" );
		assert( node.toString().matches( "\\[.*\\]") );
	}
	
	public void testDynamicScope() throws Exception
	{
		ContextClassLoader loader = new ContextClassLoader("$$1");
		ContextClassLoader loaderNext = new ContextClassLoader("$$2");
		
		Class clazz = loader.loadClass("ch.unibe.iam.scg.test.DynamicScope$$1");
		ContextAware closure = (ContextAware) clazz.newInstance();
		invoke( closure, "test" );
		ContextAware nodeNext = closure.migrateToNext( loaderNext );
		invoke( nodeNext, "test" );
	}
	
	private Object invoke( Object receiver, String method ) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException
	{
		Class clazz = receiver.getClass();
		Method meth = clazz.getMethod( method, new Class[0] );
		return meth.invoke( receiver, new Object[0] );
	}
	
	private Object invoke1( Object receiver, String method, Class c1, Object p1 ) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException
	{
		Class clazz = receiver.getClass();
		Method meth = clazz.getMethod( method, new Class[ ] { c1 } );
		return meth.invoke( receiver, new Object[]{ p1 } );
	}
	private Object invoke2( Object receiver, String method, Class c1, Class c2, Object p1, Object p2 ) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException
	{
		Class clazz = receiver.getClass();
		Method meth = clazz.getMethod( method, new Class[ ] { c1, c2 } );
		return meth.invoke( receiver, new Object[]{ p1, p2 } );
	}
}

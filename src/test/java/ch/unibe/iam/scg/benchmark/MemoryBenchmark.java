package ch.unibe.iam.scg.benchmark;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import junit.framework.TestCase;

import ch.unibe.iam.scg.ContextAware;
import ch.unibe.iam.scg.ContextClassLoader;
import ch.unibe.iam.scg.ContextInfo;
import ch.unibe.iam.scg.rewriter.helper.ArrayInterceptor;
import ch.unibe.iam.scg.test.Data;

public class MemoryBenchmark extends TestCase {
	public void testSync() throws Exception {

		ContextClassLoader loader = new ContextClassLoader("$$1");
		ContextClassLoader loaderNext = new ContextClassLoader("$$2");
		
		Class clazz = loader.loadClass("ch.unibe.iam.scg.test.Data$$1");
		ContextAware d = (ContextAware) clazz.newInstance();
		invoke0(d, "build");
		ContextAware d2 = d.migrateToNext(loaderNext);
		
		long t1 = System.currentTimeMillis();
		invoke1(d2, "syncFrom", Object.class, d);
		long t2 = System.currentTimeMillis() - t1;
		System.out.println( "Took: "+t2);
	}
	
	private Object invoke0( Object receiver, String method ) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException
	{
		Class clazz = receiver.getClass();
		Method meth = clazz.getMethod( method, new Class[ ] {  } );
		return meth.invoke( receiver, new Object[]{  } );
	}
	
	private Object invoke1( Object receiver, String method, Class c1, Object p1 ) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException
	{
		Class clazz = receiver.getClass();
		Method meth = clazz.getMethod( method, new Class[ ] { c1 } );
		return meth.invoke( receiver, new Object[]{ p1 } );
	}
	
	public void testBenchmark() throws Exception
	{
		long t1,t12, t2;
		Object map;
		HashMap m;
		Method put ;
		Method get ;
		
		ContextClassLoader loaderPrev = new ContextClassLoader("XX1");
		loaderPrev.doDelegation = false;
		Class clazz = loaderPrev.loadClass("ch.unibe.iam.scg.test.core.java.util.HashMapXX1");
		
		System.out.println( "----" );
		
		for( int k=0; k<5; k++) {
			map = clazz.newInstance();
			put = clazz.getMethod("put", Object.class, Object.class );
			get = clazz.getMethod("get", Object.class );
			t1 = System.currentTimeMillis();
			for( int i=0; i < 100000; i++ ) {
				put.invoke(map, i, 42 );
				Object value = get.invoke(map, i);
				//invoke2( map, "put", Object.class, Object.class, 1, 42 );
				//Object value = invoke1( map, "get" , Object.class,  1 );
				assert( value == Integer.valueOf(42) );
			}
			t1 = System.currentTimeMillis() - t1;
			
			t12 = System.currentTimeMillis();
			for( int i=0; i < 100000; i++ ) {
				Object value = get.invoke(map, i);
				//invoke2( map, "put", Object.class, Object.class, 1, 42 );
				//Object value = invoke1( map, "get" , Object.class,  1 );
				assert( value == Integer.valueOf(42) );
			}
			t12 = System.currentTimeMillis() - t12;
			
			map = new HashMap();
			put = HashMap.class.getMethod("put", Object.class, Object.class );
			get = HashMap.class.getMethod("get", Object.class );
			t2 = System.currentTimeMillis();
			for( int i=0; i < 100000; i++ ) {
				put.invoke(map, i, 42 );
				Object value = get.invoke(map, i);
				//invoke2( map, "put", Object.class, Object.class, 1, 42 );
				//Object value = invoke1( map, "get" , Object.class,  1 );
				assert( value == Integer.valueOf(42) );
			}
			t2 = System.currentTimeMillis() - t2;

			ConcurrentHashMap<Object, ContextInfo> a= ArrayInterceptor.arrayContextInfo;
			int s = a.size();
			System.out.println( "Instrumented: "+t1+", read: "+t12+" normal: "+t2);
			
			

		}
	}

	public void testBenchmark2() throws Exception
	{
		long t1, t2;
		Object list;
		Method add ;
		Method size ;
		
		ContextClassLoader loaderPrev = new ContextClassLoader("XX1");
		loaderPrev.doDelegation = false;
		Class clazz = loaderPrev.loadClass("ch.unibe.iam.scg.test.core.java.util.ArrayListXX1");
		
		System.out.println( "----" );
		
		for( int k=0; k<8; k++) {
			list = clazz.newInstance();
			add = clazz.getMethod("add", Object.class );
			size = clazz.getMethod("size" );
			t1 = System.currentTimeMillis();
			for( int i=0; i < 100000; i++ ) {
				add.invoke(list, 42 );
				Object value = size.invoke(list);
				//invoke2( map, "put", Object.class, Object.class, 1, 42 );
				//Object value = invoke1( map, "get" , Object.class,  1 );
				//assert( value == Integer.valueOf(42) );
			}
			t1 = System.currentTimeMillis() - t1;
			
			list = new ArrayList();
			add = ArrayList.class.getMethod("add", Object.class );
			size = ArrayList.class.getMethod("size" );
			t2 = System.currentTimeMillis();
			for( int i=0; i < 100000; i++ ) {
				add.invoke(list, 42 );
				Object value = size.invoke(list);
				//invoke2( map, "put", Object.class, Object.class, 1, 42 );
				//Object value = invoke1( map, "get" , Object.class,  1 );
				//assert( value == Integer.valueOf(42) );
			}
			t2 = System.currentTimeMillis() - t2;
	
			ConcurrentHashMap<Object, ContextInfo> a= ArrayInterceptor.arrayContextInfo;
			int s = a.size();
			System.out.println( "Instrumented: "+t1+", normal: "+t2);
		}
	}

	public void testBenchmark3() throws Exception
	{
		long t1, t2;
		Object list;
		Method add ;
		Method size ;
		
		ContextClassLoader loaderPrev = new ContextClassLoader("XX1");
		loaderPrev.doDelegation = false;
		Class clazz = loaderPrev.loadClass("ch.unibe.iam.scg.test.core.java.util.LinkedListXX1");
		
		System.out.println( "----" );
		
		for( int k=0; k<6; k++) {
			list = clazz.newInstance();
			add = clazz.getMethod("add", Object.class );
			size = clazz.getMethod("size" );
			t1 = System.currentTimeMillis();
			for( int i=0; i < 100000; i++ ) {
				add.invoke(list, 42 );
				Object value = size.invoke(list);
				//invoke2( map, "put", Object.class, Object.class, 1, 42 );
				//Object value = invoke1( map, "get" , Object.class,  1 );
				//assert( value == Integer.valueOf(42) );
			}
			t1 = System.currentTimeMillis() - t1;
			
			list = new LinkedList();
			add = LinkedList.class.getMethod("add", Object.class );
			size = LinkedList.class.getMethod("size" );
			t2 = System.currentTimeMillis();
			for( int i=0; i < 100000; i++ ) {
				add.invoke(list, 42 );
				Object value = size.invoke(list);
				//invoke2( map, "put", Object.class, Object.class, 1, 42 );
				//Object value = invoke1( map, "get" , Object.class,  1 );
				//assert( value == Integer.valueOf(42) );
			}
			t2 = System.currentTimeMillis() - t2;
	
			ConcurrentHashMap<Object, ContextInfo> a= ArrayInterceptor.arrayContextInfo;
			int s = a.size();
			System.out.println( "Instrumented: "+t1+", normal: "+t2);
		}
	}
}

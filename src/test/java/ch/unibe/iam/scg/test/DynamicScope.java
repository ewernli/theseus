package ch.unibe.iam.scg.test;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


import ch.unibe.iam.scg.ContextAware;
import ch.unibe.iam.scg.ContextClassLoader;
import ch.unibe.iam.scg.ContextInfo;

public class DynamicScope {
	
	private Dynamic dyn = new Dynamic();
	
	class Dynamic implements Runnable
	{
		public void run() {
			System.out.println( "Dynamic "+ DynamicScope.this.getClass().getName());
		}
	}

	
	public void test() throws Exception
	{
		final ContextClassLoader loader = (ContextClassLoader) this.getClass().getClassLoader();
		dyn.run();		
		new Runnable() {
			
			public void run() {
				// TODO Auto-generated method stub
				
			}
		}.run();
		new Runnable() {
			
			public void run() {
				
				new Runnable() {
					public void run() {}					
				}.run();			
			}
		}.run();
	}
}

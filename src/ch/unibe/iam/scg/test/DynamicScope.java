package ch.unibe.iam.scg.test;

import ch.unibe.iam.scg.ContextAware;
import ch.unibe.iam.scg.ContextClassLoader;
import ch.unibe.iam.scg.ContextInfo;
import ch.unibe.iam.scg.ContextualRunnable;

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
	}
}

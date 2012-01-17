package ch.unibe.iam.scg.test;

import ch.unibe.iam.scg.ContextAware;
import ch.unibe.iam.scg.ContextClassLoader;
import ch.unibe.iam.scg.ContextInfo;
import ch.unibe.iam.scg.ContextualRunnable;

public class DynamicScope {
	public void test() throws Exception
	{
		final ContextClassLoader loader = (ContextClassLoader) this.getClass().getClassLoader();
		loader.perform(new Runnable() {
			
			public void run() {
				System.out.println( "Dynamic "+ loader.getClass().getName());
			}
		});
	}
}

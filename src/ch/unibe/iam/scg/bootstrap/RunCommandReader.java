package ch.unibe.iam.scg.bootstrap;

import ch.unibe.iam.scg.Context;

public class RunCommandReader {
	public static void main( String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException 
	{
		Context loader = new Context("$$1");
		Class clazz = loader.loadClass("ch.unibe.iam.scg.test.CommandReader$$1");
		Runnable d = (Runnable) clazz.newInstance();
		d.run();
	}
}

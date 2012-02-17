package ch.unibe.iam.scg;

import java.lang.reflect.InvocationTargetException;

public class ContextHandle {
	
	Context target;
	
	public ContextHandle( Context ctx )
	{
		target = ctx;
	}

	@Override
	protected void finalize() throws Throwable {
		System.out.println("Handle finalized");
		target.disposeHandle();
	}
}

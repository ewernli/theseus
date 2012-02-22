package ch.unibe.iam.scg;

import java.lang.reflect.InvocationTargetException;

public class ContextHandle {
	
	Context target;
	
	public ContextHandle( Context ctx )
	{
		target = ctx;
		System.out.println("Handle for context "+target.suffix());
	}

	public void release()
	{
		System.out.println("Release handle for context "+target.suffix());
		target.disposeHandle();
		target = null;
	}
	
	@Override
	protected void finalize() throws Throwable {
		if( target != null ) {
			System.out.println("Handle finalized for context "+target.suffix());
			target.disposeHandle();
		}
	}
}

package ch.unibe.iam.scg.jetty;

import java.lang.reflect.Field;

import ch.unibe.iam.scg.ContextAware;
import ch.unibe.iam.scg.ContextClassLoader;
import ch.unibe.iam.scg.ContextInfo;
import ch.unibe.iam.scg.rewriter.ClassRewriter;
import ch.unibe.iam.scg.rewriter.helper.ArrayInterceptor;

public class Context614to615 extends ContextClassLoader {

	public Context614to615(String suffix) {
		super(suffix);
	}

	@Override
	public void synchronizeFromSucc(Object obj, ContextInfo info,
			int selectedFieldPosition) throws Exception {
		
		if( obj.getClass().getName().contains("org.mortbay.jetty.Server"))
		{
			this._synchronizeFromSucc(obj, info, selectedFieldPosition);
		}
		else
		{
			super.synchronizeFromSucc(obj, info, selectedFieldPosition);
		}
	}

	@Override
	public void synchronizeFromPrev(Object obj, ContextInfo info,
			int selectedFieldPosition) throws Exception {
		if( obj.getClass().getName().contains("org.mortbay.jetty.Server"))
		{
			this._synchronizeFromPrev(obj, info, selectedFieldPosition);
		}
		else
		{
			super.synchronizeFromPrev(obj, info, selectedFieldPosition);
		}
	}
	
	
	public void _synchronizeFromSucc(Object obj, ContextInfo info, int selectedFieldPosition) throws Exception {
		// synchronize with reflection
		// System.out.println("Synchronize from ancestor");
		
		Object next = info.next;
		Field[] nextFields = orderedFields( nonFinalfieldsOfClassesOnly( next.getClass() ) );
		if( selectedFieldPosition > nextFields.length) {
			throw new IndexOutOfBoundsException();
		}
		
		Field nextField = null; 
		if( selectedFieldPosition < 14 )
			nextField = nextFields[ selectedFieldPosition];
		else
			nextField = nextFields[ selectedFieldPosition+1];
		
		if( nextField.getName().equals( ClassRewriter.CONTEXT_INFO )) 
		{ 
			return ;
		}
		
		Field prevField = orderedFields( nonFinalfieldsOfClassesOnly( obj.getClass() ) )[ selectedFieldPosition ];
		Object nextValue = nextField.get(next);
		
		//System.out.println("Prev type:"+ ( prevValue==null?"null":prevValue.getClass().toString()));
		
		Object prevValue = this.migrateToPrevIfNecessary(nextValue);
		try
		{
			prevField.set(obj, prevValue);
		}
		catch( Exception e)
		{
			throw new RuntimeException("Ooops",e);
		}
		
	}
	
	
	public void _synchronizeFromPrev(Object obj, ContextInfo info, int selectedFieldPosition) throws Exception {
		// synchronize with reflection
		// System.out.println("Synchronize from ancestor");
		
		Object prev = info.prev;
		Field[] prevFields = orderedFields( nonFinalfieldsOfClassesOnly( prev.getClass() ) );
		if( selectedFieldPosition > prevFields.length) {
			throw new IndexOutOfBoundsException();
		}
		
		Field prevField = null; 
		if( selectedFieldPosition < 14 )
			prevField = prevFields[ selectedFieldPosition];
		else if (selectedFieldPosition == 14 )
			return;
		else
			prevField = prevFields[ selectedFieldPosition-1];
		
		if( prevField.getName().equals( ClassRewriter.CONTEXT_INFO )) 
		{ 
			return ;
		}
		
		Field nextField = orderedFields( nonFinalfieldsOfClassesOnly( obj.getClass() ) )[ selectedFieldPosition ];
		Object prevValue = prevField.get(prev);
		
		//System.out.println("Prev type:"+ ( prevValue==null?"null":prevValue.getClass().toString()));

		Object nextValue = this.migrateToNextIfNecessary(prevValue);
		try{
			nextField.set(obj, nextValue);
		}
		catch( Exception e)
		{
			throw new RuntimeException("Ooops",e);
		}
	}
}

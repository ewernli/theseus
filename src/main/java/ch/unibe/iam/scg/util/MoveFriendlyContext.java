package ch.unibe.iam.scg.util;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import ch.unibe.iam.scg.Context;
import ch.unibe.iam.scg.ContextInfo;
import ch.unibe.iam.scg.rewriter.ClassRewriter;
import ch.unibe.iam.scg.rewriter.helper.ArrayInterceptor;

public class MoveFriendlyContext extends Context {

	public MoveFriendlyContext(String suffix) {
		super(suffix);
	}

	public void synchronizeFromSucc(Object obj, ContextInfo info,
			int selectedFieldPosition ) throws Exception {
		// synchronize with reflection
		// System.out.println("Synchronize from ancestor");

		Field prevField = orderedFields(nonFinalfieldsOfClassesOnly(obj
				.getClass()))[selectedFieldPosition];
		
		Object next = info.next;
		Field nextField = null;
		try
		{
			 nextField = next.getClass().getField(prevField.getName());
		}
		catch( java.lang.NoSuchFieldException e )
		{
			return ; // not equivalence
		}
			
		if (nextField.getName().equals(ClassRewriter.CONTEXT_INFO)) {
			return;
		}

		Object nextValue = nextField.get(next);

		// System.out.println("Prev type:"+ (
		// prevValue==null?"null":prevValue.getClass().toString()));

		Object prevValue = this.migrateToPrevIfNecessary(nextValue);
		try {
			prevField.set(obj, prevValue);
		} catch (Exception e) {
			throw new RuntimeException("Ooops", e);
		}

	}

	public void synchronizeFromPrev(Object obj, ContextInfo info,
			int selectedFieldPosition) throws Exception {
		// synchronize with reflection
		// System.out.println("Synchronize from ancestor");

		Field nextField = orderedFields(nonFinalfieldsOfClassesOnly(obj
				.getClass()))[selectedFieldPosition];
		
		Object prev = info.prev;
		Field prevField = null;
		try{
			 prevField = prev.getClass().getField( nextField.getName() );
		}
		catch( java.lang.NoSuchFieldException e )
		{
			return ; // not equivalence
		}
		
		if (prevField.getName().equals(ClassRewriter.CONTEXT_INFO)) {
			return;
		}
		
		Object prevValue = prevField.get(prev);

		// System.out.println("Prev type:"+ (
		// prevValue==null?"null":prevValue.getClass().toString()));

		Object nextValue = this.migrateToNextIfNecessary(prevValue);
		try {
			nextField.set(obj, nextValue);
		} catch (Exception e) {
			throw new RuntimeException("Ooops", e);
		}
	}
}

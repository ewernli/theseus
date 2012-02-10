package ch.unibe.iam.scg.jetty;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import ch.unibe.iam.scg.ContextAware;
import ch.unibe.iam.scg.ContextClassLoader;
import ch.unibe.iam.scg.ContextInfo;
import ch.unibe.iam.scg.rewriter.ClassRewriter;
import ch.unibe.iam.scg.rewriter.helper.ArrayInterceptor;

public class Context614to615 extends ContextClassLoader {

	public interface Mapper {
		public int mapOldField(int oldPos);

		public int mapNewField(int newPos);
	}

	String[] classes = new String[] {
			"org.mortbay.jetty.Server",
			"org.mortbay.jetty.servlet.ServletHandler",
			"org.mortbay.jetty.servlet.DefaultServlet"
	};
	
	public Context614to615(String suffix) {
		super(suffix);
		map = new HashMap();
		map.put("org.mortbay.jetty.Server$$1", new ServerMapper());
		map.put("org.mortbay.jetty.Server$$2", new ServerMapper());
		map.put("org.mortbay.jetty.servlet.ServletHandler$$1", new HandlerMapper());
		map.put("org.mortbay.jetty.servlet.ServletHandler$$2", new HandlerMapper());
		
	}

	private Map<String,Mapper> map;
	
	private boolean hasChanged( String className ) {
		for( String s : classes )
		{
			if( className.startsWith(s)) return true;
		}
		return false;
	}
	
	@Override
	public void synchronizeFromSucc(Object obj, ContextInfo info,
			int selectedFieldPosition) throws Exception {

		if ( hasChanged(obj.getClass().getName())) {
			this.__synchronizeFromSucc(obj, info, selectedFieldPosition);
		} else {
			super.synchronizeFromSucc(obj, info, selectedFieldPosition);
		}
	}

	@Override
	public void synchronizeFromPrev(Object obj, ContextInfo info,
			int selectedFieldPosition) throws Exception {
		if ( hasChanged(obj.getClass().getName())) {
			this.__synchronizeFromPrev(obj, info, selectedFieldPosition);
		} else {
			super.synchronizeFromPrev(obj, info, selectedFieldPosition);
		}
	}

	public void _synchronizeFromSucc(Object obj, ContextInfo info,
			int selectedFieldPosition, Mapper mapper) throws Exception {
		// synchronize with reflection
		// System.out.println("Synchronize from ancestor");

		Object next = info.next;
		Field[] nextFields = orderedFields(nonFinalfieldsOfClassesOnly(next
				.getClass()));
		if (selectedFieldPosition > nextFields.length) {
			throw new IndexOutOfBoundsException();
		}

		Field nextField = nextFields[mapper.mapOldField(selectedFieldPosition) ];
		
		if (nextField.getName().equals(ClassRewriter.CONTEXT_INFO)) {
			return;
		}

		Field prevField = orderedFields(nonFinalfieldsOfClassesOnly(obj
				.getClass()))[selectedFieldPosition];
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

	public void __synchronizeFromSucc(Object obj, ContextInfo info,
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

	public void __synchronizeFromPrev(Object obj, ContextInfo info,
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
	
	public void _synchronizeFromPrev(Object obj, ContextInfo info,
			int selectedFieldPosition, Mapper mapper) throws Exception {
		// synchronize with reflection
		// System.out.println("Synchronize from ancestor");

		Object prev = info.prev;
		Field[] prevFields = orderedFields(nonFinalfieldsOfClassesOnly(prev
				.getClass()));
		if (selectedFieldPosition > prevFields.length) {
			throw new IndexOutOfBoundsException();
		}

		Field prevField = prevFields[mapper.mapNewField(selectedFieldPosition) ];

		if (prevField.getName().equals(ClassRewriter.CONTEXT_INFO)) {
			return;
		}

		Field nextField = orderedFields(nonFinalfieldsOfClassesOnly(obj
				.getClass()))[selectedFieldPosition];
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

	public class ServerMapper implements Mapper {

		public int mapNewField(int oldPos) {
			if (oldPos < 14)
				return oldPos;
			else if (oldPos == 14)
				return -1;
			else
				return oldPos - 1;
		}

		public int mapOldField(int newPos) {
			if (newPos < 14)
				return newPos;
			else
				return newPos + 1;
		}
		
	}
	public class HandlerMapper implements Mapper {
		public int mapOldField(int field) {
			int newPosition = -1;
			switch (field) {
			case 0:
				newPosition = 0;
				break;
			case 1:
				newPosition = 1;
				break;
			case 2:
				newPosition = 2;
				break;
			case 3:
				newPosition = 3;
				break;
			case 4:
				newPosition = 4;
				break;
			case 5:
				newPosition = 5;
				break;
			case 6:
				newPosition = 6;
				break;
			case 7:
				newPosition = 7;
				break;
			case 8:
				newPosition = 8;
				break;
			case 9:
				newPosition = 9;
				break;
			case 10:
				newPosition = 10;
				break;
			case 11:
				newPosition = 11;
				break;
			case 12:
				newPosition = 12;
				break;
			case 13:
				newPosition = 13;
				break;
			case 14:
				newPosition = 14;
				break;
			case 15:
				newPosition = 15;
				break;
			case 16:
				newPosition = 16;
				break;
			case 17:
				newPosition = 17;
				break;
			case 18:
				newPosition = 18;
				break;
			case 19:
				newPosition = 19;
				break;
			case 20:
				newPosition = 20;
				break;
			case 21:
				newPosition = 21;
				break;
			case 22:
				newPosition = 22;
				break;
			case 23:
				newPosition = 23;
				break;
			case 24:
				newPosition = 24;
				break;
			case 25:
				newPosition = 25;
				break;
			case 27:
				newPosition = 26;
				break;
			case 28:
				newPosition = 27;
				break;
			case 29:
				newPosition = 28;
				break;
			case 30:
				newPosition = 29;
				break;
			case 31:
				newPosition = 30;
				break;
			}
			return newPosition;
		}

		public int mapNewField(int field) {
			int newPosition = -1;
			switch (field) {
			case 0:
				newPosition = 0;
				break;
			case 1:
				newPosition = 1;
				break;
			case 2:
				newPosition = 2;
				break;
			case 3:
				newPosition = 3;
				break;
			case 4:
				newPosition = 4;
				break;
			case 5:
				newPosition = 5;
				break;
			case 6:
				newPosition = 6;
				break;
			case 7:
				newPosition = 7;
				break;
			case 8:
				newPosition = 8;
				break;
			case 9:
				newPosition = 9;
				break;
			case 10:
				newPosition = 10;
				break;
			case 11:
				newPosition = 11;
				break;
			case 12:
				newPosition = 12;
				break;
			case 13:
				newPosition = 13;
				break;
			case 14:
				newPosition = 14;
				break;
			case 15:
				newPosition = 15;
				break;
			case 16:
				newPosition = 16;
				break;
			case 17:
				newPosition = 17;
				break;
			case 18:
				newPosition = 18;
				break;
			case 19:
				newPosition = 19;
				break;
			case 20:
				newPosition = 20;
				break;
			case 21:
				newPosition = 21;
				break;
			case 22:
				newPosition = 22;
				break;
			case 23:
				newPosition = 23;
				break;
			case 24:
				newPosition = 24;
				break;
			case 25:
				newPosition = 25;
				break;
			case 26:
				newPosition = 27;
				break;
			case 27:
				newPosition = 28;
				break;
			case 28:
				newPosition = 29;
				break;
			case 29:
				newPosition = 30;
				break;
			case 30:
				newPosition = 31;
				break;
			}
			return newPosition;
		}
	}
}

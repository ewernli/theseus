package ch.unibe.iam.scg.rewriter.helper;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import ch.unibe.iam.scg.ContextClassLoader;
import ch.unibe.iam.scg.ContextInfo;

public class ArrayInterceptor {
	
	 // ArrayInterceptor.arrayWriteInt(ai, 0, 2);
	  
	public static void arrayWriteInt( Object array, int pos, int value ) {
		ContextClassLoader.synchronizeArrayWrite(array, pos);
		((int[])array)[ pos ] = value;
	}
	
	public static int arrayReadInt( Object array, int pos ) {
		ContextClassLoader.synchronizeArrayRead(array, pos);
		return ((int[])array)[ pos ];
	}
	
	public static void arrayWriteObject( Object array, int pos, Object value ) {
		ContextClassLoader.synchronizeArrayWrite(array, pos);
		((Object[])array)[ pos ] = value;
	}
	
	public static Object arrayReadObject( Object array, int pos ) {
		ContextClassLoader.synchronizeArrayRead(array, pos);
		return ((Object[])array)[ pos ];
	}
	
	public static void arrayWriteByteOrBoolean( Object array, int pos, byte value ) {
		ContextClassLoader.synchronizeArrayWrite(array, pos);
		if( array instanceof byte[] )
			((byte[])array)[ pos ] = value;
		else
			((boolean[])array)[ pos ] = value > 0;
	}
	
	public static byte arrayReadByteOrBoolean( Object array, int pos ) {
		ContextClassLoader.synchronizeArrayRead(array, pos);
		if( array instanceof byte[] )
			return ((byte[])array)[ pos ];
		else
			return ((boolean[])array)[ pos ] ? (byte) 1 : (byte) 0;
	}
	
	public static void arrayWriteChar( Object array, int pos, char value ) {
		ContextClassLoader.synchronizeArrayWrite(array, pos);
		((char[])array)[ pos ] = value;
	}
	
	public static char arrayReadChar( Object array, int pos ) {
		ContextClassLoader.synchronizeArrayRead(array, pos);
		return ((char[])array)[ pos ];
	}
	
	
	// @TODO make it weak
	static public ConcurrentHashMap<Object, ContextInfo> arrayContextInfo = new ConcurrentHashMap<Object, ContextInfo>();
	
	public static ContextInfo contextInfoOfArray( Object array ) {
		return arrayContextInfo.get(array); // It must work, because we register it at creation time
	}
	
	public static Object registerArray( Object array ) {
		arrayContextInfo.put(array, new ContextInfo() );
		return array;
	}
}

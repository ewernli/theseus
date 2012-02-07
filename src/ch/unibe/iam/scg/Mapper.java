package ch.unibe.iam.scg;

import javassist.CannotCompileException;
import javassist.ClassMap;
import javassist.NotFoundException;

public class Mapper {
	static public class NameOnlyClassMap extends ClassMap {
		
		final String versionSuffix;
		
		public NameOnlyClassMap( String suffix){
			this.versionSuffix = suffix;
		}
		
		public Object get(Object jvmClassName) {
			String name = (String) jvmClassName;
			if (needsRewrite(name))
				return rewriteName(name);
			else
				return name;
		}
		
		private String rewriteName(String className) {
			return Mapper.rewriteName(className, versionSuffix);
		}
	}
	
	public static class NameAndLoaderClassMap extends ClassMap {
		
		final String versionSuffix;
		final InstrumentingClassLoader loader;
		
		public NameAndLoaderClassMap( String suffix, InstrumentingClassLoader l){
			this.versionSuffix = suffix;
			this.loader = l;
		}
		
		public Object get(Object jvmClassName) {
			String name = toJavaName((String)jvmClassName);
			//System.out.println(jvmClassName);
			if (needsRewrite(name) && !name.endsWith(versionSuffix))
			{
				try {
					loader.findCtClass( rewriteName( name ) );
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (CannotCompileException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
				return toJvmName( rewriteName(name) );
			}
			else
			{
				return toJvmName( name );
			}
		}
		
		private String rewriteName(String className) {
			return Mapper.rewriteName(className, versionSuffix);
		}

		
		
	}
	
	public static String rewriteName( String oldName, String versionSuffix ){ 
		if ( oldName.startsWith("ch.unibe.iam.scg.test") || 
			 oldName.startsWith("org.mortbay"))
		{
			return oldName + versionSuffix; 
		}
		else 
		{
			return erase( "ch.unibe.iam.scg.test.core." + oldName ) + versionSuffix;
		}
	};
	
	public static String unrewriteName( String newName, String versionSuffix ) {
		if ( newName.startsWith("ch.unibe.iam.scg.test.core.") )
		{
			return newName.substring( "ch.unibe.iam.scg.test.core.".length(), newName.length() - versionSuffix.length());
		}else
		{
			return newName.substring(0, newName.length() - versionSuffix.length());
		}
		
	};
	
	public static String normalize( String className, String versionSuffix )
	{
		// java.util.X --> unibe.test.core.java.util.X$$1
		// unibe.test.core.java.util.X$$1 --> unibe.test.core.java.util.X$$1
		// java.util.X$$1 --> java.util.X$$1
		
		if( needsRewrite(className) && ! className.endsWith(versionSuffix) )
		{
			return rewriteName(className, versionSuffix);
		}
		else
		{
			return className;
		}
	}
	
	public static boolean needsRewrite(String className) {
		/*return  (className.startsWith("ch.unibe.iam.scg.test") ||
				className.startsWith("java.util") || 
				//className.startsWith("java.text") ||
				className.startsWith("org.mortbay") ||
				className.startsWith("sun.util") ) && 
				//className.startsWith("sun.text") ) 
				! ( className.equals("java.util.TimeZone") ||
					className.equals("java.util.Date")||
					className.startsWith("java.util.concurrent.atomic")  ||
					className.equals("java.util.ResourceBundle") ||
					className.equals("java.util.ResourceBundle$Control") ||
					className.equals("java.util.Enumeration") ||
					className.equals("java.util.Locale") 
					) 
				; */
		
		return ! ( 
				className.equals("java.lang.Object") ||
				( className.startsWith("ch.unibe.iam.scg.") && 
						!className.startsWith("ch.unibe.iam.scg.test")) ||
				className.startsWith("java.lang") ||
				className.startsWith("java.io") ||
				className.startsWith("java.net") ||
				className.startsWith("java.nio") ||
				className.startsWith("sun.reflect") ||
				className.startsWith("sun.util.calendar") ||
				className.equals("java.util.Calendar") ||
				className.startsWith("java.util.GregorianCalendar" ) ||
				className.startsWith("sun.util.BuddhistCalendar") ||
				className.startsWith("java.util.Currency") ||
				//className.startsWith("com.sun.org.apache.xerces.internal") ||
				//className.startsWith("javax.xml.parsers") ||
				className.equals("java.security.AccessController") ||
				className.equals("java.security.PrivilegedAction") ||
				className.equals("java.security.PrivilegedExceptionAction") ||
				className.startsWith("sun.misc") ||
				className.startsWith("java.util.concurrent.locks") ||
				//className.startsWith("java.util.concurrent") || // should remove
				className.equals("java.util.TimeZone") ||
				className.equals("java.util.Date")||
				className.startsWith("java.util.concurrent.atomic")  ||
				className.equals("java.util.ResourceBundle") ||
				className.equals("java.util.ResourceBundle$Control") ||
				className.equals("java.util.Enumeration") ||
				className.equals("java.util.Locale") );
	}
	
	public static String erase( String genericSignature ) {
		int first = genericSignature.indexOf('<');
		if( first > 0 )
		{
			return genericSignature.substring(0, first);
		}
		return genericSignature;
	}
}

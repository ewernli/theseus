package ch.unibe.iam.scg;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Constructor;

public class ContextUtil {
	
	private static int counter = 2;
	
	public static ContextClassLoader contextOf( Object anObject )
	{
		return (ContextClassLoader) (anObject.getClass().getClassLoader());
	}
	public static synchronized ContextClassLoader nextContext( ContextClassLoader current )
	{
		try
		{
			File f = new File( "patch.txt" );
			if( f.exists() )
			{
				
				FileReader fr = new FileReader( f );
				BufferedReader bufRead = new BufferedReader(fr);
				String contextClassName = bufRead.readLine();
				bufRead.close();
				Class contextClazz = Class.forName( contextClassName );
				Constructor cst = contextClazz.getConstructor(String.class);
				ContextClassLoader context = (ContextClassLoader) cst.newInstance( "$$" + counter++ );
				current.setNext(context);
				f.delete();
				return context;
			}
			return current;
		}
		catch(Exception e)
		{ 
			throw new RuntimeException( "Could not get latest context", e );
		}
	}
}

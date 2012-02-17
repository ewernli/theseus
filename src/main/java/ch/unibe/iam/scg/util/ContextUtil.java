package ch.unibe.iam.scg.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Constructor;

import ch.unibe.iam.scg.ContextClassLoader;

public class ContextUtil {
	
	
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
				String currentSuffix = current.suffix().substring(2);
				String newSuffix = "$$" + ( Integer.valueOf(currentSuffix) + 1 );
				ContextClassLoader context = (ContextClassLoader) cst.newInstance( newSuffix );
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

package ch.unibe.iam.scg.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;

import ch.unibe.iam.scg.Context;
import ch.unibe.iam.scg.ContextAware;
import ch.unibe.iam.scg.ContextClassLoader;

public class CommandReader implements Runnable {
	
	String currentPath = ".";
	
	public void run() 
	{
		try{
				
			while( true )
			{
				String[] line = readInput();
				
				if( line[0].equals("cd")) 
				{
					currentPath = line[1];
				}
				else if ( line[0].equals("ls"))
				{
					printDirectory( currentPath );
				}			
				
				if( fileExists( "patch") ) {
					String contextClass = readAndDeleteFile( "patch" );
					Context newContext = currentContext().newContext( contextClass );
					newContext.invoke( this, "spawn", new Class[0], new Object[0] );
					return;
				}
			}
		}
		catch( Exception e )
		{
			throw new RuntimeException("Ooops", e);
		}
	}
	
	public void spawn()
	{
		new Thread(this).start();
	}
	

	
	public boolean fileExists( String name )
	{
		File f = new File( name );
		return f.exists();
	}
	public String[] readInput() throws IOException
	{
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		return in.readLine().split(" ");
		
	}
	public void printDirectory( String path )
	{
		
	}
	
	public Context currentContext()
	{
		return (Context) this.getClass().getClassLoader();
	}
	
	public String readAndDeleteFile( String name ) throws IOException
	{
		BufferedReader bufRead = null;
		try{
			FileReader fr = new FileReader( new File(name) );
			bufRead = new BufferedReader(fr);
			String contextClassName = bufRead.readLine();
			return contextClassName;
		}
		finally {
			bufRead.close();
		}
	}
}

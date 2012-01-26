package ch.unibe.iam.scg.rewriter.helper;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import ch.unibe.iam.scg.ContextAware;
import ch.unibe.iam.scg.ContextClassLoader;
import ch.unibe.iam.scg.ContextInfo;

public class ReflectionHelper {
	public static ContextAware buildNewInstance( ContextAware oldInstance, ContextClassLoader newLoader )
	{
		try {
			String oldName = oldInstance.getClass().getName();
			Class newClass = newLoader.resolve(oldName);
			Constructor constructor = newClass.getConstructor( ContextInfo.class );
			return (ContextAware) constructor.newInstance( new ContextInfo() );
		} catch (Exception e) {
			throw new RuntimeException( "Can not create new instance reflectively", e );
		}
	}
}

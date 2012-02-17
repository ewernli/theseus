package ch.unibe.iam.scg.test;

import java.lang.reflect.Field;

import ch.unibe.iam.scg.ContextAware;
import ch.unibe.iam.scg.ContextClassLoader;

public class Data {
	public String data;
	public int version;
	public Data otherData;
	
	public Data()
	{
		data = "Hello";
		version = 0;
		otherData = null;
	}
	public Data( String d, int v, Data a )
	{
		data =d;
		version=v;
		otherData=a;
	}
	public void setOtherData( Data d )
	{
		otherData = d;
	}
	public void build()
	{
		Data current = this;
		for( int i=0;i<10000;i++)
		{
			current.otherData = new Data( "O"+i,0,null);
			current = current.otherData;
		}
	}
	public void syncFrom( Object oldData ) throws Exception
	{
		ContextClassLoader l = (ContextClassLoader) Data.class.getClassLoader();
		sun.misc.Unsafe u = sun.misc.Unsafe.getUnsafe();
		ContextAware o = null;
		data = (String) u.getObjectVolatile(oldData, 8 );
		version = u.getIntVolatile(oldData, 20 );
		o = (ContextAware) u.getObjectVolatile(oldData, 12 );
		if( otherData!=null)
		{
			otherData = o.getContextInfo().next==null?
					(Data) o.migrateToNext( l ):(Data)o.getContextInfo().next;
			otherData.syncFrom(o);		
		}
		((ContextAware)this).getContextInfo().dirty=0x00000000;
	}
}

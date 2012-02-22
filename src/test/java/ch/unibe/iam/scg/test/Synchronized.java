package ch.unibe.iam.scg.test;

public class Synchronized {
	
	private Object lock = new Object();
	
	public synchronized Object  sync1()
	{
		return lock;
	}
	
	
	public  Object  sync2()
	{	
		
//		ALOAD 3
//	    DUP
//	    ASTORE 1
//	    MONITORENTER
		synchronized (this) {
			return lock;
		}
	}
	

	public void sync3 (Object f) {
		
//		 ALOAD 1
//		    DUP
//		    ASTORE 2
	    synchronized(f) {
	        sync1();
	    }
	}
	
	public  int block() throws InterruptedException
	{	
//		ALOAD 3
//	    DUP
//	    ASTORE 1
		synchronized (lock) {
			lock.wait();
			return 42;
		}
	}
	
	public  void release()
	{	
		synchronized (lock) {
			lock.notify();
		}
	}
}

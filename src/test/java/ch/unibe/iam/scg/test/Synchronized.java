package ch.unibe.iam.scg.test;

public class Synchronized {
	
	private Object lock = new Object();
	
	public synchronized Object  sync1()
	{
		return lock;
	}
	
	
	public  Object  sync2()
	{	
		synchronized (this) {
			return lock;
		}
	}
	

	public void sync3 (Object f) {
	    synchronized(f) {
	        sync1();
	    }
	}
	
	public  int block() throws InterruptedException
	{	
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

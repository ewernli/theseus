package ch.unibe.iam.scg.test;

public class Generic<A> {

	A item;
	
	public void set(A a)
	{
		item =a;
	}
	
	public A get() 
	{
		return item;
	}
}

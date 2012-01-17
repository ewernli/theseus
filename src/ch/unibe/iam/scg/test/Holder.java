package ch.unibe.iam.scg.test;

import ch.unibe.iam.scg.rewriter.ArrayInterceptor;

public class Holder {
	int[] data;
	public Holder()
	{
		data = new int[2];
		data[0] = 2;
		data[1] = 2;
	}
	
	public int sum()
	{
		return data[0] + data[1];
	}
}

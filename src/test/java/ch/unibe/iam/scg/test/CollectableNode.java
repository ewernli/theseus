package ch.unibe.iam.scg.test;

public class CollectableNode extends Node {

	@Override
	protected void finalize() throws Throwable {
		System.out.println("Finalize dummy data");
	}
	
}

package ch.unibe.iam.scg.test;

public class SubSubWriteConstructor extends SubWriteConstructor {
	
	public SubSubWriteConstructor(int dummy) {
		System.out.println("Level 2");
		value = 82;
	}

	public SubSubWriteConstructor() {
		System.out.println("Level 2");
		value = 82;
	}
}

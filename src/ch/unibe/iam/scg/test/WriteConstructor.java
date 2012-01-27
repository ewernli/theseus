package ch.unibe.iam.scg.test;

public  class WriteConstructor {
	int value;
	public WriteConstructor()
	{
		System.out.println("Level 0");
		value = 42;
	}
	public String toString() {
		return String.valueOf(value);
	}
}

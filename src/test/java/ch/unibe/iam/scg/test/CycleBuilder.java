package ch.unibe.iam.scg.test;

public class CycleBuilder {
	public CycleLeft buildLeftCycle()
	{
		CycleLeft left = new CycleLeft();
		CycleRight right = new CycleRight();
		left.setRight(right);
		return left;
	}
}

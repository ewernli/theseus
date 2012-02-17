package ch.unibe.iam.scg.test;

public class CycleRight {
	CycleLeft left;
	String value;
	
	public CycleRight()
	{
		value = "Right";
	}

	
	public CycleLeft getLeft() {
		return left;
	}


	public void setLeft(CycleLeft left) {
		this.left = left;
		left.right = this;
	}
	
	public String getValue() {
		return value;
	}

	public String toString() {
		return left.getValue() + ": [" + this.getValue() + "]";
	}
}

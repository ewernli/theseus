package ch.unibe.iam.scg.test;

public class CycleLeft {
	CycleRight right;
	String value;
	
	public CycleLeft()
	{
		value = "Left";
	}

	
	public CycleRight getRight() {
		return right;
	}


	public void setRight(CycleRight right) {
		this.right = right;
		right.left = this;
	}


	public String getValue() {
		return value;
	}


	public String toString() {
		return  "[" + this.getValue() + "] : " + right.getValue();
	}
}

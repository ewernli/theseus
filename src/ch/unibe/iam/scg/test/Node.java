package ch.unibe.iam.scg.test;

public class Node {
	
	protected String name;
	private Node a, b;

	public Node()
	{
		System.out.println( "before");
		name = this.getClass().getName();
		System.out.println( "after");
	}
	
	public void deepen()
	{
		a = new LeafNode();
		b = new LeafNode();
	}
	
	public Node getA() {
		return a;
	}

	public void setA(Node a) {
		this.a = a;
	}

	public Node getB() {
		return b;
	}

	public void setB(Node b) {
		this.b = b;
	}
	
	public String toString()
	{
		String s = this.name + ":" + this.getClass().getName() + "@" + this.getClass().getSuperclass().getName();
		if( a == null && b == null )
			return "[" + s +  "]";
		else if ( a == null )
			return "[" + s + "," + b.toString() + "]";
		else if ( b == null )
			return "[" + a.toString() + "," + s + "]";
		else
			return "[" + a.toString() + "," + s + "," + b.toString() + "]";
	}
}

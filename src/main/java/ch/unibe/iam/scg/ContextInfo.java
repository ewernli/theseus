package ch.unibe.iam.scg;

public class ContextInfo {
	public boolean global= false;
	public Object next = null;
	public Object prev = null;
	public long dirty = 0;
	public Object id = new Object(); // contextual id for synchronization
}

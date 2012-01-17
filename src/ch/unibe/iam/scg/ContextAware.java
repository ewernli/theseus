package ch.unibe.iam.scg;

public interface ContextAware extends Cloneable {
	public ContextInfo getContextInfo();
	public void globalize();
	public ContextAware migrateToNext( ContextClassLoader nextLoader );
}

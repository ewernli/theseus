package ch.unibe.iam.scg.test;

public class ClassLiteral {
	public Object newSelfType() throws InstantiationException, IllegalAccessException {
		return ClassLiteral.class.newInstance();
	}
}

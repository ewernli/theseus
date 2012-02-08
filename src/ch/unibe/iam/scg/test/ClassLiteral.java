package ch.unibe.iam.scg.test;

public class ClassLiteral {
	public Object newSelfType() throws InstantiationException, IllegalAccessException {
		Class c = ClassLiteral.class;
		return c.newInstance();
	}
}

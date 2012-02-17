package ch.unibe.iam.scg.rewriter;

import javassist.CannotCompileException;
import javassist.CtClass;

public interface ClassRewriter {
	public static String GETTER_PREFIX = "_get";
	public static String SETTER_PREFIX = "_set";
	public static String CONTEXT_INFO = "contextInfo";
	void rewrite(CtClass clazz) throws CannotCompileException;
}

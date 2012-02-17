package ch.unibe.iam.scg.rewriter;

import javassist.CannotCompileException;
import javassist.CtClass;

public class ConcurrencyControlRewriter implements ClassRewriter {

	public void rewrite(CtClass clazz) throws CannotCompileException {
		// This one will probably require BCEL for low-level bytecode manipulation
	}

}

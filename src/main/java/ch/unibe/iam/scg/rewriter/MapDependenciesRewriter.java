package ch.unibe.iam.scg.rewriter;

import ch.unibe.iam.scg.ContextClassLoader;
import ch.unibe.iam.scg.InstrumentingClassLoader;
import ch.unibe.iam.scg.Mapper;
import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.NewExpr;

public class MapDependenciesRewriter implements ClassRewriter {

	final String versionSuffix;
	final InstrumentingClassLoader loader;

	public MapDependenciesRewriter(InstrumentingClassLoader loader, String suffix) {
		this.versionSuffix = suffix;
		this.loader = loader;
	}

	public void rewrite(CtClass ctClass) throws CannotCompileException {
		// System.out.println( "-> Remap dependencies for "+ ctClass.getName() );
		
		ctClass.replaceClassName( new Mapper.NameAndLoaderClassMap(versionSuffix, loader) );
		/*for (final CtBehavior ctMethod : ctClass.getDeclaredBehaviors()) {
			System.out.println("Method:" + ctMethod.getName());
			ExprEditor exprEditor = new InterceptConstructorEditor(ctMethod);
			ctMethod.instrument(exprEditor);
		}*/

		// System.out.println( "<- Remaped dependencies for "+ ctClass.getName() );
	}

	class InterceptConstructorEditor extends ExprEditor {

		final CtBehavior ctMethod;

		public InterceptConstructorEditor(CtBehavior method) {
			this.ctMethod = method;
		}

		private String rewriteName( String className ) {
			return Mapper.rewriteName(className, versionSuffix );
		}
		
		private String unrewriteName( String newName ) {
			return Mapper.unrewriteName(newName, versionSuffix);
		}
		
		private boolean needsRewrite( String className ) {
			return Mapper.needsRewrite(className);
		}

		@Override
		public void edit(NewExpr newInstance) throws CannotCompileException {
			String fieldClassName = newInstance.getClassName();
			if (needsRewrite(fieldClassName)
					&& !fieldClassName.endsWith(versionSuffix)) {
				//System.out.println("Class" + newInstance.getClassName());
				try {
					loader.findCtClass(  rewriteName(fieldClassName) );
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				newInstance.replace("$_ = new " + rewriteName(fieldClassName)
						+ "( $$ );");
			}
			// newInstance.replace(
			// "$_ = new ch.unibe.iam.scg.Entity( \"a\" );");
		}
	}
}
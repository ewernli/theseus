package ch.unibe.iam.scg.rewriter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Opcodes.*;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;

public class ConcurrencyControlRewriter implements ClassRewriter {

	public void rewrite(CtClass clazz) throws CannotCompileException {
		// This one will probably require BCEL for low-level bytecode
		// manipulation
		try {
			ClassPool cp = ClassPool.getDefault();
			
			byte[] b1 = clazz.toBytecode(); clazz.defrost();
			ClassReader cr = new ClassReader(b1); 
			ClassWriter cw = new ClassWriter(0);  // new ClassWriter(cr, 0); 
			TraceClassVisitor ca = new TraceClassVisitor( cw, new PrintWriter(System.out, true) );
			//CheckClassAdapter cca = new CheckClassAdapter(ca); 
			ConcurrencyControlRewriter.Adapter ca2 = new ConcurrencyControlRewriter.Adapter(ca); 
			cr.accept(ca2, 0); 
			byte[] b2 = cw.toByteArray(); 
			cp.makeClass( new ByteArrayInputStream( b2 ));
		} catch (IOException e) {
			throw new CannotCompileException(e);
		}
	}

	public static class Adapter extends ClassVisitor {

		public Adapter(ClassVisitor cv) {
			super(Opcodes.ASM4, cv);
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String desc,
				String signature, String[] exceptions) {
			MethodVisitor mv;
			
			if( (access & Opcodes.ACC_SYNCHRONIZED) > 0 ) {
				mv = cv.visitMethod(access & ~ Opcodes.ACC_SYNCHRONIZED, 
						name, desc, signature, exceptions);
				if (mv != null) {
					mv = new MethodAdapter(mv, true);
				}
				return mv;
			}
			else
			{
				mv = cv.visitMethod(access, name, desc, signature, exceptions);
				if (mv != null) {
					mv = new MethodAdapter(mv, false);
				}
				return mv;
			}
			
		}
	}

	public static class MethodAdapter extends MethodVisitor {

		boolean addSynchronized;
		
		public MethodAdapter(MethodVisitor mv, boolean add) {
			super(Opcodes.ASM4, mv);
			addSynchronized = add;
		}

		public void visitCode() {
			mv.visitCode();
			
			if( addSynchronized ) {
				mv.visitVarInsn(Opcodes.ALOAD, 0); // this
				mv.visitInsn(Opcodes.MONITORENTER);
			}
			// mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System",
			// "currentTimeMillis", "()J");
		}

		public void visitInsn(int opcode) {
			
			if( addSynchronized ) {
				if ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN)
						|| opcode == Opcodes.ATHROW) {
					mv.visitVarInsn(Opcodes.ALOAD, 0); // this
					mv.visitInsn(Opcodes.MONITOREXIT);
				}
			}
			mv.visitInsn(opcode);
		}

		public void visitMaxs(int maxStack, int maxLocals) {
			mv.visitMaxs(maxStack + 1, maxLocals);
		}

	}
}

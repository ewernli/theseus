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

import ch.unibe.iam.scg.util.PatternMethodAdapter;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.bytecode.Descriptor;

public class ConcurrencyControlRewriter implements ClassRewriter {

	public void rewrite(final CtClass clazz) throws CannotCompileException {
		try {
			ClassPool cp = ClassPool.getDefault();
			
			byte[] b1 = clazz.toBytecode(); clazz.defrost();
			ClassReader cr = new ClassReader(b1); 
			ClassWriter cw = new ClassWriter(0);  // new ClassWriter(cr, 0); 
			TraceClassVisitor ca = new TraceClassVisitor( cw, new PrintWriter(System.out, true) );
			//CheckClassAdapter cca = new CheckClassAdapter(ca); 
			ConcurrencyControlRewriter.Adapter ca2 = new ConcurrencyControlRewriter.Adapter(ca, clazz); 
			cr.accept(ca2, 0); 
			byte[] b2 = cw.toByteArray(); 
			cp.makeClass( new ByteArrayInputStream( b2 ));
		} catch (IOException e) {
			throw new CannotCompileException(e);
		}
	}

	public static class Adapter extends ClassVisitor {

		CtClass target;
		
		public Adapter(ClassVisitor cv, CtClass target) {
			super(Opcodes.ASM4, cv);
			this.target = target;
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String desc,
				String signature, String[] exceptions) {
			MethodVisitor mv;
		
			if( (access & Opcodes.ACC_SYNCHRONIZED) > 0 ) {
				mv = cv.visitMethod(access & ~ Opcodes.ACC_SYNCHRONIZED, 
						name, desc, signature, exceptions);
				if (mv != null) {
					mv = new MethodAdapter(mv, true, target);
				}
				return mv;
			}
			else
			{
				mv = cv.visitMethod(access, name, desc, signature, exceptions);
				if (mv != null) {
					mv = new MethodAdapter(mv, false, target);
				}
				return mv;
			}
			
		}
	}

	public static class MethodAdapter extends PatternMethodAdapter {

		CtClass target;
		
//		ALOAD 3
//	    DUP
//	    ASTORE 1
		
		public enum State {
			NOTHING, ALOAD, ALOAD_DUP, ALOAD_DUP_ASTORE
		}
		
		State state = State.NOTHING;
		private int astoreIndex;
		
		boolean addSynchronized;
		
		public MethodAdapter(MethodVisitor mv, boolean add, CtClass target) {
			super(mv);
			addSynchronized = add;
			this.target = target; 
		}
		
		@Override
		public void visitVarInsn(int opcode, int var) {
			
			if( opcode == Opcodes.ALOAD && state == State.NOTHING && var == 0) {
				// We only match load of "this" (var 0)
				state = State.ALOAD;
			}
			else if (opcode == Opcodes.ASTORE && state == State.ALOAD_DUP ) {
				state = State.ALOAD_DUP_ASTORE;
				astoreIndex = var;
			} else
			{
				super.visitVarInsn(opcode, var);
			}
		}


		@Override
		public void visitFieldInsn(int opcode, String owner, String name,
				String desc) {
			super.visitFieldInsn(opcode, owner, name, desc);
		}

		public void visitCode() {
			mv.visitCode();
			
			if( addSynchronized ) {
				mv.visitVarInsn(Opcodes.ALOAD, 0); // this
				mv.visitInsn(Opcodes.MONITORENTER);
			}
		}

		public void visitInsn(int opcode) {
			
			if( addSynchronized ) {
				if ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN)
						|| opcode == Opcodes.ATHROW) {
					mv.visitVarInsn(Opcodes.ALOAD, 0); // this
					mv.visitInsn(Opcodes.MONITOREXIT);
				}
			}
			
			if( opcode == Opcodes.DUP && state == State.ALOAD ) {
				state = State.ALOAD_DUP;
			}
			else if (opcode == Opcodes.MONITORENTER && state == State.ALOAD_DUP_ASTORE ) {
				// Bingo!
				String targetOwner = Descriptor.toJvmName(target.getName());
				String contextInfoOwner = "ch/unibe/iam/scg/ContextInfo";
				String contextInfoJvmName = "Lch/unibe/iam/scg/ContextInfo;";
				String objectJvmName = "Ljava/lang/Object;";
				
				mv.visitVarInsn( Opcodes.ALOAD, 0);
				mv.visitFieldInsn(Opcodes.GETFIELD, targetOwner, CONTEXT_INFO, contextInfoJvmName);
				mv.visitFieldInsn(Opcodes.GETFIELD, contextInfoOwner, "id", objectJvmName);
				//mv.visitVarInsn( Opcodes.ALOAD, aloadIndex);
				mv.visitInsn(Opcodes.DUP);
				mv.visitVarInsn( Opcodes.ASTORE, astoreIndex);
				mv.visitInsn(opcode);
				state = State.NOTHING;
			} 
			else 
			{
				super.visitInsn(opcode);
			}
		}

		public void visitMaxs(int maxStack, int maxLocals) {
			mv.visitMaxs(maxStack + 1, maxLocals);
		}

		@Override
		protected void visitInsn() {
			// flush the queued instruction
			switch( state) 
			{
			case NOTHING:
				break;
			case ALOAD:
				mv.visitVarInsn( Opcodes.ALOAD, 0);
				break;
			case ALOAD_DUP:
				mv.visitVarInsn( Opcodes.ALOAD, 0);
				mv.visitInsn(Opcodes.DUP);
				break;
			case ALOAD_DUP_ASTORE:
				mv.visitVarInsn( Opcodes.ALOAD, 0);
				mv.visitInsn(Opcodes.DUP);
				mv.visitVarInsn( Opcodes.ASTORE, astoreIndex);
				break;
			}
			state = State.NOTHING;
		}

		
	}
}

package ch.unibe.iam.scg.util;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * ASM Guide example class.
 * 
 * @author Eric Bruneton
 */
public abstract class PatternMethodAdapter extends MethodVisitor {

  public PatternMethodAdapter(MethodVisitor mv) {
	  super(Opcodes.ASM4, mv);
  }

  public void visitFrame(int type, int nLocal, Object[] local,
      int nStack, Object[] stack) {
    visitInsn();
    mv.visitFrame(type, nLocal, local, nStack, stack);
  }

  public void visitInsn(int opcode) {
    visitInsn();
    mv.visitInsn(opcode);
  }

  public void visitIntInsn(int opcode, int operand) {
    visitInsn();
    mv.visitIntInsn(opcode, operand);
  }

  public void visitVarInsn(int opcode, int var) {
    visitInsn();
    mv.visitVarInsn(opcode, var);
  }

  public void visitTypeInsn(int opcode, String desc) {
    visitInsn();
    mv.visitTypeInsn(opcode, desc);
  }

  public void visitFieldInsn(int opcode, String owner, String name,
      String desc) {
    visitInsn();
    mv.visitFieldInsn(opcode, owner, name, desc);
  }

  public void visitMethodInsn(int opcode, String owner, String name,
      String desc) {
    visitInsn();
    mv.visitMethodInsn(opcode, owner, name, desc);
  }

  public void visitJumpInsn(int opcode, Label label) {
    visitInsn();
    mv.visitJumpInsn(opcode, label);
  }

  public void visitLabel(Label label) {
    visitInsn();
    mv.visitLabel(label);
  }

  public void visitLdcInsn(Object cst) {
    visitInsn();
    mv.visitLdcInsn(cst);
  }

  public void visitIincInsn(int var, int increment) {
    visitInsn();
    mv.visitIincInsn(var, increment);
  }

  public void visitTableSwitchInsn(int min, int max, Label dflt,
      Label labels[]) {
    visitInsn();
    mv.visitTableSwitchInsn(min, max, dflt, labels);
  }

  public void visitLookupSwitchInsn(Label dflt, int keys[],
      Label labels[]) {
    visitInsn();
    mv.visitLookupSwitchInsn(dflt, keys, labels);
  }

  public void visitMultiANewArrayInsn(String desc, int dims) {
    visitInsn();
    mv.visitMultiANewArrayInsn(desc, dims);
  }

  public void visitMaxs(int maxStack, int maxLocals) {
    visitInsn();
    mv.visitMaxs(maxStack, maxLocals);
  }

  protected abstract void visitInsn();
}
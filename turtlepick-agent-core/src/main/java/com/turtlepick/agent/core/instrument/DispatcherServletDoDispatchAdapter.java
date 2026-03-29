package com.turtlepick.agent.core.instrument;

import com.turtlepick.agent.core.trace.HttpRequestContextBridge;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

public final class DispatcherServletDoDispatchAdapter extends AdviceAdapter {

    private static final String BRIDGE_OWNER = Type.getInternalName(HttpRequestContextBridge.class);
    private static final String SAFE_ENTER_DESC =
            Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Object.class));
    private static final String SAFE_EXIT_DESC =
            Type.getMethodDescriptor(Type.VOID_TYPE);

    private final Label startLabel = new Label();
    private final Label endLabel = new Label();
    private final Label handlerLabel = new Label();
    private int throwableLocalIndex = -1;

    protected DispatcherServletDoDispatchAdapter(
            MethodVisitor methodVisitor,
            int access,
            String name,
            String descriptor) {
        super(Opcodes.ASM9, methodVisitor, access, name, descriptor);
    }

    @Override
    public void visitCode() {
        super.visitCode();
        throwableLocalIndex = newLocal(Type.getType(Throwable.class));
        visitTryCatchBlock(startLabel, endLabel, handlerLabel, null);
        visitLabel(startLabel);
        loadArg(0);
        visitMethodInsn(INVOKESTATIC, BRIDGE_OWNER, "safeEnter", SAFE_ENTER_DESC, false);
    }

    @Override
    protected void onMethodExit(int opcode) {
        if (opcode != ATHROW) {
            visitMethodInsn(INVOKESTATIC, BRIDGE_OWNER, "safeExit", SAFE_EXIT_DESC, false);
        }
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        visitLabel(endLabel);
        visitLabel(handlerLabel);
        storeLocal(throwableLocalIndex);
        visitMethodInsn(INVOKESTATIC, BRIDGE_OWNER, "safeExit", SAFE_EXIT_DESC, false);
        loadLocal(throwableLocalIndex);
        throwException();
        super.visitMaxs(maxStack, maxLocals);
    }
}

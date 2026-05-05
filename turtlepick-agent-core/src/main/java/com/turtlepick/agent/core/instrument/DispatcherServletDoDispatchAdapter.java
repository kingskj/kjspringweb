package com.turtlepick.agent.core.instrument;

import com.turtlepick.agent.core.trace.AgentHttpBridge;
import com.turtlepick.agent.core.trace.HttpRequestContextBridge;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

public final class DispatcherServletDoDispatchAdapter extends AdviceAdapter {

    private static final String BRIDGE_OWNER = Type.getInternalName(HttpRequestContextBridge.class);
    private static final String SAFE_EXIT_DESC =
            Type.getMethodDescriptor(Type.VOID_TYPE);
    private static final String HTTP_BRIDGE_OWNER = Type.getInternalName(AgentHttpBridge.class);
    private static final String SAFE_ENTER_OR_HANDLE_DESC =
            Type.getMethodDescriptor(Type.BOOLEAN_TYPE,
                    Type.getType(Object.class), Type.getType(Object.class));

    private final Label startLabel = new Label();
    private final Label endLabel = new Label();
    private final Label handlerLabel = new Label();

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
        visitTryCatchBlock(startLabel, endLabel, handlerLabel, null);
        visitLabel(startLabel);

        loadArg(0);
        loadArg(1);
        visitMethodInsn(INVOKESTATIC, HTTP_BRIDGE_OWNER, "safeEnterOrHandle",
                SAFE_ENTER_OR_HANDLE_DESC, false);
        Label normalFlowLabel = new Label();
        visitJumpInsn(IFEQ, normalFlowLabel);
        visitInsn(RETURN);
        visitLabel(normalFlowLabel);
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
        visitMethodInsn(INVOKESTATIC, BRIDGE_OWNER, "safeExit", SAFE_EXIT_DESC, false);
        throwException();
        super.visitMaxs(maxStack, maxLocals);
    }
}

package com.turtlepick.agent.core.instrument;

import com.turtlepick.agent.core.trace.RuntimeMethodBridge;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

public final class MethodProbeAdviceAdapter extends AdviceAdapter {

    private static final String BRIDGE_OWNER =
            Type.getInternalName(RuntimeMethodBridge.class);

    private static final String ENTER_DESC =
            Type.getMethodDescriptor(
                    Type.VOID_TYPE,
                    Type.INT_TYPE,
                    Type.getType(String.class)
            );

    private static final String EXIT_DESC =
            Type.getMethodDescriptor(
                    Type.VOID_TYPE,
                    Type.INT_TYPE,
                    Type.BOOLEAN_TYPE
            );

    private final int methodId;
    private final String fqcnMethod;

    protected MethodProbeAdviceAdapter(
            MethodVisitor methodVisitor,
            int access,
            String name,
            String descriptor,
            int methodId,
            String fqcnMethod) {
        super(Opcodes.ASM9, methodVisitor, access, name, descriptor);
        this.methodId = methodId;
        this.fqcnMethod = fqcnMethod;
    }

    @Override
    protected void onMethodEnter() {
        push(methodId);
        visitLdcInsn(fqcnMethod);
        visitMethodInsn(INVOKESTATIC, BRIDGE_OWNER, "enter", ENTER_DESC, false);
    }

    @Override
    protected void onMethodExit(int opcode) {
        push(methodId);
        push(opcode == ATHROW);
        visitMethodInsn(INVOKESTATIC, BRIDGE_OWNER, "exit", EXIT_DESC, false);
    }
}

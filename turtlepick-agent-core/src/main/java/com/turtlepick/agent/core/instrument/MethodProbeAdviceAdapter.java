package com.turtlepick.agent.core.instrument;

import com.turtlepick.agent.core.trace.RuntimeMethodBridge;
import org.objectweb.asm.Label;
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

    private static final String EXIT_THROWABLE_ARGS_DESC =
            Type.getMethodDescriptor(
                    Type.VOID_TYPE,
                    Type.INT_TYPE,
                    Type.getType(Throwable.class),
                    Type.getType(Object[].class)
            );

    private final int methodId;
    private final String fqcnMethod;
    private final Label startLabel = new Label();
    private final Label endLabel = new Label();
    private final Label handlerLabel = new Label();

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
    public void visitCode() {
        visitTryCatchBlock(startLabel, endLabel, handlerLabel, null);
        super.visitCode();
    }

    @Override
    protected void onMethodEnter() {
        push(methodId);
        visitLdcInsn(fqcnMethod);
        visitMethodInsn(INVOKESTATIC, BRIDGE_OWNER, "enter", ENTER_DESC, false);
        visitLabel(startLabel);
    }

    @Override
    protected void onMethodExit(int opcode) {
        if (opcode == ATHROW) {
            return;
        }
        push(methodId);
        push(false);
        visitMethodInsn(INVOKESTATIC, BRIDGE_OWNER, "exit", EXIT_DESC, false);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        visitLabel(endLabel);
        visitLabel(handlerLabel);

        int exceptionLocal = newLocal(Type.getType(Throwable.class));
        storeLocal(exceptionLocal);

        int argsLocal = newLocal(Type.getType(Object[].class));
        loadArgArray();
        storeLocal(argsLocal);

        push(methodId);
        loadLocal(exceptionLocal);
        loadLocal(argsLocal);
        visitMethodInsn(INVOKESTATIC, BRIDGE_OWNER, "exit", EXIT_THROWABLE_ARGS_DESC, false);

        loadLocal(exceptionLocal);
        throwException();

        super.visitMaxs(maxStack, maxLocals);
    }
}

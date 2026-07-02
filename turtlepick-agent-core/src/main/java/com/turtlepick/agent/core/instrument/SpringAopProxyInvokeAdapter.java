package com.turtlepick.agent.core.instrument;

import com.turtlepick.agent.core.trace.RuntimeMethodBridge;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

import java.lang.reflect.Method;

final class SpringAopProxyInvokeAdapter extends AdviceAdapter {

    private static final String BRIDGE_OWNER =
            Type.getInternalName(RuntimeMethodBridge.class);

    private static final String ENTER_INTERFACE_DESC =
            Type.getMethodDescriptor(
                    Type.INT_TYPE,
                    Type.getType(Object.class),
                    Type.getType(Method.class)
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

    private final Label startLabel = new Label();
    private final Label endLabel = new Label();
    private final Label handlerLabel = new Label();

    private int methodIdLocal = -1;

    SpringAopProxyInvokeAdapter(
            MethodVisitor methodVisitor,
            int access,
            String name,
            String descriptor) {
        super(Opcodes.ASM9, methodVisitor, access, name, descriptor);
    }

    @Override
    public void visitCode() {
        visitTryCatchBlock(startLabel, endLabel, handlerLabel, null);
        super.visitCode();
    }

    @Override
    protected void onMethodEnter() {
        methodIdLocal = newLocal(Type.INT_TYPE);
        push(0);
        storeLocal(methodIdLocal);

        loadArg(0);
        loadArg(1);
        visitMethodInsn(INVOKESTATIC, BRIDGE_OWNER, "enterInterfaceMethod",
                ENTER_INTERFACE_DESC, false);
        storeLocal(methodIdLocal);

        visitLabel(startLabel);
    }

    @Override
    protected void onMethodExit(int opcode) {
        if (opcode == ATHROW) {
            return;
        }
        Label skipLabel = new Label();
        loadLocal(methodIdLocal);
        visitJumpInsn(IFLE, skipLabel);
        loadLocal(methodIdLocal);
        push(false);
        visitMethodInsn(INVOKESTATIC, BRIDGE_OWNER, "exit", EXIT_DESC, false);
        visitLabel(skipLabel);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        visitLabel(endLabel);
        visitLabel(handlerLabel);

        int exceptionLocal = newLocal(Type.getType(Throwable.class));
        storeLocal(exceptionLocal);

        Label rethrowLabel = new Label();
        loadLocal(methodIdLocal);
        visitJumpInsn(IFLE, rethrowLabel);
        loadLocal(methodIdLocal);
        loadLocal(exceptionLocal);
        loadArg(2);
        visitMethodInsn(INVOKESTATIC, BRIDGE_OWNER, "exit", EXIT_THROWABLE_ARGS_DESC, false);
        visitLabel(rethrowLabel);

        loadLocal(exceptionLocal);
        throwException();

        super.visitMaxs(maxStack, maxLocals);
    }
}

package com.turtlepick.agent.core.instrument;

import com.turtlepick.agent.core.trace.AgentHttpBridge;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

final class TomcatFilterChainDoFilterAdapter extends AdviceAdapter {

    private static final String FILTER_BRIDGE_OWNER =
            Type.getInternalName(AgentHttpBridge.class);
    private static final String SAFE_INTERCEPT_DESC =
            Type.getMethodDescriptor(Type.BOOLEAN_TYPE,
                    Type.getType(Object.class), Type.getType(Object.class));

    protected TomcatFilterChainDoFilterAdapter(
            MethodVisitor mv,
            int access,
            String name,
            String descriptor) {
        super(Opcodes.ASM9, mv, access, name, descriptor);
    }

    @Override
    public void visitCode() {
        super.visitCode();
        loadArg(0);
        loadArg(1);
        visitMethodInsn(INVOKESTATIC, FILTER_BRIDGE_OWNER, "safeIntercept",
                SAFE_INTERCEPT_DESC, false);
        Label normalFlowLabel = new Label();
        visitJumpInsn(IFEQ, normalFlowLabel);
        visitInsn(RETURN);
        visitLabel(normalFlowLabel);
    }
}

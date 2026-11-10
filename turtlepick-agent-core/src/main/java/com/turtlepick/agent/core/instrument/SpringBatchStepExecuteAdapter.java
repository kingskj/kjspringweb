package com.turtlepick.agent.core.instrument;

import com.turtlepick.agent.core.trace.RuntimeMethodBridge;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

final class SpringBatchStepExecuteAdapter extends AdviceAdapter {

    private static final String BRIDGE_OWNER =
            Type.getInternalName(RuntimeMethodBridge.class);
    private static final String ON_STEP_RETURNED_DESC =
            Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Object.class));

    SpringBatchStepExecuteAdapter(MethodVisitor methodVisitor, int access, String name, String descriptor) {
        super(Opcodes.ASM9, methodVisitor, access, name, descriptor);
    }

    @Override
    protected void onMethodExit(int opcode) {
        // 예외 종료 경로는 다루지 않는다(상위 계측 frame으로 전파될 때만 기존 규칙으로 수집).
        if (opcode == ATHROW) {
            return;
        }
        loadArg(0);
        visitMethodInsn(INVOKESTATIC, BRIDGE_OWNER, "onBatchStepReturned", ON_STEP_RETURNED_DESC, false);
    }
}

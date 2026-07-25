package com.turtlepick.agent.core.instrument;

import com.turtlepick.agent.core.sql.SqlCaptureBridge;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

final class JdbcDataSourceGetConnectionAdapter extends AdviceAdapter {

    private static final String BRIDGE_OWNER =
            Type.getInternalName(SqlCaptureBridge.class);
    private static final String WRAP_CONNECTION_DESC =
            Type.getMethodDescriptor(
                    Type.getType(java.sql.Connection.class),
                    Type.getType(java.sql.Connection.class)
            );

    JdbcDataSourceGetConnectionAdapter(
            MethodVisitor methodVisitor,
            int access,
            String name,
            String descriptor) {
        super(Opcodes.ASM9, methodVisitor, access, name, descriptor);
    }

    @Override
    protected void onMethodExit(int opcode) {
        if (opcode == ARETURN) {
            visitMethodInsn(INVOKESTATIC, BRIDGE_OWNER, "wrapConnection",
                    WRAP_CONNECTION_DESC, false);
        }
    }
}

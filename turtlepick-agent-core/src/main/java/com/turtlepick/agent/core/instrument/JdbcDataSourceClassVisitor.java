package com.turtlepick.agent.core.instrument;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

final class JdbcDataSourceClassVisitor extends ClassVisitor {

    private static final String GET_CONNECTION = "getConnection";
    private static final String GET_CONNECTION_DESC = "()Ljava/sql/Connection;";
    private static final String GET_CONNECTION_WITH_AUTH_DESC =
            "(Ljava/lang/String;Ljava/lang/String;)Ljava/sql/Connection;";

    JdbcDataSourceClassVisitor(ClassVisitor classVisitor) {
        super(Opcodes.ASM9, classVisitor);
    }

    @Override
    public MethodVisitor visitMethod(
            int access,
            String name,
            String descriptor,
            String signature,
            String[] exceptions) {

        MethodVisitor methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
        if (GET_CONNECTION.equals(name)
                && (GET_CONNECTION_DESC.equals(descriptor)
                || GET_CONNECTION_WITH_AUTH_DESC.equals(descriptor))) {
            return new JdbcDataSourceGetConnectionAdapter(methodVisitor, access, name, descriptor);
        }
        return methodVisitor;
    }
}

package com.turtlepick.agent.core.instrument;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

final class TomcatFilterChainClassVisitor extends ClassVisitor {

    private static final String JAVAX_FILTER_DESC =
            "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;)V";
    private static final String JAKARTA_FILTER_DESC =
            "(Ljakarta/servlet/ServletRequest;Ljakarta/servlet/ServletResponse;)V";

    TomcatFilterChainClassVisitor(ClassVisitor cv) {
        super(Opcodes.ASM9, cv);
    }

    @Override
    public MethodVisitor visitMethod(
            int access,
            String name,
            String descriptor,
            String signature,
            String[] exceptions) {

        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        if ("doFilter".equals(name)
                && (JAVAX_FILTER_DESC.equals(descriptor) || JAKARTA_FILTER_DESC.equals(descriptor))) {
            return new TomcatFilterChainDoFilterAdapter(mv, access, name, descriptor);
        }
        return mv;
    }
}

package com.turtlepick.agent.core.instrument;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class DispatcherServletClassVisitor extends ClassVisitor {

    private static final String JAVAX_DISPATCH_DESC =
            "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)V";
    private static final String JAKARTA_DISPATCH_DESC =
            "(Ljakarta/servlet/http/HttpServletRequest;Ljakarta/servlet/http/HttpServletResponse;)V";

    public DispatcherServletClassVisitor(ClassVisitor classVisitor) {
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
        if ("doDispatch".equals(name)
                && (JAVAX_DISPATCH_DESC.equals(descriptor) || JAKARTA_DISPATCH_DESC.equals(descriptor))) {
            return new DispatcherServletDoDispatchAdapter(methodVisitor, access, name, descriptor);
        }
        return methodVisitor;
    }
}

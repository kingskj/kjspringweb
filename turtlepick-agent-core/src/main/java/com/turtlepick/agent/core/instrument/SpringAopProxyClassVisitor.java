package com.turtlepick.agent.core.instrument;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

final class SpringAopProxyClassVisitor extends ClassVisitor {

    private final String targetMethod;
    private final String targetDescriptor;

    SpringAopProxyClassVisitor(ClassVisitor cv, String targetMethod, String targetDescriptor) {
        super(Opcodes.ASM9, cv);
        this.targetMethod = targetMethod;
        this.targetDescriptor = targetDescriptor;
    }

    @Override
    public MethodVisitor visitMethod(
            int access,
            String name,
            String descriptor,
            String signature,
            String[] exceptions) {

        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        if (targetMethod.equals(name) && targetDescriptor.equals(descriptor)) {
            return new SpringAopProxyInvokeAdapter(mv, access, name, descriptor);
        }
        return mv;
    }
}

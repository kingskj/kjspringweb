package com.turtlepick.agent.core.instrument;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

final class SpringAopProxyClassVisitor extends ClassVisitor {

    private final String ownerClassName;
    private final String targetMethod;
    private final String targetDescriptor;
    private final boolean reorderAgentCatchAll;

    SpringAopProxyClassVisitor(ClassVisitor cv, String targetMethod, String targetDescriptor) {
        this(cv, null, targetMethod, targetDescriptor, false);
    }

    SpringAopProxyClassVisitor(
            ClassVisitor cv,
            String ownerClassName,
            String targetMethod,
            String targetDescriptor,
            boolean reorderAgentCatchAll) {
        super(Opcodes.ASM9, cv);
        this.ownerClassName = ownerClassName == null ? "unknown" : ownerClassName.replace('/', '.');
        this.targetMethod = targetMethod;
        this.targetDescriptor = targetDescriptor;
        this.reorderAgentCatchAll = reorderAgentCatchAll;
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
            Label handlerLabel = new Label();
            MethodVisitor targetMethodVisitor = reorderAgentCatchAll
                    ? new AgentCatchAllReorderingMethodNode(
                            Opcodes.ASM9,
                            access,
                            name,
                            descriptor,
                            signature,
                            exceptions,
                            mv,
                            ownerClassName,
                            handlerLabel)
                    : mv;
            return new SpringAopProxyInvokeAdapter(targetMethodVisitor, access, name, descriptor, handlerLabel);
        }
        return mv;
    }
}

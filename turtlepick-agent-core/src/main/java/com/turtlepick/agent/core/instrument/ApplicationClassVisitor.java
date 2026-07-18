package com.turtlepick.agent.core.instrument;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public final class ApplicationClassVisitor extends ClassVisitor {

    private final MethodProbeIndex probeIndex;
    private final String className;
    private final boolean reorderAgentCatchAll;

    public ApplicationClassVisitor(ClassVisitor classVisitor, MethodProbeIndex probeIndex, String className) {
        this(classVisitor, probeIndex, className, false);
    }

    public ApplicationClassVisitor(
            ClassVisitor classVisitor,
            MethodProbeIndex probeIndex,
            String className,
            boolean reorderAgentCatchAll) {
        super(Opcodes.ASM9, classVisitor);
        this.probeIndex = probeIndex;
        this.className = className;
        this.reorderAgentCatchAll = reorderAgentCatchAll;
    }

    @Override
    public MethodVisitor visitMethod(
            int access,
            String name,
            String descriptor,
            String signature,
            String[] exceptions) {

        MethodVisitor methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);

        if ((access & Opcodes.ACC_NATIVE) != 0) {
            return methodVisitor;
        }
        if ((access & Opcodes.ACC_ABSTRACT) != 0) {
            return methodVisitor;
        }
        if ("<init>".equals(name) || "<clinit>".equals(name)) {
            return methodVisitor;
        }

        MethodProbeSpec spec = probeIndex.find(className, name, Type.getArgumentTypes(descriptor), Type.getReturnType(descriptor));
        if (spec == null) {
            return methodVisitor;
        }

        Label handlerLabel = new Label();
        MethodVisitor targetMethodVisitor = reorderAgentCatchAll
                ? new AgentCatchAllReorderingMethodNode(
                        Opcodes.ASM9,
                        access,
                        name,
                        descriptor,
                        signature,
                        exceptions,
                        methodVisitor,
                        className,
                        handlerLabel)
                : methodVisitor;

        return new MethodProbeAdviceAdapter(
                targetMethodVisitor,
                access,
                name,
                descriptor,
                spec.getMethodId(),
                spec.getFqcnMethod(),
                handlerLabel
        );
    }
}

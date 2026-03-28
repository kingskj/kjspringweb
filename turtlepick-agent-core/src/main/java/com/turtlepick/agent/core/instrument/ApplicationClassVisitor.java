package com.turtlepick.agent.core.instrument;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public final class ApplicationClassVisitor extends ClassVisitor {

    private final MethodProbeIndex probeIndex;
    private final String className;

    public ApplicationClassVisitor(ClassVisitor classVisitor, MethodProbeIndex probeIndex, String className) {
        super(Opcodes.ASM9, classVisitor);
        this.probeIndex = probeIndex;
        this.className = className;
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

        MethodProbeSpec spec = probeIndex.find(className, name, Type.getArgumentTypes(descriptor));
        if (spec == null) {
            return methodVisitor;
        }

        return new MethodProbeAdviceAdapter(
                methodVisitor,
                access,
                name,
                descriptor,
                spec.getMethodId(),
                spec.getFqcnMethod()
        );
    }
}

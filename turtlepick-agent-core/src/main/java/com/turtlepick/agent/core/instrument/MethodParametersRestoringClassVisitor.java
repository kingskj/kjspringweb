package com.turtlepick.agent.core.instrument;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypePath;

import java.util.List;

final class MethodParametersRestoringClassVisitor extends ClassVisitor {

    private final MethodParametersSnapshot snapshot;

    MethodParametersRestoringClassVisitor(ClassVisitor classVisitor, MethodParametersSnapshot snapshot) {
        super(Opcodes.ASM9, classVisitor);
        this.snapshot = snapshot == null ? MethodParametersSnapshot.EMPTY : snapshot;
    }

    @Override
    public MethodVisitor visitMethod(
            int access,
            String name,
            String descriptor,
            String signature,
            String[] exceptions) {

        MethodVisitor methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
        List<MethodParametersSnapshot.ParameterInfo> parameters = snapshot.find(name, descriptor);
        if (methodVisitor == null || parameters.isEmpty()) {
            return methodVisitor;
        }
        return new RestoringMethodVisitor(methodVisitor, parameters);
    }

    private static final class RestoringMethodVisitor extends MethodVisitor {

        private final List<MethodParametersSnapshot.ParameterInfo> fallbackParameters;
        private boolean sawParameter;
        private boolean flushed;

        private RestoringMethodVisitor(MethodVisitor methodVisitor, List<MethodParametersSnapshot.ParameterInfo> fallbackParameters) {
            super(Opcodes.ASM9, methodVisitor);
            this.fallbackParameters = fallbackParameters;
        }

        @Override
        public void visitParameter(String name, int access) {
            sawParameter = true;
            super.visitParameter(name, access);
        }

        @Override
        public AnnotationVisitor visitAnnotationDefault() {
            flushParametersIfNeeded();
            return super.visitAnnotationDefault();
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            flushParametersIfNeeded();
            return super.visitAnnotation(descriptor, visible);
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
            flushParametersIfNeeded();
            return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
        }

        @Override
        public void visitAnnotableParameterCount(int parameterCount, boolean visible) {
            flushParametersIfNeeded();
            super.visitAnnotableParameterCount(parameterCount, visible);
        }

        @Override
        public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
            flushParametersIfNeeded();
            return super.visitParameterAnnotation(parameter, descriptor, visible);
        }

        @Override
        public void visitAttribute(Attribute attribute) {
            flushParametersIfNeeded();
            super.visitAttribute(attribute);
        }

        @Override
        public void visitCode() {
            flushParametersIfNeeded();
            super.visitCode();
        }

        @Override
        public void visitEnd() {
            flushParametersIfNeeded();
            super.visitEnd();
        }

        private void flushParametersIfNeeded() {
            if (flushed) {
                return;
            }
            flushed = true;
            if (sawParameter) {
                return;
            }
            for (MethodParametersSnapshot.ParameterInfo parameter : fallbackParameters) {
                super.visitParameter(parameter.getName(), parameter.getAccess());
            }
        }
    }
}

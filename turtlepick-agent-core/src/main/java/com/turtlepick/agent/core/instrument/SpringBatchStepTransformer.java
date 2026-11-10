package com.turtlepick.agent.core.instrument;

import com.turtlepick.agent.core.util.AgentLog;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

// Spring Batch가 step 예외를 처리해 FAILED로 끝내면 계측된 앱 메서드는 정상 반환한다.
// AbstractStep.execute의 정상 반환 시점에 최종 상태를 확인해 trace에 실패를 남긴다.
public final class SpringBatchStepTransformer implements ClassFileTransformer {

    private static final String ABSTRACT_STEP_CLASS =
            "org/springframework/batch/core/step/AbstractStep";
    private static final String EXECUTE_METHOD = "execute";
    // spring-batch-core 6.0.2에서 확인한 descriptor만 지원한다.
    private static final String[] EXECUTE_DESCRIPTORS = new String[]{
            "(Lorg/springframework/batch/core/step/StepExecution;)V"
    };

    @Override
    public byte[] transform(
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] classfileBuffer) {

        if (loader == null || !ABSTRACT_STEP_CLASS.equals(className)) {
            return null;
        }

        try {
            ClassReader reader = new ClassReader(classfileBuffer);
            ClassWriter writer = new SafeClassWriter(reader, loader);
            StepClassVisitor visitor = new StepClassVisitor(writer);
            reader.accept(visitor, ClassReader.EXPAND_FRAMES);
            if (!visitor.transformed) {
                AgentLog.warn("spring batch step hook unsupported target=" + className
                        + " reason=DESCRIPTOR_NOT_MATCHED");
                return null;
            }
            byte[] transformed = writer.toByteArray();
            TransformedClassDumper.dump("post-reorder", className, transformed);
            AgentLog.info("spring batch step hook installed target=" + className);
            return transformed;
        } catch (Throwable t) {
            if (t instanceof VirtualMachineError || t instanceof ThreadDeath) {
                throw (Error) t;
            }
            AgentLog.warn("spring batch step transform skipped className=" + className
                    + " cause=" + t.getClass().getSimpleName() + ":" + safeMessage(t));
            return null;
        }
    }

    private static boolean isSupportedDescriptor(String descriptor) {
        for (int i = 0; i < EXECUTE_DESCRIPTORS.length; i++) {
            if (EXECUTE_DESCRIPTORS[i].equals(descriptor)) {
                return true;
            }
        }
        return false;
    }

    private static String safeMessage(Throwable throwable) {
        return throwable.getMessage() == null ? "" : throwable.getMessage();
    }

    private static final class StepClassVisitor extends ClassVisitor {

        private boolean transformed;

        private StepClassVisitor(ClassVisitor cv) {
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
            if (EXECUTE_METHOD.equals(name) && isSupportedDescriptor(descriptor)) {
                transformed = true;
                return new SpringBatchStepExecuteAdapter(mv, access, name, descriptor);
            }
            return mv;
        }
    }

    private static final class SafeClassWriter extends ClassWriter {

        private final ClassLoader loader;

        private SafeClassWriter(ClassReader classReader, ClassLoader loader) {
            super(classReader, ClassWriter.COMPUTE_FRAMES);
            this.loader = loader;
        }

        @Override
        protected String getCommonSuperClass(String type1, String type2) {
            if (type1 == null || type2 == null) {
                return "java/lang/Object";
            }
            if (type1.equals(type2)) {
                return type1;
            }
            try {
                Class<?> left = loadClass(type1);
                Class<?> right = loadClass(type2);
                if (left.isAssignableFrom(right)) {
                    return type1;
                }
                if (right.isAssignableFrom(left)) {
                    return type2;
                }
                if (left.isInterface() || right.isInterface()) {
                    return "java/lang/Object";
                }
                Class<?> candidate = left;
                while (candidate != null && !candidate.isAssignableFrom(right)) {
                    candidate = candidate.getSuperclass();
                }
                return candidate == null ? "java/lang/Object" : candidate.getName().replace('.', '/');
            } catch (Throwable ignored) {
                return "java/lang/Object";
            }
        }

        private Class<?> loadClass(String internalName) throws ClassNotFoundException {
            String className = internalName.replace('/', '.');
            ClassLoader[] candidates = new ClassLoader[]{
                    loader,
                    Thread.currentThread().getContextClassLoader(),
                    SafeClassWriter.class.getClassLoader(),
                    ClassLoader.getSystemClassLoader()
            };
            for (ClassLoader candidate : candidates) {
                if (candidate == null) {
                    continue;
                }
                try {
                    return Class.forName(className, false, candidate);
                } catch (ClassNotFoundException ignored) {
                    // try next candidate
                }
            }
            return Class.forName(className, false, null);
        }
    }
}

package com.turtlepick.agent.core.instrument;

import com.turtlepick.agent.core.util.AgentLog;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public final class MyBatisMapperProxyTransformer implements ClassFileTransformer {

    private static final String MAPPER_PROXY_CLASS =
            "org/apache/ibatis/binding/MapperProxy";
    private static final String MAPPER_PROXY_METHOD = "invoke";
    private static final String MAPPER_PROXY_DESC =
            "(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;";

    @Override
    public byte[] transform(
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] classfileBuffer) {

        if (loader == null || className == null) {
            return null;
        }
        if (!MAPPER_PROXY_CLASS.equals(className)) {
            return null;
        }

        try {
            if (TransformedClassDumper.isEnabled()) {
                TransformedClassDumper.dump("pre-reorder", className,
                        transformClass(loader, className, classfileBuffer, false));
            }
            byte[] transformed = transformClass(loader, className, classfileBuffer, true);
            TransformedClassDumper.dump("post-reorder", className, transformed);
            AgentLog.info("mybatis mapper proxy hook installed target=" + className);
            return transformed;
        } catch (Throwable t) {
            if (t instanceof VirtualMachineError || t instanceof ThreadDeath) {
                throw (Error) t;
            }
            AgentLog.warn("mybatis mapper proxy transform skipped className=" + className
                    + " cause=" + t.getClass().getSimpleName() + ":" + safeMessage(t));
            return null;
        }
    }

    private byte[] transformClass(
            ClassLoader loader,
            String className,
            byte[] classfileBuffer,
            boolean reorderAgentCatchAll) {

        ClassReader reader = new ClassReader(classfileBuffer);
        ClassWriter writer = new SafeClassWriter(reader, loader);
        ClassVisitor visitor = new MyBatisMapperProxyClassVisitor(
                writer,
                className,
                MAPPER_PROXY_METHOD,
                MAPPER_PROXY_DESC,
                reorderAgentCatchAll
        );
        reader.accept(visitor, ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

    private static String safeMessage(Throwable throwable) {
        return throwable.getMessage() == null ? "" : throwable.getMessage();
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

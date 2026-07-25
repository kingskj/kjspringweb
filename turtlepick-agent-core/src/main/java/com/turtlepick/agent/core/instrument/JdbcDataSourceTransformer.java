package com.turtlepick.agent.core.instrument;

import com.turtlepick.agent.core.util.AgentLog;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public final class JdbcDataSourceTransformer implements ClassFileTransformer {

    private static final String HIKARI_DATASOURCE =
            "com/zaxxer/hikari/HikariDataSource";
    private static final String SPRING_ABSTRACT_DRIVER_DATASOURCE =
            "org/springframework/jdbc/datasource/AbstractDriverBasedDataSource";

    @Override
    public byte[] transform(
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] classfileBuffer) {

        if (loader == null || className == null || !isTarget(className)) {
            return null;
        }

        try {
            byte[] transformed = transformClass(loader, className, classfileBuffer);
            TransformedClassDumper.dump("sql-datasource", className, transformed);
            AgentLog.info("sql datasource hook installed target=" + className);
            return transformed;
        } catch (Throwable t) {
            if (t instanceof VirtualMachineError || t instanceof ThreadDeath) {
                throw (Error) t;
            }
            AgentLog.warn("sql datasource transform skipped className=" + className
                    + " cause=" + t.getClass().getSimpleName() + ":" + safeMessage(t));
            return null;
        }
    }

    private boolean isTarget(String className) {
        return HIKARI_DATASOURCE.equals(className)
                || SPRING_ABSTRACT_DRIVER_DATASOURCE.equals(className);
    }

    private byte[] transformClass(ClassLoader loader, String className, byte[] classfileBuffer) {
        ClassReader reader = new ClassReader(classfileBuffer);
        ClassWriter writer = new SafeClassWriter(reader, loader);
        ClassVisitor visitor = new JdbcDataSourceClassVisitor(writer);
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
            for (int i = 0; i < candidates.length; i++) {
                ClassLoader candidate = candidates[i];
                if (candidate == null) {
                    continue;
                }
                try {
                    return Class.forName(className, false, candidate);
                } catch (ClassNotFoundException ignored) {
                }
            }
            return Class.forName(className, false, null);
        }
    }
}

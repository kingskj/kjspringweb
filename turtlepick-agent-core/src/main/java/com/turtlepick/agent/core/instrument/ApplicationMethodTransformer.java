package com.turtlepick.agent.core.instrument;

import com.turtlepick.agent.core.util.AgentLog;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public final class ApplicationMethodTransformer implements ClassFileTransformer {

    private volatile MethodProbeIndex probeIndex;

    public ApplicationMethodTransformer(MethodProbeIndex probeIndex) {
        this.probeIndex = probeIndex == null ? MethodProbeIndex.empty() : probeIndex;
    }

    public MethodProbeIndex updateIndex(MethodProbeIndex nextProbeIndex) {
        MethodProbeIndex previous = probeIndex;
        probeIndex = nextProbeIndex == null ? MethodProbeIndex.empty() : nextProbeIndex;
        return previous == null ? MethodProbeIndex.empty() : previous;
    }

    @Override
    public byte[] transform(
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] classfileBuffer) {

        if (loader == null) {
            return null;
        }
        if (className == null) {
            return null;
        }

        MethodProbeIndex index = probeIndex;
        String fqcn = className.replace('/', '.');
        if (!index.containsClass(fqcn)) {
            return null;
        }

        MethodParametersSnapshot methodParametersSnapshot = MethodParametersSnapshot.EMPTY;
        if (classBeingRedefined != null) {
            methodParametersSnapshot = loadOriginalMethodParameters(loader, className);
        }

        try {
            if (classBeingRedefined != null && TransformedClassDumper.isEnabled()) {
                TransformedClassDumper.dump("raw-retransform-input", className, classfileBuffer);
            }
            if (TransformedClassDumper.isEnabled()) {
                TransformedClassDumper.dump("pre-reorder", className,
                        transformClass(loader, classfileBuffer, index, fqcn, false, methodParametersSnapshot));
            }
            byte[] transformed = transformClass(loader, classfileBuffer, index, fqcn, true, methodParametersSnapshot);
            TransformedClassDumper.dump("post-reorder", className, transformed);
            return transformed;
        } catch (Throwable t) {
            if (t instanceof VirtualMachineError || t instanceof ThreadDeath) {
                throw (Error) t;
            }
            AgentLog.warn("method probe transform skipped className=" + fqcn
                    + " cause=" + t.getClass().getSimpleName() + ":" + safeMessage(t));
            return null;
        }
    }

    private byte[] transformClass(
            ClassLoader loader,
            byte[] classfileBuffer,
            MethodProbeIndex index,
            String fqcn,
            boolean reorderAgentCatchAll,
            MethodParametersSnapshot methodParametersSnapshot) {

        ClassReader reader = new ClassReader(classfileBuffer);
        ClassWriter writer = new SafeClassWriter(reader, loader);
        ClassVisitor output = writer;
        if (methodParametersSnapshot != null && !methodParametersSnapshot.isEmpty()) {
            output = new MethodParametersRestoringClassVisitor(output, methodParametersSnapshot);
        }
        ClassVisitor visitor = new ApplicationClassVisitor(output, index, fqcn, reorderAgentCatchAll);
        reader.accept(visitor, ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

    private MethodParametersSnapshot loadOriginalMethodParameters(ClassLoader loader, String className) {
        String resourceName = className + ".class";
        InputStream input = null;
        try {
            input = loader.getResourceAsStream(resourceName);
            if (input == null) {
                AgentLog.warn("method parameters restore skipped cause=RESOURCE_NOT_FOUND className=" + className);
                return MethodParametersSnapshot.EMPTY;
            }
            return MethodParametersSnapshot.extract(readAllBytes(input));
        } catch (Throwable t) {
            if (t instanceof VirtualMachineError || t instanceof ThreadDeath) {
                throw (Error) t;
            }
            AgentLog.warn("method parameters restore skipped className=" + className
                    + " cause=" + t.getClass().getSimpleName() + ":" + safeMessage(t));
            return MethodParametersSnapshot.EMPTY;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ignored) {
                    // ignore close failure
                }
            }
        }
    }

    private static byte[] readAllBytes(InputStream input) throws IOException {
        byte[] buffer = new byte[8192];
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
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

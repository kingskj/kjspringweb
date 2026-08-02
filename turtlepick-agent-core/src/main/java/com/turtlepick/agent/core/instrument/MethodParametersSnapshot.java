package com.turtlepick.agent.core.instrument;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class MethodParametersSnapshot {

    static final MethodParametersSnapshot EMPTY = new MethodParametersSnapshot(Collections.<MethodKey, List<ParameterInfo>>emptyMap());

    private final Map<MethodKey, List<ParameterInfo>> parametersByMethod;

    private MethodParametersSnapshot(Map<MethodKey, List<ParameterInfo>> parametersByMethod) {
        this.parametersByMethod = parametersByMethod;
    }

    static MethodParametersSnapshot extract(byte[] classBytes) {
        if (classBytes == null || classBytes.length == 0) {
            return EMPTY;
        }

        final Map<MethodKey, List<ParameterInfo>> captured = new HashMap<>();
        ClassReader reader = new ClassReader(classBytes);
        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(
                    int access,
                    final String name,
                    final String descriptor,
                    String signature,
                    String[] exceptions) {

                final List<ParameterInfo> parameters = new ArrayList<>();
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitParameter(String name, int access) {
                        parameters.add(new ParameterInfo(name, access));
                    }

                    @Override
                    public void visitEnd() {
                        if (!parameters.isEmpty()) {
                            MethodKey key = new MethodKey(name, descriptor);
                            captured.put(key, Collections.unmodifiableList(new ArrayList<>(parameters)));
                        }
                    }
                };
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);

        if (captured.isEmpty()) {
            return EMPTY;
        }
        return new MethodParametersSnapshot(Collections.unmodifiableMap(new HashMap<>(captured)));
    }

    boolean isEmpty() {
        return parametersByMethod.isEmpty();
    }

    List<ParameterInfo> find(String name, String descriptor) {
        List<ParameterInfo> parameters = parametersByMethod.get(new MethodKey(name, descriptor));
        return parameters == null ? Collections.<ParameterInfo>emptyList() : parameters;
    }

    static final class ParameterInfo {

        private final String name;
        private final int access;

        private ParameterInfo(String name, int access) {
            this.name = name;
            this.access = access;
        }

        String getName() {
            return name;
        }

        int getAccess() {
            return access;
        }
    }

    private static final class MethodKey {

        private final String name;
        private final String descriptor;

        private MethodKey(String name, String descriptor) {
            this.name = name;
            this.descriptor = descriptor;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof MethodKey)) {
                return false;
            }
            MethodKey that = (MethodKey) other;
            return equalsNullable(name, that.name) && equalsNullable(descriptor, that.descriptor);
        }

        @Override
        public int hashCode() {
            int result = name == null ? 0 : name.hashCode();
            result = 31 * result + (descriptor == null ? 0 : descriptor.hashCode());
            return result;
        }

        private static boolean equalsNullable(Object left, Object right) {
            return left == null ? right == null : left.equals(right);
        }
    }
}

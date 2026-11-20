/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.apim.infra.template;

import freemarker.ext.beans.ClassMemberAccessPolicy;
import freemarker.ext.beans.MemberAccessPolicy;
import freemarker.template.utility.ClassUtil;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Member access policy of the FreeMarker object wrapper used to render user-written templates.
 *
 * <p>Fields, constructors, static methods and setters are never exposed, and only {@code equals} / {@code hashCode} are exposed
 * from {@link Object}.
 * On top of that, two deny-lists apply:</p>
 * <ul>
 *     <li><b>Unsafe methods</b> ({@value #UNSAFE_METHODS_PROPERTIES}): individual methods that are never exposed.</li>
 *     <li><b>Unsafe classes</b> ({@value #UNSAFE_CLASSES_PROPERTIES}): types (and every subtype / implementation, and arrays of
 *     them) or packages for which no member at all is exposed, and that no exposed method may return.</li>
 * </ul>
 */
public final class SecureMemberAccessPolicy implements MemberAccessPolicy {

    public static final SecureMemberAccessPolicy INSTANCE = new SecureMemberAccessPolicy();

    static final String UNSAFE_METHODS_PROPERTIES = "extendedUnsafeMethods.properties";
    static final String UNSAFE_CLASSES_PROPERTIES = "extendedUnsafeClasses.properties";

    private static final Set<Method> UNSAFE_METHODS = loadUnsafeMethods();
    private static final Set<String> UNSAFE_CLASS_NAMES;
    private static final List<String> UNSAFE_PACKAGE_PREFIXES;

    static {
        Set<String> classNames = new HashSet<>();
        Set<String> packagePrefixes = new HashSet<>();
        for (String entry : loadProperties(UNSAFE_CLASSES_PROPERTIES).stringPropertyNames()) {
            String trimmed = entry.trim();
            if (trimmed.endsWith(".*")) {
                packagePrefixes.add(trimmed.substring(0, trimmed.length() - 1));
            } else if (!trimmed.isEmpty()) {
                classNames.add(trimmed);
            }
        }
        UNSAFE_CLASS_NAMES = Set.copyOf(classNames);
        UNSAFE_PACKAGE_PREFIXES = List.copyOf(packagePrefixes);
    }

    private static final ClassMemberAccessPolicy SAFE_CLASS_POLICY = new SafeClassMemberAccessPolicy();
    private static final ClassMemberAccessPolicy DENY_ALL_CLASS_POLICY = new DenyAllClassMemberAccessPolicy();

    private SecureMemberAccessPolicy() {}

    @Override
    public ClassMemberAccessPolicy forClass(Class<?> containingClass) {
        return isUnsafeClass(containingClass) ? DENY_ALL_CLASS_POLICY : SAFE_CLASS_POLICY;
    }

    @Override
    public boolean isToStringAlwaysExposed() {
        return true;
    }

    /**
     * Names are compared rather than {@link Class} instances, so that the check works whatever the class loader of the type, and
     * without loading optional types.
     */
    static boolean isUnsafeClass(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }
        if (clazz.isArray()) {
            return isUnsafeClass(clazz.getComponentType());
        }
        Set<Class<?>> visited = new HashSet<>();
        Deque<Class<?>> toVisit = new ArrayDeque<>();
        toVisit.push(clazz);
        while (!toVisit.isEmpty()) {
            Class<?> current = toVisit.pop();
            if (!visited.add(current)) {
                continue;
            }
            if (isUnsafeName(current.getName())) {
                return true;
            }
            if (current.getSuperclass() != null) {
                toVisit.push(current.getSuperclass());
            }
            for (Class<?> itf : current.getInterfaces()) {
                toVisit.push(itf);
            }
        }
        return false;
    }

    static boolean isUnsafeMethod(Method method) {
        return UNSAFE_METHODS.contains(method) || isUnsafeClass(method.getDeclaringClass()) || isUnsafeClass(method.getReturnType());
    }

    private static boolean isSetter(Method method) {
        String name = method.getName();
        return method.getParameterCount() == 1 && name.length() > 3 && name.startsWith("set") && Character.isUpperCase(name.charAt(3));
    }

    private static boolean isUnsafeName(String className) {
        return UNSAFE_CLASS_NAMES.contains(className) || UNSAFE_PACKAGE_PREFIXES.stream().anyMatch(className::startsWith);
    }

    private static Set<Method> loadUnsafeMethods() {
        Set<Method> methods = new HashSet<>();
        for (String methodSpec : loadProperties(UNSAFE_METHODS_PROPERTIES).stringPropertyNames()) {
            try {
                methods.add(parseMethodSpec(methodSpec));
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                // Method removed from the running JDK: nothing to deny. Typos are caught by SecureMemberAccessPolicyTest.
            }
        }
        return Set.copyOf(methods);
    }

    private static Properties loadProperties(String resource) {
        try {
            return ClassUtil.loadProperties(SecureMemberAccessPolicy.class, resource);
        } catch (Exception e) {
            throw new IllegalStateException("Could not load FreeMarker sandbox deny-list " + resource, e);
        }
    }

    static Method parseMethodSpec(String methodSpec) throws ClassNotFoundException, NoSuchMethodException {
        int brace = methodSpec.indexOf('(');
        int dot = methodSpec.lastIndexOf('.', brace);
        Class<?> clazz = ClassUtil.forName(methodSpec.substring(0, dot));
        String methodName = methodSpec.substring(dot + 1, brace);
        StringTokenizer tokenizer = new StringTokenizer(methodSpec.substring(brace + 1, methodSpec.length() - 1), ",");
        Class<?>[] argTypes = new Class[tokenizer.countTokens()];
        for (int i = 0; i < argTypes.length; i++) {
            String argClassName = tokenizer.nextToken();
            argTypes[i] = ClassUtil.resolveIfPrimitiveTypeName(argClassName);
            if (argTypes[i] == null) {
                argTypes[i] = ClassUtil.forName(argClassName);
            }
        }
        return clazz.getMethod(methodName, argTypes);
    }

    private static final class SafeClassMemberAccessPolicy implements ClassMemberAccessPolicy {

        @Override
        public boolean isMethodExposed(Method method) {
            if (method.getDeclaringClass() == Object.class) {
                return "equals".equals(method.getName()) || "hashCode".equals(method.getName());
            }
            return !Modifier.isStatic(method.getModifiers()) && !isSetter(method) && !isUnsafeMethod(method);
        }

        @Override
        public boolean isConstructorExposed(Constructor<?> constructor) {
            return false;
        }

        @Override
        public boolean isFieldExposed(Field field) {
            return false;
        }
    }

    private static final class DenyAllClassMemberAccessPolicy implements ClassMemberAccessPolicy {

        @Override
        public boolean isMethodExposed(Method method) {
            return false;
        }

        @Override
        public boolean isConstructorExposed(Constructor<?> constructor) {
            return false;
        }

        @Override
        public boolean isFieldExposed(Field field) {
            return false;
        }
    }
}

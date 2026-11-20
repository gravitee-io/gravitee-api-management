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

import static org.assertj.core.api.Assertions.assertThat;

import freemarker.ext.beans.ClassMemberAccessPolicy;
import freemarker.template.utility.ClassUtil;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SecureMemberAccessPolicyTest {

    private final SecureMemberAccessPolicy policy = SecureMemberAccessPolicy.INSTANCE;

    public static class Bean {

        public static String helper() {
            return "static";
        }

        public void setName(String name) {}

        public Bean withName(String name) {
            return this;
        }

        public String publicField = "value";

        public Bean() {}

        public String getName() {
            return "name";
        }
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "java.lang.Class",
            "java.lang.ClassLoader",
            "java.lang.Module",
            "java.lang.ModuleLayer",
            "java.lang.Runtime",
            "java.lang.System",
            "java.lang.Process",
            "java.lang.ProcessBuilder",
            "java.lang.ProcessHandle",
            "java.lang.Thread",
            "java.lang.reflect.Field",
            "java.lang.reflect.Method",
            "java.lang.reflect.Constructor",
            "java.lang.invoke.MethodHandles",
            "java.lang.invoke.MethodHandles$Lookup",
            "sun.misc.Unsafe",
            "java.io.File",
            "java.nio.file.Files",
            "java.net.URL",
            "java.net.URLClassLoader",
            "javax.script.ScriptEngineManager",
            "freemarker.template.utility.Execute",
            "freemarker.template.Configuration",
            "org.springframework.context.ApplicationContext",
        }
    )
    void should_deny_every_member_of_unsafe_classes(String className) throws Exception {
        Class<?> clazz = Class.forName(className);
        assertThat(SecureMemberAccessPolicy.isUnsafeClass(clazz)).isTrue();

        ClassMemberAccessPolicy classPolicy = policy.forClass(clazz);
        for (Method method : clazz.getMethods()) {
            assertThat(classPolicy.isMethodExposed(method)).as(method.toString()).isFalse();
        }
        for (Field field : clazz.getFields()) {
            assertThat(classPolicy.isFieldExposed(field)).as(field.toString()).isFalse();
        }
        for (var constructor : clazz.getConstructors()) {
            assertThat(classPolicy.isConstructorExposed(constructor)).as(constructor.toString()).isFalse();
        }
    }

    @Test
    void should_deny_subclasses_and_implementations_of_unsafe_classes() {
        class MyLoader extends ClassLoader {}
        abstract class MyStream extends InputStream {}
        assertThat(SecureMemberAccessPolicy.isUnsafeClass(MyLoader.class)).isTrue();
        assertThat(SecureMemberAccessPolicy.isUnsafeClass(MyStream.class)).isTrue();
        assertThat(SecureMemberAccessPolicy.isUnsafeClass(Path.of("/").getClass())).isTrue();
        assertThat(SecureMemberAccessPolicy.isUnsafeClass(Class[].class)).isTrue();
        assertThat(SecureMemberAccessPolicy.isUnsafeClass(File[][].class)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(classes = { String.class, Integer.class, HashMap.class, ArrayList.class, List.class, TimeUnit.class, Object.class })
    void should_allow_regular_classes(Class<?> clazz) {
        assertThat(SecureMemberAccessPolicy.isUnsafeClass(clazz)).isFalse();
    }

    @Test
    void should_only_expose_equals_and_hash_code_from_object() throws Exception {
        ClassMemberAccessPolicy beanPolicy = policy.forClass(Bean.class);
        assertThat(beanPolicy.isMethodExposed(Object.class.getMethod("getClass"))).isFalse();
        assertThat(beanPolicy.isMethodExposed(Object.class.getMethod("notify"))).isFalse();
        assertThat(beanPolicy.isMethodExposed(Object.class.getMethod("wait"))).isFalse();
        assertThat(beanPolicy.isMethodExposed(Object.class.getMethod("equals", Object.class))).isTrue();
        assertThat(beanPolicy.isMethodExposed(Object.class.getMethod("hashCode"))).isTrue();
        assertThat(policy.isToStringAlwaysExposed()).isTrue();
    }

    @Test
    void should_never_expose_fields_or_constructors() throws Exception {
        ClassMemberAccessPolicy beanPolicy = policy.forClass(Bean.class);
        assertThat(beanPolicy.isFieldExposed(Bean.class.getField("publicField"))).isFalse();
        assertThat(beanPolicy.isConstructorExposed(Bean.class.getConstructor())).isFalse();
    }

    @Test
    void should_hide_static_methods() throws Exception {
        assertThat(policy.forClass(Bean.class).isMethodExposed(Bean.class.getMethod("helper"))).isFalse();
        assertThat(policy.forClass(String.class).isMethodExposed(String.class.getMethod("valueOf", Object.class))).isFalse();
    }

    @Test
    void should_hide_setters() throws Exception {
        assertThat(policy.forClass(Bean.class).isMethodExposed(Bean.class.getMethod("setName", String.class))).isFalse();
        assertThat(policy.forClass(Bean.class).isMethodExposed(Bean.class.getMethod("withName", String.class))).isTrue();
    }

    @Test
    void should_hide_methods_returning_unsafe_types() throws Exception {
        assertThat(policy.forClass(Enum.class).isMethodExposed(Enum.class.getMethod("getDeclaringClass"))).isFalse();
        assertThat(policy.forClass(URI.class).isMethodExposed(URI.class.getMethod("toURL"))).isFalse();
    }

    @Test
    void should_expose_regular_methods() throws Exception {
        ClassMemberAccessPolicy stringPolicy = policy.forClass(String.class);
        assertThat(stringPolicy.isMethodExposed(String.class.getMethod("toUpperCase"))).isTrue();
        assertThat(stringPolicy.isMethodExposed(String.class.getMethod("substring", int.class))).isTrue();
        assertThat(policy.forClass(HashMap.class).isMethodExposed(HashMap.class.getMethod("get", Object.class))).isTrue();
        assertThat(policy.forClass(Enum.class).isMethodExposed(Enum.class.getMethod("name"))).isTrue();
        assertThat(policy.forClass(Bean.class).isMethodExposed(Bean.class.getMethod("getName"))).isTrue();
    }

    @Test
    void should_deny_the_methods_of_known_escape_chains() throws Exception {
        assertThat(SecureMemberAccessPolicy.isUnsafeMethod(Class.class.getMethod("forName", Module.class, String.class))).isTrue();
        assertThat(SecureMemberAccessPolicy.isUnsafeMethod(Class.class.getMethod("getModule"))).isTrue();
        assertThat(SecureMemberAccessPolicy.isUnsafeMethod(Class.class.getMethod("getMethods"))).isTrue();
        assertThat(SecureMemberAccessPolicy.isUnsafeMethod(Method.class.getMethod("invoke", Object.class, Object[].class))).isTrue();
        assertThat(SecureMemberAccessPolicy.isUnsafeMethod(Field.class.getMethod("trySetAccessible"))).isTrue();
        assertThat(SecureMemberAccessPolicy.isUnsafeMethod(Field.class.getMethod("get", Object.class))).isTrue();
        assertThat(SecureMemberAccessPolicy.isUnsafeMethod(ProcessBuilder.class.getMethod("start"))).isTrue();
    }

    /**
     * Methods listed in the deny-list that have been removed from recent JDKs. They are tolerated when they do not resolve; any
     * other unresolvable entry is a mistake (typo, wrong signature) that would otherwise be silently ignored at runtime.
     */
    private static final Set<String> KNOWN_REMOVED_JDK_METHODS = Set.of(
        // Removed in JDK 11 or earlier
        "java.lang.Runtime.traceInstructions(boolean)",
        "java.lang.Runtime.traceMethodCalls(boolean)",
        "java.lang.System.runFinalizersOnExit(boolean)",
        "java.lang.Thread.destroy()",
        "java.lang.Thread.stop(java.lang.Throwable)",
        // Removed in JDK 21
        "java.lang.ThreadGroup.allowThreadSuspension(boolean)",
        // Removed in JDK 23+
        "java.lang.Thread.resume()",
        "java.lang.Thread.suspend()",
        "java.lang.ThreadGroup.resume()",
        "java.lang.ThreadGroup.stop()",
        "java.lang.ThreadGroup.suspend()"
    );

    /**
     * Classes that may not be on the classpath (preview APIs, gateway frameworks).
     */
    private static final Set<String> OPTIONAL_UNSAFE_CLASSES = Set.of(
        "java.lang.ScopedValue",
        "io.vertx.core.Vertx",
        "io.vertx.core.Context",
        "io.vertx.core.file.FileSystem",
        "io.vertx.rxjava3.core.Vertx",
        "io.vertx.rxjava3.core.file.FileSystem"
    );

    @Test
    void should_resolve_every_unsafe_method_entry() throws Exception {
        Properties props = ClassUtil.loadProperties(SecureMemberAccessPolicy.class, SecureMemberAccessPolicy.UNSAFE_METHODS_PROPERTIES);
        assertThat(props).isNotEmpty();

        List<String> unresolved = new ArrayList<>();
        for (Object key : props.keySet()) {
            String entry = (String) key;
            try {
                SecureMemberAccessPolicy.parseMethodSpec(entry);
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                if (!KNOWN_REMOVED_JDK_METHODS.contains(entry)) {
                    unresolved.add(entry + " (" + e + ")");
                }
            }
        }
        assertThat(unresolved).as("Unresolvable entries in %s", SecureMemberAccessPolicy.UNSAFE_METHODS_PROPERTIES).isEmpty();
    }

    @Test
    void should_only_tolerate_known_removed_jdk_methods_that_are_listed() throws Exception {
        Properties props = ClassUtil.loadProperties(SecureMemberAccessPolicy.class, SecureMemberAccessPolicy.UNSAFE_METHODS_PROPERTIES);
        assertThat(props.stringPropertyNames()).containsAll(KNOWN_REMOVED_JDK_METHODS);
    }

    @Test
    void should_list_each_unsafe_method_once() throws Exception {
        List<String> entries;
        try (var stream = SecureMemberAccessPolicy.class.getResourceAsStream(SecureMemberAccessPolicy.UNSAFE_METHODS_PROPERTIES)) {
            entries = new String(stream.readAllBytes(), StandardCharsets.UTF_8)
                .lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .toList();
        }
        assertThat(entries).doesNotHaveDuplicates();
    }

    @Test
    void should_resolve_every_unsafe_class_entry() throws Exception {
        Properties props = ClassUtil.loadProperties(SecureMemberAccessPolicy.class, SecureMemberAccessPolicy.UNSAFE_CLASSES_PROPERTIES);
        assertThat(props).isNotEmpty();

        List<String> invalid = new ArrayList<>();
        for (String entry : props.stringPropertyNames()) {
            if (entry.endsWith(".*")) {
                if (!entry.matches("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*\\.\\*")) {
                    invalid.add(entry + " (malformed package prefix)");
                }
                continue;
            }
            if (OPTIONAL_UNSAFE_CLASSES.contains(entry)) {
                continue;
            }
            try {
                Class.forName(entry, false, getClass().getClassLoader());
            } catch (ClassNotFoundException e) {
                invalid.add(entry + " (" + e + ")");
            }
        }
        assertThat(invalid).as("Invalid entries in %s", SecureMemberAccessPolicy.UNSAFE_CLASSES_PROPERTIES).isEmpty();
    }
}

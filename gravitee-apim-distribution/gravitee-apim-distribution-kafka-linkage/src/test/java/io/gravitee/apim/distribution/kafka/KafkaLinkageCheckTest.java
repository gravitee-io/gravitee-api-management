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
package io.gravitee.apim.distribution.kafka;

import static java.lang.classfile.ClassFile.ACC_PUBLIC;
import static java.lang.constant.ConstantDescs.CD_Object;
import static java.lang.constant.ConstantDescs.CD_String;
import static java.lang.constant.ConstantDescs.CD_void;
import static java.lang.constant.ConstantDescs.INIT_NAME;
import static java.lang.constant.ConstantDescs.MTD_void;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.classfile.ClassBuilder;
import java.lang.classfile.ClassFile;
import java.lang.classfile.CodeBuilder;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class KafkaLinkageCheckTest {

    private static final ClassDesc PRODUCER = ClassDesc.of("org.apache.kafka.clients.Producer");
    private static final ClassDesc BASE_PRODUCER = ClassDesc.of("org.apache.kafka.clients.BaseProducer");
    private static final ClassDesc CLOSEABLE = ClassDesc.of("org.apache.kafka.clients.Closeable");
    private static final ClassDesc KAFKA_EXCEPTION = ClassDesc.of("org.apache.kafka.common.KafkaException");
    private static final ClassDesc RECORD = ClassDesc.of("org.apache.kafka.common.record.Record");
    private static final ClassDesc PLUGIN = ClassDesc.of("io.gravitee.policy.Plugin");

    @TempDir
    Path distribution;

    @BeforeEach
    void gateway_ships_a_kafka_library() throws IOException {
        jar(
            distribution.resolve("lib/ext/kafka-clients.jar"),
            Map.of(
                PRODUCER,
                classFile(PRODUCER, BASE_PRODUCER, List.of(), cb -> {
                    cb.withMethod(INIT_NAME, MTD_void, ACC_PUBLIC, mb -> mb.withCode(CodeBuilder::return_));
                    cb.withMethod("send", MethodTypeDesc.of(CD_void, CD_String), ACC_PUBLIC, mb -> mb.withCode(CodeBuilder::return_));
                }),
                BASE_PRODUCER,
                classFile(BASE_PRODUCER, CD_Object, List.of(CLOSEABLE), cb ->
                    cb.withMethod("flush", MTD_void, ACC_PUBLIC, mb -> mb.withCode(CodeBuilder::return_))
                ),
                CLOSEABLE,
                classFile(CLOSEABLE, CD_Object, List.of(), cb ->
                    cb
                        .withFlags(ACC_PUBLIC | ClassFile.ACC_INTERFACE | ClassFile.ACC_ABSTRACT)
                        .withMethod("close", MTD_void, ACC_PUBLIC | ClassFile.ACC_ABSTRACT, mb -> {})
                ),
                KAFKA_EXCEPTION,
                classFile(KAFKA_EXCEPTION, ClassDesc.of("java.lang.RuntimeException"), List.of(), cb -> {})
            )
        );
    }

    @Test
    void should_pass_when_every_kafka_reference_resolves() throws IOException {
        plugin("policy-ok.zip", "policy-ok.jar", code -> {
            code.new_(PRODUCER).dup().invokespecial(PRODUCER, INIT_NAME, MTD_void);
            code.ldc("topic").invokevirtual(PRODUCER, "send", MethodTypeDesc.of(CD_void, CD_String));
        });

        assertThat(check()).isEmpty();
    }

    @Test
    void should_report_a_method_that_the_gateway_library_no_longer_has() throws IOException {
        plugin("policy-broken.zip", "policy-broken.jar", code ->
            code.aconst_null().ldc("group").invokevirtual(PRODUCER, "sendOffsets", MethodTypeDesc.of(CD_void, CD_String))
        );

        assertThat(check())
            .singleElement()
            .satisfies(u -> {
                assertThat(u.plugin()).isEqualTo("policy-broken.zip");
                assertThat(u.jar()).isEqualTo("policy-broken.jar");
                assertThat(u.reference()).isEqualTo("org/apache/kafka/clients/Producer.sendOffsets(Ljava/lang/String;)V");
                assertThat(u.reason()).isEqualTo("method-gone");
                assertThat(u.referencedFrom()).containsExactly("io/gravitee/policy/Plugin");
            });
    }

    @Test
    void should_report_a_constructor_whose_signature_changed() throws IOException {
        plugin("policy-ctor.zip", "policy-ctor.jar", code ->
            code.new_(PRODUCER).dup().ldc("x").invokespecial(PRODUCER, INIT_NAME, MethodTypeDesc.of(CD_void, CD_String))
        );

        assertThat(check())
            .singleElement()
            .satisfies(u -> assertThat(u.reason()).isEqualTo("constructor-signature-changed"));
    }

    @Test
    void should_report_a_class_that_the_gateway_library_no_longer_has() throws IOException {
        plugin("policy-record.zip", "policy-record.jar", code -> code.new_(RECORD).pop());

        assertThat(check())
            .singleElement()
            .satisfies(u -> {
                assertThat(u.reference()).isEqualTo("org/apache/kafka/common/record/Record");
                assertThat(u.reason()).isEqualTo("class-gone");
            });
    }

    @Test
    void should_resolve_members_inherited_from_superclasses_interfaces_and_the_jdk() throws IOException {
        plugin("policy-inherited.zip", "policy-inherited.jar", code -> {
            code.aconst_null().invokevirtual(PRODUCER, "flush", MTD_void);
            code.aconst_null().invokevirtual(PRODUCER, "close", MTD_void);
            code.aconst_null().invokevirtual(KAFKA_EXCEPTION, "getMessage", MethodTypeDesc.of(CD_String)).pop();
        });

        assertThat(check()).isEmpty();
    }

    @Test
    void should_check_the_jars_a_plugin_bundles_in_its_lib_directory() throws IOException {
        plugin("endpoint.zip", "lib/reactor-kafka.jar", code -> code.new_(RECORD).pop());

        assertThat(check())
            .singleElement()
            .satisfies(u -> assertThat(u.jar()).isEqualTo("lib/reactor-kafka.jar"));
    }

    @Test
    void should_resolve_against_kafka_classes_a_plugin_bundles_when_the_gateway_has_none() throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("lib/caller.jar", jarBytes(Map.of(PLUGIN, caller(code -> code.new_(RECORD).pop()))));
        entries.put("lib/kafka-clients-3.7.1.jar", jarBytes(Map.of(RECORD, classFile(RECORD, CD_Object, List.of(), cb -> {}))));
        zip(distribution.resolve("plugins/bundling.zip"), entries);

        assertThat(check()).isEmpty();
    }

    @Test
    void should_ignore_bundled_kafka_classes_that_the_gateway_library_shadows() throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put(
            "lib/caller.jar",
            jarBytes(Map.of(PLUGIN, caller(code -> code.aconst_null().invokevirtual(PRODUCER, "legacy", MTD_void))))
        );
        entries.put(
            "lib/kafka-clients-3.7.1.jar",
            jarBytes(
                Map.of(
                    PRODUCER,
                    classFile(PRODUCER, CD_Object, List.of(), cb ->
                        cb.withMethod("legacy", MTD_void, ACC_PUBLIC, mb -> mb.withCode(CodeBuilder::return_))
                    )
                )
            )
        );
        zip(distribution.resolve("plugins/shadowed.zip"), entries);

        // The plugin classloader is parent-first: the gateway's Producer wins, and it has no legacy().
        assertThat(check())
            .singleElement()
            .satisfies(u -> assertThat(u.reason()).isEqualTo("method-gone"));
    }

    @Test
    void should_ignore_bundled_kafka_classes_that_nothing_in_the_plugin_can_reach() throws IOException {
        ClassDesc orphan = ClassDesc.of("org.apache.kafka.clients.Producer$1");
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("lib/caller.jar", jarBytes(Map.of(PLUGIN, caller(code -> code.new_(PRODUCER).pop()))));
        entries.put(
            "lib/kafka-clients-3.7.1.jar",
            jarBytes(
                Map.of(
                    orphan,
                    classFile(orphan, CD_Object, List.of(), cb ->
                        cb.withMethod("run", MTD_void, ACC_PUBLIC, mb ->
                            mb.withCode(c -> c.aconst_null().invokestatic(PRODUCER, "access$000", MTD_void).return_())
                        )
                    )
                )
            )
        );
        zip(distribution.resolve("plugins/orphan.zip"), entries);

        // Only the bundled Producer would load Producer$1, and the gateway's Producer shadows it.
        assertThat(check()).isEmpty();
    }

    @Test
    void should_check_bundled_kafka_classes_that_the_plugin_reaches() throws IOException {
        ClassDesc bundledOnly = ClassDesc.of("org.apache.kafka.clients.LegacyHelper");
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("lib/caller.jar", jarBytes(Map.of(PLUGIN, caller(code -> code.new_(bundledOnly).pop()))));
        entries.put(
            "lib/kafka-clients-3.7.1.jar",
            jarBytes(
                Map.of(
                    bundledOnly,
                    classFile(bundledOnly, CD_Object, List.of(), cb ->
                        cb.withMethod("run", MTD_void, ACC_PUBLIC, mb ->
                            mb.withCode(c -> c.aconst_null().invokevirtual(PRODUCER, "legacy", MTD_void).return_())
                        )
                    )
                )
            )
        );
        zip(distribution.resolve("plugins/reached.zip"), entries);

        assertThat(check())
            .singleElement()
            .satisfies(u -> {
                assertThat(u.jar()).isEqualTo("lib/kafka-clients-3.7.1.jar");
                assertThat(u.reference()).isEqualTo("org/apache/kafka/clients/Producer.legacy()V");
            });
    }

    @Test
    void should_fail_with_every_unresolved_reference_in_the_message() throws IOException {
        plugin("policy-record-1.0.0.zip", "policy-record.jar", code -> code.new_(RECORD).pop());

        assertThatThrownBy(() -> main(""))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("policy-record-1.0.0.zip")
            .hasMessageContaining("org/apache/kafka/common/record/Record")
            .hasMessageContaining("class-gone");
    }

    @Test
    void should_pass_when_every_unresolved_reference_is_in_the_baseline() throws IOException {
        plugin("policy-record-1.0.0.zip", "policy-record.jar", code -> code.new_(RECORD).pop());

        assertThatCode(() ->
            main("policy-record org/apache/kafka/common/record/Record  # Unreachable: dead branch")
        ).doesNotThrowAnyException();
    }

    @Test
    void should_fail_on_a_baseline_entry_that_no_longer_matches() throws IOException {
        plugin("policy-record-1.0.0.zip", "policy-record.jar", code -> code.new_(PRODUCER).pop());

        assertThatThrownBy(() -> main("policy-record org/apache/kafka/common/record/Record  # Unreachable: dead branch"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("no longer match")
            .hasMessageContaining("policy-record org/apache/kafka/common/record/Record");
    }

    @Test
    void should_refuse_a_distribution_without_a_kafka_library() throws IOException {
        Files.delete(distribution.resolve("lib/ext/kafka-clients.jar"));
        plugin("policy-ok.zip", "policy-ok.jar", code -> code.new_(PRODUCER).pop());

        assertThatThrownBy(this::check).isInstanceOf(IllegalStateException.class).hasMessageContaining("org/apache/kafka");
    }

    private void main(String baseline) throws IOException {
        Path baselineFile = Files.writeString(distribution.resolve("baseline.txt"), baseline);
        KafkaLinkageCheck.main(new String[] { distribution.toString(), baselineFile.toString() });
    }

    private List<KafkaLinkageCheck.Unresolved> check() throws IOException {
        return new KafkaLinkageCheck(distribution).run();
    }

    private void plugin(String zipName, String jarEntry, Consumer<CodeBuilder> code) throws IOException {
        zip(distribution.resolve("plugins").resolve(zipName), Map.of(jarEntry, jarBytes(Map.of(PLUGIN, caller(code)))));
    }

    private static byte[] caller(Consumer<CodeBuilder> code) {
        return classFile(PLUGIN, CD_Object, List.of(), cb ->
            cb.withMethod("run", MTD_void, ACC_PUBLIC, mb ->
                mb.withCode(c -> {
                    code.accept(c);
                    c.return_();
                })
            )
        );
    }

    private static byte[] classFile(ClassDesc self, ClassDesc superclass, List<ClassDesc> interfaces, Consumer<ClassBuilder> body) {
        return ClassFile.of().build(self, cb -> {
            cb.withFlags(ACC_PUBLIC).withSuperclass(superclass).withInterfaceSymbols(interfaces);
            body.accept(cb);
        });
    }

    private static void jar(Path path, Map<ClassDesc, byte[]> classes) throws IOException {
        Files.createDirectories(path.getParent());
        Files.write(path, jarBytes(classes));
    }

    private static byte[] jarBytes(Map<ClassDesc, byte[]> classes) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        classes.forEach((desc, bytes) -> entries.put(internalName(desc) + ".class", bytes));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        write(out, entries);
        return out.toByteArray();
    }

    private static void zip(Path path, Map<String, byte[]> entries) throws IOException {
        Files.createDirectories(path.getParent());
        try (var out = Files.newOutputStream(path)) {
            write(out, entries);
        }
    }

    private static void write(OutputStream out, Map<String, byte[]> entries) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (var e : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(e.getKey()));
                zip.write(e.getValue());
                zip.closeEntry();
            }
        }
    }

    private static String internalName(ClassDesc desc) {
        String d = desc.descriptorString();
        return d.substring(1, d.length() - 1);
    }
}

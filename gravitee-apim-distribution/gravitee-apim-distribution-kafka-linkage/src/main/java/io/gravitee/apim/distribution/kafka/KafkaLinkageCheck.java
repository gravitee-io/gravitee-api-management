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

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.FieldRefEntry;
import java.lang.classfile.constantpool.MemberRefEntry;
import java.lang.classfile.constantpool.PoolEntry;
import java.lang.constant.ClassDesc;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Resolves every {@code org/apache/kafka} class, method and field reference of every jar in every
 * plugin zip of an assembled gateway distribution against the Kafka classes the gateway itself ships
 * in {@code lib/} and {@code lib/ext/}. Members are matched on name and full descriptor, through
 * superclasses and superinterfaces, falling back to the JDK — what the JVM does when it first links
 * the call site.
 *
 * <p>Plugin classloaders delegate parent-first, so a Kafka class a plugin bundles is only used when
 * the gateway has no class of that name; the check models exactly that.
 */
public class KafkaLinkageCheck {

    private static final String KAFKA_PACKAGE = "org/apache/kafka/";

    public record Unresolved(String plugin, String jar, String reference, String reason, Set<String> referencedFrom) {}

    private record Member(String name, String descriptor) {}

    private record KafkaClass(String superclass, List<String> interfaces, Set<Member> methods, Set<Member> fields) {
        static KafkaClass of(ClassModel model) {
            Set<Member> methods = new HashSet<>();
            Set<Member> fields = new HashSet<>();
            model.methods().forEach(m -> methods.add(new Member(m.methodName().stringValue(), m.methodType().stringValue())));
            model.fields().forEach(f -> fields.add(new Member(f.fieldName().stringValue(), f.fieldType().stringValue())));
            return new KafkaClass(
                model.superclass().map(ClassEntry::asInternalName).orElse(null),
                model.interfaces().stream().map(ClassEntry::asInternalName).toList(),
                methods,
                fields
            );
        }
    }

    private final Path distribution;

    public KafkaLinkageCheck(Path distribution) {
        this.distribution = distribution;
    }

    /** Arguments: the assembled gateway distribution, then the baseline of accepted references. */
    public static void main(String[] args) throws IOException {
        Path distribution = Path.of(args[0]);
        Path baselineFile = Path.of(args[1]);
        List<Unresolved> unresolved = new KafkaLinkageCheck(distribution).run();
        Set<String> pluginZips;
        try (Stream<Path> zips = Files.list(distribution.resolve("plugins"))) {
            pluginZips = zips
                .map(p -> p.getFileName().toString())
                .filter(n -> n.endsWith(".zip"))
                .collect(toSet());
        }
        Baseline.Verdict verdict = Baseline.parse(Files.readAllLines(baselineFile)).evaluate(unresolved, pluginZips);
        if (!verdict.unexpected().isEmpty() || !verdict.stale().isEmpty()) {
            throw new IllegalStateException(report(distribution, baselineFile, verdict));
        }
        System.out.println(
            "Kafka linkage: every org.apache.kafka reference of every plugin in " +
                distribution +
                " resolves, apart from the " +
                unresolved.size() +
                " accepted in " +
                baselineFile
        );
    }

    public List<Unresolved> run() throws IOException {
        Map<String, KafkaClass> gateway = gatewayKafkaClasses();
        if (gateway.isEmpty()) {
            throw new IllegalStateException(
                "No " + KAFKA_PACKAGE + " class in " + distribution.resolve("lib") + ": not an assembled gateway distribution"
            );
        }
        List<Unresolved> unresolved = new ArrayList<>();
        try (Stream<Path> zips = Files.list(distribution.resolve("plugins"))) {
            for (Path zip : zips
                .filter(p -> p.toString().endsWith(".zip"))
                .sorted()
                .toList()) {
                unresolved.addAll(checkPlugin(zip, gateway));
            }
        }
        return unresolved;
    }

    private Map<String, KafkaClass> gatewayKafkaClasses() throws IOException {
        Map<String, KafkaClass> classes = new HashMap<>();
        for (Path dir : List.of(distribution.resolve("lib"), distribution.resolve("lib/ext"))) {
            if (!Files.isDirectory(dir)) {
                continue;
            }
            try (Stream<Path> jars = Files.list(dir)) {
                for (Path jar : jars.filter(p -> p.toString().endsWith(".jar")).toList()) {
                    try (InputStream in = Files.newInputStream(jar)) {
                        for (ClassModel model : classesOf(in)) {
                            String name = model.thisClass().asInternalName();
                            if (name.startsWith(KAFKA_PACKAGE)) {
                                classes.put(name, KafkaClass.of(model));
                            }
                        }
                    }
                }
            }
        }
        return classes;
    }

    private static List<Unresolved> checkPlugin(Path zip, Map<String, KafkaClass> gateway) throws IOException {
        Map<String, List<ClassModel>> jars = new LinkedHashMap<>();
        try (ZipFile zipFile = new ZipFile(zip.toFile())) {
            for (ZipEntry entry : zipFile
                .stream()
                .filter(e -> e.getName().endsWith(".jar"))
                .toList()) {
                try (InputStream in = zipFile.getInputStream(entry)) {
                    jars.put(entry.getName(), classesOf(in));
                }
            }
        }

        // Kafka classes the plugin bundles under a name the gateway does not have. The others are
        // shadowed: the parent-first plugin classloader never loads them from the plugin.
        Map<String, ClassModel> bundledOnly = new HashMap<>();
        jars
            .values()
            .forEach(models ->
                models
                    .stream()
                    .filter(m -> isKafka(m) && !gateway.containsKey(m.thisClass().asInternalName()))
                    .forEach(m -> bundledOnly.put(m.thisClass().asInternalName(), m))
            );
        Set<String> reachable = reachableBundledClasses(jars, bundledOnly);

        Map<String, KafkaClass> visible = new HashMap<>(gateway);
        bundledOnly.forEach((name, model) -> visible.put(name, KafkaClass.of(model)));

        List<Unresolved> unresolved = new ArrayList<>();
        for (var jar : jars.entrySet()) {
            Map<String, String> reasons = new TreeMap<>();
            Map<String, Set<String>> referencedFrom = new HashMap<>();
            for (ClassModel model : jar.getValue()) {
                String self = model.thisClass().asInternalName();
                if (isKafka(model) && !reachable.contains(self)) {
                    continue;
                }
                for (PoolEntry entry : model.constantPool()) {
                    String reference = null;
                    String reason = null;
                    if (entry instanceof ClassEntry classEntry) {
                        String name = elementType(classEntry.asInternalName());
                        if (name != null && name.startsWith(KAFKA_PACKAGE) && !visible.containsKey(name)) {
                            reference = name;
                            reason = "class-gone";
                        }
                    } else if (entry instanceof MemberRefEntry member) {
                        String owner = elementType(member.owner().asInternalName());
                        // A missing owner is already reported through its own class entry.
                        if (owner != null && owner.startsWith(KAFKA_PACKAGE) && visible.containsKey(owner)) {
                            boolean field = member instanceof FieldRefEntry;
                            Member m = new Member(member.name().stringValue(), member.type().stringValue());
                            if (!resolves(owner, m, field, visible, new HashSet<>())) {
                                reference = owner + "." + m.name() + (field ? ":" : "") + m.descriptor();
                                reason = field ? fieldReason(owner, m, visible) : methodReason(owner, m, visible);
                            }
                        }
                    }
                    if (reference != null) {
                        reasons.put(reference, reason);
                        referencedFrom.computeIfAbsent(reference, r -> new TreeSet<>()).add(self.replaceAll("\\$.*", ""));
                    }
                }
            }
            reasons.forEach((reference, reason) ->
                unresolved.add(new Unresolved(zip.getFileName().toString(), jar.getKey(), reference, reason, referencedFrom.get(reference)))
            );
        }
        return unresolved;
    }

    /**
     * A bundled-only Kafka class loads when the plugin's own code names it, or when another such class
     * does. The gateway's Kafka classes only name each other, so they never pull one in. A plugin that
     * bundles an older kafka-clients thus carries inner classes — {@code DescribeConfigsResult$1} —
     * whose outer class the gateway shadows, and that nothing can load.
     */
    private static Set<String> reachableBundledClasses(Map<String, List<ClassModel>> jars, Map<String, ClassModel> bundledOnly) {
        Set<String> reachable = new HashSet<>();
        Deque<ClassModel> toVisit = new ArrayDeque<>();
        jars
            .values()
            .forEach(models ->
                models
                    .stream()
                    .filter(m -> !isKafka(m))
                    .forEach(toVisit::push)
            );
        while (!toVisit.isEmpty()) {
            for (PoolEntry entry : toVisit.pop().constantPool()) {
                if (entry instanceof ClassEntry classEntry) {
                    String name = elementType(classEntry.asInternalName());
                    if (bundledOnly.containsKey(name) && reachable.add(name)) {
                        toVisit.push(bundledOnly.get(name));
                    }
                }
            }
        }
        return reachable;
    }

    private static boolean isKafka(ClassModel model) {
        return model.thisClass().asInternalName().startsWith(KAFKA_PACKAGE);
    }

    private static List<ClassModel> classesOf(InputStream jar) throws IOException {
        List<ClassModel> models = new ArrayList<>();
        ZipInputStream in = new ZipInputStream(jar);
        for (ZipEntry entry; (entry = in.getNextEntry()) != null; ) {
            String name = entry.getName();
            // Multi-release variants and module descriptors are not what the gateway links against.
            if (name.endsWith(".class") && !name.startsWith("META-INF/") && !name.endsWith("module-info.class")) {
                models.add(ClassFile.of().parse(in.readAllBytes()));
            }
        }
        return models;
    }

    /** Array types resolve through their element type; primitive arrays are not class references. */
    private static String elementType(String internalName) {
        if (!internalName.startsWith("[")) {
            return internalName;
        }
        String element = internalName.replaceAll("^\\[+", "");
        return element.startsWith("L") ? element.substring(1, element.length() - 1) : null;
    }

    private static boolean resolves(String owner, Member member, boolean field, Map<String, KafkaClass> visible, Set<String> seen) {
        if (owner == null || !seen.add(owner)) {
            return false;
        }
        KafkaClass kafkaClass = visible.get(owner);
        if (kafkaClass == null) {
            return jdkHas(owner, member, field);
        }
        if ((field ? kafkaClass.fields() : kafkaClass.methods()).contains(member)) {
            return true;
        }
        if (resolves(kafkaClass.superclass(), member, field, visible, seen)) {
            return true;
        }
        for (String itf : kafkaClass.interfaces()) {
            if (resolves(itf, member, field, visible, seen)) {
                return true;
            }
        }
        return false;
    }

    private static String fieldReason(String owner, Member member, Map<String, KafkaClass> visible) {
        return visible
                .get(owner)
                .fields()
                .stream()
                .anyMatch(f -> f.name().equals(member.name()))
            ? "field-type-changed"
            : "field-gone";
    }

    private static String methodReason(String owner, Member member, Map<String, KafkaClass> visible) {
        Set<Member> candidates = new HashSet<>();
        Deque<String> types = new ArrayDeque<>(List.of(owner));
        Set<String> seen = new HashSet<>();
        while (!types.isEmpty()) {
            KafkaClass kafkaClass = visible.get(types.pop());
            if (kafkaClass != null) {
                candidates.addAll(kafkaClass.methods());
                Stream.concat(Stream.ofNullable(kafkaClass.superclass()), kafkaClass.interfaces().stream())
                    .filter(seen::add)
                    .forEach(types::push);
            }
        }
        boolean constructor = member.name().equals("<init>");
        String parameters = member.descriptor().substring(0, member.descriptor().indexOf(')') + 1);
        if (candidates.stream().anyMatch(c -> c.name().equals(member.name()) && c.descriptor().startsWith(parameters))) {
            return "return-type-changed";
        }
        if (candidates.stream().anyMatch(c -> c.name().equals(member.name()))) {
            return constructor ? "constructor-signature-changed" : "signature-changed";
        }
        return constructor ? "constructor-gone" : "method-gone";
    }

    /** Members a Kafka class inherits from JDK types: Object, RuntimeException, Enum, Record... */
    private static boolean jdkHas(String internalName, Member member, boolean field) {
        Class<?> type;
        try {
            type = Class.forName(internalName.replace('/', '.'), false, ClassLoader.getPlatformClassLoader());
        } catch (ClassNotFoundException e) {
            return false;
        }
        Deque<Class<?>> types = new ArrayDeque<>(List.of(type, Object.class));
        while (!types.isEmpty()) {
            Class<?> current = types.pop();
            if (declares(current, member, field)) {
                return true;
            }
            if (current.getSuperclass() != null) {
                types.push(current.getSuperclass());
            }
            types.addAll(Arrays.asList(current.getInterfaces()));
        }
        return false;
    }

    private static boolean declares(Class<?> type, Member member, boolean field) {
        if (field) {
            for (Field f : type.getDeclaredFields()) {
                if (f.getName().equals(member.name()) && f.getType().descriptorString().equals(member.descriptor())) {
                    return true;
                }
            }
            return false;
        }
        boolean constructor = member.name().equals("<init>");
        Executable[] executables = constructor ? type.getDeclaredConstructors() : type.getDeclaredMethods();
        for (Executable executable : executables) {
            boolean sameName = constructor || executable.getName().equals(member.name());
            Class<?> returnType = executable instanceof Method method ? method.getReturnType() : void.class;
            if (sameName && descriptor(executable.getParameterTypes(), returnType).equals(member.descriptor())) {
                return true;
            }
        }
        return false;
    }

    private static String descriptor(Class<?>[] parameters, Class<?> returnType) {
        return Arrays.stream(parameters)
            .map(Class::descriptorString)
            .collect(joining("", "(", ")" + returnType.describeConstable().map(ClassDesc::descriptorString).orElseThrow()));
    }

    private static String report(Path distribution, Path baselineFile, Baseline.Verdict verdict) {
        StringBuilder report = new StringBuilder();
        List<Unresolved> unexpected = verdict.unexpected();
        if (!unexpected.isEmpty()) {
            report
                .append(unexpected.size())
                .append(" org.apache.kafka reference(s) do not link against the Kafka library shipped in ")
                .append(distribution.resolve("lib/ext"))
                .append(". Each would throw NoClassDefFoundError, NoSuchMethodError or NoSuchFieldError")
                .append(" the first time the plugin runs that code. Fix the plugin, or, if the code is unreachable,")
                .append(" add the reference to ")
                .append(baselineFile)
                .append(" with the reason:\n");
            unexpected
                .stream()
                .collect(groupingBy(u -> u.plugin() + " / " + u.jar(), TreeMap::new, toList()))
                .forEach((jar, references) -> {
                    report.append("\n  ").append(jar).append('\n');
                    references.forEach(u ->
                        report
                            .append("    ")
                            .append(u.reference())
                            .append("  [")
                            .append(u.reason())
                            .append("]  <- ")
                            .append(String.join(", ", u.referencedFrom()))
                            .append('\n')
                    );
                });
        }
        if (!verdict.stale().isEmpty()) {
            report
                .append("\n")
                .append(verdict.stale().size())
                .append(" entry(ies) of ")
                .append(baselineFile)
                .append(" no longer match an unresolved reference although their plugin is in the distribution. Remove them:\n");
            verdict.stale().forEach(entry -> report.append("    ").append(entry).append('\n'));
        }
        return report.toString();
    }
}

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
package io.gravitee.apim.integration.tests.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import io.gravitee.repository.analytics.engine.api.query.ObservabilityEntrypoints;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;

/**
 * Holds {@link ObservabilityEntrypoints} to the entrypoint plugins the distribution actually ships.
 *
 * <p>Elasticsearch stores {@code connector.id()} as {@code entrypoint-id}, so the only faithful
 * source for the ids is each plugin's own {@code plugin.properties}. This test opens every bundled
 * entrypoint zip and reads it from there; the artifact-name check in {@code ObservabilityEntrypointsTest}
 * stays as the fast unit-level complement, because it is the one that runs when only the registry
 * changes.
 *
 * <p>Runs in the {@code Integration tests} CI job, which attaches the workspace where
 * {@code Build backend} assembled both distributions with {@code -Dbundle=dev}. That job is the one
 * triggered by a change to the distribution pom — the moment a new entrypoint reaches the product.
 *
 * @author GraviteeSource Team
 */
class ObservabilityEntrypointsDistributionTest {

    private static final String ENTRYPOINT_CONNECTOR_TYPE = "entrypoint-connector";
    private static final Pattern VERSION_SUFFIX = Pattern.compile("-\\d.*$");

    /** One bundled entrypoint plugin, as the gateway would load it. */
    record InstalledEntrypoint(String zip, String artifactId, String id) {}

    @Test
    void should_declare_every_entrypoint_plugin_the_distribution_ships() {
        var installed = installedEntrypoints();
        var registryIds = Arrays.stream(ObservabilityEntrypoints.values()).map(ObservabilityEntrypoints::id).toList();

        var undecided = installed
            .values()
            .stream()
            .filter(e -> !registryIds.contains(e.id()))
            .toList();

        assertThat(undecided).as("bundled entrypoints whose id has no ObservabilityEntrypoints constant stating its scope").isEmpty();
    }

    @Test
    void should_not_keep_a_decision_for_a_plugin_the_distribution_no_longer_ships() {
        var installedArtifacts = installedEntrypoints().keySet();

        var stale = Arrays.stream(ObservabilityEntrypoints.values())
            .flatMap(constant ->
                constant
                    .pluginArtifactId()
                    .stream()
                    .map(artifact -> Map.entry(constant, artifact))
            )
            .filter(entry -> !installedArtifacts.contains(entry.getValue()))
            .map(entry -> entry.getKey() + " -> " + entry.getValue())
            .toList();

        assertThat(stale).as("registry constants declaring a plugin artifact that is not bundled any more").isEmpty();
    }

    @Test
    void should_record_the_id_the_plugin_really_declares() {
        var declaredIdByArtifact = Arrays.stream(ObservabilityEntrypoints.values())
            .filter(constant -> constant.pluginArtifactId().isPresent())
            .collect(Collectors.toMap(constant -> constant.pluginArtifactId().get(), ObservabilityEntrypoints::id));

        var mismatches = installedEntrypoints()
            .values()
            .stream()
            .filter(e -> declaredIdByArtifact.containsKey(e.artifactId()))
            .filter(e -> !declaredIdByArtifact.get(e.artifactId()).equals(e.id()))
            .map(
                e ->
                    e.artifactId() +
                    ": plugin.properties says '" +
                    e.id() +
                    "', registry says '" +
                    declaredIdByArtifact.get(e.artifactId()) +
                    "'"
            )
            .toList();

        assertThat(mismatches).as("registry ids that differ from the id the plugin itself declares").isEmpty();
    }

    /** Union of both distributions: the gateway assembly leaves the native-kafka zip out, the rest-api one ships it. */
    private static Map<String, InstalledEntrypoint> installedEntrypoints() {
        var installed = new LinkedHashMap<String, InstalledEntrypoint>();
        for (var pluginsDir : assembledPluginDirectories()) {
            try (Stream<Path> zips = Files.list(pluginsDir)) {
                zips
                    .filter(zip -> zip.getFileName().toString().endsWith(".zip"))
                    .sorted()
                    .map(ObservabilityEntrypointsDistributionTest::readEntrypoint)
                    .flatMap(Optional::stream)
                    .forEach(e -> installed.putIfAbsent(e.artifactId(), e));
            } catch (IOException e) {
                throw new UncheckedIOException("Unable to list " + pluginsDir, e);
            }
        }
        assertThat(installed).as("no entrypoint plugin found under %s — the parsing is stale", assembledPluginDirectories()).isNotEmpty();
        return installed;
    }

    /** The plugin's own jar sits at the zip root; {@code lib/} only holds its dependencies. */
    private static Optional<InstalledEntrypoint> readEntrypoint(Path zip) {
        try (var zipFile = new ZipFile(zip.toFile())) {
            var pluginJar = zipFile
                .stream()
                .filter(entry -> !entry.getName().contains("/") && entry.getName().endsWith(".jar"))
                .findFirst();
            if (pluginJar.isEmpty()) {
                return Optional.empty();
            }
            var properties = pluginProperties(zipFile, pluginJar.get());
            if (properties.isEmpty() || !ENTRYPOINT_CONNECTOR_TYPE.equals(properties.get().getProperty("type"))) {
                return Optional.empty();
            }
            var zipName = zip.getFileName().toString();
            var artifactId = VERSION_SUFFIX.matcher(zipName.substring(0, zipName.length() - ".zip".length())).replaceFirst("");
            return Optional.of(new InstalledEntrypoint(zipName, artifactId, properties.get().getProperty("id")));
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read " + zip, e);
        }
    }

    private static Optional<Properties> pluginProperties(ZipFile zipFile, ZipEntry pluginJar) throws IOException {
        try (var jar = new ZipInputStream(zipFile.getInputStream(pluginJar))) {
            for (ZipEntry entry = jar.getNextEntry(); entry != null; entry = jar.getNextEntry()) {
                if ("plugin.properties".equals(entry.getName())) {
                    var properties = new Properties();
                    properties.load(jar);
                    return Optional.of(properties);
                }
            }
        }
        return Optional.empty();
    }

    private static List<Path> assembledPluginDirectories() {
        var standalone = Path.of("gravitee-apim-distribution-standalone");
        for (var candidate = Path.of("").toAbsolutePath(); candidate != null; candidate = candidate.getParent()) {
            var root = candidate.resolve(standalone);
            if (Files.isDirectory(root)) {
                var dirs = new ArrayList<Path>();
                for (var component : List.of("rest-api", "gateway")) {
                    var plugins = root.resolve("gravitee-apim-distribution-standalone-" + component).resolve("target/distribution/plugins");
                    if (Files.isDirectory(plugins)) {
                        dirs.add(plugins);
                    }
                }
                if (dirs.isEmpty()) {
                    return fail(
                        "No assembled distribution under %s. Assemble it first: mvn -f gravitee-apim-distribution/pom.xml install -Pengine-snapshot -DskipTests -Dbundle=dev",
                        root
                    );
                }
                return dirs;
            }
        }
        return fail("Unable to locate %s from %s", standalone, Path.of("").toAbsolutePath());
    }
}

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
package io.gravitee.apim.integration.tests.version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Holds the version a running node reports to the version of the distribution it was assembled in.
 *
 * <p>Every consumer of the APIM version — the Cockpit hello command, the gateway heartbeat, the node
 * endpoint, the startup banner — reads {@code Version.RUNTIME_VERSION}, which is the first
 * {@code version.properties} on the classpath. The core container jar carries one stamped with the
 * core version; once the core and the distribution stop sharing a number, that is not the version
 * users know. The distribution must ship its own and make it win.
 *
 * <p>Like {@code ObservabilityEntrypointsDistributionTest}, this reads the distributions that
 * {@code Build backend} assembled, and needs them under {@code target/distribution}.
 *
 * @author GraviteeSource Team
 */
class RuntimeVersionDistributionTest {

    private static final String DISTRIBUTION_VERSION_PROPERTY = "apim.distribution.version";

    @ParameterizedTest
    @ValueSource(strings = { "rest-api", "gateway" })
    void should_report_the_distribution_version_at_runtime(String component) throws Exception {
        var distributionVersion = System.getProperty(DISTRIBUTION_VERSION_PROPERTY);
        assertThat(distributionVersion).as("system property %s", DISTRIBUTION_VERSION_PROPERTY).isNotBlank();
        var home = assembledDistribution(component);

        try (var extension = extensionClassLoader(home); var gravitee = graviteeClassLoader(home, extension)) {
            var version = gravitee.loadClass("io.gravitee.common.util.Version");
            var runtime = version.getField("RUNTIME_VERSION").get(null);

            assertThat(version.getField("MAJOR_VERSION").get(runtime)).isEqualTo(distributionVersion);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "rest-api", "gateway" })
    void should_resolve_the_version_file_the_distribution_ships_ahead_of_the_core_one(String component) throws Exception {
        var home = assembledDistribution(component);

        try (var extension = extensionClassLoader(home); var gravitee = graviteeClassLoader(home, extension)) {
            var resolved = gravitee.getResource("version.properties");

            assertThat(resolved).as("version.properties on the %s classpath", component).isNotNull();
            assertThat(resolved.toString()).isEqualTo(home.resolve("lib/ext/version.properties").toUri().toURL().toString());
        }
    }

    /** As the standalone bootstraps build it: the {@code lib/ext} directory itself, then every entry in it. */
    private static URLClassLoader extensionClassLoader(Path home) throws IOException {
        var ext = home.resolve("lib/ext");
        var urls = new ArrayList<URL>();
        urls.add(ext.toUri().toURL());
        urls.addAll(entries(ext));
        return new URLClassLoader(urls.toArray(URL[]::new), ClassLoader.getPlatformClassLoader());
    }

    /** As the standalone bootstraps build it: every entry in {@code lib}, child of the extension class loader. */
    private static URLClassLoader graviteeClassLoader(Path home, ClassLoader extension) throws IOException {
        return new URLClassLoader(entries(home.resolve("lib")).toArray(URL[]::new), extension);
    }

    private static List<URL> entries(Path directory) throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            return files
                .map(file -> {
                    try {
                        return file.toUri().toURL();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .toList();
        }
    }

    private static Path assembledDistribution(String component) {
        var standalone = Path.of("gravitee-apim-distribution-standalone");
        for (var candidate = Path.of("").toAbsolutePath(); candidate != null; candidate = candidate.getParent()) {
            var root = candidate.resolve(standalone);
            if (Files.isDirectory(root)) {
                var distribution = root.resolve("gravitee-apim-distribution-standalone-" + component).resolve("target/distribution");
                if (!Files.isDirectory(distribution.resolve("lib"))) {
                    return fail("No assembled %s distribution under %s. Assemble it first: task build-distribution", component, root);
                }
                return distribution;
            }
        }
        return fail("Unable to locate %s from %s", standalone, Path.of("").toAbsolutePath());
    }
}

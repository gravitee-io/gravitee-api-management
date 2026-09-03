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
package io.gravitee.repository.analytics.engine.api.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
class ObservabilityEntrypointsTest {

    @Nested
    class Scopes {

        @Test
        void should_expose_the_http_scope_in_declaration_order() {
            assertThat(ObservabilityEntrypoints.HTTP_SCOPE_IDS).containsExactly(
                "http-get",
                "http-post",
                "http-proxy",
                "llm-proxy",
                "mcp-proxy",
                "a2a-proxy"
            );
        }

        @Test
        void should_extend_the_http_scope_with_native_kafka_only_for_logs() {
            assertThat(ObservabilityEntrypoints.LOGS_SCOPE_IDS).containsExactly(
                "http-get",
                "http-post",
                "http-proxy",
                "llm-proxy",
                "mcp-proxy",
                "a2a-proxy",
                "native-kafka"
            );
        }
    }

    /**
     * Guards the registry against the drift it exists to prevent. Entrypoint plugins live in their
     * own repositories, so nothing here can observe a new {@code entrypoint-id} directly; bundling
     * the plugin in the distribution is the first moment a new entrypoint becomes visible to this
     * repository, and that is where this test intervenes.
     *
     * <p>It matches artifact names, which is all this reactor can see. The id each plugin really
     * declares is checked by {@code ObservabilityEntrypointsDistributionTest} in the distribution
     * reactor, against the assembled plugins; this one stays as the fast check that runs whenever
     * this module changes.
     */
    @Nested
    class DistributionCoverage {

        private static final Pattern ENTRYPOINT_PLUGIN_ARTIFACT = Pattern.compile(
            "<artifactId>((?:gravitee-entrypoint|gravitee-apim-plugin-entrypoint)-[^<]+)</artifactId>"
        );

        /** The plugin SPI every entrypoint is built against, not an entrypoint itself. */
        private static final String ENTRYPOINT_PLUGIN_SPI_ARTIFACT = "gravitee-apim-plugin-entrypoint-handler";

        @Test
        void should_declare_every_entrypoint_plugin_bundled_by_the_distribution() {
            assertThat(bundledEntrypointPluginArtifacts())
                .as("every bundled entrypoint plugin needs an ObservabilityEntrypoints entry stating its scope")
                .isSubsetOf(declaredPluginArtifacts());
        }

        @Test
        void should_not_declare_an_entrypoint_plugin_the_distribution_no_longer_bundles() {
            assertThat(declaredPluginArtifacts())
                .as("a declared entrypoint plugin that is no longer bundled leaves a stale scope decision behind")
                .isSubsetOf(bundledEntrypointPluginArtifacts());
        }

        private Set<String> declaredPluginArtifacts() {
            return Arrays.stream(ObservabilityEntrypoints.values())
                .map(ObservabilityEntrypoints::pluginArtifactId)
                .flatMap(Optional::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        private Set<String> bundledEntrypointPluginArtifacts() {
            var pom = standaloneDistributionPom();
            String content;
            try {
                content = Files.readString(pom);
            } catch (IOException e) {
                throw new UncheckedIOException("Unable to read " + pom, e);
            }

            var artifacts = new LinkedHashSet<String>();
            var matcher = ENTRYPOINT_PLUGIN_ARTIFACT.matcher(content);
            while (matcher.find()) {
                var artifact = matcher.group(1);
                if (!ENTRYPOINT_PLUGIN_SPI_ARTIFACT.equals(artifact)) {
                    artifacts.add(artifact);
                }
            }

            assertThat(artifacts).as("no entrypoint plugin found in %s — the parsing is stale", pom).isNotEmpty();
            return artifacts;
        }

        private Path standaloneDistributionPom() {
            var relativeToRoot = Path.of("gravitee-apim-distribution", "gravitee-apim-distribution-standalone", "pom.xml");
            for (var candidate = Path.of("").toAbsolutePath(); candidate != null; candidate = candidate.getParent()) {
                var pom = candidate.resolve(relativeToRoot);
                if (Files.isRegularFile(pom)) {
                    return pom;
                }
            }
            throw new IllegalStateException("Unable to locate " + relativeToRoot + " from " + Path.of("").toAbsolutePath());
        }
    }
}

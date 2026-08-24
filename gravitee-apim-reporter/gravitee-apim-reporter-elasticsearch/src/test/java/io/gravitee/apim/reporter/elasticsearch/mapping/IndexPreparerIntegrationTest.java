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
package io.gravitee.apim.reporter.elasticsearch.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.reporter.elasticsearch.UnitTestConfiguration;
import io.gravitee.apim.reporter.elasticsearch.config.PipelineConfiguration;
import io.gravitee.apim.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.apim.reporter.elasticsearch.mapping.es7.ES7IndexPreparer;
import io.gravitee.apim.reporter.elasticsearch.mapping.es8.ES8IndexPreparer;
import io.gravitee.apim.reporter.elasticsearch.mapping.es9.ES9IndexPreparer;
import io.gravitee.common.templating.FreeMarkerComponent;
import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.config.Endpoint;
import io.reactivex.rxjava3.observers.TestObserver;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Drives {@link AbstractIndexPreparer#prepare()} against a real OpenSearch container through each
 * es{@code 7,8,9}x preparer. The ES preparers are instantiated directly so the put-template HTTP path
 * actually exercises the freemarker templates this change fixes —
 * {@link io.gravitee.apim.reporter.elasticsearch.factory.BeanFactoryBuilder} would auto-pick
 * {@code OpenSearchIndexPreparer} for an OpenSearch cluster and use the unchanged {@code opensearch/}
 * templates instead, masking the regression.
 *
 * <p>This reproduces the AWS-OpenSearch-compat-mode scenario where the reporter picks the es{@code Nx}
 * template tree and the ISM property-name overrides must flow into the rendered body for OpenSearch to
 * accept the PUT.
 *
 * <p>Scope: this proves a real cluster both accepts the rendered templates and stores the lifecycle
 * settings they carry. Acceptance alone proves nothing — a cluster returns 200 for a template whose
 * lifecycle block is absent — so the stored template is read back. What the templates render for every
 * tree and type is asserted by {@link IndexTemplateTest}, which needs no container.
 */
@SpringJUnitConfig(IndexPreparerIntegrationTest.OpenSearchTestConfig.class)
@TestPropertySource(
    properties = {
        "reporters.elasticsearch.index=gravitee-ism-override",
        "reporters.elasticsearch.lifecycle.policy_property_name=index.plugins.index_state_management.policy_id",
        "reporters.elasticsearch.lifecycle.rollover_alias_property_name=index.plugins.index_state_management.rollover_alias",
        "reporters.elasticsearch.lifecycle.policies.health=policy-health",
        "reporters.elasticsearch.lifecycle.policies.monitor=policy-monitor",
        "reporters.elasticsearch.lifecycle.policies.request=policy-request",
        "reporters.elasticsearch.lifecycle.policies.log=policy-log",
        "reporters.elasticsearch.lifecycle.policies.event_metrics=policy-event-metrics",
    }
)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class IndexPreparerIntegrationTest {

    @Autowired
    private Client client;

    @Autowired
    private ReporterConfiguration configuration;

    @Autowired
    private FreeMarkerComponent freeMarkerComponent;

    @Autowired
    private PipelineConfiguration pipelineConfiguration;

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    static Stream<Arguments> es_preparers() {
        return Stream.of(Arguments.of("es7x"), Arguments.of("es8x"), Arguments.of("es9x"));
    }

    @ParameterizedTest(name = "{0} preparer against OpenSearch")
    @MethodSource("es_preparers")
    void should_put_templates_against_opensearch_when_ism_property_names_set(String esDir) throws Exception {
        AbstractIndexPreparer preparer = preparerFor(esDir);
        // Every tree PUTs the same template names into one shared container. Without this, a tree whose
        // PUT never happened would pass on the previous tree's leftovers.
        deleteStoredTemplates();

        TestObserver<Void> observer = preparer.prepare().test();
        observer.awaitDone(60, TimeUnit.SECONDS);
        observer.assertComplete();
        observer.assertNoErrors();

        // A 200 on the PUT only proves the body parsed. Read the stored template back: every cluster
        // accepts a template whose lifecycle block is missing or meaningless, so acceptance alone
        // cannot tell a working configuration from a silently dropped one.
        // Data-stream types always go through the composable index template API, on every tree.
        String eventMetrics = storedTemplate("_index_template", "gravitee-ism-override-event-metrics");
        assertThat(eventMetrics).contains("policy-event-metrics");
        assertThat(eventMetrics).doesNotContain("rollover_alias");

        // Control: alias-managed types still resolve their policy. es7x pushes those through the
        // legacy template API (useOldClient), so they are stored under _template, not _index_template.
        String legacyApi = "es7x".equals(esDir) ? "_template" : "_index_template";
        assertThat(storedTemplate(legacyApi, "gravitee-ism-override-request")).contains("policy-request");
    }

    private String storedTemplate(String api, String name) throws Exception {
        HttpResponse<String> response = send(HttpRequest.newBuilder(uri(api, name)).GET());
        assertThat(response.statusCode()).as("GET %s/%s", api, name).isEqualTo(200);
        return response.body();
    }

    private void deleteStoredTemplates() throws Exception {
        for (String name : new String[] { "event-metrics", "request" }) {
            send(HttpRequest.newBuilder(uri("_index_template", "gravitee-ism-override-" + name)).DELETE());
            send(HttpRequest.newBuilder(uri("_template", "gravitee-ism-override-" + name)).DELETE());
        }
    }

    private URI uri(String api, String name) {
        return URI.create("%s/%s/%s".formatted(configuration.getEndpoints().get(0).getUrl(), api, name));
    }

    private HttpResponse<String> send(HttpRequest.Builder request) throws Exception {
        return HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private AbstractIndexPreparer preparerFor(String esDir) {
        return switch (esDir) {
            case "es7x" -> new ES7IndexPreparer(configuration, pipelineConfiguration, freeMarkerComponent, client);
            case "es8x" -> new ES8IndexPreparer(configuration, pipelineConfiguration, freeMarkerComponent, client);
            case "es9x" -> new ES9IndexPreparer(configuration, pipelineConfiguration, freeMarkerComponent, client);
            default -> throw new IllegalArgumentException("Unknown es dir: " + esDir);
        };
    }

    @Configuration
    @Import(UnitTestConfiguration.class)
    static class OpenSearchTestConfig {

        public static final String OPENSEARCH_DEFAULT_VERSION = "2.13.0";

        @Value("${opensearch.version:" + OPENSEARCH_DEFAULT_VERSION + "}")
        private String opensearchVersion;

        @Bean
        public ReporterConfiguration configuration(GenericContainer<?> openSearchContainer) {
            ReporterConfiguration configuration = new ReporterConfiguration();
            configuration.setEndpoints(
                Collections.singletonList(
                    new Endpoint("http://" + openSearchContainer.getHost() + ":" + openSearchContainer.getMappedPort(9200))
                )
            );
            return configuration;
        }

        @Bean(destroyMethod = "stop")
        public GenericContainer<?> openSearchContainer() {
            final GenericContainer<?> container = new GenericContainer<>(
                DockerImageName.parse("opensearchproject/opensearch:" + opensearchVersion)
            )
                .withExposedPorts(9200)
                .withEnv("discovery.type", "single-node")
                .withEnv("DISABLE_SECURITY_PLUGIN", "true")
                .withEnv("DISABLE_INSTALL_DEMO_CONFIG", "true")
                .waitingFor(Wait.forHttp("/").forStatusCode(200));
            container.start();
            return container;
        }
    }
}

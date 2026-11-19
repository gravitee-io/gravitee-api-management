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
package io.gravitee.apim.reporter.elasticsearch.mapping.serverless;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.gravitee.apim.reporter.elasticsearch.UnitTestConfiguration;
import io.gravitee.apim.reporter.elasticsearch.config.PipelineConfiguration;
import io.gravitee.apim.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.apim.reporter.elasticsearch.config.ServerlessRetention;
import io.gravitee.common.templating.FreeMarkerComponent;
import io.gravitee.elasticsearch.client.Client;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.observers.TestObserver;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

/**
 * The serverless templates are managed: each carries a {@code _meta} naming the APIM version that wrote it, and the reporter
 * changes neither a template a newer APIM wrote nor one it did not write. The retention of an existing data stream follows
 * the configured one, but only decreases when the operator allows it.
 */
class ServerlessIndexPreparerTest {

    private static final String APIM_VERSION = "4.14.0";
    private static final String ACKNOWLEDGED = "{\"acknowledged\":true}";
    private static final String REQUEST_TEMPLATE = "/_index_template/gravitee-request";
    private static final String REQUEST_LIFECYCLE = "/_data_stream/gravitee-request/_lifecycle";

    @RegisterExtension
    static WireMockExtension elasticsearch = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private AnnotationConfigApplicationContext context;
    private ReporterConfiguration configuration;

    @BeforeEach
    void setUp() {
        logs.start();
        ((Logger) LoggerFactory.getLogger("io.gravitee")).addAppender(logs);
        elasticsearch.stubFor(put(urlPathMatching(".*")).willReturn(okJson(ACKNOWLEDGED)));

        context = new AnnotationConfigApplicationContext();
        context
            .getEnvironment()
            .getPropertySources()
            .addFirst(new MapPropertySource("test", Map.of("reporters.elasticsearch.endpoints[0]", elasticsearch.baseUrl())));
        context.register(UnitTestConfiguration.class);
        context.refresh();
        configuration = context.getBean(ReporterConfiguration.class);
    }

    @AfterEach
    void tearDown() {
        context.close();
        ((Logger) LoggerFactory.getLogger("io.gravitee")).detachAppender(logs);
    }

    @Nested
    class ManagedTemplates {

        @Test
        void should_mark_the_templates_it_writes_with_the_apim_version_and_the_retention() {
            prepare().test().awaitDone(10, SECONDS).assertComplete();

            elasticsearch.verify(
                putRequestedFor(urlEqualTo(REQUEST_TEMPLATE))
                    .withRequestBody(matchingJsonPath("$._meta.managed_by", equalTo("gravitee-apim")))
                    .withRequestBody(matchingJsonPath("$._meta.apim_version", equalTo(APIM_VERSION)))
                    .withRequestBody(matchingJsonPath("$._meta.data_retention", equalTo("45d")))
            );
        }

        @Test
        void should_overwrite_a_template_written_by_the_same_or_an_older_apim() {
            stubRequestTemplate(managedTemplate("4.13.2", "45d"));

            prepare().test().awaitDone(10, SECONDS).assertComplete();

            elasticsearch.verify(putRequestedFor(urlEqualTo(REQUEST_TEMPLATE)));
        }

        @Test
        void should_keep_a_template_written_by_a_newer_apim_and_the_retention_of_its_data_stream() {
            stubRequestTemplate(managedTemplate("4.15.0", "90d"));
            stubRequestLifecycle("90d");

            prepare().test().awaitDone(10, SECONDS).assertComplete();

            elasticsearch.verify(0, putRequestedFor(urlEqualTo(REQUEST_TEMPLATE)));
            elasticsearch.verify(0, putRequestedFor(urlEqualTo(REQUEST_LIFECYCLE)));
            elasticsearch.verify(putRequestedFor(urlEqualTo("/_index_template/gravitee-v4-log")));
            assertThat(messages(Level.INFO)).anySatisfy(message ->
                assertThat(message).contains("[gravitee-request]").contains("4.15.0").contains(APIM_VERSION)
            );
        }

        @Test
        void should_stop_the_setup_without_retrying_when_a_template_with_a_managed_name_lacks_the_marker() {
            stubRequestTemplate("{\"index_patterns\":[\"gravitee-request*\"],\"data_stream\":{},\"priority\":500}");

            prepare()
                .test()
                .awaitDone(10, SECONDS)
                .assertError(
                    error ->
                        error instanceof UnmanagedTemplateException &&
                        error.getMessage().contains("[gravitee-request]") &&
                        error.getMessage().contains("'reporters.elasticsearch.index'")
                );

            elasticsearch.verify(0, putRequestedFor(urlEqualTo(REQUEST_TEMPLATE)));
            assertThat(messages(Level.WARN)).noneMatch(message -> message.contains("retrying"));
        }
    }

    @Nested
    class RetentionOfExistingDataStreams {

        @Test
        void should_apply_a_longer_retention_to_an_existing_data_stream() {
            stubRequestLifecycle("30d");

            prepare().test().awaitDone(10, SECONDS).assertComplete();

            elasticsearch.verify(
                putRequestedFor(urlEqualTo(REQUEST_LIFECYCLE)).withRequestBody(equalToJson("{\"data_retention\":\"45d\"}"))
            );
            assertThat(messages(Level.INFO)).anySatisfy(message ->
                assertThat(message).contains("[gravitee-request]").contains("30d").contains("45d")
            );
        }

        @Test
        void should_refuse_a_shorter_retention_with_an_error_naming_the_data_stream_and_both_values() {
            stubRequestLifecycle("90d");

            prepare().test().awaitDone(10, SECONDS).assertComplete();

            elasticsearch.verify(0, putRequestedFor(urlEqualTo(REQUEST_LIFECYCLE)));
            assertThat(messages(Level.ERROR))
                .singleElement()
                .satisfies(message ->
                    assertThat(message)
                        .contains("[gravitee-request]")
                        .contains("90d")
                        .contains("45d")
                        .contains("'reporters.elasticsearch.lifecycle.retention.request'")
                        .contains("'reporters.elasticsearch.lifecycle.retention.allow_decrease'")
                );
        }

        @Test
        void should_refuse_to_limit_the_retention_of_a_data_stream_that_keeps_its_documents_forever() {
            stubRequestLifecycle(null);

            prepare().test().awaitDone(10, SECONDS).assertComplete();

            elasticsearch.verify(0, putRequestedFor(urlEqualTo(REQUEST_LIFECYCLE)));
            assertThat(messages(Level.ERROR))
                .singleElement()
                .satisfies(message -> assertThat(message).contains("[gravitee-request]"));
        }

        @Test
        void should_apply_a_shorter_retention_when_decreasing_is_allowed() {
            configuration.setDataRetentionAllowDecrease(true);
            stubRequestLifecycle("90d");

            prepare().test().awaitDone(10, SECONDS).assertComplete();

            elasticsearch.verify(
                putRequestedFor(urlEqualTo(REQUEST_LIFECYCLE)).withRequestBody(equalToJson("{\"data_retention\":\"45d\"}"))
            );
            assertThat(messages(Level.ERROR)).isEmpty();
        }

        @Test
        void should_leave_a_data_stream_that_already_has_the_retention() {
            stubRequestLifecycle("1080h");

            prepare().test().awaitDone(10, SECONDS).assertComplete();

            elasticsearch.verify(0, putRequestedFor(urlEqualTo(REQUEST_LIFECYCLE)));
        }

        @Test
        void should_retry_and_name_the_missing_privilege_when_the_key_cannot_change_the_lifecycle() {
            elasticsearch.stubFor(
                put(urlEqualTo(REQUEST_LIFECYCLE)).willReturn(
                    aResponse()
                        .withStatus(403)
                        .withBody("{\"error\":{\"type\":\"security_exception\",\"reason\":\"unauthorized\"},\"status\":403}")
                )
            );
            stubRequestLifecycle("30d");

            final TestObserver<Void> setup = prepare().test();

            await()
                .atMost(Duration.ofSeconds(10))
                .until(() ->
                    messages(Level.WARN)
                        .stream()
                        .anyMatch(message -> message.contains("manage_data_stream_lifecycle"))
                );
            setup.dispose();
        }
    }

    private Completable prepare() {
        final FreeMarkerComponent freeMarkerComponent = context.getBean(FreeMarkerComponent.class);
        return new ServerlessIndexPreparer(
            configuration,
            context.getBean(PipelineConfiguration.class),
            freeMarkerComponent,
            context.getBean(Client.class),
            ServerlessRetention.of(configuration),
            APIM_VERSION
        ).prepare();
    }

    private static String managedTemplate(String apimVersion, String retention) {
        return """
        {"index_patterns":["gravitee-request*"],"data_stream":{},"priority":500,
         "_meta":{"managed_by":"gravitee-apim","apim_version":"%s","data_retention":"%s"}}
        """.formatted(apimVersion, retention);
    }

    private static void stubRequestTemplate(String indexTemplate) {
        elasticsearch.stubFor(
            get(urlEqualTo(REQUEST_TEMPLATE)).willReturn(
                okJson("{\"index_templates\":[{\"name\":\"gravitee-request\",\"index_template\":" + indexTemplate + "}]}")
            )
        );
    }

    private static void stubRequestLifecycle(String retention) {
        final String lifecycle = retention == null ? "{\"enabled\":true}" : "{\"enabled\":true,\"data_retention\":\"" + retention + "\"}";
        elasticsearch.stubFor(
            get(urlEqualTo(REQUEST_LIFECYCLE)).willReturn(
                okJson("{\"data_streams\":[{\"name\":\"gravitee-request\",\"lifecycle\":" + lifecycle + "}]}")
            )
        );
    }

    private List<String> messages(Level level) {
        return logs.list
            .stream()
            .filter(event -> event.getLevel() == level)
            .filter(event -> event.getLoggerName().startsWith("io.gravitee.apim.reporter.elasticsearch"))
            .map(ILoggingEvent::getFormattedMessage)
            .toList();
    }
}

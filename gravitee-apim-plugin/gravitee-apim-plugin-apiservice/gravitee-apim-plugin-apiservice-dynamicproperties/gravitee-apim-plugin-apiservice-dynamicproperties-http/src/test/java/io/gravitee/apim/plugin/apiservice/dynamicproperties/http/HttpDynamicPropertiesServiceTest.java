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
package io.gravitee.apim.plugin.apiservice.dynamicproperties.http;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static testhelpers.Fixtures.MY_API;
import static testhelpers.Fixtures.apiWithDynamicPropertiesEnabled;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.gravitee.apim.rest.api.common.apiservices.DefaultManagementDeploymentContext;
import io.gravitee.apim.rest.api.common.apiservices.events.DynamicPropertiesEvent;
import io.gravitee.apim.rest.api.common.apiservices.events.ManagementApiServiceEvent;
import io.gravitee.common.cron.CronTrigger;
import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.event.impl.EventManagerImpl;
import io.gravitee.common.http.HttpHeader;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.gateway.reactive.api.helper.PluginConfigurationHelper;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.api.cluster.Member;
import io.gravitee.node.plugin.cluster.standalone.StandaloneMember;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import io.vertx.junit5.VertxExtension;
import io.vertx.rxjava3.core.Vertx;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import testhelpers.Assertions;
import testhelpers.Assertions.ScheduledJobAssertions;
import testhelpers.Fixtures;
import testhelpers.TestEventListener;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith({ VertxExtension.class, MockitoExtension.class })
class HttpDynamicPropertiesServiceTest {

    /**
     * JOLT transformation that extracts the json keys of a "props" object to a list of properties:
     * {
     *   "props": {
     *      "key1": "value 1",
     *      "key2": "value 2"
     *   }
     * }
     * Will generate a list of 2 properties: [<key1, value 1>, <key2, value 2>]
     */
    public static final String EXTRACT_JSON_KEYS_TRANSFORMATION =
        "[ { \"operation\": \"shift\", \"spec\": { \"props\": { \"*\": {  \"$\": \"[#2].key\", \"@\": \"[#2].value\"}}}}]";
    public static final String X_HEADER = "X-Header";
    public static final String HEADER_VALUE = "header-value";

    private static final Instant INSTANT_NOW = Instant.parse("2024-02-01T10:15:12Z");

    @RegisterExtension
    static WireMockExtension wiremock = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private GenericApplicationContext applicationContext;
    private EventManager eventManager;
    private ObjectMapper objectMapper;
    private TestScheduler testScheduler;

    @Mock
    private ClusterManager clusterManager;

    @BeforeEach
    void setUp(io.vertx.core.Vertx vertx) {
        lenient().when(clusterManager.self()).thenReturn(new StandaloneMember());
        // Prepare real EventManager and ApplicationContext
        eventManager = new EventManagerImpl();
        objectMapper = new ObjectMapper();
        PluginConfigurationHelper pluginConfigurationHelper = new PluginConfigurationHelper(null, objectMapper);
        applicationContext = new GenericApplicationContext();
        final ConfigurableApplicationContext configurableApplicationContext = applicationContext;
        final ConfigurableListableBeanFactory beanFactory = configurableApplicationContext.getBeanFactory();
        beanFactory.registerSingleton("eventManager", eventManager);
        beanFactory.registerSingleton("pluginConfigurationHelper", pluginConfigurationHelper);
        beanFactory.registerSingleton("vertx", Vertx.newInstance(vertx));
        beanFactory.registerSingleton("configuration", Fixtures.emptyNodeConfiguration());
        beanFactory.registerSingleton("clusterManager", clusterManager);
        applicationContext.refresh();

        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
        testScheduler = new TestScheduler(INSTANT_NOW.toEpochMilli(), TimeUnit.MILLISECONDS);
        RxJavaPlugins.setComputationSchedulerHandler(s -> testScheduler);
    }

    @AfterEach
    void afterEach() {
        RxJavaPlugins.reset();
    }

    @Nested
    class ErrorCases {

        @Test
        void should_not_start_on_invalid_configuration() {
            Api api = Fixtures.apiWithDynamicPropertiesEnabled();
            api.getServices().getDynamicProperty().setConfiguration("invalid configuration");
            final HttpDynamicPropertiesService cut = buildServiceFor(api);

            cut
                .start()
                .test()
                .assertError(e ->
                    e instanceof IllegalArgumentException &&
                    e.getMessage().contains("Unable to start http-dynamic-properties service for api: [" + MY_API + "]")
                );
        }

        @Test
        void should_not_publish_dynamic_properties_on_invalid_url() {
            Api api = Fixtures.apiWithDynamicPropertiesEnabled();
            final String badUrl = String.format("h://localhost:%d/propertiesBackend", wiremock.getPort());
            final HttpDynamicPropertiesServiceConfiguration configuration = HttpDynamicPropertiesServiceConfiguration
                .builder()
                .schedule("*/5 * * * * *")
                .url(badUrl)
                .transformation(EXTRACT_JSON_KEYS_TRANSFORMATION)
                .method(HttpMethod.GET)
                .headers(List.of(new HttpHeader(X_HEADER, HEADER_VALUE)))
                .build();
            Fixtures.configureDynamicPropertiesForApi(configuration, api, objectMapper);
            final HttpDynamicPropertiesService cut = buildServiceFor(api);

            final Logger logger = (Logger) LoggerFactory.getLogger(HttpDynamicPropertiesService.class);
            final ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
            listAppender.start();
            logger.addAppender(listAppender);

            // Reduce the number to ease the test
            HttpDynamicPropertiesService.setLogErrorCount(2);

            // Start the service
            cut.start().test().assertComplete().assertNoErrors();

            // Wait for the first http call
            testScheduler.advanceTimeBy(5_000, TimeUnit.MILLISECONDS);

            ScheduledJobAssertions.assertScheduledJobIsRunning(cut.scheduledJob);

            await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    wiremock.verify(0, getRequestedFor(urlPathEqualTo("/propertiesBackend")).withHeader(X_HEADER, equalTo(HEADER_VALUE)));
                });

            TestEventListener.with(eventManager).completeImmediatly().test().assertNoValues().assertComplete();

            List<ILoggingEvent> logList = listAppender.list.stream().filter(log -> log.getLevel().equals(Level.WARN)).toList();
            assertThat(logList)
                .hasSize(1)
                .element(0)
                .extracting(ILoggingEvent::getFormattedMessage)
                .isEqualTo("Unable to run dynamic properties for api [my-api] on URL [" + badUrl + "].");

            // Wait for the retry
            testScheduler.advanceTimeBy(5_000, TimeUnit.MILLISECONDS);

            ScheduledJobAssertions.assertScheduledJobIsRunning(cut.scheduledJob);

            await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    wiremock.verify(0, getRequestedFor(urlPathEqualTo("/propertiesBackend")).withHeader(X_HEADER, equalTo(HEADER_VALUE)));
                });

            // As LOG_ERROR_COUNT has been set to 2, the second call will trigger the other error message
            logList = listAppender.list.stream().filter(log -> log.getLevel().equals(Level.WARN)).toList();
            assertThat(logList)
                .hasSize(2)
                .element(1)
                .extracting(ILoggingEvent::getFormattedMessage)
                .isEqualTo(
                    "Unable to run dynamic properties for api [my-api] on URL [" +
                    badUrl +
                    "] (times: 2, see previous log report for details)."
                );

            cut.stop().test().awaitDone(10, TimeUnit.SECONDS).assertComplete();

            ScheduledJobAssertions.assertScheduledJobIsDisposed(cut.scheduledJob);
        }
    }

    @Nested
    class ServiceStarted {

        @Test
        void should_publish_computed_dynamic_properties() {
            Api api = Fixtures.apiWithDynamicPropertiesEnabled();
            final HttpDynamicPropertiesServiceConfiguration configuration = HttpDynamicPropertiesServiceConfiguration
                .builder()
                .schedule("*/5 * * * * *")
                .url(String.format("http://localhost:%d/propertiesBackend", wiremock.getPort()))
                .transformation(EXTRACT_JSON_KEYS_TRANSFORMATION)
                .method(HttpMethod.GET)
                .headers(List.of(new HttpHeader(X_HEADER, HEADER_VALUE)))
                .build();
            Fixtures.configureDynamicPropertiesForApi(configuration, api, objectMapper);
            final HttpDynamicPropertiesService cut = buildServiceFor(api);

            wiremock.stubFor(
                get("/propertiesBackend")
                    .willReturn(
                        ok(
                            Fixtures.backendResponseForProperties(
                                List.of(
                                    new Fixtures.BackendProperty("key1", "initial val 1"),
                                    new Fixtures.BackendProperty("key2", "initial val 2")
                                ),
                                objectMapper
                            )
                        )
                    )
            );

            var eventObs = TestEventListener.with(eventManager).completeAfter(1).test();

            // Start the service
            cut.start().test().assertComplete().assertNoErrors();

            // Wait for the first http call
            testScheduler.advanceTimeBy(5_000, TimeUnit.MILLISECONDS);

            ScheduledJobAssertions.assertScheduledJobIsRunning(cut.scheduledJob);

            eventObs
                .awaitDone(30, TimeUnit.SECONDS)
                .assertValueCount(1)
                .assertValue(propertyEvent -> {
                    Assertions.PropertyEventAssertions
                        .assertThatEvent(propertyEvent)
                        .contains(
                            List.of(
                                Property.builder().key("key1").value("initial val 1").dynamic(true).encrypted(false).build(),
                                Property.builder().key("key2").value("initial val 2").dynamic(true).encrypted(false).build()
                            )
                        );
                    return true;
                })
                .assertComplete();

            wiremock.verify(getRequestedFor(urlPathEqualTo("/propertiesBackend")).withHeader(X_HEADER, equalTo(HEADER_VALUE)));

            cut.stop().test().awaitDone(10, TimeUnit.SECONDS).assertComplete();

            ScheduledJobAssertions.assertScheduledJobIsDisposed(cut.scheduledJob);
        }

        @Test
        void should_not_publish_dynamic_properties_if_backend_does_not_answer_with_ok_200() {
            Api api = Fixtures.apiWithDynamicPropertiesEnabled();
            final HttpDynamicPropertiesServiceConfiguration configuration = HttpDynamicPropertiesServiceConfiguration
                .builder()
                .schedule("*/5 * * * * *")
                .url(String.format("http://localhost:%d/propertiesBackend", wiremock.getPort()))
                .transformation(EXTRACT_JSON_KEYS_TRANSFORMATION)
                .method(HttpMethod.GET)
                .headers(List.of(new HttpHeader(X_HEADER, HEADER_VALUE)))
                .build();
            Fixtures.configureDynamicPropertiesForApi(configuration, api, objectMapper);
            final HttpDynamicPropertiesService cut = buildServiceFor(api);

            wiremock.stubFor(
                get("/propertiesBackend")
                    .willReturn(
                        aResponse()
                            .withStatus(201)
                            .withBody(
                                Fixtures.backendResponseForProperties(
                                    List.of(
                                        new Fixtures.BackendProperty("key1", "initial val 1"),
                                        new Fixtures.BackendProperty("key2", "initial val 2")
                                    ),
                                    objectMapper
                                )
                            )
                    )
            );

            // Start the service
            cut.start().test().assertComplete().assertNoErrors();

            // Wait for the first http call
            testScheduler.advanceTimeBy(5_000, TimeUnit.MILLISECONDS);

            ScheduledJobAssertions.assertScheduledJobIsRunning(cut.scheduledJob);

            await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    wiremock.verify(1, getRequestedFor(urlPathEqualTo("/propertiesBackend")).withHeader(X_HEADER, equalTo(HEADER_VALUE)));
                });

            TestEventListener.with(eventManager).completeImmediatly().test().assertNoValues().assertComplete();

            cut.stop().test().awaitDone(10, TimeUnit.SECONDS).assertComplete();

            ScheduledJobAssertions.assertScheduledJobIsDisposed(cut.scheduledJob);
        }

        @Test
        void should_publish_dynamic_properties_multiple_times() {
            Api api = Fixtures.apiWithDynamicPropertiesEnabled();
            final HttpDynamicPropertiesServiceConfiguration configuration = HttpDynamicPropertiesServiceConfiguration
                .builder()
                .schedule("*/5 * * * * *")
                .url(String.format("http://localhost:%d/propertiesBackend", wiremock.getPort()))
                .transformation(EXTRACT_JSON_KEYS_TRANSFORMATION)
                .method(HttpMethod.GET)
                .headers(List.of(new HttpHeader(X_HEADER, HEADER_VALUE)))
                .build();
            Fixtures.configureDynamicPropertiesForApi(configuration, api, objectMapper);
            final HttpDynamicPropertiesService cut = buildServiceFor(api);

            wiremock.stubFor(
                get("/propertiesBackend")
                    .inScenario("multiple")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(
                        ok(
                            Fixtures.backendResponseForProperties(
                                List.of(
                                    new Fixtures.BackendProperty("key1", "initial val 1"),
                                    new Fixtures.BackendProperty("key2", "initial val 2")
                                ),
                                objectMapper
                            )
                        )
                    )
                    .willSetStateTo("firstCall")
            );
            wiremock.stubFor(
                get("/propertiesBackend")
                    .inScenario("multiple")
                    .whenScenarioStateIs("firstCall")
                    .willReturn(
                        ok(
                            Fixtures.backendResponseForProperties(
                                List.of(
                                    new Fixtures.BackendProperty("key1-updated", "updated val 1"),
                                    new Fixtures.BackendProperty("key2-updated", "updated val 2"),
                                    new Fixtures.BackendProperty("key3", "initial val 3")
                                ),
                                objectMapper
                            )
                        )
                    )
                    .willSetStateTo("secondCall")
            );
            wiremock.stubFor(
                get("/propertiesBackend")
                    .inScenario("multiple")
                    .whenScenarioStateIs("secondCall")
                    .willReturn(
                        ok(Fixtures.backendResponseForProperties(List.of(new Fixtures.BackendProperty("key4", "value 4")), objectMapper))
                    )
                    .willSetStateTo("thirdCall")
            );
            wiremock.stubFor(
                get("/propertiesBackend")
                    .inScenario("multiple")
                    .whenScenarioStateIs("thirdCall")
                    .willReturn(ok(Fixtures.backendResponseForProperties(List.of(), objectMapper)))
                    .willSetStateTo("fourthCall")
            );

            final TestObserver<Event<ManagementApiServiceEvent, DynamicPropertiesEvent>> eventObs = TestEventListener
                .with(eventManager)
                .completeAfter(4)
                .test();

            // Start the service
            cut.start().test().assertComplete().assertNoErrors();

            // Wait for the first http call
            advanceTimeBy(5_000, cut, configuration);

            // Ensure first event has been published
            eventObs.awaitCount(1);

            // Wait for the second http call
            advanceTimeBy(5_000, cut, configuration);
            // Ensure second event has been published
            eventObs.awaitCount(2);

            // Wait for the third http call
            advanceTimeBy(5_000, cut, configuration);
            // Ensure third event has been published
            eventObs.awaitCount(3);

            // Wait for the fourth http call
            advanceTimeBy(5_000, cut, configuration);
            // Ensure fourth event has been published
            eventObs.awaitCount(4);

            ScheduledJobAssertions.assertScheduledJobIsRunning(cut.scheduledJob);

            eventObs
                .awaitDone(10, TimeUnit.SECONDS)
                .assertValueCount(4)
                .assertValueAt(
                    0,
                    propertyEvent -> {
                        Assertions.PropertyEventAssertions
                            .assertThatEvent(propertyEvent)
                            .contains(
                                List.of(
                                    Property.builder().key("key1").value("initial val 1").dynamic(true).encrypted(false).build(),
                                    Property.builder().key("key2").value("initial val 2").dynamic(true).encrypted(false).build()
                                )
                            );
                        return true;
                    }
                )
                .assertValueAt(
                    1,
                    propertyEvent -> {
                        Assertions.PropertyEventAssertions
                            .assertThatEvent(propertyEvent)
                            .contains(
                                List.of(
                                    Property.builder().key("key1-updated").value("updated val 1").dynamic(true).encrypted(false).build(),
                                    Property.builder().key("key2-updated").value("updated val 2").dynamic(true).encrypted(false).build(),
                                    Property.builder().key("key3").value("initial val 3").dynamic(true).encrypted(false).build()
                                )
                            );
                        return true;
                    }
                )
                .assertValueAt(
                    2,
                    propertyEvent -> {
                        Assertions.PropertyEventAssertions
                            .assertThatEvent(propertyEvent)
                            .contains(List.of(Property.builder().key("key4").value("value 4").dynamic(true).encrypted(false).build()));
                        return true;
                    }
                )
                .assertValueAt(
                    3,
                    propertyEvent -> {
                        Assertions.PropertyEventAssertions.assertThatEvent(propertyEvent).contains(List.of());
                        return true;
                    }
                )
                .assertComplete();

            wiremock.verify(getRequestedFor(urlPathEqualTo("/propertiesBackend")).withHeader(X_HEADER, equalTo(HEADER_VALUE)));

            cut.stop().test().awaitDone(10, TimeUnit.SECONDS).assertComplete();

            ScheduledJobAssertions.assertScheduledJobIsDisposed(cut.scheduledJob);
        }

        @Test
        void should_not_publish_dynamic_properties_if_secondary_node_then_start_publish_if_primary() {
            Api api = Fixtures.apiWithDynamicPropertiesEnabled();
            final HttpDynamicPropertiesServiceConfiguration configuration = HttpDynamicPropertiesServiceConfiguration
                .builder()
                .schedule("*/5 * * * * *")
                .url(String.format("http://localhost:%d/propertiesBackend", wiremock.getPort()))
                .transformation(EXTRACT_JSON_KEYS_TRANSFORMATION)
                .method(HttpMethod.GET)
                .headers(List.of(new HttpHeader(X_HEADER, HEADER_VALUE)))
                .build();
            Fixtures.configureDynamicPropertiesForApi(configuration, api, objectMapper);
            final HttpDynamicPropertiesService cut = buildServiceFor(api);

            wiremock.stubFor(
                get("/propertiesBackend")
                    .willReturn(
                        ok(
                            Fixtures.backendResponseForProperties(
                                List.of(
                                    new Fixtures.BackendProperty("key1", "initial val 1"),
                                    new Fixtures.BackendProperty("key2", "initial val 2")
                                ),
                                objectMapper
                            )
                        )
                    )
            );

            var eventObs = TestEventListener.with(eventManager).completeAfter(1).test();

            Member member = spy(new StandaloneMember());
            when(member.primary()).thenReturn(false);
            when(clusterManager.self()).thenReturn(member);

            // Start the service
            cut.start().test().assertComplete().assertNoErrors();

            // Wait for the first http call
            testScheduler.advanceTimeBy(10_000, TimeUnit.MILLISECONDS);
            when(member.primary()).thenReturn(true);
            testScheduler.advanceTimeBy(5_000, TimeUnit.MILLISECONDS);

            ScheduledJobAssertions.assertScheduledJobIsRunning(cut.scheduledJob);

            eventObs
                .awaitDone(10, TimeUnit.SECONDS)
                .assertValueCount(1)
                .assertValue(propertyEvent -> {
                    Assertions.PropertyEventAssertions
                        .assertThatEvent(propertyEvent)
                        .contains(
                            List.of(
                                Property.builder().key("key1").value("initial val 1").dynamic(true).encrypted(false).build(),
                                Property.builder().key("key2").value("initial val 2").dynamic(true).encrypted(false).build()
                            )
                        );
                    return true;
                })
                .assertComplete();

            wiremock.verify(1, getRequestedFor(urlPathEqualTo("/propertiesBackend")).withHeader(X_HEADER, equalTo(HEADER_VALUE)));

            cut.stop().test().awaitDone(10, TimeUnit.SECONDS).assertComplete();

            ScheduledJobAssertions.assertScheduledJobIsDisposed(cut.scheduledJob);
        }
    }

    @Nested
    class UpdateCases {

        @Test
        void should_start_service_if_not_done_and_publish_computed_dynamic_properties() {
            Api api = Fixtures.apiWithDynamicPropertiesEnabled();
            final HttpDynamicPropertiesServiceConfiguration configuration = HttpDynamicPropertiesServiceConfiguration
                .builder()
                .schedule("*/5 * * * * *")
                .url(String.format("http://localhost:%d/propertiesBackend", wiremock.getPort()))
                .transformation(EXTRACT_JSON_KEYS_TRANSFORMATION)
                .method(HttpMethod.GET)
                .headers(List.of(new HttpHeader(X_HEADER, HEADER_VALUE)))
                .build();
            Fixtures.configureDynamicPropertiesForApi(configuration, api, objectMapper);
            final HttpDynamicPropertiesService cut = buildServiceFor(api);

            wiremock.stubFor(
                get("/propertiesBackend")
                    .willReturn(
                        ok(
                            Fixtures.backendResponseForProperties(
                                List.of(
                                    new Fixtures.BackendProperty("key1", "initial val 1"),
                                    new Fixtures.BackendProperty("key2", "initial val 2")
                                ),
                                objectMapper
                            )
                        )
                    )
            );

            var eventObs = TestEventListener.with(eventManager).completeAfter(1).test();

            // Start the service
            cut.update(api).test().assertComplete().assertNoErrors();

            // Wait for the first http call
            testScheduler.advanceTimeBy(5_000, TimeUnit.MILLISECONDS);

            ScheduledJobAssertions.assertScheduledJobIsRunning(cut.scheduledJob);

            eventObs
                .awaitDone(10, TimeUnit.SECONDS)
                .assertValueCount(1)
                .assertValue(propertyEvent -> {
                    Assertions.PropertyEventAssertions
                        .assertThatEvent(propertyEvent)
                        .contains(
                            List.of(
                                Property.builder().key("key1").value("initial val 1").dynamic(true).encrypted(false).build(),
                                Property.builder().key("key2").value("initial val 2").dynamic(true).encrypted(false).build()
                            )
                        );
                    return true;
                })
                .assertComplete();

            wiremock.verify(getRequestedFor(urlPathEqualTo("/propertiesBackend")).withHeader(X_HEADER, equalTo(HEADER_VALUE)));

            cut.stop().test().awaitDone(10, TimeUnit.SECONDS).assertComplete();

            ScheduledJobAssertions.assertScheduledJobIsDisposed(cut.scheduledJob);
        }

        @Test
        void should_publish_dynamic_with_a_configuration_update() {
            Api api = Fixtures.apiWithDynamicPropertiesEnabled();
            final HttpDynamicPropertiesServiceConfiguration configuration = HttpDynamicPropertiesServiceConfiguration
                .builder()
                .schedule("*/5 * * * * *")
                .url(String.format("http://localhost:%d/propertiesBackend", wiremock.getPort()))
                .transformation(EXTRACT_JSON_KEYS_TRANSFORMATION)
                .method(HttpMethod.GET)
                .headers(List.of(new HttpHeader(X_HEADER, HEADER_VALUE)))
                .build();
            Fixtures.configureDynamicPropertiesForApi(configuration, api, objectMapper);
            final HttpDynamicPropertiesService cut = buildServiceFor(api);

            wiremock.stubFor(
                get("/propertiesBackend")
                    .inScenario("multiple")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(
                        ok(
                            Fixtures.backendResponseForProperties(
                                List.of(
                                    new Fixtures.BackendProperty("key1", "initial val 1"),
                                    new Fixtures.BackendProperty("key2", "initial val 2")
                                ),
                                objectMapper
                            )
                        )
                    )
                    .willSetStateTo("firstCall")
            );
            wiremock.stubFor(
                get("/propertiesOtherBackend")
                    .inScenario("multiple")
                    .whenScenarioStateIs("firstCall")
                    .willReturn(
                        ok(
                            Fixtures.backendResponseForProperties(
                                List.of(
                                    new Fixtures.BackendProperty("key1-otherbackend", "value 1"),
                                    new Fixtures.BackendProperty("key2-otherbackend", "value 2")
                                ),
                                objectMapper
                            )
                        )
                    )
                    .willSetStateTo("secondCall")
            );

            final TestObserver<Event<ManagementApiServiceEvent, DynamicPropertiesEvent>> eventObs = TestEventListener
                .with(eventManager)
                .completeAfter(2)
                .test();

            // Start the service
            cut.start().test().assertComplete().assertNoErrors();

            // Wait for the first http call
            advanceTimeBy(5_000, cut, configuration);

            // Ensure first event has been published
            eventObs.awaitCount(1);

            final HttpDynamicPropertiesServiceConfiguration updatedConfiguration = configuration
                .toBuilder()
                .url(String.format("http://localhost:%d/propertiesOtherBackend", wiremock.getPort()))
                .build();
            // create new api to not share same object references
            final Api updatedApi = apiWithDynamicPropertiesEnabled();
            Fixtures.configureDynamicPropertiesForApi(updatedConfiguration, updatedApi, objectMapper);
            cut.update(updatedApi).test().awaitDone(10, TimeUnit.SECONDS).assertComplete().assertNoErrors();

            // Wait for the second http call
            advanceTimeBy(5_000, cut, configuration);
            // Ensure second event has been published
            eventObs.awaitCount(2);

            ScheduledJobAssertions.assertScheduledJobIsRunning(cut.scheduledJob);

            eventObs
                .awaitDone(10, TimeUnit.SECONDS)
                .assertValueCount(2)
                .assertValueAt(
                    0,
                    propertyEvent -> {
                        Assertions.PropertyEventAssertions
                            .assertThatEvent(propertyEvent)
                            .contains(
                                List.of(
                                    Property.builder().key("key1").value("initial val 1").dynamic(true).encrypted(false).build(),
                                    Property.builder().key("key2").value("initial val 2").dynamic(true).encrypted(false).build()
                                )
                            );
                        return true;
                    }
                )
                .assertValueAt(
                    1,
                    propertyEvent -> {
                        Assertions.PropertyEventAssertions
                            .assertThatEvent(propertyEvent)
                            .contains(
                                List.of(
                                    Property.builder().key("key1-otherbackend").value("value 1").dynamic(true).encrypted(false).build(),
                                    Property.builder().key("key2-otherbackend").value("value 2").dynamic(true).encrypted(false).build()
                                )
                            );
                        return true;
                    }
                )
                .assertComplete();

            wiremock.verify(getRequestedFor(urlPathEqualTo("/propertiesBackend")).withHeader(X_HEADER, equalTo(HEADER_VALUE)));
            wiremock.verify(getRequestedFor(urlPathEqualTo("/propertiesOtherBackend")).withHeader(X_HEADER, equalTo(HEADER_VALUE)));

            cut.stop().test().awaitDone(10, TimeUnit.SECONDS).assertComplete();

            ScheduledJobAssertions.assertScheduledJobIsDisposed(cut.scheduledJob);
        }

        @Test
        void should_stop_dynamic_with_a_configuration_update() {
            Api api = Fixtures.apiWithDynamicPropertiesEnabled();
            final HttpDynamicPropertiesServiceConfiguration configuration = HttpDynamicPropertiesServiceConfiguration
                .builder()
                .schedule("*/5 * * * * *")
                .url(String.format("http://localhost:%d/propertiesBackend", wiremock.getPort()))
                .transformation(EXTRACT_JSON_KEYS_TRANSFORMATION)
                .method(HttpMethod.GET)
                .headers(List.of(new HttpHeader(X_HEADER, HEADER_VALUE)))
                .build();
            Fixtures.configureDynamicPropertiesForApi(configuration, api, objectMapper);
            final HttpDynamicPropertiesService cut = buildServiceFor(api);

            wiremock.stubFor(
                get("/propertiesBackend")
                    .inScenario("multiple")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(
                        ok(
                            Fixtures.backendResponseForProperties(
                                List.of(
                                    new Fixtures.BackendProperty("key1", "initial val 1"),
                                    new Fixtures.BackendProperty("key2", "initial val 2")
                                ),
                                objectMapper
                            )
                        )
                    )
                    .willSetStateTo("firstCall")
            );

            final TestObserver<Event<ManagementApiServiceEvent, DynamicPropertiesEvent>> eventObs = TestEventListener
                .with(eventManager)
                .completeAfter(1)
                .test();

            // Start the service
            cut.start().test().assertComplete().assertNoErrors();

            // Wait for the first http call
            advanceTimeBy(5_000, cut, configuration);

            // Ensure first event has been published
            eventObs.awaitCount(1);

            // create new api to not share same object references
            final Api updatedApi = apiWithDynamicPropertiesEnabled();
            updatedApi.getServices().getDynamicProperty().setEnabled(false);
            Fixtures.configureDynamicPropertiesForApi(configuration, updatedApi, objectMapper);
            cut.update(updatedApi).test().awaitDone(10, TimeUnit.SECONDS).assertComplete().assertNoErrors();

            ScheduledJobAssertions.assertScheduledJobIsDisposed(cut.scheduledJob);

            eventObs
                .awaitDone(10, TimeUnit.SECONDS)
                .assertValueCount(1)
                .assertValue(propertyEvent -> {
                    Assertions.PropertyEventAssertions
                        .assertThatEvent(propertyEvent)
                        .contains(
                            List.of(
                                Property.builder().key("key1").value("initial val 1").dynamic(true).encrypted(false).build(),
                                Property.builder().key("key2").value("initial val 2").dynamic(true).encrypted(false).build()
                            )
                        );
                    return true;
                })
                .assertComplete();

            wiremock.verify(getRequestedFor(urlPathEqualTo("/propertiesBackend")).withHeader(X_HEADER, equalTo(HEADER_VALUE)));
        }
    }

    /**
     * This method simulates an advance in time by a specific delay.
     * First, it uses the {@link TestScheduler#advanceTimeBy(long, TimeUnit)} to change the time for the reactive chain
     * Then, it overrides the Clock to synchronize it with the current {@link TestScheduler}
     * Finally, it overrides the {@link HttpDynamicPropertiesService#cronTrigger} with a new instance, so that it builds a context with the modified Clock.
     *
     * @param delay         the delay to advance in time
     * @param cut           the class under test
     * @param configuration the configuration needed to build the {@link CronTrigger}
     */
    private void advanceTimeBy(final int delay, HttpDynamicPropertiesService cut, HttpDynamicPropertiesServiceConfiguration configuration) {
        testScheduler.advanceTimeBy(delay, TimeUnit.MILLISECONDS);
        TimeProvider.overrideClock(Clock.fixed(Instant.ofEpochMilli(testScheduler.now(TimeUnit.MILLISECONDS)), ZoneId.systemDefault()));
        cut.cronTrigger = new CronTrigger(configuration.getSchedule());
    }

    private HttpDynamicPropertiesService buildServiceFor(Api api) {
        return new HttpDynamicPropertiesService(new DefaultManagementDeploymentContext(api, applicationContext));
    }
}

/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.services.sync.synchronizer;

import static io.gravitee.definition.model.ApiBuilder.anApiV1;
import static io.gravitee.definition.model.ApiBuilder.anApiV2;
import static io.gravitee.definition.model.v4.ApiBuilder.aSyncApiV4;
import static io.gravitee.gateway.services.sync.SyncManager.TIMEFRAME_AFTER_DELAY;
import static io.gravitee.gateway.services.sync.SyncManager.TIMEFRAME_BEFORE_DELAY;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Rule;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.handlers.api.manager.ActionOnApi;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.services.sync.builder.RepositoryApiBuilder;
import io.gravitee.gateway.services.sync.cache.ApiKeysCacheService;
import io.gravitee.gateway.services.sync.cache.SubscriptionsCacheService;
import io.gravitee.gateway.services.sync.synchronizer.api.EventToReactableApiAdapter;
import io.gravitee.gateway.services.sync.synchronizer.api.PlanFetcher;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.model.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class ApiSynchronizerTest {

    private static final Integer BULK_SIZE = 5;
    private static final List<String> ENVIRONMENTS = Arrays.asList("DEFAULT", "OTHER_ENV");
    private static final String ENVIRONMENT_ID = "env#1";
    private static final String ORGANIZATION_ID = "org#1";
    private static final String ENVIRONMENT_HRID = "default-env";
    private static final String ORGANIZATION_HRID = "default-org";
    private static final String API_ID = "api-test";

    private ApiSynchronizer apiSynchronizer;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private ApiManager apiManager;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private ApiKeysCacheService apiKeysCacheService;

    @Mock
    private SubscriptionsCacheService subscriptionsCacheService;

    private ObjectMapper objectMapper;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private GatewayConfiguration gatewayConfiguration;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        var planFetcher = new PlanFetcher(objectMapper, planRepository);
        var eventToReactableApiAdapter = new EventToReactableApiAdapter(objectMapper, environmentRepository, organizationRepository);

        apiSynchronizer =
            new ApiSynchronizer(
                eventRepository,
                Executors.newFixedThreadPool(1),
                BULK_SIZE,
                apiKeysCacheService,
                subscriptionsCacheService,
                subscriptionService,
                apiManager,
                eventToReactableApiAdapter,
                planFetcher,
                gatewayConfiguration
            );
        lenient().when(apiManager.requiredActionFor(any())).thenReturn(ActionOnApi.DEPLOY);
        lenient().when(gatewayConfiguration.hasMatchingTags(any())).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        reset(eventRepository, environmentRepository, organizationRepository);
    }

    @Nested
    class InitialSynchronization {

        @Test
        void should_fetch_only_api_register_events() throws Exception {
            // 6 events to return in 2 calls because the bulkSize is 5
            when(eventRepository.searchLatest(any(), any(), anyLong(), anyLong()))
                .thenReturn(
                    List.of(
                        anEvent(anApiV2().id("api-1").build(), EventType.PUBLISH_API),
                        anEvent(anApiV2().id("api-2").build(), EventType.START_API),
                        anEvent(anApiV2().id("api-3").build(), EventType.PUBLISH_API),
                        anEvent(anApiV2().id("api-4").build(), EventType.START_API),
                        anEvent(anApiV2().id("api-5").build(), EventType.PUBLISH_API)
                    )
                )
                .thenReturn(List.of(anEvent(anApiV2().id("api-6").build(), EventType.START_API)));

            var nextLastRefresh = System.currentTimeMillis();

            apiSynchronizer.synchronize(-1L, nextLastRefresh, ENVIRONMENTS);

            var criteriaCaptor = ArgumentCaptor.forClass(EventCriteria.class);
            var eventPropertiesCaptor = ArgumentCaptor.forClass(Event.EventProperties.class);
            var pageCaptor = ArgumentCaptor.forClass(Long.class);
            var pageSizeCaptor = ArgumentCaptor.forClass(Long.class);
            verify(eventRepository, times(2))
                .searchLatest(criteriaCaptor.capture(), eventPropertiesCaptor.capture(), pageCaptor.capture(), pageSizeCaptor.capture());

            SoftAssertions.assertSoftly(
                softly -> {
                    var criteria = criteriaCaptor.getValue();
                    softly.assertThat(criteria.getTypes()).containsAll(List.of(EventType.PUBLISH_API, EventType.START_API));
                    softly.assertThat(criteria.getEnvironments()).containsExactlyElementsOf(ENVIRONMENTS);
                    softly.assertThat(criteria.isStrictMode()).isTrue();
                    softly.assertThat(criteria.getFrom()).isZero();
                    softly.assertThat(criteria.getTo()).isEqualTo(nextLastRefresh + TIMEFRAME_AFTER_DELAY);

                    softly.assertThat(eventPropertiesCaptor.getValue()).isEqualTo(Event.EventProperties.API_ID);
                    softly.assertThat(pageSizeCaptor.getValue()).isEqualTo(BULK_SIZE.longValue());
                    softly.assertThat(pageCaptor.getAllValues()).containsExactly(0L, 1L);
                }
            );
        }
    }

    @Nested
    class IncrementalSynchronization {

        @Test
        void should_fetch_api_register_and_unregister_events() throws Exception {
            // 6 events to return in 2 calls because the bulkSize is 5
            when(eventRepository.searchLatest(any(), any(), anyLong(), anyLong()))
                .thenReturn(
                    List.of(
                        anEvent(anApiV2().id(API_ID).build(), EventType.PUBLISH_API),
                        anEvent(anApiV2().id(API_ID).build(), EventType.START_API),
                        anEvent(anApiV2().id(API_ID).build(), EventType.UNPUBLISH_API),
                        anEvent(anApiV2().id(API_ID).build(), EventType.UNPUBLISH_API),
                        anEvent(anApiV2().id(API_ID).build(), EventType.STOP_API)
                    )
                )
                .thenReturn(List.of(anEvent(anApiV2().id(API_ID).build(), EventType.STOP_API)));

            var lastRefreshAt = System.currentTimeMillis() - 5000;
            var nextLastRefresh = System.currentTimeMillis();
            apiSynchronizer.synchronize(lastRefreshAt, nextLastRefresh, ENVIRONMENTS);

            var criteriaCaptor = ArgumentCaptor.forClass(EventCriteria.class);
            var eventPropertiesCaptor = ArgumentCaptor.forClass(Event.EventProperties.class);
            var pageCaptor = ArgumentCaptor.forClass(Long.class);
            var pageSizeCaptor = ArgumentCaptor.forClass(Long.class);
            verify(eventRepository, times(2))
                .searchLatest(criteriaCaptor.capture(), eventPropertiesCaptor.capture(), pageCaptor.capture(), pageSizeCaptor.capture());

            SoftAssertions.assertSoftly(
                softly -> {
                    var criteria = criteriaCaptor.getValue();
                    softly
                        .assertThat(criteria.getTypes())
                        .containsAll(List.of(EventType.PUBLISH_API, EventType.START_API, EventType.UNPUBLISH_API, EventType.STOP_API));
                    softly.assertThat(criteria.getEnvironments()).containsExactlyElementsOf(ENVIRONMENTS);
                    softly.assertThat(criteria.isStrictMode()).isTrue();
                    softly.assertThat(criteria.getFrom()).isEqualTo(lastRefreshAt - TIMEFRAME_BEFORE_DELAY);
                    softly.assertThat(criteria.getTo()).isEqualTo(nextLastRefresh + TIMEFRAME_AFTER_DELAY);

                    softly.assertThat(eventPropertiesCaptor.getValue()).isEqualTo(Event.EventProperties.API_ID);
                    softly.assertThat(pageSizeCaptor.getValue()).isEqualTo(BULK_SIZE.longValue());
                    softly.assertThat(pageCaptor.getAllValues()).containsExactly(0L, 1L);
                }
            );
        }
    }

    static class ApiRegisterEventsProcessingProvider implements ArgumentsProvider {

        @Override
        public Stream<Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                arguments(Named.named("initial synchronization", true)),
                arguments(Named.named("incremental synchronization", false))
            );
        }
    }

    @Nested
    class ApiV2 {

        @Nested
        class RegisterEventsProcessing {

            @ParameterizedTest
            @ArgumentsSource(ApiRegisterEventsProcessingProvider.class)
            void should_process_api_register_events(Boolean initialSync) throws Exception {
                var apiDefinition = anApiV2()
                    .id(API_ID)
                    .plans(List.of(io.gravitee.definition.model.Plan.builder().status("PUBLISHED").build()))
                    .build();
                final Event publishEvent = anEvent(apiDefinition, EventType.PUBLISH_API);

                givenEvents(List.of(publishEvent));
                givenAnOrganizationWithHrid();
                givenAnEnvironmentWithHrid();

                long lastRefreshAt = initialSync ? -1L : System.currentTimeMillis() - 5000;
                apiSynchronizer.synchronize(lastRefreshAt, System.currentTimeMillis(), ENVIRONMENTS);

                ArgumentCaptor<Api> apiCaptor = ArgumentCaptor.forClass(Api.class);
                verify(apiManager).register(apiCaptor.capture());
                SoftAssertions.assertSoftly(
                    softly -> {
                        Api verifyApi = apiCaptor.getValue();
                        softly.assertThat(verifyApi.getId()).isEqualTo(API_ID);
                        softly.assertThat(verifyApi.getEnvironmentId()).isEqualTo(ENVIRONMENT_ID);
                        softly.assertThat(verifyApi.getEnvironmentHrid()).isEqualTo(ENVIRONMENT_HRID);
                        softly.assertThat(verifyApi.getOrganizationId()).isEqualTo(ORGANIZATION_ID);
                        softly.assertThat(verifyApi.getOrganizationHrid()).isEqualTo(ORGANIZATION_HRID);
                    }
                );

                verify(apiKeysCacheService).register(singletonList(new Api(apiDefinition)));
                verify(subscriptionsCacheService).register(singletonList(new Api(apiDefinition)));
                verify(subscriptionService).dispatchFor(singletonList(apiDefinition.getId()));
            }

            @ParameterizedTest
            @ArgumentsSource(ApiRegisterEventsProcessingProvider.class)
            void should_process_api_register_events_with_environment_and_organization_without_hrid(Boolean initialSync) throws Exception {
                var apiDefinition = anApiV2()
                    .id(API_ID)
                    .plans(List.of(io.gravitee.definition.model.Plan.builder().status("PUBLISHED").build()))
                    .build();
                final Event publishEvent = anEvent(apiDefinition, EventType.PUBLISH_API);

                givenEvents(List.of(publishEvent));
                givenAnOrganizationWithoutHrid();
                givenAnEnvironmentWithoutHrid();

                long lastRefreshAt = initialSync ? -1L : System.currentTimeMillis() - 5000;
                apiSynchronizer.synchronize(lastRefreshAt, System.currentTimeMillis(), ENVIRONMENTS);

                ArgumentCaptor<Api> apiCaptor = ArgumentCaptor.forClass(Api.class);
                verify(apiManager).register(apiCaptor.capture());
                SoftAssertions.assertSoftly(
                    softly -> {
                        Api verifyApi = apiCaptor.getValue();
                        softly.assertThat(verifyApi.getEnvironmentHrid()).isNull();
                        softly.assertThat(verifyApi.getOrganizationHrid()).isNull();
                    }
                );
            }

            @ParameterizedTest
            @ArgumentsSource(ApiRegisterEventsProcessingProvider.class)
            void should_register_an_api_with_no_environment(Boolean initialSync) throws Exception {
                var apiDefinition = anApiV2()
                    .id(API_ID)
                    .plans(List.of(io.gravitee.definition.model.Plan.builder().status("PUBLISHED").build()))
                    .build();
                final Event publishEvent = anEvent(apiDefinition, EventType.PUBLISH_API);

                givenEvents(List.of(publishEvent));

                long lastRefreshAt = initialSync ? -1L : System.currentTimeMillis() - 5000;
                apiSynchronizer.synchronize(lastRefreshAt, System.currentTimeMillis(), ENVIRONMENTS);

                ArgumentCaptor<Api> apiCaptor = ArgumentCaptor.forClass(Api.class);
                verify(apiManager).register(apiCaptor.capture());
                SoftAssertions.assertSoftly(
                    softly -> {
                        Api verifyApi = apiCaptor.getValue();
                        softly.assertThat(verifyApi.getId()).isEqualTo(API_ID);
                        softly.assertThat(verifyApi.getEnvironmentId()).isNull();
                        softly.assertThat(verifyApi.getEnvironmentHrid()).isNull();
                        softly.assertThat(verifyApi.getOrganizationId()).isNull();
                        softly.assertThat(verifyApi.getOrganizationHrid()).isNull();
                    }
                );

                verify(apiKeysCacheService).register(singletonList(new Api(apiDefinition)));
                verify(subscriptionsCacheService).register(singletonList(new Api(apiDefinition)));
                verify(subscriptionService).dispatchFor(singletonList(apiDefinition.getId()));
            }

            @ParameterizedTest
            @ArgumentsSource(ApiRegisterEventsProcessingProvider.class)
            void should_register_an_api_with_no_organization(Boolean initialSync) throws Exception {
                var apiDefinition = anApiV2()
                    .id(API_ID)
                    .plans(List.of(io.gravitee.definition.model.Plan.builder().status("PUBLISHED").build()))
                    .build();
                final Event publishEvent = anEvent(apiDefinition, EventType.PUBLISH_API);

                givenEvents(List.of(publishEvent));
                givenAnEnvironmentWithHrid();

                long lastRefreshAt = initialSync ? -1L : System.currentTimeMillis() - 5000;
                apiSynchronizer.synchronize(lastRefreshAt, System.currentTimeMillis(), ENVIRONMENTS);

                ArgumentCaptor<Api> apiCaptor = ArgumentCaptor.forClass(Api.class);
                verify(apiManager).register(apiCaptor.capture());
                SoftAssertions.assertSoftly(
                    softly -> {
                        Api verifyApi = apiCaptor.getValue();
                        softly.assertThat(verifyApi.getOrganizationId()).isNull();
                        softly.assertThat(verifyApi.getOrganizationHrid()).isNull();
                    }
                );
            }

            @ParameterizedTest
            @ArgumentsSource(ApiRegisterEventsProcessingProvider.class)
            void should_do_nothing_when_api_has_no_published_plans(Boolean initialSync) throws Exception {
                var apiDefinition = anApiV2()
                    .id(API_ID)
                    .plans(List.of(io.gravitee.definition.model.Plan.builder().status("STAGING").build()))
                    .build();
                final Event publishEvent = anEvent(apiDefinition, EventType.PUBLISH_API);

                givenEvents(List.of(publishEvent));

                long lastRefreshAt = initialSync ? -1L : System.currentTimeMillis() - 5000;
                apiSynchronizer.synchronize(lastRefreshAt, System.currentTimeMillis(), ENVIRONMENTS);

                verify(apiManager, never()).register(any());
                verify(apiManager, never()).unregister(any());
                verify(apiKeysCacheService, never()).register(anyList());
                verify(subscriptionsCacheService, never()).register(anyList());
            }

            @ParameterizedTest
            @ArgumentsSource(ApiRegisterEventsProcessingProvider.class)
            void should_do_nothing_when_api_tags_does_not_match_with_gateway(Boolean initialSync) throws Exception {
                var apiDefinition = anApiV2()
                    .id(API_ID)
                    .tags(Set.of("gw1"))
                    .plans(List.of(io.gravitee.definition.model.Plan.builder().status("PUBLISHED").build()))
                    .build();
                final Event publishEvent = anEvent(apiDefinition, EventType.PUBLISH_API);

                when(gatewayConfiguration.hasMatchingTags(Set.of("gw1"))).thenReturn(false);
                givenEvents(List.of(publishEvent));

                long lastRefreshAt = initialSync ? -1L : System.currentTimeMillis() - 5000;
                apiSynchronizer.synchronize(lastRefreshAt, System.currentTimeMillis(), ENVIRONMENTS);

                verify(apiManager, never()).register(any());
                verify(apiManager, never()).unregister(any());
                verify(apiKeysCacheService, never()).register(anyList());
                verify(subscriptionsCacheService, never()).register(anyList());
            }

            @ParameterizedTest
            @ArgumentsSource(ApiRegisterEventsProcessingProvider.class)
            void should_do_nothing_when_api_plan_tags_does_not_match_with_gateway(Boolean initialSync) throws Exception {
                var apiDefinition = anApiV2()
                    .id(API_ID)
                    .plans(List.of(io.gravitee.definition.model.Plan.builder().status("PUBLISHED").tags(Set.of("gw1")).build()))
                    .build();
                final Event publishEvent = anEvent(apiDefinition, EventType.PUBLISH_API);

                when(gatewayConfiguration.hasMatchingTags(Set.of("gw1"))).thenReturn(false);
                givenEvents(List.of(publishEvent));

                long lastRefreshAt = initialSync ? -1L : System.currentTimeMillis() - 5000;
                apiSynchronizer.synchronize(lastRefreshAt, System.currentTimeMillis(), ENVIRONMENTS);

                verify(apiManager, never()).register(any());
                verify(apiManager, never()).unregister(any());
                verify(apiKeysCacheService, never()).register(anyList());
                verify(subscriptionsCacheService, never()).register(anyList());
            }

            @ParameterizedTest
            @ArgumentsSource(ApiRegisterEventsProcessingProvider.class)
            void should_do_nothing_when_api_has_not_changed(Boolean initialSync) throws Exception {
                lenient().when(apiManager.requiredActionFor(any())).thenReturn(ActionOnApi.NONE);

                var apiDefinition = anApiV2().id(API_ID).build();
                final Event publishEvent = anEvent(apiDefinition, EventType.PUBLISH_API);

                givenEvents(List.of(publishEvent));

                long lastRefreshAt = initialSync ? -1L : System.currentTimeMillis() - 5000;
                apiSynchronizer.synchronize(lastRefreshAt, System.currentTimeMillis(), ENVIRONMENTS);

                verify(apiManager, never()).register(any());
                verify(apiManager, never()).unregister(any());
                verify(apiKeysCacheService, never()).register(anyList());
                verify(subscriptionsCacheService, never()).register(anyList());
            }

            @ParameterizedTest
            @ArgumentsSource(ApiRegisterEventsProcessingProvider.class)
            void should_undeploy_when_new_configuration_require(Boolean initialSync) throws Exception {
                lenient().when(apiManager.requiredActionFor(any())).thenReturn(ActionOnApi.UNDEPLOY);

                var apiDefinition = anApiV2().id(API_ID).build();
                final Event publishEvent = anEvent(apiDefinition, EventType.PUBLISH_API);

                givenEvents(List.of(publishEvent));

                long lastRefreshAt = initialSync ? -1L : System.currentTimeMillis() - 5000;
                apiSynchronizer.synchronize(lastRefreshAt, System.currentTimeMillis(), ENVIRONMENTS);

                verify(apiManager, never()).register(any());
                verify(apiManager).unregister(apiDefinition.getId());
                verify(apiKeysCacheService, never()).register(anyList());
                verify(subscriptionsCacheService, never()).register(anyList());
            }
        }

        @Nested
        class UnregisterEventsProcessing {

            @Test
            void should_process_api_unregister_events() throws Exception {
                var apiDefinition1 = anApiV2().id("api1").build();
                var apiDefinition2 = anApiV2().id("api2").build();

                givenEvents(List.of(anEvent(apiDefinition1, EventType.UNPUBLISH_API), anEvent(apiDefinition2, EventType.STOP_API)));

                apiSynchronizer.synchronize(System.currentTimeMillis() - 5000, System.currentTimeMillis(), ENVIRONMENTS);

                verify(apiManager).unregister("api1");
                verify(apiManager).unregister("api2");
            }
        }
    }

    @Nested
    class ApiV4 {

        @Nested
        class RegisterEventsProcessing {

            @ParameterizedTest
            @ArgumentsSource(ApiRegisterEventsProcessingProvider.class)
            void should_process_api_register_events(Boolean initialSync) throws Exception {
                var apiDefinition = aSyncApiV4()
                    .id(API_ID)
                    .plans(List.of(io.gravitee.definition.model.v4.plan.Plan.builder().status(PlanStatus.PUBLISHED).build()))
                    .build();
                final Event publishEvent = anEvent(apiDefinition, EventType.PUBLISH_API);

                givenEvents(List.of(publishEvent));
                givenAnOrganizationWithHrid();
                givenAnEnvironmentWithHrid();

                long lastRefreshAt = initialSync ? -1L : System.currentTimeMillis() - 5000;
                apiSynchronizer.synchronize(lastRefreshAt, System.currentTimeMillis(), ENVIRONMENTS);

                ArgumentCaptor<io.gravitee.gateway.jupiter.handlers.api.v4.Api> apiCaptor = ArgumentCaptor.forClass(
                    io.gravitee.gateway.jupiter.handlers.api.v4.Api.class
                );
                verify(apiManager).register(apiCaptor.capture());
                SoftAssertions.assertSoftly(
                    softly -> {
                        var verifyApi = apiCaptor.getValue();
                        softly.assertThat(verifyApi.getId()).isEqualTo(API_ID);
                        softly.assertThat(verifyApi.getEnvironmentId()).isEqualTo(ENVIRONMENT_ID);
                        softly.assertThat(verifyApi.getEnvironmentHrid()).isEqualTo(ENVIRONMENT_HRID);
                        softly.assertThat(verifyApi.getOrganizationId()).isEqualTo(ORGANIZATION_ID);
                        softly.assertThat(verifyApi.getOrganizationHrid()).isEqualTo(ORGANIZATION_HRID);
                    }
                );

                verify(apiKeysCacheService).register(singletonList(new io.gravitee.gateway.jupiter.handlers.api.v4.Api(apiDefinition)));
                verify(subscriptionsCacheService)
                    .register(singletonList(new io.gravitee.gateway.jupiter.handlers.api.v4.Api(apiDefinition)));
                verify(subscriptionService).dispatchFor(singletonList(apiDefinition.getId()));
            }

            @ParameterizedTest
            @ArgumentsSource(ApiRegisterEventsProcessingProvider.class)
            void should_process_api_register_events_with_environment_and_organization_without_hrid(Boolean initialSync) throws Exception {
                var apiDefinition = aSyncApiV4()
                    .id(API_ID)
                    .plans(List.of(io.gravitee.definition.model.v4.plan.Plan.builder().status(PlanStatus.PUBLISHED).build()))
                    .build();
                final Event publishEvent = anEvent(apiDefinition, EventType.PUBLISH_API);

                givenEvents(List.of(publishEvent));
                givenAnOrganizationWithoutHrid();
                givenAnEnvironmentWithoutHrid();

                long lastRefreshAt = initialSync ? -1L : System.currentTimeMillis() - 5000;
                apiSynchronizer.synchronize(lastRefreshAt, System.currentTimeMillis(), ENVIRONMENTS);

                ArgumentCaptor<io.gravitee.gateway.jupiter.handlers.api.v4.Api> apiCaptor = ArgumentCaptor.forClass(
                    io.gravitee.gateway.jupiter.handlers.api.v4.Api.class
                );
                verify(apiManager).register(apiCaptor.capture());
                SoftAssertions.assertSoftly(
                    softly -> {
                        var verifyApi = apiCaptor.getValue();
                        softly.assertThat(verifyApi.getEnvironmentHrid()).isNull();
                        softly.assertThat(verifyApi.getOrganizationHrid()).isNull();
                    }
                );
            }

            @ParameterizedTest
            @ArgumentsSource(ApiRegisterEventsProcessingProvider.class)
            void should_register_an_api_with_no_environment(Boolean initialSync) throws Exception {
                var apiDefinition = aSyncApiV4()
                    .id(API_ID)
                    .plans(List.of(io.gravitee.definition.model.v4.plan.Plan.builder().status(PlanStatus.PUBLISHED).build()))
                    .build();
                final Event publishEvent = anEvent(apiDefinition, EventType.PUBLISH_API);

                givenEvents(List.of(publishEvent));

                long lastRefreshAt = initialSync ? -1L : System.currentTimeMillis() - 5000;
                apiSynchronizer.synchronize(lastRefreshAt, System.currentTimeMillis(), ENVIRONMENTS);

                ArgumentCaptor<io.gravitee.gateway.jupiter.handlers.api.v4.Api> apiCaptor = ArgumentCaptor.forClass(
                    io.gravitee.gateway.jupiter.handlers.api.v4.Api.class
                );
                verify(apiManager).register(apiCaptor.capture());
                SoftAssertions.assertSoftly(
                    softly -> {
                        var verifyApi = apiCaptor.getValue();
                        softly.assertThat(verifyApi.getId()).isEqualTo(API_ID);
                        softly.assertThat(verifyApi.getEnvironmentId()).isNull();
                        softly.assertThat(verifyApi.getEnvironmentHrid()).isNull();
                        softly.assertThat(verifyApi.getOrganizationId()).isNull();
                        softly.assertThat(verifyApi.getOrganizationHrid()).isNull();
                    }
                );

                verify(apiKeysCacheService).register(singletonList(new io.gravitee.gateway.jupiter.handlers.api.v4.Api(apiDefinition)));
                verify(subscriptionsCacheService)
                    .register(singletonList(new io.gravitee.gateway.jupiter.handlers.api.v4.Api(apiDefinition)));
                verify(subscriptionService).dispatchFor(singletonList(apiDefinition.getId()));
            }

            @ParameterizedTest
            @ArgumentsSource(ApiRegisterEventsProcessingProvider.class)
            void should_register_an_api_with_no_organization(Boolean initialSync) throws Exception {
                var apiDefinition = aSyncApiV4()
                    .id(API_ID)
                    .plans(List.of(io.gravitee.definition.model.v4.plan.Plan.builder().status(PlanStatus.PUBLISHED).build()))
                    .build();
                final Event publishEvent = anEvent(apiDefinition, EventType.PUBLISH_API);

                givenEvents(List.of(publishEvent));
                givenAnEnvironmentWithHrid();

                long lastRefreshAt = initialSync ? -1L : System.currentTimeMillis() - 5000;
                apiSynchronizer.synchronize(lastRefreshAt, System.currentTimeMillis(), ENVIRONMENTS);

                ArgumentCaptor<io.gravitee.gateway.jupiter.handlers.api.v4.Api> apiCaptor = ArgumentCaptor.forClass(
                    io.gravitee.gateway.jupiter.handlers.api.v4.Api.class
                );
                verify(apiManager).register(apiCaptor.capture());
                SoftAssertions.assertSoftly(
                    softly -> {
                        var verifyApi = apiCaptor.getValue();
                        softly.assertThat(verifyApi.getOrganizationId()).isNull();
                        softly.assertThat(verifyApi.getOrganizationHrid()).isNull();
                    }
                );
            }

            @ParameterizedTest
            @ArgumentsSource(ApiRegisterEventsProcessingProvider.class)
            void should_do_nothing_when_api_has_no_published_plans(Boolean initialSync) throws Exception {
                var apiDefinition = aSyncApiV4()
                    .id(API_ID)
                    .plans(List.of(io.gravitee.definition.model.v4.plan.Plan.builder().status(PlanStatus.STAGING).build()))
                    .build();
                final Event publishEvent = anEvent(apiDefinition, EventType.PUBLISH_API);

                givenEvents(List.of(publishEvent));

                long lastRefreshAt = initialSync ? -1L : System.currentTimeMillis() - 5000;
                apiSynchronizer.synchronize(lastRefreshAt, System.currentTimeMillis(), ENVIRONMENTS);

                verify(apiManager, never()).register(any());
                verify(apiManager, never()).unregister(any());
                verify(apiKeysCacheService, never()).register(anyList());
                verify(subscriptionsCacheService, never()).register(anyList());
            }

            @ParameterizedTest
            @ArgumentsSource(ApiRegisterEventsProcessingProvider.class)
            void should_do_nothing_when_api_tags_does_not_match_with_gateway(Boolean initialSync) throws Exception {
                var apiDefinition = aSyncApiV4()
                    .id(API_ID)
                    .tags(Set.of("gw1"))
                    .plans(List.of(io.gravitee.definition.model.v4.plan.Plan.builder().status(PlanStatus.PUBLISHED).build()))
                    .build();
                final Event publishEvent = anEvent(apiDefinition, EventType.PUBLISH_API);

                when(gatewayConfiguration.hasMatchingTags(Set.of("gw1"))).thenReturn(false);
                givenEvents(List.of(publishEvent));

                long lastRefreshAt = initialSync ? -1L : System.currentTimeMillis() - 5000;
                apiSynchronizer.synchronize(lastRefreshAt, System.currentTimeMillis(), ENVIRONMENTS);

                verify(apiManager, never()).register(any());
                verify(apiManager, never()).unregister(any());
                verify(apiKeysCacheService, never()).register(anyList());
                verify(subscriptionsCacheService, never()).register(anyList());
            }

            @ParameterizedTest
            @ArgumentsSource(ApiRegisterEventsProcessingProvider.class)
            void should_do_nothing_when_api_plan_tags_does_not_match_with_gateway(Boolean initialSync) throws Exception {
                var apiDefinition = aSyncApiV4()
                    .id(API_ID)
                    .plans(
                        List.of(
                            io.gravitee.definition.model.v4.plan.Plan.builder().status(PlanStatus.PUBLISHED).tags(Set.of("gw1")).build()
                        )
                    )
                    .build();
                final Event publishEvent = anEvent(apiDefinition, EventType.PUBLISH_API);

                when(gatewayConfiguration.hasMatchingTags(Set.of("gw1"))).thenReturn(false);
                givenEvents(List.of(publishEvent));

                long lastRefreshAt = initialSync ? -1L : System.currentTimeMillis() - 5000;
                apiSynchronizer.synchronize(lastRefreshAt, System.currentTimeMillis(), ENVIRONMENTS);

                verify(apiManager, never()).register(any());
                verify(apiManager, never()).unregister(any());
                verify(apiKeysCacheService, never()).register(anyList());
                verify(subscriptionsCacheService, never()).register(anyList());
            }

            @ParameterizedTest
            @ArgumentsSource(ApiRegisterEventsProcessingProvider.class)
            void should_do_nothing_when_api_has_not_changed(Boolean initialSync) throws Exception {
                lenient().when(apiManager.requiredActionFor(any())).thenReturn(ActionOnApi.NONE);

                var apiDefinition = aSyncApiV4().id(API_ID).build();
                final Event publishEvent = anEvent(apiDefinition, EventType.PUBLISH_API);

                givenEvents(List.of(publishEvent));

                long lastRefreshAt = initialSync ? -1L : System.currentTimeMillis() - 5000;
                apiSynchronizer.synchronize(lastRefreshAt, System.currentTimeMillis(), ENVIRONMENTS);

                verify(apiManager, never()).register(any());
                verify(apiManager, never()).unregister(any());
                verify(apiKeysCacheService, never()).register(anyList());
                verify(subscriptionsCacheService, never()).register(anyList());
            }

            @ParameterizedTest
            @ArgumentsSource(ApiRegisterEventsProcessingProvider.class)
            void should_undeploy_when_new_configuration_require(Boolean initialSync) throws Exception {
                lenient().when(apiManager.requiredActionFor(any())).thenReturn(ActionOnApi.UNDEPLOY);

                var apiDefinition = aSyncApiV4().id(API_ID).build();
                final Event publishEvent = anEvent(apiDefinition, EventType.PUBLISH_API);

                givenEvents(List.of(publishEvent));

                long lastRefreshAt = initialSync ? -1L : System.currentTimeMillis() - 5000;
                apiSynchronizer.synchronize(lastRefreshAt, System.currentTimeMillis(), ENVIRONMENTS);

                verify(apiManager, never()).register(any());
                verify(apiManager).unregister(apiDefinition.getId());
                verify(apiKeysCacheService, never()).register(anyList());
                verify(subscriptionsCacheService, never()).register(anyList());
            }
        }

        @Nested
        class UnregisterEventsProcessing {

            @Test
            void should_process_api_unregister_events() throws Exception {
                var apiDefinition1 = aSyncApiV4().id("api1").build();
                var apiDefinition2 = aSyncApiV4().id("api2").build();

                givenEvents(List.of(anEvent(apiDefinition1, EventType.UNPUBLISH_API), anEvent(apiDefinition2, EventType.STOP_API)));

                apiSynchronizer.synchronize(System.currentTimeMillis() - 5000, System.currentTimeMillis(), ENVIRONMENTS);

                verify(apiManager).unregister("api1");
                verify(apiManager).unregister("api2");
            }
        }
    }

    @Nested
    class ApiV1Support {

        @ParameterizedTest
        @ArgumentsSource(ApiRegisterEventsProcessingProvider.class)
        void should_process_api_register_events_for_v1_by_loading_plans_from_repository(Boolean initialSync) throws Exception {
            var apiDefinition = anApiV1().id(API_ID).build();
            final Event publishEvent = anEvent(apiDefinition, EventType.PUBLISH_API);

            givenPlansFor(List.of(apiDefinition.getId()));
            givenEvents(List.of(publishEvent));
            givenAnOrganizationWithHrid();
            givenAnEnvironmentWithHrid();

            long lastRefreshAt = initialSync ? -1L : System.currentTimeMillis() - 5000;
            apiSynchronizer.synchronize(lastRefreshAt, System.currentTimeMillis(), ENVIRONMENTS);

            ArgumentCaptor<Api> apiCaptor = ArgumentCaptor.forClass(Api.class);
            verify(apiManager).register(apiCaptor.capture());
            SoftAssertions.assertSoftly(
                softly -> {
                    Api verifyApi = apiCaptor.getValue();
                    softly.assertThat(verifyApi.getId()).isEqualTo(API_ID);
                    softly.assertThat(verifyApi.getEnvironmentId()).isEqualTo(ENVIRONMENT_ID);
                    softly.assertThat(verifyApi.getEnvironmentHrid()).isEqualTo(ENVIRONMENT_HRID);
                    softly.assertThat(verifyApi.getOrganizationId()).isEqualTo(ORGANIZATION_ID);
                    softly.assertThat(verifyApi.getOrganizationHrid()).isEqualTo(ORGANIZATION_HRID);

                    var plan = verifyApi.getDefinition().getPlan("plan-" + apiDefinition.getId());
                    softly.assertThat(plan.getApi()).isEqualTo(apiDefinition.getId());
                    softly.assertThat(plan.getStatus()).isEqualTo("PUBLISHED");
                    softly.assertThat(plan.getPaths()).isNotEmpty();
                }
            );

            verify(apiKeysCacheService).register(singletonList(new Api(apiDefinition)));
            verify(subscriptionsCacheService).register(singletonList(new Api(apiDefinition)));
            verify(subscriptionService).dispatchFor(singletonList(apiDefinition.getId()));
        }

        @ParameterizedTest
        @ArgumentsSource(ApiRegisterEventsProcessingProvider.class)
        void should_optimize_calls_to_handle_lots_of_events(Boolean initialSync) throws Exception {
            long page = 0;
            var bulkSize = 100;
            List<String> v1ApiIds = new ArrayList<>(500);
            List<Event> eventAccumulator = new ArrayList<>(100);

            apiSynchronizer.bulkItems = bulkSize;
            for (int i = 1; i <= 500; i++) {
                var apiDefinition = anApiV1().id("api" + i + "-test").build();
                v1ApiIds.add(apiDefinition.getId());

                eventAccumulator.add(anEvent(apiDefinition, EventType.START_API));

                if (i % bulkSize == 0) {
                    when(
                        eventRepository.searchLatest(
                            any(EventCriteria.class),
                            eq(Event.EventProperties.API_ID),
                            eq(page),
                            eq((long) bulkSize)
                        )
                    )
                        .thenReturn(eventAccumulator);

                    page++;
                    eventAccumulator = new ArrayList<>();
                }
            }
            givenAnEnvironmentWithHrid();
            givenAnOrganizationWithHrid();
            givenPlansFor(v1ApiIds);

            long lastRefreshAt = initialSync ? -1L : System.currentTimeMillis() - 5000;
            apiSynchronizer.synchronize(lastRefreshAt, System.currentTimeMillis(), ENVIRONMENTS);

            // register all apis
            verify(apiManager, times(500)).register(any(Api.class));

            // Fetch plans and register subscriptions only once by page
            verify(planRepository, times(5)).findByApis(anyList());
            verify(apiKeysCacheService, times(5)).register(anyList());
            verify(subscriptionsCacheService, times(5)).register(anyList());

            // Check that only one call to env and org repositories have been made, others should hit the cache.
            verify(environmentRepository, times(1)).findById(ENVIRONMENT_ID);
            verify(organizationRepository, times(1)).findById(ORGANIZATION_ID);
        }
    }

    Event anEvent(final io.gravitee.definition.model.Api apiDefinition, EventType eventType) throws Exception {
        return anEvent(
            apiDefinition.getId(),
            apiDefinition.getDefinitionVersion(),
            objectMapper.writeValueAsString(apiDefinition),
            eventType
        );
    }

    Event anEvent(final io.gravitee.definition.model.v4.Api apiDefinition, EventType eventType) throws Exception {
        return anEvent(
            apiDefinition.getId(),
            apiDefinition.getDefinitionVersion(),
            objectMapper.writeValueAsString(apiDefinition),
            eventType
        );
    }

    Event anEvent(final String apiId, final DefinitionVersion definitionVersion, final String apiDefinition, EventType eventType)
        throws Exception {
        Map<String, String> properties = new HashMap<>();
        properties.put(Event.EventProperties.API_ID.getValue(), apiId);

        Event event = new Event();
        event.setType(eventType);
        event.setCreatedAt(new Date());
        event.setProperties(properties);
        event.setEnvironments(singleton(ENVIRONMENT_ID));
        event.setPayload(
            objectMapper.writeValueAsString(
                new RepositoryApiBuilder()
                    .id(apiId)
                    .definitionVersion(definitionVersion)
                    .updatedAt(new Date())
                    .definition(apiDefinition)
                    .environment(ENVIRONMENT_ID)
                    .build()
            )
        );

        return event;
    }

    private void givenPlansFor(List<String> apiIds) throws Exception {
        var plans = apiIds
            .stream()
            .map(
                id -> {
                    try {
                        final Plan plan = new Plan();
                        plan.setId("plan-" + id);
                        plan.setApi(id);
                        plan.setStatus(Plan.Status.PUBLISHED);
                        plan.setDefinition(objectMapper.writeValueAsString(Map.of("/", List.of(new Rule()))));
                        return plan;
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }
            )
            .collect(Collectors.toList());

        when(planRepository.findByApis(anyList()))
            .thenAnswer(
                (Answer<List<Plan>>) invocation -> {
                    @SuppressWarnings("unchecked")
                    var ids = (List<String>) invocation.getArgument(0, List.class);
                    return plans.stream().filter(p -> ids.contains(p.getApi())).collect(Collectors.toList());
                }
            );
    }

    void givenEvents(List<Event> events) {
        when(eventRepository.searchLatest(any(EventCriteria.class), eq(Event.EventProperties.API_ID), anyLong(), anyLong()))
            .thenReturn(events);
    }

    void givenAnOrganizationWithHrid() throws Exception {
        final Organization organization = new Organization();
        organization.setId(ORGANIZATION_ID);
        organization.setHrids(List.of(ApiSynchronizerTest.ORGANIZATION_HRID));
        lenient().when(organizationRepository.findById(ORGANIZATION_ID)).thenReturn(Optional.of(organization));
    }

    void givenAnOrganizationWithoutHrid() throws Exception {
        final Organization organization = new Organization();
        organization.setId(ORGANIZATION_ID);
        organization.setHrids(List.of());
        lenient().when(organizationRepository.findById(ORGANIZATION_ID)).thenReturn(Optional.of(organization));
    }

    void givenAnEnvironmentWithHrid() throws Exception {
        final Environment environment = new Environment();
        environment.setId(ENVIRONMENT_ID);
        environment.setOrganizationId(ORGANIZATION_ID);
        environment.setHrids(List.of(ApiSynchronizerTest.ENVIRONMENT_HRID));
        lenient().when(environmentRepository.findById(ENVIRONMENT_ID)).thenReturn(Optional.of(environment));
    }

    void givenAnEnvironmentWithoutHrid() throws Exception {
        final Environment environment = new Environment();
        environment.setId(ENVIRONMENT_ID);
        environment.setOrganizationId(ORGANIZATION_ID);
        environment.setHrids(List.of());
        lenient().when(environmentRepository.findById(ENVIRONMENT_ID)).thenReturn(Optional.of(environment));
    }
}

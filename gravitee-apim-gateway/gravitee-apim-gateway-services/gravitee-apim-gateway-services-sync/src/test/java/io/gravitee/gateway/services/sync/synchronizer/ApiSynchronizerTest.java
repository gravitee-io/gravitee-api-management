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
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.handlers.api.manager.ActionOnApi;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.services.sync.builder.RepositoryApiBuilder;
import io.gravitee.gateway.services.sync.cache.ApiKeysCacheService;
import io.gravitee.gateway.services.sync.cache.SubscriptionsCacheService;
import io.gravitee.gateway.services.sync.synchronizer.api.EventToReactableApiAdapter;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.*;
import java.util.*;
import java.util.concurrent.Executors;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class ApiSynchronizerTest {

    static final List<String> ENVIRONMENTS = Arrays.asList("DEFAULT", "OTHER_ENV");
    private static final String ENVIRONMENT_ID = "env#1";
    private static final String ORGANIZATION_ID = "orga#1";
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

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        var eventToReactableApiAdapter = new EventToReactableApiAdapter(objectMapper, environmentRepository, organizationRepository);

        apiSynchronizer =
            new ApiSynchronizer(
                eventRepository,
                objectMapper,
                Executors.newFixedThreadPool(1),
                100,
                planRepository,
                apiKeysCacheService,
                subscriptionsCacheService,
                subscriptionService,
                apiManager,
                eventToReactableApiAdapter
            );
        lenient().when(apiManager.requiredActionFor(any())).thenReturn(ActionOnApi.DEPLOY);
    }

    @Test
    void initialSynchronize() throws Exception {
        var apiDefinition = anApiV2().id(API_ID).build();

        final Event mockEvent = anEvent(apiDefinition, EventType.PUBLISH_API);
        when(
            eventRepository.searchLatest(
                argThat(
                    criteria ->
                        criteria != null &&
                        criteria.getTypes().containsAll(asList(EventType.PUBLISH_API, EventType.START_API)) &&
                        criteria.getEnvironments().containsAll(ENVIRONMENTS)
                ),
                eq(Event.EventProperties.API_ID),
                anyLong(),
                anyLong()
            )
        )
            .thenReturn(singletonList(mockEvent));

        mockEnvironmentAndOrganization();

        apiSynchronizer.synchronize(-1L, System.currentTimeMillis(), ENVIRONMENTS);

        ArgumentCaptor<Api> apiCaptor = ArgumentCaptor.forClass(Api.class);

        verify(apiManager).register(apiCaptor.capture());

        Api verifyApi = apiCaptor.getValue();
        SoftAssertions.assertSoftly(
            softly -> {
                softly.assertThat(verifyApi.getId()).isEqualTo(API_ID);
                softly.assertThat(verifyApi.getEnvironmentId()).isEqualTo(ENVIRONMENT_ID);
                softly.assertThat(verifyApi.getEnvironmentHrid()).isEqualTo(ENVIRONMENT_HRID);
                softly.assertThat(verifyApi.getOrganizationId()).isEqualTo(ORGANIZATION_ID);
                softly.assertThat(verifyApi.getOrganizationHrid()).isEqualTo(ORGANIZATION_HRID);
            }
        );

        verify(apiManager, never()).unregister(any(String.class));
        verify(planRepository, never()).findByApis(anyList());
        verify(apiKeysCacheService).register(singletonList(new Api(apiDefinition)));
        verify(subscriptionsCacheService).register(singletonList(new Api(apiDefinition)));
        verify(subscriptionService).dispatchFor(singletonList(apiDefinition.getId()));
    }

    @Test
    void initialSynchronizeWithNoEnvironment() throws Exception {
        var apiDefinition = anApiV2().id(API_ID).build();

        final Event mockEvent = anEvent(apiDefinition, EventType.PUBLISH_API);
        when(
            eventRepository.searchLatest(
                argThat(
                    criteria ->
                        criteria != null &&
                        criteria.getTypes().containsAll(asList(EventType.PUBLISH_API, EventType.START_API)) &&
                        criteria.getEnvironments().containsAll(ENVIRONMENTS)
                ),
                eq(Event.EventProperties.API_ID),
                anyLong(),
                anyLong()
            )
        )
            .thenReturn(singletonList(mockEvent));

        apiSynchronizer.synchronize(-1L, System.currentTimeMillis(), ENVIRONMENTS);

        ArgumentCaptor<Api> apiCaptor = ArgumentCaptor.forClass(Api.class);

        verify(apiManager).register(apiCaptor.capture());

        Api verifyApi = apiCaptor.getValue();
        SoftAssertions.assertSoftly(
            softly -> {
                softly.assertThat(verifyApi.getId()).isEqualTo(API_ID);
                softly.assertThat(verifyApi.getEnvironmentId()).isNull();
                softly.assertThat(verifyApi.getEnvironmentHrid()).isNull();
                softly.assertThat(verifyApi.getOrganizationId()).isNull();
                softly.assertThat(verifyApi.getOrganizationHrid()).isNull();
            }
        );

        verify(apiManager, never()).unregister(any(String.class));
        verify(planRepository, never()).findByApis(anyList());
        verify(apiKeysCacheService)
            .register(
                argThat(
                    (ArgumentMatcher<List<ReactableApi<?>>>) reactableApis ->
                        reactableApis.size() == 1 && reactableApis.get(0).equals(new Api(apiDefinition))
                )
            );
        verify(subscriptionsCacheService)
            .register(
                argThat(
                    (ArgumentMatcher<List<ReactableApi<?>>>) reactableApis ->
                        reactableApis.size() == 1 && reactableApis.get(0).equals(new Api(apiDefinition))
                )
            );
    }

    @Test
    void initialSynchronize_withEnvironmentAndOrganization_withoutHrId_shouldSetHrIdToNull() throws Exception {
        var apiDefinition = anApiV2().id(API_ID).build();

        final Event mockEvent = anEvent(apiDefinition, EventType.PUBLISH_API);
        when(
            eventRepository.searchLatest(
                argThat(
                    criteria ->
                        criteria != null &&
                        criteria.getTypes().containsAll(asList(EventType.PUBLISH_API, EventType.START_API)) &&
                        criteria.getEnvironments().containsAll(ENVIRONMENTS)
                ),
                eq(Event.EventProperties.API_ID),
                anyLong(),
                anyLong()
            )
        )
            .thenReturn(singletonList(mockEvent));

        mockEnvironmentAndOrganizationWithoutHrIds();

        apiSynchronizer.synchronize(-1L, System.currentTimeMillis(), ENVIRONMENTS);

        ArgumentCaptor<Api> apiCaptor = ArgumentCaptor.forClass(Api.class);

        verify(apiManager).register(apiCaptor.capture());

        Api verifyApi = apiCaptor.getValue();
        SoftAssertions.assertSoftly(
            softly -> {
                softly.assertThat(verifyApi.getId()).isEqualTo(API_ID);
                softly.assertThat(verifyApi.getEnvironmentId()).isEqualTo(ENVIRONMENT_ID);
                softly.assertThat(verifyApi.getEnvironmentHrid()).isNull();
                softly.assertThat(verifyApi.getOrganizationId()).isEqualTo(ORGANIZATION_ID);
                softly.assertThat(verifyApi.getOrganizationHrid()).isNull();
            }
        );
    }

    @Test
    void initialSynchronizeWithNoOrganization() throws Exception {
        var apiDefinition = anApiV2().id(API_ID).build();

        final Event mockEvent = anEvent(apiDefinition, EventType.PUBLISH_API);
        when(
            eventRepository.searchLatest(
                argThat(
                    criteria ->
                        criteria != null &&
                        criteria.getTypes().containsAll(asList(EventType.PUBLISH_API, EventType.START_API)) &&
                        criteria.getEnvironments().containsAll(ENVIRONMENTS)
                ),
                eq(Event.EventProperties.API_ID),
                anyLong(),
                anyLong()
            )
        )
            .thenReturn(singletonList(mockEvent));

        final Environment environment = new Environment();
        environment.setId(ENVIRONMENT_ID);
        environment.setOrganizationId(ORGANIZATION_ID);
        environment.setHrids(List.of(ENVIRONMENT_HRID));
        when(environmentRepository.findById(ENVIRONMENT_ID)).thenReturn(Optional.of(environment));
        when(organizationRepository.findById(ORGANIZATION_ID)).thenReturn(Optional.empty());

        apiSynchronizer.synchronize(-1L, System.currentTimeMillis(), ENVIRONMENTS);

        ArgumentCaptor<Api> apiCaptor = ArgumentCaptor.forClass(Api.class);

        verify(apiManager).register(apiCaptor.capture());

        Api verifyApi = apiCaptor.getValue();
        SoftAssertions.assertSoftly(
            softly -> {
                softly.assertThat(verifyApi.getId()).isEqualTo(API_ID);
                softly.assertThat(verifyApi.getEnvironmentId()).isEqualTo(ENVIRONMENT_ID);
                softly.assertThat(verifyApi.getEnvironmentHrid()).isEqualTo(ENVIRONMENT_HRID);
                softly.assertThat(verifyApi.getOrganizationId()).isNull();
                softly.assertThat(verifyApi.getOrganizationHrid()).isNull();
            }
        );

        verify(apiManager, never()).unregister(any(String.class));
        verify(planRepository, never()).findByApis(anyList());
        verify(apiKeysCacheService).register(singletonList(new Api(apiDefinition)));
        verify(subscriptionsCacheService).register(singletonList(new Api(apiDefinition)));
    }

    @Test
    void publishWithDefinitionV2() throws Exception {
        var apiDefinition = anApiV2().id(API_ID).build();

        final Event mockEvent = anEvent(apiDefinition, EventType.PUBLISH_API);
        when(
            eventRepository.searchLatest(
                argThat(
                    criteria ->
                        criteria != null &&
                        criteria
                            .getTypes()
                            .containsAll(asList(EventType.PUBLISH_API, EventType.START_API, EventType.UNPUBLISH_API, EventType.STOP_API)) &&
                        criteria.getEnvironments().containsAll(ENVIRONMENTS)
                ),
                eq(Event.EventProperties.API_ID),
                anyLong(),
                anyLong()
            )
        )
            .thenReturn(singletonList(mockEvent));

        mockEnvironmentAndOrganization();

        apiSynchronizer.synchronize(System.currentTimeMillis() - 5000, System.currentTimeMillis(), ENVIRONMENTS);

        verify(apiManager).register(new Api(apiDefinition));
        verify(apiManager, never()).unregister(any(String.class));
        verify(planRepository, never()).findByApis(anyList());
        verify(apiKeysCacheService).register(singletonList(new Api(apiDefinition)));
        verify(subscriptionsCacheService).register(singletonList(new Api(apiDefinition)));
    }

    @Test
    void publishWithDefinitionV1() throws Exception {
        var apiDefinition = anApiV1().id(API_ID).build();

        final Event mockEvent = anEvent(apiDefinition, EventType.PUBLISH_API);
        when(
            eventRepository.searchLatest(
                argThat(
                    criteria ->
                        criteria != null &&
                        criteria
                            .getTypes()
                            .containsAll(asList(EventType.PUBLISH_API, EventType.START_API, EventType.UNPUBLISH_API, EventType.STOP_API)) &&
                        criteria.getEnvironments().containsAll(ENVIRONMENTS)
                ),
                eq(Event.EventProperties.API_ID),
                anyLong(),
                anyLong()
            )
        )
            .thenReturn(singletonList(mockEvent));

        final Plan plan = new Plan();
        plan.setId("planId");
        plan.setApi(apiDefinition.getId());
        plan.setStatus(Plan.Status.PUBLISHED);
        when(planRepository.findByApis(anyList())).thenReturn(singletonList(plan));
        mockEnvironmentAndOrganization();

        apiSynchronizer.synchronize(System.currentTimeMillis() - 5000, System.currentTimeMillis(), ENVIRONMENTS);

        ArgumentCaptor<Api> apiCaptor = ArgumentCaptor.forClass(Api.class);
        verify(apiManager).register(apiCaptor.capture());
        Api verifyApi = apiCaptor.getValue();
        SoftAssertions.assertSoftly(
            softly -> {
                softly.assertThat(verifyApi.getId()).isEqualTo(API_ID);
                softly.assertThat(verifyApi.getDefinition().getPlan("planId").getApi()).isEqualTo(apiDefinition.getId());
            }
        );

        verify(apiManager, never()).unregister(any(String.class));
        verify(planRepository, times(1)).findByApis(anyList());
        verify(apiKeysCacheService).register(singletonList(new Api(apiDefinition)));
        verify(subscriptionsCacheService).register(singletonList(new Api(apiDefinition)));
    }

    @Test
    void publishWithPagination() throws Exception {
        var apiDefinition1 = anApiV2().id(API_ID).build();
        var apiDefinition2 = anApiV2().id("api2-test").build();

        // Force bulk size to 1.
        apiSynchronizer.bulkItems = 1;

        final Event mockEvent = anEvent(apiDefinition1, EventType.PUBLISH_API);
        final Event mockEvent2 = anEvent(apiDefinition2, EventType.PUBLISH_API);
        when(
            eventRepository.searchLatest(
                argThat(
                    criteria ->
                        criteria != null &&
                        criteria
                            .getTypes()
                            .containsAll(asList(EventType.PUBLISH_API, EventType.START_API, EventType.UNPUBLISH_API, EventType.STOP_API)) &&
                        criteria.getEnvironments().containsAll(ENVIRONMENTS)
                ),
                eq(Event.EventProperties.API_ID),
                eq(0L),
                eq(1L)
            )
        )
            .thenReturn(singletonList(mockEvent));

        when(
            eventRepository.searchLatest(
                argThat(
                    criteria ->
                        criteria != null &&
                        criteria
                            .getTypes()
                            .containsAll(asList(EventType.PUBLISH_API, EventType.START_API, EventType.UNPUBLISH_API, EventType.STOP_API)) &&
                        criteria.getEnvironments().containsAll(ENVIRONMENTS)
                ),
                eq(Event.EventProperties.API_ID),
                eq(1L),
                eq(1L)
            )
        )
            .thenReturn(singletonList(mockEvent2));

        mockEnvironmentAndOrganization();

        apiSynchronizer.synchronize(System.currentTimeMillis() - 5000, System.currentTimeMillis(), ENVIRONMENTS);

        verify(apiManager, times(1)).register(new Api(apiDefinition1));
        verify(apiManager, times(1)).register(new Api(apiDefinition2));
        verify(apiManager, never()).unregister(any(String.class));
        verify(planRepository, never()).findByApis(anyList());
        verify(apiKeysCacheService).register(singletonList(new Api(apiDefinition1)));
        verify(apiKeysCacheService).register(singletonList(new Api(apiDefinition2)));
        verify(subscriptionsCacheService).register(singletonList(new Api(apiDefinition1)));
        verify(subscriptionsCacheService).register(singletonList(new Api(apiDefinition2)));
    }

    @Test
    void publishWithPaginationAndDefinitionV1AndV2() throws Exception {
        var apiDefinition1 = anApiV1().id(API_ID).build();
        var apiDefinition2 = anApiV2().id("api2-test").build();

        // Force bulk size to 1.
        apiSynchronizer.bulkItems = 1;

        final Event mockEvent = anEvent(apiDefinition1, EventType.PUBLISH_API);
        final Event mockEvent2 = anEvent(apiDefinition2, EventType.PUBLISH_API);
        when(
            eventRepository.searchLatest(
                argThat(
                    criteria ->
                        criteria != null &&
                        criteria
                            .getTypes()
                            .containsAll(asList(EventType.PUBLISH_API, EventType.START_API, EventType.UNPUBLISH_API, EventType.STOP_API)) &&
                        criteria.getEnvironments().containsAll(ENVIRONMENTS)
                ),
                eq(Event.EventProperties.API_ID),
                eq(0L),
                eq(1L)
            )
        )
            .thenReturn(singletonList(mockEvent));

        when(
            eventRepository.searchLatest(
                argThat(
                    criteria ->
                        criteria != null &&
                        criteria
                            .getTypes()
                            .containsAll(asList(EventType.PUBLISH_API, EventType.START_API, EventType.UNPUBLISH_API, EventType.STOP_API)) &&
                        criteria.getEnvironments().containsAll(ENVIRONMENTS)
                ),
                eq(Event.EventProperties.API_ID),
                eq(1L),
                eq(1L)
            )
        )
            .thenReturn(singletonList(mockEvent2));

        final Plan plan = new Plan();
        plan.setApi(apiDefinition1.getId());
        when(planRepository.findByApis(singletonList(plan.getApi()))).thenReturn(singletonList(plan));
        mockEnvironmentAndOrganization();

        apiSynchronizer.synchronize(System.currentTimeMillis() - 5000, System.currentTimeMillis(), ENVIRONMENTS);

        verify(apiManager, times(1)).register(new Api(apiDefinition1));
        verify(apiManager, times(1)).register(new Api(apiDefinition2));
        verify(apiManager, never()).unregister(any(String.class));
        verify(planRepository, times(1)).findByApis(singletonList(plan.getApi()));
        verify(apiKeysCacheService).register(singletonList(new Api(apiDefinition1)));
        verify(apiKeysCacheService).register(singletonList(new Api(apiDefinition2)));
        verify(subscriptionsCacheService).register(singletonList(new Api(apiDefinition1)));
        verify(subscriptionsCacheService).register(singletonList(new Api(apiDefinition2)));
    }

    @Test
    void unpublish() throws Exception {
        var apiDefinition = anApiV2().id(API_ID).build();

        final Event mockEvent = anEvent(apiDefinition, EventType.UNPUBLISH_API);
        when(
            eventRepository.searchLatest(
                argThat(
                    criteria ->
                        criteria != null &&
                        criteria
                            .getTypes()
                            .containsAll(asList(EventType.PUBLISH_API, EventType.START_API, EventType.UNPUBLISH_API, EventType.STOP_API)) &&
                        criteria.getEnvironments().containsAll(ENVIRONMENTS)
                ),
                eq(Event.EventProperties.API_ID),
                anyLong(),
                anyLong()
            )
        )
            .thenReturn(singletonList(mockEvent));

        apiSynchronizer.synchronize(System.currentTimeMillis() - 5000, System.currentTimeMillis(), ENVIRONMENTS);

        verify(apiManager, never()).register(new Api(apiDefinition));
        verify(apiManager).unregister(apiDefinition.getId());
        verify(planRepository, never()).findByApis(anyList());
        verify(apiKeysCacheService, never()).register(singletonList(new Api(apiDefinition)));
        verify(subscriptionsCacheService, never()).register(singletonList(new Api(apiDefinition)));
    }

    @Test
    void unpublishWithPagination() throws Exception {
        var apiDefinition1 = anApiV1().id(API_ID).build();
        var apiDefinition2 = anApiV2().id("api2-test").build();

        // Force bulk size to 1.
        apiSynchronizer.bulkItems = 1;

        final Event mockEvent = anEvent(apiDefinition1, EventType.UNPUBLISH_API);
        final Event mockEvent2 = anEvent(apiDefinition2, EventType.UNPUBLISH_API);
        when(
            eventRepository.searchLatest(
                argThat(
                    criteria ->
                        criteria != null &&
                        criteria
                            .getTypes()
                            .containsAll(asList(EventType.PUBLISH_API, EventType.START_API, EventType.UNPUBLISH_API, EventType.STOP_API)) &&
                        criteria.getEnvironments().containsAll(ENVIRONMENTS)
                ),
                eq(Event.EventProperties.API_ID),
                eq(0L),
                eq(1L)
            )
        )
            .thenReturn(singletonList(mockEvent));

        when(
            eventRepository.searchLatest(
                argThat(
                    criteria ->
                        criteria != null &&
                        criteria
                            .getTypes()
                            .containsAll(asList(EventType.PUBLISH_API, EventType.START_API, EventType.UNPUBLISH_API, EventType.STOP_API)) &&
                        criteria.getEnvironments().containsAll(ENVIRONMENTS)
                ),
                eq(Event.EventProperties.API_ID),
                eq(1L),
                eq(1L)
            )
        )
            .thenReturn(singletonList(mockEvent2));

        apiSynchronizer.synchronize(System.currentTimeMillis() - 5000, System.currentTimeMillis(), ENVIRONMENTS);

        verify(apiManager, never()).register(new Api(apiDefinition1));
        verify(apiManager).unregister(apiDefinition1.getId());
        verify(apiManager).unregister(apiDefinition2.getId());
        verify(planRepository, never()).findByApis(anyList());
        verify(apiKeysCacheService, never()).register(singletonList(new Api(apiDefinition1)));
        verify(subscriptionsCacheService, never()).register(singletonList(new Api(apiDefinition1)));
    }

    @Test
    void synchronizeWithLotOfApiEvents() throws Exception {
        long page = 0;
        List<Event> eventAccumulator = new ArrayList<>(100);

        for (int i = 1; i <= 500; i++) {
            var apiDefinition = anApiV1().id("api" + i + "-test").build();

            if (i % 2 == 0) {
                eventAccumulator.add(anEvent(apiDefinition, EventType.START_API));
            } else {
                eventAccumulator.add(anEvent(apiDefinition, EventType.STOP_API));
            }

            if (i % 100 == 0) {
                when(
                    eventRepository.searchLatest(
                        argThat(
                            criteria ->
                                criteria != null &&
                                criteria
                                    .getTypes()
                                    .containsAll(
                                        asList(EventType.PUBLISH_API, EventType.START_API, EventType.UNPUBLISH_API, EventType.STOP_API)
                                    ) &&
                                criteria.getEnvironments().containsAll(ENVIRONMENTS)
                        ),
                        eq(Event.EventProperties.API_ID),
                        eq(page),
                        eq(100L)
                    )
                )
                    .thenReturn(eventAccumulator);

                page++;
                eventAccumulator = new ArrayList<>();
            }
        }

        mockEnvironmentAndOrganization();
        apiSynchronizer.synchronize(System.currentTimeMillis() - 5000, System.currentTimeMillis(), ENVIRONMENTS);

        verify(planRepository, times(3)).findByApis(anyList());
        verify(apiKeysCacheService, times(3)).register(anyList());
        verify(subscriptionsCacheService, times(3)).register(anyList());
        verify(apiManager, times(250)).register(any(Api.class));
        verify(apiManager, times(250)).unregister(anyString());

        // Check that only one call to env and org repositories have been made, others should hit the cache.
        verify(environmentRepository, times(1)).findById(ENVIRONMENT_ID);
        verify(organizationRepository, times(1)).findById(ORGANIZATION_ID);
    }

    @Test
    void shouldNotDeployWhichDoesntRequireRedeployment() throws Exception {
        lenient().when(apiManager.requiredActionFor(any())).thenReturn(ActionOnApi.NONE);

        var apiDefinition = anApiV2().id(API_ID).build();

        final Event mockEvent = anEvent(apiDefinition, EventType.PUBLISH_API);
        when(
            eventRepository.searchLatest(
                argThat(
                    criteria ->
                        criteria != null &&
                        criteria
                            .getTypes()
                            .containsAll(asList(EventType.PUBLISH_API, EventType.START_API, EventType.UNPUBLISH_API, EventType.STOP_API)) &&
                        criteria.getEnvironments().containsAll(ENVIRONMENTS)
                ),
                eq(Event.EventProperties.API_ID),
                anyLong(),
                anyLong()
            )
        )
            .thenReturn(singletonList(mockEvent));

        mockEnvironmentAndOrganization();

        apiSynchronizer.synchronize(System.currentTimeMillis() - 5000, System.currentTimeMillis(), ENVIRONMENTS);

        verify(apiManager, never()).register(new Api(apiDefinition));
        verify(apiManager, never()).unregister(any(String.class));
        verify(planRepository, never()).findByApis(anyList());
        verify(apiKeysCacheService, never()).register(singletonList(new Api(apiDefinition)));
        verify(subscriptionsCacheService, never()).register(singletonList(new Api(apiDefinition)));
    }

    @Test
    void shouldUnDeployWhichDoesRequireUndeployment() throws Exception {
        lenient().when(apiManager.requiredActionFor(any())).thenReturn(ActionOnApi.UNDEPLOY);

        var apiDefinition = anApiV2().id(API_ID).build();

        final Event mockEvent = anEvent(apiDefinition, EventType.PUBLISH_API);
        when(
            eventRepository.searchLatest(
                argThat(
                    criteria ->
                        criteria != null &&
                        criteria
                            .getTypes()
                            .containsAll(asList(EventType.PUBLISH_API, EventType.START_API, EventType.UNPUBLISH_API, EventType.STOP_API)) &&
                        criteria.getEnvironments().containsAll(ENVIRONMENTS)
                ),
                eq(Event.EventProperties.API_ID),
                anyLong(),
                anyLong()
            )
        )
            .thenReturn(singletonList(mockEvent));

        mockEnvironmentAndOrganization();

        apiSynchronizer.synchronize(System.currentTimeMillis() - 5000, System.currentTimeMillis(), ENVIRONMENTS);

        verify(apiManager, never()).register(new Api(apiDefinition));
        verify(apiManager, times(1)).unregister(any(String.class));
        verify(planRepository, never()).findByApis(anyList());
        verify(apiKeysCacheService, never()).register(singletonList(new Api(apiDefinition)));
        verify(subscriptionsCacheService, never()).register(singletonList(new Api(apiDefinition)));
    }

    private Event anEvent(final io.gravitee.definition.model.Api apiDefinition, EventType eventType) throws Exception {
        Map<String, String> properties = new HashMap<>();
        properties.put(Event.EventProperties.API_ID.getValue(), apiDefinition.getId());

        Event event = new Event();
        event.setType(eventType);
        event.setCreatedAt(new Date());
        event.setProperties(properties);
        event.setPayload(apiDefinition.getId());
        event.setEnvironments(singleton(ENVIRONMENT_ID));
        event.setPayload(
            objectMapper.writeValueAsString(
                new RepositoryApiBuilder()
                    .id(apiDefinition.getId())
                    .updatedAt(new Date())
                    .definition(objectMapper.writeValueAsString(apiDefinition))
                    .environment(ENVIRONMENT_ID)
                    .build()
            )
        );

        return event;
    }

    private void mockEnvironmentAndOrganizationWithoutHrIds() throws io.gravitee.repository.exceptions.TechnicalException {
        mockEnvironmentAndOrganization(List.of(), List.of());
    }

    private void mockEnvironmentAndOrganization() throws io.gravitee.repository.exceptions.TechnicalException {
        mockEnvironmentAndOrganization(List.of(ENVIRONMENT_HRID), List.of(ORGANIZATION_HRID));
    }

    private void mockEnvironmentAndOrganization(List<String> envHrIds, List<String> orgHrIds)
        throws io.gravitee.repository.exceptions.TechnicalException {
        final Environment environment = new Environment();
        environment.setId(ENVIRONMENT_ID);
        environment.setOrganizationId(ORGANIZATION_ID);
        environment.setHrids(envHrIds);
        when(environmentRepository.findById(ENVIRONMENT_ID)).thenReturn(Optional.of(environment));
        final Organization organization = new Organization();
        organization.setId(ORGANIZATION_ID);
        organization.setHrids(orgHrIds);
        when(organizationRepository.findById(ORGANIZATION_ID)).thenReturn(Optional.of(organization));
    }
}

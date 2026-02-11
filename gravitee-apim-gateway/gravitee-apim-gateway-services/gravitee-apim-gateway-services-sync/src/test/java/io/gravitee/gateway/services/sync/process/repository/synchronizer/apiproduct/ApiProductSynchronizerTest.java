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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.apiproduct;

import static io.gravitee.repository.management.model.Event.EventProperties.API_PRODUCT_ID;
import static io.gravitee.repository.management.model.EventType.DEPLOY_API_PRODUCT;
import static io.gravitee.repository.management.model.EventType.UNDEPLOY_API_PRODUCT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.gateway.services.sync.process.common.deployer.ApiProductDeployer;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.repository.fetcher.LatestEventFetcher;
import io.gravitee.gateway.services.sync.process.repository.mapper.ApiProductMapper;
import io.gravitee.gateway.services.sync.process.repository.service.EnvironmentService;
import io.gravitee.repository.management.model.Event;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Arpit Mishra (arpit.mishra at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiProductSynchronizerTest {

    private final ObjectMapper objectMapper = new GraviteeMapper();

    @Mock
    private LatestEventFetcher latestEventFetcher;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private ApiProductPlanAppender apiProductPlanAppender;

    @Mock
    private DeployerFactory deployerFactory;

    @Mock
    private ApiProductDeployer apiProductDeployer;

    private ApiProductSynchronizer cut;

    @BeforeEach
    void setUp() {
        cut = new ApiProductSynchronizer(
            latestEventFetcher,
            new ApiProductMapper(objectMapper, environmentService),
            apiProductPlanAppender,
            deployerFactory,
            new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>()),
            new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>())
        );
        lenient()
            .when(apiProductPlanAppender.appends(any(), any()))
            .thenAnswer(inv -> inv.getArgument(0));

        lenient().when(latestEventFetcher.bulkItems()).thenReturn(1);
        lenient().when(deployerFactory.createApiProductDeployer()).thenReturn(apiProductDeployer);
        lenient().when(apiProductDeployer.deploy(any())).thenReturn(Completable.complete());
        lenient().when(apiProductDeployer.undeploy(any())).thenReturn(Completable.complete());
        lenient().when(apiProductDeployer.doAfterDeployment(any())).thenReturn(Completable.complete());
        lenient().when(apiProductDeployer.doAfterUndeployment(any())).thenReturn(Completable.complete());
    }

    @Nested
    class NoEventTest {

        @Test
        void should_not_synchronize_api_product_when_no_events() throws InterruptedException {
            when(latestEventFetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.empty());
            cut.synchronize(-1L, Instant.now().toEpochMilli(), Set.of()).test().await().assertComplete();

            verifyNoInteractions(apiProductDeployer);
        }

        @Test
        void should_not_synchronize_api_product_when_events_with_unknown_type() throws InterruptedException {
            Event event = new Event();
            event.setId("event");
            when(latestEventFetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(event)));
            cut.synchronize(-1L, Instant.now().toEpochMilli(), Set.of()).test().await().assertComplete();

            verifyNoInteractions(apiProductDeployer);
        }

        @Test
        void should_fetch_init_events() throws InterruptedException {
            when(latestEventFetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.empty());
            cut.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("event")).test().await().assertComplete();
            verify(latestEventFetcher).fetchLatest(eq(-1L), any(), eq(API_PRODUCT_ID), eq(Set.of("event")), eq(Set.of(DEPLOY_API_PRODUCT)));
        }

        @Test
        void should_fetch_incremental_events() throws InterruptedException {
            when(latestEventFetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.empty());
            cut.synchronize(Instant.now().toEpochMilli(), Instant.now().toEpochMilli(), Set.of("event")).test().await().assertComplete();
            verify(latestEventFetcher).fetchLatest(
                any(),
                any(),
                eq(API_PRODUCT_ID),
                eq(Set.of("event")),
                eq(Set.of(DEPLOY_API_PRODUCT, UNDEPLOY_API_PRODUCT))
            );
        }
    }

    @Nested
    class DeployEventTest {

        @Test
        void should_deploy_api_product_when_fetching_deploy_events() throws InterruptedException, JsonProcessingException {
            Event event = new Event();
            event.setId("event-id");
            event.setType(DEPLOY_API_PRODUCT);
            event.setCreatedAt(new Date());
            event.setPayload(
                objectMapper.writeValueAsString(
                    Map.of(
                        "id",
                        "api-product-123",
                        "name",
                        "Test Product",
                        "description",
                        "Test Description",
                        "version",
                        "1.0",
                        "apiIds",
                        Set.of("api-1", "api-2"),
                        "environmentId",
                        "env-id"
                    )
                )
            );

            when(latestEventFetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(event)));
            cut.synchronize(-1L, Instant.now().toEpochMilli(), Set.of()).test().await().assertComplete();

            verify(apiProductDeployer).deploy(any());
            verify(apiProductDeployer).doAfterDeployment(any());
        }

        @Test
        void should_deploy_multiple_api_products_when_fetching_multiple_deploy_events()
            throws InterruptedException, JsonProcessingException {
            Event event1 = createDeployEvent("api-product-1", "Product 1");
            Event event2 = createDeployEvent("api-product-2", "Product 2");
            Event event3 = createDeployEvent("api-product-3", "Product 3");

            when(latestEventFetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(
                Flowable.just(List.of(event1, event2, event3))
            );
            cut.synchronize(-1L, Instant.now().toEpochMilli(), Set.of()).test().await().assertComplete();

            verify(apiProductDeployer, times(3)).deploy(any());
            verify(apiProductDeployer, times(3)).doAfterDeployment(any());
        }

        @Test
        void should_handle_deploy_error_gracefully() throws InterruptedException, JsonProcessingException {
            Event event1 = createDeployEvent("api-product-success", "Success Product");
            Event event2 = createDeployEvent("api-product-fail", "Fail Product");

            when(latestEventFetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(event1, event2)));
            when(apiProductDeployer.deploy(any()))
                .thenReturn(Completable.complete())
                .thenReturn(Completable.error(new RuntimeException("Deploy failed")));

            cut.synchronize(-1L, Instant.now().toEpochMilli(), Set.of()).test().await().assertComplete();

            verify(apiProductDeployer, times(2)).deploy(any());
        }

        private Event createDeployEvent(String apiProductId, String name) throws JsonProcessingException {
            Event event = new Event();
            event.setId("event-" + apiProductId);
            event.setType(DEPLOY_API_PRODUCT);
            event.setCreatedAt(new Date());
            event.setPayload(
                objectMapper.writeValueAsString(
                    Map.of("id", apiProductId, "name", name, "version", "1.0", "apiIds", Set.of("api-1"), "environmentId", "env-id")
                )
            );
            return event;
        }
    }

    @Nested
    class UndeployEventTest {

        @Test
        void should_undeploy_api_product_when_fetching_undeploy_events() throws InterruptedException {
            Event event = new Event();
            event.setId("event-id");
            event.setType(UNDEPLOY_API_PRODUCT);
            event.setProperties(Map.of(API_PRODUCT_ID.getValue(), "api-product-123"));

            when(latestEventFetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(event)));
            cut.synchronize(-1L, Instant.now().toEpochMilli(), Set.of()).test().await().assertComplete();

            verify(apiProductDeployer).undeploy(any());
            verify(apiProductDeployer).doAfterUndeployment(any());
        }

        @Test
        void should_undeploy_multiple_api_products_when_fetching_multiple_undeploy_events() throws InterruptedException {
            Event event1 = createUndeployEvent("api-product-1");
            Event event2 = createUndeployEvent("api-product-2");
            Event event3 = createUndeployEvent("api-product-3");

            when(latestEventFetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(
                Flowable.just(List.of(event1, event2, event3))
            );
            cut.synchronize(-1L, Instant.now().toEpochMilli(), Set.of()).test().await().assertComplete();

            verify(apiProductDeployer, times(3)).undeploy(any());
            verify(apiProductDeployer, times(3)).doAfterUndeployment(any());
        }

        @Test
        void should_skip_undeploy_when_api_product_id_missing() throws InterruptedException {
            Event event = new Event();
            event.setId("event-id");
            event.setType(UNDEPLOY_API_PRODUCT);
            event.setProperties(Map.of("other-property", "value"));

            when(latestEventFetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(event)));
            cut.synchronize(-1L, Instant.now().toEpochMilli(), Set.of()).test().await().assertComplete();

            verifyNoInteractions(apiProductDeployer);
        }

        @Test
        void should_handle_undeploy_error_gracefully() throws InterruptedException {
            Event event1 = createUndeployEvent("api-product-success");
            Event event2 = createUndeployEvent("api-product-fail");

            when(latestEventFetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(event1, event2)));
            when(apiProductDeployer.undeploy(any()))
                .thenReturn(Completable.complete())
                .thenReturn(Completable.error(new RuntimeException("Undeploy failed")));

            cut.synchronize(-1L, Instant.now().toEpochMilli(), Set.of()).test().await().assertComplete();

            verify(apiProductDeployer, times(2)).undeploy(any());
        }

        private Event createUndeployEvent(String apiProductId) {
            Event event = new Event();
            event.setId("event-" + apiProductId);
            event.setType(UNDEPLOY_API_PRODUCT);
            event.setProperties(Map.of(API_PRODUCT_ID.getValue(), apiProductId));
            return event;
        }
    }

    @Nested
    class MixedEventsTest {

        @Test
        void should_handle_both_deploy_and_undeploy_events() throws InterruptedException, JsonProcessingException {
            Event deployEvent = new Event();
            deployEvent.setId("deploy-event");
            deployEvent.setType(DEPLOY_API_PRODUCT);
            deployEvent.setCreatedAt(new Date());
            deployEvent.setPayload(
                objectMapper.writeValueAsString(
                    Map.of(
                        "id",
                        "api-product-deploy",
                        "name",
                        "Deploy Product",
                        "version",
                        "1.0",
                        "apiIds",
                        Set.of("api-1"),
                        "environmentId",
                        "env-id"
                    )
                )
            );

            Event undeployEvent = new Event();
            undeployEvent.setId("undeploy-event");
            undeployEvent.setType(UNDEPLOY_API_PRODUCT);
            undeployEvent.setProperties(Map.of(API_PRODUCT_ID.getValue(), "api-product-undeploy"));

            when(latestEventFetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(
                Flowable.just(List.of(deployEvent, undeployEvent))
            );
            cut.synchronize(-1L, Instant.now().toEpochMilli(), Set.of()).test().await().assertComplete();

            verify(apiProductDeployer).deploy(any());
            verify(apiProductDeployer).doAfterDeployment(any());
            verify(apiProductDeployer).undeploy(any());
            verify(apiProductDeployer).doAfterUndeployment(any());
        }
    }

    @Nested
    class SynchronizerPropertiesTest {

        @Test
        void should_return_correct_order() {
            assertThat(cut.order()).isEqualTo(7);
        }
    }
}

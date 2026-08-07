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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import appender.MemoryAppender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.services.sync.process.common.mapper.SubscriptionMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.SubscriptionCursor;
import io.gravitee.repository.management.model.SubscriptionReferenceType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SubscriptionAppenderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final MemoryAppender memoryAppender = new MemoryAppender();

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private io.gravitee.gateway.handlers.api.registry.ApiProductRegistry apiProductRegistry;

    private SubscriptionAppender cut;

    private static final int BULK_ITEMS = 2;

    @BeforeEach
    public void beforeEach() {
        when(apiProductRegistry.getApiProductPlanEntriesForApi(any(), any())).thenReturn(List.of());
        SubscriptionMapper subscriptionMapper = new SubscriptionMapper(objectMapper, apiProductRegistry);
        cut = new SubscriptionAppender(subscriptionRepository, subscriptionMapper, apiProductRegistry, BULK_ITEMS);
        memoryAppender.reset();
    }

    @Test
    void should_do_nothing_when_no_subscriptions_for_given_deployable() {
        ApiReactorDeployable apiReactorDeployable1 = ApiReactorDeployable.builder()
            .apiId("api1")
            .reactableApi(mock(ReactableApi.class))
            .subscribablePlans(new HashSet<>(Set.of("plan1")))
            .apiKeyPlans(new HashSet<>(Set.of("plan1")))
            .build();
        ApiReactorDeployable apiReactorDeployable2 = ApiReactorDeployable.builder()
            .apiId("api2")
            .reactableApi(mock(ReactableApi.class))
            .subscribablePlans(new HashSet<>(Set.of("plan2")))
            .apiKeyPlans(new HashSet<>(Set.of("plan2")))
            .build();
        List<ApiReactorDeployable> appends = cut.appends(true, List.of(apiReactorDeployable1, apiReactorDeployable2), Set.of("env"));
        assertThat(appends).hasSize(2);
        assertThat(appends.get(0).subscriptions()).isEmpty();
        assertThat(appends.get(1).subscriptions()).isEmpty();
    }

    @Test
    void should_appends_subscriptions_for_given_deployable() throws TechnicalException {
        ApiReactorDeployable apiReactorDeployable1 = ApiReactorDeployable.builder()
            .apiId("api1")
            .reactableApi(mock(ReactableApi.class))
            .subscribablePlans(new HashSet<>(Set.of("plan1")))
            .apiKeyPlans(new HashSet<>(Set.of("plan1")))
            .build();
        io.gravitee.repository.management.model.Subscription subscription1 = new io.gravitee.repository.management.model.Subscription();
        subscription1.setId("sub1");
        subscription1.setApi("api1");
        io.gravitee.repository.management.model.Subscription subscription2 = new io.gravitee.repository.management.model.Subscription();
        subscription2.setId("sub2");
        subscription2.setApi("api1");
        when(
            subscriptionRepository.searchAfter(
                argThat(
                    argument -> argument != null && argument.getPlans().contains("plan1") && argument.getEnvironments().contains("env")
                ),
                any(),
                isNull(),
                eq(BULK_ITEMS)
            )
        ).thenReturn(List.of(subscription1, subscription2));
        when(subscriptionRepository.searchAfter(any(), any(), argThat(c -> c != null && "sub2".equals(c.id())), eq(BULK_ITEMS))).thenReturn(
            List.of()
        );
        ApiReactorDeployable apiReactorDeployable2 = ApiReactorDeployable.builder()
            .apiId("api2")
            .reactableApi(mock(ReactableApi.class))
            .subscribablePlans(new HashSet<>(Set.of("nosubscriptionplan")))
            .apiKeyPlans(new HashSet<>(Set.of("nosubscriptionplan")))
            .build();
        List<ApiReactorDeployable> deployables = cut.appends(true, List.of(apiReactorDeployable1, apiReactorDeployable2), Set.of("env"));
        assertThat(deployables).hasSize(2);
        assertThat(deployables.get(0).subscriptions()).hasSize(2);
        assertThat(deployables.get(1).subscriptions()).isEmpty();
    }

    @Test
    void should_aggregate_subscriptions_across_multiple_pages() throws TechnicalException {
        ApiReactorDeployable deployable = ApiReactorDeployable.builder()
            .apiId("api1")
            .reactableApi(mock(ReactableApi.class))
            .subscribablePlans(new HashSet<>(Set.of("plan1")))
            .apiKeyPlans(new HashSet<>(Set.of("plan1")))
            .build();

        io.gravitee.repository.management.model.Subscription a = sub("sub-a");
        io.gravitee.repository.management.model.Subscription b = sub("sub-b");
        io.gravitee.repository.management.model.Subscription c = sub("sub-c");

        when(subscriptionRepository.searchAfter(any(), any(), isNull(), eq(BULK_ITEMS))).thenReturn(List.of(a, b));
        when(
            subscriptionRepository.searchAfter(any(), any(), argThat(cur -> cur != null && "sub-b".equals(cur.id())), eq(BULK_ITEMS))
        ).thenReturn(List.of(c));

        List<ApiReactorDeployable> deployables = cut.appends(true, List.of(deployable), Set.of("env"));
        assertThat(deployables).hasSize(1);
        assertThat(deployables.get(0).subscriptions()).hasSize(3);
    }

    private static io.gravitee.repository.management.model.Subscription sub(String id) {
        io.gravitee.repository.management.model.Subscription s = new io.gravitee.repository.management.model.Subscription();
        s.setId(id);
        s.setApi("api1");
        return s;
    }

    @Test
    void should_expand_api_product_subscription_only_for_apis_in_current_batch() throws TechnicalException {
        configureMemoryAppender();
        ApiReactorDeployable apiReactorDeployable1 = ApiReactorDeployable.builder()
            .apiId("api1")
            .reactableApi(mock(ReactableApi.class))
            .subscribablePlans(new HashSet<>(Set.of("product-plan")))
            .apiKeyPlans(new HashSet<>(Set.of("product-plan")))
            .build();
        ApiReactorDeployable apiReactorDeployable2 = ApiReactorDeployable.builder()
            .apiId("api2")
            .reactableApi(mock(ReactableApi.class))
            .subscribablePlans(new HashSet<>(Set.of("nosubscriptionplan")))
            .apiKeyPlans(new HashSet<>(Set.of("nosubscriptionplan")))
            .build();

        ReactableApiProduct product = ReactableApiProduct.builder().id("product-1").apiIds(Set.of("api1", "api3")).build();
        when(apiProductRegistry.get("product-1", "env")).thenReturn(product);

        io.gravitee.repository.management.model.Subscription productSubscription =
            new io.gravitee.repository.management.model.Subscription();
        productSubscription.setId("sub1");
        productSubscription.setPlan("product-plan");
        productSubscription.setReferenceType(SubscriptionReferenceType.API_PRODUCT);
        productSubscription.setReferenceId("product-1");
        productSubscription.setEnvironmentId("env");
        when(subscriptionRepository.searchAfter(any(), any(), isNull(), eq(BULK_ITEMS))).thenReturn(List.of(productSubscription));

        List<ApiReactorDeployable> deployables = cut.appends(true, List.of(apiReactorDeployable1, apiReactorDeployable2), Set.of("env"));

        assertThat(deployables).hasSize(2);
        assertThat(deployables.get(0).subscriptions())
            .singleElement()
            .satisfies(subscription -> {
                assertThat(subscription.getId()).isEqualTo("sub1");
                assertThat(subscription.getApi()).isEqualTo("api1");
                assertThat(subscription.getApiProductId()).isEqualTo("product-1");
            });
        assertThat(deployables.get(1).subscriptions()).isEmpty();
        assertThat(memoryAppender.getLoggedEvents()).isEmpty();
    }

    private void configureMemoryAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(SubscriptionAppender.class);
        memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.setLevel(Level.WARN);
        logger.addAppender(memoryAppender);
        memoryAppender.start();
    }
}

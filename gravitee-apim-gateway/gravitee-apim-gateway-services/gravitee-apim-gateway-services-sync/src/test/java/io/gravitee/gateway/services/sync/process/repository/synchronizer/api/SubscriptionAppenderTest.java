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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import appender.MemoryAppender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.services.sync.process.repository.mapper.SubscriptionMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionRepository;
import java.util.List;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
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

    private SubscriptionAppender cut;

    @BeforeEach
    public void beforeEach() {
        SubscriptionMapper subscriptionMapper = new SubscriptionMapper(objectMapper);
        cut = new SubscriptionAppender(subscriptionRepository, subscriptionMapper);
        memoryAppender.reset();
    }

    @Test
    void should_do_nothing_when_no_subscriptions_for_given_deployable() {
        ApiReactorDeployable apiReactorDeployable1 = ApiReactorDeployable
            .builder()
            .apiId("api1")
            .reactableApi(mock(ReactableApi.class))
            .subscribablePlans(Set.of("plan1"))
            .build();
        ApiReactorDeployable apiReactorDeployable2 = ApiReactorDeployable
            .builder()
            .apiId("api2")
            .reactableApi(mock(ReactableApi.class))
            .subscribablePlans(Set.of("plan2"))
            .build();
        List<ApiReactorDeployable> appends = cut.appends(true, List.of(apiReactorDeployable1, apiReactorDeployable2), Set.of("env"));
        assertThat(appends).hasSize(2);
        assertThat(appends.get(0).subscriptions()).isEmpty();
        assertThat(appends.get(1).subscriptions()).isEmpty();
    }

    @Test
    void should_appends_subscriptions_for_given_deployable() throws TechnicalException {
        ApiReactorDeployable apiReactorDeployable1 = ApiReactorDeployable
            .builder()
            .apiId("api1")
            .reactableApi(mock(ReactableApi.class))
            .subscribablePlans(Set.of("plan1"))
            .build();
        io.gravitee.repository.management.model.Subscription subscription1 = new io.gravitee.repository.management.model.Subscription();
        subscription1.setId("sub1");
        subscription1.setApi("api1");
        io.gravitee.repository.management.model.Subscription subscription2 = new io.gravitee.repository.management.model.Subscription();
        subscription2.setId("sub2");
        subscription2.setApi("api1");
        when(
            subscriptionRepository.search(
                argThat(argument -> argument.getPlans().contains("plan1") && argument.getEnvironments().contains("env")),
                any()
            )
        )
            .thenReturn(List.of(subscription1, subscription2));
        ApiReactorDeployable apiReactorDeployable2 = ApiReactorDeployable
            .builder()
            .apiId("api2")
            .reactableApi(mock(ReactableApi.class))
            .subscribablePlans(Set.of("nosubscriptionplan"))
            .build();
        List<ApiReactorDeployable> deployables = cut.appends(true, List.of(apiReactorDeployable1, apiReactorDeployable2), Set.of("env"));
        assertThat(deployables).hasSize(2);
        assertThat(deployables.get(0).subscriptions()).hasSize(2);
        assertThat(deployables.get(1).subscriptions()).isEmpty();
    }

    @Test
    void should_ignore_and_log_subscriptions_with_missing_deployable() throws TechnicalException {
        configureMemoryAppender();
        ApiReactorDeployable apiReactorDeployable1 = ApiReactorDeployable
            .builder()
            .apiId("api1")
            .reactableApi(mock(ReactableApi.class))
            .subscribablePlans(Set.of("plan1"))
            .build();
        io.gravitee.repository.management.model.Subscription subscription1 = new io.gravitee.repository.management.model.Subscription();
        subscription1.setId("sub1");
        subscription1.setApi("api1");
        io.gravitee.repository.management.model.Subscription subscription2 = new io.gravitee.repository.management.model.Subscription();
        subscription2.setId("sub2");
        subscription2.setApi("api3");
        io.gravitee.repository.management.model.Subscription subscription3 = new io.gravitee.repository.management.model.Subscription();
        subscription3.setId("sub3");
        subscription3.setApi("api3");
        when(subscriptionRepository.search(any(), any())).thenReturn(List.of(subscription1, subscription2, subscription3));
        ApiReactorDeployable apiReactorDeployable2 = ApiReactorDeployable
            .builder()
            .apiId("api2")
            .reactableApi(mock(ReactableApi.class))
            .subscribablePlans(Set.of("nosubscriptionplan"))
            .build();
        List<ApiReactorDeployable> deployables = cut.appends(true, List.of(apiReactorDeployable1, apiReactorDeployable2), Set.of("env"));
        assertThat(deployables).hasSize(2);
        assertThat(deployables.get(0).subscriptions()).hasSize(1);
        assertThat(deployables.get(1).subscriptions()).isEmpty();

        Assertions.assertThat(memoryAppender.getLoggedEvents()).hasSize(1);
        SoftAssertions.assertSoftly(soft -> {
            var event = memoryAppender.getLoggedEvents().get(0);
            soft.assertThat(event.getMessage()).contains("Cannot find api {} for subscriptions [{}]");
            soft.assertThat(event.getArgumentArray()).contains("api3", "sub2,sub3");
        });
    }

    private void configureMemoryAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(SubscriptionAppender.class);
        memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.setLevel(Level.WARN);
        logger.addAppender(memoryAppender);
        memoryAppender.start();
    }
}

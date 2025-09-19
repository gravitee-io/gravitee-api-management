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

import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.services.sync.process.repository.mapper.ApiKeyMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.model.ApiKey;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiKeyAppenderTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    private ApiKeyAppender cut;

    @BeforeEach
    public void beforeEach() {
        cut = new ApiKeyAppender(apiKeyRepository, new ApiKeyMapper());
    }

    @Test
    void should_do_nothing_when_no_apikeys_for_given_deployables() {
        ApiReactorDeployable apiReactorDeployable1 = ApiReactorDeployable.builder()
            .apiId("api1")
            .reactableApi(mock(ReactableApi.class))
            .subscriptions(List.of(Subscription.builder().id("subscription1").plan("plan1").build()))
            .apiKeyPlans(Set.of("plan1"))
            .build();
        ApiReactorDeployable apiReactorDeployable2 = ApiReactorDeployable.builder()
            .apiId("api2")
            .reactableApi(mock(ReactableApi.class))
            .subscriptions(List.of(Subscription.builder().id("subscription2").plan("plan2").build()))
            .build();
        List<ApiReactorDeployable> appends = cut.appends(true, List.of(apiReactorDeployable1, apiReactorDeployable2), Set.of("env"));
        assertThat(appends).hasSize(2);
        assertThat(appends.get(0).apiKeys()).isEmpty();
        assertThat(appends.get(1).apiKeys()).isEmpty();
    }

    @Test
    void should_appends_apikeys_for_given_deployable() throws TechnicalException {
        ApiReactorDeployable apiReactorDeployable1 = ApiReactorDeployable.builder()
            .apiId("api1")
            .reactableApi(mock(ReactableApi.class))
            .subscriptions(
                List.of(
                    Subscription.builder().id("subscription1").api("api1").plan("plan1").build(),
                    Subscription.builder().id("subscription2").api("api1").plan("plan1").build()
                )
            )
            .apiKeyPlans(Set.of("plan1"))
            .build();
        ApiKey e1 = new ApiKey();
        e1.setSubscriptions(List.of("subscription1"));
        ApiKey e2 = new ApiKey();
        e2.setSubscriptions(List.of("subscription1"));
        when(apiKeyRepository.findByCriteria(argThat(argument -> argument.getEnvironments().equals(Set.of("env"))), any())).thenReturn(
            List.of(e1, e2)
        );
        ApiReactorDeployable apiReactorDeployable2 = ApiReactorDeployable.builder()
            .apiId("api2")
            .reactableApi(mock(ReactableApi.class))
            .subscriptions(List.of(Subscription.builder().id("subscription2").api("api2").plan("noapikeyplan").build()))
            .build();
        List<ApiReactorDeployable> deployables = cut.appends(true, List.of(apiReactorDeployable1, apiReactorDeployable2), Set.of("env"));
        assertThat(deployables).hasSize(2);
        assertThat(deployables.get(0).apiKeys()).hasSize(2);
        assertThat(deployables.get(1).apiKeys()).isEmpty();
    }
}

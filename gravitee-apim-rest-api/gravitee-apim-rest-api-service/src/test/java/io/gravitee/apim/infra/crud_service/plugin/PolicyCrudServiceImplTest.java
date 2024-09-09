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
package io.gravitee.apim.infra.crud_service.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.v4.policy.PolicyPluginEntity;
import io.gravitee.rest.api.service.exceptions.PluginNotFoundException;
import io.gravitee.rest.api.service.exceptions.PolicyNotFoundException;
import io.gravitee.rest.api.service.v4.PolicyPluginService;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class PolicyCrudServiceImplTest {

    PolicyPluginService policyService;

    PolicyCrudServiceImpl service;

    @BeforeEach
    void setUp() {
        policyService = mock(PolicyPluginService.class);

        service = new PolicyCrudServiceImpl(policyService);
    }

    @Nested
    class FindById {

        @Test
        void should_return_policy_and_adapt_it() {
            // Given
            var policyId = "policy-id";
            when(policyService.findById(policyId))
                .thenAnswer(invocation ->
                    PolicyPluginEntity
                        .builder()
                        .id(invocation.getArgument(0))
                        .name("name")
                        .description("description")
                        .icon("icon")
                        .feature("feature")
                        .deployed(true)
                        .category("category")
                        .version("1")
                        .build()
                );

            // When
            var policy = service.get(policyId);

            // Then
            assertThat(policy.isPresent()).isTrue();

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(policy.get().getId()).isEqualTo(policyId);
                soft.assertThat(policy.get().getName()).isEqualTo("name");
                soft.assertThat(policy.get().getDescription()).isEqualTo("description");
                soft.assertThat(policy.get().getIcon()).isEqualTo("icon");
                soft.assertThat(policy.get().getFeature()).isEqualTo("feature");
                soft.assertThat(policy.get().isDeployed()).isEqualTo(true);
                soft.assertThat(policy.get().getCategory()).isEqualTo("category");
                soft.assertThat(policy.get().getVersion()).isEqualTo("1");
            });
        }

        @Test
        void should_return_empty_when_no_policy_found() {
            // Given
            String policyId = "unknown";
            when(policyService.findById(policyId)).thenThrow(PluginNotFoundException.class);

            // When
            var policy = service.get(policyId);

            // Then
            assertThat(policy.isEmpty()).isTrue();
        }
    }
}

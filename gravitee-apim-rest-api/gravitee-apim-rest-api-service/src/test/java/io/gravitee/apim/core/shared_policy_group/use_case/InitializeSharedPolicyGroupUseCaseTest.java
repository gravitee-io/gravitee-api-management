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
package io.gravitee.apim.core.shared_policy_group.use_case;

import static io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroup.SharedPolicyGroupLifecycleState.DEPLOYED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import inmemory.EventCrudInMemory;
import inmemory.EventLatestCrudInMemory;
import inmemory.PolicyPluginCrudServiceInMemory;
import inmemory.SharedPolicyGroupCrudServiceInMemory;
import inmemory.SharedPolicyGroupHistoryCrudServiceInMemory;
import io.gravitee.apim.core.plugin.model.PolicyPlugin;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroup;
import io.gravitee.apim.infra.adapter.GraviteeJacksonMapper;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.model.EventType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class InitializeSharedPolicyGroupUseCaseTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private final String ORG_ID = "org-id";
    private final String ENV_ID = "env-id";

    private final SharedPolicyGroupCrudServiceInMemory sharedPolicyGroupCrudService = new SharedPolicyGroupCrudServiceInMemory();
    private final PolicyPluginCrudServiceInMemory policyPluginCrudService = new PolicyPluginCrudServiceInMemory();
    private final EventCrudInMemory eventCrudInMemory = new EventCrudInMemory();
    private final EventLatestCrudInMemory eventLatestCrudInMemory = new EventLatestCrudInMemory();
    private final SharedPolicyGroupHistoryCrudServiceInMemory sharedPolicyGroupHistoryCrudService =
        new SharedPolicyGroupHistoryCrudServiceInMemory();
    private InitializeSharedPolicyGroupUseCase initializeSharedPolicyGroupUseCase;

    @BeforeAll
    static void beforeAll() {
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
    }

    @AfterAll
    static void afterAll() {
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @BeforeEach
    void setUp() {
        initializeSharedPolicyGroupUseCase =
            new InitializeSharedPolicyGroupUseCase(
                sharedPolicyGroupCrudService,
                policyPluginCrudService,
                eventCrudInMemory,
                eventLatestCrudInMemory,
                sharedPolicyGroupHistoryCrudService
            );
    }

    @Test
    void should_initialize_all_shared_policy_group() {
        // Given
        policyPluginCrudService.initWith(
            List.of(
                PolicyPlugin.builder().id("groovy").name("Groovy").build(),
                PolicyPlugin.builder().id("rate-limit").name("Rate Limit").build(),
                PolicyPlugin.builder().id("policy-assign-attributes").name("Assign Attributes").build(),
                PolicyPlugin.builder().id("policy-http-callout").name("HTTP Callout").build(),
                PolicyPlugin.builder().id("policy-assign-content").name("Assign Content").build(),
                PolicyPlugin.builder().id("dynamic-routing").name("Dynamic Routing").build()
            )
        );

        // When
        initializeSharedPolicyGroupUseCase.execute(
            InitializeSharedPolicyGroupUseCase.Input.builder().organizationId(ORG_ID).environmentId(ENV_ID).build()
        );

        // Then
        // - Check shared policy group is created
        assertThat(sharedPolicyGroupCrudService.storage()).hasSize(3);
        assertThat(sharedPolicyGroupCrudService.storage())
            .extracting(
                SharedPolicyGroup::getName,
                SharedPolicyGroup::getLifecycleState,
                SharedPolicyGroup::getVersion,
                SharedPolicyGroup::getApiType,
                SharedPolicyGroup::getPhase,
                SharedPolicyGroup::getEnvironmentId
            )
            .containsExactly(
                tuple("\uD83E\uDD16 AI - Redirect to HuggingFace", DEPLOYED, 0, ApiType.PROXY, PolicyPlugin.ExecutionPhase.REQUEST, ENV_ID),
                tuple(
                    "\uD83E\uDD16 AI - Prompt Templating Example",
                    DEPLOYED,
                    0,
                    ApiType.PROXY,
                    PolicyPlugin.ExecutionPhase.REQUEST,
                    ENV_ID
                ),
                tuple(
                    "\uD83E\uDD16 AI - Rate Limit & Request token limit",
                    DEPLOYED,
                    0,
                    ApiType.PROXY,
                    PolicyPlugin.ExecutionPhase.REQUEST,
                    ENV_ID
                )
            );

        // - Check events
        assertThat(eventCrudInMemory.storage())
            .hasSize(3)
            .first()
            .satisfies(event -> {
                assertThat(event.getEnvironments()).containsExactly(ENV_ID);
                assertThat(event.getType()).isEqualTo(EventType.DEPLOY_SHARED_POLICY_GROUP);
                final io.gravitee.definition.model.v4.sharedpolicygroup.SharedPolicyGroup sharedPolicyGroupDefinition =
                    GraviteeJacksonMapper
                        .getInstance()
                        .readValue(event.getPayload(), io.gravitee.definition.model.v4.sharedpolicygroup.SharedPolicyGroup.class);
                assertThat(sharedPolicyGroupDefinition.getEnvironmentId()).isEqualTo(ENV_ID);
                assertThat(sharedPolicyGroupDefinition.getPhase())
                    .isEqualTo(io.gravitee.definition.model.v4.sharedpolicygroup.SharedPolicyGroup.Phase.REQUEST);
            });
        assertThat(eventLatestCrudInMemory.storage())
            .hasSize(3)
            .first()
            .satisfies(event -> {
                assertThat(event.getEnvironments()).containsExactly(ENV_ID);
                assertThat(event.getType()).isEqualTo(EventType.DEPLOY_SHARED_POLICY_GROUP);
                final io.gravitee.definition.model.v4.sharedpolicygroup.SharedPolicyGroup sharedPolicyGroupDefinition =
                    GraviteeJacksonMapper
                        .getInstance()
                        .readValue(event.getPayload(), io.gravitee.definition.model.v4.sharedpolicygroup.SharedPolicyGroup.class);
                assertThat(sharedPolicyGroupDefinition.getEnvironmentId()).isEqualTo(ENV_ID);
                assertThat(sharedPolicyGroupDefinition.getPhase())
                    .isEqualTo(io.gravitee.definition.model.v4.sharedpolicygroup.SharedPolicyGroup.Phase.REQUEST);
            });

        // - Check shared policy group history is created
        assertThat(sharedPolicyGroupHistoryCrudService.storage()).hasSize(3);
    }

    @Test
    void should_not_throw_exception_when_shared_policy_group_have_missing_policies() {
        // Given
        policyPluginCrudService.initWith(
            List.of(
                PolicyPlugin.builder().id("groovy").name("Groovy").build(),
                PolicyPlugin.builder().id("rate-limit").name("Rate Limit").build()
            )
        );

        // When
        initializeSharedPolicyGroupUseCase.execute(
            InitializeSharedPolicyGroupUseCase.Input.builder().organizationId(ORG_ID).environmentId(ENV_ID).build()
        );

        // Then
        // Only one example is created as the other two have missing policies
        assertThat(sharedPolicyGroupCrudService.storage()).hasSize(1);
        assertThat(eventCrudInMemory.storage()).hasSize(1);
        assertThat(eventLatestCrudInMemory.storage()).hasSize(1);
        assertThat(sharedPolicyGroupHistoryCrudService.storage()).hasSize(1);
    }
}

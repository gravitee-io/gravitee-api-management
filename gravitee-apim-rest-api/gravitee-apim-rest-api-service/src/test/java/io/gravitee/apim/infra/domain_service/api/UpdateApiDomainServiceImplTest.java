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
package io.gravitee.apim.infra.domain_service.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import fixtures.core.model.ApiFixtures;
import fixtures.core.model.AuditInfoFixtures;
import inmemory.ApiCrudServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.definition.model.ResponseTemplate;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.failover.Failover;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.execution.FlowExecution;
import io.gravitee.definition.model.v4.service.ApiServices;
import io.gravitee.rest.api.model.v4.api.UpdateApiEntity;
import io.gravitee.rest.api.service.v4.ApiService;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UpdateApiDomainServiceImplTest {

    private static final String ORGANIZATION_ID = "org-id";
    private static final String ENVIRONMENT_ID = "env-id";
    private static final String USER_ID = "user-id";

    ApiService delegate = mock(ApiService.class);
    ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    AuditInfo auditInfo = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);

    UpdateApiDomainServiceImpl cut;

    @BeforeEach
    void setUp() {
        cut = new UpdateApiDomainServiceImpl(delegate, apiCrudService);
    }

    private void stubValidate(Consumer<UpdateApiEntity> mutator) {
        doAnswer(inv -> {
            mutator.accept(inv.getArgument(2));
            return null;
        })
            .when(delegate)
            .validate(any(), any(), any(), any());
    }

    @Test
    void should_return_sanitized_tags_from_mutated_update_api_entity() {
        var api = ApiFixtures.aProxyApiV4();
        stubValidate(entity -> entity.setTags(Set.of("sanitized-tag")));

        var result = cut.validateV4(api, auditInfo);

        assertThat(result.getApiDefinitionHttpV4().getTags()).containsExactly("sanitized-tag");
    }

    @Test
    void should_return_sanitized_groups_from_mutated_update_api_entity() {
        var api = ApiFixtures.aProxyApiV4().toBuilder().groups(new HashSet<>(Set.of("requested-group"))).build();
        stubValidate(entity -> entity.setGroups(Set.of("filtered-group")));

        var result = cut.validateV4(api, auditInfo);

        assertThat(result.getGroups()).containsExactly("filtered-group");
    }

    @Test
    void should_return_sanitized_lifecycle_state() {
        var api = ApiFixtures.aProxyApiV4().toBuilder().apiLifecycleState(Api.ApiLifecycleState.CREATED).build();
        stubValidate(entity -> entity.setLifecycleState(io.gravitee.rest.api.model.api.ApiLifecycleState.PUBLISHED));

        var result = cut.validateV4(api, auditInfo);

        assertThat(result.getApiLifecycleState()).isEqualTo(Api.ApiLifecycleState.PUBLISHED);
    }

    @Test
    void should_return_sanitized_analytics() {
        var api = ApiFixtures.aProxyApiV4();
        var sanitizedAnalytics = Analytics.builder().enabled(false).build();
        stubValidate(entity -> entity.setAnalytics(sanitizedAnalytics));

        var result = cut.validateV4(api, auditInfo);

        assertThat(result.getApiDefinitionHttpV4().getAnalytics()).isEqualTo(sanitizedAnalytics);
    }

    @Test
    void should_preserve_non_mutated_fields() {
        var api = ApiFixtures.aProxyApiV4();

        var result = cut.validateV4(api, auditInfo);

        assertThat(result.getName()).isEqualTo(api.getName());
        assertThat(result.getDescription()).isEqualTo(api.getDescription());
        assertThat(result.getVersion()).isEqualTo(api.getVersion());
        assertThat(result.getVisibility()).isEqualTo(api.getVisibility());
        assertThat(result.getCategories()).isEqualTo(api.getCategories());
    }

    @Test
    void should_preserve_original_tags_when_validator_returns_null() {
        var api = ApiFixtures.aProxyApiV4();
        var originalTags = api.getApiDefinitionHttpV4().getTags();
        stubValidate(entity -> entity.setTags(null));

        var result = cut.validateV4(api, auditInfo);

        assertThat(result.getApiDefinitionHttpV4().getTags()).isEqualTo(originalTags);
    }

    @Test
    void should_preserve_original_analytics_when_validator_returns_null() {
        var api = ApiFixtures.aProxyApiV4();
        var originalAnalytics = api.getApiDefinitionHttpV4().getAnalytics();
        stubValidate(entity -> entity.setAnalytics(null));

        var result = cut.validateV4(api, auditInfo);

        assertThat(result.getApiDefinitionHttpV4().getAnalytics()).isEqualTo(originalAnalytics);
    }

    @Test
    void should_preserve_original_groups_when_validator_returns_null() {
        var api = ApiFixtures.aProxyApiV4().toBuilder().groups(new HashSet<>(Set.of("group-1"))).build();
        var originalGroups = api.getGroups();
        stubValidate(entity -> entity.setGroups(null));

        var result = cut.validateV4(api, auditInfo);

        assertThat(result.getGroups()).isEqualTo(originalGroups);
    }

    @Test
    void should_preserve_lifecycle_state_when_validator_does_not_set_one() {
        var api = ApiFixtures.aProxyApiV4().toBuilder().apiLifecycleState(Api.ApiLifecycleState.PUBLISHED).build();
        stubValidate(entity -> entity.setLifecycleState(null));

        var result = cut.validateV4(api, auditInfo);

        assertThat(result.getApiLifecycleState()).isEqualTo(Api.ApiLifecycleState.PUBLISHED);
    }

    @Test
    void should_return_sanitized_failover_from_mutated_update_api_entity() {
        var api = ApiFixtures.aProxyApiV4();
        var sanitizedFailover = Failover.builder().enabled(true).build();
        stubValidate(entity -> entity.setFailover(sanitizedFailover));

        var result = cut.validateV4(api, auditInfo);

        assertThat(result.getApiDefinitionHttpV4().getFailover()).isEqualTo(sanitizedFailover);
    }

    @Test
    void should_return_sanitized_flow_execution_from_mutated_update_api_entity() {
        var api = ApiFixtures.aProxyApiV4();
        var sanitizedFlowExecution = new FlowExecution();
        stubValidate(entity -> entity.setFlowExecution(sanitizedFlowExecution));

        var result = cut.validateV4(api, auditInfo);

        assertThat(result.getApiDefinitionHttpV4().getFlowExecution()).isEqualTo(sanitizedFlowExecution);
    }

    @Test
    void should_return_sanitized_services_from_mutated_update_api_entity() {
        var api = ApiFixtures.aProxyApiV4();
        var sanitizedServices = new ApiServices();
        stubValidate(entity -> entity.setServices(sanitizedServices));

        var result = cut.validateV4(api, auditInfo);

        assertThat(result.getApiDefinitionHttpV4().getServices()).isEqualTo(sanitizedServices);
    }

    @Test
    void should_return_sanitized_response_templates_from_mutated_update_api_entity() {
        var api = ApiFixtures.aProxyApiV4();
        Map<String, Map<String, ResponseTemplate>> sanitizedResponseTemplates = Map.of("DEFAULT", Map.of());
        stubValidate(entity -> entity.setResponseTemplates(sanitizedResponseTemplates));

        var result = cut.validateV4(api, auditInfo);

        assertThat(result.getApiDefinitionHttpV4().getResponseTemplates()).isEqualTo(sanitizedResponseTemplates);
    }

    @Test
    void should_return_sanitized_flows_from_mutated_update_api_entity() {
        var api = ApiFixtures.aProxyApiV4();
        var sanitizedFlows = List.of(new Flow());
        stubValidate(entity -> entity.setFlows(sanitizedFlows));

        var result = cut.validateV4(api, auditInfo);

        assertThat(result.getApiDefinitionHttpV4().getFlows()).isEqualTo(sanitizedFlows);
        verify(delegate, never()).update(any(), any(), any(), anyBoolean(), any());
    }

    @Test
    void should_preserve_original_flows_when_validator_returns_null() {
        var originalFlows = List.of(new Flow());
        var originalDefinition = ApiFixtures.aProxyApiV4().getApiDefinitionHttpV4().toBuilder().flows(originalFlows).build();
        var api = ApiFixtures.aProxyApiV4().toBuilder().apiDefinitionHttpV4(originalDefinition).build();
        stubValidate(entity -> entity.setFlows(null));

        var result = cut.validateV4(api, auditInfo);

        assertThat(result.getApiDefinitionHttpV4().getFlows()).isEqualTo(originalFlows);
        verify(delegate, never()).update(any(), any(), any(), anyBoolean(), any());
    }

    @Test
    void should_preserve_original_allowed_in_api_products_when_validator_returns_null() {
        var originalDefinition = ApiFixtures.aProxyApiV4().getApiDefinitionHttpV4().toBuilder().allowedInApiProducts(true).build();
        var api = ApiFixtures.aProxyApiV4().toBuilder().apiDefinitionHttpV4(originalDefinition).build();
        stubValidate(entity -> entity.setAllowedInApiProducts(null));

        var result = cut.validateV4(api, auditInfo);

        assertThat(result.getApiDefinitionHttpV4().getAllowedInApiProducts()).isTrue();
    }
}

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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import fixtures.core.model.ApiFixtures;
import fixtures.core.model.AuditInfoFixtures;
import inmemory.ApiCrudServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.rest.api.model.v4.api.UpdateApiEntity;
import io.gravitee.rest.api.service.v4.ApiService;
import java.util.HashSet;
import java.util.Set;
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

    UpdateApiDomainServiceImpl cut;

    @BeforeEach
    void setUp() {
        cut = new UpdateApiDomainServiceImpl(delegate, apiCrudService);
    }

    @Test
    void validateV4_returns_sanitized_tags_from_mutated_update_api_entity() {
        var api = ApiFixtures.aProxyApiV4();
        doAnswer(inv -> {
            UpdateApiEntity entity = inv.getArgument(2);
            entity.setTags(Set.of("sanitized-tag"));
            return null;
        })
            .when(delegate)
            .validate(any(), any(), any(), any());

        var result = cut.validateV4(api, AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID));

        assertThat(result.getApiDefinitionHttpV4().getTags()).containsExactly("sanitized-tag");
    }

    @Test
    void validateV4_returns_sanitized_groups_from_mutated_update_api_entity() {
        var api = ApiFixtures.aProxyApiV4().toBuilder().groups(new HashSet<>(Set.of("requested-group"))).build();
        doAnswer(inv -> {
            UpdateApiEntity entity = inv.getArgument(2);
            entity.setGroups(Set.of("filtered-group"));
            return null;
        })
            .when(delegate)
            .validate(any(), any(), any(), any());

        var result = cut.validateV4(api, AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID));

        assertThat(result.getGroups()).containsExactly("filtered-group");
    }

    @Test
    void validateV4_returns_sanitized_lifecycle_state() {
        var api = ApiFixtures.aProxyApiV4().toBuilder().apiLifecycleState(Api.ApiLifecycleState.CREATED).build();
        doAnswer(inv -> {
            UpdateApiEntity entity = inv.getArgument(2);
            entity.setLifecycleState(io.gravitee.rest.api.model.api.ApiLifecycleState.PUBLISHED);
            return null;
        })
            .when(delegate)
            .validate(any(), any(), any(), any());

        var result = cut.validateV4(api, AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID));

        assertThat(result.getApiLifecycleState()).isEqualTo(Api.ApiLifecycleState.PUBLISHED);
    }

    @Test
    void validateV4_returns_sanitized_analytics() {
        var api = ApiFixtures.aProxyApiV4();
        var sanitizedAnalytics = Analytics.builder().enabled(false).build();
        doAnswer(inv -> {
            UpdateApiEntity entity = inv.getArgument(2);
            entity.setAnalytics(sanitizedAnalytics);
            return null;
        })
            .when(delegate)
            .validate(any(), any(), any(), any());

        var result = cut.validateV4(api, AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID));

        assertThat(result.getApiDefinitionHttpV4().getAnalytics()).isEqualTo(sanitizedAnalytics);
    }

    @Test
    void validateV4_preserves_non_mutated_fields() {
        var api = ApiFixtures.aProxyApiV4();

        var result = cut.validateV4(api, AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID));

        assertThat(result.getName()).isEqualTo(api.getName());
        assertThat(result.getDescription()).isEqualTo(api.getDescription());
        assertThat(result.getVersion()).isEqualTo(api.getVersion());
        assertThat(result.getVisibility()).isEqualTo(api.getVisibility());
        assertThat(result.getCategories()).isEqualTo(api.getCategories());
    }

    @Test
    void validateV4_preserves_original_tags_when_validator_returns_null() {
        var api = ApiFixtures.aProxyApiV4();
        var originalTags = api.getApiDefinitionHttpV4().getTags();
        doAnswer(inv -> {
            UpdateApiEntity entity = inv.getArgument(2);
            entity.setTags(null);
            return null;
        })
            .when(delegate)
            .validate(any(), any(), any(), any());

        var result = cut.validateV4(api, AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID));

        assertThat(result.getApiDefinitionHttpV4().getTags()).isEqualTo(originalTags);
    }

    @Test
    void validateV4_preserves_original_analytics_when_validator_returns_null() {
        var api = ApiFixtures.aProxyApiV4();
        var originalAnalytics = api.getApiDefinitionHttpV4().getAnalytics();
        doAnswer(inv -> {
            UpdateApiEntity entity = inv.getArgument(2);
            entity.setAnalytics(null);
            return null;
        })
            .when(delegate)
            .validate(any(), any(), any(), any());

        var result = cut.validateV4(api, AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID));

        assertThat(result.getApiDefinitionHttpV4().getAnalytics()).isEqualTo(originalAnalytics);
    }

    @Test
    void validateV4_preserves_original_groups_when_validator_returns_null() {
        var api = ApiFixtures.aProxyApiV4().toBuilder().groups(new HashSet<>(Set.of("group-1"))).build();
        var originalGroups = api.getGroups();
        doAnswer(inv -> {
            UpdateApiEntity entity = inv.getArgument(2);
            entity.setGroups(null);
            return null;
        })
            .when(delegate)
            .validate(any(), any(), any(), any());

        var result = cut.validateV4(api, AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID));

        assertThat(result.getGroups()).isEqualTo(originalGroups);
    }

    @Test
    void validateV4_preserves_lifecycle_state_when_validator_does_not_set_one() {
        var api = ApiFixtures.aProxyApiV4().toBuilder().apiLifecycleState(Api.ApiLifecycleState.PUBLISHED).build();
        doAnswer(inv -> {
            UpdateApiEntity entity = inv.getArgument(2);
            entity.setLifecycleState(null);
            return null;
        })
            .when(delegate)
            .validate(any(), any(), any(), any());

        var result = cut.validateV4(api, AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID));

        assertThat(result.getApiLifecycleState()).isEqualTo(Api.ApiLifecycleState.PUBLISHED);
    }
}

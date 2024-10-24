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
package io.gravitee.rest.api.management.v2.rest.resource.environment;

import static assertions.MAPIAssertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.SharedPolicyGroupFixtures;
import io.gravitee.apim.core.shared_policy_group.use_case.SearchSharedPolicyGroupHistoryUseCase;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SharedPolicyGroupResource_GetHistoriesTest extends AbstractResourceTest {

    private static final String ENV_ID = "my-env";
    private static final String SHARED_POLICY_GROUP_ID = "my-shared-policy-group";

    @Inject
    SearchSharedPolicyGroupHistoryUseCase searchSharedPolicyGroupHistoryUseCase;

    @Override
    protected String contextPath() {
        return "/environments/" + ENV_ID + "/shared-policy-groups/" + SHARED_POLICY_GROUP_ID + "/histories";
    }

    @BeforeEach
    void init() {
        super.setUp();
        GraviteeContext.cleanContext();

        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setId(ENV_ID);
        environmentEntity.setOrganizationId(ORGANIZATION);
        when(environmentService.findById(ENV_ID)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENV_ID)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENV_ID);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
    }

    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();
        reset(searchSharedPolicyGroupHistoryUseCase);
    }

    @Test
    void should_get_shared_policy_group_histories() {
        // Given
        var sharedPolicyGroup = SharedPolicyGroupFixtures.aSharedPolicyGroup();
        when(searchSharedPolicyGroupHistoryUseCase.execute(any()))
            .thenReturn(new SearchSharedPolicyGroupHistoryUseCase.Output(new Page<>(List.of(sharedPolicyGroup), 1, 1, 1)));

        // When
        var response = rootTarget().request().get();

        // Then
        assertThat(response).hasStatus(200);
        verify(searchSharedPolicyGroupHistoryUseCase)
            .execute(new SearchSharedPolicyGroupHistoryUseCase.Input(ENV_ID, SHARED_POLICY_GROUP_ID, new PageableImpl(0, 10), null));
    }

    @Test
    void should_get_shared_policy_group_histories_with_pagination_and_sorting() {
        // Given
        var sharedPolicyGroup = SharedPolicyGroupFixtures.aSharedPolicyGroup();
        when(searchSharedPolicyGroupHistoryUseCase.execute(any()))
            .thenReturn(new SearchSharedPolicyGroupHistoryUseCase.Output(new Page<>(List.of(sharedPolicyGroup), 1, 1, 1)));

        // When
        var response = rootTarget().queryParam("page", 2).queryParam("perPage", 1).queryParam("sortBy", "version").request().get();

        // Then
        assertThat(response).hasStatus(200);
        verify(searchSharedPolicyGroupHistoryUseCase)
            .execute(new SearchSharedPolicyGroupHistoryUseCase.Input(ENV_ID, SHARED_POLICY_GROUP_ID, new PageableImpl(2, 1), "version"));
    }
}

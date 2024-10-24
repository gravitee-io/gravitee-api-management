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
import io.gravitee.apim.core.shared_policy_group.use_case.GetSharedPolicyGroupPolicyPluginsUseCase;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SharedPolicyGroupsResource_GetPolicyPluginsTest extends AbstractResourceTest {

    private static final String ENV_ID = "my-env";

    @Inject
    GetSharedPolicyGroupPolicyPluginsUseCase getSharedPolicyGroupPolicyPluginsUseCase;

    @Override
    protected String contextPath() {
        return "/environments/" + ENV_ID + "/shared-policy-groups/policy-plugins";
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
        reset(getSharedPolicyGroupPolicyPluginsUseCase);
    }

    @Test
    void should_get_policy_plugins() {
        // Given
        when(getSharedPolicyGroupPolicyPluginsUseCase.execute(any()))
            .thenReturn(
                new GetSharedPolicyGroupPolicyPluginsUseCase.Output(List.of(SharedPolicyGroupFixtures.aSharedPolicyGroupPolicyPlugin()))
            );

        // When
        final Response response = rootTarget().request().get();

        // Then
        assertThat(response).hasStatus(200);
        verify(getSharedPolicyGroupPolicyPluginsUseCase).execute(new GetSharedPolicyGroupPolicyPluginsUseCase.Input(ENV_ID));
    }
}

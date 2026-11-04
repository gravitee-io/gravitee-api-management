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

import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import assertions.MAPIAssertions;
import fixtures.GuardianFixtures;
import io.gravitee.apim.core.guardian.use_case.SearchGuardianUseCase;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.management.v2.rest.model.GuardiansResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GuardiansResource_SearchTest extends AbstractResourceTest {

    private static final String ENV_ID = "my-env";

    @Inject
    SearchGuardianUseCase searchGuardianUseCase;

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

    @Override
    protected String contextPath() {
        return "/environments/" + ENV_ID + "/guardians";
    }

    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();
        reset(searchGuardianUseCase);
    }

    @Test
    void should_return_a_page_of_guardians() {
        when(searchGuardianUseCase.execute(any())).thenReturn(
            new SearchGuardianUseCase.Output(new Page<>(List.of(GuardianFixtures.aGuardian()), 1, 1, 1))
        );

        final Response response = rootTarget().request().get();

        assertThat(response.getStatus()).isEqualTo(OK_200);
        var body = response.readEntity(GuardiansResponse.class);
        assertThat(body.getData()).hasSize(1);
        assertThat(body.getData().get(0).getName()).isEqualTo("Credential leakage");
        assertThat(body.getPagination().getTotalCount()).isEqualTo(1L);
    }

    @Test
    void should_return_403_without_the_read_permission() {
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.ENVIRONMENT_GUARDIAN),
                eq(ENV_ID),
                eq(RolePermissionAction.READ)
            )
        ).thenReturn(false);

        MAPIAssertions.assertThat(rootTarget().request().get())
            .hasStatus(FORBIDDEN_403)
            .asError()
            .hasHttpStatus(FORBIDDEN_403)
            .hasMessage("You do not have sufficient rights to access this resource");
    }
}

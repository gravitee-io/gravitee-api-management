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
import static fixtures.SharedPolicyGroupFixtures.aSharedPolicyGroupCRD;
import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static jakarta.ws.rs.client.Entity.json;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import assertions.MAPIAssertions;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroupCRDStatus;
import io.gravitee.apim.core.shared_policy_group.use_case.ImportSharedPolicyGroupCRDCRDUseCase;
import io.gravitee.common.utils.UUID;
import io.gravitee.rest.api.management.v2.rest.model.ApiType;
import io.gravitee.rest.api.management.v2.rest.model.FlowPhase;
import io.gravitee.rest.api.management.v2.rest.model.SharedPolicyGroupCRD;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class SharedPolicyGroupsResource_CreateCRDTest extends AbstractResourceTest {

    private static final String ENV_ID = "my-env";

    @Inject
    ImportSharedPolicyGroupCRDCRDUseCase importSharedPolicyGroupCRDCRDUseCase;

    @Override
    protected String contextPath() {
        return "/environments/" + ENV_ID + "/shared-policy-groups/_import/crd";
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
        reset(importSharedPolicyGroupCRDCRDUseCase);
    }

    @Test
    void should_create_shared_policy_group_crd() {
        SharedPolicyGroupCRD crd = new SharedPolicyGroupCRD();
        crd.setCrossId(UUID.random().toString());
        crd.setName("test");
        crd.setApiType(ApiType.PROXY);
        crd.setPhase(FlowPhase.REQUEST);

        when(importSharedPolicyGroupCRDCRDUseCase.execute(any())).thenReturn(
            new ImportSharedPolicyGroupCRDCRDUseCase.Output(
                new SharedPolicyGroupCRDStatus(crd.getCrossId(), "API_ID", "organizationId", "environmentId", null)
            )
        );

        final Response response = rootTarget().request().put(json(crd));
        assertThat(response.getStatus()).isEqualTo(OK_200);
        var createdSharedPolicyGroup = response.readEntity(SharedPolicyGroupCRDStatus.class);

        assertThat(createdSharedPolicyGroup)
            .isNotNull()
            .hasFieldOrPropertyWithValue("crossId", crd.getCrossId())
            .hasFieldOrPropertyWithValue("id", "API_ID")
            .hasFieldOrPropertyWithValue("organizationId", "organizationId")
            .hasFieldOrPropertyWithValue("environmentId", "environmentId");

        var captor = ArgumentCaptor.forClass(ImportSharedPolicyGroupCRDCRDUseCase.Input.class);
        verify(importSharedPolicyGroupCRDCRDUseCase).execute(captor.capture());
        SoftAssertions.assertSoftly(soft -> {
            var input = captor.getValue();
            soft.assertThat(input.crd().getName()).isEqualTo(crd.getName());
            soft.assertThat(input.auditInfo()).isInstanceOf(AuditInfo.class);
        });
    }

    @Test
    void should_return_400_if_execute_fails_with_invalid_data_exception() {
        var sharedPolicyGroupCRD = aSharedPolicyGroupCRD();

        when(importSharedPolicyGroupCRDCRDUseCase.execute(any())).thenThrow(new InvalidDataException("Name is required."));

        final Response response = rootTarget().request().put(json(sharedPolicyGroupCRD));
        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
    }

    @Test
    void should_return_400_if_missing_body() {
        final Response response = rootTarget().request().put(json(new SharedPolicyGroupCRD()));

        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
    }

    @Test
    void should_return_400_if_name_not_specified() {
        SharedPolicyGroupCRD crd = new SharedPolicyGroupCRD();
        crd.setName(null);
        crd.setApiType(ApiType.PROXY);
        crd.setPhase(FlowPhase.ENTRYPOINT_CONNECT);

        final Response response = rootTarget().request().put(json(crd));

        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
    }

    @Test
    public void should_return_403_if_incorrect_permissions() {
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.ENVIRONMENT_SHARED_POLICY_GROUP),
                eq(ENV_ID),
                eq(RolePermissionAction.CREATE)
            )
        ).thenReturn(false);

        final Response response = rootTarget().request().put(json(aSharedPolicyGroupCRD()));

        MAPIAssertions.assertThat(response)
            .hasStatus(FORBIDDEN_403)
            .asError()
            .hasHttpStatus(FORBIDDEN_403)
            .hasMessage("You do not have sufficient rights to access this resource");
    }
}

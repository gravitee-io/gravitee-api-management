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
package io.gravitee.rest.api.management.v2.rest.resource.application;

import static assertions.MAPIAssertions.assertThat;
import static io.gravitee.apim.core.member.model.SystemRole.PRIMARY_OWNER;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import assertions.MAPIAssertions;
import fixtures.ApplicationFixtures;
import io.gravitee.apim.core.api.domain_service.CreateApiDomainService;
import io.gravitee.apim.core.api.domain_service.OAIDomainService;
import io.gravitee.apim.core.application.model.crd.ApplicationCRDStatus;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.management.v2.rest.model.ApplicationCRDSpec;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.ApplicationMetadataEntity;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ApplicationsResource_UpdateApplicationFromCRD extends AbstractResourceTest {

    private static final String ENVIRONMENT_ID = "my-env";

    @Autowired
    private OAIDomainService oaiDomainService;

    @Autowired
    private CreateApiDomainService createApiDomainService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT_ID + "/applications/_import/crd";
    }

    @BeforeEach
    public void init() throws TechnicalException {
        applicationCRDDomainService.reset();
        applicationMetadataCrudService.reset();
        applicationCrudService.reset();
        GraviteeContext.cleanContext();
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT_ID);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);

        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setId(ENVIRONMENT_ID);
        environmentEntity.setOrganizationId(ORGANIZATION);
        when(environmentService.findById(ENVIRONMENT_ID)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT_ID)).thenReturn(environmentEntity);

        userCrudService.initWith(List.of(BaseUserEntity.builder().id(USER_NAME).build()));
        roleQueryService.initWith(
            List.of(
                Role.builder()
                    .name(PRIMARY_OWNER.name())
                    .referenceType(Role.ReferenceType.ORGANIZATION)
                    .referenceId(ORGANIZATION)
                    .id("primary_owner_id")
                    .scope(Role.Scope.APPLICATION)
                    .build()
            )
        );
    }

    @Nested
    class UpdateApplicationFromCRD {

        @Test
        void should_return_403_if_incorrect_permissions() {
            when(
                permissionService.hasPermission(
                    GraviteeContext.getExecutionContext(),
                    RolePermission.ENVIRONMENT_API,
                    ENVIRONMENT_ID,
                    RolePermissionAction.CREATE
                )
            ).thenReturn(false);

            final Response response = rootTarget().request().put(Entity.json(ApplicationFixtures.anApplicationCRDSpec()));

            MAPIAssertions.assertThat(response)
                .hasStatus(FORBIDDEN_403)
                .asError()
                .hasHttpStatus(FORBIDDEN_403)
                .hasMessage("You do not have sufficient rights to access this resource");
        }

        @Test
        void should_update_application_from_crd() {
            var baseApplicationEntity = new BaseApplicationEntity();
            baseApplicationEntity.setId(UuidString.generateRandom());
            baseApplicationEntity.setName("test");
            baseApplicationEntity.setDescription("old_description");
            applicationCrudService.initWith(List.of(baseApplicationEntity));
            applicationCRDDomainService.initWith(List.of(baseApplicationEntity));

            ApplicationCRDSpec entity = ApplicationFixtures.anApplicationCRDSpec();
            entity.setId(baseApplicationEntity.getId());
            final Response response = rootTarget().request().put(Entity.json(entity));

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(ApplicationCRDStatus.class)
                .satisfies(status ->
                    SoftAssertions.assertSoftly(soft -> {
                        soft.assertThat(status.getId()).isNotNull();
                        soft.assertThat(status.getId()).isEqualTo(baseApplicationEntity.getId());
                        soft.assertThat(status.getEnvironmentId()).isEqualTo(ENVIRONMENT_ID);
                        soft.assertThat(status.getOrganizationId()).isEqualTo(ORGANIZATION);

                        BaseApplicationEntity application = applicationCRDDomainService.storage().get(0);
                        soft.assertThat(application.getDescription()).isEqualTo(entity.getDescription());
                    })
                );

            // Make sure that metadata is also created
            List<ApplicationMetadataEntity> allByApplication = applicationMetadataCrudService.storage();
            assertEquals(1, allByApplication.size());
        }

        @Test
        void should_update_application_metadata() {
            String applicationId = UuidString.generateRandom();
            ApplicationCRDSpec entity = ApplicationFixtures.anApplicationCRDSpec();
            entity.setId(applicationId);
            var baseApplicationEntity = new BaseApplicationEntity();
            baseApplicationEntity.setId(applicationId);
            baseApplicationEntity.setName("test");
            baseApplicationEntity.setDescription("old_description");

            applicationCrudService.initWith(List.of(baseApplicationEntity));
            applicationCRDDomainService.initWith(List.of(baseApplicationEntity));

            ApplicationMetadataEntity metadata = new ApplicationMetadataEntity();
            metadata.setApplicationId(applicationId);
            metadata.setName(entity.getMetadata().get(0).getName());
            metadata.setValue(entity.getMetadata().get(0).getValue());
            applicationMetadataQueryService.initWith(List.of(metadata));
            applicationMetadataCrudService.initWith(List.of(metadata));

            entity.getMetadata().get(0).setValue("updated_value");

            final Response response = rootTarget().request().put(Entity.json(entity));

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(ApplicationCRDStatus.class)
                .satisfies(status ->
                    SoftAssertions.assertSoftly(soft -> {
                        soft.assertThat(status.getId()).isNotNull();
                        soft.assertThat(status.getId()).isEqualTo(baseApplicationEntity.getId());
                        soft.assertThat(status.getEnvironmentId()).isEqualTo(ENVIRONMENT_ID);
                        soft.assertThat(status.getOrganizationId()).isEqualTo(ORGANIZATION);

                        BaseApplicationEntity application = applicationCRDDomainService.storage().get(0);
                        soft.assertThat(application.getDescription()).isEqualTo(entity.getDescription());

                        ApplicationMetadataEntity metadataEntity = applicationMetadataCrudService.storage().get(0);
                        soft.assertThat(metadataEntity.getValue()).isEqualTo(entity.getMetadata().get(0).getValue());
                    })
                );

            // Make sure that metadata is also created
            List<ApplicationMetadataEntity> allByApplication = applicationMetadataCrudService.storage();
            assertEquals(1, allByApplication.size());
        }
    }
}

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

import static io.gravitee.apim.core.member.model.SystemRole.PRIMARY_OWNER;
import static org.mockito.Mockito.doReturn;

import inmemory.ApplicationCrudServiceInMemory;
import io.gravitee.apim.core.api.model.crd.ApiCRDStatus;
import io.gravitee.apim.core.application.model.crd.ApplicationCRDStatus;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class ApplicationsResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "fake-env";

    @Autowired
    private ApplicationCrudServiceInMemory applicationCrudService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/applications";
    }

    @BeforeEach
    public void setup() {
        GraviteeContext.cleanContext();
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);

        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId(ENVIRONMENT);
        environment.setOrganizationId(ORGANIZATION);

        doReturn(environment).when(environmentService).findById(ENVIRONMENT);
        doReturn(environment).when(environmentService).findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT);
    }

    @Nested
    class ImportCRD {

        WebTarget target;

        @BeforeEach
        void setUp() {
            target = rootTarget().path("/_import/crd");
            applicationCrudService.reset();
            roleQueryService.initWith(
                List.of(
                    Role
                        .builder()
                        .name(PRIMARY_OWNER.name())
                        .referenceType(Role.ReferenceType.ORGANIZATION)
                        .referenceId(ORGANIZATION)
                        .id("primary_owner_id")
                        .scope(Role.Scope.APPLICATION)
                        .build()
                )
            );
        }

        @Test
        void should_return_group_warning_in_status_without_saving_if_dry_run() {
            var crdStatus = doImport("/crd/application/with-unknown-group.json", true);

            SoftAssertions.assertSoftly(soft -> {
                soft
                    .assertThat(crdStatus)
                    .isEqualTo(
                        ApplicationCRDStatus
                            .builder()
                            .organizationId(ORGANIZATION)
                            .environmentId(ENVIRONMENT)
                            .errors(
                                ApplicationCRDStatus.Errors
                                    .builder()
                                    .warning(List.of("Group [unknown-group] could not be found in environment [fake-env]"))
                                    .severe(List.of())
                                    .build()
                            )
                            .build()
                    );

                soft.assertThat(applicationCrudService.storage()).isEmpty();
            });
        }

        private ApplicationCRDStatus doImport(String crdResource, boolean dryRun) {
            try (var response = target.queryParam("dryRun", dryRun).request().put(Entity.json(readJSON(crdResource)))) {
                Assertions.assertThat(response.getStatus()).isEqualTo(200);
                return response.readEntity(ApplicationCRDStatus.class);
            }
        }

        private String readJSON(String resource) {
            try (var reader = this.getClass().getResourceAsStream(resource)) {
                return IOUtils.toString(reader, Charset.defaultCharset());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

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
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import inmemory.ApplicationCrudServiceInMemory;
import io.gravitee.apim.core.application.model.crd.ApplicationCRDStatus;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.SneakyThrows;
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
            reset(applicationRepository);
            reset(parameterService);
            when(
                parameterService.findAsBoolean(
                    new ExecutionContext(ORGANIZATION, ENVIRONMENT),
                    Key.APPLICATION_REGISTRATION_ENABLED,
                    ENVIRONMENT,
                    ParameterReferenceType.ENVIRONMENT
                )
            )
                .thenReturn(true);
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
            var crdStatus = doImport("/crd/application/simple-app-with-unknown-group.json", true);

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

        @Test
        @SneakyThrows
        void should_validate_backend_to_backend_application_without_saving_if_dry_run() {
            var crdStatus = doImport("/crd/application/backend-app.json", true);

            SoftAssertions.assertSoftly(soft -> {
                soft
                    .assertThat(crdStatus)
                    .isEqualTo(ApplicationCRDStatus.builder().organizationId(ORGANIZATION).environmentId(ENVIRONMENT).build());

                soft.assertThat(applicationCrudService.storage()).isEmpty();
            });
        }

        @Test
        @SneakyThrows
        void should_return_client_id_error_in_status_without_saving_if_dry_run() {
            when(applicationRepository.findAllByEnvironment(ENVIRONMENT, ApplicationStatus.ACTIVE))
                .thenReturn(
                    Set.of(
                        Application
                            .builder()
                            .name("conflicting-app")
                            .id("conflicting-app-id")
                            .metadata(Map.of("client_id", "test-client-id"))
                            .build()
                    )
                );

            var crdStatus = doImport("/crd/application/simple-app-with-client-id.json", true);

            SoftAssertions.assertSoftly(soft -> {
                soft
                    .assertThat(crdStatus)
                    .isEqualTo(
                        ApplicationCRDStatus
                            .builder()
                            .id("app-id")
                            .organizationId(ORGANIZATION)
                            .environmentId(ENVIRONMENT)
                            .errors(
                                ApplicationCRDStatus.Errors
                                    .builder()
                                    .warning(List.of())
                                    .severe(
                                        List.of(
                                            "client id [test-client-id] is already defined for application [conflicting-app] with id [conflicting-app-id] on environment [fake-env]"
                                        )
                                    )
                                    .build()
                            )
                            .build()
                    );

                soft.assertThat(applicationCrudService.storage()).isEmpty();
            });
        }

        @Test
        @SneakyThrows
        void should_return_no_client_id_error_in_status_on_updates_without_saving_if_dry_run() {
            when(applicationRepository.findAllByEnvironment(ENVIRONMENT, ApplicationStatus.ACTIVE))
                .thenReturn(Set.of(Application.builder().id("app-id").metadata(Map.of("client_id", "test-client-id")).build()));

            var crdStatus = doImport("/crd/application/simple-app-with-client-id.json", true);

            SoftAssertions.assertSoftly(soft -> {
                soft
                    .assertThat(crdStatus)
                    .isEqualTo(ApplicationCRDStatus.builder().id("app-id").organizationId(ORGANIZATION).environmentId(ENVIRONMENT).build());

                soft.assertThat(applicationCrudService.storage()).isEmpty();
            });
        }

        @Test
        @SneakyThrows
        void should_return_invalid_spa_grant_type_error_in_status_without_saving_if_dry_run() {
            var crdStatus = doImport("/crd/application/browser-app-with-invalid-grant-type.json", true);

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
                                    .warning(List.of())
                                    .severe(List.of("unknown grant types [client_credentials] for OAuth application of type [SPA]"))
                                    .build()
                            )
                            .build()
                    );

                soft.assertThat(applicationCrudService.storage()).isEmpty();
            });
        }

        @Test
        @SneakyThrows
        void should_return_disabled_dcr_error_in_status_without_saving_if_dry_run() {
            when(
                parameterService.findAsBoolean(
                    new ExecutionContext(ORGANIZATION, ENVIRONMENT),
                    Key.APPLICATION_REGISTRATION_ENABLED,
                    ENVIRONMENT,
                    ParameterReferenceType.ENVIRONMENT
                )
            )
                .thenReturn(false);

            var crdStatus = doImport("/crd/application/browser-app.json", true);

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
                                    .warning(List.of())
                                    .severe(
                                        List.of("configuring OAuth requires client registration to be enabled on environment [fake-env]")
                                    )
                                    .build()
                            )
                            .build()
                    );

                soft.assertThat(applicationCrudService.storage()).isEmpty();
            });
        }

        @Test
        @SneakyThrows
        void should_return_invalid_redirect_uri_error_in_status_without_saving_if_dry_run() {
            var crdStatus = doImport("/crd/application/browser-app-with-invalid-redirect-uri.json", true);

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
                                    .warning(List.of())
                                    .severe(List.of("invalid redirect URI [https://invalid my-redirect-url.com]"))
                                    .build()
                            )
                            .build()
                    );

                soft.assertThat(applicationCrudService.storage()).isEmpty();
            });
        }

        @Test
        @SneakyThrows
        void should_return_empty_redirect_uris_error_in_status_without_saving_if_dry_run() {
            var crdStatus = doImport("/crd/application/browser-app-with-empty-redirect-uri.json", true);

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
                                    .warning(List.of())
                                    .severe(List.of("application type [SPA] requires redirect URIs to be defined"))
                                    .build()
                            )
                            .build()
                    );

                soft.assertThat(applicationCrudService.storage()).isEmpty();
            });
        }

        @Test
        @SneakyThrows
        void should_return_missing_mandatory_grant_type_error_in_status_without_saving_if_dry_run() {
            var crdStatus = doImport("/crd/application/web-app-with-missing-mandatory-grant-type.json", true);

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
                                    .warning(List.of())
                                    .severe(
                                        List.of("OAuth application of type [Web] must have at least [authorization_code] as a grant type")
                                    )
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

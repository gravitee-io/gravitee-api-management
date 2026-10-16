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
package io.gravitee.apim.rest.api.automation.resource;

import static io.gravitee.rest.api.model.permissions.RolePermission.APPLICATION_DEFINITION;
import static io.gravitee.rest.api.model.permissions.RolePermission.ENVIRONMENT_APPLICATION;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.CREATE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.application.domain_service.ValidateApplicationCRDDomainService;
import io.gravitee.apim.core.application.model.crd.ApplicationCRDStatus;
import io.gravitee.apim.core.application.use_case.ImportApplicationCRDUseCase;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.apim.rest.api.automation.model.ApplicationState;
import io.gravitee.apim.rest.api.automation.resource.base.AbstractResourceTest;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class ApplicationsResourceTest extends AbstractResourceTest {

    private static final String ORGANIZATION_ADMIN_HEADER = "X-Test-Organization-Admin";

    @Inject
    private ImportApplicationCRDUseCase importApplicationCRDUseCase;

    @Inject
    private ValidateApplicationCRDDomainService validateApplicationCRDDomainService;

    @Override
    protected void decorate(ResourceConfig resourceConfig) {
        super.decorate(resourceConfig);
        // The default AuthenticationFilter authenticates a caller without authorities, so not an organization admin
        resourceConfig.register(
            (ContainerRequestFilter) requestContext -> {
                if (requestContext.getHeaderString(ORGANIZATION_ADMIN_HEADER) != null) {
                    requestContext.setSecurityContext(asOrganizationAdmin(requestContext.getSecurityContext()));
                    requestContext.getSecurityContext().getUserPrincipal();
                }
            },
            6
        );
    }

    @AfterEach
    void tearDown() {
        reset(importApplicationCRDUseCase);
        reset(validateApplicationCRDDomainService);
        reset(permissionService);
        applicationCrudService.reset();
    }

    @Nested
    class Run {

        @BeforeEach
        void setUp() {
            when(importApplicationCRDUseCase.execute(any(ImportApplicationCRDUseCase.Input.class))).thenReturn(
                new ImportApplicationCRDUseCase.Output(
                    ApplicationCRDStatus.builder().id("application-id").organizationId(ORGANIZATION).environmentId(ENVIRONMENT).build()
                )
            );
        }

        @Test
        void should_return_state_from_hrid() {
            var state = expectEntity("application-with-hrid.json");
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(state.getId()).isEqualTo("application-id");
                soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                soft.assertThat(state.getHrid()).isEqualTo("application-hrid");
            });
        }

        @Test
        void should_pass_additional_client_metadata_of_oauth_settings_to_the_use_case() {
            expectEntity("application-with-oauth-metadata.json");

            var input = ArgumentCaptor.forClass(ImportApplicationCRDUseCase.Input.class);
            verify(importApplicationCRDUseCase).execute(input.capture());

            var oauth = input.getValue().crd().getSettings().getOauth();
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(oauth.getAdditionalClientMetadata()).containsExactly(Map.entry("software_id", "template-app-id"));
                soft.assertThat(oauth.getApplicationType()).isEqualTo("web");
                soft.assertThat(oauth.getGrantTypes()).containsExactly("authorization_code");
            });
        }

        @Test
        void should_return_state_from_guid() {
            when(importApplicationCRDUseCase.execute(any(ImportApplicationCRDUseCase.Input.class))).thenAnswer(call -> {
                ImportApplicationCRDUseCase.Input input = call.getArgument(0, ImportApplicationCRDUseCase.Input.class);
                return new ImportApplicationCRDUseCase.Output(
                    ApplicationCRDStatus.builder().id(input.crd().getId()).organizationId(ORGANIZATION).environmentId(ENVIRONMENT).build()
                );
            });

            var state = expectEntity("application-with-uuid.json", false, true);
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(state.getHrid()).isEqualTo("application-id");
                soft.assertThat(state.getId()).isEqualTo("application-id");
                soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
            });
        }
    }

    @Nested
    class DryRun {

        boolean dryRun = true;

        @Test
        void should_return_state_from_hrid() {
            when(validateApplicationCRDDomainService.validateAndSanitize(any(ValidateApplicationCRDDomainService.Input.class))).thenAnswer(
                call -> Validator.Result.ofValue(call.getArgument(0))
            );

            var state = expectEntity("application-with-hrid.json", dryRun);
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(state.getId()).isNotNull();
                soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                soft.assertThat(state.getHrid()).isEqualTo("application-hrid");
            });
        }

        @Test
        void should_return_error_no_hrid() {
            try (
                var response = rootTarget()
                    .queryParam("dryRun", dryRun)
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("application-with-no-hrid.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(400);
            }
        }
    }

    @Nested
    class IgnoreGroups {

        @BeforeEach
        void setUp() {
            when(importApplicationCRDUseCase.execute(any(ImportApplicationCRDUseCase.Input.class))).thenReturn(
                new ImportApplicationCRDUseCase.Output(
                    ApplicationCRDStatus.builder().id("application-id").organizationId(ORGANIZATION).environmentId(ENVIRONMENT).build()
                )
            );
            when(validateApplicationCRDDomainService.validateAndSanitize(any(ValidateApplicationCRDDomainService.Input.class))).thenAnswer(
                call -> Validator.Result.ofValue(call.getArgument(0))
            );
        }

        @Test
        void should_pass_ignore_groups_to_the_use_case() {
            put("application-with-hrid.json", false, true);

            var input = ArgumentCaptor.forClass(ImportApplicationCRDUseCase.Input.class);
            verify(importApplicationCRDUseCase).execute(input.capture());
            assertThat(input.getValue().crd().isIgnoreGroups()).isTrue();
        }

        @Test
        void should_not_ignore_groups_by_default() {
            put("application-with-hrid.json", false, null);

            var input = ArgumentCaptor.forClass(ImportApplicationCRDUseCase.Input.class);
            verify(importApplicationCRDUseCase).execute(input.capture());
            assertThat(input.getValue().crd().isIgnoreGroups()).isFalse();
        }

        @Test
        void should_pass_ignore_groups_to_the_dry_run_validation() {
            put("application-with-hrid.json", true, true);

            var input = ArgumentCaptor.forClass(ValidateApplicationCRDDomainService.Input.class);
            verify(validateApplicationCRDDomainService).validateAndSanitize(input.capture());
            assertThat(input.getValue().spec().isIgnoreGroups()).isTrue();
        }

        private void put(String spec, boolean dryRun, Boolean ignoreGroups) {
            var target = rootTarget().queryParam("dryRun", dryRun);
            if (ignoreGroups != null) {
                target = target.queryParam("ignoreGroups", ignoreGroups);
            }
            try (var response = target.request().accept(MediaType.APPLICATION_JSON_TYPE).put(Entity.json(readJSON(spec)))) {
                assertThat(response.getStatus()).isEqualTo(200);
            }
        }
    }

    @Nested
    class Authorization {

        private static final String APPLICATION_ID = "application-id";

        @BeforeEach
        void setUp() {
            when(importApplicationCRDUseCase.execute(any(ImportApplicationCRDUseCase.Input.class))).thenReturn(
                new ImportApplicationCRDUseCase.Output(
                    ApplicationCRDStatus.builder().id(APPLICATION_ID).organizationId(ORGANIZATION).environmentId(ENVIRONMENT).build()
                )
            );
            when(validateApplicationCRDDomainService.validateAndSanitize(any(ValidateApplicationCRDDomainService.Input.class))).thenAnswer(
                call -> Validator.Result.ofValue(call.getArgument(0))
            );
        }

        @Test
        void should_return_403_when_the_caller_cannot_update_the_existing_application() {
            givenApplication(ApplicationStatus.ACTIVE);
            when(permissionService.hasPermission(any(), eq(APPLICATION_DEFINITION), eq(APPLICATION_ID), eq(UPDATE))).thenReturn(false);

            assertThat(put(false, false)).isEqualTo(403);
            verify(importApplicationCRDUseCase, never()).execute(any());
        }

        @Test
        void should_return_403_on_dry_run_when_the_caller_cannot_update_the_existing_application() {
            givenApplication(ApplicationStatus.ACTIVE);
            when(permissionService.hasPermission(any(), eq(APPLICATION_DEFINITION), eq(APPLICATION_ID), eq(UPDATE))).thenReturn(false);

            assertThat(put(true, false)).isEqualTo(403);
            verify(validateApplicationCRDDomainService, never()).validateAndSanitize(any());
        }

        @Test
        void should_update_the_existing_application_with_update_on_it_and_no_create_on_the_environment() {
            givenApplication(ApplicationStatus.ACTIVE);
            when(permissionService.hasPermission(any(), eq(ENVIRONMENT_APPLICATION), eq(ENVIRONMENT), eq(CREATE))).thenReturn(false);

            assertThat(put(false, false)).isEqualTo(200);
            verify(importApplicationCRDUseCase).execute(any());
        }

        @Test
        void should_return_403_when_the_caller_cannot_create_a_new_application() {
            when(permissionService.hasPermission(any(), eq(ENVIRONMENT_APPLICATION), eq(ENVIRONMENT), eq(CREATE))).thenReturn(false);

            assertThat(put(false, false)).isEqualTo(403);
            verify(importApplicationCRDUseCase, never()).execute(any());
        }

        @Test
        void should_create_a_new_application_with_create_on_the_environment() {
            when(permissionService.hasPermission(any(), eq(APPLICATION_DEFINITION), any(), eq(UPDATE))).thenReturn(false);

            assertThat(put(false, false)).isEqualTo(200);
            verify(importApplicationCRDUseCase).execute(any());
        }

        @Test
        void should_return_403_when_a_non_admin_imports_an_archived_application() {
            givenApplication(ApplicationStatus.ARCHIVED);

            assertThat(put(false, false)).isEqualTo(403);
            verify(importApplicationCRDUseCase, never()).execute(any());
        }

        @Test
        void should_let_an_admin_import_an_archived_application() {
            givenApplication(ApplicationStatus.ARCHIVED);

            assertThat(put(false, true)).isEqualTo(200);
            verify(importApplicationCRDUseCase).execute(any());
        }

        private void givenApplication(ApplicationStatus status) {
            var application = new BaseApplicationEntity();
            application.setId(APPLICATION_ID);
            application.setEnvironmentId(ENVIRONMENT);
            application.setStatus(status.name());
            applicationCrudService.initWith(List.of(application));
        }

        private int put(boolean dryRun, boolean organizationAdmin) {
            var request = rootTarget()
                .queryParam("dryRun", dryRun)
                .queryParam("hridContainsUUID", true)
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE);
            if (organizationAdmin) {
                request = request.header(ORGANIZATION_ADMIN_HEADER, true);
            }
            try (var response = request.put(Entity.json(readJSON("application-with-uuid.json")))) {
                return response.getStatus();
            }
        }
    }

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/applications";
    }

    private ApplicationState expectEntity(String spec) {
        return expectEntity(spec, false, false);
    }

    private ApplicationState expectEntity(String spec, boolean dryRun) {
        return expectEntity(spec, dryRun, false);
    }

    private ApplicationState expectEntity(String spec, boolean dryRun, boolean hridContainsUUID) {
        try (
            var response = rootTarget()
                .queryParam("dryRun", dryRun)
                .queryParam("hridContainsUUID", hridContainsUUID)
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(readJSON(spec)))
        ) {
            assertThat(response.getStatus()).isEqualTo(200);
            return response.readEntity(ApplicationState.class);
        }
    }

    private static SecurityContext asOrganizationAdmin(SecurityContext authenticated) {
        return new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                var user = (UsernamePasswordAuthenticationToken) authenticated.getUserPrincipal();
                var admin = new UsernamePasswordAuthenticationToken(
                    user.getPrincipal(),
                    user.getCredentials(),
                    List.of(new SimpleGrantedAuthority("ORGANIZATION:ADMIN"))
                );
                SecurityContextHolder.getContext().setAuthentication(admin);
                return admin;
            }

            @Override
            public boolean isUserInRole(String role) {
                return authenticated.isUserInRole(role);
            }

            @Override
            public boolean isSecure() {
                return authenticated.isSecure();
            }

            @Override
            public String getAuthenticationScheme() {
                return authenticated.getAuthenticationScheme();
            }
        };
    }
}

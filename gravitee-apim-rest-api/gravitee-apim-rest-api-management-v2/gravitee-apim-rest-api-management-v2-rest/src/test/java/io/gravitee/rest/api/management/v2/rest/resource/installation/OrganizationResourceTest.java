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
package io.gravitee.rest.api.management.v2.rest.resource.installation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.FlowMode;
import io.gravitee.definition.model.llm.ContextManagementPolicy;
import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.rest.api.management.v2.rest.model.ContextManagementPolicy.ModeEnum;
import io.gravitee.rest.api.management.v2.rest.model.GraviteeLicense;
import io.gravitee.rest.api.management.v2.rest.model.Organization;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.service.OrganizationContextPolicyService;
import io.gravitee.rest.api.service.OrganizationService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.OrganizationNotFoundException;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class OrganizationResourceTest extends AbstractResourceTest {

    @Inject
    private OrganizationService organizationService;

    @Inject
    private LicenseManager licenseManager;

    @Inject
    private OrganizationContextPolicyService organizationContextPolicyService;

    @Override
    protected String contextPath() {
        return "/organizations";
    }

    @BeforeEach
    public void init() {
        reset(organizationService, organizationContextPolicyService, licenseManager);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
        GraviteeContext.setCurrentEnvironment(null);
    }

    @Nested
    class GetOrganizationById {

        @Test
        public void shouldGetOrganizationById() {
            mockExistingOrganization(ORGANIZATION);

            final Response response = rootTarget(ORGANIZATION).request().get();
            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);

            var body = response.readEntity(Organization.class);
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(body).isNotNull();
                soft.assertThat(body.getId()).isEqualTo(ORGANIZATION);
                soft.assertThat(body.getName()).isEqualTo("org-name");
                soft.assertThat(body.getDescription()).isEqualTo("A nice description");
            });
        }
    }

    @Nested
    class GetOrganizationLicense {

        @Test
        public void shouldReturnOrganizationLicenseWithFeatures() {
            mockExistingOrganization(ORGANIZATION);

            var now = Instant.now();
            var nowDate = Date.from(now);

            final License license = mock(License.class);
            when(licenseManager.getOrganizationLicenseOrPlatform(ORGANIZATION)).thenReturn(license);
            when(license.getTier()).thenReturn("universe");
            when(license.getPacks()).thenReturn(Set.of("observability"));
            when(license.getFeatures()).thenReturn(Set.of("apim-reporter-datadog"));
            when(license.getExpirationDate()).thenReturn(nowDate);
            when(license.getReferenceType()).thenReturn("PLATFORM");
            when(license.isExpired()).thenReturn(true);

            var response = rootTarget(ORGANIZATION).path("license").request().get();
            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);

            var graviteeLicense = response.readEntity(GraviteeLicense.class);
            assertThat(graviteeLicense).isNotNull();
            assertThat(graviteeLicense.getTier()).isEqualTo("universe");
            assertThat(graviteeLicense.getPacks()).containsExactly("observability");
            assertThat(graviteeLicense.getFeatures()).containsExactly("apim-reporter-datadog");
            assertThat(graviteeLicense.getExpiresAt().toInstant().toEpochMilli()).isEqualTo(now.toEpochMilli());
            assertThat(graviteeLicense.getScope()).isEqualTo("PLATFORM");
            assertThat(graviteeLicense.getIsExpired()).isEqualTo(true);
        }

        @Test
        public void shouldReturn404IfOrganizationDoesNotExist() {
            doThrow(new OrganizationNotFoundException(ORGANIZATION)).when(organizationService).findById(ORGANIZATION);

            var response = rootTarget(ORGANIZATION).path("license").request().get();
            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        }
    }

    @Nested
    class ContextPolicy {

        @Test
        void shouldReturnConfiguredPolicy() {
            mockExistingOrganization(ORGANIZATION);
            when(organizationContextPolicyService.find(ORGANIZATION)).thenReturn(
                new ContextManagementPolicy(ContextManagementPolicy.Mode.ENFORCE, 2048, null)
            );

            Response response = rootTarget(ORGANIZATION).path("context-policy").request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            var policy = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.ContextManagementPolicy.class);
            assertThat(policy.getMode()).isEqualTo(ModeEnum.ENFORCE);
            assertThat(policy.getCompactThreshold()).isEqualTo(2048);
        }

        @Test
        void shouldReturnNoContentWhenPolicyIsAbsent() {
            mockExistingOrganization(ORGANIZATION);
            when(organizationContextPolicyService.find(ORGANIZATION)).thenReturn(null);

            Response response = rootTarget(ORGANIZATION).path("context-policy").request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NO_CONTENT_204);
        }

        @Test
        void shouldSaveAndPublishPolicy() {
            mockExistingOrganization(ORGANIZATION);
            var policy = new io.gravitee.rest.api.management.v2.rest.model.ContextManagementPolicy()
                .mode(ModeEnum.DEFAULT)
                .compactThreshold(1024);
            when(organizationContextPolicyService.save(any(), eq(ORGANIZATION), any())).thenReturn(
                new ContextManagementPolicy(ContextManagementPolicy.Mode.DEFAULT, 1024, null)
            );

            Response response = rootTarget(ORGANIZATION)
                .path("context-policy")
                .request()
                .put(Entity.entity(policy, MediaType.APPLICATION_JSON_TYPE));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            verify(organizationContextPolicyService).save(
                eq(new io.gravitee.rest.api.service.common.ExecutionContext(ORGANIZATION)),
                eq(ORGANIZATION),
                eq(new ContextManagementPolicy(ContextManagementPolicy.Mode.DEFAULT, 1024, null))
            );
            verify(organizationService).publishOrganization(ORGANIZATION);
        }

        @Test
        void shouldRejectInvalidPolicy() {
            mockExistingOrganization(ORGANIZATION);
            var policy = new io.gravitee.rest.api.management.v2.rest.model.ContextManagementPolicy()
                .mode(ModeEnum.PASSTHROUGH)
                .compactThreshold(1024);

            Response response = rootTarget(ORGANIZATION)
                .path("context-policy")
                .request()
                .put(Entity.entity(policy, MediaType.APPLICATION_JSON_TYPE));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
            verifyNoInteractions(organizationContextPolicyService);
            verify(organizationService, never()).publishOrganization(anyString());
        }

        @Test
        void shouldClearAndPublishPolicy() {
            mockExistingOrganization(ORGANIZATION);

            Response response = rootTarget(ORGANIZATION).path("context-policy").request().delete();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NO_CONTENT_204);
            verify(organizationContextPolicyService).clear(
                eq(new io.gravitee.rest.api.service.common.ExecutionContext(ORGANIZATION)),
                eq(ORGANIZATION)
            );
            verify(organizationService).publishOrganization(ORGANIZATION);
        }
    }

    private void mockExistingOrganization(String orgId) {
        OrganizationEntity organizationEntity = new OrganizationEntity();
        organizationEntity.setId(orgId);
        organizationEntity.setName("org-name");
        organizationEntity.setDescription("A nice description");
        organizationEntity.setCockpitId("cockpit-id");
        organizationEntity.setFlowMode(FlowMode.BEST_MATCH);
        organizationEntity.setHrids(List.of("one-hrid"));

        when(organizationService.findById(eq(orgId))).thenReturn(organizationEntity);
    }
}

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
package io.gravitee.rest.api.management.rest.resource;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import io.gravitee.common.data.domain.MetadataPage;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.rest.api.model.audit.AuditEntity;
import jakarta.ws.rs.core.Response;
import java.util.*;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Test;

/**
 * @author GraviteeSource Team
 */
public class AuditResourceTest extends AbstractResourceTest {

    @Inject
    private LicenseManager licenseManager;

    private License license;

    @Override
    protected String contextPath() {
        return "audit/";
    }

    @Before
    public void init() {
        license = mock(License.class);
        when(licenseManager.getPlatformLicense()).thenReturn(license);
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);
        reset(auditService);
    }

    private static class TestAudit extends AuditEntity {

        protected TestAudit(String id) {
            super();
            this.setId(id);
        }
    }

    @Test
    public void should_not_list_env_audit_without_license() {
        when(license.isFeatureEnabled("apim-audit-trail")).thenReturn(false);
        final Response response = envTarget().request().get();
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void should_list_env_audit() {
        when(license.isFeatureEnabled("apim-audit-trail")).thenReturn(true);
        List<AuditEntity> audits = List.of(new TestAudit("audit-1"));
        Map<String, String> metadata = new HashMap<>();
        when(auditService.search(any(), argThat(Objects::nonNull))).thenReturn(new MetadataPage<>(audits, 1, 1, 1, metadata));

        final Response response = envTarget().request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
    }

    @Test
    public void should_list_env_audit_with_event() {
        when(license.isFeatureEnabled("apim-audit-trail")).thenReturn(true);
        List<AuditEntity> audits = List.of(new TestAudit("audit-1"));
        Map<String, String> metadata = new HashMap<>();
        when(auditService.search(any(), argThat(o -> o != null && o.getEvents().equals(Collections.singletonList("eventId"))))).thenReturn(
            new MetadataPage<>(audits, 1, 1, 1, metadata)
        );

        final Response response = envTarget().queryParam("event", "eventId").request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
    }

    @Test
    public void should_not_list_org_audit_without_license() {
        when(license.isFeatureEnabled("apim-audit-trail")).thenReturn(false);
        final Response response = orgTarget().request().get();
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void should_list_org_audit() {
        when(license.isFeatureEnabled("apim-audit-trail")).thenReturn(true);
        List<AuditEntity> audits = List.of(new TestAudit("audit-1"));
        Map<String, String> metadata = new HashMap<>();

        when(auditService.search(any(), argThat(Objects::nonNull))).thenReturn(new MetadataPage<>(audits, 1, 1, 1, metadata));

        final Response response = orgTarget().request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
    }
}

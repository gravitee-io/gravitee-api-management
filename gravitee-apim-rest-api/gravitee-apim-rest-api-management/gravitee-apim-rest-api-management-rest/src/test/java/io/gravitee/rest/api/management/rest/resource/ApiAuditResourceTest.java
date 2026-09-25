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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.data.domain.MetadataPage;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.rest.api.model.audit.AuditEntity;
import io.gravitee.rest.api.model.audit.AuditQuery;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import javax.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author GraviteeSource Team
 */
public class ApiAuditResourceTest extends AbstractResourceTest {

    private static final String API = "my-api";

    @Inject
    private LicenseManager licenseManager;

    private License license;

    @Override
    protected String contextPath() {
        return "apis";
    }

    @BeforeEach
    public void init() {
        license = mock(License.class);
        when(licenseManager.getPlatformLicense()).thenReturn(license);
        when(license.isFeatureEnabled("apim-audit-trail")).thenReturn(true);
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);
        reset(auditService);
        when(auditService.search(any(), any())).thenReturn(new MetadataPage<>(List.of(), 1, 0, 0, new HashMap<>()));
    }

    private WebTarget apiAuditsTarget() {
        return envTarget().path(API).path("audit");
    }

    @Test
    public void should_search_the_api_audits() {
        final Response response = apiAuditsTarget().request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        assertEquals(List.of(API), capturedQuery().getApiIds());
    }

    @Test
    public void should_ignore_the_encrypted_filter_which_only_the_environment_audits_support() {
        final Response response = apiAuditsTarget().queryParam("encrypted", true).request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        assertNull(capturedQuery().getProperties());
    }

    private AuditQuery capturedQuery() {
        ArgumentCaptor<AuditQuery> query = ArgumentCaptor.forClass(AuditQuery.class);
        verify(auditService).search(any(), query.capture());
        return query.getValue();
    }
}

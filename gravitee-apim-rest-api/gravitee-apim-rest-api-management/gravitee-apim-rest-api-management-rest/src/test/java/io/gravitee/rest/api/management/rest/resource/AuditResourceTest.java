/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
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
import io.gravitee.rest.api.model.audit.AuditEntity;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;

/**
 * @author GraviteeSource Team
 */
public class AuditResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "audit/";
    }

    @Before
    public void setUp() {
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);
        reset(auditService);
    }

    private class TestAudit extends AuditEntity {

        protected TestAudit(String id) {
            super();
            this.setId(id);
        }
    }

    @Test
    public void should_list_env_audit() {
        List<AuditEntity> audits = List.of(new TestAudit("audit-1"));
        Map<String, String> metadata = new HashMap<>();
        when(auditService.search(any(), argThat(o -> o != null))).thenReturn(new MetadataPage<>(audits, 1, 1, 1, metadata));

        final Response response = envTarget().request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
    }

    @Test
    public void should_list_env_audit_with_event() {
        List<AuditEntity> audits = List.of(new TestAudit("audit-1"));
        Map<String, String> metadata = new HashMap<>();
        when(auditService.search(any(), argThat(o -> o != null && o.getEvents().equals(Collections.singletonList("eventId")))))
            .thenReturn(new MetadataPage<>(audits, 1, 1, 1, metadata));

        final Response response = envTarget().queryParam("event", "eventId").request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
    }

    @Test
    public void should_list_org_audit() {
        List<AuditEntity> audits = List.of(new TestAudit("audit-1"));
        Map<String, String> metadata = new HashMap<>();

        when(auditService.search(any(), argThat(o -> o != null))).thenReturn(new MetadataPage<>(audits, 1, 1, 1, metadata));

        final Response response = orgTarget().request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
    }
}

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
package io.gravitee.apim.infra.domain_service.analytics_engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.analytics_engine.model.AnalyticsQueryContext;
import io.gravitee.apim.core.analytics_engine.model.AnalyticsScope;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DelegatingAnalyticsQueryContextLoaderTest {

    private final ManagementContextLoader managementContextLoader = mock(ManagementContextLoader.class);
    private final PortalContextLoader portalContextLoader = mock(PortalContextLoader.class);
    private final AuditInfo auditInfo = AuditInfo.builder().organizationId("org").environmentId("env").build();

    private AnalyticsQueryContext aContext(String apiId) {
        return new AnalyticsQueryContext(auditInfo, new ExecutionContext("org", "env"), Set.of(apiId), Map.of(), Map.of(), Map.of());
    }

    @Test
    void should_route_management_scope_to_management_loader() {
        var expected = aContext("mgmt-api");
        when(managementContextLoader.load(auditInfo)).thenReturn(expected);

        var router = new DelegatingAnalyticsQueryContextLoader(managementContextLoader, portalContextLoader);
        var result = router.load(auditInfo, AnalyticsScope.MANAGEMENT);

        assertThat(result).isSameAs(expected);
        verify(managementContextLoader).load(auditInfo);
        verifyNoInteractions(portalContextLoader);
    }

    @Test
    void should_route_portal_scope_to_portal_loader() {
        var expected = aContext("portal-api");
        when(portalContextLoader.load(auditInfo)).thenReturn(expected);

        var router = new DelegatingAnalyticsQueryContextLoader(managementContextLoader, portalContextLoader);
        var result = router.load(auditInfo, AnalyticsScope.PORTAL);

        assertThat(result).isSameAs(expected);
        verify(portalContextLoader).load(auditInfo);
        verifyNoInteractions(managementContextLoader);
    }

    @Test
    void should_fail_when_requested_scope_loader_is_absent() {
        var router = new DelegatingAnalyticsQueryContextLoader(managementContextLoader, null);

        assertThatThrownBy(() -> router.load(auditInfo, AnalyticsScope.PORTAL))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("PORTAL");
    }
}

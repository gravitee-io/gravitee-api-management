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

import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryContextLoader;
import io.gravitee.apim.core.analytics_engine.model.AnalyticsQueryContext;
import io.gravitee.apim.core.analytics_engine.model.AnalyticsScope;
import io.gravitee.apim.core.audit.model.AuditInfo;

/**
 * Routes analytics context loading to the loader matching the calling surface. The analytics use
 * cases are shared singletons served to both the Management and the Portal REST surfaces, so a
 * single {@link AnalyticsQueryContextLoader} bean must dispatch to the right implementation based on
 * the {@link AnalyticsScope} carried by the call — otherwise a bean-name collision would silently
 * wire one surface's loader to both (see PORTAL-77).
 *
 * <p>Each concrete loader is optional so the router also works in single-surface deployments; the
 * absent surface simply must never be requested.
 *
 * @author GraviteeSource Team
 */
public class DelegatingAnalyticsQueryContextLoader implements AnalyticsQueryContextLoader {

    private final ManagementContextLoader managementContextLoader;
    private final PortalContextLoader portalContextLoader;

    public DelegatingAnalyticsQueryContextLoader(ManagementContextLoader managementContextLoader, PortalContextLoader portalContextLoader) {
        this.managementContextLoader = managementContextLoader;
        this.portalContextLoader = portalContextLoader;
    }

    @Override
    public AnalyticsQueryContext load(AuditInfo auditInfo, AnalyticsScope scope) {
        return switch (scope) {
            case MANAGEMENT -> require(managementContextLoader, scope).load(auditInfo);
            case PORTAL -> require(portalContextLoader, scope).load(auditInfo);
        };
    }

    private static <T> T require(T loader, AnalyticsScope scope) {
        if (loader == null) {
            throw new IllegalStateException("No analytics query context loader available for scope " + scope);
        }
        return loader;
    }
}

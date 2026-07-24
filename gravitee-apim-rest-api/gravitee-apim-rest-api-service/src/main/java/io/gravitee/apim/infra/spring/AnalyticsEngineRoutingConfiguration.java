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
package io.gravitee.apim.infra.spring;

import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryContextLoader;
import io.gravitee.apim.infra.domain_service.analytics_engine.DelegatingAnalyticsQueryContextLoader;
import io.gravitee.apim.infra.domain_service.analytics_engine.ManagementContextLoader;
import io.gravitee.apim.infra.domain_service.analytics_engine.PortalContextLoader;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the single {@link AnalyticsQueryContextLoader} bean shared by the analytics use cases,
 * routing to the Management or Portal loader based on the calling surface.
 *
 * <p>Imported by both the Management and the Portal REST configurations; Spring de-duplicates the
 * import so exactly one router bean exists in the merged context, which avoids the previous
 * bean-name collision where the two surface loaders overrode each other (PORTAL-77). The concrete
 * loaders are resolved lazily via {@link ObjectProvider} so the router also works in single-surface
 * deployments.
 *
 * @author GraviteeSource Team
 */
@Configuration
public class AnalyticsEngineRoutingConfiguration {

    @Bean
    public AnalyticsQueryContextLoader analyticsQueryContextLoader(
        ObjectProvider<ManagementContextLoader> managementContextLoader,
        ObjectProvider<PortalContextLoader> portalContextLoader
    ) {
        return new DelegatingAnalyticsQueryContextLoader(managementContextLoader.getIfAvailable(), portalContextLoader.getIfAvailable());
    }
}

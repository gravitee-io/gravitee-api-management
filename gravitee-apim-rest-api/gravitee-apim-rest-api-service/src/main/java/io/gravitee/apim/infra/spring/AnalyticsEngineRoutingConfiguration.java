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
import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryContextLoaderResolver;
import io.gravitee.apim.core.analytics_engine.domain_service.QueryFilterTransformer;
import io.gravitee.apim.core.analytics_engine.model.AnalyticsScope;
import io.gravitee.apim.infra.domain_service.analytics_engine.ManagementContextLoader;
import io.gravitee.apim.infra.domain_service.analytics_engine.PortalContextLoader;
import io.gravitee.apim.infra.domain_service.analytics_engine.processors.ApplicationScopeFilterTransformer;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the {@link AnalyticsQueryContextLoaderResolver} mapping each surface to its loader, and the filter applying
 * the application scope a loader sets. Imported by both the Management and Portal REST configurations; the shared
 * import keeps a single bean of each, and {@link ObjectProvider} lets it work when only one surface is present.
 */
@Configuration
public class AnalyticsEngineRoutingConfiguration {

    @Bean
    public AnalyticsQueryContextLoaderResolver analyticsQueryContextLoaderResolver(
        ObjectProvider<ManagementContextLoader> managementContextLoader,
        ObjectProvider<PortalContextLoader> portalContextLoader
    ) {
        Map<AnalyticsScope, AnalyticsQueryContextLoader> loadersByScope = new EnumMap<>(AnalyticsScope.class);
        managementContextLoader.ifAvailable(loader -> loadersByScope.put(AnalyticsScope.MANAGEMENT, loader));
        portalContextLoader.ifAvailable(loader -> loadersByScope.put(AnalyticsScope.PORTAL, loader));
        return new AnalyticsQueryContextLoaderResolver(loadersByScope);
    }

    @Bean
    public QueryFilterTransformer applicationScopeFilterTransformer() {
        return new ApplicationScopeFilterTransformer();
    }
}

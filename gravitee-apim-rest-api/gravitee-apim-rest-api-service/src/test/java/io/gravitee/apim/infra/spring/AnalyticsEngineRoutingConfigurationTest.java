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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryContextLoaderResolver;
import io.gravitee.apim.core.analytics_engine.model.AnalyticsQueryContext;
import io.gravitee.apim.core.analytics_engine.model.AnalyticsScope;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.infra.domain_service.analytics_engine.ManagementContextLoader;
import io.gravitee.apim.infra.domain_service.analytics_engine.PortalContextLoader;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Verifies that loading both surfaces together yields a single resolver bean routing each scope to
 * its loader, instead of two colliding loader beans.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AnalyticsEngineRoutingConfigurationTest {

    private static AnalyticsQueryContext context(String apiId) {
        return new AnalyticsQueryContext(
            mock(AuditInfo.class),
            new ExecutionContext("org", "env"),
            Set.of(apiId),
            Map.of(),
            Map.of(),
            Map.of()
        );
    }

    // Mirror the two REST configs: each contributes its loader and imports the routing config.
    @Configuration
    @Import(AnalyticsEngineRoutingConfiguration.class)
    static class ManagementSurfaceConfig {

        @Bean
        ManagementContextLoader managementContextLoader() {
            var loader = mock(ManagementContextLoader.class);
            when(loader.load(org.mockito.ArgumentMatchers.any())).thenReturn(context("mgmt-api"));
            return loader;
        }
    }

    @Configuration
    @Import(AnalyticsEngineRoutingConfiguration.class)
    static class PortalSurfaceConfig {

        @Bean
        PortalContextLoader portalContextLoader() {
            var loader = mock(PortalContextLoader.class);
            when(loader.load(org.mockito.ArgumentMatchers.any())).thenReturn(context("portal-api"));
            return loader;
        }
    }

    @Test
    void should_expose_a_single_resolver_bean_when_both_surfaces_are_loaded_together() {
        try (var ctx = new AnnotationConfigApplicationContext(ManagementSurfaceConfig.class, PortalSurfaceConfig.class)) {
            assertThat(ctx.getBeanNamesForType(AnalyticsQueryContextLoaderResolver.class)).hasSize(1);
        }
    }

    @Test
    void should_route_each_scope_to_the_matching_surface_loader() {
        try (var ctx = new AnnotationConfigApplicationContext(ManagementSurfaceConfig.class, PortalSurfaceConfig.class)) {
            var resolver = ctx.getBean(AnalyticsQueryContextLoaderResolver.class);
            var auditInfo = mock(AuditInfo.class);

            assertThat(resolver.load(auditInfo, AnalyticsScope.MANAGEMENT).authorizedApiIds()).containsExactly("mgmt-api");
            assertThat(resolver.load(auditInfo, AnalyticsScope.PORTAL).authorizedApiIds()).containsExactly("portal-api");
        }
    }
}

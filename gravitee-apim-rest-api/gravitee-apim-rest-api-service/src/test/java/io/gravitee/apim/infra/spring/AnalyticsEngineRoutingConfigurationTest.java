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

import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryContextLoader;
import io.gravitee.apim.core.analytics_engine.model.AnalyticsQueryContext;
import io.gravitee.apim.core.analytics_engine.model.AnalyticsScope;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.infra.domain_service.analytics_engine.DelegatingAnalyticsQueryContextLoader;
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
 * Reproduces the PORTAL-77 wiring: when both the Management and the Portal surfaces are loaded in the
 * same context, there must be a single {@link AnalyticsQueryContextLoader} bean (the router) instead
 * of two colliding beans silently overriding each other. This is the scenario module-isolated
 * resource tests could not catch.
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

    // Two independent configs each contributing their concrete loader AND importing the routing config,
    // mirroring RestManagementConfiguration and RestPortalConfiguration both @Import-ing it.
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
    void should_expose_a_single_router_bean_when_both_surfaces_are_loaded_together() {
        try (var ctx = new AnnotationConfigApplicationContext(ManagementSurfaceConfig.class, PortalSurfaceConfig.class)) {
            var loaderBeans = ctx.getBeanNamesForType(AnalyticsQueryContextLoader.class);
            assertThat(loaderBeans).hasSize(1);

            var loader = ctx.getBean(AnalyticsQueryContextLoader.class);
            assertThat(loader).isInstanceOf(DelegatingAnalyticsQueryContextLoader.class);
        }
    }

    @Test
    void should_route_each_scope_to_the_matching_surface_loader() {
        try (var ctx = new AnnotationConfigApplicationContext(ManagementSurfaceConfig.class, PortalSurfaceConfig.class)) {
            var loader = ctx.getBean(AnalyticsQueryContextLoader.class);
            var auditInfo = mock(AuditInfo.class);

            assertThat(loader.load(auditInfo, AnalyticsScope.MANAGEMENT).authorizedApiIds()).containsExactly("mgmt-api");
            assertThat(loader.load(auditInfo, AnalyticsScope.PORTAL).authorizedApiIds()).containsExactly("portal-api");
        }
    }
}

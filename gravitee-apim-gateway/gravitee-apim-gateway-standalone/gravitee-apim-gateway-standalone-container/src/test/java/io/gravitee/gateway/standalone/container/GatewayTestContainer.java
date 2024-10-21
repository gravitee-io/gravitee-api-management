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
package io.gravitee.gateway.standalone.container;

import static org.mockito.Mockito.mock;

import io.gravitee.gateway.reactive.api.tracing.Tracer;
import io.gravitee.gateway.standalone.GatewayContainer;
import io.gravitee.gateway.standalone.license.PermissiveLicenseManager;
import io.gravitee.gateway.standalone.reporter.FakeReporter;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.cache.CacheManager;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.node.container.NodeFactory;
import io.gravitee.node.monitoring.spring.NodeMonitoringConfiguration;
import io.gravitee.node.opentelemetry.tracer.noop.NoOpTracer;
import io.gravitee.node.plugin.cache.standalone.StandaloneCacheManager;
import io.gravitee.node.plugin.cluster.standalone.StandaloneClusterManager;
import io.gravitee.reporter.api.Reporter;
import io.gravitee.repository.management.api.AccessPointRepository;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.InstallationRepository;
import io.gravitee.repository.management.api.LicenseRepository;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.vertx.core.Vertx;
import java.util.List;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * This class allows to extends the {@link GatewayContainer} in order to be able to override the {@link NodeFactory}
 * and provide a {@link GatewayTestNode}
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GatewayTestContainer extends GatewayContainer {

    @Override
    protected List<Class<?>> annotatedClasses() {
        List<Class<?>> classes = super.annotatedClasses();
        classes.add(GatewayTestConfiguration.class);
        classes.remove(NodeMonitoringConfiguration.class);
        return classes;
    }

    @Configuration
    public static class GatewayTestConfiguration {

        @Bean
        public Node node() {
            return new GatewayTestNode();
        }

        @Bean
        public ClusterManager clusterManager(final Vertx vertx) {
            return new StandaloneClusterManager(vertx);
        }

        @Bean
        public CacheManager cacheManager() {
            return new StandaloneCacheManager();
        }

        @Bean
        public Tracer tracer() {
            return new Tracer(Vertx.vertx().getOrCreateContext(), new NoOpTracer());
        }

        @Bean
        public Reporter fakeReporter() {
            return new FakeReporter();
        }

        @Bean
        public SubscriptionRepository subscriptionRepository() {
            return mock(SubscriptionRepository.class);
        }

        @Bean
        public InstallationRepository installationRepository() {
            return Mockito.mock(InstallationRepository.class);
        }

        @Bean
        public OrganizationRepository organizationRepository() {
            return Mockito.mock(OrganizationRepository.class);
        }

        @Bean
        public LicenseRepository licenseRepository() {
            return Mockito.mock(LicenseRepository.class);
        }

        @Bean
        public AccessPointRepository accessPointRepository() {
            return Mockito.mock(AccessPointRepository.class);
        }

        @Bean
        public EnvironmentRepository environmentRepository() {
            return Mockito.mock(EnvironmentRepository.class);
        }

        @Bean
        public LicenseManager licenseManager() {
            return new PermissiveLicenseManager();
        }
    }
}

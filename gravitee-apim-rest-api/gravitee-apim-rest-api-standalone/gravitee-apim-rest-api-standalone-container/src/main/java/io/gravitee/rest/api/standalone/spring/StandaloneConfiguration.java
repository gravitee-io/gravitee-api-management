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
package io.gravitee.rest.api.standalone.spring;

import io.gravitee.gamma.rest.infra.config.GammaAnalyticsConfiguration;
import io.gravitee.gamma.rest.infra.config.GammaLogsConfiguration;
import io.gravitee.gamma.rest.infra.config.GammaObservabilityFilterConfiguration;
import io.gravitee.gamma.rest.infra.config.GammaTracingConfiguration;
import io.gravitee.integration.controller.spring.IntegrationControllerConfiguration;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.NodeMetadataResolver;
import io.gravitee.node.container.NodeFactory;
import io.gravitee.platform.repository.api.RepositoryScopeProvider;
import io.gravitee.repository.management.api.StartupLockRepository;
import io.gravitee.rest.api.management.rest.spring.RestManagementConfiguration;
import io.gravitee.rest.api.portal.rest.spring.RestPortalConfiguration;
import io.gravitee.rest.api.repository.plugins.RestApiRepositoryScopeProvider;
import io.gravitee.rest.api.standalone.jetty.JettyConfiguration;
import io.gravitee.rest.api.standalone.jetty.JettyEmbeddedContainer;
import io.gravitee.rest.api.standalone.jetty.JettyServerFactory;
import io.gravitee.rest.api.standalone.node.AcquireStartupLockService;
import io.gravitee.rest.api.standalone.node.GraviteeApisNode;
import io.gravitee.rest.api.standalone.node.GraviteeApisNodeMetadataResolver;
import io.gravitee.rest.api.standalone.node.NodeStartupLock;
import io.gravitee.rest.api.standalone.node.ReleaseStartupLockService;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
@Import(
    {
        RestManagementConfiguration.class,
        io.gravitee.rest.api.management.v2.rest.spring.RestManagementConfiguration.class,
        IntegrationControllerConfiguration.class,
        RestPortalConfiguration.class,
        GammaTracingConfiguration.class,
        GammaObservabilityFilterConfiguration.class,
        GammaLogsConfiguration.class,
        GammaAnalyticsConfiguration.class,
    }
)
public class StandaloneConfiguration {

    private static final long DEFAULT_STARTUP_LOCK_TIMEOUT_MS = Duration.ofMinutes(10).toMillis();

    @Bean
    public Node node() {
        return new GraviteeApisNode();
    }

    @Bean
    public JettyConfiguration jettyConfiguration(io.gravitee.node.api.configuration.Configuration configuration) {
        return new JettyConfiguration(configuration);
    }

    @Bean
    public JettyServerFactory server(JettyConfiguration jettyConfiguration) {
        return new JettyServerFactory(jettyConfiguration);
    }

    @Bean
    public JettyEmbeddedContainer container(JettyServerFactory jettyServerFactory) {
        return new JettyEmbeddedContainer(jettyServerFactory);
    }

    @Bean
    public NodeStartupLock nodeStartupLock(
        io.gravitee.node.api.configuration.Configuration configuration,
        @Lazy StartupLockRepository startupLockRepository
    ) {
        Duration timeout = Duration.ofMillis(
            configuration.getProperty("upgrade.lock.timeout", Long.class, DEFAULT_STARTUP_LOCK_TIMEOUT_MS)
        );
        return new NodeStartupLock(startupLockRepository, timeout);
    }

    @Bean
    public AcquireStartupLockService acquireStartupLockService(NodeStartupLock nodeStartupLock) {
        return new AcquireStartupLockService(nodeStartupLock);
    }

    @Bean
    public ReleaseStartupLockService releaseStartupLockService(NodeStartupLock nodeStartupLock) {
        return new ReleaseStartupLockService(nodeStartupLock);
    }

    @Bean
    public NodeMetadataResolver nodeMetadataResolver() {
        return new GraviteeApisNodeMetadataResolver();
    }

    @Bean
    public RepositoryScopeProvider repositoryScopeProvider() {
        return new RestApiRepositoryScopeProvider();
    }
}

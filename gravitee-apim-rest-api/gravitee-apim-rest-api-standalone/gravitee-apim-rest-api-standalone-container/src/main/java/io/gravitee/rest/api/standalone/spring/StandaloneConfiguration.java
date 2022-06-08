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
package io.gravitee.rest.api.standalone.spring;

import io.gravitee.node.api.NodeMetadataResolver;
import io.gravitee.node.certificates.spring.NodeCertificatesConfiguration;
import io.gravitee.node.container.NodeFactory;
import io.gravitee.node.vertx.spring.VertxConfiguration;
import io.gravitee.platform.repository.api.RepositoryScopeProvider;
import io.gravitee.rest.api.management.rest.spring.RestManagementConfiguration;
import io.gravitee.rest.api.portal.rest.spring.RestPortalConfiguration;
import io.gravitee.rest.api.repository.plugins.RestApiRepositoryScopeProvider;
import io.gravitee.rest.api.repository.spring.RepositoryConfiguration;
import io.gravitee.rest.api.standalone.jetty.JettyConfiguration;
import io.gravitee.rest.api.standalone.jetty.JettyEmbeddedContainer;
import io.gravitee.rest.api.standalone.jetty.JettyServerFactory;
import io.gravitee.rest.api.standalone.node.GraviteeApisNode;
import io.gravitee.rest.api.standalone.node.GraviteeApisNodeMetadataResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
@Import(
    {
        VertxConfiguration.class,
        RepositoryConfiguration.class,
        RestManagementConfiguration.class,
        RestPortalConfiguration.class,
        NodeCertificatesConfiguration.class,
    }
)
public class StandaloneConfiguration {

    @Bean
    public NodeFactory node() {
        return new NodeFactory(GraviteeApisNode.class);
    }

    @Bean
    public JettyConfiguration jettyConfiguration() {
        return new JettyConfiguration();
    }

    @Bean
    public JettyServerFactory server() {
        return new JettyServerFactory();
    }

    @Bean
    public JettyEmbeddedContainer container() {
        return new JettyEmbeddedContainer();
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

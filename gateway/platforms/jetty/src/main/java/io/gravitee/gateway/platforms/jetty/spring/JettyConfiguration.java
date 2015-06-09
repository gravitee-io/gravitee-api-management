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
package io.gravitee.gateway.platforms.jetty.spring;

import io.gravitee.common.component.LifecycleComponent;
import io.gravitee.gateway.api.Node;
import io.gravitee.gateway.core.spring.CoreConfiguration;
import io.gravitee.gateway.platforms.jetty.JettyEmbeddedContainer;
import io.gravitee.gateway.platforms.jetty.node.JettyNode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Configuration
@Import(value = {CoreConfiguration.class})
public class JettyConfiguration {

    @Bean
    public Node node() {
        return new JettyNode();
    }

    @Bean(name = "jetty")
    public LifecycleComponent container() {
        return new JettyEmbeddedContainer();
    }
}

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

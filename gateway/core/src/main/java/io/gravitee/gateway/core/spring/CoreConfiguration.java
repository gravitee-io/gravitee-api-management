package io.gravitee.gateway.core.spring;

import io.gravitee.gateway.api.Registry;
import io.gravitee.gateway.core.Reactor;
import io.gravitee.gateway.core.impl.DefaultReactor;
import io.gravitee.gateway.core.registry.FileRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Configuration
public class CoreConfiguration {

    @Bean
    public Reactor reactor() {
        return new DefaultReactor();
    }

    @Bean
    public Registry registry() {
        return new FileRegistry();
    }
}

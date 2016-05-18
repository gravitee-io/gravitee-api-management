package io.gravitee.management.repository.spring;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
@Configuration
@ComponentScan(basePackages = "io.gravitee.management.repository.proxy")
public class RepositoryConfiguration {
}

package io.gravitee.gamma.module.authz.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring configuration for the authz Gamma plugin.
 *
 * <p>The plugin loader ({@code GammaModulePluginHandler}) creates a child
 * Spring context per plugin, scans this class via
 * {@code ReflectionBasedPluginConfigurationResolver}, and copies every bean
 * it produces into the parent rest-api context as a namespaced singleton.
 * Without the component scan reaching every {@code @Service}/{@code @Component}
 * /{@code @Repository} in the plugin, none of them get registered and Jersey's
 * {@code @Inject} resolves to null at request time.
 *
 * <p>{@code basePackages} MUST match the actual Java package the plugin lives
 * in — refactoring the package without updating this scan is a silent runtime
 * breakage (compilation stays green, only a live boot reveals it).
 */
@Configuration
@EnableScheduling
@ComponentScan(basePackages = "io.gravitee.gamma.module.authz")
public class SpringConfig {}

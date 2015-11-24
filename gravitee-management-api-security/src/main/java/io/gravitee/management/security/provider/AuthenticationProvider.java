package io.gravitee.management.security.provider;

import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public interface AuthenticationProvider {

    AuthenticationProviderType type();

    void configure(AuthenticationManagerBuilder authenticationManagerBuilder, int providerIdx) throws Exception;
}

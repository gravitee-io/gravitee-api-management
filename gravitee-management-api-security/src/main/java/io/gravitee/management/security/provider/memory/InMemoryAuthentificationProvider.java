package io.gravitee.management.security.provider.memory;

import io.gravitee.management.security.provider.AuthenticationProvider;
import io.gravitee.management.security.provider.AuthenticationProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class InMemoryAuthentificationProvider implements AuthenticationProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryAuthentificationProvider.class);

    @Autowired
    private Environment environment;

    @Override
    public void configure(AuthenticationManagerBuilder authenticationManagerBuilder, int providerIdx) throws Exception {
        boolean found = true;
        int userIdx = 0;

        while (found) {
            String user = environment.getProperty("security.providers[" + providerIdx + "].users[" + userIdx + "].user");
            found = (user != null);

            if (found) {
                String username = environment.getProperty("security.providers[" + providerIdx + "].users[" + userIdx + "].username");
                String password = environment.getProperty("security.providers[" + providerIdx + "].users[" + userIdx + "].password");
                String roles = environment.getProperty("security.providers[" + providerIdx + "].users[" + userIdx + "].roles");
                LOGGER.debug("Adding an in-memory user for username {}", username);
                userIdx++;
                authenticationManagerBuilder.inMemoryAuthentication().withUser(username).password(password).roles(roles);
            }
        }
    }

    @Override
    public AuthenticationProviderType type() {
        return AuthenticationProviderType.MEMORY;
    }
}

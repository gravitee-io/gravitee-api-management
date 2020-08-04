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
package io.gravitee.rest.api.idp.repository.authentication;

import io.gravitee.rest.api.idp.api.authentication.AuthenticationProvider;
import io.gravitee.rest.api.idp.repository.RepositoryIdentityProvider;
import io.gravitee.rest.api.idp.repository.authentication.spring.RepositoryAuthenticationProviderConfiguration;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.exceptions.UnauthorizedAccessException;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Import(RepositoryAuthenticationProviderConfiguration.class)
public class RepositoryAuthenticationProvider extends AbstractUserDetailsAuthenticationProvider
        implements AuthenticationProvider<org.springframework.security.authentication.AuthenticationProvider> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryAuthenticationProvider.class);

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        if (authentication.getCredentials() == null) {
            LOGGER.debug("Authentication failed: no credentials provided");
            throw new BadCredentialsException(messages.getMessage("AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad credentials"));
        }

        String presentedPassword = authentication.getCredentials().toString();

        if (!passwordEncoder.matches(presentedPassword, userDetails.getPassword())) {
            LOGGER.debug("Authentication failed: password does not match stored value");
            throw new BadCredentialsException(messages.getMessage("AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad credentials"));
        }

    }

    @Override
    protected UserDetails retrieveUser(String username, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        try {
            UserEntity user = userService.findBySource(RepositoryIdentityProvider.PROVIDER_TYPE, username, true);
            if (RepositoryIdentityProvider.PROVIDER_TYPE.equals(user.getSource())) {
                if (user.getPassword() == null) {
                    throw new BadCredentialsException(messages.getMessage(
                            "AbstractUserDetailsAuthenticationProvider.badCredentials",
                            "Bad credentials"));
                }
                if (user.getStatus().toUpperCase().equals("PENDING")) {
                    throw new UnauthorizedAccessException();
                }
                return mapUserEntityToUserDetails(user);
            } else {
                throw new UserNotFoundException(username);
            }
        } catch (UserNotFoundException notFound) {
            throw new UsernameNotFoundException(String.format("User '%s' not found", username), notFound);
        } catch (Exception repositoryProblem) {
            LOGGER.error("Failed to retrieveUser : {}", username, repositoryProblem);
            throw new InternalAuthenticationServiceException(repositoryProblem.getMessage(), repositoryProblem);
        }
    }

    private UserDetails mapUserEntityToUserDetails(UserEntity userEntity) {
        List<GrantedAuthority> authorities = AuthorityUtils.NO_AUTHORITIES;
        if (userEntity.getRoles() != null && userEntity.getRoles().size() > 0) {

            authorities = AuthorityUtils.commaSeparatedStringToAuthorityList(
                    userEntity.getRoles().stream().map(r -> r.getScope().name() + ':' + r.getName()).collect(Collectors.joining(","))
            );
        }

        io.gravitee.rest.api.idp.api.authentication.UserDetails userDetails = new io.gravitee.rest.api.idp.api.authentication.UserDetails(
                userEntity.getId(), userEntity.getPassword(), authorities);

        userDetails.setFirstname(userEntity.getFirstname());
        userDetails.setLastname(userEntity.getLastname());
        userDetails.setEmail(userEntity.getEmail());
        userDetails.setSource(RepositoryIdentityProvider.PROVIDER_TYPE);
        userDetails.setSourceId(userEntity.getSourceId());

        return userDetails;
    }

    @Override
    public org.springframework.security.authentication.AuthenticationProvider configure() throws Exception {
        return this;
    }
}

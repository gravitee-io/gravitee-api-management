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
package io.gravitee.management.security;

import io.gravitee.management.security.authentication.AuthenticationProviderManager;
import io.gravitee.management.security.authentication.impl.AuthenticationProviderManagerImpl;
import io.gravitee.management.security.config.BasicSecurityConfigurerAdapter;
import io.gravitee.management.security.cookies.CookieGenerator;
import io.gravitee.management.security.utils.AuthoritiesProvider;
import io.gravitee.management.service.MembershipService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.authentication.configuration.GlobalAuthenticationConfigurerAdapter;

/**
 * 
 * @author Titouan COMPIEGNE
 *
 */
@Configuration
@Import(BasicSecurityConfigurerAdapter.class)
public class SecurityConfiguration extends GlobalAuthenticationConfigurerAdapter {

    @Bean
    public CookieGenerator jwtCookieGenerator() {
        return new CookieGenerator();
    }

    @Bean
    public AuthoritiesProvider authoritiesProvider(MembershipService membershipService) {
        return new AuthoritiesProvider(membershipService);
    }

    @Bean
    public AuthenticationProviderManager authenticationProviderManager() {
        return new AuthenticationProviderManagerImpl();
    }
}

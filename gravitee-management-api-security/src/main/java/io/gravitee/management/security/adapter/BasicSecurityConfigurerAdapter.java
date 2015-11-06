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
package io.gravitee.management.security.adapter;

import io.gravitee.management.security.authentication.AuthenticationProviderType;
import io.gravitee.management.security.authentication.GraviteeAccountAuthenticationProvider;
import io.gravitee.management.security.filter.CORSFilter;
import io.gravitee.management.security.ldap.UserDetailsContextPropertiesMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.ldap.LdapAuthenticationProviderConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

import javax.servlet.Filter;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Titouan COMPIEGNE
 *
 */
@Configuration
@Profile("basic-auth")
@EnableWebSecurity
public class BasicSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(BasicSecurityConfigurerAdapter.class);
	
	@Autowired
	private Environment environment;
	
	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		LOGGER.info("Loading security providers for basic authentication");

		List<String> providers = getSecurityProviders();

		for (int idx = 0 ; idx < providers.size() ; idx++) {
			String providerType = providers.get(idx);
			switch (AuthenticationProviderType.valueOf(providerType.toUpperCase())) {
				case MEMORY:
					configureInMemorySecurityProvider(auth, idx);
					break;
				case LDAP:
					configureLDAPSecurityProvider(auth, idx);
					break;
				case GRAVITEE:
					configureGraviteeSecurityProvider(auth, idx);
					break;
				default:
					LOGGER.error("No authentication provider found for type: {}", providerType);
			}
		}
	}

	private List<String> getSecurityProviders() {
		LOGGER.debug("Looking for security provider...");
		List<String> providers = new ArrayList<>();

		boolean found = true;
		int idx = 0;

		while (found) {
			String type = environment.getProperty("security.providers[" + (idx++) + "].type");
			found = (type != null);
			if (found) {
				LOGGER.debug("\tSecurity type {} has been defined", type);
				providers.add(type);
			}
		}

		return providers;
	}

	private void configureInMemorySecurityProvider(AuthenticationManagerBuilder auth, int providerIdx) throws Exception {
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
				auth.inMemoryAuthentication().withUser(username).password(password).roles(roles);
			}
		}
	}

	private void configureGraviteeSecurityProvider(AuthenticationManagerBuilder auth, int providerIdx) throws Exception {
		GraviteeAccountAuthenticationProvider graviteeAccountAuthenticationProvider = graviteeAccountAuthenticationProvider();
		if (environment.getProperty("security.providers[" + providerIdx + "].password-encoding", boolean.class, false)) {
			graviteeAccountAuthenticationProvider.setPasswordEncoder(passwordEncoder());
		}
		auth.authenticationProvider(graviteeAccountAuthenticationProvider);
	}

	private void configureLDAPSecurityProvider(AuthenticationManagerBuilder auth, int providerIdx) throws Exception {
		LdapAuthenticationProviderConfigurer<AuthenticationManagerBuilder> ldapAuthenticationProviderConfigurer = auth.ldapAuthentication();
		ldapAuthenticationProviderConfigurer.userDnPatterns(environment.getProperty("security.providers[" + providerIdx + "].user-dn-patterns","uid={0},ou=people"));
		ldapAuthenticationProviderConfigurer.groupSearchBase(environment.getProperty("security.providers[" + providerIdx + "].group-search-base","ou=groups"));

		// set up embedded mode
		if (environment.getProperty("security.providers[" + providerIdx + "].embedded", boolean.class, false)) {
			ldapAuthenticationProviderConfigurer.contextSource()
					.root(environment.getProperty("security.providers[" + providerIdx + "].context-source-base"))
					.ldif("classpath:/ldap/gravitee-io-management-rest-api-ldap-test.ldif");
		} else {
			ldapAuthenticationProviderConfigurer.contextSource()
					.root(environment.getProperty("security.providers[" + providerIdx + "].context-source-base"))
					.managerDn(environment.getProperty("security.providers[" + providerIdx + "].context-source-username"))
					.managerPassword(environment.getProperty("security.providers[" + providerIdx + "].context-source-url"))
					.url(environment.getProperty("security.providers[" + providerIdx + "].url"));
		}
		// set up roles mapper
		if (environment.getProperty("security.providers[" + providerIdx + "].role-mapping", boolean.class, false)) {
			UserDetailsContextPropertiesMapper userDetailsContextPropertiesMapper = new UserDetailsContextPropertiesMapper();
			userDetailsContextPropertiesMapper.setAuthenticationProviderId(providerIdx);
			userDetailsContextPropertiesMapper.setEnvironment(environment);
			ldapAuthenticationProviderConfigurer.userDetailsContextMapper(userDetailsContextPropertiesMapper);
		}
	}

	/*
     * TODO : fix filter order between Jersey Filter (CORSResponseFilter) and Spring Security Filter
     * TODO : remove this filter or CORSResponseFilter when the problem will be solved
     */
    @Bean
	public Filter corsFilter() {
		return new CORSFilter();
	}
    
    @Bean
    public GraviteeAccountAuthenticationProvider graviteeAccountAuthenticationProvider() {
    	return new GraviteeAccountAuthenticationProvider();
    }
	
    @Bean
	public PasswordEncoder passwordEncoder(){
		return new BCryptPasswordEncoder();
    }
    
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
			.httpBasic()
				.realmName("Gravitee Management API")
			.and()
				.sessionManagement()
					.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			.and()
				.authorizeRequests()
					.antMatchers(HttpMethod.OPTIONS, "**").permitAll()
					.anyRequest().authenticated()
			.and()
				.csrf()
					.disable()
			.addFilterAfter(corsFilter(), AbstractPreAuthenticatedProcessingFilter.class);
	}
}

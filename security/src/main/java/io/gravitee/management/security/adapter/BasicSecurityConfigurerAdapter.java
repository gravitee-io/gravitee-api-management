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

import java.util.Properties;

import javax.servlet.Filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.ldap.LdapAuthenticationProviderConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

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
	private Properties graviteeProperties;
	
	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		int authenticationProviderSize = (int) graviteeProperties.get("security.authentication-manager.authentication-providers.size");
		for (int i = 1; i <= authenticationProviderSize; i++) {
			String authenticationProviderType = (String) graviteeProperties.get("security.authentication-manager.authentication-providers.authentication-provider-"+i+".type");
			switch (AuthenticationProviderType.valueOf(authenticationProviderType.toUpperCase())) {
				case MEMORY :
					int userSize = (int) graviteeProperties.get("security.authentication-manager.authentication-providers.authentication-provider-"+i+".users.size");
					for (int j = 1; j <= userSize; j++) {
						String username = (String) graviteeProperties.get("security.authentication-manager.authentication-providers.authentication-provider-"+i+".users.user-"+j+".username");
						String password = (String) graviteeProperties.get("security.authentication-manager.authentication-providers.authentication-provider-"+i+".users.user-"+j+".password");
						String roles = (String) graviteeProperties.get("security.authentication-manager.authentication-providers.authentication-provider-"+i+".users.user-"+j+".roles");
						auth.inMemoryAuthentication().withUser(username).password(password).roles(roles);
					}
					break;
				case LDAP :
					LdapAuthenticationProviderConfigurer<AuthenticationManagerBuilder> ldapAuthenticationProviderConfigurer = auth.ldapAuthentication();
					ldapAuthenticationProviderConfigurer.userDnPatterns((String) graviteeProperties.getProperty("security.authentication-manager.authentication-providers.authentication-provider-"+i+".user-dn-patterns","uid={0},ou=people"));
					ldapAuthenticationProviderConfigurer.groupSearchBase((String) graviteeProperties.getProperty("security.authentication-manager.authentication-providers.authentication-provider-"+i+".group-search-base","ou=groups"));
					// set up embedded mode
					if ((boolean) graviteeProperties.get("security.authentication-manager.authentication-providers.authentication-provider-"+i+".embedded")) {
						ldapAuthenticationProviderConfigurer.contextSource()
							.root((String) graviteeProperties.get("security.authentication-manager.authentication-providers.authentication-provider-"+i+".context-source-base"))
							.ldif("classpath:/ldap/gravitee-io-management-rest-api-ldap-test.ldif");
					} else {
						ldapAuthenticationProviderConfigurer.contextSource()
							.root((String) graviteeProperties.get("security.authentication-manager.authentication-providers.authentication-provider-"+i+".context-source-base"))
							.managerDn((String) graviteeProperties.get("security.authentication-manager.authentication-providers.authentication-provider-"+i+".context-source-username"))
							.managerPassword((String) graviteeProperties.get("security.authentication-manager.authentication-providers.authentication-provider-"+i+".context-source-url"))
							.url((String) graviteeProperties.get("security.authentication-manager.authentication-providers.authentication-provider-"+i+".url"));
					}
					// set up roles mapper
					if ((boolean) graviteeProperties.get("security.authentication-manager.authentication-providers.authentication-provider-"+i+".role-mapping")) {
						UserDetailsContextMapper userDetailsContextPropertiesMapper = new UserDetailsContextPropertiesMapper();
						((UserDetailsContextPropertiesMapper) userDetailsContextPropertiesMapper).setAuthenticationProviderId(i);
						((UserDetailsContextPropertiesMapper) userDetailsContextPropertiesMapper).setProperties(graviteeProperties);
						ldapAuthenticationProviderConfigurer.userDetailsContextMapper(userDetailsContextPropertiesMapper);
					}
					break;
				case GRAVITEE :
					GraviteeAccountAuthenticationProvider graviteeAccountAuthenticationProvider = graviteeAccountAuthenticationProvider();
					if ((boolean) graviteeProperties.get("security.authentication-manager.authentication-providers.authentication-provider-"+i+".password-encoding")) {
						graviteeAccountAuthenticationProvider.setPasswordEncoder(passwordEncoder());
					}
					auth.authenticationProvider(graviteeAccountAuthenticationProvider);
					break;
				default:
					LOGGER.info("No AuthenticationProviderType found for {}", authenticationProviderType);
			}
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
					.sessionCreationPolicy(SessionCreationPolicy.NEVER)
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

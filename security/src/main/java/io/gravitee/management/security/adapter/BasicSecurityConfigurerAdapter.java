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

import java.util.Properties;

import javax.servlet.Filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

import io.gravitee.management.security.filter.CORSFilter;

/**
 * 
 * @author Titouan COMPIEGNE
 *
 */
@Configuration
@Profile("basic-auth")
@EnableWebSecurity
//@ImportResource("classpath:/spring/gravitee-io-management-rest-api-security-context.xml")
public class BasicSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {
	
	@Autowired
	private Properties graviteeProperties;
	
	@Bean(name="authenticationManager")
    @Override
	public AuthenticationManager authenticationManagerBean() throws Exception {
		return super.authenticationManagerBean();
	}
	
	@Autowired
	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		int authenticationProviderSize = (int) graviteeProperties.get("security.authentication-manager.authentication-providers.size");
		for (int i = 1; i <= authenticationProviderSize; i++) {
			String authenticationProviderType = (String) graviteeProperties.get("security.authentication-manager.authentication-providers.authentication-provider-"+i+".type");
			switch (authenticationProviderType) {
				case "memory" :
					int userSize = (int) graviteeProperties.get("security.authentication-manager.authentication-providers.authentication-provider-"+i+".users.size");
					for (int j = 1; j <= userSize; j++) {
						String username = (String) graviteeProperties.get("security.authentication-manager.authentication-providers.authentication-provider-"+i+".users.user-"+j+".username");
						String password = (String) graviteeProperties.get("security.authentication-manager.authentication-providers.authentication-provider-"+i+".users.user-"+j+".password");
						String roles = (String) graviteeProperties.get("security.authentication-manager.authentication-providers.authentication-provider-"+i+".users.user-"+j+".roles");
						auth.inMemoryAuthentication().withUser(username).password(password).roles(roles);
					}
					break;
				case "ldap" :
					if ((boolean) graviteeProperties.get("security.authentication-manager.authentication-providers.authentication-provider-"+i+".embedded")) {
						auth
						.ldapAuthentication()
							.userDnPatterns("uid={0},ou=people")
							.groupSearchBase("ou=groups")
							.contextSource().root("dc=gravitee,dc=io").ldif("classpath:/spring/gravitee-io-management-rest-api-ldap-test.ldif");
					} else {
						auth
						.ldapAuthentication()
							.userDnPatterns("uid={0},ou=people")
							.contextSource()
								.managerDn((String) graviteeProperties.get("security.authentication-manager.authentication-providers.authentication-provider-"+i+".managerDn"))
								.managerPassword((String) graviteeProperties.get("security.authentication-manager.authentication-providers.authentication-provider-"+i+".managerPassword"))
								.url((String) graviteeProperties.get("security.authentication-manager.authentication-providers.authentication-provider-"+i+".url"));
					}
					break;
				default:
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

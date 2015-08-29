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

import io.gravitee.management.security.filter.CORSFilter;

import javax.servlet.Filter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

/**
 * 
 * @author Titouan COMPIEGNE
 *
 */
@Configuration
@Profile("basic-auth")
@EnableWebSecurity
@ImportResource("classpath:/spring/gravitee-io-management-rest-api-security-context.xml")
public class BasicSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

	@Bean(name = "graviteeAuthenticationManager")
	@Override
	public AuthenticationManager authenticationManagerBean() throws Exception {
		return super.authenticationManagerBean();
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
					.anyRequest().hasRole("USER")
			.and()
				.csrf()
					.disable()
			.addFilterAfter(corsFilter(), AbstractPreAuthenticatedProcessingFilter.class);
	}
}

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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;

/**
 * 
 * @author Titouan COMPIEGNE
 *
 */
@Configuration
@Profile("oauth2")
@EnableWebSecurity
@EnableResourceServer
public class OAuth2SecurityConfigurerAdapter extends ResourceServerConfigurerAdapter {
	
	private static final String RESOURCE_ID = "openid";
	
	@Value("${oauth.endpoint.check_token}")
	private String oauthEndpointCheckToken;
	
	@Value("{oauth.client.id}")
	private String oauthClientId;
	
	@Value("{oauth.client.secret}")
	private String oauthClientSecret;
	
	@Autowired
	private ResourceServerTokenServices tokenServices;
	
	@Override
	public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
		resources.tokenServices(tokenServices).resourceId(RESOURCE_ID);
	}
	
	@Override
	public void configure(HttpSecurity http) throws Exception {
		http.authorizeRequests().antMatchers("/**").access("#oauth2.hasScope('read')");	
	}
	
	@Bean
	public RemoteTokenServices remoteTokenServices() {
		RemoteTokenServices s = new RemoteTokenServices();
		s.setCheckTokenEndpointUrl(oauthEndpointCheckToken);
		s.setClientId(oauthClientId);
		s.setClientSecret(oauthClientSecret);
		return s;
	}
}

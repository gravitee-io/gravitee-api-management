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
package io.gravitee.management.providers.ldap.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Configuration
public class LdapConfiguration {

	@Bean
	public LdapContextSourceFactory contextSourceFactory() {
		return new LdapContextSourceFactory();

	}

	/*
	@Bean
	public LdapContextSource contextSource () {
		// Embedded LDAP server (launch via the Security Configuration)
		// TODO : Retrieve properties for gravitee.yml
		// Make it an Identity Provider
		LdapContextSource contextSource= new LdapContextSource();
		contextSource.setUrl("ldap://localhost:33389");
		contextSource.setBase("dc=gravitee,dc=io");
		return contextSource;
	}
	*/

	@Bean
	public LdapTemplate ldapTemplate(LdapContextSource contextSource) {
		return new LdapTemplate(contextSource);
	}
}

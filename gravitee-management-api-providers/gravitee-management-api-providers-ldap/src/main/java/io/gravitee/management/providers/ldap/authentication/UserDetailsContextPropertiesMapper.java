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
package io.gravitee.management.providers.ldap.authentication;

import io.gravitee.management.providers.core.authentication.GraviteeUserDetails;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at gravitee.io)
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class UserDetailsContextPropertiesMapper implements UserDetailsContextMapper {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(UserDetailsContextPropertiesMapper.class);

	private int authenticationProviderId;
	
	private Environment environment;

	@Override
	public UserDetails mapUserFromContext(DirContextOperations ctx, String username, Collection<? extends GrantedAuthority> authorities) {
		List<GrantedAuthority> mappedAuthorities = new ArrayList<>();
		try {
			for (GrantedAuthority granted : authorities) {
				String mappedAuthority = environment.getProperty("security.providers[" + authenticationProviderId+ "].role-mapper."+granted.getAuthority());
				if (!StringUtils.isEmpty(mappedAuthority)) {
					mappedAuthorities.add(new SimpleGrantedAuthority(mappedAuthority));
				}
			}
		} catch (Exception e){
			LOGGER.error("Failed to load mapped authorities", e);
		}
		mappedAuthorities.addAll(authorities);
		return new GraviteeUserDetails(username, "", mappedAuthorities, ctx.getStringAttribute("mail"),
				ctx.getStringAttribute("givenName"), ctx.getStringAttribute("sn"));
	}

	@Override
	public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
		throw new UnsupportedOperationException(
				"UserDetailsContextPropertiesMapper only supports reading from a context. Please"
						+ "use a subclass if mapUserToContext() is required.");
	}

	public void setAuthenticationProviderId(int authenticationProviderId) {
		this.authenticationProviderId = authenticationProviderId;
	}
	
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

}

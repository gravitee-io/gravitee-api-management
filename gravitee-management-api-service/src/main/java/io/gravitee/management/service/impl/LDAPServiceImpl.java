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
package io.gravitee.management.service.impl;

import io.gravitee.management.model.UserEntity;
import io.gravitee.management.service.LDAPService;

import java.util.List;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.filter.WhitespaceWildcardsFilter;
import org.springframework.stereotype.Component;

/**
 * @author Titouan COMPIEGNE
 */
@Component
public class LDAPServiceImpl implements LDAPService {

	@Autowired
	private LdapTemplate ldapTemplate;

	@Override
	public List<UserEntity> findByCn(String cn) {
		AndFilter filter = new AndFilter();
		filter.and(new EqualsFilter("objectclass", "person"));
		filter.and(new WhitespaceWildcardsFilter("cn", cn));
		return (List<UserEntity>) ldapTemplate.search("", filter.encode(), new UserAttributesMapper());
	}

	private class UserAttributesMapper implements AttributesMapper {
		public Object mapFromAttributes(Attributes attrs) throws NamingException {
			UserEntity user = new UserEntity();
			user.setUsername((String) attrs.get("uid").get());
			user.setFirstname((String) attrs.get("sn").get());
			user.setLastname((String) attrs.get("cn").get());
			return user;
		}
	}
}

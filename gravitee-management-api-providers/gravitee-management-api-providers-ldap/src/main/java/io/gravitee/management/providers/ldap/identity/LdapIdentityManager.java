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
package io.gravitee.management.providers.ldap.identity;

import io.gravitee.management.providers.core.identity.IdentityManager;
import io.gravitee.management.providers.core.identity.User;
import io.gravitee.management.providers.ldap.LdapProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.filter.WhitespaceWildcardsFilter;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import java.util.Collection;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at gravitee.io)
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class LdapIdentityManager implements IdentityManager {

	@Autowired
	private LdapTemplate ldapTemplate;

	@Override
	public Collection<User> search(String query) {
		AndFilter filter = new AndFilter();
		filter.and(new EqualsFilter("objectclass", "person"));
		filter.and(new WhitespaceWildcardsFilter("cn", query));
		return ldapTemplate.search("", filter.encode(), new UserAttributesMapper());
	}

	private class UserAttributesMapper implements AttributesMapper<User> {
		public User mapFromAttributes(Attributes attrs) throws NamingException {
			User user = new User();
			user.setId(attributeValue(attrs, "uid"));
			user.setFirstname(attributeValue(attrs, "givenname"));
			user.setLastname(attributeValue(attrs, "cn"));
			user.setEmail(attributeValue(attrs, "mail"));
			user.setProvider(LdapProvider.PROVIDER_TYPE);
			return user;
		}
	}

	private String attributeValue(Attributes attrs, String attributeId) throws NamingException {
		Attribute attr = attrs.get(attributeId);
		if (attr != null) {
			return (String) attr.get();
		}

		return null;
	}
}

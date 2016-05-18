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
package io.gravitee.management.idp.ldap.lookup;

import io.gravitee.management.idp.api.identity.IdentityLookup;
import io.gravitee.management.idp.api.identity.User;
import io.gravitee.management.idp.ldap.lookup.spring.LdapIdentityLookupConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
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
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
@Import(LdapIdentityLookupConfiguration.class)
public class LdapIdentityLookup implements IdentityLookup<String> {

    @Autowired
    private LdapTemplate ldapTemplate;

    @Override
    public Collection<User> search(String query) {
        AndFilter filter = new AndFilter();
        filter.and(new EqualsFilter("objectclass", "person"));
        filter.and(new WhitespaceWildcardsFilter("cn", query));
        return ldapTemplate.search("", filter.encode(), new UserAttributesMapper());
    }

    @Override
    public User retrieve(String dn) {
        return ldapTemplate.lookup(dn, new UserAttributesMapper());
    }

    private class UserAttributesMapper implements AttributesMapper<User> {
        public User mapFromAttributes(Attributes attrs) throws NamingException {
            LdapUser user = new LdapUser(attributeValue(attrs, "uid"));
            user.setFirstname(attributeValue(attrs, "givenname"));
            user.setLastname(attributeValue(attrs, "cn"));
            user.setEmail(attributeValue(attrs, "mail"));
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

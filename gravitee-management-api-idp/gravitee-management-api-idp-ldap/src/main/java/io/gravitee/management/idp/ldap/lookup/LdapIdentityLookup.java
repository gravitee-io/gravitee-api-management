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

import com.sun.jndi.ldap.LdapCtx;
import io.gravitee.management.idp.api.identity.IdentityLookup;
import io.gravitee.management.idp.api.identity.User;
import io.gravitee.management.idp.ldap.lookup.spring.LdapIdentityLookupConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.filter.WhitespaceWildcardsFilter;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.ldap.query.LdapQueryBuilder;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import java.util.Collection;
import java.util.List;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
@Import(LdapIdentityLookupConfiguration.class)
public class LdapIdentityLookup implements IdentityLookup<String>, InitializingBean {

    private final Logger LOGGER = LoggerFactory.getLogger(LdapIdentityLookup.class);

    @Autowired
    private LdapTemplate ldapTemplate;

    @Autowired
    private Environment environment;

    private String identifierAttribute = "uid";

    @Override
    public void afterPropertiesSet() throws Exception {
        String searchFilter = environment.getProperty("user-search-filter");
        LOGGER.debug("Looking for a LDAP user's identifier using search filter [{}]", searchFilter);

        if (searchFilter != null) {
            // Search filter can be uid={0} or mail={0}
            identifierAttribute = searchFilter.split("=")[0];
        }

        LOGGER.info("User identifier is based on the [{}] attribute", identifierAttribute);
    }

    @Override
    public Collection<User> search(String query) {
        AndFilter filter = new AndFilter();
        filter.and(new EqualsFilter("objectclass", "person"));
        filter.and(new WhitespaceWildcardsFilter("cn", query));

        LdapQuery ldapQuery = LdapQueryBuilder
                .query()
                .countLimit(20)
                .timeLimit(5000)
                .attributes(identifierAttribute, "givenname", "sn", "mail")
                .filter(filter);


        return ldapTemplate.search(ldapQuery, new UserAttributesMapper());
    }

    @Override
    public User retrieve(String id) {
        AndFilter filter = new AndFilter();
        filter.and(new EqualsFilter("objectclass", "person"));
        filter.and(new EqualsFilter(identifierAttribute, id));

        LdapQuery ldapQuery = LdapQueryBuilder
                .query()
                .countLimit(1)
                .timeLimit(5000)
                .attributes(identifierAttribute, "givenname", "sn", "mail")
                .filter(filter);

        List<User> users = ldapTemplate.search(ldapQuery, new UserAttributesMapper());
        if (users != null && ! users.isEmpty()) {
            LdapUser user = (LdapUser) users.iterator().next();
            List<String> result = ldapTemplate.search(
                    "", filter.encode(),
                    (ContextMapper<String>) o -> ((LdapCtx) o).getNameInNamespace());
            user.setDn(result.iterator().next());

            return user;
        } else {
            return null;
        }
    }

    private class UserAttributesMapper implements AttributesMapper<User> {
        public User mapFromAttributes(Attributes attrs) throws NamingException {
            LdapUser user = new LdapUser(attributeValue(attrs, identifierAttribute));
            user.setFirstname(attributeValue(attrs, "givenname"));
            user.setLastname(attributeValue(attrs, "sn"));
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

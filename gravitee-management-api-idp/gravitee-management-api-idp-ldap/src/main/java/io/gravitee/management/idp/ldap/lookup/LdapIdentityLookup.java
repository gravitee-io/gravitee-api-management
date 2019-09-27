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
import io.gravitee.management.idp.api.identity.IdentityReference;
import io.gravitee.management.idp.api.identity.User;
import io.gravitee.management.idp.ldap.LdapIdentityProvider;
import io.gravitee.management.idp.ldap.lookup.spring.LdapIdentityLookupConfiguration;
import io.gravitee.management.idp.ldap.utils.LdapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.ldap.CommunicationException;
import org.springframework.ldap.LimitExceededException;
import org.springframework.ldap.NameNotFoundException;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.AbstractContextMapper;
import org.springframework.ldap.filter.*;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.ldap.query.SearchScope;
import org.springframework.ldap.support.LdapNameBuilder;

import javax.naming.ldap.LdapName;
import java.util.Collection;
import java.util.Collections;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Import(LdapIdentityLookupConfiguration.class)
public class LdapIdentityLookup implements IdentityLookup, InitializingBean {

    private final Logger LOGGER = LoggerFactory.getLogger(LdapIdentityLookup.class);

    private final static String LDAP_ATTRIBUTE_GIVENNAME = "givenName";
    private final static String LDAP_ATTRIBUTE_SURNAME = "sn";
    private final static String LDAP_ATTRIBUTE_MAIL = "mail";
    private final static String LDAP_ATTRIBUTE_DISPLAYNAME = "displayName";
    private final static String LDAP_DEFAULT_LOOKUP_FILTER ="(&(objectClass=Person)(|(cn=*{0}*)(uid={0})))";


    @Autowired
    private LdapTemplate ldapTemplate;

    @Autowired
    private Environment environment;

    private String identifierAttribute;

    private LdapName baseDn;

    private String [] userAttributes;

    @Override
    public void afterPropertiesSet() throws Exception {
        String searchFilter = environment.getProperty("lookup.user.filter");
        LOGGER.debug("Looking for a LDAP user's identifier using search filter [{}]", searchFilter);

        if (searchFilter != null) {
            identifierAttribute = LdapUtils.extractAttribute(searchFilter);

            LOGGER.info("User identifier is based on the [{}] attribute", identifierAttribute);

            userAttributes = new String [] {
                    identifierAttribute, LDAP_ATTRIBUTE_GIVENNAME, LDAP_ATTRIBUTE_SURNAME,
                    LDAP_ATTRIBUTE_MAIL, LDAP_ATTRIBUTE_DISPLAYNAME
            };
        } else {
            userAttributes = new String [] {
                    LDAP_ATTRIBUTE_GIVENNAME, LDAP_ATTRIBUTE_SURNAME,
                    LDAP_ATTRIBUTE_MAIL, LDAP_ATTRIBUTE_DISPLAYNAME
            };
        }

        // Base DN to search for users
        baseDn = LdapNameBuilder
                .newInstance(environment.getProperty("context.base"))
                .add(environment.getProperty("lookup.user.base", ""))
                .build();

        LOGGER.info("User search is based on DN [{}]", baseDn);
    }

    @Override
    public Collection<User> search(String query) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            String usersSearchFilter = environment.getProperty("lookup.user.filter", LDAP_DEFAULT_LOOKUP_FILTER);
            String hardcodedFilter = usersSearchFilter.replaceAll("\\{0}", LdapUtils.addWhitespaceWildcards(query));

            LdapQuery ldapQuery = LdapQueryBuilder
                    .query()
                    .base(baseDn)
                    .countLimit(20)
                    .timeLimit(5000)
                    .searchScope(SearchScope.SUBTREE)
                    .attributes(
                            LDAP_ATTRIBUTE_GIVENNAME,
                            LDAP_ATTRIBUTE_SURNAME,
                            LDAP_ATTRIBUTE_MAIL,
                            LDAP_ATTRIBUTE_DISPLAYNAME)
                    .filter(new HardcodedFilter(hardcodedFilter));

            return ldapTemplate.search(ldapQuery, USER_CONTEXT_MAPPER);
        } catch(LimitExceededException lee) {
            LOGGER.info("Too much results while searching for [{}]. Returns an empty list.", query);
            return Collections.emptyList();
        } catch(CommunicationException ce) {
            LOGGER.error("LDAP server is not reachable.");
            return Collections.emptyList();
        } finally {
            Thread.currentThread().setContextClassLoader(classLoader);
        }
    }

    @Override
    public boolean canHandle(IdentityReference identityReference) {
        return LdapIdentityProvider.PROVIDER_TYPE.equalsIgnoreCase(identityReference.getSource());
    }

    @Override
    public User retrieve(IdentityReference identityReference) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            return ldapTemplate.lookup(identityReference.getReference(), userAttributes, USER_CONTEXT_MAPPER);
        } catch (final NameNotFoundException nnfe) {
            return null;
        } finally {
            Thread.currentThread().setContextClassLoader(classLoader);
        }
    }

    @Override
    public boolean allowEmailInSearchResults() {
        Boolean allow = environment.getProperty("lookup.allow-email-in-search-results",Boolean.class, false);
        return allow != null && allow;
    }

    private final ContextMapper<User> USER_CONTEXT_MAPPER = new AbstractContextMapper<User>() {

        @Override
        protected User doMapFromContext(DirContextOperations ctx) {
            LdapUser user = new LdapUser(ctx.getDn().toString());
            user.setFirstname(ctx.getStringAttribute(LDAP_ATTRIBUTE_GIVENNAME));
            user.setLastname(ctx.getStringAttribute(LDAP_ATTRIBUTE_SURNAME));
            user.setEmail(ctx.getStringAttribute(LDAP_ATTRIBUTE_MAIL));
            user.setDisplayName(ctx.getStringAttribute(LDAP_ATTRIBUTE_DISPLAYNAME));

            if (user.getDisplayName() == null) {
                user.setDisplayName(user.getFirstname() + ' ' + user.getLastname());
            }

            return user;
        }
    };
}

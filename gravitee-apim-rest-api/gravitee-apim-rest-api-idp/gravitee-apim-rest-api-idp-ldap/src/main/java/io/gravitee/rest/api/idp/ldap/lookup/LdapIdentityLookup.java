/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.idp.ldap.lookup;

import io.gravitee.rest.api.idp.api.identity.IdentityLookup;
import io.gravitee.rest.api.idp.api.identity.IdentityReference;
import io.gravitee.rest.api.idp.api.identity.User;
import io.gravitee.rest.api.idp.ldap.LdapIdentityProvider;
import io.gravitee.rest.api.idp.ldap.lookup.spring.LdapIdentityLookupConfiguration;
import io.gravitee.rest.api.idp.ldap.utils.LdapUtils;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.naming.ldap.LdapName;
import lombok.CustomLog;
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
import org.springframework.ldap.filter.HardcodedFilter;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.ldap.query.SearchScope;
import org.springframework.ldap.support.LdapNameBuilder;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@Import(LdapIdentityLookupConfiguration.class)
public class LdapIdentityLookup implements IdentityLookup, InitializingBean {

    private static final String LDAP_ATTRIBUTE_GIVENNAME = "givenName";
    private static final String LDAP_ATTRIBUTE_SURNAME = "sn";
    private static final String LDAP_ATTRIBUTE_MAIL = "mail";
    private static final String LDAP_ATTRIBUTE_DISPLAYNAME = "displayName";
    private static final String LDAP_DEFAULT_LOOKUP_FILTER = "(&(objectClass=Person)(|(cn=*{0}*)(uid={0})))";

    @Autowired
    private LdapTemplate ldapTemplate;

    @Autowired
    private Environment environment;

    private String identifierAttribute;

    private LdapName baseDn;

    private String[] userAttributes;
    private Set<String> lookupActivations;

    @Override
    public void afterPropertiesSet() throws Exception {
        String searchFilter = environment.getProperty("lookup.user.filter");
        log.debug("Looking for a LDAP user's identifier using search filter [{}]", searchFilter);

        if (searchFilter != null) {
            identifierAttribute = LdapUtils.extractAttribute(searchFilter);

            log.info("User identifier is based on the [{}] attribute", identifierAttribute);

            userAttributes = new String[] {
                identifierAttribute,
                LDAP_ATTRIBUTE_GIVENNAME,
                LDAP_ATTRIBUTE_SURNAME,
                LDAP_ATTRIBUTE_MAIL,
                LDAP_ATTRIBUTE_DISPLAYNAME,
            };
        } else {
            userAttributes = new String[] {
                LDAP_ATTRIBUTE_GIVENNAME,
                LDAP_ATTRIBUTE_SURNAME,
                LDAP_ATTRIBUTE_MAIL,
                LDAP_ATTRIBUTE_DISPLAYNAME,
            };
        }

        // Base DN to search for users
        baseDn = LdapNameBuilder.newInstance(environment.getProperty("context.base"))
            .add(environment.getProperty("lookup.user.base", ""))
            .build();

        log.info("User search is based on DN [{}]", baseDn);
        lookupActivations = readLookupActivations();
        if (!lookupActivations.isEmpty()) {
            log.info("User search is restricted to the activations {}", lookupActivations);
        }
    }

    @Override
    public Collection<User> search(String query) {
        if (!activatedForCurrentRequest()) {
            return Collections.emptyList();
        }
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            String usersSearchFilter = environment.getProperty("lookup.user.filter", LDAP_DEFAULT_LOOKUP_FILTER);
            String hardcodedFilter = usersSearchFilter.replaceAll("\\{0}", LdapUtils.addWhitespaceWildcards(query));

            LdapQuery ldapQuery = LdapQueryBuilder.query()
                .base(baseDn)
                .countLimit(20)
                .timeLimit(5000)
                .searchScope(SearchScope.SUBTREE)
                .attributes(LDAP_ATTRIBUTE_GIVENNAME, LDAP_ATTRIBUTE_SURNAME, LDAP_ATTRIBUTE_MAIL, LDAP_ATTRIBUTE_DISPLAYNAME)
                .filter(new HardcodedFilter(hardcodedFilter));

            return ldapTemplate.search(ldapQuery, USER_CONTEXT_MAPPER);
        } catch (LimitExceededException lee) {
            log.info("Too much results while searching for [{}]. Returns an empty list.", query);
            return Collections.emptyList();
        } catch (CommunicationException ce) {
            log.error("LDAP server is not reachable.");
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
        if (!activatedForCurrentOrganization()) {
            return null;
        }
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
        Boolean allow = environment.getProperty("lookup.allow-email-in-search-results", Boolean.class, false);
        return allow != null && allow;
    }

    private boolean activatedForCurrentRequest() {
        return lookupActivations.isEmpty() || activated(currentTarget());
    }

    // The console searches at organization level but adds members at environment level, so a reference
    // found by an organization-level search must stay resolvable from the environments of that organization.
    private boolean activatedForCurrentOrganization() {
        return lookupActivations.isEmpty() || activated(GraviteeContext.getCurrentOrganization()) || activated(currentTarget());
    }

    private boolean activated(String target) {
        if (lookupActivations.contains(target)) {
            return true;
        }
        log.debug("Lookup is not activated for [{}], the directory is not queried", target);
        return false;
    }

    private Set<String> readLookupActivations() {
        Set<String> activations = new LinkedHashSet<>();
        String commaSeparated = environment.getProperty("lookup.activations");
        if (commaSeparated != null) {
            for (String activation : commaSeparated.split(",")) {
                addActivation(activations, activation);
            }
        }
        for (int i = 0; ; i++) {
            String activation = environment.getProperty("lookup.activations[" + i + "]");
            if (activation == null) {
                return activations;
            }
            addActivation(activations, activation);
        }
    }

    private static void addActivation(Set<String> activations, String activation) {
        if (activation.isBlank()) {
            log.warn("Ignoring a blank entry in lookup.activations");
        } else {
            activations.add(activation.strip());
        }
    }

    // Same targets as the activations of the other identity providers: an organization id for an
    // organization-level request (console user search), organization:environment for an
    // environment-level request (portal user search).
    private static String currentTarget() {
        String organizationId = GraviteeContext.getCurrentOrganization();
        String environmentId = GraviteeContext.getCurrentEnvironment();
        return environmentId == null ? organizationId : organizationId + ":" + environmentId;
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

    @Override
    public int getOrder() {
        return 30;
    }
}

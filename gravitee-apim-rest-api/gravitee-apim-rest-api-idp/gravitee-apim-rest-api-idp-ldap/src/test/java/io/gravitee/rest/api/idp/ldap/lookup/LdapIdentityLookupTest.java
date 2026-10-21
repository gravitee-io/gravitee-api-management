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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.idp.api.identity.IdentityReference;
import io.gravitee.rest.api.idp.api.identity.User;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.mock.env.MockEnvironment;

@ExtendWith(MockitoExtension.class)
class LdapIdentityLookupTest {

    private static final String ORGANIZATION = "acme";
    private static final String OTHER_ORGANIZATION = "other-organization";
    private static final String ENVIRONMENT = "prod";
    private static final User ALICE = new LdapUser("uid=alice,ou=users,dc=example,dc=org");

    @Mock
    private LdapTemplate ldapTemplate;

    @Spy
    private MockEnvironment environment = new MockEnvironment()
        .withProperty("context.base", "dc=example,dc=org")
        .withProperty("lookup.user.base", "ou=users");

    @InjectMocks
    private LdapIdentityLookup cut;

    @AfterEach
    void clean_context() {
        GraviteeContext.cleanContext();
    }

    @Test
    void should_query_the_directory_from_any_organization_without_activations() throws Exception {
        givenLookupWithoutActivations();
        givenDirectoryHolds(ALICE);
        givenConsoleRequestFrom(OTHER_ORGANIZATION);

        assertThat(cut.search("ali")).containsExactly(ALICE);
    }

    @Test
    void should_query_the_directory_from_an_activated_organization() throws Exception {
        givenLookupActivatedFor(ORGANIZATION);
        givenDirectoryHolds(ALICE);
        givenConsoleRequestFrom(ORGANIZATION);

        assertThat(cut.search("ali")).containsExactly(ALICE);
    }

    @Test
    void should_return_nothing_from_an_organization_that_is_not_activated() throws Exception {
        givenLookupActivatedFor(ORGANIZATION);
        givenConsoleRequestFrom(OTHER_ORGANIZATION);

        assertThat(cut.search("ali")).isEmpty();
        verifyNoInteractions(ldapTemplate);
    }

    @Test
    void should_return_nothing_in_the_portal_of_an_activated_organization_without_environment_activation() throws Exception {
        givenLookupActivatedFor(ORGANIZATION);
        givenPortalRequestFrom(ORGANIZATION, ENVIRONMENT);

        assertThat(cut.search("ali")).isEmpty();
        verifyNoInteractions(ldapTemplate);
    }

    @Test
    void should_query_the_directory_in_the_portal_of_an_activated_environment() throws Exception {
        givenLookupActivatedFor(ORGANIZATION + ":" + ENVIRONMENT);
        givenDirectoryHolds(ALICE);
        givenPortalRequestFrom(ORGANIZATION, ENVIRONMENT);

        assertThat(cut.search("ali")).containsExactly(ALICE);
    }

    @Test
    void should_return_nothing_from_the_console_with_only_an_environment_activation() throws Exception {
        givenLookupActivatedFor(ORGANIZATION + ":" + ENVIRONMENT);
        givenConsoleRequestFrom(ORGANIZATION);

        assertThat(cut.search("ali")).isEmpty();
        verifyNoInteractions(ldapTemplate);
    }

    @Test
    void should_query_the_directory_from_the_console_and_the_portal_with_both_forms() throws Exception {
        givenLookupActivatedFor(ORGANIZATION, ORGANIZATION + ":" + ENVIRONMENT);
        givenDirectoryHolds(ALICE);

        givenConsoleRequestFrom(ORGANIZATION);
        assertThat(cut.search("ali")).containsExactly(ALICE);
        givenPortalRequestFrom(ORGANIZATION, ENVIRONMENT);
        assertThat(cut.search("ali")).containsExactly(ALICE);

        verify(ldapTemplate, times(2)).search(any(LdapQuery.class), ArgumentMatchers.<ContextMapper<User>>any());
    }

    @Test
    void should_ignore_blank_activations() throws Exception {
        givenLookupActivatedFor("", "  ");
        givenDirectoryHolds(ALICE);
        givenConsoleRequestFrom(OTHER_ORGANIZATION);

        assertThat(cut.search("ali")).containsExactly(ALICE);
    }

    @Test
    void should_ignore_the_spaces_around_an_activation() throws Exception {
        givenLookupActivatedFor(" " + ORGANIZATION + " ");
        givenDirectoryHolds(ALICE);
        givenConsoleRequestFrom(ORGANIZATION);

        assertThat(cut.search("ali")).containsExactly(ALICE);
    }

    @Test
    void should_read_a_scalar_activation() throws Exception {
        givenScalarLookupActivations(ORGANIZATION);
        givenDirectoryHolds(ALICE);

        givenConsoleRequestFrom(ORGANIZATION);
        assertThat(cut.search("ali")).containsExactly(ALICE);
        givenConsoleRequestFrom(OTHER_ORGANIZATION);
        assertThat(cut.search("ali")).isEmpty();
    }

    @Test
    void should_read_comma_separated_activations() throws Exception {
        givenScalarLookupActivations(ORGANIZATION + ", " + ORGANIZATION + ":" + ENVIRONMENT);
        givenDirectoryHolds(ALICE);

        givenConsoleRequestFrom(ORGANIZATION);
        assertThat(cut.search("ali")).containsExactly(ALICE);
        givenPortalRequestFrom(ORGANIZATION, ENVIRONMENT);
        assertThat(cut.search("ali")).containsExactly(ALICE);
        givenConsoleRequestFrom(OTHER_ORGANIZATION);
        assertThat(cut.search("ali")).isEmpty();
    }

    @Test
    void should_retrieve_a_user_by_reference_from_an_activated_organization() throws Exception {
        givenLookupActivatedFor(ORGANIZATION);
        givenDirectoryHolds(ALICE);
        givenConsoleRequestFrom(ORGANIZATION);

        assertThat(cut.retrieve(new IdentityReference("ldap", ALICE.getReference()))).isEqualTo(ALICE);
    }

    @Test
    void should_not_retrieve_a_user_by_reference_from_an_organization_that_is_not_activated() throws Exception {
        givenLookupActivatedFor(ORGANIZATION);
        givenConsoleRequestFrom(OTHER_ORGANIZATION);

        assertThat(cut.retrieve(new IdentityReference("ldap", ALICE.getReference()))).isNull();
        verifyNoInteractions(ldapTemplate);
    }

    @Test
    void should_retrieve_a_user_by_reference_from_an_environment_of_an_activated_organization() throws Exception {
        givenLookupActivatedFor(ORGANIZATION);
        givenDirectoryHolds(ALICE);
        givenPortalRequestFrom(ORGANIZATION, ENVIRONMENT);

        assertThat(cut.retrieve(new IdentityReference("ldap", ALICE.getReference()))).isEqualTo(ALICE);
    }

    @Test
    void should_not_retrieve_a_user_by_reference_from_an_environment_of_an_organization_that_is_not_activated() throws Exception {
        givenLookupActivatedFor(ORGANIZATION);
        givenPortalRequestFrom(OTHER_ORGANIZATION, ENVIRONMENT);

        assertThat(cut.retrieve(new IdentityReference("ldap", ALICE.getReference()))).isNull();
        verifyNoInteractions(ldapTemplate);
    }

    private void givenLookupWithoutActivations() throws Exception {
        cut.afterPropertiesSet();
    }

    private void givenLookupActivatedFor(String... activations) throws Exception {
        for (int i = 0; i < activations.length; i++) {
            environment.setProperty("lookup.activations[" + i + "]", activations[i]);
        }
        cut.afterPropertiesSet();
    }

    private void givenScalarLookupActivations(String value) throws Exception {
        environment.setProperty("lookup.activations", value);
        cut.afterPropertiesSet();
    }

    private void givenDirectoryHolds(User user) {
        lenient().when(ldapTemplate.search(any(LdapQuery.class), ArgumentMatchers.<ContextMapper<User>>any())).thenReturn(List.of(user));
        lenient()
            .when(ldapTemplate.lookup(eq(user.getReference()), any(String[].class), ArgumentMatchers.<ContextMapper<User>>any()))
            .thenReturn(user);
    }

    private static void givenConsoleRequestFrom(String organizationId) {
        GraviteeContext.setCurrentOrganization(organizationId);
        GraviteeContext.setCurrentEnvironment(null);
    }

    private static void givenPortalRequestFrom(String organizationId, String environmentId) {
        GraviteeContext.setCurrentOrganization(organizationId);
        GraviteeContext.setCurrentEnvironment(environmentId);
    }
}

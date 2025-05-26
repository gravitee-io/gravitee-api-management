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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import io.gravitee.common.data.domain.Page;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.*;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.model.*;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiPrimaryOwnerRemovalUpgraderTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private Appender<ILoggingEvent> appender;

    private ApiPrimaryOwnerRemovalUpgrader upgrader;

    private static final String ORG_ID = "org-test";
    private static final String API_PO_ROLE_ID = "po-role-test";
    private static final String DEFAULT_PRIMARY_OWNER_ID = "po-default-test";

    @Before
    public void setUp() throws Exception {
        upgrader =
            new ApiPrimaryOwnerRemovalUpgrader(
                roleRepository,
                apiRepository,
                membershipRepository,
                organizationRepository,
                environmentRepository,
                userRepository,
                groupRepository
            );

        Logger logger = (Logger) LoggerFactory.getLogger(ApiPrimaryOwnerRemovalUpgrader.class);
        logger.addAppender(appender);
    }

    @Test(expected = UpgraderException.class)
    public void upgrade_should_failed_because_of_exception() throws TechnicalException, UpgraderException {
        when(organizationRepository.findAll()).thenThrow(new RuntimeException());

        upgrader.upgrade();

        verify(organizationRepository, times(1)).findAll();
        verifyNoMoreInteractions(organizationRepository);
    }

    @Test
    public void shouldWarnAndStopWithPrimaryOwnerMissingAndNoConfigurationProperty() throws TechnicalException, UpgraderException {
        when(organizationRepository.findAll()).thenReturn(Set.of(organization()));

        when(roleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(any(), any(), any(), any()))
            .thenReturn(Optional.of(apiPoRole()));

        when(apiRepository.searchIds(anyList(), any(Pageable.class), any(Sortable.class)))
            .thenReturn(
                new Page<>(List.of("api-test"), 0, 1, 2),
                new Page<>(List.of("api-test-2"), 1, 1, 2),
                new Page<>(Collections.emptyList(), 2, 0, 2)
            );

        when(membershipRepository.findByReferencesAndRoleId(any(), any(), any())).thenReturn(Set.of());

        boolean success = upgrader.upgrade();

        assertThat(success).isFalse();

        verify(appender, times(1)).doAppend(argThat(event -> Level.WARN == event.getLevel() && "api-test".equals(event.getMessage())));
        verify(appender, times(1)).doAppend(argThat(event -> Level.WARN == event.getLevel() && "api-test-2".equals(event.getMessage())));
        verify(apiRepository, times(3)).searchIds(anyList(), any(Pageable.class), any(Sortable.class));
        verify(membershipRepository, never()).create(any());
    }

    @Test
    public void shouldFixWithUserWithPrimaryOwnerMissingAndConfigurationProperty() throws TechnicalException, UpgraderException {
        ReflectionTestUtils.setField(upgrader, "defaultPrimaryOwnerId", DEFAULT_PRIMARY_OWNER_ID);

        when(organizationRepository.findAll()).thenReturn(Set.of(organization()));

        when(roleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(any(), any(), any(), any()))
            .thenReturn(Optional.of(apiPoRole()));

        when(apiRepository.searchIds(anyList(), any(Pageable.class), any(Sortable.class)))
            .thenReturn(
                new Page<>(List.of("api-test"), 0, 1, 2),
                new Page<>(List.of("api-test-2"), 1, 1, 2),
                new Page<>(Collections.emptyList(), 2, 0, 2)
            );

        when(membershipRepository.findByReferencesAndRoleId(any(), any(), any())).thenReturn(Set.of());

        when(userRepository.findById(DEFAULT_PRIMARY_OWNER_ID)).thenReturn(Optional.of(user()));

        boolean success = upgrader.upgrade();

        assertThat(success).isTrue();

        verify(membershipRepository, times(2))
            .create(
                argThat(membership ->
                    membership.getMemberType() == MembershipMemberType.USER && membership.getRoleId().equals(API_PO_ROLE_ID)
                )
            );
    }

    @Test
    public void shouldFixWithGroupWithPrimaryOwnerMissingAndConfigurationProperty() throws TechnicalException, UpgraderException {
        ReflectionTestUtils.setField(upgrader, "defaultPrimaryOwnerId", DEFAULT_PRIMARY_OWNER_ID);

        when(organizationRepository.findAll()).thenReturn(Set.of(organization()));

        when(roleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(any(), any(), any(), any()))
            .thenReturn(Optional.of(apiPoRole()));

        when(apiRepository.searchIds(anyList(), any(Pageable.class), any(Sortable.class)))
            .thenReturn(new Page<>(List.of("api-test"), 0, 1, 1), new Page<>(Collections.emptyList(), 1, 0, 1));

        when(membershipRepository.findByReferencesAndRoleId(any(), any(), any())).thenReturn(Set.of());

        when(groupRepository.findById(DEFAULT_PRIMARY_OWNER_ID)).thenReturn(Optional.of(group()));

        boolean success = upgrader.upgrade();

        assertThat(success).isTrue();

        verify(membershipRepository, times(1))
            .create(
                argThat(membership ->
                    membership.getMemberType() == MembershipMemberType.GROUP && membership.getRoleId().equals(API_PO_ROLE_ID)
                )
            );
    }

    @Test
    public void shouldNotFixWithoutPrimaryOwnerMissing() throws TechnicalException, UpgraderException {
        when(organizationRepository.findAll()).thenReturn(Set.of(organization()));

        when(roleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(any(), any(), any(), any()))
            .thenReturn(Optional.of(apiPoRole()));

        when(apiRepository.searchIds(anyList(), any(Pageable.class), any(Sortable.class)))
            .thenReturn(
                new Page<>(List.of("api-test"), 0, 1, 2),
                new Page<>(List.of("api-test-2"), 1, 1, 2),
                new Page<>(Collections.emptyList(), 2, 0, 2)
            );

        when(membershipRepository.findByReferencesAndRoleId(any(), any(), any()))
            .thenReturn(Set.of(membership("api-test")), Set.of(membership("api-test-2")));

        boolean success = upgrader.upgrade();

        assertThat(success).isTrue();

        verify(appender, never()).doAppend(argThat(event -> Level.WARN == event.getLevel() && "api-test".equals(event.getMessage())));

        verify(membershipRepository, never()).create(any());
    }

    @Test
    public void test_order() {
        Assert.assertEquals(UpgraderOrder.API_PRIMARY_OWNER_REMOVAL_UPGRADER, upgrader.getOrder());
    }

    private static Organization organization() {
        Organization organization = new Organization();
        organization.setId(ORG_ID);
        return organization;
    }

    private static Role apiPoRole() {
        Role role = new Role();
        role.setId(API_PO_ROLE_ID);
        return role;
    }

    private static Membership membership(String referenceId) {
        Membership membership = new Membership();
        membership.setReferenceId(referenceId);
        return membership;
    }

    private static User user() {
        User user = new User();
        user.setId(DEFAULT_PRIMARY_OWNER_ID);
        return user;
    }

    private static Group group() {
        Group group = new Group();
        group.setId(DEFAULT_PRIMARY_OWNER_ID);
        return group;
    }
}

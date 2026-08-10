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
package io.gravitee.rest.api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.application_certificate.crud_service.ClientCertificateCrudService;
import io.gravitee.apim.core.application_certificate.model.ClientCertificate;
import io.gravitee.apim.core.application_certificate.model.ClientCertificateStatus;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.search.ApplicationCriteria;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.repository.management.model.ApplicationType;
import io.gravitee.rest.api.model.MembershipEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.application.ApplicationQuery;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationServiceImplTest {

    @InjectMocks
    private final ApplicationServiceImpl applicationService = new ApplicationServiceImpl();

    @Mock
    private MembershipService membershipService;

    @Mock
    private RoleService roleService;

    @Mock
    private UserService userService;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ClientCertificateCrudService clientCertificateCrudService;

    @Test
    public void findByIdsAndStatus_shouldNotFetchCertificatesPerApplication() throws TechnicalException {
        ExecutionContext executionContext = new ExecutionContext("org1", "env1");

        Application app1 = Application.builder().id("app1").status(ApplicationStatus.ACTIVE).type(ApplicationType.SIMPLE).build();
        Application app2 = Application.builder().id("app2").status(ApplicationStatus.ACTIVE).type(ApplicationType.SIMPLE).build();

        when(applicationRepository.search(any(ApplicationCriteria.class), isNull())).thenReturn(new Page<>(List.of(app1, app2), 0, 2, 2));

        RoleEntity primaryOwnerRole = new RoleEntity();
        primaryOwnerRole.setId("role1");
        when(roleService.findPrimaryOwnerRoleByOrganization("org1", RoleScope.APPLICATION)).thenReturn(primaryOwnerRole);

        MembershipEntity membership1 = MembershipEntity.builder().id("m1").referenceId("app1").memberId("user1").build();
        MembershipEntity membership2 = MembershipEntity.builder().id("m2").referenceId("app2").memberId("user2").build();
        when(
            membershipService.getMembershipsByReferencesAndRole(MembershipReferenceType.APPLICATION, List.of("app1", "app2"), "role1")
        ).thenReturn(Set.of(membership1, membership2));

        when(userService.findByIds(eq(executionContext), any(), eq(false))).thenReturn(Set.of());

        ClientCertificate app1Certificate = new ClientCertificate(
            "cert1",
            null,
            "app1",
            "cert-name",
            null,
            null,
            new Date(),
            null,
            "app1-certificate-pem",
            null,
            null,
            null,
            null,
            null,
            ClientCertificateStatus.ACTIVE
        );
        when(clientCertificateCrudService.findByApplicationIdsAndStatuses(any(), any(ClientCertificateStatus[].class))).thenReturn(
            List.of(app1Certificate)
        );

        Set<ApplicationListItem> result = applicationService.findByIdsAndStatus(executionContext, List.of("app1", "app2"), null);

        assertThat(result).hasSize(2);
        ApplicationListItem app1Item = result
            .stream()
            .filter(item -> "app1".equals(item.getId()))
            .findFirst()
            .orElseThrow();
        ApplicationListItem app2Item = result
            .stream()
            .filter(item -> "app2".equals(item.getId()))
            .findFirst()
            .orElseThrow();
        // Locks the actual contract, not just "no N+1": the batched certificate must still land on the right list item.
        assertThat(app1Item.getSettings().getTls().getClientCertificate()).isEqualTo("app1-certificate-pem");
        assertThat(app2Item.getSettings().getTls()).isNull();

        // any(ClientCertificateStatus[].class) matches the vararg's underlying array regardless of how many
        // statuses are passed, so a regression can't slip past never() just by changing call arity.
        verify(clientCertificateCrudService, never()).findByApplicationIdAndStatuses(anyString(), any(ClientCertificateStatus[].class));
        verify(clientCertificateCrudService).findByApplicationIdsAndStatuses(any(), any(ClientCertificateStatus[].class));
    }

    @Test
    public void buildSearchCriteria() {
        ExecutionContext executionContext = new ExecutionContext("org1", "env1");
        ApplicationQuery query = ApplicationQuery.builder()
            .query("app1")
            .name("name1")
            .groups(Set.of("group1", "group2"))
            .status("active")
            .user("user1")
            .ids(Set.of("app1", "app2"))
            .build();
        when(
            membershipService.getReferenceIdsByMemberAndReference(MembershipMemberType.USER, "user1", MembershipReferenceType.APPLICATION)
        ).thenReturn(Set.of("app1", "app2", "app3"));
        when(
            membershipService.getMembershipsByMemberAndReference(MembershipMemberType.USER, "user1", MembershipReferenceType.GROUP)
        ).thenReturn(Set.of());
        ApplicationCriteria criteria = applicationService.buildSearchCriteria(executionContext, query);

        assertAll(
            () -> assertThat(criteria.getEnvironmentIds().size()).isEqualTo(1),
            () -> assertTrue(criteria.getEnvironmentIds().contains("env1")),
            () -> assertThat(criteria.getRestrictedToIds().size()).isEqualTo(2),
            () -> assertTrue(criteria.getRestrictedToIds().containsAll(List.of("app1", "app2"))),
            () -> assertThat(criteria.getName()).isEqualTo("name1"),
            () -> assertThat(criteria.getStatus()).isEqualTo(ApplicationStatus.ACTIVE),
            () -> assertThat(criteria.getGroups().size()).isEqualTo(2),
            () -> assertThat(criteria.getGroups()).containsAll(List.of("group1", "group2")),
            () -> assertThat(criteria.getQuery()).isEqualTo("app1")
        );
    }

    @Test
    public void buildSearchCriteria_userAndIds() {
        ExecutionContext executionContext = new ExecutionContext("org1", "env1");
        ApplicationQuery query = ApplicationQuery.builder().user("user1").ids(Set.of("app1")).build();
        when(
            membershipService.getReferenceIdsByMemberAndReference(MembershipMemberType.USER, "user1", MembershipReferenceType.APPLICATION)
        ).thenReturn(Set.of("app1", "app2"));
        when(
            membershipService.getMembershipsByMemberAndReference(MembershipMemberType.USER, "user1", MembershipReferenceType.GROUP)
        ).thenReturn(Set.of());
        ApplicationCriteria criteria = applicationService.buildSearchCriteria(executionContext, query);
        assertThat(criteria.getEnvironmentIds().size()).isEqualTo(1);
        assertTrue(criteria.getEnvironmentIds().contains("env1"));
        assertThat(criteria.getRestrictedToIds().size()).isEqualTo(1);
        assertTrue(criteria.getRestrictedToIds().contains("app1"));
    }

    @Test
    public void buildSearchCriteria_userAndIds_noAuthorizedApps() {
        ExecutionContext executionContext = new ExecutionContext("org1", "env1");
        ApplicationQuery query = ApplicationQuery.builder().user("user1").ids(Set.of("app3")).build();
        when(
            membershipService.getReferenceIdsByMemberAndReference(MembershipMemberType.USER, "user1", MembershipReferenceType.APPLICATION)
        ).thenReturn(Set.of("app1", "app2"));
        when(
            membershipService.getMembershipsByMemberAndReference(MembershipMemberType.USER, "user1", MembershipReferenceType.GROUP)
        ).thenReturn(Set.of());
        ApplicationCriteria criteria = applicationService.buildSearchCriteria(executionContext, query);
        assertNull(criteria);
    }
}

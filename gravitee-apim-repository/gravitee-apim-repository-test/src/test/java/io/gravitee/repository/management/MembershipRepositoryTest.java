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
package io.gravitee.repository.management;

import static io.gravitee.repository.management.model.MembershipReferenceType.API;
import static io.gravitee.repository.utils.DateUtils.close;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.*;
import java.time.Instant;
import java.util.*;
import org.junit.Test;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MembershipRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/membership-tests/";
    }

    @Test
    public void shouldFindById() throws TechnicalException {
        Optional<Membership> membership = membershipRepository.findById("api1_user1");
        assertThat(membership).isPresent();

        Membership membership1 = membership.get();

        assertThat(membership1.getRoleId()).isEqualTo("API_OWNER");
        assertThat(membership1.getReferenceId()).isEqualTo("api1");
        assertThat(membership1.getMemberId()).isEqualTo("user1");
        assertThat(membership1.getReferenceType()).isEqualTo(API);
        assertThat(membership1.getSource()).isEqualTo("myIdp");

        assertThat(membership1.getUpdatedAt()).is(close("2015-08-08T08:20:10.883+00:00"));
        assertThat(membership1.getCreatedAt()).is(close("2015-08-08T08:20:10.883+00:00"));
    }

    @Test
    public void shouldNotFindById() throws TechnicalException {
        Optional<Membership> membership = membershipRepository.findById("api1");
        assertThat(membership).isEmpty();
    }

    @Test
    public void shouldFindAllApiMembers() throws TechnicalException {
        Set<Membership> memberships = membershipRepository.findByReferenceAndRoleId(MembershipReferenceType.API, "api1", null);
        assertThat(memberships).map(Membership::getMemberId).contains("user1");
    }

    @Test
    public void shouldFindAllApisMembers() throws TechnicalException {
        // When
        Set<Membership> memberships = membershipRepository.findByReferencesAndRoleId(
            MembershipReferenceType.API,
            List.of("api2", "api3"),
            null
        );
        // Then
        var membership1 = new Membership(
            "api2_user2",
            "user2",
            MembershipMemberType.USER,
            "api2",
            MembershipReferenceType.API,
            "API_OWNER"
        );
        membership1.setId("api2_user2");
        var membership2 = new Membership("api3_user3", "user3", MembershipMemberType.USER, "api3", MembershipReferenceType.API, "API_USER");
        membership2.setId("api3_user3");
        assertThat(memberships).containsOnly(membership1, membership2);
    }

    @Test
    public void shouldReturnEmptyListWithEmptyReferenceIdList() throws TechnicalException {
        Set<Membership> memberships = membershipRepository.findByReferencesAndRoleId(MembershipReferenceType.API, List.of(), null);
        assertThat(memberships).isEmpty();
    }

    @Test
    public void shouldFindApisOwners() throws TechnicalException {
        // When
        Set<Membership> memberships = membershipRepository.findByReferencesAndRoleId(
            MembershipReferenceType.API,
            List.of("api2", "api3"),
            "API_OWNER"
        );
        // Then
        Membership membership1 = new Membership(
            "api2_user2",
            "user2",
            MembershipMemberType.USER,
            "api2",
            MembershipReferenceType.API,
            "API_OWNER"
        );
        membership1.setId("api2_user2");
        assertThat(memberships).containsOnly(membership1);
    }

    @Test
    public void shouldFindByReferenceIdAndReferenceType() throws TechnicalException {
        // When
        List<Membership> memberships = membershipRepository.findByReferenceIdAndReferenceType("api1", MembershipReferenceType.API);
        // Then
        assertThat(memberships).containsOnly(
            new Membership("api1_user1", "user1", MembershipMemberType.USER, "api1", MembershipReferenceType.API, "API_OWNER")
        );
    }

    @Test
    public void shouldFindMembersApis() throws TechnicalException {
        Set<Membership> memberships = membershipRepository.findByMemberIdsAndMemberTypeAndReferenceType(
            List.of("user2", "user3"),
            MembershipMemberType.USER,
            MembershipReferenceType.API
        );
        Membership membership2 = new Membership(
            "api2_user2",
            "user2",
            MembershipMemberType.USER,
            "api2",
            MembershipReferenceType.API,
            "API_OWNER"
        );
        Membership membership3 = new Membership(
            "api3_user3",
            "user3",
            MembershipMemberType.USER,
            "api3",
            MembershipReferenceType.API,
            "API_OWNER"
        );
        assertThat(memberships).containsOnly(membership2, membership3);
    }

    @Test
    public void shouldFindApiOwner() throws TechnicalException {
        var memberships = membershipRepository.findByReferenceAndRoleId(MembershipReferenceType.API, "api1", "API_OWNER");
        assertThat(memberships).map(Membership::getMemberId).contains("user1");
    }

    @Test
    public void shouldFindByMemberIdAndMemberTypeAndReferenceType() throws TechnicalException {
        var memberships = membershipRepository.findByMemberIdAndMemberTypeAndReferenceType(
            "user1",
            MembershipMemberType.USER,
            MembershipReferenceType.API
        );
        assertThat(memberships).map(Membership::getReferenceId).contains("api1");
    }

    @Test
    public void shouldfindByMemberIdAndMemberTypeAndReferenceTypeAndSource() throws TechnicalException {
        var memberships = membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndSource(
            "user1",
            MembershipMemberType.USER,
            MembershipReferenceType.API,
            "myIdp"
        );
        assertThat(memberships).map(Membership::getReferenceId).contains("api1");
    }

    @Test
    public void shouldFindByMemberIdAndMemberTypeAndReferenceTypeAndRoleId() throws TechnicalException {
        var memberships = membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndRoleId(
            "user1",
            MembershipMemberType.USER,
            MembershipReferenceType.API,
            "API_OWNER"
        );
        assertThat(memberships).isNotEmpty();
        Membership membership = memberships.iterator().next();
        assertThat(membership.getReferenceId()).isEqualTo("api1");
        assertThat(membership.getMemberId()).isEqualTo("user1");
        assertThat(membership.getRoleId()).isEqualTo("API_OWNER");
    }

    @Test
    public void shouldFindByMemberIdAndMemberTypeAndReferenceTypeAndRoleIdIn() throws TechnicalException {
        var memberships = membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndRoleIdIn(
            "user1",
            MembershipMemberType.USER,
            MembershipReferenceType.API,
            Set.of("API_OWNER", "UNKNOWN_ROLE")
        );
        assertThat(memberships).isNotEmpty();
        Membership membership = memberships.iterator().next();
        assertThat(membership.getReferenceId()).isEqualTo("api1");
        assertThat(membership.getMemberId()).isEqualTo("user1");
        assertThat(membership.getRoleId()).isEqualTo("API_OWNER");
    }

    @Test
    public void shouldFindRefIdByMemberAndRefTypeAndRoleIdIn() throws TechnicalException {
        var referenceIds = membershipRepository.findRefIdByMemberAndRefTypeAndRoleIdIn(
            "user1",
            MembershipMemberType.USER,
            MembershipReferenceType.API,
            Set.of("API_OWNER", "UNKNOWN_ROLE")
        );
        assertThat(referenceIds).containsExactly("api1");
    }

    @Test
    public void shouldFindByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId() throws TechnicalException {
        var memberships = membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
            "user1",
            MembershipMemberType.USER,
            MembershipReferenceType.API,
            "api1"
        );
        assertThat(memberships).isNotEmpty();
        Membership membership = memberships.iterator().next();
        assertThat(membership.getReferenceId()).isEqualTo("api1");
        assertThat(membership.getMemberId()).isEqualTo("user1");
        assertThat(membership.getRoleId()).isEqualTo("API_OWNER");
    }

    @Test
    public void shouldFindByMemberIdAndMemberTypeAndReferenceTypeAndReferenceIdAndRoleId() throws TechnicalException {
        var memberships = membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceIdAndRoleId(
            "user1",
            MembershipMemberType.USER,
            MembershipReferenceType.API,
            "api1",
            "API_OWNER"
        );
        assertThat(memberships).isNotEmpty();
        Membership membership = memberships.iterator().next();
        assertThat(membership.getReferenceId()).isEqualTo("api1");
        assertThat(membership.getMemberId()).isEqualTo("user1");
        assertThat(membership.getRoleId()).isEqualTo("API_OWNER");
    }

    @Test
    public void shouldFindMembershipWithNullReferenceId() throws TechnicalException {
        var memberships = membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
            "group1",
            MembershipMemberType.GROUP,
            MembershipReferenceType.API,
            null
        );
        assertThat(memberships).isNotEmpty();
        Membership membership = memberships.iterator().next();
        assertThat(membership.getReferenceId()).isNull();
        assertThat(membership.getMemberId()).isEqualTo("group1");
        assertThat(membership.getRoleId()).isEqualTo("11baec92-8823-4f8b-baec-9288238f8b5c");
    }

    @Test
    public void shouldFindByRoleId() throws TechnicalException {
        var memberships = membershipRepository.findByRoleId("APPLICATION_USER");

        assertThat(memberships).hasSize(2);
    }

    @Test
    public void shouldDelete() throws TechnicalException {
        membershipRepository.delete("app1_userToDelete");

        var optional = membershipRepository.findById("app1_userToDelete");

        assertThat(optional).isEmpty();
    }

    @Test
    public void shouldUpdate() throws TechnicalException {
        Membership membership = new Membership(
            "app1_userToDelete",
            "userToUpdate",
            MembershipMemberType.USER,
            "app1",
            MembershipReferenceType.APPLICATION,
            "APPLICATION_USER"
        );
        membership.setId("app1_userToUpdate");
        membership.setCreatedAt(Date.from(Instant.parse("2001-09-09T01:46:40Z")));

        // When
        Membership update = membershipRepository.update(membership);

        assertThat(update).isNotNull();
        assertThat(update.getCreatedAt()).is(close("2001-09-09T01:46:40Z"));
    }

    @Test
    public void shouldFindByIds() throws TechnicalException {
        Set<Membership> memberships = membershipRepository.findByIds(Set.of("api1_user_findByIds", "api2_user_findByIds", "unknown"));

        assertThat(memberships).hasSize(2).map(Membership::getId).contains("api1_user_findByIds", "api2_user_findByIds");
    }

    @Test
    public void shouldFindByMemberIdAndMemberType() throws TechnicalException {
        Set<Membership> memberships = membershipRepository.findByMemberIdAndMemberType("user_findByIds", MembershipMemberType.USER);

        assertThat(memberships).hasSize(2).map(Membership::getId).contains("api1_user_findByIds", "api2_user_findByIds");
    }

    public void shouldNotUpdateUnknownMembership() {
        Membership unknownMembership = new Membership();
        unknownMembership.setId("unknown");
        unknownMembership.setMemberId("unknown");
        unknownMembership.setReferenceId("unknown");
        unknownMembership.setReferenceType(MembershipReferenceType.ENVIRONMENT);
        Throwable throwable = catchThrowable(() -> membershipRepository.update(unknownMembership));
        assertThat(throwable).isInstanceOf(IllegalStateException.class);
    }

    public void shouldNotUpdateNull() throws Exception {
        membershipRepository.update(null);
        Throwable throwable = catchThrowable(() -> membershipRepository.update(null));
        assertThat(throwable).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void shouldDeleteMembers() throws TechnicalException {
        int nbBeforeDeletion = membershipRepository.findByReferenceAndRoleId(API, "api_deleteRef", null).size();

        List<String> deleted = membershipRepository.deleteByReferenceIdAndReferenceType("api_deleteRef", API);

        int nbAfterDeletion = membershipRepository.findByReferenceAndRoleId(API, "api_deleteRef", null).size();

        assertThat(nbBeforeDeletion).isEqualTo(2);
        assertThat(nbAfterDeletion).isZero();
        assertThat(deleted).hasSize(2);
    }
}

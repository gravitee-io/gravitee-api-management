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
package io.gravitee.repository;

import io.gravitee.repository.config.AbstractRepositoryTest;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
public class MembershipRepositoryTest extends AbstractRepositoryTest {
    @Override
    protected String getTestCasesPath() {
        return "/data/membership-tests/";
    }

    @Test
    public void shouldFindById() throws TechnicalException {
        Optional<Membership> membership = membershipRepository.findById("user1", MembershipReferenceType.API, "api1");
        assertTrue("There is a membership", membership.isPresent());
        assertTrue( membership.get().getRoles().containsKey(RoleScope.API.getId()));
        assertEquals("OWNER", membership.get().getRoles().get(RoleScope.API.getId()));
    }

    @Test
    public void shouldNotFindById() throws TechnicalException {
        Optional<Membership> membership = membershipRepository.findById(null, MembershipReferenceType.API, "api1");
        assertFalse(membership.isPresent());
    }

    @Test
    public void shouldFindAllApiMembers() throws TechnicalException {
        Set<Membership> memberships = membershipRepository.findByReferenceAndRole(MembershipReferenceType.API, "api1", null, null);
        assertNotNull("result must not be null", memberships);
        assertTrue(!memberships.isEmpty());
        assertEquals("user1", memberships.iterator().next().getUserId());
    }

    @Test
    public void shouldFindAllApisMembers() throws TechnicalException {
        Set<Membership> memberships = membershipRepository.findByReferencesAndRole(MembershipReferenceType.API, Arrays.asList("api2", "api3"), null,  null);
        assertNotNull("result must not be null", memberships);
        assertEquals(2, memberships.size());
        Membership membership1 = new Membership("user2", "api2", MembershipReferenceType.API);
        membership1.setRoles(Collections.singletonMap(3, "OWNER"));
        Membership membership2 = new Membership("user3", "api3", MembershipReferenceType.API);
        membership2.setRoles(Collections.singletonMap(3, "OWNER"));
        Set<Membership> expectedResult = new HashSet<>(Arrays.asList(membership1, membership2));
        assertTrue("must contain api2 and api3", memberships.containsAll(expectedResult));
    }

    @Test
    public void shouldFindApisOwners() throws TechnicalException {
        Set<Membership> memberships = membershipRepository.findByReferencesAndRole(MembershipReferenceType.API, Arrays.asList("api2", "api3"), RoleScope.API, "OWNER");
        assertNotNull("result must not be null", memberships);
        assertEquals(1, memberships.size());
        Membership membership1 = new Membership("user2", "api2", MembershipReferenceType.API);
        membership1.setRoles(Collections.singletonMap(3, "OWNER"));
        Set<Membership> expectedResult = new HashSet<>(Collections.singletonList(membership1));
        assertTrue("must contain api2 and api3", memberships.containsAll(expectedResult));
    }

    @Test
    public void shouldFindApiOwner() throws TechnicalException {
        Set<Membership> memberships = membershipRepository.findByReferenceAndRole(MembershipReferenceType.API, "api1", RoleScope.API, "OWNER");
        assertNotNull("result must not be null", memberships);
        assertTrue(!memberships.isEmpty());
        assertEquals("user1", memberships.iterator().next().getUserId());
    }

    @Test
    public void shouldFindByUserAndReferenceType() throws TechnicalException {
        Set<Membership> memberships = membershipRepository.findByUserAndReferenceType("user1", MembershipReferenceType.API);
        assertNotNull("result must not be null", memberships);
        assertTrue(!memberships.isEmpty());
        assertEquals("api1", memberships.iterator().next().getReferenceId());
    }

    @Test
    public void shouldFindByUserAndReferenceTypeAndRole() throws TechnicalException {
        Set<Membership> memberships = membershipRepository.findByUserAndReferenceTypeAndRole("user1", MembershipReferenceType.API, RoleScope.API, "OWNER");
        assertNotNull("result must not be null", memberships);
        assertTrue(!memberships.isEmpty());
        Membership membership = memberships.iterator().next();
        assertTrue( membership.getRoles().containsKey(RoleScope.API.getId()));
        assertEquals("OWNER", membership.getRoles().get(RoleScope.API.getId()));
        assertEquals("api1", membership.getReferenceId());
        assertEquals("user1", membership.getUserId());
    }

    @Test
    public void shouldDelete() throws TechnicalException {
        Membership membership = new Membership("userToDelete", "app1", MembershipReferenceType.APPLICATION);

        membershipRepository.delete(membership);

        Optional<Membership> optional = membershipRepository.findById("userToDelete", MembershipReferenceType.APPLICATION, "app1");
        assertFalse("There is no membership", optional.isPresent());
    }

    @Test
    public void shouldUpdate() throws TechnicalException {
        Membership membership = new Membership("userToUpdate", "app1", MembershipReferenceType.APPLICATION);
        membership.setCreatedAt(new Date(0));

        Membership update = membershipRepository.update(membership);

        assertNotNull(update);
        assertEquals(new Date(0), update.getCreatedAt());
    }

    @Test
    public void shouldFindByIds() throws TechnicalException {
        Set<Membership> memberships = membershipRepository.findByIds(
                "user_findByIds",
                MembershipReferenceType.API,
                new HashSet<>(Arrays.asList("api1_findByIds", "api2_findByIds", "unknown")));

        assertNotNull(memberships);
        assertFalse(memberships.isEmpty());
        assertEquals(2, memberships.size());
        assertTrue(memberships.
                stream().
                map(Membership::getReferenceId).
                collect(Collectors.toList()).
                containsAll(Arrays.asList("api1_findByIds", "api2_findByIds")));
    }
}

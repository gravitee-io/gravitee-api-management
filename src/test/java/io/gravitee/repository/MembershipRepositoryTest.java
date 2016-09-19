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
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

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
        Assert.assertTrue("There is a membership", membership.isPresent());
        Assert.assertEquals("OWNER", membership.get().getType());
    }

    @Test
    public void shouldFindAllApiMembers() throws TechnicalException {
        Set<Membership> memberships = membershipRepository.findByReferenceAndMembershipType(MembershipReferenceType.API, "api1", null);
        Assert.assertNotNull("result must not be null", memberships);
        Assert.assertTrue(!memberships.isEmpty());
        Assert.assertEquals("user1", memberships.iterator().next().getUserId());
    }

    @Test
    public void shouldFindAllApisMembers() throws TechnicalException {
        Set<Membership> memberships = membershipRepository.findByReferencesAndMembershipType(MembershipReferenceType.API, Arrays.asList("api2", "api3"), null);
        Assert.assertNotNull("result must not be null", memberships);
        Assert.assertEquals(2, memberships.size());
        Membership membership1 = new Membership("user2", "api2", MembershipReferenceType.API);
        membership1.setType("OWNER");
        Membership membership2 = new Membership("user3", "api3", MembershipReferenceType.API);
        membership2.setType("USER");
        Set<Membership> expectedResult = new HashSet<>(Arrays.asList(membership1, membership2));
        Assert.assertTrue("must contain api2 and api3", memberships.containsAll(expectedResult));
    }

    @Test
    public void shouldFindApisOwners() throws TechnicalException {
        Set<Membership> memberships = membershipRepository.findByReferencesAndMembershipType(MembershipReferenceType.API, Arrays.asList("api2", "api3"), "OWNER");
        Assert.assertNotNull("result must not be null", memberships);
        Assert.assertEquals(1, memberships.size());
        Membership membership1 = new Membership("user2", "api2", MembershipReferenceType.API);
        membership1.setType("OWNER");
        Set<Membership> expectedResult = new HashSet<>(Collections.singletonList(membership1));
        Assert.assertTrue("must contain api2 and api3", memberships.containsAll(expectedResult));
    }

    @Test
    public void shouldFindApiOwner() throws TechnicalException {
        Set<Membership> memberships = membershipRepository.findByReferenceAndMembershipType(MembershipReferenceType.API, "api1", "OWNER");
        Assert.assertNotNull("result must not be null", memberships);
        Assert.assertTrue(!memberships.isEmpty());
        Assert.assertEquals("user1", memberships.iterator().next().getUserId());
    }

    @Test
    public void shouldFindByUserAndReferenceType() throws TechnicalException {
        Set<Membership> memberships = membershipRepository.findByUserAndReferenceType("user1", MembershipReferenceType.API);
        Assert.assertNotNull("result must not be null", memberships);
        Assert.assertTrue(!memberships.isEmpty());
        Assert.assertEquals("api1", memberships.iterator().next().getReferenceId());
    }

    @Test
    public void shouldFindByUserAndReferenceTypeAndMembershipType() throws TechnicalException {
        Set<Membership> memberships = membershipRepository.findByUserAndReferenceTypeAndMembershipType("user1", MembershipReferenceType.API, "OWNER");
        Assert.assertNotNull("result must not be null", memberships);
        Assert.assertTrue(!memberships.isEmpty());
        Membership membership = memberships.iterator().next();
        Assert.assertEquals("OWNER", membership.getType());
        Assert.assertEquals("api1", membership.getReferenceId());
        Assert.assertEquals("user1", membership.getUserId());
    }
}

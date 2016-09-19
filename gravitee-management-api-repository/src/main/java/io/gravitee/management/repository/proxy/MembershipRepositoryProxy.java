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
package io.gravitee.management.repository.proxy;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipReferenceType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
@Component
public class MembershipRepositoryProxy extends AbstractProxy<MembershipRepository> implements MembershipRepository {
    @Override
    public Membership create(Membership membership) throws TechnicalException {
        return target.create(membership);
    }

    @Override
    public Membership update(Membership membership) throws TechnicalException {
        return target.update(membership);
    }

    @Override
    public void delete(Membership membership) throws TechnicalException {
        target.delete(membership);
    }

    @Override
    public Optional<Membership> findById(String userId, MembershipReferenceType referenceType, String referenceId) throws TechnicalException {
        return target.findById(userId, referenceType, referenceId);
    }

    @Override
    public Set<Membership> findByReferenceAndMembershipType(MembershipReferenceType referenceType, String referenceId, String membershipType) throws TechnicalException {
        return target.findByReferenceAndMembershipType(referenceType, referenceId, membershipType);
    }

    @Override
    public Set<Membership> findByUserAndReferenceType(String userId, MembershipReferenceType referenceType) throws TechnicalException {
        return target.findByUserAndReferenceType(userId, referenceType);
    }

    @Override
    public Set<Membership> findByUserAndReferenceTypeAndMembershipType(String userId, MembershipReferenceType referenceType, String membershipType) throws TechnicalException {
        return target.findByUserAndReferenceTypeAndMembershipType(userId, referenceType, membershipType);
    }

    @Override
    public Set<Membership> findByReferencesAndMembershipType(MembershipReferenceType referenceType, List<String> referenceIds, String membershipType) throws TechnicalException {
        return target.findByReferencesAndMembershipType(referenceType, referenceIds, membershipType);
    }
}

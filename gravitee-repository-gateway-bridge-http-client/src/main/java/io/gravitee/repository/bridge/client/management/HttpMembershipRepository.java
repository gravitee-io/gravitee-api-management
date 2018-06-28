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
package io.gravitee.repository.bridge.client.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class HttpMembershipRepository extends AbstractRepository implements MembershipRepository {

    @Override
    public Membership create(Membership membership) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Membership update(Membership membership) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public void delete(Membership membership) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Optional<Membership> findById(String userId, MembershipReferenceType referenceType, String referenceId) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Set<Membership> findByIds(String userId, MembershipReferenceType referenceType, Set<String> referenceIds) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Set<Membership> findByReferenceAndRole(MembershipReferenceType referenceType, String referenceId, RoleScope roleScope, String roleName) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Set<Membership> findByReferencesAndRole(MembershipReferenceType referenceType, List<String> referenceIds, RoleScope roleScope, String roleName) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Set<Membership> findByUserAndReferenceType(String userId, MembershipReferenceType referenceType) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Set<Membership> findByRole(RoleScope roleScope, String roleName) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Set<Membership> findByUserAndReferenceTypeAndRole(String userId, MembershipReferenceType referenceType, RoleScope roleScope, String roleName) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Set<Membership> findByUser(String userId) throws TechnicalException {
        throw new IllegalStateException();
    }
}

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
package inmemory;

import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.query_service.MembershipQueryService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MembershipQueryServiceInMemory implements MembershipQueryService, InMemoryAlternative<Membership> {

    final ArrayList<Membership> storage;

    public MembershipQueryServiceInMemory() {
        storage = new ArrayList<>();
    }

    public MembershipQueryServiceInMemory(MembershipCrudServiceInMemory membershipCrudServiceInMemory) {
        this.storage = membershipCrudServiceInMemory.storage;
    }

    @Override
    public Collection<Membership> findByReferenceAndRoleId(Membership.ReferenceType referenceType, String referenceId, String roleId) {
        return storage
            .stream()
            .filter(
                membership ->
                    membership.getReferenceType().equals(referenceType) &&
                    membership.getReferenceId().equals(referenceId) &&
                    membership.getRoleId().equals(roleId)
            )
            .toList();
    }

    @Override
    public Collection<Membership> findByReferencesAndRoleId(
        Membership.ReferenceType referenceType,
        List<String> referenceIds,
        String roleId
    ) {
        return storage
            .stream()
            .filter(
                membership ->
                    membership.getReferenceType().equals(referenceType) &&
                    referenceIds.contains(membership.getReferenceId()) &&
                    membership.getRoleId().equals(roleId)
            )
            .toList();
    }

    @Override
    public Collection<Membership> findByReference(Membership.ReferenceType referenceType, String referenceId) {
        return storage
            .stream()
            .filter(membership -> membership.getReferenceType().equals(referenceType) && membership.getReferenceId().equals(referenceId))
            .toList();
    }

    @Override
    public Collection<Membership> findGroupsThatUserBelongsTo(String userId) {
        return storage
            .stream()
            .filter(membership -> membership.isGroupUser() && membership.getMemberId().equals(userId))
            .toList();
    }

    @Override
    public List<String> findClustersIdsThatUserBelongsTo(String userId) {
        return storage
            .stream()
            .filter(membership -> membership.getMemberType() == Membership.Type.USER)
            .filter(membership -> membership.getReferenceType() == Membership.ReferenceType.CLUSTER)
            .filter(membership -> membership.getMemberId().equals(userId))
            .map(Membership::getReferenceId)
            .toList();
    }

    @Override
    public void initWith(List<Membership> items) {
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<Membership> storage() {
        return Collections.unmodifiableList(storage);
    }
}

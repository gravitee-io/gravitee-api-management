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

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.InvitationRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.model.Invitation;
import io.gravitee.repository.management.model.InvitationReferenceType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class InvitationRepositoryInMemory implements InvitationRepository {

    private final Map<String, Invitation> storage = new LinkedHashMap<>();
    private TechnicalException failure;

    public void initWith(List<Invitation> invitations) {
        invitations.forEach(i -> storage.put(i.getId(), i));
    }

    public void failsWith(TechnicalException ex) {
        this.failure = ex;
    }

    public void reset() {
        storage.clear();
        failure = null;
    }

    public Map<String, Invitation> storage() {
        return storage;
    }

    private void maybeThrow() throws TechnicalException {
        if (failure != null) throw failure;
    }

    @Override
    public Optional<Invitation> findById(String id) throws TechnicalException {
        maybeThrow();
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Invitation create(Invitation invitation) throws TechnicalException {
        maybeThrow();
        storage.put(invitation.getId(), invitation);
        return invitation;
    }

    @Override
    public Invitation update(Invitation invitation) throws TechnicalException {
        maybeThrow();
        storage.put(invitation.getId(), invitation);
        return invitation;
    }

    @Override
    public void delete(String id) throws TechnicalException {
        maybeThrow();
        storage.remove(id);
    }

    @Override
    public Set<Invitation> findAll() throws TechnicalException {
        maybeThrow();
        return Set.copyOf(storage.values());
    }

    @Override
    public List<Invitation> findByEmail(String email) throws TechnicalException {
        maybeThrow();
        return storage
            .values()
            .stream()
            .filter(i -> email.equals(i.getEmail()))
            .toList();
    }

    @Override
    public List<Invitation> findByReferenceIdAndReferenceType(String referenceId, InvitationReferenceType referenceType)
        throws TechnicalException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Page<Invitation> search(InvitationCriteria criteria, Sortable sortable, Pageable pageable) throws TechnicalException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> deleteByReferenceIdAndReferenceType(String referenceId, InvitationReferenceType referenceType)
        throws TechnicalException {
        throw new UnsupportedOperationException();
    }
}

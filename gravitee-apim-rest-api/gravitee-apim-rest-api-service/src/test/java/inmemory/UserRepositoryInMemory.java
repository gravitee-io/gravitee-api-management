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
import io.gravitee.repository.management.api.UserRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.UserCriteria;
import io.gravitee.repository.management.model.User;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class UserRepositoryInMemory implements UserRepository {

    private final Map<String, User> storage = new LinkedHashMap<>();
    private TechnicalException failure;

    public void initWith(List<User> users) {
        users.forEach(u -> storage.put(u.getId(), u));
    }

    public void failsWith(TechnicalException ex) {
        this.failure = ex;
    }

    public void reset() {
        storage.clear();
        failure = null;
    }

    public Map<String, User> storage() {
        return storage;
    }

    private void maybeThrow() throws TechnicalException {
        if (failure != null) throw failure;
    }

    @Override
    public Optional<User> findById(String id) throws TechnicalException {
        maybeThrow();
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public User create(User user) throws TechnicalException {
        maybeThrow();
        storage.put(user.getId(), user);
        return user;
    }

    @Override
    public User update(User user) throws TechnicalException {
        maybeThrow();
        storage.put(user.getId(), user);
        return user;
    }

    @Override
    public void delete(String id) throws TechnicalException {
        storage.remove(id);
    }

    @Override
    public Optional<User> findBySource(String source, String sourceId, String organizationId) throws TechnicalException {
        return storage
            .values()
            .stream()
            .filter(
                u ->
                    Objects.equals(u.getSource(), source) &&
                    Objects.equals(u.getSourceId(), sourceId) &&
                    Objects.equals(u.getOrganizationId(), organizationId)
            )
            .findFirst();
    }

    @Override
    public List<User> findByEmail(String email, String organizationId) throws TechnicalException {
        maybeThrow();
        return storage
            .values()
            .stream()
            .filter(u -> u.getEmail() != null && u.getEmail().equalsIgnoreCase(email))
            .filter(u -> Objects.equals(u.getOrganizationId(), organizationId))
            .toList();
    }

    @Override
    public Set<User> findByIds(Collection<String> ids) throws TechnicalException {
        maybeThrow();
        return ids.stream().map(storage::get).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    @Override
    public Page<User> search(UserCriteria criteria, Pageable pageable) throws TechnicalException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> deleteByOrganizationId(String organizationId) throws TechnicalException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<User> findAll() throws TechnicalException {
        return Set.copyOf(storage.values());
    }
}

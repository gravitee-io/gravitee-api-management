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
package io.gravitee.gamma.rest.core.guardian.inmemory;

import io.gravitee.common.data.domain.Page;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import io.gravitee.gamma.rest.core.guardian.model.GuardianSearchQuery;
import io.gravitee.gamma.rest.core.guardian.port.repository.GuardianRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * In-memory {@link GuardianRepository} for domain tests.
 *
 * <p>Scopes every read on the environment and answers empty the way the real adapter does, so
 * use-case tests exercise the environment-isolation contract rather than mocking it away. Search
 * applies the name filter, the sort and the paging for the same reason.
 *
 * @author GraviteeSource Team
 */
public class InMemoryGuardianRepository implements GuardianRepository {

    private final List<Guardian> guardians = new ArrayList<>();

    public void givenGuardian(Guardian guardian) {
        guardians.add(guardian);
    }

    public List<Guardian> storedGuardians() {
        return List.copyOf(guardians);
    }

    public void reset() {
        guardians.clear();
    }

    @Override
    public Guardian create(Guardian guardian) {
        guardians.add(guardian);
        return guardian;
    }

    @Override
    public Optional<Guardian> findByEnvironmentId(String environmentId, String guardianId) {
        return guardians
            .stream()
            .filter(g -> Objects.equals(g.getId(), guardianId) && Objects.equals(g.getEnvironmentId(), environmentId))
            .findFirst();
    }

    @Override
    public List<Guardian> findByEnvironmentIdAndName(String environmentId, String name) {
        // Both real implementations require a name, so this one does too: a double that tolerated null
        // would answer empty where the store throws, and hide the caller that passes one.
        Objects.requireNonNull(name, "Name must not be null");
        return guardians
            .stream()
            .filter(g -> Objects.equals(g.getEnvironmentId(), environmentId) && name.equalsIgnoreCase(g.getName()))
            .toList();
    }

    @Override
    public void updateUpdatedAt(String environmentId, String guardianId, Instant updatedAt) {
        guardians.replaceAll(guardian -> {
            if (
                Objects.equals(environmentId, guardian.getEnvironmentId()) &&
                Objects.equals(guardianId, guardian.getId()) &&
                (guardian.getUpdatedAt() == null || guardian.getUpdatedAt().isBefore(updatedAt))
            ) {
                return guardian.toBuilder().updatedAt(updatedAt).build();
            }
            return guardian;
        });
    }

    @Override
    public Guardian update(Guardian guardian) {
        guardians.removeIf(g -> Objects.equals(g.getId(), guardian.getId()));
        guardians.add(guardian);
        return guardian;
    }

    @Override
    public void delete(String guardianId) {
        guardians.removeIf(g -> Objects.equals(g.getId(), guardianId));
    }

    @Override
    public Page<Guardian> search(GuardianSearchQuery query) {
        var matching = guardians
            .stream()
            .filter(g -> Objects.equals(g.getEnvironmentId(), query.environmentId()))
            .filter(g -> matchesName(g, query.query()))
            .sorted(comparator(query.sortBy()))
            .toList();

        var from = Math.min((query.pageNumber() - 1) * query.pageSize(), matching.size());
        var to = Math.min(from + query.pageSize(), matching.size());
        var content = matching.subList(from, to);

        return new Page<>(content, query.pageNumber(), content.size(), matching.size());
    }

    private static boolean matchesName(Guardian guardian, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        return guardian.getName() != null && guardian.getName().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
    }

    private static Comparator<Guardian> comparator(String sortBy) {
        var ascending = sortBy == null || !sortBy.startsWith("-");
        var field = sortBy == null ? "updatedAt" : (ascending ? sortBy : sortBy.substring(1));

        // Every field SearchGuardianUseCase accepts has a case here. A missing one used to fall
        // through to updatedAt, which would let a test asserting that order pass against this double
        // while the real backends ordered by another column.
        Comparator<Guardian> comparator = switch (field) {
            case "name" -> Comparator.comparing(Guardian::getName, Comparator.nullsLast(Comparator.naturalOrder()));
            case "phase" -> Comparator.comparing(
                g -> g.getPhase() == null ? null : g.getPhase().name(),
                Comparator.nullsLast(Comparator.naturalOrder())
            );
            case "createdAt" -> Comparator.comparing(Guardian::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "updatedAt" -> Comparator.comparing(Guardian::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "deployedAt" -> Comparator.comparing(Guardian::getDeployedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> throw new IllegalArgumentException("This double does not sort on '" + field + "'");
        };
        return ascending ? comparator : comparator.reversed();
    }
}

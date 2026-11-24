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
package io.gravitee.gamma.rest.core.guardrail.inmemory;

import io.gravitee.common.data.domain.Page;
import io.gravitee.gamma.rest.core.guardrail.model.Guardrail;
import io.gravitee.gamma.rest.core.guardrail.model.GuardrailSearchQuery;
import io.gravitee.gamma.rest.core.guardrail.port.repository.GuardrailRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * In-memory {@link GuardrailRepository} for domain tests.
 *
 * <p>Scopes every read on the environment and answers empty the way the real adapter does, so
 * use-case tests exercise the environment-isolation contract rather than mocking it away. Search
 * applies the name filter, the sort and the paging for the same reason, and answers the order the
 * Mongo and the JDBC backend answer: {@code createdAt} ascending when the query carries no sort, and
 * the id as the tie-breaker in every case.
 *
 * @author GraviteeSource Team
 */
public class InMemoryGuardrailRepository implements GuardrailRepository {

    private final List<Guardrail> guardrails = new ArrayList<>();
    private UnaryOperator<Guardrail> otherWrite;
    private int otherWritesLeft;

    public void givenGuardrail(Guardrail guardrail) {
        guardrails.add(guardrail);
    }

    public List<Guardrail> storedGuardrails() {
        return List.copyOf(guardrails);
    }

    public void reset() {
        guardrails.clear();
        otherWrite = null;
    }

    @Override
    public Guardrail create(Guardrail guardrail) {
        guardrails.add(guardrail);
        return guardrail;
    }

    @Override
    public Optional<Guardrail> findByEnvironmentId(String environmentId, String guardrailId) {
        return guardrails
            .stream()
            .filter(g -> Objects.equals(g.getId(), guardrailId) && Objects.equals(g.getEnvironmentId(), environmentId))
            .findFirst();
    }

    @Override
    public List<Guardrail> findByEnvironmentIdAndIds(String environmentId, Set<String> guardrailIds) {
        Objects.requireNonNull(environmentId, "EnvironmentId must not be null");
        var ids = Set.copyOf(guardrailIds);
        return guardrails
            .stream()
            .filter(g -> environmentId.equals(g.getEnvironmentId()) && ids.contains(g.getId()))
            .toList();
    }

    @Override
    public List<Guardrail> findByEnvironmentIdAndName(String environmentId, String name) {
        // Both real implementations require a name, so this one does too: a double that tolerated null
        // would answer empty where the store throws, and hide the caller that passes one.
        Objects.requireNonNull(name, "Name must not be null");
        return guardrails
            .stream()
            .filter(g -> Objects.equals(g.getEnvironmentId(), environmentId) && name.equalsIgnoreCase(g.getName()))
            .toList();
    }

    @Override
    public void updateUpdatedAt(String environmentId, String guardrailId, Instant updatedAt) {
        guardrails.replaceAll(guardrail -> {
            if (
                Objects.equals(environmentId, guardrail.getEnvironmentId()) &&
                Objects.equals(guardrailId, guardrail.getId()) &&
                (guardrail.getUpdatedAt() == null || guardrail.getUpdatedAt().isBefore(updatedAt))
            ) {
                return guardrail.toBuilder().updatedAt(updatedAt).build();
            }
            return guardrail;
        });
    }

    @Override
    public Guardrail update(Guardrail guardrail) {
        guardrails.removeIf(g -> Objects.equals(g.getId(), guardrail.getId()));
        guardrails.add(guardrail);
        return guardrail;
    }

    /**
     * Has another writer change the stored Guardrail just before the next conditional write reaches the
     * store — the window between a use case reading the draft and writing it back. A writer answering
     * {@code null} deletes it.
     */
    public void givenAnotherWriterGetsInFirst(UnaryOperator<Guardrail> otherWrite) {
        givenAnotherWriterGetsInFirst(1, otherWrite);
    }

    /** The same, before each of the next {@code times} conditional writes. */
    public void givenAnotherWriterGetsInFirst(int times, UnaryOperator<Guardrail> otherWrite) {
        this.otherWrite = otherWrite;
        this.otherWritesLeft = times;
    }

    /** The same, before every conditional write: a Guardrail written faster than it can be overwritten. */
    public void givenAnotherWriterAlwaysGetsInFirst(UnaryOperator<Guardrail> otherWrite) {
        givenAnotherWriterGetsInFirst(Integer.MAX_VALUE, otherWrite);
    }

    @Override
    public Optional<Guardrail> updateIfVersionMatches(Guardrail guardrail, int expectedVersion) {
        if (otherWrite != null && otherWritesLeft > 0) {
            otherWritesLeft--;
            guardrails.replaceAll(g -> Objects.equals(g.getId(), guardrail.getId()) ? otherWrite.apply(g) : g);
            guardrails.removeIf(Objects::isNull);
        }
        var stored = guardrails
            .stream()
            .filter(g -> Objects.equals(g.getId(), guardrail.getId()))
            .findFirst();
        if (stored.isEmpty() || !Objects.equals(stored.get().getVersion(), expectedVersion)) {
            return Optional.empty();
        }
        return Optional.of(update(guardrail));
    }

    @Override
    public void delete(String guardrailId) {
        guardrails.removeIf(g -> Objects.equals(g.getId(), guardrailId));
    }

    @Override
    public Page<Guardrail> search(GuardrailSearchQuery query) {
        var matching = guardrails
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

    private static boolean matchesName(Guardrail guardrail, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        return guardrail.getName() != null && guardrail.getName().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
    }

    private static Comparator<Guardrail> comparator(String sortBy) {
        var ascending = sortBy == null || !sortBy.startsWith("-");
        // No sort asked for: both real backends order by created_at ascending, so this one does too.
        var field = sortBy == null ? "createdAt" : (ascending ? sortBy : sortBy.substring(1));

        // Every field SearchGuardrailUseCase accepts has a case here. An unknown one is refused rather
        // than ordered on something else, which would let a test asserting that order pass against this
        // double while the real backends sorted by another column.
        Comparator<Guardrail> comparator = switch (field) {
            case "name" -> Comparator.comparing(Guardrail::getName, Comparator.nullsLast(Comparator.naturalOrder()));
            case "phase" -> Comparator.comparing(
                g -> g.getPhase() == null ? null : g.getPhase().name(),
                Comparator.nullsLast(Comparator.naturalOrder())
            );
            case "createdAt" -> Comparator.comparing(Guardrail::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "updatedAt" -> Comparator.comparing(Guardrail::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "deployedAt" -> Comparator.comparing(Guardrail::getDeployedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> throw new IllegalArgumentException("This double does not sort on '" + field + "'");
        };
        // The id breaks ties, ascending whatever the main order asks for, as both real backends do: a
        // Guardrail's phase takes one of two values and its deployedAt is null until something deploys it,
        // so a single sort key leaves the order undefined and offset paging could then repeat or skip
        // rows between two pages.
        return (ascending ? comparator : comparator.reversed()).thenComparing(Guardrail::getId);
    }
}

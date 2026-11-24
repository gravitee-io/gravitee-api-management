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
package io.gravitee.gamma.rest.infra.adapter;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.common.data.domain.Page;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailNameAlreadyExistsException;
import io.gravitee.gamma.rest.core.guardrail.model.Guardrail;
import io.gravitee.gamma.rest.core.guardrail.model.GuardrailSearchQuery;
import io.gravitee.gamma.rest.core.guardrail.port.repository.GuardrailRepository;
import io.gravitee.repository.management.api.search.GuardrailCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;

/**
 * The core's {@link GuardrailRepository} port on the platform's management repository SPI.
 *
 * <p>A Guardrail is stored as its definition, serialized into the {@code definition} column and read
 * back from it: that column is the record of truth. The {@code id}, {@code environment_id},
 * {@code name} and {@code phase} columns are a projection of it, written on every save so the search
 * can filter, scope and sort without deserializing every row, and never read back — which is what
 * keeps the two from disagreeing. {@code description} is not a projection: the gateway has no use for
 * it, so it is a column the core owns outright, carried both ways. {@code lifecycle_state} and
 * {@code deployed_at} are carried too, but the core derives both from the Guardrail's publications and
 * writes back only what it read.
 *
 * <p>The three translations the core must not know about — the serialization above, the enums the SPI declares as its
 * own and the {@link Instant} timestamps it declares as {@link Date} — live in {@link GuardrailRepositoryMapper}.
 *
 * <p>Paging also shifts here: the core counts pages from one, the way the console does, and the SPI
 * counts from zero. The sort string arrives already validated by the use case, so this class only
 * has to split the {@code -} prefix off the field name.
 *
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class GammaGuardrailRepositoryAdapter implements GuardrailRepository {

    private final io.gravitee.repository.management.api.GuardrailRepository guardrailRepository;

    @Override
    public Guardrail create(Guardrail guardrail) {
        try {
            return RepositoryCalls.wrap(
                () -> GuardrailRepositoryMapper.toCore(guardrailRepository.create(GuardrailRepositoryMapper.toRepository(guardrail))),
                "Failed to create guardrail '%s'".formatted(guardrail.getId())
            );
        } catch (RuntimeException e) {
            throw refusalForCreate(e, guardrail);
        }
    }

    /**
     * Whether a failed write broke a unique key rather than being the store's fault. Narrowed to a
     * duplicate key on purpose: a NOT NULL or a length violation is neither a taken id nor a taken
     * name, and reporting it as a 400 would be as misleading as the 500 it replaced.
     *
     * <p>The two backends do not report it the same way: Mongo lets Spring's unchecked
     * {@link DuplicateKeyException} out of {@code insert()}, while JDBC wraps it in the
     * SPI's checked exception, which {@link RepositoryCalls} has already turned into a
     * {@link TechnicalDomainException} by the time it reaches here. Walking the cause chain covers
     * both without this adapter having to know which backend is wired.
     */
    private static boolean clashesOnDuplicateKey(Throwable failure) {
        for (var cause = failure; cause != null && cause != cause.getCause(); cause = cause.getCause()) {
            if (cause instanceof DuplicateKeyException) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Optional<Guardrail> findByEnvironmentId(String environmentId, String guardrailId) {
        var found = RepositoryCalls.wrap(
            () -> guardrailRepository.findById(guardrailId),
            "Failed to fetch guardrail '%s' for environment '%s'".formatted(guardrailId, environmentId)
        );
        return found.filter(guardrail -> environmentId.equals(guardrail.getEnvironmentId())).map(GuardrailRepositoryMapper::toCore);
    }

    @Override
    public List<Guardrail> findByEnvironmentIdAndIds(String environmentId, Set<String> guardrailIds) {
        return RepositoryCalls.wrap(
            () -> guardrailRepository.findByEnvironmentIdAndIds(environmentId, guardrailIds),
            "Failed to fetch Guardrails for environment '%s'".formatted(environmentId)
        )
            .stream()
            .map(GuardrailRepositoryMapper::toCore)
            .toList();
    }

    @Override
    public List<Guardrail> findByEnvironmentIdAndName(String environmentId, String name) {
        return RepositoryCalls.wrap(
            () -> guardrailRepository.findByEnvironmentIdAndNameIgnoreCase(environmentId, name),
            "Failed to fetch guardrails named '%s' for environment '%s'".formatted(name, environmentId)
        )
            .stream()
            .map(GuardrailRepositoryMapper::toCore)
            .toList();
    }

    @Override
    public void updateUpdatedAt(String environmentId, String guardrailId, Instant updatedAt) {
        RepositoryCalls.wrap(
            () -> {
                guardrailRepository.updateUpdatedAt(environmentId, guardrailId, Date.from(updatedAt));
                return null;
            },
            "Failed to update Guardrail timestamp '" + guardrailId + "'"
        );
    }

    @Override
    public Guardrail update(Guardrail guardrail) {
        try {
            return RepositoryCalls.wrap(
                () -> GuardrailRepositoryMapper.toCore(guardrailRepository.update(GuardrailRepositoryMapper.toRepository(guardrail))),
                "Failed to update guardrail '%s'".formatted(guardrail.getId())
            );
        } catch (RuntimeException e) {
            // A rename can break the unique name as readily as a creation can. The domain refuses a
            // duplicate first, so reaching here means two writers renamed onto one name at once; the
            // loser deserves the same 409 as the create path rather than a technical failure.
            throw refusalForUpdate(e, guardrail);
        }
    }

    @Override
    public Optional<Guardrail> updateIfVersionMatches(Guardrail guardrail, int expectedVersion) {
        try {
            return RepositoryCalls.wrap(
                () -> guardrailRepository.updateIfVersionMatches(GuardrailRepositoryMapper.toRepository(guardrail), expectedVersion),
                "Failed to update guardrail '%s'".formatted(guardrail.getId())
            ).map(GuardrailRepositoryMapper::toCore);
        } catch (RuntimeException e) {
            throw refusalForUpdate(e, guardrail);
        }
    }

    /**
     * What to raise for a creation the store refused.
     *
     * <p>Two unique keys can refuse one: the primary key, and (environmentId, name) since the Guardrail
     * name became unique. The store does not say which, so the name is looked up — taken by another
     * Guardrail means the name, and nothing else leaves the id as the only explanation.
     */
    private RuntimeException refusalForCreate(RuntimeException failure, Guardrail guardrail) {
        if (!clashesOnDuplicateKey(failure)) {
            return failure;
        }
        if (nameIsTakenByAnother(guardrail)) {
            return new GuardrailNameAlreadyExistsException(guardrail.getName());
        }
        // Deliberately "not available" and not "already exists": the primary key is global, so
        // asserting existence would describe a Guardrail the caller may have no access to.
        return new ValidationDomainException("Id '%s' is not available.".formatted(guardrail.getId()));
    }

    /**
     * What to raise for an update the store refused. Only the name can clash: the id comes from the
     * path and already names a stored Guardrail, so anything else is the store's own failure.
     */
    private RuntimeException refusalForUpdate(RuntimeException failure, Guardrail guardrail) {
        if (clashesOnDuplicateKey(failure) && nameIsTakenByAnother(guardrail)) {
            return new GuardrailNameAlreadyExistsException(guardrail.getName());
        }
        return failure;
    }

    /**
     * Whether another Guardrail of the environment already carries this name.
     *
     * <p>Excluding this one: a creation refused on its primary key finds the row holding that id, and
     * reporting its name would hide the id clash the caller actually hit. A failure of this read is
     * swallowed on purpose — it is a second guess at an error already in hand, and letting it out
     * would replace a 400 or a 409 with a 500.
     */
    private boolean nameIsTakenByAnother(Guardrail guardrail) {
        try {
            return findByEnvironmentIdAndName(guardrail.getEnvironmentId(), guardrail.getName())
                .stream()
                .anyMatch(existing -> !existing.getId().equals(guardrail.getId()));
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    @Override
    public void delete(String guardrailId) {
        RepositoryCalls.wrap(
            () -> {
                guardrailRepository.delete(guardrailId);
                return null;
            },
            "Failed to delete guardrail '%s'".formatted(guardrailId)
        );
    }

    @Override
    public Page<Guardrail> search(GuardrailSearchQuery query) {
        var criteria = GuardrailCriteria.builder().environmentId(query.environmentId()).query(query.query()).build();

        var page = RepositoryCalls.wrap(
            () -> guardrailRepository.search(criteria, toPageable(query), toSortable(query.sortBy())),
            "Failed to search guardrails for environment '%s'".formatted(query.environmentId())
        );

        return new Page<>(
            page.getContent().stream().map(GuardrailRepositoryMapper::toCore).toList(),
            query.pageNumber(),
            (int) page.getPageElements(),
            page.getTotalElements()
        );
    }

    private static Pageable toPageable(GuardrailSearchQuery query) {
        // the core counts pages from one, the SPI from zero
        return new PageableBuilder().pageNumber(query.pageNumber() - 1).pageSize(query.pageSize()).build();
    }

    private static Sortable toSortable(String sortBy) {
        if (sortBy == null) {
            // GuardrailSearchQuery documents null as "let the repository apply its default", and both
            // SPI implementations honour it. Returning null here rather than throwing keeps that
            // contract true for any caller of the port, not just the use case that always fills it.
            return null;
        }
        var ascending = !sortBy.startsWith("-");
        return new SortableBuilder().field(ascending ? sortBy : sortBy.substring(1)).setAsc(ascending).build();
    }
}

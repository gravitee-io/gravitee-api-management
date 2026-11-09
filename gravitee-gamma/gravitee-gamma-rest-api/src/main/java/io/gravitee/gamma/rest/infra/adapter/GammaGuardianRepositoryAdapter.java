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
import io.gravitee.apim.infra.adapter.GraviteeJacksonMapper;
import io.gravitee.common.data.domain.Page;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianNameAlreadyExistsException;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import io.gravitee.gamma.rest.core.guardian.model.GuardianLifecycleState;
import io.gravitee.gamma.rest.core.guardian.model.GuardianSearchQuery;
import io.gravitee.gamma.rest.core.guardian.port.repository.GuardianRepository;
import io.gravitee.repository.management.api.search.GuardianCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;

/**
 * The core's {@link GuardianRepository} port on the platform's management repository SPI.
 *
 * <p>A Guardian is stored as its definition, serialized into the {@code definition} column and read
 * back from it: that column is the record of truth. The {@code id}, {@code environment_id},
 * {@code name} and {@code phase} columns are a projection of it, written on every save so the search
 * can filter, scope and sort without deserializing every row, and never read back — which is what
 * keeps the two from disagreeing. {@code description} and {@code deployed_at} are not a projection:
 * the gateway has no use for either, so they are columns the core owns outright, carried both ways.
 *
 * <p>Three translations the core must not know about: the serialization above, the phase enum, which
 * the SPI declares as its own, and the {@link ZonedDateTime} timestamps, which it declares as
 * {@link Date}.
 *
 * <p>Paging also shifts here: the core counts pages from one, the way the console does, and the SPI
 * counts from zero. The sort string arrives already validated by the use case, so this class only
 * has to split the {@code -} prefix off the field name.
 *
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class GammaGuardianRepositoryAdapter implements GuardianRepository {

    private final io.gravitee.repository.management.api.GuardianRepository guardianRepository;

    @Override
    public Guardian create(Guardian guardian) {
        try {
            return RepositoryCalls.wrap(
                () -> toCore(guardianRepository.create(toRepository(guardian))),
                "Failed to create guardian '%s'".formatted(guardian.getId())
            );
        } catch (RuntimeException e) {
            throw refusalForCreate(e, guardian);
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
    public Optional<Guardian> findByEnvironmentId(String environmentId, String guardianId) {
        var found = RepositoryCalls.wrap(
            () -> guardianRepository.findById(guardianId),
            "Failed to fetch guardian '%s' for environment '%s'".formatted(guardianId, environmentId)
        );
        return found.filter(guardian -> environmentId.equals(guardian.getEnvironmentId())).map(GammaGuardianRepositoryAdapter::toCore);
    }

    @Override
    public List<Guardian> findByEnvironmentIdAndName(String environmentId, String name) {
        return RepositoryCalls.wrap(
            () -> guardianRepository.findByEnvironmentIdAndNameIgnoreCase(environmentId, name),
            "Failed to fetch guardians named '%s' for environment '%s'".formatted(name, environmentId)
        )
            .stream()
            .map(GammaGuardianRepositoryAdapter::toCore)
            .toList();
    }

    @Override
    public Guardian update(Guardian guardian) {
        try {
            return RepositoryCalls.wrap(
                () -> toCore(guardianRepository.update(toRepository(guardian))),
                "Failed to update guardian '%s'".formatted(guardian.getId())
            );
        } catch (RuntimeException e) {
            // A rename can break the unique name as readily as a creation can. The domain refuses a
            // duplicate first, so reaching here means two writers renamed onto one name at once; the
            // loser deserves the same 409 as the create path rather than a technical failure.
            throw refusalForUpdate(e, guardian);
        }
    }

    /**
     * What to raise for a creation the store refused.
     *
     * <p>Two unique keys can refuse one: the primary key, and (environmentId, name) since the Guardian
     * name became unique. The store does not say which, so the name is looked up — taken by another
     * Guardian means the name, and nothing else leaves the id as the only explanation.
     */
    private RuntimeException refusalForCreate(RuntimeException failure, Guardian guardian) {
        if (!clashesOnDuplicateKey(failure)) {
            return failure;
        }
        if (nameIsTakenByAnother(guardian)) {
            return new GuardianNameAlreadyExistsException(guardian.getName());
        }
        // Deliberately "not available" and not "already exists": the primary key is global, so
        // asserting existence would describe a Guardian the caller may have no access to.
        return new ValidationDomainException("Id '%s' is not available.".formatted(guardian.getId()));
    }

    /**
     * What to raise for an update the store refused. Only the name can clash: the id comes from the
     * path and already names a stored Guardian, so anything else is the store's own failure.
     */
    private RuntimeException refusalForUpdate(RuntimeException failure, Guardian guardian) {
        if (clashesOnDuplicateKey(failure) && nameIsTakenByAnother(guardian)) {
            return new GuardianNameAlreadyExistsException(guardian.getName());
        }
        return failure;
    }

    /**
     * Whether another Guardian of the environment already carries this name.
     *
     * <p>Excluding this one: a creation refused on its primary key finds the row holding that id, and
     * reporting its name would hide the id clash the caller actually hit. A failure of this read is
     * swallowed on purpose — it is a second guess at an error already in hand, and letting it out
     * would replace a 400 or a 409 with a 500.
     */
    private boolean nameIsTakenByAnother(Guardian guardian) {
        try {
            return findByEnvironmentIdAndName(guardian.getEnvironmentId(), guardian.getName())
                .stream()
                .anyMatch(existing -> !existing.getId().equals(guardian.getId()));
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    @Override
    public void delete(String guardianId) {
        RepositoryCalls.wrap(
            () -> {
                guardianRepository.delete(guardianId);
                return null;
            },
            "Failed to delete guardian '%s'".formatted(guardianId)
        );
    }

    @Override
    public Page<Guardian> search(GuardianSearchQuery query) {
        var criteria = GuardianCriteria.builder().environmentId(query.environmentId()).query(query.query()).build();

        var page = RepositoryCalls.wrap(
            () -> guardianRepository.search(criteria, toPageable(query), toSortable(query.sortBy())),
            "Failed to search guardians for environment '%s'".formatted(query.environmentId())
        );

        return new Page<>(
            page.getContent().stream().map(GammaGuardianRepositoryAdapter::toCore).toList(),
            query.pageNumber(),
            (int) page.getPageElements(),
            page.getTotalElements()
        );
    }

    private static Pageable toPageable(GuardianSearchQuery query) {
        // the core counts pages from one, the SPI from zero
        return new PageableBuilder().pageNumber(query.pageNumber() - 1).pageSize(query.pageSize()).build();
    }

    private static Sortable toSortable(String sortBy) {
        if (sortBy == null) {
            // GuardianSearchQuery documents null as "let the repository apply its default", and both
            // SPI implementations honour it. Returning null here rather than throwing keeps that
            // contract true for any caller of the port, not just the use case that always fills it.
            return null;
        }
        var ascending = !sortBy.startsWith("-");
        return new SortableBuilder().field(ascending ? sortBy : sortBy.substring(1)).setAsc(ascending).build();
    }

    private static Guardian toCore(io.gravitee.repository.management.model.Guardian source) {
        if (source == null) {
            return null;
        }
        return Guardian.builder()
            .definition(deserialize(source.getDefinition(), source.getId()))
            .description(source.getDescription())
            .lifecycleState(source.getLifecycleState() == null ? null : GuardianLifecycleState.valueOf(source.getLifecycleState().name()))
            .deployedAt(source.getDeployedAt())
            .createdAt(toZonedDateTime(source.getCreatedAt()))
            .updatedAt(toZonedDateTime(source.getUpdatedAt()))
            .build();
    }

    private static io.gravitee.repository.management.model.Guardian toRepository(Guardian source) {
        var target = new io.gravitee.repository.management.model.Guardian();
        target.setDefinition(serialize(source.getDefinition(), source.getId()));
        target.setId(source.getId());
        target.setEnvironmentId(source.getEnvironmentId());
        target.setName(source.getName());
        target.setDescription(source.getDescription());
        target.setPhase(
            source.getPhase() == null ? null : io.gravitee.repository.management.model.Guardian.Phase.valueOf(source.getPhase().name())
        );
        target.setLifecycleState(
            source.getLifecycleState() == null
                ? null
                : io.gravitee.repository.management.model.GuardianLifecycleState.valueOf(source.getLifecycleState().name())
        );
        target.setDeployedAt(source.getDeployedAt());
        target.setCreatedAt(toDate(source.getCreatedAt()));
        target.setUpdatedAt(toDate(source.getUpdatedAt()));
        return target;
    }

    /**
     * Reads the stored definition, or fails. A row whose definition cannot be read carries no name, no
     * phase and no guardrail sequence, so returning a half-built Guardian would hand the console an
     * entry it can neither render nor repair; the search maps every row, so one such row fails the
     * whole page rather than itself, which is the point.
     */
    private static io.gravitee.definition.model.v4.guardian.Guardian deserialize(String definition, String guardianId) {
        if (definition == null || definition.isBlank()) {
            throw new TechnicalDomainException("Guardian '%s' has no stored definition.".formatted(guardianId));
        }
        try {
            return GraviteeJacksonMapper.getInstance().readValue(definition, io.gravitee.definition.model.v4.guardian.Guardian.class);
        } catch (IOException e) {
            throw new TechnicalDomainException("Failed to read the definition of guardian '%s'.".formatted(guardianId), e);
        }
    }

    private static String serialize(io.gravitee.definition.model.v4.guardian.Guardian definition, String guardianId) {
        try {
            return GraviteeJacksonMapper.getInstance().writeValueAsString(definition);
        } catch (IOException e) {
            throw new TechnicalDomainException("Failed to write the definition of guardian '%s'.".formatted(guardianId), e);
        }
    }

    private static ZonedDateTime toZonedDateTime(Date date) {
        return date == null ? null : ZonedDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
    }

    private static Date toDate(ZonedDateTime dateTime) {
        return dateTime == null ? null : Date.from(dateTime.toInstant());
    }
}

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
            if (clashesOnTheId(e)) {
                // Deliberately "not available" and not "already exists": the primary key is global, so
                // asserting existence would describe a Guardian the caller may have no access to.
                throw new ValidationDomainException("Id '%s' is not available.".formatted(guardian.getId()));
            }
            throw e;
        }
    }

    /**
     * Whether a failed write is a clash on the caller-supplied id rather than the store's fault.
     * Narrowed to a duplicate key on purpose: a NOT NULL or a length violation is not "this id is
     * taken", and reporting it as a 400 would be as misleading as the 500 it replaced.
     *
     * <p>The two backends do not report it the same way: Mongo lets Spring's unchecked
     * {@link DuplicateKeyException} out of {@code insert()}, while JDBC wraps it in the
     * SPI's checked exception, which {@link RepositoryCalls} has already turned into a
     * {@link TechnicalDomainException} by the time it reaches here. Walking the cause chain covers
     * both without this adapter having to know which backend is wired.
     */
    private static boolean clashesOnTheId(Throwable failure) {
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
    public Guardian update(Guardian guardian) {
        return RepositoryCalls.wrap(
            () -> toCore(guardianRepository.update(toRepository(guardian))),
            "Failed to update guardian '%s'".formatted(guardian.getId())
        );
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

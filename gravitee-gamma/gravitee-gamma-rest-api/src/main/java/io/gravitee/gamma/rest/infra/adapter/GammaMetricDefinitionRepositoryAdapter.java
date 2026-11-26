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

import static io.gravitee.gamma.rest.core.observability.catalog.model.MetricDefinitionNames.printable;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.gamma.rest.core.observability.catalog.exception.DuplicateMetricDefinitionException;
import io.gravitee.gamma.rest.core.observability.catalog.exception.MetricDefinitionNotFoundException;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricDefinition;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricField;
import io.gravitee.gamma.rest.core.observability.catalog.port.repository.AdditionalMetricDefinitionRepository;
import io.gravitee.gamma.rest.core.observability.catalog.port.service_provider.AdditionalMetricGrammarPort;
import io.gravitee.repository.exceptions.DuplicateKeyException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.GammaMetricDefinitionRepository;
import io.gravitee.repository.management.model.GammaMetricDefinition;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * Wraps the {@code GammaMetricDefinitionRepository} SPI for the core layer.
 *
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class GammaMetricDefinitionRepositoryAdapter implements AdditionalMetricDefinitionRepository {

    private final GammaMetricDefinitionRepository repository;
    private final AdditionalMetricGrammarPort grammar;

    @Override
    public List<AdditionalMetricDefinition> findByEnvironmentId(String environmentId) {
        return RepositoryCalls.wrap(
            () -> repository.findByEnvironmentId(environmentId).stream().map(this::toCore).toList(),
            "Failed to list the metric definitions of environment '%s'".formatted(environmentId)
        );
    }

    @Override
    public Optional<AdditionalMetricDefinition> findByEnvironmentIdAndField(String environmentId, AdditionalMetricField field) {
        return RepositoryCalls.wrap(
            () -> repository.findByEnvironmentIdAndRawKey(environmentId, storedKey(field)).map(this::toCore),
            "Failed to fetch the metric definition '%s' of environment '%s'".formatted(printable(field.name()), environmentId)
        );
    }

    @Override
    public AdditionalMetricDefinition create(AdditionalMetricDefinition definition) {
        try {
            return toCore(repository.create(toRepository(definition)));
        } catch (DuplicateKeyException e) {
            // Lost a race with a concurrent creation on the same key.
            throw new DuplicateMetricDefinitionException(definition.field().name());
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(
                "Failed to create the metric definition '%s'".formatted(printable(definition.field().name())),
                e
            );
        }
    }

    /** Both stores raise {@link IllegalStateException} when no record has the id any more: a concurrent delete. */
    @Override
    public AdditionalMetricDefinition update(AdditionalMetricDefinition definition) {
        try {
            return toCore(repository.update(toRepository(definition)));
        } catch (IllegalStateException deletedMeanwhile) {
            throw new MetricDefinitionNotFoundException(definition.field().name());
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(
                "Failed to update the metric definition '%s'".formatted(printable(definition.field().name())),
                e
            );
        }
    }

    @Override
    public void delete(String id) {
        RepositoryCalls.wrap(
            () -> {
                repository.delete(id);
                return null;
            },
            "Failed to delete the metric definition '%s'".formatted(id)
        );
    }

    /**
     * The key a record is stored under: an additional key bare, as before custom keys had records, a custom key with
     * its root. An additional key starts with its type prefix, never with {@code custom.}, so the two never meet.
     */
    private static String storedKey(AdditionalMetricField field) {
        return field.servedRawKey();
    }

    /** The wire name of a key stored by {@link #storedKey}. */
    private static String nameOfStoredKey(String stored) {
        return stored.startsWith(AdditionalMetricField.Root.CUSTOM.prefix()) ? stored : AdditionalMetricField.NAME_PREFIX + stored;
    }

    private AdditionalMetricDefinition toCore(GammaMetricDefinition source) {
        String stored = source.getRawKey();
        var field = grammar
            .parse(nameOfStoredKey(stored))
            .orElseThrow(() ->
                new TechnicalDomainException(
                    "Persisted metric definition '%s' is not an additional or custom metric key".formatted(printable(stored))
                )
            );
        return new AdditionalMetricDefinition(
            source.getId(),
            source.getEnvironmentId(),
            field,
            source.getLabel(),
            source.getDescription(),
            source.getUnit(),
            source.isExposed(),
            source.getCreatedAt() == null ? null : source.getCreatedAt().toInstant(),
            source.getUpdatedAt() == null ? null : source.getUpdatedAt().toInstant()
        );
    }

    private static GammaMetricDefinition toRepository(AdditionalMetricDefinition source) {
        return GammaMetricDefinition.builder()
            .id(source.id())
            .environmentId(source.environmentId())
            .rawKey(storedKey(source.field()))
            .label(source.label())
            .description(source.description())
            .unit(source.unit())
            .exposed(source.exposed())
            .createdAt(source.createdAt() == null ? null : Date.from(source.createdAt()))
            .updatedAt(source.updatedAt() == null ? null : Date.from(source.updatedAt()))
            .build();
    }
}

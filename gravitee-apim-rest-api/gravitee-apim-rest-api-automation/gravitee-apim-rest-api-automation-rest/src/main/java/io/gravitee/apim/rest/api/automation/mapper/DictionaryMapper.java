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
package io.gravitee.apim.rest.api.automation.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.dictionary.model.Dictionary;
import io.gravitee.apim.core.dictionary.model.DictionaryProperty;
import io.gravitee.apim.rest.api.automation.model.DictionaryProvider;
import io.gravitee.apim.rest.api.automation.model.DictionarySpec;
import io.gravitee.apim.rest.api.automation.model.DictionaryState;
import io.gravitee.apim.rest.api.automation.model.DictionaryTrigger;
import io.gravitee.apim.rest.api.automation.model.DictionaryType;
import io.gravitee.apim.rest.api.automation.model.DynamicDictionarySpec;
import io.gravitee.apim.rest.api.automation.model.EncryptableValue;
import io.gravitee.apim.rest.api.automation.model.HttpDictionaryProvider;
import io.gravitee.apim.rest.api.automation.model.ManualDictionarySpec;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryEntity;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryPropertyOptions;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryProviderEntity;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryTriggerEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.impl.configuration.dictionary.InvalidDictionaryPropertyOptionsException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DictionaryMapper {
    DictionaryMapper INSTANCE = Mappers.getMapper(DictionaryMapper.class);

    ObjectMapper GRAVITEE_MAPPER = new GraviteeMapper();

    // ===== DictionarySpec → Dictionary (core) =====

    @Mapping(source = "type", target = "type")
    @Mapping(target = "properties", expression = "java(mapManualProperties(spec))")
    @Mapping(source = "dynamic.provider", target = "provider")
    @Mapping(source = "dynamic.trigger", target = "trigger")
    Dictionary toDictionary(DictionarySpec spec);

    /**
     * Merges the manifest's two property maps into one core list.
     *
     * <p>A key in {@code properties} states nothing about encryption, so the stored classification
     * stands and a manifest written before {@code encryptedProperties} existed applies unchanged. A
     * key in {@code encryptedProperties} carries a plaintext value to encrypt on save.
     */
    default List<DictionaryProperty> mapManualProperties(DictionarySpec spec) {
        if (spec.getManual() == null) {
            return null;
        }
        Map<String, String> plain = spec.getManual().getProperties();
        Map<String, EncryptableValue> secret = spec.getManual().getEncryptedProperties();
        if (plain == null && secret == null) {
            return null;
        }
        rejectKeysDeclaredTwice(plain, secret);
        return Stream.concat(
            plain == null ? Stream.empty() : plain.entrySet().stream().map(DictionaryMapper::toPlainProperty),
            secret == null ? Stream.empty() : secret.entrySet().stream().map(DictionaryMapper::toSecretProperty)
        ).toList();
    }

    private static DictionaryProperty toPlainProperty(Map.Entry<String, String> property) {
        return DictionaryProperty.builder().key(property.getKey()).value(property.getValue()).build();
    }

    private static DictionaryProperty toSecretProperty(Map.Entry<String, EncryptableValue> property) {
        return DictionaryProperty.builder()
            .key(property.getKey())
            .value(property.getValue() == null ? null : property.getValue().getValue())
            .encryptable(true)
            .build();
    }

    private static void rejectKeysDeclaredTwice(Map<String, String> plain, Map<String, EncryptableValue> secret) {
        if (plain == null || secret == null) {
            return;
        }
        secret
            .keySet()
            .stream()
            .filter(plain::containsKey)
            .findFirst()
            .ifPresent(key -> {
                throw new InvalidDictionaryPropertyOptionsException(key, "it is declared in both 'properties' and 'encryptedProperties'");
            });
    }

    io.gravitee.apim.core.dictionary.model.DictionaryType toCoreType(DictionaryType type);

    @Mapping(source = "unit", target = "unit", qualifiedByName = "specTriggerUnitToTimeUnit")
    io.gravitee.apim.core.dictionary.model.DictionaryTrigger toCoreTrigger(DictionaryTrigger trigger);

    default io.gravitee.apim.core.dictionary.model.DictionaryProvider toCoreProvider(DictionaryProvider provider) {
        if (provider == null) return null;
        Object actual = provider.getActualInstance();
        if (actual instanceof HttpDictionaryProvider http) {
            return io.gravitee.apim.core.dictionary.model.DictionaryProvider.builder()
                .type(http.getType().getValue())
                .configuration(GRAVITEE_MAPPER.valueToTree(http))
                .build();
        }
        return null;
    }

    /**
     * Reports a secret property as its key alone. The value is never returned, so a client can compare
     * a {@code GET} against its manifest without seeing a difference on a value it cannot read back.
     *
     * <p>Returns {@code null} rather than an empty map when nothing is encrypted, so a dictionary
     * without secrets serializes exactly as it did before this field existed.
     */
    private static Map<String, EncryptableValue> toSpecEncryptedProperties(Set<String> encryptedKeys) {
        if (encryptedKeys.isEmpty()) {
            return null;
        }
        return encryptedKeys
            .stream()
            .collect(LinkedHashMap::new, (values, key) -> values.put(key, new EncryptableValue()), LinkedHashMap::putAll);
    }

    private static Set<String> encryptedKeys(Map<String, DictionaryPropertyOptions> options) {
        if (options == null) {
            return Set.of();
        }
        return options
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue() != null && Boolean.TRUE.equals(entry.getValue().getEncrypted()))
            .map(Map.Entry::getKey)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Map<String, String> toSpecPlainProperties(Map<String, String> properties, Set<String> encryptedKeys) {
        if (properties == null) {
            return Map.of();
        }
        return properties
            .entrySet()
            .stream()
            .filter(entry -> !encryptedKeys.contains(entry.getKey()))
            .collect(LinkedHashMap::new, (plain, entry) -> plain.put(entry.getKey(), entry.getValue()), LinkedHashMap::putAll);
    }

    // ===== DictionaryEntity → DictionaryState =====

    default DictionaryState toDictionaryState(DictionaryEntity entity, ExecutionContext executionContext) {
        DictionaryState state = new DictionaryState(
            entity.getId(),
            executionContext.getEnvironmentId(),
            executionContext.getOrganizationId(),
            null
        );
        state.setHrid(entity.getKey() != null ? entity.getKey() : entity.getId());
        state.setName(entity.getName());
        state.setDescription(entity.getDescription());
        state.setType(toSpecType(entity.getType()));
        if (entity.getType() == io.gravitee.rest.api.model.configuration.dictionary.DictionaryType.MANUAL) {
            state.setDeployed(entity.getDeployedAt() != null);
            Set<String> encryptedKeys = encryptedKeys(entity.getPropertyOptions());
            ManualDictionarySpec manual = new ManualDictionarySpec();
            manual.setProperties(toSpecPlainProperties(entity.getProperties(), encryptedKeys));
            manual.setEncryptedProperties(toSpecEncryptedProperties(encryptedKeys));
            state.setManual(manual);
        } else {
            state.setDeployed(isEntityStarted(entity));
            DynamicDictionarySpec dynamic = new DynamicDictionarySpec();
            dynamic.setProvider(toSpecProvider(entity.getProvider()));
            dynamic.setTrigger(toSpecTrigger(entity.getTrigger()));
            state.setDynamic(dynamic);
        }
        return state;
    }

    default DictionaryState toDictionaryState(DictionarySpec spec) {
        var state = new DictionaryState();
        mapSpecToState(spec, state);
        stripSecretValues(state);
        return state;
    }

    /** A dry run echoes the caller's own payload, which must not carry the secrets back out. */
    private static void stripSecretValues(DictionaryState state) {
        if (state.getManual() == null || state.getManual().getEncryptedProperties() == null) {
            return;
        }
        state.getManual().setEncryptedProperties(toSpecEncryptedProperties(state.getManual().getEncryptedProperties().keySet()));
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "environmentId", ignore = true)
    @Mapping(target = "organizationId", ignore = true)
    @Mapping(target = "errors", ignore = true)
    void mapSpecToState(DictionarySpec spec, @MappingTarget DictionaryState state);

    DictionaryType toSpecType(io.gravitee.rest.api.model.configuration.dictionary.DictionaryType type);

    @Mapping(source = "unit", target = "unit", qualifiedByName = "timeUnitToSpecTriggerUnit")
    DictionaryTrigger toSpecTrigger(DictionaryTriggerEntity entity);

    default DictionaryProvider toSpecProvider(DictionaryProviderEntity entity) {
        if (entity == null) return null;
        try {
            HttpDictionaryProvider http = GRAVITEE_MAPPER.treeToValue(entity.getConfiguration(), HttpDictionaryProvider.class);
            http.setType(HttpDictionaryProvider.TypeEnum.fromValue(entity.getType()));
            return new DictionaryProvider(http);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to deserialize provider configuration", e);
        }
    }

    // ===== @Named conversion helpers =====

    @Named("specTriggerUnitToTimeUnit")
    default TimeUnit specTriggerUnitToTimeUnit(DictionaryTrigger.UnitEnum unit) {
        return unit != null ? TimeUnit.valueOf(unit.getValue()) : null;
    }

    @Named("timeUnitToSpecTriggerUnit")
    default DictionaryTrigger.UnitEnum timeUnitToSpecTriggerUnit(TimeUnit unit) {
        return unit != null ? DictionaryTrigger.UnitEnum.fromValue(unit.name()) : null;
    }

    default boolean isEntityStarted(DictionaryEntity entity) {
        return entity.getState() != null && "STARTED".equals(entity.getState().name());
    }
}

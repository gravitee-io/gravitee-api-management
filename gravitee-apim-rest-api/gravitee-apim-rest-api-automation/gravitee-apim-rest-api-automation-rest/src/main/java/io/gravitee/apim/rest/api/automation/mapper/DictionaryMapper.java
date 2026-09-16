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
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.rest.api.automation.model.DictionaryPropertyOptions;
import io.gravitee.apim.rest.api.automation.model.DictionaryProvider;
import io.gravitee.apim.rest.api.automation.model.DictionarySpec;
import io.gravitee.apim.rest.api.automation.model.DictionaryState;
import io.gravitee.apim.rest.api.automation.model.DictionaryTrigger;
import io.gravitee.apim.rest.api.automation.model.DictionaryType;
import io.gravitee.apim.rest.api.automation.model.DynamicDictionarySpec;
import io.gravitee.apim.rest.api.automation.model.HttpDictionaryProvider;
import io.gravitee.apim.rest.api.automation.model.ManualDictionarySpec;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryEntity;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryProviderEntity;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryTriggerEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
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
     * Zips the manifest's property map with its options map.
     *
     * <p>Values come from {@code properties}; {@code propertyOptions} only classifies them, and a key
     * it does not mention keeps whatever classification the stored property already has. A manifest
     * written before {@code propertyOptions} existed therefore applies unchanged.
     */
    default List<DictionaryProperty> mapManualProperties(DictionarySpec spec) {
        if (spec.getManual() == null || spec.getManual().getProperties() == null) {
            return null;
        }
        Map<String, DictionaryPropertyOptions> options = spec.getManual().getPropertyOptions();
        rejectOptionsWithoutProperty(spec.getManual().getProperties(), options);
        return spec
            .getManual()
            .getProperties()
            .entrySet()
            .stream()
            .map(entry -> toCoreProperty(entry, options == null ? null : options.get(entry.getKey())))
            .toList();
    }

    private static DictionaryProperty toCoreProperty(Map.Entry<String, String> property, DictionaryPropertyOptions options) {
        return DictionaryProperty.builder()
            .key(property.getKey())
            .value(property.getValue())
            .encrypted(options == null ? null : options.getEncrypted())
            .encryptable(options == null ? null : options.getEncryptable())
            .build();
    }

    private static void rejectOptionsWithoutProperty(Map<String, String> properties, Map<String, DictionaryPropertyOptions> options) {
        if (options == null) {
            return;
        }
        options
            .keySet()
            .stream()
            .filter(key -> !properties.containsKey(key))
            .findFirst()
            .ifPresent(key -> {
                throw new ValidationDomainException(
                    "Dictionary propertyOptions name '" + key + "', which is not declared in properties."
                );
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

    default Map<String, DictionaryPropertyOptions> toSpecPropertyOptions(
        Map<String, io.gravitee.rest.api.model.configuration.dictionary.DictionaryPropertyOptions> options
    ) {
        if (options == null) {
            return Map.of();
        }
        return options
            .entrySet()
            .stream()
            .collect(
                LinkedHashMap::new,
                (specOptions, entry) -> specOptions.put(entry.getKey(), new DictionaryPropertyOptions().encrypted(entry.getValue().getEncrypted())),
                LinkedHashMap::putAll
            );
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
            ManualDictionarySpec manual = new ManualDictionarySpec();
            manual.setProperties(entity.getProperties() == null ? Map.of() : entity.getProperties());
            manual.setPropertyOptions(toSpecPropertyOptions(entity.getPropertyOptions()));
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
        return state;
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

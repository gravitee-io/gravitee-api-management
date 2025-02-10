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
package io.gravitee.apim.infra.adapter;

import io.gravitee.apim.core.application_dictionary.model.Dictionary;
import io.gravitee.apim.core.application_dictionary.model.ManualDictionary;
import io.gravitee.repository.management.model.DictionaryType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DictionaryAdapter {
    DictionaryAdapter INSTANCE = Mappers.getMapper(DictionaryAdapter.class);

    @Mapping(target = "id", source = "dictionary.id")
    @Mapping(target = "environmentId", source = "dictionary.environmentId")
    @Mapping(target = "name", source = "dictionary.name")
    @Mapping(target = "description", source = "dictionary.description")
    @Mapping(target = "createdAt", source = "dictionary.createdAt")
    @Mapping(target = "updatedAt", source = "dictionary.updatedAt")
    @Mapping(target = "deployedAt", source = "dictionary.deployedAt")
    @Mapping(target = "properties", source = "dictionary.properties")
    ManualDictionary fromRepositoryManualDictionary(io.gravitee.repository.management.model.Dictionary dictionary);

    default Dictionary toEntity(io.gravitee.repository.management.model.Dictionary dictionary) {
        if (dictionary.getType() == DictionaryType.MANUAL) {
            return fromRepositoryManualDictionary(dictionary);
        }

        throw new IllegalArgumentException("Dynamic dictionary is not supported");
    }

    @Mapping(target = "id", source = "dictionary.id")
    @Mapping(target = "environmentId", source = "dictionary.environmentId")
    @Mapping(target = "name", source = "dictionary.name")
    @Mapping(target = "description", source = "dictionary.description")
    @Mapping(target = "createdAt", source = "dictionary.createdAt")
    @Mapping(target = "updatedAt", source = "dictionary.updatedAt")
    @Mapping(target = "deployedAt", source = "dictionary.deployedAt")
    @Mapping(target = "type", constant = "MANUAL")
    @Mapping(target = "properties", source = "dictionary.properties")
    io.gravitee.repository.management.model.Dictionary toRepositoryManualDictionary(ManualDictionary dictionary);

    default io.gravitee.repository.management.model.Dictionary toRepository(Dictionary dictionary) {
        if (dictionary instanceof ManualDictionary) {
            return toRepositoryManualDictionary((ManualDictionary) dictionary);
        }

        throw new IllegalArgumentException("Dynamic dictionary is not supported");
    }
}

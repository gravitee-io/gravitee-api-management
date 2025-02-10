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
package io.gravitee.rest.api.management.v2.rest.mapper;

import io.gravitee.apim.core.application_dictionary.model.Dictionary;
import io.gravitee.apim.core.application_dictionary.model.ManualDictionary;
import io.gravitee.apim.core.application_dictionary.use_case.UpdateApplicationDictionaryUseCase;
import io.gravitee.rest.api.management.v2.rest.model.ApplicationDictionary;
import io.gravitee.rest.api.service.common.GraviteeContext;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(imports = { GraviteeContext.class })
public interface ApplicationDictionaryMapper {
    ApplicationDictionaryMapper INSTANCE = Mappers.getMapper(ApplicationDictionaryMapper.class);

    @Mapping(target = "applicationId", source = "applicationId")
    @Mapping(target = "enabled", source = "applicationDictionary.enabled")
    @Mapping(target = "executionContext", expression = "java(GraviteeContext.getExecutionContext())")
    @Mapping(target = "dictionary", expression = "java(toCoreDictionary(applicationId, applicationDictionary))")
    UpdateApplicationDictionaryUseCase.Input toInput(String applicationId, ApplicationDictionary applicationDictionary);

    @Mapping(target = "enabled", source = "output.enabled")
    @Mapping(target = "dictionary", source = "output.dictionary")
    ApplicationDictionary toApplicationDictionary(UpdateApplicationDictionaryUseCase.Output output);

    @Mapping(target = "id", source = "applicationId")
    @Mapping(target = "name", source = "applicationDictionary.dictionary.name")
    @Mapping(target = "description", source = "applicationDictionary.dictionary.description")
    @Mapping(target = "properties", source = "applicationDictionary.dictionary.properties")
    ManualDictionary toManualDictionary(String applicationId, ApplicationDictionary applicationDictionary);

    default Dictionary toCoreDictionary(String applicationId, ApplicationDictionary applicationDictionary) {
        if (applicationDictionary.getDictionary().getType() == io.gravitee.rest.api.management.v2.rest.model.Dictionary.TypeEnum.MANUAL) {
            return toManualDictionary(applicationId, applicationDictionary);
        }

        throw new IllegalArgumentException("Dynamic dictionary is not supported");
    }
}

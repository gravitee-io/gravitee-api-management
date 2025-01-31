package io.gravitee.rest.api.management.v2.rest.mapper;

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
    @Mapping(target = "type", source = "applicationDictionary.dictionary.type")
    @Mapping(target = "properties", source = "applicationDictionary.dictionary.properties")
    @Mapping(target = "description", source = "applicationDictionary.dictionary.description")
    UpdateApplicationDictionaryUseCase.Input toInput(String applicationId, ApplicationDictionary applicationDictionary);

    @Mapping(target = "enabled", source = "output.enabled")
    @Mapping(target = "dictionary.properties", source = "output.dictionary.properties")
    @Mapping(target = "dictionary.type", source = "output.dictionary.type")
    @Mapping(target = "dictionary.description", source = "output.dictionary.description")
    ApplicationDictionary toApplicationDictionary(UpdateApplicationDictionaryUseCase.Output output);
}

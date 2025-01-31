package io.gravitee.apim.core.application_dictionary.crud_service;

import io.gravitee.repository.management.model.Dictionary;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Optional;

public interface ApplicationDictionaryCrudService {
    Optional<Dictionary> findById(String dictionaryId);

    void delete(ExecutionContext executionContext, Dictionary dictionary);

    Dictionary create(ExecutionContext executionContext, Dictionary dictionary);

    Dictionary update(ExecutionContext executionContext, Dictionary dictionary);
}

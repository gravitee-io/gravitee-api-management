package inmemory;

import io.gravitee.apim.core.application_dictionary.crud_service.ApplicationDictionaryCrudService;
import io.gravitee.repository.management.model.Dictionary;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ApplicationDictionaryCrudServiceInMemory implements ApplicationDictionaryCrudService {

    private Map<String, Dictionary> dictionaries;

    @Override
    public Optional<Dictionary> findById(String dictionaryId) {
        return Optional.ofNullable(dictionaries.get(dictionaryId));
    }

    @Override
    public void delete(ExecutionContext executionContext, Dictionary dictionary) {
        dictionaries.remove(dictionary.getId());
    }

    @Override
    public Dictionary create(ExecutionContext executionContext, Dictionary dictionary) {
        dictionaries.put(dictionary.getId(), dictionary);
        return dictionary;
    }

    @Override
    public Dictionary update(ExecutionContext executionContext, Dictionary dictionary) {
        dictionaries.put(dictionary.getId(), dictionary);
        return dictionary;
    }

    public void initWith(List<Dictionary> objects) {
        dictionaries = objects.stream().collect(Collectors.toMap(Dictionary::getId, Function.identity()));
    }

    public void reset() {
        dictionaries.clear();
    }
}

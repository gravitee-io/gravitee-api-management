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
package inmemory;

import io.gravitee.apim.core.application_dictionary.crud_service.ApplicationDictionaryCrudService;
import io.gravitee.apim.core.application_dictionary.model.Dictionary;
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

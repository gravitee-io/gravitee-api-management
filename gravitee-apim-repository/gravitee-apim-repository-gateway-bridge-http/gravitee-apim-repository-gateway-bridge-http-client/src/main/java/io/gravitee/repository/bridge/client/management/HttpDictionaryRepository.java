/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.bridge.client.management;

import io.gravitee.repository.bridge.client.utils.BodyCodecs;
import io.gravitee.repository.bridge.client.utils.ExcludeMethodFromGeneratedCoverage;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.DictionaryRepository;
import io.gravitee.repository.management.model.Dictionary;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class HttpDictionaryRepository extends AbstractRepository implements DictionaryRepository {

    @Override
    public Set<Dictionary> findAll() throws TechnicalException {
        return blockingGet(get("/dictionaries", BodyCodecs.set(Dictionary.class)).send()).payload();
    }

    @Override
    @ExcludeMethodFromGeneratedCoverage
    public Dictionary create(Dictionary dictionary) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    @ExcludeMethodFromGeneratedCoverage
    public Dictionary update(Dictionary dictionary) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    @ExcludeMethodFromGeneratedCoverage
    public void delete(String s) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    @ExcludeMethodFromGeneratedCoverage
    public Optional<Dictionary> findById(String s) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Set<Dictionary> findAllByEnvironments(Set<String> environments) throws TechnicalException {
        try {
            return blockingGet(
                get("/dictionaries", BodyCodecs.set(Dictionary.class))
                    .addQueryParam("environmentsIds", String.join(",", environments))
                    .send()
            )
                .payload();
        } catch (TechnicalException te) {
            // Ensure that an exception is thrown and managed by the caller
            throw new IllegalStateException(te);
        }
    }
}

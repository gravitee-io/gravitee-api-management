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

import io.gravitee.apim.core.api.domain_service.FetchApiDefinitionFromUrlDomainService;
import java.util.ArrayList;
import java.util.List;

public class FetchApiDefinitionFromUrlDomainServiceInMemory implements FetchApiDefinitionFromUrlDomainService, InMemoryAlternative<String> {

    private final List<String> storage = new ArrayList<>();
    private RuntimeException toThrow;

    public void willThrow(RuntimeException ex) {
        this.toThrow = ex;
    }

    @Override
    public String fetch(String url, List<String> whitelist, boolean allowPrivate) {
        if (toThrow != null) {
            throw toThrow;
        }
        return storage.isEmpty() ? "" : storage.get(0);
    }

    @Override
    public void initWith(List<String> items) {
        reset();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
        toThrow = null;
    }

    @Override
    public List<String> storage() {
        return storage;
    }
}
